package com.zfdang.chess.gamelogic;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BoardTest {

    @Test
    public void testToFENString() {
        Board board = new Board();
        String expectedFEN = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1";
        String actualFEN = board.toFENString();
        assertEquals(expectedFEN, actualFEN);
    }

    @Test
    public void testRestoreFromFEN() {
        Board board = new Board();
        board.randomizePieces();
        String fenString = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1";
        boolean result = board.restoreFromFEN(fenString);
        assertTrue(result);
        assertEquals(fenString, board.toFENString());
    }

    public void testGetPieceByPosition() {
        Board board = new Board();

        assertEquals(Piece.BJU, board.getPieceByPosition(0, 0));

        assertEquals(Piece.BJIANG, board.getPieceByPosition(0, 4));

        assertEquals(Piece.WSHUAI, board.getPieceByPosition(9, 4));

    }

    public void testTestGetPieceByPosition() {
    }
}