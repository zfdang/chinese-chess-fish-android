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

    public String readString(int size, Charset set) {
        byte[] buff = read(size);
        try {
            return new String(buff, set);
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

    // toString method, show buffer as hex string
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("XQFBufferDecoder{");
        sb.append("buffer=\n");
        if (buffer == null) {
            sb.append("null, ");
        } else {
            sb.append('[');
            for (int i = 0; i < buffer.length; ++i) {
                sb.append(Integer.toHexString(buffer[i] & 0xFF));
                if( i % 15 == 0 && i > 0) {
                    sb.append("\n");
                } else if (i < buffer.length - 1) {
                    sb.append(" ");
                }
            }
            sb.append("], ");
        }
        sb.append("index=").append(index).append(", ");
        sb.append("length=").append(length).append("}");
        return sb.toString();
    }

}