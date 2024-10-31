package org.petero.droidfish.player;

import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.Position;
import com.zfdang.chess.gamelogic.PvInfo;

import java.util.ArrayList;

/**
 * Used to get various search information during search.
 */
public interface SearchListener {

    /**
     * Report current engine search depth.
     */
    void notifyDepth(int id, int depth);

    /**
     * Report the move, valid in position pos, that the engine is currently searching.
     */
    void notifyCurrMove(int id, Position pos, Move m, int moveNr);

    /**
     * Report PV information. If ponderMove is non-null, ponderMove is the first move
     * to play from position pos.
     */
    void notifyPV(int id, Position pos, ArrayList<PvInfo> pvInfo, Move ponderMove);

    /**
     * Report search statistics.
     */
    void notifyStats(int id, long nodes, int nps, long tbHits, int hash, int time, int seldepth);

    /**
     * Report opening book information.
     */
    void notifyBookInfo(int id, String bookInfo, ArrayList<Move> moveList,
                        String eco, int distToEcoTree);

    /**
     * Report move (or command, such as "resign") played by the engine.
     */
    void notifySearchResult(int id, String cmd, String ponder);

    /**
     * Report engine name.
     */
    void notifyEngineName(String engineName);

    /**
     * Report engine error.
     */
    void reportEngineError(String errMsg);

    /**
     * Report that engine has been initialized.
     */
    void notifyEngineInitialized();
}
