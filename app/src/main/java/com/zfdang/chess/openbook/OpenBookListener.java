package com.zfdang.chess.openbook;

import java.util.ArrayList;
import java.util.List;

public interface OpenBookListener {
    // this method might be called multiple, please handle it properly
    public void onBookData(ArrayList<BookData> data);
}
