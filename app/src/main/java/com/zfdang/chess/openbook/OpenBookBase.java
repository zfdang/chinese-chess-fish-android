package com.zfdang.chess.openbook;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

abstract public class OpenBookBase implements OpenBook{

    abstract protected List<BookData> get(long vkey);

    abstract protected List<BookData> get(String fenCode, boolean onlyFinalPhase);

    @Override
    public List<BookData> query(long vkey, boolean redGo, SortRule rule) {
        List<BookData> list = get(vkey);
        sort(list, rule);
        return list;
    }

    @Override
    public List<BookData> query(String fenCode, boolean onlyFinalPhase, SortRule rule) {
        List<BookData> list = get(fenCode, onlyFinalPhase);
        sort(list, rule);
        return list;
    }

    void sort(List<BookData> list, SortRule rule) {
        Collections.sort(list, new Comparator<BookData>() {
            private Random rd = new SecureRandom();
            @Override
            public int compare(BookData o1, BookData o2) {
                switch (rule) {
                    case BEST_SCORE: {
                        return o1.getScore() > o2.getScore() ? -1 : (o1.getScore() < o2.getScore() ? 1 : 0);
                    }
                    case BEST_WINRATE: {
                        return o1.getWinRate() > o2.getWinRate() ? -1 : (o1.getWinRate() < o2.getWinRate() ? 1 : 0);
                    }
                    case FULL_RANDOM: {
                        if (rd.nextInt(100) < 50) {
                            return 1;
                        } else {
                            return -1;
                        }
                    }
                    case POSITIVE_RANDOM: {
                        if (o1.getScore() > 0 && o2.getScore() > 0) {
                            if (rd.nextInt(100) < 50) {
                                return 1;
                            } else {
                                return -1;
                            }
                        } else {
                            return o1.getScore() > o2.getScore() ? -1 : (o1.getScore() < o2.getScore() ? 1 : 0);
                        }
                    }
                    default: {
                        return 0;
                    }
                }
            }
        });
    }

    @Override
    public void close() {

    }
}
