package com.zfdang.chess.controllers;

import com.zfdang.chess.gamelogic.PvInfo;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class GameControllerTest {
    private ArrayList<PvInfo> pvs(int count) {
        ArrayList<PvInfo> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(new PvInfo(0, 0, 0, 0, 0, 0, 0, 0,
                false, false, false, new ArrayList<>()));
        return result;
    }

    @Test public void publicationDoesNotMutatePreviousSnapshotOrRetainSourceList() {
        GameController controller = new GameController(null, null);
        ArrayList<PvInfo> source = pvs(3);
        controller.notifyPV(1, null, source, null);
        List<PvInfo> snapshot = controller.multiPVs;
        source.clear();
        controller.notifyPV(1, null, pvs(1), null);
        assertEquals(3, snapshot.size());
        assertEquals(1, controller.multiPVs.size());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @Test public void concurrentPublicationLeavesIterationStable() throws Exception {
        GameController controller = new GameController(null, null);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < 5000; i++) controller.notifyPV(1, null, pvs(i % 5), null);
            } catch (Throwable error) { failure.set(error); }
        });
        writer.start();
        while (writer.isAlive()) {
            List<PvInfo> snapshot = controller.multiPVs;
            int count = 0;
            for (PvInfo pv : snapshot) { assertNotNull(pv); count++; }
            assertEquals(snapshot.size(), count);
        }
        writer.join();
        assertNull(failure.get());
    }

    @Test public void callbacksAfterCloseCannotPublishOrUseGui() {
        GameController controller = new GameController(null, null);
        controller.notifyPV(1, null, pvs(2), null);
        List<PvInfo> beforeClose = controller.multiPVs;
        controller.close();
        controller.close();
        controller.notifyPV(1, null, pvs(3), null);
        controller.notifySearchResult(1, "a0a1", null);
        controller.notifyEvalResult(1, 2f);
        assertSame(beforeClose, controller.multiPVs);
    }
}
