package org.petero.droidfish.engine;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import com.zfdang.chess.ChessApp;
import com.zfdang.chess.engine.EngineAssets;
import com.zfdang.chess.engine.IPikafishCallback;
import com.zfdang.chess.engine.IPikafishService;
import com.zfdang.chess.engine.PikafishService;
import org.petero.droidfish.player.EngineListener;
import org.petero.droidfish.utils.LocalPipe;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** UCI transport over Binder to a JNI library, without spawning an executable. */
public final class PikafishNativeEngine extends UCIEngineBase {
    private final Context context = ChessApp.getContext();
    private final EngineListener listener;
    private final LocalPipe output = new LocalPipe();
    private final List<String> pending = new ArrayList<>();
    private IPikafishService service;
    private long session;
    private volatile boolean closed;
    private boolean bound;

    public PikafishNativeEngine(EngineListener listener) { this.listener = listener; }
    private final IPikafishCallback.Stub callback = new IPikafishCallback.Stub() {
        @Override public void onLine(String line) { if (!closed) output.addLine(line); }
        @Override public void onFinished(String error) {
            if (!closed) fail(error.isEmpty() ? "Pikafish engine stopped" : error);
        }
    };
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            synchronized (PikafishNativeEngine.this) {
                if (closed) return;
                service = IPikafishService.Stub.asInterface(binder);
                try {
                    session = service.start(callback);
                    for (String command : pending) service.command(session, command);
                    pending.clear();
                } catch (RemoteException | RuntimeException error) { fail(error.toString()); }
            }
        }
        @Override public void onServiceDisconnected(ComponentName name) { fail("Pikafish service terminated"); }
        @Override public void onBindingDied(ComponentName name) { fail("Pikafish service binding died"); }
        @Override public void onNullBinding(ComponentName name) { fail("Pikafish service unavailable"); }
    };
    private void fail(String message) {
        if (!closed) { listener.reportEngineError(message); shutDown(); }
    }
    @Override protected synchronized void startProcess() {
        try {
            bound = context.bindService(new Intent(context, PikafishService.class), connection, Context.BIND_AUTO_CREATE);
            if (!bound) fail("Cannot bind Pikafish service");
        } catch (RuntimeException error) { fail(error.toString()); }
    }
    @Override public synchronized void writeLineToEngine(String line) {
        if (closed) return;
        if (service == null) { pending.add(line); return; }
        try { service.command(session, line); } catch (RemoteException error) { fail(error.toString()); }
    }
    @Override public String readLineFromEngine(int timeoutMillis) { return output.readLine(timeoutMillis); }
    @Override public synchronized void shutDown() {
        if (closed) return;
        closed = true;
        pending.clear();
        output.close();
        if (service != null) {
            try { service.shutdown(session); } catch (RemoteException ignored) { }
            service = null;
        }
        if (bound) { context.unbindService(connection); bound = false; }
    }
    @Override public boolean setOption(String name, String value) {
        if ("evalfile".equals(name.toLowerCase(Locale.ROOT))) {
            value = EngineAssets.networkFile(context).getAbsolutePath();
        }
        return super.setOption(name, value);
    }
    @Override protected boolean editableOption(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "numapolicy": case "threads": case "hash": case "clear hash":
            case "ponder": case "multipv": case "move overhead": case "evalfile":
                return true;
            default: return false;
        }
    }
    @Override protected File getIniFile() {
        File config = new File(context.getFilesDir(), "pikafish.ini");
        if (!config.exists()) {
            Properties defaults = new Properties();
            defaults.setProperty("Threads", "4");
            defaults.setProperty("Hash", "512");
            defaults.setProperty("MultiPV", "5");
            defaults.setProperty("EvalFile", EngineAssets.networkFile(context).getAbsolutePath());
            try (FileOutputStream out = new FileOutputStream(config)) { defaults.store(out, null); }
            catch (IOException error) { listener.reportEngineError(error.toString()); }
        }
        return config;
    }
    @Override public boolean configOk(EngineConfig config) { return isConfigOk && !closed; }
}
