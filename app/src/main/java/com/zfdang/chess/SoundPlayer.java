package com.zfdang.chess;


import android.content.Context;
import android.media.MediaPlayer;

import com.zfdang.chess.controllers.GameController;

public class SoundPlayer {
    private final GameController controller;
    private MediaPlayer selectSound;
    private MediaPlayer moveSound;
    private MediaPlayer captureSound;
    private MediaPlayer checkSound;
    private MediaPlayer invalidSound;
    private MediaPlayer checkmateSound;
    private MediaPlayer readySound;

    public SoundPlayer(Context context, GameController gameController) {
        selectSound = MediaPlayer.create(context, R.raw.select);
        moveSound = MediaPlayer.create(context, R.raw.move);
        captureSound = MediaPlayer.create(context, R.raw.capture);
        checkSound = MediaPlayer.create(context, R.raw.check);
        invalidSound = MediaPlayer.create(context, R.raw.invalid);
        checkmateSound = MediaPlayer.create(context, R.raw.checkmate);
        readySound = MediaPlayer.create(context, R.raw.ready);

        controller = gameController;
    }

    public void select() {
        if (controller != null && controller.settings.getSound_effect()) {
            selectSound.start();
        }
    }

    public void move() {
        if (controller != null && controller.settings.getSound_effect()) {
            moveSound.start();
        }
    }

    public void capture() {
        if (controller != null && controller.settings.getSound_effect()) {
            captureSound.start();
        }
    }

    public void check() {
        if (controller != null && controller.settings.getSound_effect()) {
            checkSound.start();
        }
    }

    public void illegal() {
        if (controller != null && controller.settings.getSound_effect()) {
            invalidSound.start();
        }
    }

    public void checkmate() {
        if (controller != null && controller.settings.getSound_effect()) {
            checkmateSound.start();
        }
    }

    public void ready() {
        if (controller != null && controller.settings.getSound_effect()) {
            readySound.start();
        }
    }

    public void release() {
        selectSound.release();
        moveSound.release();
        captureSound.release();
        checkSound.release();
        invalidSound.release();
        checkmateSound.release();
        readySound.release();
    }
}
