package com.zfdang.chess.openbook;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

// https://github.com/jgilfelt/android-sqlite-asset-helper
public class HSKDDatabase extends SQLiteAssetHelper {

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

    // 华弈开局库V20221122（116MB）.obk
<<<<<<< HEAD
    private static final String DATABASE_NAME = "桔库09.09.2023精修库.obk";
    private static final int DATABASE_VERSION = 15;
=======
    private static final String DATABASE_NAME = "狂刀华山库2024N2.09.obk";
    private static final int DATABASE_VERSION = 8;
>>>>>>> refs/remotes/origin/master

    public HSKDDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        // Upgrades via overwrite
        setForcedUpgrade();
    }
}