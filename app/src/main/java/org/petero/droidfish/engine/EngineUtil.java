/*
    DroidFish - An Android chess program.
    Copyright (C) 2011-2012  Peter Österlund, peterosterlund2@gmail.com

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

import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class EngineUtil {
    static {
        System.loadLibrary("nativeutil");
    }

    /**
     * Remove characters from s that are not safe to use in a filename.
     */
    private static String sanitizeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (((ch >= 'A') && (ch <= 'Z')) ||
                    ((ch >= 'a') && (ch <= 'z')) ||
                    ((ch >= '0') && (ch <= '9')))
                sb.append(ch);
            else
                sb.append('_');
        }
        return sb.toString();
    }

    /**
     * Executes chmod 744 exePath.
     */
    static native boolean chmod(String exePath);

    /**
     * Change the priority of a process.
     */
    static native void reNice(int pid, int prio);

    /**
     * Return true if the required SIMD instructions are supported by the CPU.
     */
    static native boolean isSimdSupported();

    /**
     * For synchronizing non thread safe native calls.
     */
    public static final Object nativeLock = new Object();
}
