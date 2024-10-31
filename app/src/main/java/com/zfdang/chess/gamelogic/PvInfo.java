package com.zfdang.chess.gamelogic;

import java.util.ArrayList;

public class PvInfo {
    int depth;
    int score;
    int time;
    long nodes;
    int nps;
    long tbHits;
    int hash;
    int seldepth;
    boolean isMate;
    boolean upperBound;
    boolean lowerBound;
    ArrayList<Move> pv;
    String pvStr = "";

    public PvInfo(int depth, int score, int time, long nodes, int nps, long tbHits, int hash, int seldepth,
                  boolean isMate, boolean upperBound, boolean lowerBound, ArrayList<Move> pv) {
        this.depth = depth;
        this.score = score;
        this.time = time;
        this.nodes = nodes;
        this.nps = nps;
        this.tbHits = tbHits;
        this.hash = hash;
        this.seldepth = seldepth;
        this.isMate = isMate;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
        this.pv = pv;
    }

}
