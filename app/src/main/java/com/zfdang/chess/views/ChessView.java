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

import com.zfdang.chess.ChessApp;
import com.zfdang.chess.R;
import com.zfdang.chess.controllers.GameController;
import com.zfdang.chess.gamelogic.Board;
import com.zfdang.chess.gamelogic.Game;
import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.Piece;
import com.zfdang.chess.gamelogic.Position;
import com.zfdang.chess.utils.ArrowShape;
import com.zfdang.chess.utils.DrawableUtil;

import android.graphics.Path;

import java.util.List;


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
    public Bitmap ThinkBitmap;
    final int MAX_SUGGESTED_MOVES = 5;

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

    // 绘制时复用的对象，避免每帧分配内存
    private final Rect tmpSrcRect = new Rect();
    private final Rect tmpDestRect = new Rect();
    private final Paint suggestionPaint = new Paint();
    private final Paint historyPaint = new Paint();
    private final Path arrowPath = new Path();
    private final ArrowShape arrowShape = new ArrowShape();
    public int Board_width, Board_height;
    public float scaleRatio;

    public GameController controller;

    public String[] thinkMood = new String[]{"😀", "🙂", "😶", "😣", "😵", "😭"};
    public int thinkIndex = 0;
    public int thinkFlag = 0;
    public String thinkContent = "😀·····";

    public ChessView(Context context, GameController controller) {
        super(context);
        this.controller = controller;
        getHolder().addCallback(this);
        initBitmaps();

        suggestionPaint.setStyle(Paint.Style.FILL);
        suggestionPaint.setAntiAlias(true);
        suggestionPaint.setColor(Color.GREEN);
        historyPaint.setStyle(Paint.Style.FILL);
        historyPaint.setAntiAlias(true);
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

        // get drawable for think state
        ThinkBitmap = DrawableUtil.drawableToBitmap(ChessApp.getContext().getDrawable(R.drawable.intelligence));
    }

    private void drawBitmap(Canvas canvas, Bitmap bitmap, Rect dest) {
        tmpSrcRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(bitmap, tmpSrcRect, dest, null);
    }

    public void Draw(Canvas canvas) {
        Game game = controller.game;
        if(canvas == null || game == null) {
            return;
        }
        // draw chess board
        canvas.drawBitmap(ChessBoardBitmap, srcBoardRect, destBoardRect, null);

        Board board = game.currentBoard;

        // draw controller state in the middle of destBoardRect
        showControllerState(canvas);

        // draw piece
        for (int x = 0; x < Board.BOARD_PIECE_WIDTH; x++) {
            for (int y = 0; y < Board.BOARD_PIECE_HEIGHT; y++) {
                int piece = board.getPieceByPosition(x, y);
                if (Piece.isValid(piece)) {
                    // valid piece, draw the bitmap
                    drawBitmap(canvas, PieceBitmaps[piece-1], getDestRect(x, y, tmpDestRect));
                }
            }
        }

        Position startPos = game.startPos;
        if(startPos != null) {
            int piece = board.getPieceByPosition(startPos);
            if (Piece.isValid(piece)) {
                // highlight selected piece
                drawBitmap(canvas, Piece.isRed(piece) ? R_box : B_box, getDestRect(startPos.x, startPos.y, tmpDestRect));

                // draw all possible moves for selected piece
                Bitmap pot = Piece.isRed(piece) ? R_pot : B_pot;
                for (Position pos : game.possibleToPositions) {
                    drawBitmap(canvas, pot, getDestRect(pos.x, pos.y, tmpDestRect));
                }
            }
        }

        // draw arrows for last moves
        if(game.history.size() > 0) {
            DrawMoveHistory(canvas, game);
        }

        // if there are suggested moves, show them on the board
        List<Move> suggestedMoves = game.suggestedMoves;
        for(int i = 0; i < suggestedMoves.size() && i < MAX_SUGGESTED_MOVES ; i++) {
            Move move = suggestedMoves.get(i);
            DrawArrow(canvas, getCoordByPosition(move.fromPosition), getCoordByPosition(move.toPosition),
                    suggestionPaint, ChoiceBitmaps[i]);
        }
    }

    private void showControllerState(Canvas canvas) {
        int targetSize = Scale(30);
        int xOffset = Scale(BOARD_GRID_INTERVAL * 4 + 45);
        tmpDestRect.set(destBoardRect.centerX() - targetSize + xOffset, destBoardRect.centerY() - targetSize,
                destBoardRect.centerX() + targetSize + xOffset, destBoardRect.centerY() + targetSize);
        Bitmap bitmap;
        if (controller.isRedTurn()) {
            bitmap = PieceBitmaps[0];
        } else if (controller.isBlackTurn()) {
            bitmap = PieceBitmaps[7];
        } else {
            bitmap = ThinkBitmap;
        }
        drawBitmap(canvas, bitmap, tmpDestRect);
    }

    private void DrawMoveHistory(Canvas canvas, Game game) {
        // draw arrow for the last several moves in historyMoves
        int num_of_history_moves = 2;
        if(controller.settings != null) {
            num_of_history_moves = controller.settings.getHistory_moves();
        }
        List<Game.HistoryRecord> history = game.history;
        int size = history.size();
        for(int i = size - 1; i >= 0 && i >= size - num_of_history_moves; i--) {
            Game.HistoryRecord record = history.get(i);

            // color
            historyPaint.setColor(Piece.isRed(record.move.piece) ? Color.RED : Color.BLACK);

            // calculate alpha value, the last move is the most opaque one
            int idx = (size - 1 - i);
            historyPaint.setAlpha(Math.max(0, 220 - idx * 40));

            DrawArrow(canvas, getCoordByPosition(record.move.fromPosition),
                    getCoordByPosition(record.move.toPosition), historyPaint, null);
        }
    }

    /*
        * 画箭头，并在箭头上显示bitmap。这个bitmap一般是数字，来标识箭头
     */
    void DrawArrow(Canvas canvas, XYCoord crd0, XYCoord crd1, Paint p, Bitmap bitmap) {
        arrowPath.reset();
        arrowShape.getTransformedPath(arrowPath, crd0.x, crd0.y, crd1.x, crd1.y);
        canvas.drawPath(arrowPath, p);

        if(bitmap != null) {
            int offset_to_endpos = 80;
            int width_of_bitmap = 80;
            // 找到合适的位置，然后在那个位置画bitmap

            // 离crd1 offset_to_endpos个像素的位置
            XYCoord crd3 = new XYCoord(0, 0);
            int dx = crd1.x - crd0.x;
            int dy = crd1.y - crd0.y;
            int d = Math.max(1, (int)Math.sqrt(dx*dx + dy*dy));
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
            tmpDestRect.set(crd.x - nx, crd.y - ny, crd.x + nx, crd.y + ny);
            drawBitmap(canvas, bitmap, tmpDestRect);
        }
    }

    public int Scale(int x) {
        return (int)(x * scaleRatio);
    }

    @NonNull
    private Rect getDestRect(int x, int y, @NonNull Rect out) {
        out.set(Scale(x * BOARD_GRID_INTERVAL + BOARD_X_OFFSET),
                Scale(y * BOARD_GRID_INTERVAL + BOARD_Y_OFFSET),
                Scale(x * BOARD_GRID_INTERVAL + BOARD_X_OFFSET + BOARD_PIECE_SIZE),
                Scale(y * BOARD_GRID_INTERVAL + BOARD_Y_OFFSET + BOARD_PIECE_SIZE));
        return out;
    }


    public XYCoord getCoordByPosition(Position pos) {
        int half = BOARD_PIECE_SIZE / 2;
        return new XYCoord(Scale(pos.x * BOARD_GRID_INTERVAL + BOARD_X_OFFSET + half),
                Scale(pos.y * BOARD_GRID_INTERVAL + BOARD_Y_OFFSET + half));
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
        stopDrawThread();
        this.thread = new ChessViewThread(holder);
        this.thread.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // surface销毁前必须停止绘制线程，否则每次重建surface都会泄漏一个线程
        stopDrawThread();
    }

    private void stopDrawThread() {
        ChessViewThread t = this.thread;
        this.thread = null;
        if (t == null) {
            return;
        }
        t.running = false;
        t.interrupt();
        try {
            t.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Position getPosByCoord(float x, float y) {
        float vx = x / scaleRatio;
        float vy = y / scaleRatio;

        int ix = (int)((vx - BOARD_X_OFFSET) / BOARD_GRID_INTERVAL);
        int iy = (int)((vy - BOARD_Y_OFFSET) / BOARD_GRID_INTERVAL);

        if (ix < 0 || ix >= Board.BOARD_PIECE_WIDTH || iy < 0 || iy >= Board.BOARD_PIECE_HEIGHT) {
            return null;
        }
        Rect rect = getDestRect(ix, iy, new Rect());
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
        public final SurfaceHolder surfaceHolder;
        volatile boolean running = true;

        public ChessViewThread(SurfaceHolder surfaceHolder) {
            super("ChessViewThread");
            this.surfaceHolder = surfaceHolder;
        }

        public void run() {
            while (running) {
                Canvas c = this.surfaceHolder.lockCanvas();
                if (c != null) {
                    try {
                        Draw(c);
                    } catch (Exception e) {
                        Log.e("ChessView", "Draw failed", e);
                    } finally {
                        try {
                            this.surfaceHolder.unlockCanvasAndPost(c);
                        } catch (IllegalStateException | IllegalArgumentException e) {
                            // surface已被销毁
                            return;
                        }
                    }
                }
                try {
                    Thread.sleep(span);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
