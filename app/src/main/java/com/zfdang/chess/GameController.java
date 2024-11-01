package com.zfdang.chess;

import com.zfdang.chess.gamelogic.Game;
import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.Position;
import com.zfdang.chess.gamelogic.PvInfo;

import org.petero.droidfish.engine.EngineOptions;
import org.petero.droidfish.player.ComputerPlayer;
import org.petero.droidfish.player.EngineListener;
import org.petero.droidfish.player.SearchListener;

import java.util.ArrayList;

public class GameController{
    public static final int MAX_MOVES = 2048;
    public ComputerPlayer computerPlayer;
    public Game game = null;

    private EngineListener engineListener = null;
    private SearchListener searchListener = null;
    public GameController(EngineListener eListener, SearchListener sListener) {
        engineListener = eListener;
        searchListener = sListener;

        // Initialize computer player
        if(computerPlayer == null) {
            computerPlayer = new ComputerPlayer(engineListener, searchListener);
            computerPlayer.setEngineOptions(new EngineOptions());
            computerPlayer.queueStartEngine(1024,"pikafish");
        }
    }


    public void newGame() {
        computerPlayer.uciNewGame();
    }

    public void nextMove(){
        computerPlayer.sendToEngine("position fen rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1 moves h2e2 h9g7 h0g2 g6g5");
        computerPlayer.sendToEngine("go depth 5");
    }

}
