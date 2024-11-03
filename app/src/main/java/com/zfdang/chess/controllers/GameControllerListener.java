package com.zfdang.chess.controllers;

import com.zfdang.chess.gamelogic.GameStatus;

public interface GameControllerListener {

    // create fun to send game over event
    public void onGameEvent(GameStatus event, String message);

    public void onGameEvent(GameStatus event);
}
