package com.zfdang.chess.engine;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import com.zfdang.chess.MainActivity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.petero.droidfish.engine.EngineConfig;
import org.petero.droidfish.engine.PikafishNativeEngine;
import org.petero.droidfish.player.EngineListener;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Uses the packaged JNI libraries and pinned NNUE on an actual Android device. */
@RunWith(AndroidJUnit4.class)
public class PikafishIntegrationTest {
    // OEM background freezers may suspend an instrumentation process with no visible activity.
    @Rule public final ActivityScenarioRule<MainActivity> activity = new ActivityScenarioRule<>(MainActivity.class);
    private final AtomicReference<String> error = new AtomicReference<>();
    private PikafishNativeEngine connect() {
        PikafishNativeEngine engine = new PikafishNativeEngine(new EngineListener() {
            public void reportEngineError(String message) { error.set(message); }
            public void notifyEngineName(String name) {}
            public void notifySearchResult(int id, String move, String ponder) {}
            public void notifyEngineInitialized() {}
            public void notifyEvalResult(int id, float score) {}
        });
        engine.initialize(); engine.initConfig(new EngineConfig()); engine.writeLineToEngine("uci");
        long deadline = System.currentTimeMillis() + 30000;
        boolean correctVersion = false;
        while (System.currentTimeMillis() < deadline) {
            String line = engine.readLineFromEngine(1000);
            android.util.Log.i("PikafishIntegration", "handshake: " + line);
            assertNotNull("Engine disconnected: " + error.get(), line);
            if (line.startsWith("option ")) engine.registerOption(line.split("\\s+"));
            if (line.startsWith("id name ")) correctVersion = line.contains("2026-09-06");
            if (line.equals("uciok")) {
                assertTrue("Pinned engine version", correctVersion);
                assertTrue("Options must survive every engine restart", engine.getUCIOptions().contains("MultiPV"));
                engine.setOption("Hash", 32); engine.setOption("Threads", 2);
                engine.setOption("EvalFile", "pikafish.nnue");
                engine.writeLineToEngine("isready");
                waitFor(engine, "readyok");
                return engine;
            }
        }
        engine.shutDown();
        throw new AssertionError("UCI handshake timeout: " + error.get());
    }
    private String waitFor(PikafishNativeEngine engine, String prefix) {
        long deadline = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < deadline) {
            String line = engine.readLineFromEngine(1000);
            android.util.Log.i("PikafishIntegration", "wait " + prefix + ": " + line);
            assertNotNull("Engine disconnected: " + error.get(), line);
            if (line.startsWith(prefix)) return line;
        }
        throw new AssertionError("Timeout waiting for " + prefix);
    }
    @Test public void pinnedNetworkSupportsSearchMultiPvAndEvaluation() {
        PikafishNativeEngine engine = connect();
        try {
            engine.setOption("MultiPV", 3);
            engine.writeLineToEngine("position startpos");
            engine.writeLineToEngine("go depth 8");
            boolean[] pvs = new boolean[3];
            long deadline = System.currentTimeMillis() + 30000;
            String bestMove = null;
            while (System.currentTimeMillis() < deadline) {
                String line = engine.readLineFromEngine(1000);
                android.util.Log.i("PikafishIntegration", "search: " + line);
                assertNotNull(line);
                for (int i = 0; i < 3; i++) if (line.contains(" multipv " + (i + 1) + " ")) pvs[i] = true;
                if (line.startsWith("bestmove ")) { bestMove = line; break; }
            }
            assertNotNull(bestMove);
            assertTrue("Missing PVs: " + java.util.Arrays.toString(pvs), pvs[0] && pvs[1] && pvs[2]);
            engine.writeLineToEngine("eval");
            String evaluation = waitFor(engine, "Final evaluation");
            float score = Float.parseFloat(evaluation.trim().split("\\s+")[2]);
            assertTrue(Float.isFinite(score));
            assertNull(error.get());
        } finally { engine.shutDown(); }
    }
    @Test public void stopAndRepeatedRestartReleaseSessions() {
        for (int i = 0; i < 3; i++) {
            PikafishNativeEngine engine = connect();
            try {
                engine.writeLineToEngine("position startpos"); engine.writeLineToEngine("go infinite");
                waitFor(engine, "info depth 3 ");
                engine.writeLineToEngine("stop");
                assertTrue(waitFor(engine, "bestmove ").matches("bestmove [a-i][0-9][a-i][0-9].*"));
                assertNull(error.get());
            } finally { engine.shutDown(); }
            engine.shutDown();
            assertNull(engine.readLineFromEngine(1));
        }
    }
    @Test public void invalidPositionCannotTerminateUiAndNextEngineCanStart() {
        PikafishNativeEngine engine = connect();
        engine.writeLineToEngine("position fen invalid");
        long deadline = System.currentTimeMillis() + 15000;
        while (engine.readLineFromEngine(500) != null && System.currentTimeMillis() < deadline) { }
        assertNotNull("Service death should be reported", error.get());
        engine.shutDown();
        error.set(null);
        PikafishNativeEngine next = connect();
        try { next.writeLineToEngine("position startpos"); next.writeLineToEngine("go depth 3"); waitFor(next, "bestmove "); }
        finally { next.shutDown(); }
    }
}
