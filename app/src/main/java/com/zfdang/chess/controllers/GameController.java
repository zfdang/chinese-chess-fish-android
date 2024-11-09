package com.zfdang.chess.controllers;

import android.util.Log;

import com.zfdang.chess.ChessApp;
import com.zfdang.chess.gamelogic.Board;
import com.zfdang.chess.gamelogic.Game;
import com.zfdang.chess.gamelogic.GameStatus;
import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.Piece;
import com.zfdang.chess.gamelogic.Position;
import com.zfdang.chess.gamelogic.PvInfo;
import com.zfdang.chess.gamelogic.Rule;
import com.zfdang.chess.openbook.BHOpenBook;
import com.zfdang.chess.openbook.BookData;
import com.zfdang.chess.openbook.OpenBook;
import com.zfdang.chess.openbook.OpenBookManager;

import org.jetbrains.annotations.NotNull;
import org.petero.droidfish.player.ComputerPlayer;
import org.petero.droidfish.player.EngineListener;
import org.petero.droidfish.player.SearchListener;
import org.petero.droidfish.player.SearchRequest;

import java.util.ArrayList;
import java.util.List;

public class GameController implements EngineListener, SearchListener {
    public ComputerPlayer player = null;
    private String engineName = "pikafish";
    public static final int MAX_MOVES = 2048;

    public Game game = null;
    public Game oldGame = null;
    private int searchId;
    private long searchStartTime;
    private long searchEndTime;

    public boolean isComputerPlaying = true;
    public boolean isAutoPlay = true;

    private GameControllerListener gui = null;
    ArrayList<PvInfo> multiPVs = new ArrayList<>();

    OpenBookManager bookManager = null;
    BHOpenBook bhBook = null;

    public ControllerState state;

    // create enum for controller state
    enum ControllerState {
        IDLE,
        WAITING_FOR_USER, // 红方出子
        WAITING_FOR_USER_MULTIPV, // 红方寻求帮助，等待多PV结果
        WAITING_FOR_ENGINE, // 黑方出子
        WAITING_FOR_ENGINE_BESTMV, // 黑方寻找最佳着法
        WAITING_FOR_ENGINE_MULTIPV // 黑方变着，等待多PV结果
    }

    public GameController(GameControllerListener cListener) {
        gui = cListener;

        isComputerPlaying = true;
        isAutoPlay = true;
        searchId = 0;

        bhBook = new BHOpenBook(ChessApp.getContext());

        // Initialize computer player
        if(player == null) {
            player = new ComputerPlayer(this, this);
        }
        player.queueStartEngine(searchId++, engineName);
        newGame();
    }

    public synchronized void newGame() {
        Log.d("GameController", "New game");
        state = ControllerState.WAITING_FOR_USER;
        game = new Game();
        player.uciNewGame();
    }

    public synchronized void toggleComputer() {
        isComputerPlaying = !isComputerPlaying;
    }

    public synchronized void toggleComputerAutoPlay() {
        isAutoPlay = !isAutoPlay;
    }

    public boolean isRedTurn(){
        return state == ControllerState.WAITING_FOR_USER;
    }

    public boolean isBlackTurn(){
        return state == ControllerState.WAITING_FOR_ENGINE;
    }

    public synchronized void setSatate(boolean isRedTurn){
        if(isRedTurn){
            state = ControllerState.WAITING_FOR_USER;
        } else {
            state = ControllerState.WAITING_FOR_ENGINE;
        }
    }

    // this can be called by either GUI or computer
    public synchronized boolean stepBack() {
        // 只在WAINTING_FOR_USER或WAINTING_FOR_ENGINE状态下才能悔棋
        if(state != ControllerState.WAITING_FOR_USER && state != ControllerState.WAITING_FOR_ENGINE) {
            gui.onGameEvent(GameStatus.ILLEGAL, "搜索中，请稍候...");
            return false;
        }
        if(game.history.size() > 0) {
            Game.HistoryRecord record = game.undoMove();
            setSatate(record.isRedMove);
            gui.onGameEvent(GameStatus.MOVE, game.getLastMoveDesc());
            return true;
        } else {
            gui.onGameEvent(GameStatus.ILLEGAL, "无棋可悔");
            return false;
        }
    }

    // computer to play his turn
    public synchronized void computerForward() {
        if(state == ControllerState.WAITING_FOR_USER) {
            // play only plays the black
            gui.onGameEvent(GameStatus.ILLEGAL, "该红方出子");
            return;
        }
        if(state != ControllerState.WAITING_FOR_ENGINE) {
            // play only plays the black
            gui.onGameEvent(GameStatus.ILLEGAL, "搜索中，请稍候...");
            return;
        }

        gui.onGameEvent(GameStatus.SELECT, "检索开局库...");
        // search openbook first
        long vkey = game.currentBoard.getZobrist(isRedTurn());
        List<BookData> bookData = bhBook.query(vkey, isRedTurn(), OpenBook.SortRule.BEST_SCORE);
        // iterate bookData one by one
        for (BookData bd : bookData) {
            Log.d("GameController", "Openbook hit: " + bd.getMove());
        }
        if(bookData!= null && bookData.size() > 0){
            Log.d("GameController", "Openbook hit: " + bookData.get(0).getMove());
            computerMovePiece(bookData.get(0).getMove());
            return;
        }

        gui.onGameEvent(GameStatus.SELECT, "电脑搜索着法中...");
        state = ControllerState.WAITING_FOR_ENGINE_BESTMV;

        // trigger searchrequest, engine will call notifySearchResult for bestmove
        searchStartTime = System.currentTimeMillis();
        Board board = null;
        if (game.history.size() == 0) {
            board = game.currentBoard;
        } else {
            board = game.history.get(0).move.board;
        }
        SearchRequest sr = SearchRequest.searchRequest(
                searchId++,
                board,
                game.getMoveList(),
                new Board(game.currentBoard),
                null,
                false,
                engineName,
                3);
        player.queueSearchRequest(sr);
    }

    public synchronized void computerAskForMultiPV() {
        if(state == ControllerState.WAITING_FOR_ENGINE_MULTIPV) {
            gui.onGameEvent(GameStatus.ILLEGAL, "电脑搜索变着中,请稍候...");
            return;
        }
        if(state == ControllerState.WAITING_FOR_USER_MULTIPV) {
            gui.onGameEvent(GameStatus.ILLEGAL, "用户寻求帮助中,请稍候...");
            return;
        }
        if(state == ControllerState.WAITING_FOR_ENGINE_BESTMV) {
            // play only plays the black
            gui.onGameEvent(GameStatus.ILLEGAL, "电脑自动出着后方可变着,请稍候...");
            return;
        }

        // 判断当前的状态，如果是红方出着，那么悔棋一步；如果无法悔棋，则无法变着
        if(state == ControllerState.WAITING_FOR_USER) {
            // undo
            boolean result = stepBack();
            if(!result) {
                gui.onGameEvent(GameStatus.ILLEGAL, "该红方出着，无法变着");
                return;
            } else {
                toggleTurn();
            }
        }

        // 最后检查目前的状态是否是等待引擎出着
        if(state != ControllerState.WAITING_FOR_ENGINE) {
            Log.d("GameController", "Invalid state, computerAskForMultiPV should not be called now." + state);
            gui.onGameEvent(GameStatus.ILLEGAL, "黑方出招时方可变着");
            return;
        }

        // 开始搜索中
        gui.onGameEvent(GameStatus.SELECT, "电脑搜索变着中...");
        state = ControllerState.WAITING_FOR_ENGINE_MULTIPV;

        // trigger searchrequest, engine will call notifySearchResult for bestmove
        searchStartTime = System.currentTimeMillis();
        Board board = null;
        if(game.history.size() == 0) {
            board = game.currentBoard;
        } else {
            board = game.history.get(0).move.board;
        }
        SearchRequest sr = SearchRequest.searchRequest(
                searchId++,
                board,
                game.getMoveList(),
                new Board(game.currentBoard),
                null,
                false,
                engineName,
                3);
        player.queueSearchRequest(sr);
    }


    public synchronized void playerAskForHelp() {
        if(state == ControllerState.WAITING_FOR_USER_MULTIPV) {
            // play only plays the black
            gui.onGameEvent(GameStatus.ILLEGAL, "正在搜索着法，请稍候...");
            return;
        }
        if(state != ControllerState.WAITING_FOR_USER) {
            // play only plays the black
            gui.onGameEvent(GameStatus.ILLEGAL, "己方出招时方可寻求帮助");
            return;
        }

        state = ControllerState.WAITING_FOR_USER_MULTIPV;

        // trigger searchrequest, engine will call notifySearchResult for bestmove
        searchStartTime = System.currentTimeMillis();
        Board board = null;
        if(game.history.size() == 0) {
            board = game.currentBoard;
        } else {
            board = game.history.get(0).move.board;
        }
        SearchRequest sr = SearchRequest.searchRequest(
                searchId++,
                board,
                game.getMoveList(),
                new Board(game.currentBoard),
                null,
                false,
                engineName,
                3);
        player.queueSearchRequest(sr);
    }

    public void touchPosition(@NotNull Position pos) {
        if(game.startPos == null) {
            // start position is empty
            if(Piece.isValid(game.currentBoard.getPieceByPosition(pos))){
                // and the piece is valid
                game.setStartPos(pos);
                gui.onGameEvent(GameStatus.SELECT, "选择棋子");
            }
        } else {
            // startPos is not empty
            if(game.startPos.equals(pos)) {
                // click the same position, unselect
                game.clearStartPos();
                gui.onGameEvent(GameStatus.SELECT, "取消选择棋子");
                return;
            }

            // 判断pos是否是合法的move的终点
            Move tempMove = new Move(game.startPos, pos, game.currentBoard);
            boolean valid = Rule.isValidMove(tempMove, game.currentBoard);
            if(valid) {
                game.setEndPos(pos);

                // 非走棋状态，不允许走棋
                if(state != ControllerState.WAITING_FOR_USER && state != ControllerState.WAITING_FOR_ENGINE) {
                    Log.d("GameController", "非走棋状态，请稍候");
                    gui.onGameEvent(GameStatus.ILLEGAL, "非走棋状态，请稍候...");

                    // reset start/end position
                    game.startPos = null;
                    game.endPos = null;
                    return;
                }

                int piece = game.currentBoard.getPieceByPosition(game.startPos);
                // 确保棋子颜色和当前状态匹配
                if(state == ControllerState.WAITING_FOR_USER && !Piece.isRed(piece)) {
                    gui.onGameEvent(GameStatus.ILLEGAL, "该红方出着");
                    game.startPos = null;
                    game.endPos = null;
                    return;
                } else if(state == ControllerState.WAITING_FOR_ENGINE && Piece.isRed(piece)) {
                    gui.onGameEvent(GameStatus.ILLEGAL, "该黑方出着");
                    game.startPos = null;
                    game.endPos = null;
                    return;
                }

                doMoveAndUpdateStatus();

                if(isAutoPlay && isComputerPlaying && isBlackTurn()) {
                    computerForward();
                }
            } else {
                gui.onGameEvent(GameStatus.ILLEGAL);
            }
        }

    }
    public void moveNow() {
        // send "stop" to engine for "bestmove"
        if(state == ControllerState.WAITING_FOR_ENGINE_BESTMV) {
            player.stopSearch();
            gui.onGameEvent(GameStatus.SELECT, "已命令电脑立刻出着...");
        } else {
            gui.onGameEvent(GameStatus.ILLEGAL, "非搜索着法状态");
        }
    }

    public void computerMovePiece(String bestmove) {
        // validate the move
        Move move = new Move(game.currentBoard);
        boolean result = move.fromUCCIString(bestmove);

        if(result) {
            Log.d("GameController", "computer move: " + move.getChsString());

            game.setStartPos(move.fromPosition);
            game.setEndPos(move.toPosition);
            doMoveAndUpdateStatus();
        }
    }

    public void processMultiPVInfos(){
        // show multiPV infos
        for(PvInfo pv : multiPVs) {
            Log.d("GameController", "PV: " + pv);
        }
        game.generateSuggestedMoves(multiPVs);

        // notify GUI
        gui.onGameEvent(GameStatus.MULTIPV, "选择编号或直接移动棋子：");
    }

    public void selectMultiPV(int index) {
        Move move = game.getSuggestedMove(index);
        game.clearSuggestedMoves();

        if(move != null) {
            game.startPos = move.fromPosition;
            game.endPos = move.toPosition;
            doMoveAndUpdateStatus();

            if(isAutoPlay && isComputerPlaying && isBlackTurn()) {
                computerForward();
            }
        }
    }


    public void doMoveAndUpdateStatus(){
        // game.startPos & game.endPos should be ready

        if(state != ControllerState.WAITING_FOR_USER && state != ControllerState.WAITING_FOR_ENGINE) {
            Log.e("GameController", "Invalid state, doMoveAndUpdateStatus should not be called now." + state);
            return;
        }

        // check piece color to move
        int piece = game.currentBoard.getPieceByPosition(game.startPos);
        if(Piece.isRed(piece) != isRedTurn()) {
            Log.e("GameController", "Invalid move, piece color is not match");
            if(isRedTurn()) {
                gui.onGameEvent(GameStatus.ILLEGAL, "该红方出着");
            } else {
                gui.onGameEvent(GameStatus.ILLEGAL, "该黑方出着");
            }
            // reset start/end position
            game.startPos = null;
            game.endPos = null;
            return;
        }

        game.movePiece();

        // update controller status
        toggleTurn();

        // update game status after the move
        GameStatus status = game.updateGameStatus();

        // clear multipv status
        game.clearSuggestedMoves();

        // send notification to GUI
        if(status == GameStatus.CHECKMATE) {
            gui.onGameEvent(GameStatus.CHECKMATE, "将死！");
        } else if(status == GameStatus.CHECK) {
            gui.onGameEvent(GameStatus.CHECK, "将军！");
        } else {
            gui.onGameEvent(GameStatus.MOVE, game.getLastMoveDesc());
        }

    }

    private void toggleTurn() {
        if(state == ControllerState.WAITING_FOR_USER) {
            state = ControllerState.WAITING_FOR_ENGINE;
        } else if(state == ControllerState.WAITING_FOR_ENGINE) {
            state = ControllerState.WAITING_FOR_USER;
        } else {
            Log.e("GameController", "Invalid state, toggleTurn should not be called now." + state);
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
    public void notifyPV(int id, Board board, ArrayList<PvInfo> pvInfos, Move ponderMove) {
        // show infos about all pvInfos
        multiPVs.clear();
        for(PvInfo pv : pvInfos) {
//            Log.d("GameController", "PV: " + pv);
            multiPVs.add(pv);
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
        searchEndTime = System.currentTimeMillis();

        // engine返回bestmove, 有两种情况，一种是电脑搜索的结果，一种是红方寻求帮助的结果
        if(state == ControllerState.WAITING_FOR_USER_MULTIPV){
            // 红方寻求帮助
            // 把multiPV的结果显示在界面上，让用户选择
            state = ControllerState.WAITING_FOR_USER;
            gui.runOnUIThread(() -> processMultiPVInfos());

        } else if(state == ControllerState.WAITING_FOR_ENGINE_MULTIPV) {
            // 电脑被强制要求变着
            // 把multiPV的结果显示在界面上，让用户选择
            state = ControllerState.WAITING_FOR_ENGINE;
            gui.runOnUIThread(() -> processMultiPVInfos());
        } else if(state == ControllerState.WAITING_FOR_ENGINE_BESTMV) {
            // 电脑发起的请求，走下一步棋子
            state = ControllerState.WAITING_FOR_ENGINE;
            gui.runOnUIThread(() -> computerMovePiece(bestMove));
        }
    }

    @Override
    public void notifyEngineInitialized() {
        Log.d("GameController", "Engine initialized");
    }

    public void swapSides() {
        // search openbook first
        long vkey = game.currentBoard.getZobrist(isRedTurn());
        List<BookData> bookData = bhBook.query(vkey, isRedTurn(), OpenBook.SortRule.BEST_SCORE);
        // iterate bookData one by one
        for (BookData bd : bookData) {
            Log.d("GameController", "Openbook hit: " + bd.getMove());
        }
    }
}
