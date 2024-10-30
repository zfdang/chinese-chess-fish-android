package com.zfdang.chess.gamelogic;

import android.util.Log;

import java.io.Serializable;

/**
 * Created by zfdang on 2016-4-17.
 * This class represents the chess board data, basically it's represention of FEN string;
 */
public class Board implements Cloneable, Serializable {
    private static final long serialVersionUID = 2194052829642412444L;

    public boolean bRedGo = true;
    public int rounds = 0;
    static public int BOARD_PIECE_WIDTH = 9;
    static public int BOARD_PIECE_HEIGHT = 10;

    // do not access piece value directly, use getPieceByPosition instead
    // it's easy to make mistake with values of (x,y)
    private int[][] piece = new int[][]{
            {Piece.BJU, Piece.BMA, Piece.BXIANG, Piece.BSHI, Piece.BJIANG, Piece.BSHI, Piece.BXIANG, Piece.BMA, Piece.BJU},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, Piece.BPAO, 0, 0, 0, 0, 0, Piece.BPAO, 0},
            {Piece.BZU, 0, Piece.BZU, 0, Piece.BZU, 0, Piece.BZU, 0, Piece.BZU},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},

            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {Piece.WBING, 0, Piece.WBING, 0, Piece.WBING, 0, Piece.WBING, 0, Piece.WBING},
            {0, Piece.WPAO, 0, 0, 0, 0, 0, Piece.WPAO, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {Piece.WJU, Piece.WMA, Piece.WXIANG, Piece.WSHI, Piece.WSHUAI, Piece.WSHI, Piece.WXIANG, Piece.WMA, Piece.WJU},
    };

    public Board() {
        bRedGo = true;
        rounds = 0;
    }

    public void setInfo(Board board) throws CloneNotSupportedException {
        this.bRedGo = board.bRedGo;
        this.rounds = board.rounds;
        for (int i = 0; i < this.piece.length; i++) {
            this.piece[i] = board.piece[i].clone();
        }
    }

    public boolean setPieceByPosition(Position pos, int value){

        if (pos.x >= 0 && pos.x <= BOARD_PIECE_WIDTH && pos.y >= 0 && pos.y <= BOARD_PIECE_HEIGHT && Piece.isValid(value)) {
            piece[pos.y][pos.x] = value;
            return true;
        } else {
            return false;
        }
    }

    public boolean setPieceByPosition(int x, int y, int value){
        if (x >= 0 && x <= BOARD_PIECE_WIDTH && y >= 0 && y <= BOARD_PIECE_HEIGHT && Piece.isValid(value)) {
            piece[y][x] = value;
            return true;
        } else {
            return false;
        }
    }

    public int getPieceByPosition(Position pos) {
        if (pos.x >= 0 && pos.x <= 8 && pos.y >= 0 && pos.y <= 9) {
            return piece[pos.y][pos.x];
        } else {
            return -1;
        }
    }

    public int getPieceByPosition(int x, int y) {
        if (x >= 0 && x <= 8 && y >= 0 && y <= 9) {
            return piece[y][x];
        } else {
            return -1;
        }
    }

    public String convertToFEN() {
        // https://www.xqbase.com/protocol/pgnfen2.htm
        // https://www.xqbase.com/protocol/cchess_fen.htm
        // 按白方视角，描述由上至下、由左至右的盘面，以/符号来分隔相邻横列。白方大写字母、黑方小写字母。
        // 棋盘的编号：从左到右为a-i，从下到上为0-9
        // FEN字符串中棋子的位置的顺序是从左到右，从上到下，空格用数字表示:
        // a9-i9, a8-i8, ..., a1-i1, a0-i0
        String fen = "";
        for (int y = 0; y < BOARD_PIECE_HEIGHT; y++) {
            String row = "";
            int zeros = 0;
            for (int x = 0; x < BOARD_PIECE_WIDTH; x++) {
                int piece = getPieceByPosition(x, y);
                if (piece != 0) {
                    // if zeros > 0, add zeros to fen
                    if (zeros > 0) {
                        row += zeros;
                        zeros = 0;
                    }
                    // convert piece to FEN
                    char fenPiece = Piece.pieceCharMap.get(piece);
                    row += (char) (fenPiece);
                } else {
                    zeros++;
                }
            }
            if (zeros > 0) {
                row += zeros;
            }
            fen += row;
            if (y < BOARD_PIECE_HEIGHT - 1) {
                // add / to separate rows
                fen += "/";
            }
        }
        String result = String.format("%s %s - - %s %s", fen, bRedGo ? "w" : "b", 0, rounds);
//        Log.d("Board", "FEN: " + result);
        return result;
    }

    public boolean restoreFromFEN(String fenString) {
        fenString = fenString.trim();
        String[] parts = fenString.split(" ");
        if (parts.length != 6) {
            return false;
        }

        // parse isRedGo
        String side = parts[1].toLowerCase();
        if(side.equals("w") || side.equals("r")) {
            // white or red, both are valid
            bRedGo = true;
        } else if(side.equals("b")) {
            bRedGo = false;
        } else {
            return false;
        }

        // parse rounds
        String part5 = parts[5];
        try {
            rounds = Integer.parseInt(part5);
        } catch (NumberFormatException e) {
            return false;
        }

        // parse fen string
        String fen = parts[0];
        int x = 0;
        int y = 0;
        for (int i = 0; i < fen.length(); i++) {
            char c = fen.charAt(i);
            if (c == '/') {
                // next row
                x = 0;
                y++;
            } else if (c >= '0' && c <= '9') {
                for (int k = 0; k < c - '0'; k++) {
                    piece[y][x] = 0;
                    x++;
                }
            } else {
                int value = Piece.pieceValueMap.get(c);
                piece[y][x] = value;
                x++;
            }
        }
        return true;
    }

    public void randomizePieces(){
        // randomize values for all pieces
        for (int y = 0; y < BOARD_PIECE_HEIGHT; y++) {
            for (int x = 0; x < BOARD_PIECE_WIDTH; x++) {
                piece[y][x] = (int) (Math.random() * 14);
            }
        }
    }
}
