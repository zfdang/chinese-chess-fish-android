package com.zfdang.chess.controllers;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zfdang.chess.ChessApp;
import com.zfdang.chess.Settings;
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

import org.jetbrains.annotations.NotNull;
import org.petero.droidfish.player.ComputerPlayer;
import org.petero.droidfish.player.EngineListener;
import org.petero.droidfish.player.SearchListener;
import org.petero.droidfish.player.SearchRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameController implements EngineListener, SearchListener {
    public ComputerPlayer player = null;
    private String engineName = "pikafish";
    public String engineInfo; // returned by engine

    // 如果开启了电脑的着法随机选项，那么前N步会随机选择一个着法。这个数值不易过大，否则电脑棋力下降太多
    private final int random_before_max_rounds = 12;

    public Game game = null;
    private int searchId;
    private long searchStartTime;
    private long searchEndTime;

    public boolean isComputerPlaying = true;
    public boolean isAutoPlay = true;

    public boolean isShowTrends = false;

    private ControllerListener gui = null;
    ArrayList<PvInfo> multiPVs = new ArrayList<>();

    BHOpenBook bhBook = null;

    public ControllerState state;
    public ControllerState preEvalState;

    public Settings settings = null;

    public void toggleShowTrends() {
        isShowTrends = !isShowTrends;
    }

    // create enum for controller state
    enum ControllerState {
        IDLE,
        WAITING_FOR_USER, // 红方出子
        WAITING_FOR_USER_MULTIPV, // 红方寻求帮助，等待多PV结果
        WAITING_FOR_ENGINE, // 黑方出子
        WAITING_FOR_ENGINE_BESTMV, // 黑方寻找最佳着法
        WAITING_FOR_ENGINE_MULTIPV, // 黑方变着，等待多PV结果
        WAITING_FOR_EVAL, // 等待eval的结果
        MANUAL_MODE, // 打谱模式
    }

    public boolean isNonGameMode() {
        return state == ControllerState.MANUAL_MODE;
    }
    public GameController(ControllerListener cListener) {
        gui = cListener;

        isComputerPlaying = true;
        isAutoPlay = true;
        searchId = 0;

        bhBook = new BHOpenBook(ChessApp.getContext());

        settings = new Settings(ChessApp.getContext());

        // Initialize computer player
        if (player == null) {
            player = new ComputerPlayer(this, this, this);
        }
        player.queueStartEngine(searchId++, engineName);
        startNewGame();
    }

    public synchronized void startNewGame() {
        Log.d("GameController", "New game");
        if (settings.getRed_go_first()) {
            state = ControllerState.WAITING_FOR_USER;
        } else {
            state = ControllerState.WAITING_FOR_ENGINE;
        }
        game = new Game(settings.getRed_go_first());
        player.stopSearch();
        player.uciNewGame();
    }

    public synchronized void startFENGame(String fen) {
        Log.d("GameController", "New FEN game" + fen);
        Game tempGame = new Game(true);
        boolean result = tempGame.currentBoard.restoreFromFEN(fen);
        if (!result) {
            Log.e("GameController", "Failed to restore from FEN: " + fen);
            gui.onGameEvent(GameStatus.ILLEGAL, "无效的FEN串");
            return;
        }

        game = tempGame;
        if (game.currentBoard.bRedGo) {
            state = ControllerState.WAITING_FOR_USER;
        } else {
            state = ControllerState.WAITING_FOR_ENGINE;
        }
        game.history.clear();
        game.startPos = null;
        game.endPos = null;
        gui.onGameEvent(GameStatus.UPDATEUI, "从FEN开局");
        player.stopSearch();
        player.uciNewGame();
    }

    public synchronized void toggleComputer() {
        isComputerPlaying = !isComputerPlaying;
    }

    public synchronized void toggleComputerAutoPlay() {
        isAutoPlay = !isAutoPlay;
    }

    public boolean isRedTurn() {
        return state == ControllerState.WAITING_FOR_USER;
    }

    public boolean isBlackTurn() {
        return state == ControllerState.WAITING_FOR_ENGINE;
    }

    public synchronized void setSatate(boolean isRedTurn) {
        if (isRedTurn) {
            state = ControllerState.WAITING_FOR_USER;
        } else {
            state = ControllerState.WAITING_FOR_ENGINE;
        }
    }

    // this can be called by either GUI or computer
    public synchronized boolean stepBack() {
        // 只在WAINTING_FOR_USER或WAINTING_FOR_ENGINE状态下才能悔棋
        if (state != ControllerState.WAITING_FOR_USER && state != ControllerState.WAITING_FOR_ENGINE) {
            gui.onGameEvent(GameStatus.ILLEGAL, "搜索中，请稍候...");
            return false;
        }
        if (game.history.size() > 0) {
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
        if (state == ControllerState.WAITING_FOR_USER) {
            // play only plays the black
            gui.onGameEvent(GameStatus.ILLEGAL, "该红方出子");
            return;
        }
        if (state != ControllerState.WAITING_FOR_ENGINE) {
            // play only plays the black
            gui.onGameEvent(GameStatus.ILLEGAL, "搜索中，请稍候或闪电出着");
            return;
        }

        if(settings.getOpenbook()) {
            // search openbook first
            gui.onGameEvent(GameStatus.UPDATEUI, "检索开局库...");

            long vkey = game.currentBoard.getZobrist(isRedTurn());
            List<BookData> bookData = bhBook.query(vkey, isRedTurn(), OpenBook.SortRule.BEST_SCORE);
            if (bookData != null && bookData.size() > 0) {
                int idx = 0;
                if(game.currentBoard.rounds <= random_before_max_rounds && settings.getRandom_move()) {
                    // 如果在前12步，那么可以随机选择一个着法
                    idx =  (int) (Math.random() * bookData.size());
                }
                Log.d("GameController", "Openbook hit: size = " + bookData.size() + "; bestmove = " + bookData.get(0).getMove() + "; currentMove = " + bookData.get(idx).getMove());
                computerMovePiece(bookData.get(idx).getMove());
                return;
            }
        }

        if (settings.getGo_infinite()) {
            gui.onGameEvent(GameStatus.UPDATEUI, "无限搜索着法中, 须闪电出着!");
        } else {
            gui.onGameEvent(GameStatus.UPDATEUI, "搜索着法中...");
        }
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
                settings.getRandom_move() ? 3 : 1);
        player.queueSearchRequest(sr);
    }

    public synchronized void computerAskForMultiPV() {
        if (state == ControllerState.WAITING_FOR_ENGINE_MULTIPV) {
            gui.onGameEvent(GameStatus.ILLEGAL, "搜索变着中,请稍候或闪电出着");
            return;
        }
        if (state == ControllerState.WAITING_FOR_ENGINE_BESTMV) {
            // play only plays the black
            gui.onGameEvent(GameStatus.ILLEGAL, "电脑正在出着,请稍候或闪电出着");
            return;
        }

        // 判断当前的状态，如果是红方出着，那么悔棋一步；如果无法悔棋，则无法变着
        if (state == ControllerState.WAITING_FOR_USER) {
            // undo
            boolean result = stepBack();
            if (!result) {
                gui.onGameEvent(GameStatus.ILLEGAL, "该红方出着，无法变着");
                return;
            }
        }

        // 最后检查目前的状态是否是等待引擎出着
        if (state != ControllerState.WAITING_FOR_ENGINE) {
            Log.d("GameController", "Invalid state, computerAskForMultiPV should not be called now." + state);
            gui.onGameEvent(GameStatus.ILLEGAL, "黑方出招时方可变着");
            return;
        }

        // 开始搜索中
        if (settings.getGo_infinite()) {
            gui.onGameEvent(GameStatus.UPDATEUI, "无限搜索变着中, 须闪电出着!");
        } else {
            gui.onGameEvent(GameStatus.UPDATEUI, "搜索变着中...");
        }
        state = ControllerState.WAITING_FOR_ENGINE_MULTIPV;
        multiPVs.clear();

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


    public synchronized void playerAskForHelp() {
        if (state == ControllerState.WAITING_FOR_USER_MULTIPV) {
            // play only plays the black
            gui.onGameEvent(GameStatus.ILLEGAL, "正在寻求帮助，请稍候或闪电出着");
            return;
        }
        if (state != ControllerState.WAITING_FOR_USER) {
            // play only plays the black
            gui.onGameEvent(GameStatus.ILLEGAL, "己方出招时方可寻求帮助");
            return;
        }

        // 开始搜索中
        if (settings.getGo_infinite()) {
            gui.onGameEvent(GameStatus.UPDATEUI, "无限寻求帮助中, 须闪电出着!");
        } else {
            gui.onGameEvent(GameStatus.UPDATEUI, "寻求帮助中...");
        }
        state = ControllerState.WAITING_FOR_USER_MULTIPV;
        multiPVs.clear();

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

    public synchronized void evalCurrentBoard() {
        if (!(state == ControllerState.WAITING_FOR_USER || state == ControllerState.WAITING_FOR_ENGINE)) {
            gui.onGameEvent(GameStatus.ILLEGAL, "等待出着时方可评估局面");
            return;
        }

        preEvalState = state;
        state = ControllerState.WAITING_FOR_EVAL;

        // trigger searchrequest, engine will call notifySearchResult for bestmove
        searchStartTime = System.currentTimeMillis();
        Board board = game.currentBoard;
        SearchRequest sr = SearchRequest.evalRequest(
                searchId++,
                board,
                engineName);
        player.queueSearchRequest(sr);
        // result will be returned by notifyEvalResult
    }

    public void touchPosition(@NotNull Position pos) {
        if (game.startPos == null) {
            // start position is empty
            if (Piece.isValid(game.currentBoard.getPieceByPosition(pos))) {
                // and the piece is valid
                game.setStartPos(pos);
                gui.onGameEvent(GameStatus.SELECT, "选择棋子");
            }
        } else {
            // startPos is not empty
            if (game.startPos.equals(pos)) {
                // click the same position, unselect
                game.clearStartPos();
                gui.onGameEvent(GameStatus.SELECT, "取消选择棋子");
                return;
            }

            // 判断pos是否是合法的move的终点
            Move tempMove = new Move(game.startPos, pos, game.currentBoard);
            boolean valid = Rule.isValidMove(tempMove, game.currentBoard);
            if (valid) {
                game.setEndPos(pos);

                // 非走棋状态，不允许走棋
                if (state != ControllerState.WAITING_FOR_USER && state != ControllerState.WAITING_FOR_ENGINE) {
                    Log.d("GameController", "非走棋状态，请稍候");
                    gui.onGameEvent(GameStatus.ILLEGAL, "非走棋状态，请稍候...");

                    // reset start/end position
                    game.startPos = null;
                    game.endPos = null;
                    return;
                }

                int piece = game.currentBoard.getPieceByPosition(game.startPos);
                // 确保棋子颜色和当前状态匹配
                if (state == ControllerState.WAITING_FOR_USER && !Piece.isRed(piece)) {
                    gui.onGameEvent(GameStatus.ILLEGAL, "该红方出着");
                    game.startPos = null;
                    game.endPos = null;
                    return;
                } else if (state == ControllerState.WAITING_FOR_ENGINE && Piece.isRed(piece)) {
                    gui.onGameEvent(GameStatus.ILLEGAL, "该黑方出着");
                    game.startPos = null;
                    game.endPos = null;
                    return;
                }

                doMoveAndUpdateStatus(null);
            } else {
                // clear start position
                game.startPos = null;
                gui.onGameEvent(GameStatus.ILLEGAL, "非法走法");
            }
        }
    }

    public void stopSearchNow() {
        // send "stop" to engine for "bestmove"
        if (state == ControllerState.WAITING_FOR_ENGINE_BESTMV || state == ControllerState.WAITING_FOR_ENGINE_MULTIPV) {
            player.stopSearch();
            gui.onGameEvent(GameStatus.SELECT, "闪电出着中...");
        } else if (state == ControllerState.WAITING_FOR_USER_MULTIPV) {
            player.stopSearch();
            gui.onGameEvent(GameStatus.SELECT, "停止寻求帮助...");
        } else {
            gui.onGameEvent(GameStatus.ILLEGAL, "无搜索任务");
        }
    }

    public void applyEngineSetting() {
        // apply settings to engine
        Log.d("GameController", "Apply engine settings");
        Map<String, String> uciOptions = new HashMap<>();
        uciOptions.put("Hash", String.valueOf(settings.getHash_size()));
        player.setUCIOptions(uciOptions);
    }


    public void computerMovePiece(String bestmove) {
        // validate the move
        Move move = new Move(game.currentBoard);
        boolean result = move.fromUCCIString(bestmove);

        if (result) {
            Log.d("GameController", "computer move: " + move.getChsString());

            game.setStartPos(move.fromPosition);
            game.setEndPos(move.toPosition);
            doMoveAndUpdateStatus(null);
        } else {
            Log.e("GameController", "Invalid move: " + bestmove);
            gui.onGameEvent(GameStatus.LOSE, "无路可走");
        }
    }

    public void processMultiPVInfos(String bestmove) {
        // show multiPV infos
        for (PvInfo pv : multiPVs) {
            Log.d("GameController", "PV: " + pv);
        }
        if (multiPVs.size() == 0) {
            // add bestmove to multiPVs
            ArrayList<Move> moves = new ArrayList<>();
            Move m = new Move(game.currentBoard);
            boolean result = m.fromUCCIString(bestmove);
            if (result) {
                moves.add(m);
                PvInfo pvinfo = new PvInfo(0, 0, 0, 0, 0, 0, 0, 0, false, false, false, moves);
                multiPVs.add(pvinfo);
            } else {
                Log.e("GameController", "Invalid move: " + bestmove);
                gui.onGameEvent(GameStatus.LOSE, "无路可走");
                return;
            }
        }

        game.generateSuggestedMoves(multiPVs);

        // notify GUI
        gui.onGameEvent(GameStatus.MULTIPV, "选择编号或直接移动棋子：");
    }

    public void selectMultiPV(int index) {
        Move move = game.getSuggestedMove(index);
        game.clearSuggestedMoves();
        PvInfo pvinfo = multiPVs.get(index);

        if (move != null) {
            game.startPos = move.fromPosition;
            game.endPos = move.toPosition;
            doMoveAndUpdateStatus(pvinfo);
        }
    }


    public void doMoveAndUpdateStatus(PvInfo pvinfo) {
        // game.startPos & game.endPos should be ready
        if (state != ControllerState.WAITING_FOR_USER && state != ControllerState.WAITING_FOR_ENGINE) {
            Log.e("GameController", "Invalid state, doMoveAndUpdateStatus should not be called now." + state);
            return;
        }

        // check piece color to move
        int piece = game.currentBoard.getPieceByPosition(game.startPos);
        if (Piece.isRed(piece) != isRedTurn()) {
            Log.e("GameController", "Invalid move, piece color is not match");
            if (isRedTurn()) {
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
        if (status == GameStatus.CHECKMATE) {
            gui.onGameEvent(GameStatus.CHECKMATE, "将死！");
        } else if (status == GameStatus.CHECK) {
            gui.onGameEvent(GameStatus.CHECK, "将军！");
        } else {
            if (pvinfo != null) {
                Log.d("GameController", "PV: " + pvinfo);

                // show multiPV infos
                Board b = new Board(game.currentBoard);
                ArrayList<Move> moves = pvinfo.pv;
                String desc = "预测着法：";
                for (int i = 1; i < moves.size() && i <= 4; i++) {
                    Move m = moves.get(i);
                    m.setBoard(b);
                    b.doMove(m);
                    desc += m.getChsString() + " ";
                }
                gui.onGameEvent(GameStatus.MOVE, desc);
            } else {
                gui.onGameEvent(GameStatus.MOVE, game.getLastMoveDesc());
            }
        }

        // start request to evaluate board status
        // result will be returned by notifyEvalResult
        evalCurrentBoard();
    }

    protected void toggleTurn() {
        if (state == ControllerState.WAITING_FOR_USER) {
            state = ControllerState.WAITING_FOR_ENGINE;
        } else if (state == ControllerState.WAITING_FOR_ENGINE) {
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
        this.engineInfo = engineName;
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
        for (PvInfo pv : pvInfos) {
            multiPVs.add(pv);
        }
        Log.d("GameController", "PV: " + pvInfos);
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
        // engine返回bestmove, 有3种情况，一种是电脑搜索走棋的结果，一种是红方寻求帮助的结果，一种是电脑被强制要求变着的结果

        Log.d("GameController", "Search result: bestMove=" + bestMove + ", nextPonderMove=" + nextPonderMove);
        searchEndTime = System.currentTimeMillis();

        if (state == ControllerState.WAITING_FOR_USER_MULTIPV) {
            // 红方寻求帮助
            // 把multiPV的结果显示在界面上，让用户选择
            state = ControllerState.WAITING_FOR_USER;
            gui.runOnUIThread(() -> processMultiPVInfos(bestMove));
        } else if (state == ControllerState.WAITING_FOR_ENGINE_MULTIPV) {
            // 电脑被强制要求变着
            // 把multiPV的结果显示在界面上，让用户选择
            state = ControllerState.WAITING_FOR_ENGINE;
            gui.runOnUIThread(() -> processMultiPVInfos(bestMove));
        } else if (state == ControllerState.WAITING_FOR_ENGINE_BESTMV) {
            // 电脑发起的请求，走下一步棋子
            state = ControllerState.WAITING_FOR_ENGINE;
            // 如果设置了引擎的随机性，则从multiPV中随机选择一个着法。这个只针对前12步有效，后期不让电脑随机选择，否则棋力降低太多
            if (settings.getRandom_move() && multiPVs.size() > 0 && game.currentBoard.rounds <= random_before_max_rounds) {
                int idx = (int) (Math.random() * multiPVs.size());
                String randomMove = multiPVs.get(idx).pv.get(0).getUCCIString();
                gui.runOnUIThread(() -> computerMovePiece(randomMove));
                Log.d("GameController", "Search result: bestMove=" + bestMove + ", randomMove=" + randomMove);
            } else {
                gui.runOnUIThread(() -> computerMovePiece(bestMove));
                Log.d("GameController", "Search result: bestMove=" + bestMove);
            }
        }
    }

    @Override
    public void notifyEvalResult(int searchId, float eval) {
        Log.d("GameController", "Eval result: eval=" + eval);
        game.currentBoard.score = eval;

        gui.runOnUIThread(() -> {
                gui.onGameEvent(GameStatus.UPDATEUI);
        });

        // restore state
        state = preEvalState;

        // auto nextstep for computer if applicable
        if (isAutoPlay && isComputerPlaying && isBlackTurn()) {
            // 因为要显示预测着法，所以让电脑延迟500s走棋
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    computerForward();
                }
            }, 500);
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

    public int getMultiPVSize() {
        return multiPVs.size();
    }

    public void saveGameStatus() {
        // save game status
        try {
            game.saveToFile(ChessApp.getContext());
            Log.d("GameController", "Game status saved");
        } catch (IOException e) {
            Log.e("GameController", "Failed to save game status" + e);
        }
    }

    public void loadGameStatus() {
        // load game status
        try {
            game = Game.loadFromFile(ChessApp.getContext());
            if (game.currentBoard.bRedGo) {
                state = ControllerState.WAITING_FOR_USER;
            } else {
                state = ControllerState.WAITING_FOR_ENGINE;
            }
            Log.d("GameController", "Game status loaded");
        } catch (IOException e) {
            Log.e("GameController", "Failed to load game status: " + e);
        } catch (ClassNotFoundException e) {
            Log.e("GameController", "Failed to load game status: " + e);
        }
    }
}
