package com.zfdang.chess.openbook;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

// https://github.com/jgilfelt/android-sqlite-asset-helper
public class BHDatabase extends SQLiteAssetHelper {

//    CREATE TABLE bhobk([id] INTEGER PRIMARY KEY AUTOINCREMENT,
//                       [vkey] INTEGER,
//                       [vmove] INTEGER,
//                       [vscore] INTEGER,
//                       [vwin] INTEGER,
//                       [vdraw] INTEGER,
//                       [vlost] INTEGER,
//                       [vvalid] INTEGER,
//                       [vmemo] BLOB,
//                       [vindex] INTEGER)

    public static final String OPENBOOK_NAME = "桔库09.09.2023精修库";
    private static final String DATABASE_NAME = "桔库09.09.2023精修库.obk";
    private static final int DATABASE_VERSION = 15;

    public BHDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        // Upgrades via overwrite
        setForcedUpgrade();
    }
}