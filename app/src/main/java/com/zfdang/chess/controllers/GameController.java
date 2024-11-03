package com.zfdang.chess.controllers;

import android.util.Log;

import com.zfdang.chess.gamelogic.Game;
import com.zfdang.chess.gamelogic.GameStatus;
import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.Piece;
import com.zfdang.chess.gamelogic.Position;
import com.zfdang.chess.gamelogic.PvInfo;
import com.zfdang.chess.gamelogic.Rule;

import org.jetbrains.annotations.NotNull;
import org.petero.droidfish.engine.EngineOptions;
import org.petero.droidfish.player.ComputerPlayer;
import org.petero.droidfish.player.EngineListener;
import org.petero.droidfish.player.SearchListener;

import java.util.ArrayList;

public class GameController implements EngineListener, SearchListener {
    public static final int MAX_MOVES = 2048;
    public ComputerPlayer player;
    public Game game = null;
    public Game oldGame = null;
    private int requestId;
    public boolean isComputerPlaying = true;
    public boolean isAutoPlay = true;

    private GameControllerListener gameControllerListener = null;
    public GameController(GameControllerListener cListener) {
        gameControllerListener = cListener;

        isComputerPlaying = true;
        isAutoPlay = true;
        requestId = 0;
    }

    public void newGame() {
        game = new Game();

        // Initialize computer player
        if(player == null) {
            player = new ComputerPlayer(this, this);
            player.setEngineOptions(new EngineOptions());
        }
        player.queueStartEngine(requestId++,"pikafish");
        player.uciNewGame();
    }

    public void togglePlayer() {
        isComputerPlaying = !isComputerPlaying;
    }

    public void toggleAutoPlay() {
        isAutoPlay = !isAutoPlay;
    }
    public void nextMove(){
        // 判断当前move是否合法
        game.movePiece();

        // 如果合法的话，就发送给引擎
        String fen = game.currentBoard.convertToFEN();

        player.sendToEngine("position fen rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1 moves h2e2 h9g7 h0g2 g6g5");
        player.sendToEngine("go depth 5");
    }

    public void playerBack() {

    }

    public void playerForward() {

    }

    public void touchPosition(@NotNull Position pos) {
        if(game.startPos == null) {
            // start position is empty
            if(Piece.isValid(game.currentBoard.getPieceByPosition(pos))){
                // and the piece is valid
                game.setStartPos(pos);
                gameControllerListener.onGameEvent(GameStatus.SELECT);
            }
        } else {
            // startPos is not empty
            if(game.startPos.equals(pos)) {
                // click the same position, unselect
                game.clearStartPos();
                gameControllerListener.onGameEvent(GameStatus.SELECT);
                return;
            }

            // 判断pos是否是合法的move的终点
            Move tempMove = new Move(game.startPos, pos, game.currentBoard);
            boolean valid = Rule.isValidMove(tempMove, game.currentBoard);
            if(valid) {
                int piece = game.currentBoard.getPieceByPosition(pos);
                game.setEndPos(pos);
                game.movePiece();

                GameStatus status = game.updateMoveStatus();
                if(status == GameStatus.CHECKMATE) {
                    gameControllerListener.onGameEvent(GameStatus.CHECKMATE, "将死！");
                } else if(status == GameStatus.CHECK) {
                    gameControllerListener.onGameEvent(GameStatus.CHECK, "将军！");
                } else {
                    gameControllerListener.onGameEvent(GameStatus.MOVE, game.getLastMoveDesc());
                }
            } else {
                gameControllerListener.onGameEvent(GameStatus.ILLEGAL);
            }
        }

    }

    @Override
    public void reportEngineError(String errMsg) {

    }

    @Override
    public void notifyEngineName(String engineName) {
        Log.d("GameController", "Engine name: " + engineName);
    }

    @Override
    public void notifyDepth(int id, int depth) {

    }

    @Override
    public void notifyCurrMove(int id, Position pos, Move m, int moveNr) {

    }

    @Override
    public void notifyPV(int id, Position pos, ArrayList<PvInfo> pvInfo, Move ponderMove) {

    }

    @Override
    public void notifyStats(int id, long nodes, int nps, long tbHits, int hash, int time, int seldepth) {

    }

    @Override
    public void notifyBookInfo(int id, String bookInfo, ArrayList<Move> moveList, String eco, int distToEcoTree) {

    }

    @Override
    public void notifySearchResult(int searchId, String bestMove, String nextPonderMove) {

    }

    @Override
    public void notifyEngineInitialized() {

    }
}
