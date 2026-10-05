#include <jni.h>
#include <condition_variable>
#include <deque>
#include <iostream>
#include <memory>
#include <mutex>
#include <streambuf>
#include <string>
#include <unordered_map>
#include "src/attacks.h"
#include "src/misc.h"
#include "src/position.h"
#include "src/tune.h"
#include "src/uci.h"

namespace {
JavaVM* vm;
std::mutex sessionsMutex, engineMutex;
std::once_flag initializeTables;
jlong nextId = 1;

// Returns a JNIEnv usable on the calling thread, or nullptr when it cannot be obtained.
// Deliberately not cached in a thread_local: those destructors run from __cxa_thread_finalize
// during pthread_exit, by which point ART has already detached the thread and any JNI call
// (even ExceptionCheck) aborts the process with "JNI calls without being attached".
// Attaching per line costs a few microseconds, which is far below the cost of a search.
bool acquireEnv(JNIEnv** out, bool* attached) {
    *out = nullptr;
    *attached = false;
    if (vm == nullptr) return false;
    JNIEnv* env = nullptr;
    jint result = vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (result == JNI_OK) { *out = env; return true; }
    // JNI_EVERSION or any other failure leaves env untouched, so never dereference it here.
    if (result != JNI_EDETACHED) return false;
    if (vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return false;
    *out = env;
    *attached = true;
    return true;
}

class InputBuffer : public std::streambuf {
    std::mutex mutex;
    std::condition_variable ready;
    std::deque<std::string> commands;
    std::string current;
    bool closing = false;
public:
    void send(std::string command) {
        std::lock_guard lock(mutex);
        if (!closing) commands.push_back(std::move(command) + "\n");
        ready.notify_one();
    }
    void close() {
        std::lock_guard lock(mutex);
        closing = true;
        commands.clear();
        commands.emplace_back("quit\n");
        ready.notify_one();
    }
protected:
    int_type underflow() override {
        std::unique_lock lock(mutex);
        ready.wait(lock, [&] { return !commands.empty() || closing; });
        if (commands.empty()) return traits_type::eof();
        current = std::move(commands.front());
        commands.pop_front();
        char* begin = current.data();
        setg(begin, begin, begin + current.size());
        return traits_type::to_int_type(*gptr());
    }
};

class OutputBuffer : public std::streambuf {
    jobject callback;
    jmethodID lineMethod;
    // Shared by every engine thread. Upstream serializes whole lines with its own
    // IO_LOCK (sync_cout/sync_endl in src/misc.h), so characters never interleave here.
    std::string line;
    std::mutex mutex;
public:
    OutputBuffer(JNIEnv* env, jobject target) : callback(env->NewGlobalRef(target)) {
        jclass clazz = env->GetObjectClass(target);
        lineMethod = env->GetMethodID(clazz, "onLine", "(Ljava/lang/String;)V");
        env->DeleteLocalRef(clazz);
    }
    ~OutputBuffer() override {
        JNIEnv* env = nullptr;
        bool attached = false;
        if (acquireEnv(&env, &attached)) env->DeleteGlobalRef(callback);
        if (attached) vm->DetachCurrentThread();
    }
protected:
    int_type overflow(int_type value) override {
        if (traits_type::eq_int_type(value, traits_type::eof())) return traits_type::not_eof(value);
        std::lock_guard lock(mutex);
        char c = traits_type::to_char_type(value);
        if (c != '\n') { if (c != '\r') line += c; return value; }
        JNIEnv* env = nullptr;
        bool attached = false;
        if (!acquireEnv(&env, &attached)) {
            // Drop the line instead of returning eof: eof would put std::cout into a bad
            // state and silently swallow every later line, including bestmove.
            line.clear();
            return traits_type::not_eof(value);
        }
        jstring text = env->NewStringUTF(line.c_str());
        if (text != nullptr) {
            env->CallVoidMethod(callback, lineMethod, text);
            env->DeleteLocalRef(text);
        }
        // Binder failures must not leave a pending Java exception on a search thread.
        if (env->ExceptionCheck()) env->ExceptionClear();
        if (attached) vm->DetachCurrentThread();
        line.clear();
        return value;
    }
    int sync() override { return 0; }
};
struct Session {
    InputBuffer input;
    OutputBuffer output;
    Session(JNIEnv* env, jobject callback) : output(env, callback) {}
};
std::unordered_map<jlong, std::shared_ptr<Session>> sessions;
std::shared_ptr<Session> find(jlong id) {
    std::lock_guard lock(sessionsMutex);
    auto it = sessions.find(id);
    return it == sessions.end() ? nullptr : it->second;
}
}

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* javaVm, void*) {
    vm = javaVm;
    return JNI_VERSION_1_6;
}
extern "C" JNIEXPORT jlong JNICALL
Java_com_zfdang_chess_engine_NativeBridge_create(JNIEnv* env, jclass, jobject callback) {
    auto session = std::make_shared<Session>(env, callback);
    std::lock_guard lock(sessionsMutex);
    jlong id = nextId++;
    sessions.emplace(id, std::move(session));
    return id;
}
extern "C" JNIEXPORT void JNICALL
Java_com_zfdang_chess_engine_NativeBridge_command(JNIEnv* env, jclass, jlong id, jstring text) {
    auto session = find(id);
    if (!session) return;
    const char* chars = env->GetStringUTFChars(text, nullptr);
    if (chars) { session->input.send(chars); env->ReleaseStringUTFChars(text, chars); }
}
extern "C" JNIEXPORT void JNICALL
Java_com_zfdang_chess_engine_NativeBridge_stop(JNIEnv*, jclass, jlong id) {
    if (auto session = find(id)) session->input.close();
}
extern "C" JNIEXPORT void JNICALL
Java_com_zfdang_chess_engine_NativeBridge_run(JNIEnv* env, jclass, jlong id, jstring directory) {
    auto session = find(id);
    if (!session) return;
    const char* chars = env->GetStringUTFChars(directory, nullptr);
    if (!chars) return;
    std::string executable = std::string(chars) + "/pikafish";
    env->ReleaseStringUTFChars(directory, chars);
    // Stream routing is confined to the dedicated :pikafish Android service process.
    // Serialize sessions so an engine restart cannot reuse streams while old searches exist.
    std::lock_guard engineLock(engineMutex);
    auto* oldInput = std::cin.rdbuf(&session->input);
    auto* oldOutput = std::cout.rdbuf(&session->output);
    std::cin.clear();
    std::cout.clear();
    try {
        std::call_once(initializeTables, [] {
            Stockfish::Attacks::init();
            Stockfish::Position::init();
        });
        char* argv[] = {executable.data()};
        Stockfish::UCIEngine uci(Stockfish::CommandLine(1, argv));
        Stockfish::Tune::init(uci.engine_options());
        uci.loop();
    } catch (const std::exception& error) {
        std::cout << "info string JNI engine error: " << error.what() << std::endl;
    }
    // UCIEngine and its search threads have been destroyed before restoring streams.
    std::cin.rdbuf(oldInput);
    std::cout.rdbuf(oldOutput);
    std::lock_guard lock(sessionsMutex);
    sessions.erase(id);
}

extern "C" JNIEXPORT void JNICALL
Java_com_zfdang_chess_engine_NativeBridge_release(JNIEnv*, jclass, jlong id) {
    std::lock_guard lock(sessionsMutex);
    sessions.erase(id);
}
