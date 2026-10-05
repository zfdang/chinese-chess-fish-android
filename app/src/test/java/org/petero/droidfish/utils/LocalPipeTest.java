package org.petero.droidfish.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class LocalPipeTest {
    private void awaitBlocked(Thread thread) {
        long deadline = System.nanoTime() + 2_000_000_000L;
        while (thread.getState() != Thread.State.WAITING && System.nanoTime() < deadline)
            Thread.yield();
        assertEquals("Thread must be waiting before close", Thread.State.WAITING, thread.getState());
    }

    @Test public void closingFullPipeReleasesBlockedWriterAndDiscardsLateLines() throws Exception {
        LocalPipe pipe = new LocalPipe();
        for (int i = 0; i < 10000; i++) pipe.addLine("info");
        Thread writer = new Thread(() -> pipe.addLine("late"));
        writer.setDaemon(true);
        writer.start();
        try {
            awaitBlocked(writer);
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
        try {
            for (Thread reader : readers) awaitBlocked(reader);
            pipe.close();
            for (Thread reader : readers) {
                reader.join(2000);
                assertFalse("Every reader must finish", reader.isAlive());
            }
        } finally {
            pipe.close();
            for (Thread reader : readers) reader.interrupt();
        }
    }
}
