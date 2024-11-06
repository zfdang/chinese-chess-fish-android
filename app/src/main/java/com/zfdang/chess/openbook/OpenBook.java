package com.zfdang.chess.openbook;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public interface OpenBook {

    public enum SortRule {
        BEST_SCORE,
        BEST_WINRATE,
        POSITIVE_RANDOM,
        FULL_RANDOM
    }

    // for local openbook
    public List<BookData> query(long vkey, boolean redGo, SortRule rule);

    // for cloud openbook
    public List<BookData> query(String fenCode, boolean onlyFinalPhase, SortRule rule);

    // close database
    public void close();
}
