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

import android.util.Log;

import com.zfdang.chess.BuildConfig;
import com.zfdang.chess.ChessApp;

import org.petero.droidfish.player.EngineListener;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

/**
 * Stockfish engine running as process, started from assets resource.
 * originally, this class will copy the engine file from assets to /data/data/org.petero.droidfish/files
 * but not we will use the engine file in /data/lib directory directly
 * this is because Android Q restricts access to the app's private directories
 * see https://withme.skullzbones.com/blog/programming/execute-native-binaries-android-q-no-root/
 * so you will see lots of legacy codes here
 */
public class PikafishExternalEngine extends ExternalEngine {
    private static final String[] networkAsssetFiles = {"libpikafish.nnue.so", "libpikafish.ini.so"};
    private static final String[] networkOptions = {"evalfile"};
    private final File[] networkFiles = {null, null}; // Full path of the copied network files

    // PikafishEngineFile: the name of the engine file in /data/lib directory, two flavors:
    // pikafish-armv8
    // pikafish-armv8-dotprod
    private static final String PikafishEngineFile = BuildConfig.PIKAFISH_ENGINE_FILE;

    private final String nativeLibraryDir = ChessApp.getContext().getApplicationInfo().nativeLibraryDir;

    public PikafishExternalEngine(String workDir, EngineListener listener) {
        super("pikafish", workDir, listener);
    }

    @Override
    protected File getIniFile() {
        return new File(nativeLibraryDir, networkAsssetFiles[1]);
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
        if (super.editableOption(name))
            return true;
        // pikafish可修改的选项
        String[] editable = {"numapolicy", "threads", "hash", "clear hash", "ponder", "multipv", "move overhead", "skill level",
                "mate threat depth", "repetition rule", "draw rule", "rule60maxply", "scoretype", "lu_output", "evalfile"};
        return Arrays.asList(editable).contains(name);
    }


    @Override
    protected String copyFile(File from, File exeDir) throws IOException {
        // now this method does not copy file any longer,
        // it just use files in data/lib/ folder
        File to = new File(nativeLibraryDir, PikafishEngineFile);

        // assign networkAsssetFiles to networkFiles
        for (int i = 0; i < networkAsssetFiles.length; i++) {
            networkFiles[i] = new File(nativeLibraryDir, networkAsssetFiles[i]);
        }

        return to.getAbsolutePath();
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
