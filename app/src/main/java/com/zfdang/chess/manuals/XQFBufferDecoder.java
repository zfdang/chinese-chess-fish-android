package com.zfdang.chess.manuals;

import java.nio.charset.Charset;
import java.util.Arrays;

public class XQFBufferDecoder {
    private byte[] buffer;
    private int index;
    private int length;

    public XQFBufferDecoder(byte[] buffer) {
        this.buffer = buffer;
        this.index = 0;
        this.length = buffer.length;
    }

    private byte[] read(int size) {
        int start = index;
        int stop = Math.min(index + size, length);
        index = stop;
        return Arrays.copyOfRange(buffer, start, stop);
    }

    public String readString(int size, String encoding) {
        byte[] buff = read(size);
        try {
            return new String(buff, Charset.forName(encoding));
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] readBytes(int size) {
        return read(size);
    }

    public int readInt() {
        byte[] bytes = readBytes(4);
        return bytes[0] + (bytes[1] << 8) + (bytes[2] << 16) + (bytes[3] << 24);
    }
}