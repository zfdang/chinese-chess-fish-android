package com.zfdang.chess.manuals;

import org.junit.Test;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;
import static org.junit.Assert.*;

public class XQFParserTest {
    private byte[] manual(int size, int version) {
        byte[] bytes = new byte[size];
        bytes[0] = 'X'; bytes[1] = 'Q'; bytes[2] = (byte) version;
        Arrays.fill(bytes, 16, 48, (byte) 0xff);
        return bytes;
    }
    private void putInt(byte[] bytes, int offset, int value) {
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, value);
    }

    @Test public void truncatedFilesAndAnnotationsReturnFailure() {
        assertNull(XQFParser.parse(null));
        for (int length = 0; length < 0x408; length++) assertNull(XQFParser.parse(new byte[length]));
        byte[] valid = manual(0x408, 10);
        assertNotNull(XQFParser.parse(valid));
        for (int size : new int[]{-1, Integer.MIN_VALUE, Integer.MAX_VALUE, 1}) {
            putInt(valid, 0x404, size);
            assertNull(XQFParser.parse(valid));
        }
        byte[] modern = manual(0x408, 11);
        modern[0x402] = 0x20;
        putInt(modern, 0x404, Integer.MIN_VALUE);
        assertNull(XQFParser.parse(modern));
    }

    @Test public void decoderRejectsOversizedReadsWithoutAdvancing() {
        XQFBufferDecoder decoder = new XQFBufferDecoder(new byte[]{1, 2, 3, 4, 5});
        assertEquals(1, decoder.readBytes(1)[0]);
        assertThrows(IllegalArgumentException.class, () -> decoder.readBytes(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> decoder.readBytes(-1));
        assertEquals(4, decoder.remaining());
        assertEquals(0x05040302, decoder.readInt());
        assertThrows(IllegalArgumentException.class, decoder::readInt);
    }

    @Test public void requiredContinuationCannotBeSilentlyTruncated() {
        byte[] bytes = manual(0x410, 10);
        bytes[0x408] = 0x18; bytes[0x409] = 0x21;
        bytes[0x40a] = (byte) 0xf0;
        assertNull(XQFParser.parse(bytes));
    }

    @Test public void deepMoveChainDoesNotOverflowParserStack() {
        int count = 10000;
        byte[] bytes = manual(0x408 + count * 8, 10);
        for (int i = 0; i < count; i++) {
            int offset = 0x408 + i * 8;
            bytes[offset] = 0x18; bytes[offset + 1] = 0x21;
            bytes[offset + 2] = i < count - 1 ? (byte) 0xf0 : 0;
        }
        assertNotNull(XQFParser.parse(bytes));
    }

    @Test public void unicodeAnnotationsRemainTrimmedOnOlderAndroidApis() {
        XQFManual manual = new XQFManual();
        manual.setAnnotation("\u2003 注释 \u2003");
        assertEquals("注释", manual.getAnnotation());
        manual.setAnnotation(null);
        assertNull(manual.getAnnotation());
    }

    @Test public void bundledManualsStillParse() throws Exception {
        try (Stream<Path> files = Files.walk(Path.of("src/main/assets/XQF"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().toLowerCase().endsWith(".xqf"))::iterator) {
                assertNotNull(file.toString(), XQFParser.parse(Files.readAllBytes(file)));
            }
        }
    }
}
