package com.zfdang.chess.engine;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.zfdang.chess.MainActivity;
import com.zfdang.chess.ManualActivity;
import com.zfdang.chess.GameActivity;
import com.zfdang.chess.R;
import com.zfdang.chess.controllers.GameController;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class PikafishLifecycleTest {
    @Rule public final ActivityScenarioRule<MainActivity> activity = new ActivityScenarioRule<>(MainActivity.class);

    @Test public void installedNetworkIsRepairedEvenWithStaleVerificationMarker() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File network = EngineAssets.prepare(context);
        try {
            // Reproduce a same-size corrupted file plus a marker from the previous installer.
            java.nio.file.Files.write(new File(network.getParent(), "pikafish.nnue.verified").toPath(),
                    (EngineAssets.VERSION + " 50706378 7d13d73569a9b571ba0eb20cf1596247bc2a42738967e61afef6482b231e900e")
                            .getBytes(StandardCharsets.UTF_8));
            int original;
            try (RandomAccessFile file = new RandomAccessFile(network, "rw")) {
                original = file.read();
                file.seek(0); file.write(original ^ 255);
            }
            EngineAssets.prepare(context);
            try (RandomAccessFile file = new RandomAccessFile(network, "r")) { assertEquals(original, file.read()); }
            try (RandomAccessFile file = new RandomAccessFile(network, "rw")) { file.setLength(1); }
            EngineAssets.prepare(context);
            assertEquals(50706378L, network.length());
        } finally { EngineAssets.prepare(context); }
    }

    @Test public void serviceDestructionFinishesActiveAndQueuedSessions() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CountDownLatch connected = new CountDownLatch(1), finished = new CountDownLatch(3);
        AtomicReference<IPikafishService> service = new AtomicReference<>();
        ServiceConnection connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                service.set(IPikafishService.Stub.asInterface(binder)); connected.countDown();
            }
            public void onServiceDisconnected(ComponentName name) {}
        };
        assertTrue(context.bindService(new Intent(context, PikafishService.class), connection, Context.BIND_AUTO_CREATE));
        boolean bound = true;
        try {
            assertTrue(connected.await(10, TimeUnit.SECONDS));
            for (int i = 0; i < 3; i++) {
                assertTrue(service.get().start(new IPikafishCallback.Stub() {
                    public void onLine(String line) {}
                    public void onFinished(String error) { finished.countDown(); }
                }) > 0);
            }
            context.unbindService(connection); bound = false;
            assertTrue("Queued sessions must run their cleanup", finished.await(30, TimeUnit.SECONDS));
        } finally { if (bound) context.unbindService(connection); }
    }

    private GameController resumedController(Class<? extends Activity> type) {
        AtomicReference<GameController> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Activity screen : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                if (!type.isInstance(screen)) continue;
                try {
                    Field field = type.getDeclaredField("controller"); field.setAccessible(true);
                    result.set((GameController) field.get(screen));
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            }
        });
        return result.get();
    }
    private GameController waitForEngine(Class<? extends Activity> type) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            GameController controller = resumedController(type);
            if (controller != null && controller.player.computerLoaded()) return controller;
            Thread.sleep(100);
        }
        throw new AssertionError("Engine did not initialize in " + type.getSimpleName());
    }
    @Test public void gameOpenedFromManualDoesNotWaitBehindManualEngine() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File config = new File(context.getFilesDir(), "pikafish.ini");
        byte[] previous = config.isFile() ? java.nio.file.Files.readAllBytes(config.toPath()) : null;
        java.nio.file.Files.write(config.toPath(), "Threads=1\nHash=32\nMultiPV=3\n".getBytes(StandardCharsets.UTF_8));
        try (ActivityScenario<ManualActivity> manual = ActivityScenario.launch(ManualActivity.class)) {
            GameController manualController = waitForEngine(ManualActivity.class);
            assertEquals("Saved thread count must be respected", "1",
                    manualController.player.getUCIOptions().getOption("Threads").getStringValue());
            manual.onActivity(screen -> screen.findViewById(R.id.gamebt).performClick());
            GameController game = waitForEngine(GameActivity.class);
            assertTrue(game.engineInfo.contains("2026-09-06"));
            assertEquals("1", game.player.getUCIOptions().getOption("Threads").getStringValue());
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity screen : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                    if (screen instanceof GameActivity) screen.finish();
                }
            });
        } finally {
            if (previous != null) java.nio.file.Files.write(config.toPath(), previous);
            else config.delete();
        }
    }
}
