package com.zfdang.chess;

import android.app.Application;
import android.content.Context;

public class ChessApp extends Application {
    private static ChessApp instance;

    private static Context appContext;

    /** Get the application context. */
    public static Context getContext() {
        return appContext;
    }

    public ChessApp() {
        instance = this;
        appContext = this;
    }

    public static ChessApp getInstance() {
        return instance;
    }
}
