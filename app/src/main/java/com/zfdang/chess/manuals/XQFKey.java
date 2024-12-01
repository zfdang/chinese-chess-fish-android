package com.zfdang.chess.manuals;

public class XQFKey {
    private byte keyXY;      // Chess piece position encryption key
    private byte keyXYf;     // Move source position encryption key
    private byte keyXYt;     // Move target position encryption key
    private int keyRMKSize;  // Annotation size encryption key
    private byte[] fKeyBytes;
    private byte[] f32Keys;

    public XQFKey() {
        f32Keys = "[(C) Copyright Mr. Dong Shiwei.]".getBytes();
    }

    // Getters and setters
    public byte getKeyXY() { return keyXY; }
    public void setKeyXY(byte keyXY) { this.keyXY = keyXY; }
    
    public byte getKeyXYf() { return keyXYf; }
    public void setKeyXYf(byte keyXYf) { this.keyXYf = keyXYf; }
    
    public byte getKeyXYt() { return keyXYt; }
    public void setKeyXYt(byte keyXYt) { this.keyXYt = keyXYt; }
    
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
}