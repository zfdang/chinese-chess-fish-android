package com.zfdang.chess.engine;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class EngineAssets {
    public static final String VERSION = "2026-09-06";
    private static final String SHA256 = "7d13d73569a9b571ba0eb20cf1596247bc2a42738967e61afef6482b231e900e";
    private static final long NETWORK_SIZE = 50706378;

    public static File networkFile(Context context) {
        return new File(context.getFilesDir(), "pikafish/" + VERSION + "/pikafish.nnue");
    }

    /** Run on the service worker, never on the UI thread. Atomically install the pinned network. */
    static synchronized File prepare(Context context) throws IOException {
        File target = networkFile(context);
        if (target.isFile() && verify(target)) return target;
        File dir = target.getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create network directory");
        File temporary = new File(dir, "pikafish.nnue.tmp");
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            try (InputStream in = context.getAssets().open("pikafish/pikafish-" + VERSION + ".nnue");
                 FileOutputStream out = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[65536];
                int count;
                while ((count = in.read(buffer)) != -1) { out.write(buffer, 0, count); hash.update(buffer, 0, count); }
                out.getFD().sync();
            }
            StringBuilder digest = new StringBuilder();
            for (byte b : hash.digest()) digest.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            if (!SHA256.contentEquals(digest) || temporary.length() != NETWORK_SIZE)
                throw new IOException("Pikafish network checksum mismatch");
            if (!temporary.renameTo(target)) throw new IOException("Cannot install Pikafish network");
            return target;
        } catch (NoSuchAlgorithmException error) {
            throw new IOException(error);
        } finally { temporary.delete(); }
    }

    /** Re-verify an already-installed network by both length and SHA-256; a corrupt or truncated file is re-extracted. */
    private static boolean verify(File file) {
        if (!file.isFile() || file.length() != NETWORK_SIZE) return false;
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[65536];
                int count;
                while ((count = in.read(buffer)) != -1) hash.update(buffer, 0, count);
            }
            StringBuilder digest = new StringBuilder();
            for (byte b : hash.digest()) digest.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return SHA256.contentEquals(digest);
        } catch (IOException | NoSuchAlgorithmException error) {
            return false;
        }
    }
    private EngineAssets() {}
}
