package com.zfdang.chess.engine;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bound, non-exported service; native fatal errors cannot terminate the UI process. */
public final class PikafishService extends Service {
    private final ConcurrentHashMap<Long, IPikafishCallback> sessions = new ConcurrentHashMap<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "Pikafish-UCI"));
    private final IPikafishService.Stub binder = new IPikafishService.Stub() {
        @Override public long start(IPikafishCallback callback) {
            long id = NativeBridge.create(line -> {
                try { callback.onLine(line); } catch (RemoteException ignored) { }
            });
            sessions.put(id, callback);
            worker.execute(() -> {
                String error = "";
                try {
                    File network = EngineAssets.prepare(PikafishService.this);
                    NativeBridge.run(id, network.getParent());
                } catch (Exception | LinkageError failure) { error = failure.toString(); }
                finally {
                    NativeBridge.release(id);
                    sessions.remove(id);
                    try { callback.onFinished(error); } catch (RemoteException ignored) { }
                }
            });
            return id;
        }
        @Override public void command(long session, String command) { NativeBridge.command(session, command); }
        @Override public void shutdown(long session) { NativeBridge.stop(session); }
    };
    @Override public IBinder onBind(Intent intent) { return binder; }
    @Override public void onDestroy() {
        for (long id : sessions.keySet()) NativeBridge.stop(id);
        worker.shutdownNow();
        super.onDestroy();
    }
}
