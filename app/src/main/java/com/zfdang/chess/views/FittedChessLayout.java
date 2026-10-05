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
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = widthMode == MeasureSpec.UNSPECIFIED
                ? Math.max(getSuggestedMinimumWidth(), ChessView.BOARD_WIDTH)
                : MeasureSpec.getSize(widthMeasureSpec);
        int contentWidth = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        int desiredHeight = contentWidth * ChessView.BOARD_HEIGHT / ChessView.BOARD_WIDTH
                + getPaddingTop() + getPaddingBottom();
        int height = heightMode == MeasureSpec.EXACTLY ? MeasureSpec.getSize(heightMeasureSpec)
                : resolveSize(Math.max(getSuggestedMinimumHeight(), desiredHeight), heightMeasureSpec);
        int contentHeight = Math.max(0, height - getPaddingTop() - getPaddingBottom());
        int boardWidth = Math.min(contentWidth,
                contentHeight * ChessView.BOARD_WIDTH / ChessView.BOARD_HEIGHT);
        int boardHeight = boardWidth * ChessView.BOARD_HEIGHT / ChessView.BOARD_WIDTH;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            child.measure(MeasureSpec.makeMeasureSpec(boardWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(boardHeight, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            int x = getPaddingLeft() + (getWidth() - getPaddingLeft() - getPaddingRight()
                    - child.getMeasuredWidth()) / 2;
            int y = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom()
                    - child.getMeasuredHeight()) / 2;
            child.layout(x, y, x + child.getMeasuredWidth(), y + child.getMeasuredHeight());
        }
    }
}
