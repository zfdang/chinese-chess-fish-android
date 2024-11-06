package com.zfdang.chess.openbook;

import android.content.Context;

import com.zfdang.chess.gamelogic.Board;

import java.util.ArrayList;

public class OpenBookManager {
    private volatile static OpenBookManager instance;
    private Context context = null;

    private OpenBook.SortRule sortRule = OpenBook.SortRule.BEST_SCORE;

    private OpenBook cloudOpenBook;
    private ArrayList<OpenBook> localOpenBooks;

    private OpenBookManager(Context context) {
        this.context = context;

        this.cloudOpenBook = new CloudOpenBook();

        this.localOpenBooks = new ArrayList<>();
        setLocalOpenBooks();
    }

    public synchronized void close() {
        for (OpenBook ob : localOpenBooks) {
            ob.close();
        }
    }

    public synchronized void setLocalOpenBooks() {
        close();
        localOpenBooks.clear();

        // now we support BHOpenBook only
        localOpenBooks.add(new BHOpenBook(this.context));
    }

    public synchronized ArrayList<BookData> queryBook(Board board, boolean redGo, boolean onlinebook, boolean localbook) {

        ArrayList<BookData> cloudResults = new ArrayList<>();
        if (onlinebook) {
            String fenCode = board.toFENString();
            cloudResults.addAll(cloudOpenBook.query(fenCode, false, sortRule));
        }

        ArrayList<BookData> localResults = new ArrayList<>();
        if (localbook) {
            for (OpenBook ob : this.localOpenBooks) {
                long vkey = 0;
                localResults.addAll(ob.query(vkey, redGo, sortRule));
            }
        }

        if (true) {
            localResults.addAll(cloudResults);
            return localResults;
        } else {
            cloudResults.addAll(localResults);
            return cloudResults;
        }
    }

    public static OpenBookManager getInstance(Context context) {
        if (instance == null) {
            synchronized (OpenBookManager.class) {
                if (instance == null) {
                    instance = new OpenBookManager(context);
                }
            }
        }
        return instance;
    }
}
