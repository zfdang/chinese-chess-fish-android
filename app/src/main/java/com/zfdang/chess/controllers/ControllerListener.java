package com.zfdang.chess.controllers;

import com.zfdang.chess.gamelogic.GameStatus;

public interface ControllerListener {

    // create fun to send game over event
    public void onGameEvent(GameStatus event, String message);

    public void onGameEvent(GameStatus event);

    /** Run code on the GUI thread. */
    // https://stackoverflow.com/questions/5161951/android-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-vi
    // Android "Only the original thread that created a view hierarchy can touch its views."
    void runOnUIThread(Runnable runnable);
}
