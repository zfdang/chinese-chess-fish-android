package com.zfdang.chess.engine;
import com.zfdang.chess.engine.IPikafishCallback;
interface IPikafishService {
    long start(IPikafishCallback callback);
    oneway void command(long session, String command);
    oneway void shutdown(long session);
}
