package org.petero.droidfish.player;

public enum EngineStateValue {
    READ_OPTIONS,  // "uci" command sent, waiting for "option" and "uciok" response.
    WAIT_READY,    // "isready" sent, waiting for "readyok".
    IDLE,          // engine not searching.
    SEARCH,        // "go" sent, waiting for "bestmove";
    EVAL,          // "eval" sent, waiting for Final evaluation
    PONDER,        // "go" sent, waiting for "bestmove"
    STOP_FOR_BESTMOVE,   // "stop" sent, waiting for "bestmove"
    DEAD,          // engine process has terminated
}
