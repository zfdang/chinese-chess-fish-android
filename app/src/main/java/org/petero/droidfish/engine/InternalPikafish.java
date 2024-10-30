/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2014  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish.engine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;

import android.util.Log;

import org.petero.droidfish.EngineOptions;

/** Stockfish engine running as process, started from assets resource. */
public class InternalPikafish extends ExternalEngine {
    private static final String[] defaultNetworkFiles = {"pikafish.nnue", "pikafish.ini", "version.txt"};
    private static final String engineFile = "pikafish";

    private final File[] defaultDestNetworkFiles = {null, null, null}; // Full path of the copied default network files
    private static final String[] netOptions = {"evalfile", "evalfilesmall"};

    public InternalPikafish(Report report, String workDir) {
        super("pikafish", workDir, report);
    }

    @Override
    protected File getOptionsFile() {
        File extDir = context.getFilesDir();
        return new File(extDir, "pikafish.ini");
    }

    @Override
    protected boolean editableOption(String name) {
        name = name.toLowerCase(Locale.US);
        if (!super.editableOption(name))
            return false;
        if (name.equals("skill level") || name.equals("write debug log") ||
            name.equals("write search log"))
            return false;
        return true;
    }

    private long readCheckSum(File f) {
        try (InputStream is = new FileInputStream(f);
             DataInputStream dis = new DataInputStream(is)) {
            return dis.readLong();
        } catch (IOException e) {
            return 0;
        }
    }

    private void writeCheckSum(File f, long checkSum) {
        try (OutputStream os = new FileOutputStream(f);
             DataOutputStream dos = new DataOutputStream(os)) {
            dos.writeLong(checkSum);
        } catch (IOException ignore) {
        }
    }

    private long computeAssetsCheckSum(String sfExe) {
        try (InputStream is = context.getAssets().open(sfExe)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[8192];
            while (true) {
                int len = is.read(buf);
                if (len <= 0)
                    break;
                md.update(buf, 0, len);
            }
            byte[] digest = md.digest(new byte[]{0});
            long ret = 0;
            for (int i = 0; i < 8; i++) {
                ret ^= ((long)digest[i]) << (i * 8);
            }
            return ret;
        } catch (IOException e) {
            return -1;
        } catch (NoSuchAlgorithmException e) {
            return -1;
        }
    }

    @Override
    protected String copyFile(File from, File exeDir) throws IOException {
        File to = new File(exeDir, "engine.exe");

        // The checksum test is to avoid writing to /data unless necessary,
        // on the assumption that it will reduce memory wear.
        long oldCSum = readCheckSum(new File(engineCheckSumFilePath()));
        long newCSum = computeAssetsCheckSum(engineFile);
        if (oldCSum != newCSum) {
            copyAssetFile(engineFile, to);
            writeCheckSum(new File(engineCheckSumFilePath()), newCSum);
        }
        try {
            chmod(to.getAbsolutePath());
        } catch (Exception e) {
            Log.d("InternalPikafish", "chmod failed: " + e);
            throw new RuntimeException(e);
        }
        copyNetworkFiles(exeDir);
        return to.getAbsolutePath();
    }

    /** Copy the Stockfish default network files to "exeDir" if they are not already there. */
    private void copyNetworkFiles(File exeDir) throws IOException {
        for (int i = 0; i < defaultNetworkFiles.length; i++) {
            defaultDestNetworkFiles[i] = new File(exeDir, defaultNetworkFiles[i]);
            if (!defaultDestNetworkFiles[i].exists()) {
                File tmpFile = new File(exeDir, defaultNetworkFiles[i] + ".tmp");
                copyAssetFile(defaultNetworkFiles[i], tmpFile);
                if (!tmpFile.renameTo(defaultDestNetworkFiles[i])) {
                    Log.d("InternalPikafish", "Rename failed " + defaultDestNetworkFiles[i]);
                    throw new IOException("Rename failed");
                }
            } else {
                Log.d("InternalPikafish", "Network file " + defaultNetworkFiles[i] + " already exists");
            }
        }
    }

    /** Copy a file resource from the AssetManager to the file system,
     *  so it can be used by native code like the Stockfish engine. */
    private void copyAssetFile(String assetName, File targetFile) throws IOException {
        try (InputStream is = context.getAssets().open(assetName);
             OutputStream os = new FileOutputStream(targetFile)) {
            byte[] buf = new byte[8192];
            while (true) {
                int len = is.read(buf);
                if (len <= 0)
                    break;
                os.write(buf, 0, len);
            }
        }
    }

    /** Return true if file "f" should be kept in the exeDir directory.
     *  It would be inefficient to remove the network file every time
     *  an engine different from Stockfish is used, so this is a static
     *  check performed for all engines. */
    public static boolean keepExeDirFile(File f) {
        return Arrays.asList(defaultNetworkFiles).contains(f.getName());
    }

    @Override
    public void initOptions(EngineOptions engineOptions) {
        super.initOptions(engineOptions);
        for (int i = 0; i < 2; i++) {
            UCIOptions.OptionBase opt = getUCIOptions().getOption(netOptions[i]);
            if (opt != null)
                setOption(netOptions[i], opt.getStringValue());
        }
    }

    /** Handles setting the EvalFile UCI option to a full path if needed,
     *  pointing to the network file embedded in DroidFish. */
    @Override
    public boolean setOption(String name, String value) {
        for (int i = 0; i < 2; i++) {
            if (name.toLowerCase(Locale.US).equals(netOptions[i]) &&
                (defaultNetworkFiles[i].equals(value) || value.isEmpty())) {
                getUCIOptions().getOption(name).setFromString(value);
                value = defaultDestNetworkFiles[i].getAbsolutePath();
                writeLineToEngine(String.format(Locale.US, "setoption name %s value %s", name, value));
                return true;
            }
        }
        return super.setOption(name, value);
    }
}
