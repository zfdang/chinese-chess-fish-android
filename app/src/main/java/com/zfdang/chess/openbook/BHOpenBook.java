package com.zfdang.chess.openbook;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.zfdang.chess.gamelogic.Zobrist;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BHOpenBook extends OpenBookBase {

    private Connection connection;

    private String name;
    private BHDatabase bhDB = null;
    SQLiteDatabase db = null;

    public BHOpenBook(Context context){
        // database asset file is managed by BHDatabase class
        bhDB = new BHDatabase(context);
        db = bhDB.getReadableDatabase();

        name = bhDB.OPENBOOK_NAME;
    }


    @Override
    protected List<BookData> get(long vkey) {
        List<BookData> list = new ArrayList<>();
        try {
            String sql = String.format("select * from bhobk where vvalid = 1 and vkey = %d order by vscore desc, vwin desc limit 5", vkey, vkey);
            Log.d("BHOpenBook", "get: " + sql);
            try (Cursor cursor = db.rawQuery(sql, null)) {
                // Missing columns are reported by the query failure handler; the cursor always closes.
                while (cursor.moveToNext()) {
                    BookData bd = new BookData();
                    int vmove = cursor.getInt(cursor.getColumnIndexOrThrow("vmove"));
                    String move = Zobrist.getMoveFromVmove(vmove);
                    bd.setMove(move);
                    bd.setScore(cursor.getInt(cursor.getColumnIndexOrThrow("vscore")));
                    bd.setWinRate(cursor.getDouble(cursor.getColumnIndexOrThrow("vwin")));
                    bd.setDrawNum(cursor.getInt(cursor.getColumnIndexOrThrow("vdraw")));
                    bd.setLoseNum(cursor.getInt(cursor.getColumnIndexOrThrow("vlost")));
                    bd.setNote(cursor.getString(cursor.getColumnIndexOrThrow("vmemo")));
                    bd.setSource(this.name);
                    list.add(bd);
                }
            }
        } catch (Exception e) {
            Log.d("BHOpenBook", "get: " + e.getMessage());
            Log.d("BHOpenBook", "get: " + e.getStackTrace());
        }
        return list;
    }

    @Override
    protected List<BookData> get(String fenCode, boolean onlyFinalPhase) {
        return Collections.emptyList();
    }


    @Override
    public void close() {
        bhDB.close();
    }
}
