package org.petero.droidfish.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class LocalPipeTest {
    @Test public void closingFullPipeReleasesBlockedWriterAndDiscardsLateLines() throws Exception {
        LocalPipe pipe = new LocalPipe();
        for (int i = 0; i < 10000; i++) pipe.addLine("info");
        Thread writer = new Thread(() -> pipe.addLine("late"));
        writer.setDaemon(true);
        writer.start();
        try {
            long deadline = System.nanoTime() + 2_000_000_000L;
            while (writer.getState() != Thread.State.WAITING && System.nanoTime() < deadline)
                Thread.yield();
            assertTrue("Writer should apply backpressure", writer.isAlive());
            pipe.close();
            writer.join(2000);
            assertFalse("Close must unblock the writer", writer.isAlive());
            pipe.addLine("after close");
            assertNull(pipe.readLine(1));
        } finally { pipe.close(); writer.interrupt(); }
    }

    @Test public void closeWakesAllWaitingReaders() throws Exception {
        LocalPipe pipe = new LocalPipe();
        Thread[] readers = new Thread[3];
        for (int i = 0; i < readers.length; i++) {
            readers[i] = new Thread(() -> assertNull(pipe.readLine()));
            readers[i].setDaemon(true);
            readers[i].start();
        }
        pipe.close();
        for (Thread reader : readers) {
            reader.join(2000);
            assertFalse("Every reader must finish", reader.isAlive());
        }
    }
}
