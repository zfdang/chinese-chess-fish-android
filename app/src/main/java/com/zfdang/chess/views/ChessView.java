package com.zfdang.chess.views;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.zfdang.chess.R;
import com.zfdang.chess.data.ChessStatus;
import com.zfdang.chess.data.Position;

import org.jetbrains.annotations.NotNull;


public class ChessView extends SurfaceView implements SurfaceHolder.Callback {
    public ChessViewThread thread;

    public Paint paint;

    public Bitmap ChessBoardBitmap;
    public Bitmap B_box, R_box, Pot;
    public Bitmap[] PieceBitmaps = new Bitmap[14];

    // 如何设置下面的几个参数：
    // 有2个假设：棋盘的每个格子是正方形的, 棋子也是正方形的
    // 要计算下面的几个参数，需要找到棋盘上的几个点：格子左上角的坐标(x1, y1)，格子右上角的坐标(x2, y2)
    // 本次使用的棋盘x1 = 43, y1=80, x2=706, y2=80
    final int BOARD_WIDTH = 750;  // 根据棋盘的实际宽度来设置
    final int BOARD_HEIGHT = 909; // 根据棋盘的实际高度来设置
    static final int BOARD_PIECE_SIZE = 70;  // 根据棋盘的实际格子大小来设置
    static final int BOARD_X_OFFSET = 8; // x1 - BOARD_PIECE_SIZE/2
    static final int BOARD_Y_OFFSET = 45; // y1 - BOARD_PIECE_SIZE/2
    static final int BOARD_GRID_INTERVAL = 83;  // (x2-x1)/8

    public Rect srcBoardRect, destBoardRect;
    public int Board_width, Board_height;
    public float scaleRatio;

    public ChessStatus chessStatus;

    public String[] thinkMood = new String[]{"😀", "🙂", "😶", "😣", "😵", "😭"};
    public int thinkIndex = 0;
    public int thinkFlag = 0;
    public String thinkContent = "😀·····";

    public ChessView(Context context, ChessStatus chessStatus) {
        super(context);
        this.chessStatus = chessStatus;
        getHolder().addCallback(this);
        initBitmaps();
    }

    public void initBitmaps() {
        ChessBoardBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.chessboard);
        srcBoardRect = new Rect(0, 0, ChessBoardBitmap.getWidth(), ChessBoardBitmap.getHeight());

        B_box = BitmapFactory.decodeResource(getResources(), R.drawable.b_box);
        R_box = BitmapFactory.decodeResource(getResources(), R.drawable.r_box);
        Pot = BitmapFactory.decodeResource(getResources(), R.drawable.pot);

        PieceBitmaps[0] = BitmapFactory.decodeResource(getResources(), R.drawable.b_jiang);
        PieceBitmaps[1] = BitmapFactory.decodeResource(getResources(), R.drawable.b_shi);
        PieceBitmaps[2] = BitmapFactory.decodeResource(getResources(), R.drawable.b_xiang);
        PieceBitmaps[3] = BitmapFactory.decodeResource(getResources(), R.drawable.b_ma);
        PieceBitmaps[4] = BitmapFactory.decodeResource(getResources(), R.drawable.b_ju);
        PieceBitmaps[5] = BitmapFactory.decodeResource(getResources(), R.drawable.b_pao);
        PieceBitmaps[6] = BitmapFactory.decodeResource(getResources(), R.drawable.b_zu);
        PieceBitmaps[7] = BitmapFactory.decodeResource(getResources(), R.drawable.r_shuai);
        PieceBitmaps[8] = BitmapFactory.decodeResource(getResources(), R.drawable.r_shi);
        PieceBitmaps[9] = BitmapFactory.decodeResource(getResources(), R.drawable.r_xiang);
        PieceBitmaps[10] = BitmapFactory.decodeResource(getResources(), R.drawable.r_ma);
        PieceBitmaps[11] = BitmapFactory.decodeResource(getResources(), R.drawable.r_ju);
        PieceBitmaps[12] = BitmapFactory.decodeResource(getResources(), R.drawable.r_pao);
        PieceBitmaps[13] = BitmapFactory.decodeResource(getResources(), R.drawable.r_bing);
    }

    public void Draw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        // draw chess board
        canvas.drawBitmap(ChessBoardBitmap, srcBoardRect, destBoardRect, null);

        // draw piece
        Rect tempSrcRect, tempDesRect;
        for (int x = 0; x < ChessStatus.BOARD_PIECE_WIDTH; x++) {
            for (int y = 0; y < ChessStatus.BOARD_PIECE_HEIGHT; y++) {
                Position pos = new Position(x, y);
                int piece = chessStatus.getPieceByPosition(pos);
                if (piece > 0) {
                    // valid piece, draw the bitmap
                    Bitmap bitmap = PieceBitmaps[piece-1];
                    tempSrcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    tempDesRect = getDestRect(pos);
                    canvas.drawBitmap(bitmap, tempSrcRect, tempDesRect, null);
                }
            }
        }

        // draw selected piece
        Position pos = chessStatus.selectedPosition;
        int piece = chessStatus.getPieceByPosition(pos);
        if (piece > 0) {
            // valid piece is selected
            tempDesRect = getDestRect(pos);
            if(chessStatus.isPieceRed(pos)) {
                tempSrcRect = new Rect(0, 0, B_box.getWidth(), B_box.getHeight());
                canvas.drawBitmap(B_box, tempSrcRect, tempDesRect, null);
            } else {
                tempSrcRect = new Rect(0, 0, R_box.getWidth(), R_box.getHeight());
                canvas.drawBitmap(R_box, tempSrcRect, tempDesRect, null);
            }
        }

//        if (chessStatus.prePosition.equals(new Position(-1, -1)) == false && chessStatus.IsChecked == false) {
//            int real_curX = chessStatus.curPosition.x;
//            int real_curY = chessStatus.curPosition.y;
//
//            Position realPre = Rule.reversePos(chessStatus.prePosition, chessStatus.IsReverse);
//            Position realCur = Rule.reversePos(chessStatus.curPosition, chessStatus.IsReverse);
//            int draw_preX = realPre.x;
//            int draw_preY = realPre.y;
//            int draw_curX = realCur.x;
//            int draw_curY = realCur.y;
//
//            Rect tmpRect;
//
//            tempDesRect = new Rect(Scale(draw_curX * 85 + 3), Scale(draw_curY * 85 + 41), Scale(draw_curX * 85 + 83), Scale(draw_curY * 85 + 121));
//            tmpRect = new Rect(Scale(draw_preX * 85 + 3), Scale(draw_preY * 85 + 41), Scale(draw_preX * 85 + 83), Scale(draw_preY * 85 + 121));
//
////            if (chessStatus.piece[real_curY][real_curX] >= 1 && chessStatus.piece[real_curY][real_curX] <= 7) {
////                tempSrcRect = new Rect(0, 0, B_box.getWidth(), B_box.getHeight());
////                canvas.drawBitmap(B_box, tempSrcRect, tempDesRect, null);
////                canvas.drawBitmap(B_box, tempSrcRect, tmpRect, null);
////            } else {
////                tempSrcRect = new Rect(0, 0, R_box.getWidth(), R_box.getHeight());
////                canvas.drawBitmap(R_box, tempSrcRect, tempDesRect, null);
////                canvas.drawBitmap(R_box, tempSrcRect, tmpRect, null);
////            }
//        }

        if (chessStatus.status == 1) {
            if (chessStatus.isMachine == true) {
                if (thinkFlag == 0) {
                    thinkContent = "";
                    for (int i = 0; i < thinkIndex; i++) {
                        thinkContent += '·';
                    }
                    thinkContent += thinkMood[thinkIndex];
                    for (int i = thinkIndex + 1; i < 6; i++) {
                        thinkContent += '·';
                    }
                    thinkIndex = (thinkIndex + 1) % 6;
                }
                thinkFlag = (thinkFlag + 1) % 5;
                canvas.drawText(thinkContent, Board_width / 2, Board_height / 2 + Scale(57) * 7 / 20, paint);
            } else {
                thinkIndex = 0;
                thinkContent = "😀·····";
            }
        }
    }

    @NonNull
    private Rect getDestRect(Position pos) {
        return new Rect(
                Scale(pos.x * BOARD_GRID_INTERVAL + BOARD_X_OFFSET),
                Scale(pos.y * BOARD_GRID_INTERVAL + BOARD_Y_OFFSET),
                Scale(pos.x * BOARD_GRID_INTERVAL + BOARD_X_OFFSET + BOARD_PIECE_SIZE),
                Scale(pos.y * BOARD_GRID_INTERVAL + BOARD_Y_OFFSET + BOARD_PIECE_SIZE));
    }

    public int Scale(int x) {
        return (int)(x * scaleRatio);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        Board_width = MeasureSpec.getSize(widthMeasureSpec);
        Board_height = Board_width * BOARD_HEIGHT / BOARD_WIDTH;
        scaleRatio = (float) Board_width / BOARD_WIDTH;

        destBoardRect = new Rect(0, 0, Board_width, Board_height);
        setMeasuredDimension(Board_width, Board_height);


    }


    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    public void surfaceCreated(SurfaceHolder holder) {
        this.thread = new ChessViewThread(getHolder());
        this.thread.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @NotNull
    public Position getPosByCoord(float x, float y) {
        float vx = x / scaleRatio;
        float vy = y / scaleRatio;

        int ix = (int)((vx - BOARD_X_OFFSET) / BOARD_GRID_INTERVAL);
        int iy = (int)((vy - BOARD_Y_OFFSET) / BOARD_GRID_INTERVAL);

        Rect rect = getDestRect(new Position(ix, iy));
        if(rect.contains((int)x, (int)y)) {
            Log.d("ChessView", "getPosByCoord: " + ix + ", " + iy);
            return new Position(ix, iy);
        } else {
            Log.d("ChessView", "getPosByCoord: " + "out of bound");
        }

        return new Position(-1, -1);
    }

    class ChessViewThread extends Thread {
        //刷帧线程
        public int span = 100;//睡眠100毫秒数
        public SurfaceHolder surfaceHolder;

        public ChessViewThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }

        public void run() {//重写的方法
            Canvas c;//画布
            while (true) {//循环绘制
                c = this.surfaceHolder.lockCanvas();
                try {
                    Draw(c);//绘制方法
                } catch (Exception e) {
                    e.printStackTrace();//输出异常堆栈信息
                }
                if (c != null) this.surfaceHolder.unlockCanvasAndPost(c);
                try {
                    Thread.sleep(span);//睡眠时间，单位是毫秒
                } catch (Exception e) {
                    e.printStackTrace();//输出异常堆栈信息
                }
            }
        }
    }
}