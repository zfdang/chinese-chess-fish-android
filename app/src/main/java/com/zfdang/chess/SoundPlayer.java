package com.zfdang.chess;


import android.content.Context;
import android.media.MediaPlayer;

public class SoundPlayer {
    private MediaPlayer selectSound;
    private MediaPlayer moveSound;
    private MediaPlayer captureSound;
    private MediaPlayer checkSound;
    private MediaPlayer invalidSound;
    private MediaPlayer checkmateSound;
    private MediaPlayer readySound;

    public SoundPlayer(Context context) {
        selectSound = MediaPlayer.create(context, R.raw.select);
        moveSound = MediaPlayer.create(context, R.raw.move);
        captureSound = MediaPlayer.create(context, R.raw.capture);
        checkSound = MediaPlayer.create(context, R.raw.check);
        invalidSound = MediaPlayer.create(context, R.raw.invalid);
        checkmateSound = MediaPlayer.create(context, R.raw.checkmate);
        readySound = MediaPlayer.create(context, R.raw.ready);
    }

    public void select() {
        selectSound.start();
    }

    public void move() {
        moveSound.start();
    }

    public void capture() {
        captureSound.start();
    }

    public void check() {
        checkSound.start();
    }

    public void illegal() {
        invalidSound.start();
    }

    public void checkmate() {
        checkmateSound.start();
    }

    public void ready() {
        readySound.start();
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
