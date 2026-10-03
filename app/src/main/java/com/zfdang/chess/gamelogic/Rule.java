package com.zfdang.chess.gamelogic;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by 77304 on 2021/4/4.
 *
 * 走法生成与将军检测。
 * PossibleToPositions 返回的是"完全合法"的走法：走完之后己方将帅不会被攻击（包括将帅照面）。
 */

public class Rule {
    private static final int W = Board.BOARD_PIECE_WIDTH;
    private static final int H = Board.BOARD_PIECE_HEIGHT;

    // 0 棋盘外 1 黑盘 2 黑九宫 3 红盘 4 红九宫
    private static final int[][] area = new int[][]{
            {1, 1, 1, 2, 2, 2, 1, 1, 1},
            {1, 1, 1, 2, 2, 2, 1, 1, 1},
            {1, 1, 1, 2, 2, 2, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1},

            {3, 3, 3, 3, 3, 3, 3, 3, 3},
            {3, 3, 3, 3, 3, 3, 3, 3, 3},
            {3, 3, 3, 4, 4, 4, 3, 3, 3},
            {3, 3, 3, 4, 4, 4, 3, 3, 3},
            {3, 3, 3, 4, 4, 4, 3, 3, 3},
    };

    // 帅 将：上下左右一步
    private static final int[] KING_DX = {0, 0, 1, -1};
    private static final int[] KING_DY = {1, -1, 0, 0};
    // 仕 士：斜一步
    private static final int[] SHI_DX = {1, 1, -1, -1};
    private static final int[] SHI_DY = {1, -1, 1, -1};
    // 相 象：斜两步，象眼在中间
    private static final int[] XIANG_DX = {2, 2, -2, -2};
    private static final int[] XIANG_DY = {2, -2, 2, -2};
    // 马：日字，蹩马腿为相邻的直线格
    private static final int[] MA_DX = {1, 1, -1, -1, 2, 2, -2, -2};
    private static final int[] MA_DY = {2, -2, 2, -2, 1, -1, 1, -1};
    private static final int[] MA_LEG_DX = {0, 0, 0, 0, 1, 1, -1, -1};
    private static final int[] MA_LEG_DY = {1, -1, 1, -1, 0, 0, 0, 0};
    // 反向蹩马腿：从被攻击点出发，检查攻击它的马的马腿（在被攻击点的斜角）
    private static final int[] MA_REV_LEG_DX = {1, 1, -1, -1, 1, 1, -1, -1};
    private static final int[] MA_REV_LEG_DY = {1, -1, 1, -1, 1, -1, 1, -1};

    private static int inArea(int x, int y) {
        if (x < 0 || x >= W || y < 0 || y >= H) {
            return 0;
        }
        return area[y][x];
    }

    /*
    在棋盘中找到将帅的位置
     */
    public static Position findJiangShuaiPos(int piece, Board board){
        int yStart;
        if(piece == Piece.WSHUAI) {
            yStart = 7;
        } else if (piece == Piece.BJIANG) {
            yStart = 0;
        } else {
            return null;
        }
        for (int y = yStart; y <= yStart + 2; y++) {
            for (int x = 3; x <= 5; x++) {
                if (board.getPieceByPosition(x, y) == piece) {
                    return new Position(x, y);
                }
            }
        }
        return null;
    }

    /*
     * 返回(fromX, fromY)上的棋子的所有合法落点
     */
    public static List<Position> PossibleToPositions(int pieceId, int fromX, int fromY, Board board) {
        List<Position> ret = new ArrayList<>();
        if (!Piece.isValid(pieceId)) {
            return ret;
        }
        generatePseudoMoves(pieceId, fromX, fromY, board, ret);
        if (ret.isEmpty()) {
            return ret;
        }

        // 过滤掉走完后己方将帅仍处于被攻击状态的走法
        boolean red = Piece.isRed(pieceId);
        int king = red ? Piece.WSHUAI : Piece.BJIANG;
        Position kingPos = (pieceId == king) ? null : findJiangShuaiPos(king, board);
        if (pieceId != king && kingPos == null) {
            // 残局摆子时可能没有将帅，此时无法判断将军，直接返回
            return ret;
        }

        Board scratch = new Board(board);
        List<Position> legal = new ArrayList<>(ret.size());
        for (Position to : ret) {
            if (!leavesKingInDanger(scratch, king, kingPos, pieceId, fromX, fromY, to.x, to.y)) {
                legal.add(to);
            }
        }
        return legal;
    }

    /*
    检查走法是否合法
     */
    public static boolean isValidMove(Move move, Board board) {
        if (move == null || move.fromPosition == null || move.toPosition == null) {
            return false;
        }

        int piece = board.getPieceByPosition(move.fromPosition);
        List<Position> positions = PossibleToPositions(piece, move.fromPosition.x, move.fromPosition.y, board);
        return positions.contains(move.toPosition);
    }

    /*
     * 判断piece(帅或将)在pos处是否被对方攻击，包括将帅照面
     */
    public static boolean isJiangShuaiInDanger(int piece, Position pos, Board board) {
        if (pos == null) {
            return false;
        }
        if (piece == Piece.WSHUAI) {
            return isAttacked(pos.x, pos.y, true, board);
        } else if (piece == Piece.BJIANG) {
            return isAttacked(pos.x, pos.y, false, board);
        }
        return false;
    }

    /*
     * 判断某一方是否还有合法走法。找到第一个合法走法即返回。
     * 没有合法走法时：被将军则为将死，否则为困毙，两者都判负。
     */
    public static boolean hasLegalMove(boolean red, Board board) {
        int king = red ? Piece.WSHUAI : Piece.BJIANG;
        Position kingPos = findJiangShuaiPos(king, board);
        Board scratch = new Board(board);
        List<Position> buffer = new ArrayList<>(17);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int pieceId = board.getPieceByPosition(x, y);
                if (!Piece.isValid(pieceId) || Piece.isRed(pieceId) != red) {
                    continue;
                }
                buffer.clear();
                generatePseudoMoves(pieceId, x, y, board, buffer);
                for (Position to : buffer) {
                    if (kingPos == null && pieceId != king) {
                        return true;
                    }
                    if (!leavesKingInDanger(scratch, king, kingPos, pieceId, x, y, to.x, to.y)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /*
     * 在scratch上临时走一步，检查己方将帅是否被攻击，然后还原。scratch必须与原棋盘一致。
     */
    private static boolean leavesKingInDanger(Board scratch, int king, Position kingPos, int pieceId,
                                              int fromX, int fromY, int toX, int toY) {
        int captured = scratch.getPieceByPosition(toX, toY);
        scratch.setPieceByPosition(fromX, fromY, Piece.EMPTY);
        scratch.setPieceByPosition(toX, toY, pieceId);

        boolean red = (king == Piece.WSHUAI);
        boolean danger;
        if (pieceId == king) {
            danger = isAttacked(toX, toY, red, scratch);
        } else {
            danger = isAttacked(kingPos.x, kingPos.y, red, scratch);
        }

        scratch.setPieceByPosition(toX, toY, captured);
        scratch.setPieceByPosition(fromX, fromY, pieceId);
        return danger;
    }

    /*
     * 判断(x, y)处的己方将帅(red表示红方)是否被对方攻击
     * 车、炮、对方将帅(照面)直接沿四个方向扫描，无需生成走法列表
     */
    private static boolean isAttacked(int x, int y, boolean red, Board board) {
        int enemyMa = red ? Piece.BMA : Piece.WMA;
        int enemyJu = red ? Piece.BJU : Piece.WJU;
        int enemyPao = red ? Piece.BPAO : Piece.WPAO;
        int enemyKing = red ? Piece.BJIANG : Piece.WSHUAI;
        int enemyPawn = red ? Piece.BZU : Piece.WBING;

        // 马
        for (int i = 0; i < MA_DX.length; i++) {
            if (board.getPieceByPosition(x + MA_DX[i], y + MA_DY[i]) == enemyMa
                    && board.getPieceByPosition(x + MA_REV_LEG_DX[i], y + MA_REV_LEG_DY[i]) == Piece.EMPTY) {
                return true;
            }
        }

        // 兵卒：红帅被黑卒从上方(y-1)或左右攻击；黑将被红兵从下方(y+1)或左右攻击
        int pawnDy = red ? -1 : 1;
        if (board.getPieceByPosition(x - 1, y) == enemyPawn
                || board.getPieceByPosition(x + 1, y) == enemyPawn
                || board.getPieceByPosition(x, y + pawnDy) == enemyPawn) {
            return true;
        }

        // 车、炮、将帅照面
        for (int d = 0; d < KING_DX.length; d++) {
            int dx = KING_DX[d], dy = KING_DY[d];
            int cx = x + dx, cy = y + dy;
            boolean screened = false;
            while (cx >= 0 && cx < W && cy >= 0 && cy < H) {
                int p = board.getPieceByPosition(cx, cy);
                if (p != Piece.EMPTY) {
                    if (!screened) {
                        if (p == enemyJu || (p == enemyKing && dx == 0)) {
                            return true;
                        }
                        screened = true;
                    } else {
                        if (p == enemyPao) {
                            return true;
                        }
                        break;
                    }
                }
                cx += dx;
                cy += dy;
            }
        }
        return false;
    }

    private static boolean isEnemyOrEmpty(int pieceId, int target) {
        return target == Piece.EMPTY || (Piece.isValid(target) && Piece.isRed(target) != Piece.isRed(pieceId));
    }

    /*
     * 生成伪合法走法（未考虑走后己方是否被将军），结果追加到out
     */
    private static void generatePseudoMoves(int pieceId, int fromX, int fromY, Board board, List<Position> out) {
        boolean red = Piece.isRed(pieceId);
        switch (pieceId) {
            case Piece.WSHUAI:
            case Piece.BJIANG: {
                int palace = red ? 4 : 2;
                for (int i = 0; i < KING_DX.length; i++) {
                    int toX = fromX + KING_DX[i];
                    int toY = fromY + KING_DY[i];
                    if (inArea(toX, toY) == palace && isEnemyOrEmpty(pieceId, board.getPieceByPosition(toX, toY))) {
                        out.add(new Position(toX, toY));
                    }
                }
                break;
            }
            case Piece.WSHI:
            case Piece.BSHI: {
                int palace = red ? 4 : 2;
                for (int i = 0; i < SHI_DX.length; i++) {
                    int toX = fromX + SHI_DX[i];
                    int toY = fromY + SHI_DY[i];
                    if (inArea(toX, toY) == palace && isEnemyOrEmpty(pieceId, board.getPieceByPosition(toX, toY))) {
                        out.add(new Position(toX, toY));
                    }
                }
                break;
            }
            case Piece.WXIANG:
            case Piece.BXIANG: {
                // 相象不能过河
                int lo = red ? 3 : 1;
                for (int i = 0; i < XIANG_DX.length; i++) {
                    int toX = fromX + XIANG_DX[i];
                    int toY = fromY + XIANG_DY[i];
                    int a = inArea(toX, toY);
                    if (a >= lo && a <= lo + 1
                            && board.getPieceByPosition(fromX + XIANG_DX[i] / 2, fromY + XIANG_DY[i] / 2) == Piece.EMPTY
                            && isEnemyOrEmpty(pieceId, board.getPieceByPosition(toX, toY))) {
                        out.add(new Position(toX, toY));
                    }
                }
                break;
            }
            case Piece.WMA:
            case Piece.BMA:
                for (int i = 0; i < MA_DX.length; i++) {
                    int toX = fromX + MA_DX[i];
                    int toY = fromY + MA_DY[i];
                    if (inArea(toX, toY) != 0
                            && board.getPieceByPosition(fromX + MA_LEG_DX[i], fromY + MA_LEG_DY[i]) == Piece.EMPTY
                            && isEnemyOrEmpty(pieceId, board.getPieceByPosition(toX, toY))) {
                        out.add(new Position(toX, toY));
                    }
                }
                break;
            case Piece.WJU:
            case Piece.BJU:
            case Piece.WPAO:
            case Piece.BPAO: {
                boolean isPao = (pieceId == Piece.WPAO || pieceId == Piece.BPAO);
                for (int d = 0; d < KING_DX.length; d++) {
                    int dx = KING_DX[d], dy = KING_DY[d];
                    int cx = fromX + dx, cy = fromY + dy;
                    boolean screened = false;
                    while (cx >= 0 && cx < W && cy >= 0 && cy < H) {
                        int p = board.getPieceByPosition(cx, cy);
                        if (!screened) {
                            if (p == Piece.EMPTY) {
                                out.add(new Position(cx, cy));
                            } else {
                                if (!isPao && isEnemyOrEmpty(pieceId, p)) {
                                    out.add(new Position(cx, cy));
                                }
                                if (!isPao) {
                                    break;
                                }
                                screened = true;
                            }
                        } else if (p != Piece.EMPTY) {
                            // 炮隔一子吃子
                            if (isEnemyOrEmpty(pieceId, p)) {
                                out.add(new Position(cx, cy));
                            }
                            break;
                        }
                        cx += dx;
                        cy += dy;
                    }
                }
                break;
            }
            case Piece.WBING:
            case Piece.BZU: {
                int forward = red ? -1 : 1;
                boolean crossed = red ? fromY <= 4 : fromY >= 5;
                int toY = fromY + forward;
                if (inArea(fromX, toY) != 0 && isEnemyOrEmpty(pieceId, board.getPieceByPosition(fromX, toY))) {
                    out.add(new Position(fromX, toY));
                }
                if (crossed) {
                    for (int dx = -1; dx <= 1; dx += 2) {
                        int toX = fromX + dx;
                        if (inArea(toX, fromY) != 0 && isEnemyOrEmpty(pieceId, board.getPieceByPosition(toX, fromY))) {
                            out.add(new Position(toX, fromY));
                        }
                    }
                }
                break;
            }
            default:
                break;
        }
    }
}
