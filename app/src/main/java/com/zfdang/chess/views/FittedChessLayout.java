package com.zfdang.chess.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/** Fits the board into the remaining screen space without stretching its artwork. */
public class FittedChessLayout extends FrameLayout {
    public FittedChessLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int boardWidth = Math.min(width, height * 1240 / 1340);
        int boardHeight = boardWidth * 1340 / 1240;
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(boardWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(boardHeight, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int x = (getWidth() - child.getMeasuredWidth()) / 2;
            int y = (getHeight() - child.getMeasuredHeight()) / 2;
            child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
        }
    }
}
