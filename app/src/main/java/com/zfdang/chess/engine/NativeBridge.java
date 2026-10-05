package com.zfdang.chess.engine;

import com.zfdang.chess.BuildConfig;

/** Loaded only in the engine service process. */
final class NativeBridge {
    static { System.loadLibrary(BuildConfig.PIKAFISH_LIBRARY); }
    interface Callback { void onLine(String line); }
    static native long create(Callback callback);
    static native void command(long session, String command);
    static native void release(long session);
    static native void stop(long session);
    static native void run(long session, String networkDirectory);
    /** True when the CPU implements the ARM dot-product extension required by the dotprod flavor. */
    static native boolean supportsDotprod();
}
