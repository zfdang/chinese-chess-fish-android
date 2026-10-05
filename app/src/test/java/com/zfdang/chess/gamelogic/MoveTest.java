package com.zfdang.chess.gamelogic;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MoveTest {

    @Test
    public void commentWhitespaceRemainsCompatibleWithOlderAndroid() {
        Move move = new Move(new Board());
        move.setComment("\u2003 注释 \u2003");
        assertEquals("注释", move.getComment());
        move.setComment(null);
        assertEquals(null, move.getComment());
    }

    @Test
    public void testGetUCCIString() {
        Position from = new Position(7, 7); // h2
        Position to = new Position(4, 7); // e2
        Move move = new Move(from, to);
        String expectedDescription = "h2e2";
        assertEquals(expectedDescription, move.getUCCIString());
    }

    @Test
    public void testGetChsString() {
        // Move(Board) keeps its own copy of the board, so mutate that copy
        Move move = new Move(new Board());
        Board board = move.board;

        // red pieces
        move.fromPosition = new Position(8, 9);
        move.toPosition = new Position(8, 3);
        board.setPieceByPosition(move.fromPosition, Piece.WJU); // Set a piece at the from position
        assertEquals("车一进六", move.getChsString());

        move.fromPosition = new Position(2, 2);
        move.toPosition = new Position(2, 9);
        board.setPieceByPosition(move.fromPosition, Piece.WPAO); // Set a piece at the from position
        assertEquals("炮七退七", move.getChsString());

        move.fromPosition = new Position(2, 9);
        move.toPosition = new Position(4, 7);
        board.setPieceByPosition(move.fromPosition, Piece.WXIANG); // Set a piece at the from position
        assertEquals("相七进五", move.getChsString());

        move.fromPosition = new Position(5, 8);
        move.toPosition = new Position(4, 8);
        board.setPieceByPosition(move.fromPosition, Piece.WSHUAI); // Set a piece at the from position
        assertEquals("帅四平五", move.getChsString());

        move.fromPosition = new Position(3, 7);
        move.toPosition = new Position(4, 8);
        board.setPieceByPosition(move.fromPosition, Piece.WSHI); // Set a piece at the from position
        assertEquals("仕六退五", move.getChsString());

        // black pieces

        move.fromPosition = new Position(3, 9);
        move.toPosition = new Position(3, 8) ;
        board.setPieceByPosition(move.fromPosition, Piece.BJU); // Set a piece at the from position
        assertEquals("车4退1", move.getChsString());

        move.fromPosition = new Position(7, 7);
        move.toPosition = new Position(3, 7) ;
        board.setPieceByPosition(move.fromPosition, Piece.BPAO); // Set a piece at the from position
        assertEquals("前炮平4", move.getChsString());

        move.fromPosition = new Position(2, 0);
        move.toPosition = new Position(4, 2) ;
        board.setPieceByPosition(move.fromPosition, Piece.BXIANG); // Set a piece at the from position
        assertEquals("象3进5", move.getChsString());

        move.fromPosition = new Position(2, 2);
        move.toPosition = new Position(1, 4) ;
        board.setPieceByPosition(move.fromPosition, Piece.BMA); // Set a piece at the from position
        assertEquals("马3进2", move.getChsString());
    }

    @Test
    public void testFromUCCIString() {
        Move move = new Move(new Board());

        // Test valid UCCI string
        boolean result = move.fromUCCIString("h2e2");
        assertTrue(result);
        assertEquals(new Position(7, 7), move.fromPosition);
        assertEquals(new Position(4, 7), move.toPosition);

        // Test invalid UCCI string (length not 4)
        result = move.fromUCCIString("h2e");
        assertFalse(result);

        // Test invalid UCCI string (invalid characters)
        result = move.fromUCCIString("h2e@");
        assertFalse(result);
    }

    @Test
    public void testUCCIRoundTrip() {
        for (int y = 0; y < Board.BOARD_PIECE_HEIGHT; y++) {
            for (int x = 0; x < Board.BOARD_PIECE_WIDTH; x++) {
                Move move = new Move(new Position(x, y), new Position(8 - x, 9 - y));
                Move parsed = new Move(new Board());
                assertTrue(parsed.fromUCCIString(move.getUCCIString()));
                assertEquals(move.fromPosition, parsed.fromPosition);
                assertEquals(move.toPosition, parsed.toPosition);
            }
        }
        assertEquals("a9i0", new Move(new Position(0, 0), new Position(8, 9)).getUCCIString());
    }

    @Test
    public void testChsStringFromInitialPosition() {
        Board board = new Board();
        assertEquals("炮二平五", new Move(new Position(7, 7), new Position(4, 7), board).getChsString());
        assertEquals("马八进七", new Move(new Position(1, 9), new Position(2, 7), board).getChsString());
        assertEquals("炮8平5", new Move(new Position(7, 2), new Position(4, 2), board).getChsString());
        assertEquals("马2进3", new Move(new Position(1, 0), new Position(2, 2), board).getChsString());
    }
}
