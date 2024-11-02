package com.zfdang.chess.gamelogic;


import static java.lang.Math.abs;

import java.io.Serializable;
import java.util.HashMap;

public class Move implements Serializable {
    public Board board = null;

    public Position fromPosition;
    public Position toPosition;
    public boolean isRedMove = false;

    // create hashmap to convert Arabic numerals to Chinese numerals
    public static final HashMap<Integer, String> arabicToChineseMap = new HashMap<>();
    static {
        arabicToChineseMap.put(1, "一");
        arabicToChineseMap.put(2, "二");
        arabicToChineseMap.put(3, "三");
        arabicToChineseMap.put(4, "四");
        arabicToChineseMap.put(5, "五");
        arabicToChineseMap.put(6, "六");
        arabicToChineseMap.put(7, "七");
        arabicToChineseMap.put(8, "八");
        arabicToChineseMap.put(9, "九");
    }
    public Move(Position fromPosition, Position toPosition, Board board) {
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
        this.board = board;
        updateMoveColor();
    }

    public Move(Board board) {
        this.board = board;
        updateMoveColor();
    }

    private void updateMoveColor(){
        int piece = board.getPieceByPosition(fromPosition);
        if(Piece.isValid(piece)) {
            isRedMove = Piece.isRed(piece);
        }
    }

    public Move(Position fromPosition, Position toPosition) {
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
    }

    // h2e2
    public String getCoordDescription(){
        char s, e;
        s = (char)('a' + fromPosition.x);
        e = (char)('a' + toPosition.x);
        String result = String.format("%c%d%c%d", s, 9 - fromPosition.y, e, 9 - toPosition.y);
        return result;
    }

    /*
    * 从一个位置移动到另一个位置的中文描述
    * 例如：车一进六, 炮七退七, 相七进九, 帅四平五, 帅五进一, 将五退一, 未知动作
    * 这个函数的设计比较复杂，需要考虑的因素很多：
    * 1. 对于横轴来讲，红方是从右到左数的，黑方是从左到右数的
    * 2. 对于纵轴来讲，红方是从下到上是进，黑方是从下到上是退
    * 3. 对于走直线的棋子，需要考虑是进退（最后一个数字是纵坐标的差值），还是平移（最后一个数字是目标位置的横坐标）
    * 4. 对于走斜线的棋子（马、相、士），需要考虑是进退，最后一个数字是目标位置的横坐标
     */
    public String getChineseStyleDescription(){
        if(board == null){
            // depends on board status
            return "未知动作";
        }
        int piece = board.getPieceByPosition(fromPosition);
        if(Piece.isValid(piece)) {
            char name = Piece.pieceNameMap.get(piece);

            if(Piece.isRed(piece)) {
                String num1 = arabicToChineseMap.get(9 - fromPosition.x);

                String action = "平";
                // 3.对于走直线的棋子，需要考虑是进退（最后一个数字是纵坐标的差值），还是平移（最后一个数字是目标位置的横坐标）
                String num2 = arabicToChineseMap.get(abs(toPosition.y - fromPosition.y));
                if(toPosition.y > fromPosition.y){
                    action = "退";
                } else if(toPosition.y < fromPosition.y){
                    action = "进";
                } else {
                    // 3.对于走直线的棋子，需要考虑是进退（最后一个数字是纵坐标的差值），还是平移（最后一个数字是目标位置的横坐标）
                    num2 = arabicToChineseMap.get(9 - toPosition.x);
                }

                if(Piece.isDiagonalPiece(piece)) {
                    // 4. 对于走斜线的棋子（马、相、士），需要考虑是进退，最后一个数字是目标位置的横坐标
                    num2 = arabicToChineseMap.get(9 - toPosition.x);
                }

                return name + num1 + action + num2;
            } else if(Piece.isBlack(piece)) {
                String num1 = arabicToChineseMap.get(fromPosition.x + 1);

                String action = "平";
                // 3.对于走直线的棋子，需要考虑是进退（最后一个数字是纵坐标的差值），还是平移（最后一个数字是目标位置的横坐标）
                String num2 = arabicToChineseMap.get(abs(toPosition.y - fromPosition.y));
                if(toPosition.y > fromPosition.y){
                    action = "进";
                } else if(toPosition.y < fromPosition.y){
                    action = "退";
                } else {
                    // 3.对于走直线的棋子，需要考虑是进退（最后一个数字是纵坐标的差值），还是平移（最后一个数字是目标位置的横坐标）
                    num2 = arabicToChineseMap.get(toPosition.x + 1);
                }

                if(Piece.isDiagonalPiece(piece)) {
                    // 4. 对于走斜线的棋子（马、相、士），需要考虑是进退，最后一个数字是目标位置的横坐标
                    num2 = arabicToChineseMap.get(toPosition.x + 1);
                }

                return name + num1 + action + num2;
            }
        }
        return "未知动作";
    }
}
