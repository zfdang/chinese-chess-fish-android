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

import org.petero.droidfish.player.EngineListener;

/**
 * Stockfish engine running as process, started from assets resource.
 */
public class PikafishExternalEngine extends ExternalEngine {
    private static final String[] networkAsssetFiles = {"pikafish.nnue", "pikafish.ini", "version.txt"};
    private static final String[] networkOptions = {"evalfile"};

    // PikafishEngineFile: the name of the engine file in the assets directory
    // ChineseChess/app/src/main/assets/pikafish-armv8
    private static final String PikafishEngineFile = "pikafish-armv8";

    private final File[] networkFiles = {null, null, null}; // Full path of the copied network files

    public PikafishExternalEngine(String workDir, EngineListener listener) {
        super("pikafish", workDir, listener);

        // uci_showwdl is not included in the uci response, so we need to add it manually
        registerOption("option name uci_showwdl type check default false".split("\\s+"));
    }

    @Override
    protected File getIniFile() {
        File extDir = new File(context.getFilesDir(), "pikafish");
        return new File(extDir, "pikafish.ini");
    }

    /**
     * Return true if the UCI option can be edited in the "Engine Options" dialog.
     */
    //    pikafish目前的uci命令的输出
    //    id author the Pikafish developers (see AUTHORS file)
    //    option name Debug Log File type string default
    //    option name NumaPolicy type string default auto
    //    option name Threads type spin default 1 min 1 max 1024
    //    option name Hash type spin default 16 min 1 max 33554432
    //    option name Clear Hash type button
    //    option name Ponder type check default false
    //    option name MultiPV type spin default 1 min 1 max 128
    //    option name Move Overhead type spin default 10 min 0 max 5000
    //    option name nodestime type spin default 0 min 0 max 10000
    //    option name Skill Level type spin default 20 min 0 max 20
    //    option name Mate Threat Depth type spin default 10 min 0 max 10
    //    option name Repetition Rule type combo default AsianRule var AsianRule var ChineseRule var SkyRule var ComputerRule var AllowChase var YitianRule
    //    option name Draw Rule type combo default None var None var DrawAsBlackWin var DrawAsRedWin var DrawRepAsBlackWin var DrawRepAsRedWin
    //    option name Rule60MaxPly type spin default 120 min 0 max 120
    //    option name UCI_LimitStrength type check default false
    //    option name UCI_Elo type spin default 1280 min 1280 max 3133
    //    option name ScoreType type combo default Elo var Elo var PawnValueNormalized var Raw
    //    option name LU_Output type check default true
    //    option name EvalFile type string default pikafish.nnue
    //    uciok
    @Override
    protected boolean editableOption(String name) {
        name = name.toLowerCase(Locale.US);
        if (!super.editableOption(name))
            return false;
        // pikafish可修改的选项
        String[] editable = {"numapolicy", "threads", "hash", "clear hash", "ponder", "multipv", "move overhead", "skill level",
                "mate threat depth", "repetition rule", "draw rule", "rule60maxply", "scoretype", "lu_output", "evalfile", "uci_showwdl"};
        return Arrays.asList(editable).contains(name);
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
                ret ^= ((long) digest[i]) << (i * 8);
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
        // from is ignore, we always use the embedded engine: PikafishEngineFile
        File pikaDir = new File(exeDir.getParentFile(), "pikafish");
        if(pikaDir.exists() && !pikaDir.isDirectory()){
            pikaDir.delete();
            Log.d("ExternalPikafishEngine", "Deleted file " + pikaDir.getAbsolutePath());
        }

        if(!pikaDir.exists() && !pikaDir.mkdir()) {
            Log.d("ExternalPikafishEngine", "Failed to create directory " + pikaDir.getAbsolutePath());
        }

        File to = new File(pikaDir, PikafishEngineFile);

        // The checksum test is to avoid writing to /data unless necessary,
        // on the assumption that it will reduce memory wear.
        long oldCSum = readCheckSum(new File(getCheckSumFile(PikafishEngineFile)));
        long newCSum = computeAssetsCheckSum(PikafishEngineFile);
        if (!to.exists() || oldCSum != newCSum) {
            copyAssetFile(PikafishEngineFile, to);
            writeCheckSum(new File(getCheckSumFile(PikafishEngineFile)), newCSum);
            Log.d("ExternalPikafishEngine", "Copied " + PikafishEngineFile + " to " + to.getAbsolutePath());
        } else {
            Log.d("ExternalPikafishEngine", "Engine file " + to.getAbsolutePath() + " already exists");
        }

        copyNetworkFiles(pikaDir);
        return to.getAbsolutePath();
    }

    protected String getCheckSumFile(String filename) {
        return String.format("%s/%s.checksum", context.getFilesDir().getAbsolutePath(), filename);
    }

    /**
     * Copy the Stockfish default network files to "exeDir" if they are not already there.
     */
    private void copyNetworkFiles(File pikaDir) throws IOException {
        for (int i = 0; i < networkAsssetFiles.length; i++) {
            networkFiles[i] = new File(pikaDir, networkAsssetFiles[i]);

            long oldCSum = readCheckSum(new File(getCheckSumFile(networkAsssetFiles[i])));
            long newCSum = computeAssetsCheckSum(networkAsssetFiles[i]);

            if (!networkFiles[i].exists() || oldCSum != newCSum) {
                copyAssetFile(networkAsssetFiles[i], networkFiles[i]);
                writeCheckSum(new File(getCheckSumFile(networkAsssetFiles[i])), newCSum);
                Log.d("ExternalPikafishEngine", "Copied " + networkAsssetFiles[i] + " to " + networkFiles[i].getAbsolutePath());
            } else {
                Log.d("ExternalPikafishEngine", "Network file " + networkFiles[i].getAbsolutePath() + " already exists");
            }
        }
    }

    /**
     * Copy a file resource from the AssetManager to the file system,
     * so it can be used by native code like the Stockfish engine.
     */
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
        } catch (IOException e) {
            Log.d("ExternalPikafishEngine", "Failed to copy asset file " + assetName + " to " + targetFile.getAbsolutePath());
            Log.d("ExternalPikafishEngine", "Exception: " + e);
            throw e;
        }
    }

    @Override
    public void initConfig(EngineConfig engineConfig) {
        super.initConfig(engineConfig);
    }

    /**
     * Handles setting the EvalFile UCI option to a full path if needed,
     * pointing to the network file embedded in DroidFish.
     */
    @Override
    public boolean setOption(String name, String value) {
        for (int i = 0; i < networkOptions.length; i++) {
            if (name.toLowerCase(Locale.US).equals(networkOptions[i]) && networkAsssetFiles[i].equals(value)) {
                getUCIOptions().getOption(name).setFromString(value);
                value = networkFiles[i].getAbsolutePath();
                writeLineToEngine(String.format(Locale.US, "setoption name %s value %s", name, value));
                Log.d(  "ExternalPikafishEngine", "setOption: " + name + " " + value);
                return true;
            }
        }
        return super.setOption(name, value);
    }
}
