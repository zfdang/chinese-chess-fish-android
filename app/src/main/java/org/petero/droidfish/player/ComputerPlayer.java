/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2014  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish.player;

import android.util.Log;

import com.zfdang.chess.Settings;
import com.zfdang.chess.controllers.GameController;
import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.PvInfo;
import com.zfdang.chess.utils.CpuInfo;

import org.petero.droidfish.engine.EngineConfig;
import org.petero.droidfish.engine.UCIEngine;
import org.petero.droidfish.engine.UCIEngineBase;
import org.petero.droidfish.engine.UCIOptions;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * A computer algorithm player.
 */
public class ComputerPlayer {
    private EngineListener engineListener = null;
    private SearchListener searchListener = null;
    private GameController gameController = null;
    private UCIEngine uciEngine = null;

    // engineOption目前为止没有实际作用，没有删除只是因为不想改动太多代码
    private EngineConfig engineConfig = new EngineConfig();

    /**
     * Pending UCI options to send when engine becomes idle.
     */
    private Map<String, String> pendingOptions = new TreeMap<>();

    /**
     * Set when "ucinewgame" needs to be sent.
     */
    private boolean newGame = false;

    /**
     * >1 if multiPV mode is supported.
     * this value will be read from engine options during initialization
     * so we don't have to set it manually
     */
    private int maxPV = 1;

    public int getMaxPV() {
        return maxPV;
    }


    private EngineState engineState = new EngineState();
    private SearchRequest searchRequest = null;
    private Thread engineMonitor;

    // constructor to accept listener
    public ComputerPlayer(EngineListener engineListener, SearchListener searchListener, GameController gameController) {
        this.engineListener = engineListener;
        this.searchListener = searchListener;
        this.gameController = gameController;
    }

    /**
     * Return true if computer player is consuming CPU time.
     */
    public final synchronized boolean computerBusy() {
        switch (engineState.state) {
            case SEARCH:
            case PONDER:
            case STOP_FOR_BESTMOVE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Return true if computer player has been loaded.
     */
    public final synchronized boolean computerLoaded() {
        return (engineState.state != EngineStateValue.READ_OPTIONS) &&
                (engineState.state != EngineStateValue.DEAD);
    }

    public final synchronized UCIOptions getUCIOptions() {
        UCIEngine uci = uciEngine;
        if (uci == null)
            return null;
        UCIOptions opts = uci.getUCIOptions();
        if (opts == null)
            return null;
        try {
            opts = opts.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
        for (Map.Entry<String, String> e : pendingOptions.entrySet()) {
            UCIOptions.OptionBase o = opts.getOption(e.getKey());
            if (o != null)
                o.setFromString(e.getValue());
        }
        Log.d("ComputerPlayer", "getUCIOptions: " + opts.toString());
        return opts;
    }

    /**
     * Send pending UCI option changes to the engine.
     */
    private synchronized boolean applyPendingOptions() {
        Log.d("ComputerPlayer", "applyPendingOptions: " + pendingOptions.toString());
        if (pendingOptions.isEmpty())
            return false;
        boolean modified = false;
        UCIEngine uci = uciEngine;
        if (uci != null)
            modified = uci.setUCIOptions(pendingOptions);
        pendingOptions.clear();
        return modified;
    }

    public synchronized void setUCIOptions(Map<String, String> uciOptions) {
        Log.d("ComputerPlayer", "setUCIOptions: " + uciOptions.toString());
        pendingOptions.putAll(uciOptions);
        boolean modified = true;
        if (engineState.state == EngineStateValue.IDLE)
            modified = applyPendingOptions();
        if (modified) {
            UCIEngine uci = uciEngine;
            if (uci != null)
                uci.saveIniFile(getUCIOptions());
        }
    }

    /**
     * Sends "ucinewgame". Takes effect when next search started.
     */
    public final synchronized void uciNewGame() {
        newGame = true;
    }

    public final synchronized void setOptimizedThreads() {
        // https://github.com/official-pikafish/Pikafish/wiki/Pikafish-FAQ#threads
        int cores = CpuInfo.getCoresCount();
        if (uciEngine != null) {
            uciEngine.setOption("Threads", cores);
            Log.d("ComputerPlayer", "setOptimizedThreads: " + cores);
        }
    }

    /**
     * Sends "ponderhit" command to engine.
     */
    public final synchronized void ponderHit(int id) {
        if ((searchRequest == null) ||
                (searchRequest.ponderMove == null) ||
                (searchRequest.searchId != id))
            return;

        searchRequest.ponderHit();
        if (engineState.state != EngineStateValue.PONDER)
            searchRequest.startTime = System.currentTimeMillis();

        if (engineState.state == EngineStateValue.PONDER) {
            uciEngine.writeLineToEngine("ponderhit");
            engineState.setState(EngineStateValue.SEARCH);
            pvModified = true;
            notifyListener();
        }
    }

    /**
     * Stop the engine process.
     */
    public final synchronized void shutdownEngine() {
        if (uciEngine != null) {
            engineMonitor.interrupt();
            engineMonitor = null;
            uciEngine.shutDown();
            uciEngine = null;
        }
        engineState.setState(EngineStateValue.DEAD);
    }

    /**
     * Start an engine, if not already started.
     * Will shut down old engine first, if needed.
     */
    public final synchronized void queueStartEngine(int id, String engineName) {
        killOldEngine(engineName);
        stopSearch();
        searchRequest = SearchRequest.startRequest(id, engineName);
        handleQueue();
    }

    /**
     * Decide what moves to search. Filters out non-optimal moves if tablebases are used.
     * 原来遗留的函数，现在没用上
     */
    private ArrayList<Move> movesToSearch(SearchRequest sr) {
        ArrayList<Move> moves = sr.searchMoves;
        // process moves if necessary
        return moves;
    }

    /**
     * Start analyzing a board.
     */
    public final synchronized void queueSearchRequest(SearchRequest sr) {
        killOldEngine(sr.engineName);
        stopSearch();

        searchRequest = sr;
        handleQueue();
    }

    private void handleQueue() {
        if (engineState.state == EngineStateValue.DEAD) {
            engineState.engineName = "";
            engineState.setState(EngineStateValue.IDLE);
        }
        if (engineState.state == EngineStateValue.IDLE)
            handleIdleState();
    }

    private void killOldEngine(String engine) {
        boolean needShutDown = !engine.equals(engineState.engineName);
        if (!needShutDown) {
            UCIEngine uci = uciEngine;
            if (uci != null)
                needShutDown = !uci.configOk(engineConfig);
        }
        if (needShutDown)
            shutdownEngine();
    }

    /**
     * Tell engine to stop searching.
     */
    public final synchronized boolean stopSearch() {
        searchRequest = null;
        switch (engineState.state) {
            case SEARCH:
            case PONDER:
                uciEngine.writeLineToEngine("stop");
                engineState.setState(EngineStateValue.STOP_FOR_BESTMOVE);
                return true;
            default:
                return false;
        }
    }

    /**
     * Return true if current search job is equal to id.
     */
    public final synchronized boolean sameSearchId(int id) {
        return (searchRequest != null) && (searchRequest.searchId == id);
    }

    /**
     * Type of search the engine is currently requested to perform.
     */
    public enum SearchType {
        NONE,
        SEARCH,
        PONDER,
        ANALYZE
    }

    /**
     * Return type of search the engine is currently requested to perform.
     */
    public final synchronized SearchType getSearchType() {
        if (searchRequest == null)
            return SearchType.NONE;
        if (searchRequest.isSearch)
            return SearchType.SEARCH;
        if (searchRequest.ponderMove == null)
            return SearchType.SEARCH;
        else
            return SearchType.PONDER;
    }

    /**
     * Determine what to do next when in idle state.
     */
    private void handleIdleState() {
        Log.d("ComputerPlayer", "handleIdleState: ");
        SearchRequest sr = searchRequest;
        if (sr == null)
            return;

        // Make sure correct engine is running
        if ((uciEngine == null) || !engineState.engineName.equals(sr.engineName)) {
            shutdownEngine();
            startEngine();
            return;
        }

        // Send "ucinewgame" if needed
        if (newGame) {
            uciEngine.writeLineToEngine("ucinewgame");
            uciEngine.writeLineToEngine("isready");
            engineState.setState(EngineStateValue.WAIT_READY);
            newGame = false;
            return;
        }

        // Apply pending UCI option changes
        if (applyPendingOptions()) {
            uciEngine.writeLineToEngine("isready");
            engineState.setState(EngineStateValue.WAIT_READY);
            return;
        }

        // Check if only engine start was requested
        boolean isSearch = sr.isSearch;
        if (!isSearch) {
            searchRequest = null;
            return;
        }

        engineState.searchId = searchRequest.searchId;

        // Set strength and MultiPV parameters
        clearInfo();
        if (maxPV > 1) {
            int num = Math.min(maxPV, searchRequest.numPV);
            uciEngine.setOption("MultiPV", num);
        }

        if (isSearch){
            // A value higher than 1 weakens the quality of the best move computed, as resources are used to compute other moves.
//            uciEngine.setOption("MultiPV", 1);

            StringBuilder posStr = new StringBuilder();
            posStr.append("position fen ");
            posStr.append(sr.prevBoard.toFENString());
            Log.d("ComputerPlayer", "handleIdleState: " + sr.currBoard.toFENString());
            int nMoves = sr.mList.size();
            if (nMoves > 0) {
                posStr.append(" moves");
                for (int i = 0; i < nMoves; i++) {
                    posStr.append(" ");
                    posStr.append(sr.mList.get(i).getUCCIString());
                }
            }

            uciEngine.writeLineToEngine(posStr.toString());

            StringBuilder goCmd = new StringBuilder(96);
            // 这里的命令可以根据需要设置
            // https://github.com/official-pikafish/Pikafish/wiki/UCI-&-Commands#standard-commands
            if(gameController!= null && gameController.settings != null) {
                goCmd.append(gameController.settings.getGoCmd());
            } else {
                // default
                goCmd.append("go depth 20");
            }

            // 如果有searchMoves，就加上searchMoves, 这个以后可以从开局库获取
            if (sr.searchMoves != null) {
                goCmd.append(" searchmoves");
                for (Move m : sr.searchMoves) {
                    goCmd.append(' ');
                    goCmd.append(m.getUCCIString());
                }
            }

            uciEngine.writeLineToEngine(goCmd.toString());
            engineState.setState(EngineStateValue.SEARCH);
        }
    }


    private void startEngine() {
        myAssert(uciEngine == null);
        myAssert(engineMonitor == null);
        myAssert(engineState.state == EngineStateValue.DEAD);
        myAssert(searchRequest != null);

        uciEngine = UCIEngineBase.getEngine(searchRequest.engineName, engineConfig, engineListener);
        uciEngine.initialize();

        final UCIEngine uci = uciEngine;
        engineMonitor = new Thread(() -> monitorLoop(uci));
        engineMonitor.start();

        uciEngine.writeLineToEngine("uci");
        engineState.engineName = searchRequest.engineName;
        engineState.setState(EngineStateValue.READ_OPTIONS);
    }

    private final static long guiUpdateInterval = 100;
    private long lastGUIUpdate = 0;

    private void monitorLoop(UCIEngine uci) {
        while (true) {
            int timeout = getReadTimeout();
            if (Thread.currentThread().isInterrupted())
                return;
            String s = uci.readLineFromEngine(timeout);
            long t0 = System.currentTimeMillis();
            while (s != null && !s.isEmpty()) {
                if (Thread.currentThread().isInterrupted())
                    return;
                processEngineOutput(uci, s);
                s = uci.readLineFromEngine(1);
                long t1 = System.currentTimeMillis();
                if (t1 - t0 >= 1000)
                    break;
            }
            if ((s == null) || Thread.currentThread().isInterrupted())
                return;
            processEngineOutput(uci, s);
            if (Thread.currentThread().isInterrupted())
                return;
            notifyListener();
            if (Thread.currentThread().isInterrupted())
                return;
        }
    }

    /**
     * Process one line of data from the engine.
     */
    private synchronized void processEngineOutput(UCIEngine uci, String s) {
        Log.d("ComputerPlayer", "processEngineOutput: " + " state: " + engineState.state + " s: " + s);
        if (Thread.currentThread().isInterrupted())
            return;

        if (s == null) {
            shutdownEngine();
            return;
        }

        if (s.length() == 0)
            return;

//        Log.d("ComputerPlayer", "processEngineOutput: " + " state: " + engineState.state + " s: " + s);
        switch (engineState.state) {
            case READ_OPTIONS: {
                if (readUCIOption(uci, s)) {
                    pendingOptions.clear();
                    uci.initConfig(engineConfig);
                    uci.applyIniFile();
                    setOptimizedThreads();
                    uci.writeLineToEngine("ucinewgame");
                    uci.writeLineToEngine("isready");
                    engineState.setState(EngineStateValue.WAIT_READY);
                    engineListener.notifyEngineInitialized();
                }
                break;
            }
            case WAIT_READY: {
                if ("readyok".equals(s)) {
                    engineState.setState(EngineStateValue.IDLE);
                    handleIdleState();
                }
                break;
            }
            case PONDER:
            case SEARCH: {
                String[] tokens = tokenize(s);
                int nTok = tokens.length;
                if (nTok > 0) {
                    if (tokens[0].equals("info")) {
                        parseInfoCmd(tokens);
                    } else if (tokens[0].equals("bestmove")) {
                        String bestMoveStr = nTok > 1 ? tokens[1] : "";
                        Log.d("ComputerPlayer", "bestmove: " + bestMoveStr);
                        String nextPonderMoveStr = "";
                        if ((nTok >= 4) && (tokens[2].equals("ponder"))) {
                            nextPonderMoveStr = tokens[3];
                            Log.d("ComputerPlayer", "nextPonderMoveStr: " + nextPonderMoveStr);
                        }

                        reportMove(bestMoveStr, nextPonderMoveStr);

                        engineState.setState(EngineStateValue.IDLE);
                        searchRequest = null;
                        handleIdleState();
                    }
                }
                break;
            }
            case STOP_FOR_BESTMOVE: {
                String[] tokens = tokenize(s);
                int nTok = tokens.length;
                if(nTok > 0) {
                    if (tokens[0].equals("bestmove")) {
                        String bestMoveStr = nTok > 1 ? tokens[1] : "";
                        Log.d("ComputerPlayer", "bestmove: " + bestMoveStr);
                        String nextPonderMoveStr = "";
                        if ((nTok >= 4) && (tokens[2].equals("ponder"))) {
                            nextPonderMoveStr = tokens[3];
                            Log.d("ComputerPlayer", "nextPonderMoveStr: " + nextPonderMoveStr);
                        }

                        reportMove(bestMoveStr, nextPonderMoveStr);

                        uci.writeLineToEngine("isready");
                        engineState.setState(EngineStateValue.WAIT_READY);
                    }
                }
                break;
            }
            default:
        }
    }

    /**
     * Handle reading of UCI options. Return true when finished.
     */
    private boolean readUCIOption(UCIEngine uci, String s) {
        String[] tokens = tokenize(s);
        if (tokens[0].equals("uciok"))
            return true;

        if (tokens[0].equals("id")) {
            if (tokens[1].equals("name")) {
                String tempEngineName = "";
                for (int i = 2; i < tokens.length; i++) {
                    if (tempEngineName.length() > 0)
                        tempEngineName += " ";
                    tempEngineName += tokens[i];
                }
                engineListener.notifyEngineName(tempEngineName);
            }
        } else if (tokens[0].equals("option")) {
            UCIOptions.OptionBase o = uci.registerOption(tokens);
            if (o instanceof UCIOptions.SpinOption &&
                    o.name.toLowerCase(Locale.US).equals("multipv"))
                maxPV = Math.max(maxPV, ((UCIOptions.SpinOption) o).maxValue);
        }
        return false;
    }

    private void reportMove(String bestMoveStr, String nextPonderMoveStr) {
        SearchRequest sr = searchRequest;
        engineListener.notifySearchResult(engineState.searchId, bestMoveStr, nextPonderMoveStr);
    }

    /**
     * Convert a string to tokens by splitting at whitespace characters.
     */
    private String[] tokenize(String cmdLine) {
        cmdLine = cmdLine.trim();
        return cmdLine.split("\\s+");
    }

    private int statCurrDepth = 0;
    private int statPVDepth = 0;
    private int statScore = 0;
    private boolean statIsMate = false;
    private boolean statUpperBound = false;
    private boolean statLowerBound = false;
    private int statTime = 0;
    private long statNodes = 0;
    private long statTBHits = 0;
    private int statHash = 0;
    private int statSelDepth = 0;
    private int statNps = 0;
    private ArrayList<String> statPV = new ArrayList<>();
    private String statCurrMove = "";
    private int statCurrMoveNr = 0;

    private ArrayList<PvInfo> statPvInfo = new ArrayList<>();

    private boolean depthModified = false;
    private boolean currMoveModified = false;
    private boolean pvModified = false;
    private boolean statsModified = false;

    private void clearInfo() {
        statCurrDepth = statPVDepth = statScore = 0;
        statIsMate = statUpperBound = statLowerBound = false;
        statTime = 0;
        statNodes = statTBHits = 0;
        statHash = 0;
        statSelDepth = 0;
        statNps = 0;
        depthModified = true;
        currMoveModified = true;
        pvModified = true;
        statsModified = true;
        statPvInfo.clear();
        statCurrMove = "";
        statCurrMoveNr = 0;
    }

    private synchronized int getReadTimeout() {
        boolean needGuiUpdate = (searchRequest != null && searchRequest.currBoard != null) &&
                (depthModified || currMoveModified || pvModified || statsModified);
        int timeout = 2000000000;
        if (needGuiUpdate) {
            long now = System.currentTimeMillis();
            timeout = (int) (lastGUIUpdate + guiUpdateInterval - now + 1);
            timeout = Math.max(1, Math.min(1000, timeout));
        }
        return timeout;
    }

    private void parseInfoCmd(String[] tokens) {
        try {
            boolean havePvData = false;
            int nTokens = tokens.length;
            int i = 1;
            int pvNum = 0;
            while (i < nTokens - 1) {
                String is = tokens[i++];
                if (is.equals("depth")) {
                    statCurrDepth = Integer.parseInt(tokens[i++]);
                    depthModified = true;
                } else if (is.equals("seldepth")) {
                    statSelDepth = Integer.parseInt(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("currmove")) {
                    statCurrMove = tokens[i++];
                    currMoveModified = true;
                } else if (is.equals("currmovenumber")) {
                    statCurrMoveNr = Integer.parseInt(tokens[i++]);
                    currMoveModified = true;
                } else if (is.equals("time")) {
                    statTime = Integer.parseInt(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("nodes")) {
                    statNodes = Long.parseLong(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("tbhits")) {
                    statTBHits = Long.parseLong(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("hashfull")) {
                    statHash = Integer.parseInt(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("nps")) {
                    statNps = Integer.parseInt(tokens[i++]);
                    statsModified = true;
                } else if (is.equals("multipv")) {
                    pvNum = Integer.parseInt(tokens[i++]) - 1;
                    if (pvNum < 0) pvNum = 0;
                    if (pvNum > 255) pvNum = 255;
                    pvModified = true;
                } else if (is.equals("pv")) {
                    statPV.clear();
                    while (i < nTokens)
                        statPV.add(tokens[i++]);
                    pvModified = true;
                    havePvData = true;
                    statPVDepth = statCurrDepth;
                } else if (is.equals("score")) {
                    statIsMate = tokens[i++].equals("mate");
                    statScore = Integer.parseInt(tokens[i++]);
                    statUpperBound = false;
                    statLowerBound = false;
                    if (tokens[i].equals("upperbound")) {
                        statUpperBound = true;
                        i++;
                    } else if (tokens[i].equals("lowerbound")) {
                        statLowerBound = true;
                        i++;
                    }
                    pvModified = true;
                }
            }
            if (havePvData) {
                while (statPvInfo.size() < pvNum)
                    statPvInfo.add(new PvInfo(0, 0, 0, 0, 0, 0, 0, 0, false, false, false, new ArrayList<>()));
                if (statPvInfo.size() == pvNum)
                    statPvInfo.add(null);
                ArrayList<Move> moves = new ArrayList<>();
                int nMoves = statPV.size();
                for (i = 0; i < nMoves; i++) {
                    Move m = new Move(null, null);
                    m.fromUCCIString(statPV.get(i));
                    if (m == null)
                        break;
                    moves.add(m);
                }
                statPvInfo.set(pvNum, new PvInfo(statPVDepth, statScore, statTime, statNodes, statNps,
                                                 statTBHits, statHash, statSelDepth,
                                                 statIsMate, statUpperBound, statLowerBound, moves));
            }
        } catch (NumberFormatException nfe) {
            Log.d("ComputerPlayer", "parseInfoCmd: " + nfe);
            // Ignore
        } catch (ArrayIndexOutOfBoundsException aioob) {
            Log.d("ComputerPlayer", "parseInfoCmd: " + aioob);
            // Ignore
        }
    }

    /**
     * Notify listener about search statistics.
     */
    private synchronized void notifyListener() {
        if (Thread.currentThread().isInterrupted())
            return;

        if ((searchRequest == null) || (searchRequest.currBoard == null))
            return;

        long now = System.currentTimeMillis();
        if (now < lastGUIUpdate + guiUpdateInterval)
            return;

        int id = engineState.searchId;
        if (depthModified) {
            searchListener.notifyDepth(id, statCurrDepth);
            depthModified = false;
        }
        if (currMoveModified) {
//            Move m = new Move(searchRequest.currBoard);
//            boolean result = m.fromUCCIString(statCurrMove);
//            Board board = searchRequest.currBoard;
//            if ((searchRequest.ponderMove != null) && (m != null)) {
//                board = new Board(board);
//                UndoInfo ui = new UndoInfo();
//                board.makeMove(searchRequest.ponderMove, ui);
//            }
//            searchListener.notifyCurrMove(id, board, m, statCurrMoveNr);
            currMoveModified = false;
        }
        if (pvModified) {
            searchListener.notifyPV(id, searchRequest.currBoard, statPvInfo, searchRequest.ponderMove);
            pvModified = false;
        }
        if (statsModified) {
            searchListener.notifyStats(id, statNodes, statNps, statTBHits, statHash, statTime, statSelDepth);
            statsModified = false;
        }
        lastGUIUpdate = System.currentTimeMillis();
    }

    private static void myAssert(boolean b) {
        if (!b)
            throw new RuntimeException();
    }

    public void sendToEngine(String command) {
        // for troubleshooting only
        if (uciEngine != null) {
            uciEngine.writeLineToEngine(command);
            Log.d("ComputerPlayer", "sendToEngine: " + command);
        }
    }
}
