package com.zfdang.chess.controllers;

import android.content.Context;
import android.util.Log;

import com.zfdang.chess.ChessApp;
import com.zfdang.chess.manuals.XQFManual;
import com.zfdang.chess.manuals.XQFParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ManualController extends GameController{
    public XQFManual manual = null;
    public XQFManual.MoveNode moveNode = null;

    private Context context = null;

    public ManualController(ControllerListener listener) {
        super(listener);

        this.context = ChessApp.getContext();
    }

    public boolean loadManualFromFile(String filename) {
        if(filename.toLowerCase().endsWith(".xqf")) {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(new File(filename));
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                inputStream.close();

                // use XQFGame to parse the buffer
                XQFManual xqfManual = XQFParser.parse(buffer);
                manual = xqfManual;
                moveNode = manual.getHeadMove();

                // reset game
                game.currentBoard = xqfManual.board;
                game.history.clear();
                game.suggestedMoves.clear();
                game.startPos = null;
                game.endPos = null;

                return true;
            } catch (IOException e) {
                Log.e("ManualController", "Failed to load manual from file: " + filename);
                Log.e("ManualController", e.getMessage());
            }

            return false;
        }

        return false;
    }

    public void manualForward() {

    }

    public void manualBack() {

    }

    public void manualFirst() {

    }

    public void selectBranch(int i) {

    }
}
