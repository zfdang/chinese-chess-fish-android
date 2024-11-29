package org.petero.droidfish.player;

import com.zfdang.chess.gamelogic.Board;
import com.zfdang.chess.gamelogic.Move;

import java.util.ArrayList;


/**
 * Information about current/next engine search task.
 */
public class SearchRequest {
    int searchId;           // Unique identifier for this search request
    long startTime;         // System time (milliseconds) when search request was created

    Board prevBoard;       // Board at last null move
    ArrayList<Move> mList;  // Moves after prevBoard, including ponderMove
    Move ponderMove;        // Ponder move, or null if not a ponder search
    Board currBoard;       // currBoard = prevboard + mList - ponderMove
    boolean drawOffer;      // True if other side made draw offer
    boolean isSearch;       // True if regular search or ponder search
    boolean isEval;         // True if evaluation only
    int wTime;              // White remaining time, milliseconds
    int bTime;              // Black remaining time, milliseconds
    int wInc;               // White time increment per move, milliseconds
    int bInc;               // Black time increment per move, milliseconds
    int movesToGo;          // Number of moves to next time control

    String engineName;      // Engine name (identifier)
    int numPV;              // Number of PV lines to compute

    boolean ponderEnabled;  // True if pondering enabled, for engine time management

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
        sr.isEval = false;
        sr.engineName = engine;
        return sr;
    }

    /**
     * Create a search request object.*
     */
    public static SearchRequest searchRequest(int id,
                                              Board prevBoard,
                                              ArrayList<Move> moves,
                                              Board currBoard,
                                              ArrayList<Move> searchmoves,
                                              boolean drawOffer,
                                              String engine,
                                              int numPV) {
        SearchRequest sr = new SearchRequest();
        sr.searchId = id;
        sr.startTime = System.currentTimeMillis();
        sr.prevBoard = prevBoard;
        sr.mList = moves;
        sr.searchMoves = searchmoves;
        sr.currBoard = currBoard;
        sr.drawOffer = drawOffer;
        sr.isSearch = true;
        sr.isEval = false;
        sr.wTime = sr.bTime = sr.wInc = sr.bInc = sr.movesToGo = 0;
        sr.engineName = engine;
        sr.numPV = numPV;
        sr.ponderEnabled = false;
        sr.ponderMove = null;
        return sr;
    }

    /**
     * Create a search request object.*
     */
    public static SearchRequest evalRequest(int id,
                                              Board currBoard,
                                              String engine) {
        SearchRequest sr = new SearchRequest();
        sr.searchId = id;
        sr.startTime = System.currentTimeMillis();
        sr.currBoard = currBoard;
        sr.isSearch = false;
        sr.isEval = true;
        sr.engineName = engine;
        return sr;
    }

}