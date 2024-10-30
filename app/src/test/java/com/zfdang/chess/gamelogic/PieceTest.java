package com.zfdang.chess.gamelogic;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PieceTest {

    @Test
    public void testIsRed() {
        assertTrue(Piece.isRed(Piece.WSHUAI));
        assertTrue(Piece.isRed(Piece.WBING));
        assertFalse(Piece.isRed(Piece.BJIANG));
        assertFalse(Piece.isRed(Piece.EMPTY));
    }

    @Test
    public void testIsBlack() {
        assertTrue(Piece.isBlack(Piece.BJIANG));
        assertTrue(Piece.isBlack(Piece.BZU));
        assertFalse(Piece.isBlack(Piece.WSHUAI));
        assertFalse(Piece.isBlack(Piece.EMPTY));
    }

    @Test
    public void testSwapColor() {
        assertEquals(Piece.BJIANG, Piece.swapColor(Piece.WSHUAI));
        assertEquals(Piece.WSHUAI, Piece.swapColor(Piece.BJIANG));
        assertEquals(Piece.EMPTY, Piece.swapColor(Piece.EMPTY));
    }

    @Test
    public void testGetByteByValue() {
        assertEquals((byte) 'K', Piece.getByteByValue(Piece.WSHUAI));
        assertEquals((byte) 'k', Piece.getByteByValue(Piece.BJIANG));
        assertEquals(Piece.EMPTY_BYTE, Piece.getByteByValue(Piece.EMPTY));
    }

    @Test
    public void testGetValueByByte() {
        assertEquals(Piece.WSHUAI, Piece.getValueByByte((byte) 'K'));
        assertEquals(Piece.BJIANG, Piece.getValueByByte((byte) 'k'));
        assertEquals(Piece.EMPTY, Piece.getValueByByte(Piece.EMPTY_BYTE));
    }
}