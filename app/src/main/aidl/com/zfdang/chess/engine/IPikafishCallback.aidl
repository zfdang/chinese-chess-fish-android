package com.zfdang.chess.engine;
oneway interface IPikafishCallback {
    void onLine(String line);
    void onFinished(String error);
}
