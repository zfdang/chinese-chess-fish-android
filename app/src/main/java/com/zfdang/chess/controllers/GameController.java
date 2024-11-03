package com.zfdang.chess.controllers;

import android.util.Log;

import com.zfdang.chess.gamelogic.Board;
import com.zfdang.chess.gamelogic.Game;
import com.zfdang.chess.gamelogic.GameStatus;
import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.Piece;
import com.zfdang.chess.gamelogic.Position;
import com.zfdang.chess.gamelogic.PvInfo;
import com.zfdang.chess.gamelogic.Rule;

import org.jetbrains.annotations.NotNull;
import org.petero.droidfish.player.ComputerPlayer;
import org.petero.droidfish.player.EngineListener;
import org.petero.droidfish.player.SearchListener;
import org.petero.droidfish.player.SearchRequest;

import java.util.ArrayList;

public class GameController implements EngineListener, SearchListener {
    public static final int MAX_MOVES = 2048;
    public ComputerPlayer player;
    public Game game = null;
    public Game oldGame = null;
    private int searchId;
    public boolean isComputerPlaying = true;
    public boolean isAutoPlay = true;
    private String engineName = "pikafish";

    private GameControllerListener gui = null;
    public GameController(GameControllerListener cListener) {
        gui = cListener;

        isComputerPlaying = true;
        isAutoPlay = true;
        searchId = 0;
    }

    public void newGame() {
        game = new Game();

        // Initialize computer player
        if(player == null) {
            player = new ComputerPlayer(this, this);
        }
        player.queueStartEngine(searchId++,engineName);
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
        String fen = game.currentBoard.toFENString();

        player.sendToEngine("position fen rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1 moves h2e2 h9g7 h0g2 g6g5");
        player.sendToEngine("go depth 5");
    }

    public void playerBack() {

    }

    public void playerForward() {
        long now = System.currentTimeMillis();

        SearchRequest sr = SearchRequest.searchRequest(
                searchId,
                game.history.get(0).board,
                game.getMoveList(),
                new Board(game.currentBoard),
                null,
                false,
                engineName,
                3);
        player.queueAnalyzeRequest(sr);
    }

    public void touchPosition(@NotNull Position pos) {
        if(game.startPos == null) {
            // start position is empty
            if(Piece.isValid(game.currentBoard.getPieceByPosition(pos))){
                // and the piece is valid
                game.setStartPos(pos);
                gui.onGameEvent(GameStatus.SELECT);
            }
        } else {
            // startPos is not empty
            if(game.startPos.equals(pos)) {
                // click the same position, unselect
                game.clearStartPos();
                gui.onGameEvent(GameStatus.SELECT);
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
                    gui.onGameEvent(GameStatus.CHECKMATE, "将死！");
                } else if(status == GameStatus.CHECK) {
                    gui.onGameEvent(GameStatus.CHECK, "将军！");
                } else {
                    gui.onGameEvent(GameStatus.MOVE, game.getLastMoveDesc());
                }
            } else {
                gui.onGameEvent(GameStatus.ILLEGAL);
            }
        }

    }
    public void moveNow() {
        // send "stop" to engine for "bestmove"
        player.stopSearch();
    }

    public void playerMovePiece(String bestmove) {
        Move move = new Move(game.currentBoard);
        boolean result = move.fromUCCIString(bestmove);
        if(result) {
            Log.d("GameController", "Player move: " + move.getChineseStyleString());

            game.startPos = move.fromPosition;
            game.endPos = move.toPosition;
            game.movePiece();

            GameStatus status = game.updateMoveStatus();
            if(status == GameStatus.CHECKMATE) {
                gui.onGameEvent(GameStatus.CHECKMATE);
            } else if(status == GameStatus.CHECK) {
                gui.onGameEvent(GameStatus.CHECK);
            } else {
                gui.onGameEvent(GameStatus.MOVE);
            }
        }
    }


    @Override
    public void reportEngineError(String errMsg) {
        Log.d("GameController", "Engine error: " + errMsg);
    }

    @Override
    public void notifyEngineName(String engineName) {
        Log.d("GameController", "Engine name: " + engineName);
    }

    @Override
    public void notifyDepth(int id, int depth) {
        Log.d("GameController", "Depth: " + depth);
    }

    @Override
    public void notifyCurrMove(int id, Board board, Move m, int moveNr) {
        Log.d("GameController", "Current move: " + m.getUCCIString());
    }

    @Override
    public void notifyPV(int id, Board board, ArrayList<PvInfo> pvInfo, Move ponderMove) {
        // show infos about all pvInfos
        for(PvInfo pv : pvInfo) {
            Log.d("GameController", "PV: " + pv);
        }
    }

    @Override
    public void notifyStats(int id, long nodes, int nps, long tbHits, int hash, int time, int seldepth) {
        Log.d("GameController", "Stats: nodes=" + nodes + ", nps=" + nps + ", tbHits=" + tbHits + ", hash=" + hash + ", time=" + time + ", seldepth=" + seldepth);
    }

    @Override
    public void notifyBookInfo(int id, String bookInfo, ArrayList<Move> moveList, String eco, int distToEcoTree) {
        Log.d("GameController", "Book info: bookInfo=" + bookInfo + ", eco=" + eco + ", distToEcoTree=" + distToEcoTree);
    }

    @Override
    public void notifySearchResult(int searchId, String bestMove, String nextPonderMove) {
        Log.d("GameController", "Search result: bestMove=" + bestMove + ", nextPonderMove=" + nextPonderMove);
        gui.runOnUIThread(() -> playerMovePiece(bestMove));
    }

    @Override
    public void notifyEngineInitialized() {
        Log.d("GameController", "Engine initialized");
    }

}
