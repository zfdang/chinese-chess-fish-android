package org.petero.droidfish.player;

import org.junit.Test;
import org.petero.droidfish.engine.UCIEngine;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ComputerPlayerTest {
    private static class Listener implements EngineListener {
        float score;
        int calls;
        int id;
        public void reportEngineError(String message) {}
        public void notifyEngineName(String name) {}
        public void notifySearchResult(int id, String move, String ponder) {}
        public void notifyEngineInitialized() {}
        public void notifyEvalResult(int id, float score) {
            this.id = id;
            this.score = score;
            calls++;
        }
    }

    private static Field field(String name) throws Exception {
        Field field = ComputerPlayer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private float evaluate(String output) throws Exception {
        Listener listener = new Listener();
        ComputerPlayer player = new ComputerPlayer(listener, null, null);
        EngineState state = (EngineState) field("engineState").get(player);
        state.setState(EngineStateValue.EVAL);
        state.searchId = 42;
        Method process = ComputerPlayer.class.getDeclaredMethod("processEngineOutput", UCIEngine.class, String.class);
        process.setAccessible(true);
        process.invoke(player, null, output);
        assertEquals(1, listener.calls);
        assertEquals(42, listener.id);
        assertEquals(EngineStateValue.IDLE, state.state);
        assertFalse(player.computerBusy());
        return listener.score;
    }

    @Test public void unavailableEvaluationCompletesWithoutInventingZero() throws Exception {
        assertTrue(Float.isNaN(evaluate("Final evaluation: none (in check)")));
    }

    @Test public void validEvaluationsIncludeZeroAndNegativeScores() throws Exception {
        assertEquals(0f, evaluate("Final evaluation: +0.00 (white side)"), 0f);
        assertEquals(-1.25f, evaluate("Final evaluation: -1.25 (white side)"), 0f);
    }

    @Test public void malformedAndNonFiniteEvaluationsAreUnknown() throws Exception {
        assertTrue(Float.isNaN(evaluate("Final evaluation: invalid")));
        assertTrue(Float.isNaN(evaluate("Final evaluation: Infinity")));
    }

    @Test public void closeReleasesEngineAndMonitorOnlyOnce() throws Exception {
        ComputerPlayer player = new ComputerPlayer(new Listener(), null, null);
        AtomicInteger shutdowns = new AtomicInteger();
        UCIEngine engine = (UCIEngine) Proxy.newProxyInstance(UCIEngine.class.getClassLoader(),
                new Class<?>[]{UCIEngine.class}, (proxy, method, args) -> {
                    if (method.getName().equals("shutDown")) shutdowns.incrementAndGet();
                    return null;
                });
        Thread monitor = new Thread();
        field("uciEngine").set(player, engine);
        field("engineMonitor").set(player, monitor);
        field("searchRequest").set(player, SearchRequest.startRequest(1, "test"));
        player.close();
        player.close();
        assertEquals(1, shutdowns.get());
        assertTrue(monitor.isInterrupted());
        assertNull(field("uciEngine").get(player));
        assertNull(field("engineMonitor").get(player));
        assertNull(field("searchRequest").get(player));
        assertEquals(EngineStateValue.DEAD, ((EngineState) field("engineState").get(player)).state);
    }

    @Test public void internalEngineRestartPreservesPendingRequest() throws Exception {
        ComputerPlayer player = new ComputerPlayer(new Listener(), null, null);
        SearchRequest request = SearchRequest.startRequest(1, "test");
        field("searchRequest").set(player, request);
        player.shutdownEngine();
        assertSame(request, field("searchRequest").get(player));
    }
}
