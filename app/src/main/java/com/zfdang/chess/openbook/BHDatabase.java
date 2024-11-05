package com.zfdang.chess.openbook;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class BHDatabase extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "HYV20221122.db.zip";
    private static final int DATABASE_VERSION = 1;

    public BHDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        // Upgrades via overwrite
        setForcedUpgrade();
    }

    public String getLocation(String dottedIP) {
        String intIP = dottedIP;

        SQLiteDatabase db = getReadableDatabase();
        Cursor result = db.rawQuery("select ip,country from qqwry where ip< " + intIP + " order by ip desc limit 1", null);
        result.moveToFirst();
        String country = result.getString(1);
        result.close();

        return country;
    }
}