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
        // Hashing 50 MB on every start costs a few hundred milliseconds. Once a copy has been
        // verified, record that against this exact version so later starts can skip the scan.
        if (target.isFile() && isVerified(target)) return target;
        if (target.isFile() && verify(target)) {
            markVerified(target);
            pruneOldVersions(target);
            return target;
        }
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
            markVerified(target);
            pruneOldVersions(target);
            return target;
        } catch (NoSuchAlgorithmException error) {
            throw new IOException(error);
        } finally { temporary.delete(); }
    }

    private static File markerFile(File network) {
        return new File(network.getParentFile(), "pikafish.nnue.verified");
    }

    /** The marker records the constants it was checked against, so a new version invalidates it. */
    private static String markerContent() {
        return VERSION + " " + NETWORK_SIZE + " " + SHA256;
    }

    private static boolean isVerified(File network) {
        File marker = markerFile(network);
        if (!marker.isFile()) return false;
        try (java.io.InputStream in = new java.io.FileInputStream(marker)) {
            byte[] bytes = new byte[(int) Math.min(marker.length(), 4096)];
            int read = 0;
            while (read < bytes.length) {
                int count = in.read(bytes, read, bytes.length - read);
                if (count < 0) break;
                read += count;
            }
            return markerContent().contentEquals(new String(bytes, 0, read, java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException error) {
            return false;
        }
    }

    private static void markVerified(File network) {
        try (java.io.OutputStream out = new java.io.FileOutputStream(markerFile(network))) {
            out.write(markerContent().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException error) {
            // Losing the marker only costs a re-hash next time; the network itself is installed.
        }
    }

    /** Drop networks from older pinned versions so upgrades do not accumulate 50 MB per release. */
    private static void pruneOldVersions(File network) {
        File versionDir = network.getParentFile();
        File versionsRoot = versionDir != null ? versionDir.getParentFile() : null;
        if (versionsRoot == null) return;
        File[] versions = versionsRoot.listFiles(File::isDirectory);
        if (versions == null) return;
        for (File version : versions) {
            if (!VERSION.equals(version.getName())) deleteRecursively(version);
        }
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteRecursively(child);
        file.delete();
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
