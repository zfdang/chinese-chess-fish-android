# 运行pikafish引擎

在安卓应用里运行可执行程序


## pikafish引擎运行的问题

象棋程序会在程序里启动pikafish进程，并通过uci协议和pifafish进程进行通讯。

在Android Q之前，安卓程序可以将pikafish从assets里复制到data目录，然后运行data目录里的二进制文件。

但是Android Q之后，为了安全，程序就不允许运行data目录里的文件了。

error=13, Permission denied

From Android Q onwards, you cannot execute binaries in your app's private data directory

https://stackoverflow.com/questions/60370424/permission-is-denied-using-android-q-ffmpeg-error-13-permission-denied

```
The change to block exec() on application data files for targetAPI >= Q is working-as-intended.
Please see https://android-review.googlesource.com/c/platform/system/sepolicy/+/804149 for background on this change.
 Calling exec() on writable application files is a W^X (https://en.wikipedia.org/wiki/W%5EX) violation
 and represents an unsafe application practice.
Executable code should always be loaded from the application APK.

While exec() no longer works on files within the application home directory, it continues
to be supported for files within the read-only /data/app directory. In particular, it
should be possible to package the binaries into your application's native libs directory
and enable android:extractNativeLibs=true, and then call exec() on the /data/app artifacts.
 A similar approach is done with the wrap.sh functionality,
documented at https://developer.android.com/ndk/guides/wrap-script#packaging_wrapsh .

Additionally, please be aware that executables executed via exec() are not managed according
 to the Android process lifecycle, and generally speaking, exec() is discouraged from Android
applications. While not Android documentation, Using "exec()" with NDK covers this in some
detail. Relying on exec() may be problematic in future Android versions.
```


## Android Q之后的解决方法

https://withme.skullzbones.com/blog/programming/execute-native-binaries-android-q-no-root/

这篇文章里，介绍了一种方法，将pikafish文件作为jniLibs，复制到程序的lib目录里运行，这种方法是Android新版本也允许的。

本程序里的具体代码为：

### app/build.gradle.kts

创建一个task, 将src/main/pikafish/里的所有文件都打包成native-libs.jar

```
// https://withme.skullzbones.com/blog/programming/execute-native-binaries-android-q-no-root/
tasks.register<Jar>("nativeLibsToJar") {
    description = "create a jar archive of the native libs"
    destinationDirectory.set(layout.buildDirectory.dir("native-libs"))
    archiveBaseName.set("native-libs")
    from(fileTree("src/main/pikafish/") {
        include("**/*")
    })
    into("lib/")
}

tasks.named("preBuild") {
    dependsOn(tasks.named("nativeLibsToJar"))
}
```

本程序依赖生成的native-libs.jar

```
    // https://withme.skullzbones.com/blog/programming/execute-native-binaries-android-q-no-root/
    implementation(files("$buildDir/native-libs/native-libs.jar"))
```

### src/main/pikafish/的内容

app/src/main/pikafish/arm64-v8a/libpikafish-armv8-dotprod.so

```
❯ pwd
app/src/main/pikafish
❯ tree
.
└── arm64-v8a
    ├── libpikafish-armv8-dotprod.so
    ├── libpikafish-armv8.so
    ├── libpikafish.ini.so
    └── libpikafish.nnue.so

2 directories, 4 files
```

### 调用代码

app/src/main/java/org/petero/droidfish/engine/ExternalEngine.java

```
    private final String nativeLibraryDir = ChessApp.getContext().getApplicationInfo().nativeLibraryDir;

    File to = new File(nativeLibraryDir, PikafishEngineFile);

    String exePath = to.getAbsolutePath();
    Log.d("ExternalEngine", "Starting engine: " + exePath);

    String engineWorkDir = new File(exePath).getParentFile();

    ProcessBuilder pb = new ProcessBuilder(exePath);
    pb.directory(engineWorkDir);
    synchronized (EngineUtil.nativeLock) {
        engineProc = pb.start();
    }
    reNice();
```
    
    



## 上述方法在debug/release版本的差异

上面的方法，确实可以把目录里的任意文件都打包进apk里去，debug版本也可以正常的安装到lib里去

但是，release版本，会过滤非libxxx.so格式的文件！！！只有符合这个格式的，才会被复制到lib里去

所以如果你的可执行程序不符合这个规则，目前的解决方法是只能改名字了
