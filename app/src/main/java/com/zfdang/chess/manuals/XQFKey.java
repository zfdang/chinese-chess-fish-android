package com.zfdang.chess.manuals;

public class XQFKey {
    // keyXY, keyXYf, keyXYt, keyRMKSize are all saved as int, since java does not support unsigned byte well
    private int keyXY;      // Chess piece position encryption key
    private int keyXYf;     // Move source position encryption key
    private int keyXYt;     // Move target position encryption key
    private int keyRMKSize;  // Annotation size encryption key

    private byte[] fKeyBytes;
    private byte[] f32Keys;

    public XQFKey() {
        f32Keys = "[(C) Copyright Mr. Dong Shiwei.]".getBytes();
    }

    // Getters and setters
    public int getKeyXY() { return keyXY; }
    public void setKeyXY(int keyXY) { this.keyXY = keyXY; }
    
    public int getKeyXYf() { return keyXYf; }
    public void setKeyXYf(int keyXYf) { this.keyXYf = keyXYf; }
    
    public int getKeyXYt() { return keyXYt; }
    public void setKeyXYt(int keyXYt) { this.keyXYt = keyXYt; }
    
    public int getKeyRMKSize() { return keyRMKSize; }
    public void setKeyRMKSize(int keyRMKSize) { this.keyRMKSize = keyRMKSize; }
    
    public byte[] getFKeyBytes() { return fKeyBytes; }
    public void setFKeyBytes(byte[] fKeyBytes) { this.fKeyBytes = fKeyBytes; }
    
    public byte[] getF32Keys() { return f32Keys; }
    public void setF32Keys(byte[] f32Keys) { this.f32Keys = f32Keys; }

    public void initF32Keys() {
        for (int i = 0; i < f32Keys.length; i++) {
            f32Keys[i] &= fKeyBytes[i % 4];
        }
    }

    // toString method, show fKeyBytes & f32Keys as hex strings
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("XQFKey{");
        sb.append("keyXY=").append(keyXY).append(", ");
        sb.append("keyXYf=").append(keyXYf).append(", ");
        sb.append("keyXYt=").append(keyXYt).append(", ");
        sb.append("keyRMKSize=").append(keyRMKSize).append(", ");
        sb.append("fKeyBytes=");
        if (fKeyBytes == null) {
            sb.append("null, ");
        } else {
            sb.append('[');
            for (int i = 0; i < fKeyBytes.length; ++i) {
                sb.append(Integer.toHexString(fKeyBytes[i] & 0xFF));
                if (i < fKeyBytes.length - 1) {
                    sb.append(" ");
                }
            }
            sb.append(']').append(", ");
        }
        sb.append("f32Keys=");
        if (f32Keys == null) {
            sb.append("null");
        } else {
            sb.append('[');
            for (int i = 0; i < f32Keys.length; ++i) {
                sb.append(Integer.toHexString(f32Keys[i] & 0xFF));
                if (i < f32Keys.length - 1) {
                    sb.append(" ");
                }
            }
            sb.append(']');
        }
        sb.append('}');
        return sb.toString();
    }

}