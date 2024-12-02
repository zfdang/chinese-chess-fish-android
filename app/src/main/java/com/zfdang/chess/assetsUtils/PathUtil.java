package com.zfdang.chess.assetsUtils;

import android.content.Context;
import android.os.Environment;

/**
 * Created by tongdexin on 2017/2/21.
 * https://github.com/ohyes666/CopyAssets
 **/
public class PathUtil {

    /**
     * ($rootDir)
     * +- /data                -> Environment.getDataDirectory()
     * |   |
     * |   |   ($appDataDir)
     * |   +- data/package.name
     * |       |
     * |       |   ($filesDir)
     * |       +- files            -> Context.getFilesDir() / Context.getFileStreamPath("")
     * |       |       |
     * |       |       +- filexample    -> Context.getFileStreamPath("filexample")
     * |       |   ($cacheDir)
     * |       +- cache            -> Context.getCacheDir()
     * |       |
     * |       +- app_$name        ->(Context.getDir(String name, int mode)
     * |
     * |   ($rootDir)
     * +- /storage/sdcard0     -> Environment.getExternalStorageDirectory()
     * |                       / Environment.getExternalStoragePublicDirectory("")
     * |
     * +- dir1             -> Environment.getExternalStoragePublicDirectory("dir1")
     * |
     * |   ($appDataDir)
     * +- Andorid/data/package.name
     * |
     * |   ($filesDir)
     * +- files        -> Context.getExternalFilesDir("")
     * |   |
     * |   +- filexample    -> Context.getExternalFilesDir("filexample")
     * |   +- Music    -> Context.getExternalFilesDir(Environment.Music);
     * |   +- Picture  -> ... Environment.Picture
     * |   +- ...
     * |
     * |   ($cacheDir)
     * +- cache        -> Context.getExternalCacheDir()
     * |
     * +-
     */
    public static void showAndroidDir() {

    }


    /**
     * 手机内部存储根目录 : $rootDir/data
     */
    public static String getInternalRootDir() {
        return Environment.getDataDirectory().toString();
    }

    /**
     * 手机内部存储 应用数据文件目录 : $rootDir/data/data/[package.name]/files
     * 应用删除后,文件内容删除
     */
    public static String getInternalAppFilesDir(Context context) {
        return getInternalAppFilesDir(context, "");
    }

    /**
     * 手机内部存储 应用数据文件目录下 指定文件夹目录 : $rootDir/data/data/[package.name]/files/[folderName]
     * 应用删除后,文件内容删除
     *
     * @param folderName 指定文件夹
     */
    public static String getInternalAppFilesDir(Context context, String folderName) {
        return context.getFileStreamPath(folderName).toString();
    }

    /**
     * 手机内部存储 应用数据缓存目录 : $rootDir/data/data/[package.name]/cache
     * 手机内部存储空间不够时,删除
     * 应用删除后,文件内容删除
     */
    public static String getInternalAppCacheDir(Context context) {
        return context.getCacheDir().toString();
    }

    /**
     * 手机内部存储 $cacheDir / $filesDir 同级目录
     * $rootDir/data/data/[package.name]/app_dirExample
     * 应用删除后,文件内容删除
     *
     * @param dirExample 文件夹名称
     * @param mode       Context.MODE_PRIVATE
     */
    public static String getInternalAppDir(Context context, String dirExample, int mode) {
        return context.getDir(dirExample, mode).toString();
    }

    /**
     * 外部存储根目录 /storage/sdcard0/
     */
    public static String getExternalStorageRootDir() {
        return getExternalStorageRootDir("");
    }

    /**
     * 外部存储根目录下指定文件夹 :/storage/sdcard0/[folderName]
     */
    public static String getExternalStorageRootDir(String folderName) {
        return Environment.getExternalStoragePublicDirectory(folderName).toString();
    }

    /**
     * 外部存储 应用数据文件 :/storage/sdcard0/Andorid/data/package.name/files
     * 应用删除后,文件内容删除
     */
    public static String getExternalStorageAppFileDir(Context context) {
        return context.getExternalFilesDir("").toString();
    }

    /**
     * 外部存储 应用数据文件夹下指定文件夹 :/storage/sdcard0/Andorid/data/package.name/files/[folderName]
     * 应用删除后,文件内容删除
     *
     * @param folderName Environment.DIRECTORY_MUSIC/Environment.Picture/"filexample"
     */
    public static String getExternalStorageAppFileDir(Context context, String folderName) {
        return getExternalStorageAppFileDir(context, folderName);
    }

    /**
     * 外部存储 应用数据缓存文件 :/storage/sdcard0/Andorid/data/package.name/cache
     * 应用删除后,文件内容删除
     */
    public static String getExternalStorageAppCacheDir(Context context) {
        return context.getExternalCacheDir().toString();
    }
}