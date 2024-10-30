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
    public void testGetCharByValue() {
        assertEquals(new Character('K'), Piece.getCharByValue(Piece.WSHUAI));
        assertEquals(new Character('k'), Piece.getCharByValue(Piece.BJIANG));
        assertEquals(Piece.EMPTY_CHAR, Piece.getCharByValue(Piece.EMPTY));
    }

    @Test
    public void testGetValueByChar() {
        assertEquals(Piece.WSHUAI, Piece.getValueByChar('K'));
        assertEquals(Piece.BJIANG, Piece.getValueByChar('k'));
        assertEquals(Piece.EMPTY, Piece.getValueByChar(Piece.EMPTY_CHAR));
    }
}