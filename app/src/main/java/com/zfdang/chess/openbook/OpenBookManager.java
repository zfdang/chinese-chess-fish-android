package com.zfdang.chess.openbook;


import java.util.ArrayList;
import java.util.List;

public class OpenBookManager {

    private volatile static OpenBookManager instance;

    private MoveRule moveRule = MoveRule.BEST_SCORE;

    private OpenBook cloudOpenBook;
    private List<OpenBook> localOpenBooks;

    private OpenBookManager() {
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
        ArrayList<String> openBookList = new ArrayList<>();
        // init openBookList
        for (String path : openBookList) {
            try {
                if (path.endsWith(".obk")) {
                    localOpenBooks.add(new BHOpenBook(path));
                } else if (path.endsWith(".pfBook")) {
                    localOpenBooks.add(new PFOpenBook(path));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized List<BookData> queryBook(char[][] b, boolean redGo, boolean offManual) {

        List<BookData> cloudResults = new ArrayList<>();
        if (true) {
//            String fenCode = ChessBoard.fenCode(b, redGo);
            String fenCode = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1";
            cloudResults.addAll(cloudOpenBook.query(fenCode, offManual, moveRule));
        }

        List<BookData> localResults = new ArrayList<>();
        if (!offManual) {
            for (OpenBook ob : this.localOpenBooks) {
                localResults.addAll(ob.query(b, redGo, moveRule));
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

    public static OpenBookManager getInstance() {
        if (instance == null) {
            synchronized (OpenBookManager.class) {
                if (instance == null) {
                    instance = new OpenBookManager();
                }
            }
        }
        return instance;
    }



}
