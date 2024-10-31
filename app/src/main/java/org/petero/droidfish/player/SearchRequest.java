package org.petero.droidfish.player;

import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.Position;

import java.util.ArrayList;


/**
 * Information about current/next engine search task.
 */
public class SearchRequest {
    int searchId;           // Unique identifier for this search request
    long startTime;         // System time (milliseconds) when search request was created

    Position prevPos;       // Position at last null move
    ArrayList<Move> mList;  // Moves after prevPos, including ponderMove
    Position currPos;       // currPos = prevPos + mList - ponderMove
    boolean drawOffer;      // True if other side made draw offer

    boolean isSearch;       // True if regular search or ponder search
    boolean isAnalyze;      // True if analysis search
    int wTime;              // White remaining time, milliseconds
    int bTime;              // Black remaining time, milliseconds
    int wInc;               // White time increment per move, milliseconds
    int bInc;               // Black time increment per move, milliseconds
    int movesToGo;          // Number of moves to next time control

    String engineName;          // Engine name (identifier)
    int numPV;              // Number of PV lines to compute

    boolean ponderEnabled;  // True if pondering enabled, for engine time management
    Move ponderMove;        // Ponder move, or null if not a ponder search

    long[] posHashList;     // For draw decision after completed search
    int posHashListSize;    // For draw decision after completed search
    ArrayList<Move> searchMoves; // Moves to search, or null to search all moves

    /**
     * Create a request to start an engine.
     *
     * @param id     Search ID.
     * @param engine Chess engine to use for searching.
     */
    public static SearchRequest startRequest(int id, String engine) {
        SearchRequest sr = new SearchRequest();
        sr.searchId = id;
        sr.isSearch = false;
        sr.isAnalyze = false;
        sr.engineName = engine;
        sr.posHashList = null;
        sr.posHashListSize = 0;
        return sr;
    }

    /**
     * Create a search request object.
     *
     * @param id            Search ID.
     * @param now           Current system time.
     * @param mList         List of moves to go from the earlier position to the current position.
     *                      This list makes it possible for the computer to correctly handle draw
     *                      by repetition/50 moves.
     * @param ponderEnabled True if pondering is enabled in the GUI. Can affect time management.
     * @param ponderMove    Move to ponder, or null for non-ponder search.
     * @param engine        Chess engine to use for searching.
     */
    public static SearchRequest searchRequest(int id, long now,
                                              Position prevPos, ArrayList<Move> mList,
                                              Position currPos, boolean drawOffer,
                                              int wTime, int bTime, int wInc, int bInc, int movesToGo,
                                              boolean ponderEnabled, Move ponderMove,
                                              String engine) {
        SearchRequest sr = new SearchRequest();
        sr.searchId = id;
        sr.startTime = now;
        sr.prevPos = prevPos;
        sr.mList = mList;
        sr.currPos = currPos;
        sr.drawOffer = drawOffer;
        sr.isSearch = true;
        sr.isAnalyze = false;
        sr.wTime = wTime;
        sr.bTime = bTime;
        sr.wInc = wInc;
        sr.bInc = bInc;
        sr.movesToGo = movesToGo;
        sr.engineName = engine;
        sr.numPV = 1;
        sr.ponderEnabled = ponderEnabled;
        sr.ponderMove = ponderMove;
        sr.posHashList = null;
        sr.posHashListSize = 0;
        return sr;
    }

    /**
     * Create an analysis request object.
     *
     * @param id        Search ID.
     * @param prevPos   Position corresponding to last irreversible move.
     * @param mList     List of moves from prevPos to currPos.
     * @param currPos   Position to analyze.
     * @param drawOffer True if other side have offered draw.
     * @param engine    Chess engine to use for searching
     * @param numPV     Multi-PV mode.
     */
    public static SearchRequest analyzeRequest(int id, Position prevPos,
                                               ArrayList<Move> mList,
                                               Position currPos,
                                               boolean drawOffer,
                                               String engine,
                                               int numPV) {
        SearchRequest sr = new SearchRequest();
        sr.searchId = id;
        sr.startTime = System.currentTimeMillis();
        sr.prevPos = prevPos;
        sr.mList = mList;
        sr.currPos = currPos;
        sr.drawOffer = drawOffer;
        sr.isSearch = false;
        sr.isAnalyze = true;
        sr.wTime = sr.bTime = sr.wInc = sr.bInc = sr.movesToGo = 0;
        sr.engineName = engine;
        sr.numPV = numPV;
        sr.ponderEnabled = false;
        sr.ponderMove = null;
        sr.posHashList = null;
        sr.posHashListSize = 0;
        return sr;
    }

    /**
     * Update data for ponder hit.
     */
    final void ponderHit() {
        if (ponderMove == null)
            return;
//            UndoInfo ui = new UndoInfo();
//            currPos.makeMove(ponderMove, ui);
//            ponderMove = null;
    }
}