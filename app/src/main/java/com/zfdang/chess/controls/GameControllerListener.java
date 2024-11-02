package com.zfdang.chess.controls;

public interface GameControllerListener {
    // create enum for various sound
    public enum GameSound {
        SELECT, MOVE, CAPTURE, CHECK, ILLEGAL, WIN
    }

    // create enum for various events
    public enum GameEvent {
        MOVE, CAPTURE, CHECK, CHECKMATE, ILLEGAL
    }

    // create fun to send play sound event
    public void onGameSound(GameSound sound);


    // create fun to send game over event
    public void onGameEvent(GameEvent event, String message);

}
