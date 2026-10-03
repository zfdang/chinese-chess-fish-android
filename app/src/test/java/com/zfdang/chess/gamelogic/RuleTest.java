package com.zfdang.chess.gamelogic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class RuleTest {

    private static Board fromFEN(String fen) {
        Board board = new Board();
        assertTrue(board.restoreFromFEN(fen));
        return board;
    }

    // perft: 统计指定深度下的所有合法走法序列数
    private static long perft(Board board, int depth) {
        if (depth == 0) {
            return 1;
        }
        long nodes = 0;
        for (int y = 0; y < Board.BOARD_PIECE_HEIGHT; y++) {
            for (int x = 0; x < Board.BOARD_PIECE_WIDTH; x++) {
                int piece = board.getPieceByPosition(x, y);
                if (!Piece.isValid(piece) || Piece.isRed(piece) != board.bRedGo) {
                    continue;
                }
                List<Position> targets = Rule.PossibleToPositions(piece, x, y, board);
                if (depth == 1) {
                    nodes += targets.size();
                    continue;
                }
                for (Position to : targets) {
                    Board next = new Board(board);
                    next.doMove(new Move(new Position(x, y), to));
                    nodes += perft(next, depth - 1);
                }
            }
        }
        return nodes;
    }

    @Test
    public void testPerftFromInitialPosition() {
        // 标准象棋perft数值
        Board board = new Board();
        assertEquals(44, perft(board, 1));
        assertEquals(1920, perft(board, 2));
        assertEquals(79666, perft(board, 3));
    }

    @Test
    public void testCannotExposeKingsFacing() {
        // 红仕挡在将帅之间，不能走开
        Board board = fromFEN("4k4/9/9/9/9/9/9/9/4A4/4K4 w - - 0 1");
        assertTrue(Rule.PossibleToPositions(Piece.WSHI, 4, 8, board).isEmpty());
        // 将帅照面视为被攻击
        Board facing = fromFEN("4k4/9/9/9/9/9/9/9/9/4K4 w - - 0 1");
        assertTrue(Rule.isJiangShuaiInDanger(Piece.WSHUAI, new Position(4, 9), facing));
    }

    @Test
    public void testCannotLeaveKingInCheck() {
        // 黑车将军，红车只能挡在中间或吃车，其他走法都非法
        Board board = fromFEN("3k5/9/9/9/4r4/9/9/R8/9/4K4 w - - 0 1");
        List<Position> targets = Rule.PossibleToPositions(Piece.WJU, 0, 7, board);
        assertEquals(1, targets.size());
        assertEquals(new Position(4, 7), targets.get(0));
        assertFalse(Rule.isValidMove(new Move(new Position(0, 7), new Position(0, 0)), board));
    }

    @Test
    public void testCheckmateAndStalemate() {
        // 双车错杀：黑将被将死
        Board mate = fromFEN("3k5/4R4/3R5/9/9/9/9/9/9/5K3 b - - 0 1");
        Position bk = Rule.findJiangShuaiPos(Piece.BJIANG, mate);
        assertTrue(Rule.isJiangShuaiInDanger(Piece.BJIANG, bk, mate));
        assertTrue(Rule.isJiangShuaiDead(Piece.BJIANG, bk, mate));

        // 困毙：黑将未被将军，但无子可走
        Board stalemate = fromFEN("3k5/4R4/9/9/9/9/9/9/9/5K3 b - - 0 1");
        bk = Rule.findJiangShuaiPos(Piece.BJIANG, stalemate);
        assertFalse(Rule.isJiangShuaiInDanger(Piece.BJIANG, bk, stalemate));
        assertFalse(Rule.hasLegalMove(false, stalemate));

        // 初始局面双方都有棋可走
        assertTrue(Rule.hasLegalMove(true, new Board()));
        assertTrue(Rule.hasLegalMove(false, new Board()));
    }

    @Test
    public void testCannonAndHorseAttacks() {
        // 炮隔子将军
        Board pao = fromFEN("4k4/9/4P4/9/4C4/9/9/9/9/3K5 b - - 0 1");
        assertTrue(Rule.isJiangShuaiInDanger(Piece.BJIANG, new Position(4, 0), pao));
        // 马将军，但被蹩马腿则不算
        Board ma = fromFEN("4k4/9/3N5/9/9/9/9/9/9/3K5 b - - 0 1");
        assertTrue(Rule.isJiangShuaiInDanger(Piece.BJIANG, new Position(4, 0), ma));
        Board blocked = fromFEN("4k4/3a5/3N5/9/9/9/9/9/9/3K5 b - - 0 1");
        assertFalse(Rule.isJiangShuaiInDanger(Piece.BJIANG, new Position(4, 0), blocked));
    }

    @Test
    public void testMalformedFENDoesNotCorruptBoard() {
        Board board = new Board();
        String before = board.toFENString();
        assertFalse(board.restoreFromFEN("rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNRX w - - 0 1"));
        assertFalse(board.restoreFromFEN("rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR/9 w - - 0 1"));
        assertEquals(before, board.toFENString());
    }
}
