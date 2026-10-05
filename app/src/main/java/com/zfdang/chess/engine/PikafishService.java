package com.zfdang.chess.engine;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import com.zfdang.chess.BuildConfig;
import org.petero.droidfish.engine.EngineUtil;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bound, non-exported service; native fatal errors cannot terminate the UI process. */
public final class PikafishService extends Service {
    private static final boolean NEEDS_DOTPROD = "pikafish_dotprod".equals(BuildConfig.PIKAFISH_LIBRARY);
    private final ConcurrentHashMap<Long, IPikafishCallback> sessions = new ConcurrentHashMap<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "Pikafish-UCI"));
    private boolean destroyed;
    private final IPikafishService.Stub binder = new IPikafishService.Stub() {
        @Override public long start(IPikafishCallback callback) { return startSession(callback); }
        @Override public void command(long session, String command) { NativeBridge.command(session, command); }
        @Override public void shutdown(long session) { NativeBridge.stop(session); }
    };

    private static void finished(IPikafishCallback callback, String error) {
        try { callback.onFinished(error); } catch (RemoteException ignored) { }
    }

    // Serialize acceptance with onDestroy so no session can slip past shutdown's stop loop.
    private synchronized long startSession(IPikafishCallback callback) {
        if (callback == null) return 0;
        if (destroyed) {
            finished(callback, "Pikafish service is shutting down");
            return 0;
        }
        final long id;
        try {
            // NativeBridge's static initializer loads the optimized library. Check with the
            // baseline library first: even library constructors may use dot-product instructions.
            if (NEEDS_DOTPROD && !EngineUtil.isSimdSupported()) {
                finished(callback, "This device lacks the ARM dot-product extension;"
                        + " install the standard ARMv8 build instead.");
                return 0;
            }
            id = NativeBridge.create(line -> {
                try { callback.onLine(line); } catch (RemoteException ignored) { }
            });
        } catch (RuntimeException | LinkageError failure) {
            finished(callback, failure.toString());
            return 0;
        }
        IBinder.DeathRecipient death = () -> NativeBridge.stop(id);
        try {
            callback.asBinder().linkToDeath(death, 0);
        } catch (RemoteException deadClient) {
            NativeBridge.release(id);
            return 0;
        }
        sessions.put(id, callback);
        try {
            worker.execute(() -> {
                String error = "";
                try {
                    File network = EngineAssets.prepare(PikafishService.this);
                    NativeBridge.run(id, network.getParent());
                } catch (Exception | LinkageError failure) { error = failure.toString(); }
                finally {
                    NativeBridge.release(id);
                    sessions.remove(id);
                    callback.asBinder().unlinkToDeath(death, 0);
                    finished(callback, error);
                }
            });
        } catch (RuntimeException rejected) {
            NativeBridge.release(id);
            sessions.remove(id);
            callback.asBinder().unlinkToDeath(death, 0);
            finished(callback, rejected.toString());
            return 0;
        }
        return id;
    }
    @Override public IBinder onBind(Intent intent) { return binder; }
    @Override public synchronized void onDestroy() {
        destroyed = true;
        for (long id : sessions.keySet()) NativeBridge.stop(id);
        // Queued sessions must still reach their finally blocks to release native/global refs.
        // shutdownNow would discard those tasks; quit above stops the active native UCI loop.
        worker.shutdown();
        super.onDestroy();
    }
}
