package com.zfdang.chess.gamelogic;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;

public class BoardTest {

    private Board roundTrip(Board board) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(board); }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (Board) input.readObject();
        }
    }

    @Test public void legacyUnknownZeroIsMigratedButNewZeroIsPreserved() throws Exception {
        Board board = new Board();
        board.score = 0f;
        assertEquals(0f, roundTrip(board).score, 0f);
        Field format = Board.class.getDeclaredField("evaluationFormatVersion");
        format.setAccessible(true);
        format.setInt(board, 0);
        assertTrue(Float.isNaN(roundTrip(board).score));
        board.score = -1.25f;
        assertEquals(-1.25f, roundTrip(board).score, 0f);
    }

    @Test
    public void evaluationIsUnknownUntilComputedAndResetOnFenImport() {
        Board board = new Board();
        assertTrue(Float.isNaN(board.score));
        board.score = 1.25f;
        assertEquals(1.25f, new Board(board).score, 0f);
        assertTrue(board.restoreFromFEN(board.toFENString()));
        assertTrue(Float.isNaN(board.score));
    }

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
