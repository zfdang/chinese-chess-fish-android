package com.zfdang.chess.gamelogic;

// create enum for various events
public enum GameStatus {
    SELECT, // 棋子被选择，或者取消选择
    MOVE, // 棋子移动
    CAPTURE, // 吃子
    CHECK, // 将军
    CHECKMATE, // 将死
    ILLEGAL, // 非法移动
    WIN, // 胜利
    LOSE, // 失败
    DRAW, // 和棋
    ENGINE, // 引擎事件
}