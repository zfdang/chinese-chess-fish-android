package org.petero.droidfish.player;

public interface EngineListener {
    void reportEngineError(String errMsg);

    void notifyEngineName(String engineName);

    void notifySearchResult(int searchId, String bestMove, String nextPonderMove);

    void notifyEngineInitialized();
}
