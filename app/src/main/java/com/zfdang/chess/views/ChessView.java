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
import com.zfdang.chess.gamelogic.Board;
import com.zfdang.chess.gamelogic.Game;
import com.zfdang.chess.gamelogic.Piece;
import com.zfdang.chess.gamelogic.Position;
import com.zfdang.chess.utils.ArrowShape;

import android.graphics.Path;


public class ChessView extends SurfaceView implements SurfaceHolder.Callback {
    public ChessViewThread thread;

    protected static class XYCoord {
        public int x;
        public int y;
        public XYCoord(int x, int y) { this.x = x; this.y = y; }
    }
    public Paint paint;

    public Bitmap ChessBoardBitmap;
    public Bitmap B_box, R_box, R_pot, B_pot;
    public Bitmap[] PieceBitmaps = new Bitmap[14];
    public Bitmap[] ChoiceBitmaps = new Bitmap[5];

    // 如何设置下面的几个参数：
    // 有2个假设：棋盘的每个格子是正方形的, 棋子也是正方形的
    // 要计算下面的几个参数，需要找到棋盘上的几个点：格子左上角的坐标(x1, y1)，格子右上角的坐标(x2, y2)
    // 本次使用的棋盘x1=77, y1=60, x2=1165, y2=60
    final int BOARD_WIDTH = 1240;  // 根据棋盘的实际宽度来设置
    final int BOARD_HEIGHT = 1340; // 根据棋盘的实际高度来设置
    static final int BOARD_PIECE_SIZE = 110;  // 根据棋盘的实际格子大小来设置
    static final int BOARD_X_OFFSET = 22; // x1 - BOARD_PIECE_SIZE/2
    static final int BOARD_Y_OFFSET = 5; // y1 - BOARD_PIECE_SIZE/2
    static final int BOARD_GRID_INTERVAL = 136;  // (x2-x1)/8

    public Rect srcBoardRect, destBoardRect;
    public int Board_width, Board_height;
    public float scaleRatio;

    public Game game;

    public String[] thinkMood = new String[]{"😀", "🙂", "😶", "😣", "😵", "😭"};
    public int thinkIndex = 0;
    public int thinkFlag = 0;
    public String thinkContent = "😀·····";

    public ChessView(Context context, Game game) {
        super(context);
        this.game = game;
        getHolder().addCallback(this);
        initBitmaps();
    }

    // method to set game
    public void setGame(Game game) {
        this.game = game;
    }

    public void initBitmaps() {
        ChessBoardBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.chessboard);
        srcBoardRect = new Rect(0, 0, ChessBoardBitmap.getWidth(), ChessBoardBitmap.getHeight());

        B_box = BitmapFactory.decodeResource(getResources(), R.drawable.b_box);
        R_box = BitmapFactory.decodeResource(getResources(), R.drawable.r_box);
        R_pot = BitmapFactory.decodeResource(getResources(), R.drawable.redpot);
        B_pot = BitmapFactory.decodeResource(getResources(), R.drawable.blackpot);

        // these values should be consistent with Piece.java
        PieceBitmaps[0] = BitmapFactory.decodeResource(getResources(), R.drawable.r_shuai);
        PieceBitmaps[1] = BitmapFactory.decodeResource(getResources(), R.drawable.r_shi);
        PieceBitmaps[2] = BitmapFactory.decodeResource(getResources(), R.drawable.r_xiang);
        PieceBitmaps[3] = BitmapFactory.decodeResource(getResources(), R.drawable.r_ma);
        PieceBitmaps[4] = BitmapFactory.decodeResource(getResources(), R.drawable.r_ju);
        PieceBitmaps[5] = BitmapFactory.decodeResource(getResources(), R.drawable.r_pao);
        PieceBitmaps[6] = BitmapFactory.decodeResource(getResources(), R.drawable.r_bing);
        PieceBitmaps[7] = BitmapFactory.decodeResource(getResources(), R.drawable.b_jiang);
        PieceBitmaps[8] = BitmapFactory.decodeResource(getResources(), R.drawable.b_shi);
        PieceBitmaps[9] = BitmapFactory.decodeResource(getResources(), R.drawable.b_xiang);
        PieceBitmaps[10] = BitmapFactory.decodeResource(getResources(), R.drawable.b_ma);
        PieceBitmaps[11] = BitmapFactory.decodeResource(getResources(), R.drawable.b_ju);
        PieceBitmaps[12] = BitmapFactory.decodeResource(getResources(), R.drawable.b_pao);
        PieceBitmaps[13] = BitmapFactory.decodeResource(getResources(), R.drawable.b_zu);

        // load drawables for choice bitmaps
        ChoiceBitmaps[0] = BitmapFactory.decodeResource(getResources(), R.drawable.digit1);
        ChoiceBitmaps[1] = BitmapFactory.decodeResource(getResources(), R.drawable.digit2);
        ChoiceBitmaps[2] = BitmapFactory.decodeResource(getResources(), R.drawable.digit3);
        ChoiceBitmaps[3] = BitmapFactory.decodeResource(getResources(), R.drawable.digit4);
        ChoiceBitmaps[4] = BitmapFactory.decodeResource(getResources(), R.drawable.digit5);
    }

    public void Draw(Canvas canvas) {
        if(canvas == null) {
            return;
        }
        // draw chess board
        canvas.drawBitmap(ChessBoardBitmap, srcBoardRect, destBoardRect, null);

        Board board = game.currentBoard;

        // draw piece
        Rect tempSrcRect, tempDesRect;
        for (int x = 0; x < Board.BOARD_PIECE_WIDTH; x++) {
            for (int y = 0; y < Board.BOARD_PIECE_HEIGHT; y++) {
                Position pos = new Position(x, y);
                int piece = board.getPieceByPosition(pos);
                if (Piece.isValid(piece)) {
                    // valid piece, draw the bitmap
                    Bitmap bitmap = PieceBitmaps[piece-1];
                    tempSrcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    tempDesRect = getDestRect(pos);
                    canvas.drawBitmap(bitmap, tempSrcRect, tempDesRect, null);
                }
            }
        }

        if(game.startPos != null) {
            // highlight selected piece
            HighlighSelectedPiece(canvas);

            // show all possible moves for selected piece
            int piece = game.currentBoard.getPieceByPosition(game.startPos);
            // draw all possible moves
            if(Piece.isRed(piece)) {
                tempSrcRect = new Rect(0, 0, R_pot.getWidth(), R_pot.getHeight());
                for (Position pos : game.possibleToPositions) {
                    tempDesRect = getDestRect(pos);
                    canvas.drawBitmap(R_pot, tempSrcRect, tempDesRect, null);
                }
            } else if(Piece.isBlack(piece)){
                tempSrcRect = new Rect(0, 0, B_pot.getWidth(), B_pot.getHeight());
                for (Position pos : game.possibleToPositions) {
                    tempDesRect = getDestRect(pos);
                    canvas.drawBitmap(B_pot, tempSrcRect, tempDesRect, null);
                }
            }
        }

        // draw arrows for last moves
        if(game.history.size() > 1) {
            DrawMoveHistory(canvas);
        }

    }

    private void HighlighSelectedPiece(Canvas canvas) {
        // draw selected piece
        Board board = game.currentBoard;
        Rect tempDesRect, tempSrcRect;
        Position pos = game.startPos;
        int piece = board.getPieceByPosition(pos);
        if (Piece.isValid(piece)) {
            // valid piece is selected
            tempDesRect = getDestRect(pos);
            if (Piece.isRed(piece)) {
                tempSrcRect = new Rect(0, 0, R_box.getWidth(), R_box.getHeight());
                canvas.drawBitmap(R_box, tempSrcRect, tempDesRect, null);
            } else {
                tempSrcRect = new Rect(0, 0, B_box.getWidth(), B_box.getHeight());
                canvas.drawBitmap(B_box, tempSrcRect, tempDesRect, null);
            }
        }
    }

    private void DrawMoveHistory(Canvas canvas) {
        Board board = game.currentBoard;
        XYCoord crd0, crd1;

        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);
        p.setAntiAlias(true);

        // draw arrow for the last several moves in historyMoves
        int num_of_moves = 2;
        // game.historyRecords.get(0) is game without any move
        for(int i = game.history.size() - 1; i >= 1 && i >= game.history.size() - num_of_moves; i--) {
            Game.HistoryRecord record = game.history.get(i);
            crd0 = getCoordByPosition(record.move.fromPosition);
            crd1 = getCoordByPosition(record.move.toPosition);

            // color
            if(Piece.isRed(record.move.piece)) {
                p.setColor(Color.RED);
            } else {
                p.setColor(Color.BLACK);
            }

            // calculate alpha value, the last move is the most opaque one
            int idx = (game.history.size() - 1 - i);
            int value = 200 - idx * 50;
            if(value < 0) value = 0;
            p.setAlpha(value);

            DrawArrow(canvas, crd0, crd1, p, null);
        }
    }

    /*
        * 画箭头，并在箭头上显示bitmap。这个bitmap一般是数字，来标识箭头
     */
    void DrawArrow(Canvas canvas, XYCoord crd0, XYCoord crd1, Paint p, Bitmap bitmap) {
        ArrowShape arrow = new ArrowShape();
        Path path = new Path();
        arrow.getTransformedPath(path, crd0.x, crd0.y, crd1.x, crd1.y);
        canvas.drawPath(path, p);

        if(bitmap != null) {
            int offset_to_endpos = 80;
            int width_of_bitmap = 80;
            // 找到合适的位置，然后在那个位置画bitmap

            // 离crd1 offset_to_endpos个像素的位置
            XYCoord crd3 = new XYCoord(0, 0);
            int dx = crd1.x - crd0.x;
            int dy = crd1.y - crd0.y;
            int d = (int)Math.sqrt(dx*dx + dy*dy);
            crd3.x = crd1.x - offset_to_endpos * dx / d;
            crd3.y = crd1.y - offset_to_endpos * dy / d;

            // 两者之间3/5的位置
            XYCoord crd4 = new XYCoord((crd0.x*2 + crd1.x*3) / 5, (crd0.y*2 + crd1.y*3) / 5);

            // 取离crd1最近的点, 防止箭头太长时，数字离箭头太远
            int d3 = (crd3.x - crd1.x) * (crd3.x - crd1.x) + (crd3.y - crd1.y) * (crd3.y - crd1.y);
            int d4 = (crd4.x - crd1.x) * (crd4.x - crd1.x) + (crd4.y - crd1.y) * (crd4.y - crd1.y);
            XYCoord crd = d3 < d4 ? crd3 : crd4;

            // draw bitmap to crd position
            int sx = bitmap.getWidth();
            int sy = bitmap.getHeight();
            int nx = width_of_bitmap / 2;
            int ny = nx * sy / sx / 2;
            Rect tempSrcRect = new Rect(0, 0, sx, sy);
            Rect tempDesRect = new Rect(crd.x - nx, crd.y - ny, crd.x + nx, crd.y + ny);
            canvas.drawBitmap(bitmap, tempSrcRect, tempDesRect, null);
        }
    }

    public int Scale(int x) {
        return (int)(x * scaleRatio);
    }

        @NonNull
    private Rect getDestRect(Position pos) {
        return new Rect(
                Scale(pos.x * BOARD_GRID_INTERVAL + BOARD_X_OFFSET),
                Scale(pos.y * BOARD_GRID_INTERVAL + BOARD_Y_OFFSET),
                Scale(pos.x * BOARD_GRID_INTERVAL + BOARD_X_OFFSET + BOARD_PIECE_SIZE),
                Scale(pos.y * BOARD_GRID_INTERVAL + BOARD_Y_OFFSET + BOARD_PIECE_SIZE));
    }


    public XYCoord getCoordByPosition(Position pos) {
        Rect r = getDestRect(pos);
        return new XYCoord(r.centerX(), r.centerY());
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

        return null;
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