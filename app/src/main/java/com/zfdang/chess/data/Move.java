package com.zfdang.chess.data;


import java.io.Serializable;

public class Move implements Serializable {
    private static final long serialVersionUID = -1608509463525143473L;

    public Position fromPosition;
    public Position toPosition;

    public Move(Position fromPosition, Position toPosition) {
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
    }
}
