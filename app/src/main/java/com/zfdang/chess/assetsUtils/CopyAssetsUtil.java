package com.zfdang.chess.assetsUtils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by tongdexin on 2017/2/21.
 * https://github.com/ohyes666/CopyAssets
 **/
public class CopyAssetsUtil {

    /**
     * 递归拷贝Assets下所有的文件
     *
     * @param context  上下文
     * @param outPath  复制后输出的目录
     * @param copyKeys 复制关键字
     */
    public static void copyAssets(Context context, String outPath, String[] copyKeys) throws IOException {
        String fileNames[] = context.getAssets().list("");
        File file = new File(outPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        if (fileNames.length > 0) {
            for (String fileName : fileNames) {
                for (String copyKey : copyKeys) {
                    if (fileName.startsWith(copyKey)) {
                        copyAssets(context, fileName, outPath + File.separator + fileName);
                    }
                }

            }
        }
    }


    /**
     * 递归拷贝Assets下指定文件夹中所有的文件
     *
     * @param context    上下文
     * @param folderName 拷贝的文件夹
     * @param outPath    复制后输出的目录
     */
    public static void copyAssets(Context context, String folderName, String outPath) throws IOException {
        //Assets下指定文件夹(folderName)下所有文件
        String fileNames[] = context.getAssets().list(folderName);
        if (fileNames.length > 0) {
            File file = new File(outPath);
            file.mkdirs();
            for (String fileName : fileNames) {
                copyAssets(context, folderName + File.separator + fileName, outPath + File.separator + fileName);
            }
        } else {
            InputStream is = context.getAssets().open(folderName);
            FileOutputStream fos = new FileOutputStream(new File(outPath));
            byte[] buffer = new byte[1024];
            int byteCount = 0;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
        }
    }

    /**
     * 解压assets的zip压缩文件到指定目录
     *
     * @param context         上下文对象
     * @param assetName       压缩文件名
     * @param outputDirectory 输出目录
     * @param isReWrite       是否覆盖
     */
    public static void unZip(Context context, String assetName,
                             String outputDirectory, boolean isReWrite) throws IOException {
        //创建解压目标目录
        File file = new File(outputDirectory);
        //如果目标目录不存在，则创建
        if (!file.exists()) {
            file.mkdirs();
        }
        //打开压缩文件
        InputStream inputStream = context.getAssets().open(assetName);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        //读取一个进入点
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        //使用1Mbuffer
        byte[] buffer = new byte[1024 * 1024];
        //解压时字节计数
        int count = 0;
        //如果进入点为空说明已经遍历完所有压缩包中文件和目录
        while (zipEntry != null) {
            //如果是一个目录
            if (zipEntry.isDirectory()) {
                file = new File(outputDirectory + File.separator + zipEntry.getName());
                //文件需要覆盖或者是文件不存在
                if (isReWrite || !file.exists()) {
                    file.mkdir();
                }
            } else {
                //如果是文件
                file = new File(outputDirectory + File.separator
                        + zipEntry.getName());
                //文件需要覆盖或者文件不存在，则解压文件
                if (isReWrite || !file.exists()) {
                    file.createNewFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    while ((count = zipInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, count);
                    }
                    fileOutputStream.close();
                }
            }
            //定位到下一个文件入口
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
    }
}