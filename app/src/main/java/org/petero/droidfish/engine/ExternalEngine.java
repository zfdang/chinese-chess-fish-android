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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;

import com.zfdang.chess.ChessApp;
import com.zfdang.chess.R;

import org.petero.droidfish.player.EngineListener;
import org.petero.droidfish.utils.LocalPipe;

import android.content.Context;
import android.util.Log;

/**
 * Engine running as a process started from an external resource.
 */
public class ExternalEngine extends UCIEngineBase {
    protected final Context context;

    private File engineFileName;
    private File engineWorkDir;
    private final EngineListener listener;
    private Process engineProc;
    private Thread startupThread;
    private Thread exitThread;
    private Thread stdInThread;
    private Thread stdErrThread;
    private final LocalPipe inLines;
    private boolean startedOk;
    private boolean isRunning;

    public ExternalEngine(String engine, String workDir, EngineListener listener) {
        context = ChessApp.getContext();
        this.listener = listener;
        engineFileName = new File(engine);
        engineWorkDir = new File(workDir);
        engineProc = null;
        startupThread = null;
        exitThread = null;
        stdInThread = null;
        stdErrThread = null;
        inLines = new LocalPipe();
        startedOk = false;
        isRunning = false;
    }


    @Override
    protected void startProcess() {
        try {
            // https://stackoverflow.com/questions/60370424/permission-is-denied-using-android-q-ffmpeg-error-13-permission-denied
            // https://withme.skullzbones.com/blog/programming/execute-native-binaries-android-q-no-root/

            // originally, this method will copy binary from assets/ to data folder
            // but now we will launch binary from data/lib directly
            String exePath = copyFile(null, null);
            Log.d("ExternalEngine", "Starting engine: " + exePath);

            engineWorkDir = new File(exePath).getParentFile();

            ProcessBuilder pb = new ProcessBuilder(exePath);
            pb.directory(engineWorkDir);
            synchronized (EngineUtil.nativeLock) {
                engineProc = pb.start();
            }
            reNice();

            startupThread = new Thread(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    return;
                }
                if (startedOk && isRunning && !isConfigOk)
                    listener.reportEngineError(context.getString(R.string.uci_protocol_error));
            });
            startupThread.start();

            exitThread = new Thread(() -> {
                try {
                    Process ep = engineProc;
                    if (ep != null)
                        ep.waitFor();
                    isRunning = false;
                    if (!startedOk)
                        listener.reportEngineError(context.getString(R.string.failed_to_start_engine));
                    else {
                        listener.reportEngineError(context.getString(R.string.engine_terminated));
                    }
                } catch (InterruptedException ignore) {
                }
            });
            exitThread.start();

            // Start a thread to read stdin
            stdInThread = new Thread(() -> {
                Process ep = engineProc;
                if (ep == null)
                    return;
                InputStream is = ep.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr, 8192);
                String line;
                try {
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        if (Thread.currentThread().isInterrupted())
                            return;
                        synchronized (inLines) {
                            inLines.addLine(line);
                            if (first) {
                                startedOk = true;
                                isRunning = true;
                                first = false;
                            }
                        }
                    }
                } catch (IOException ignore) {
                }
                inLines.close();
            });
            stdInThread.start();

            // Start a thread to ignore stderr
            stdErrThread = new Thread(() -> {
                byte[] buffer = new byte[128];
                while (true) {
                    Process ep = engineProc;
                    if ((ep == null) || Thread.currentThread().isInterrupted())
                        return;
                    try {
                        int len = ep.getErrorStream().read(buffer, 0, 1);
                        if (len < 0)
                            break;
                    } catch (IOException e) {
                        return;
                    }
                }
            });
            stdErrThread.start();
        } catch (IOException | SecurityException ex) {
            listener.reportEngineError(ex.getMessage());
            Log.d("ExternalEngine", "Failed to start engine", ex);
        }
    }

    /**
     * Try to lower the engine process priority.
     */
    private void reNice() {
        try {
            java.lang.reflect.Field f = engineProc.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            int pid = f.getInt(engineProc);
            EngineUtil.reNice(pid, 10);
        } catch (Throwable ignore) {
        }
    }

    private boolean optionsInitialized = false;

    @Override
    public void initConfig(EngineConfig engineConfig) {
        super.initConfig(engineConfig);
        optionsInitialized = true;
    }

    @Override
    public boolean configOk(EngineConfig engineConfig) {
        return optionsInitialized;
    }

    @Override
    protected File getIniFile() {
        return new File(engineFileName.getAbsolutePath() + ".ini");
    }

    @Override
    public String readLineFromEngine(int timeoutMillis) {
        String ret = inLines.readLine(timeoutMillis);
        if (ret == null)
            return null;
        if (ret.length() > 0) {
            Log.d("ExternalEngine", "Read from engine: " + ret);
        }
        return ret;
    }

    // XXX Writes should be handled by separate thread.
    @Override
    public void writeLineToEngine(String data) {
        data += "\n";
        try {
            Process ep = engineProc;
            if (ep != null) {
                ep.getOutputStream().write(data.getBytes());
                ep.getOutputStream().flush();
            }
            Log.d("ExternalEngine", "Wrote to engine: " + data);
        } catch (IOException ignore) {
            Log.d("ExternalEngine", "Failed to write to engine: " + ignore.getMessage());
        }
    }

    @Override
    public void shutDown() {
        if (startupThread != null)
            startupThread.interrupt();
        if (exitThread != null)
            exitThread.interrupt();
        super.shutDown();
        if (engineProc != null) {
            for (int i = 0; i < 25; i++) {
                try {
                    engineProc.exitValue();
                    break;
                } catch (IllegalThreadStateException e) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
            engineProc.destroy();
        }
        engineProc = null;
        if (stdInThread != null)
            stdInThread.interrupt();
        if (stdErrThread != null)
            stdErrThread.interrupt();
    }

    protected String copyFile(File from, File exeDir) throws IOException {
        File to = new File(exeDir, from.getName());
        if (to.exists() && (from.length() == to.length()) && (from.lastModified() == to.lastModified()))
            return to.getAbsolutePath();
        try (FileInputStream fis = new FileInputStream(from);
             FileChannel inFC = fis.getChannel();
             FileOutputStream fos = new FileOutputStream(to);
             FileChannel outFC = fos.getChannel()) {
            long cnt = outFC.transferFrom(inFC, 0, inFC.size());
            if (cnt < inFC.size())
                throw new IOException("File copy failed");
        } finally {
            to.setLastModified(from.lastModified());
        }
        return to.getAbsolutePath();
    }
}
