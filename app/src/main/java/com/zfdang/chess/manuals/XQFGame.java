package com.zfdang.chess.manuals;

import android.util.Log;

import com.zfdang.chess.gamelogic.Board;
import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.Piece;
import com.zfdang.chess.gamelogic.Position;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// XQF format
// https://github.com/zfdang/chinese-chess-fish-android/blob/master/%E6%A3%8B%E8%B0%B1/XQF%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F%E8%AF%B4%E6%98%8E.TXT
public class XQFGame {
    private String format;
    private int version;

    private Board board;

    private String result;

    private String category;

    private String title;
    private String match;
    private String date;
    private String place;
    private String red;
    private String black;
    private String redDuration;
    private String blackDuration;

    private String commenter;
    private String author;

    private List<Move> moves;

    private static final Charset GB2312 = Charset.forName("GB2312");

    public XQFGame() {
        moves = new ArrayList<>();
        board = new Board();
    }

    // Getters and setters for each field
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRedDuration() {
        return redDuration;
    }

    public void setRedDuration(String redDuration) {
        this.redDuration = redDuration;
    }

    public String getRed() {
        return red;
    }

    public void setRed(String red) {
        this.red = red;
    }

    public String getBlackDuration() {
        return blackDuration;
    }

    public void setBlackDuration(String blackDuration) {
        this.blackDuration = blackDuration;
    }

    public String getBlack() {
        return black;
    }

    public void setBlack(String black) {
        this.black = black;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<Move> getMoves() {
        return moves;
    }

    public void setMoves(List<Move> moves) {
        this.moves = moves;
    }


    public static XQFGame parse(byte[] buffer) {
        if (buffer.length < 0x400) {
            Log.e("XQFGame", "Invalid XQF file: " + buffer.length);
            return null;
        }

        XQFGame game = new XQFGame();

        // 0000 - 0001      固定为“XQ”和版本号，如“XQF V1.0 ”
        game.setFormat(new String(buffer, 0, 2, StandardCharsets.UTF_8));
        if (!game.getFormat().equals("XQ")) {
            Log.e("XQFGame", "Invalid XQF format: " + game.getFormat());
            return null;
        }

        // 0002             XQF文件版本号，必须为0x0A = (XQF 1.0)。
        if (buffer[0x0002] != 0x0A) {
            Log.e("XQFGame", "Invalid XQF version: " + buffer[0x0002]);
            return null;
        }
        game.setVersion(1);

        // 0010 - 002F      这32个字节是棋局的开局局面，局面说明见“局面表示”
        parseBoard(buffer, game);

        game.setResult(parseResult(buffer[0x0033]));
        game.setCategory(parseCategory(buffer[0x0040]));

        game.setTitle(readString(buffer, 0x50, 0x90));
        game.setMatch(readString(buffer, 0xD0, 0x110));
        game.setDate(readString(buffer, 0x110, 0x120));
        game.setPlace(readString(buffer, 0x120, 0x130));

        game.setRed(readString(buffer, 0x130, 0x140));
        game.setBlack(readString(buffer, 0x140, 0x150));

        game.setRedDuration(readString(buffer, 0x190, 0x1A0));
        game.setBlackDuration(readString(buffer, 0x1A0, 0x1B0));

        game.setCommenter(readString(buffer, 0x1D0, 0x1E0));
        game.setAuthor(readString(buffer, 0x1E0, 0x1F0));

        parseMoves(buffer, game, buffer.length);

        return game;
    }

    private void setCategory(String category) {
        this.category = category;
    }

    private static String parseCategory(byte b) {
        //     0040             棋局的类型: 0x00-全局文件, 0x01-布局文件,
        //                                 0x02-中局文件, 0x03-残局文件
        if (b == 0x00) {
            return "Full game";
        } else if (b == 0x01) {
            return "Opening";
        } else if (b == 0x02) {
            return "Middle game";
        } else if (b == 0x03) {
            return "End game";
        }
        return null;
    }

    private static void parseBoard(byte[] buffer, XQFGame game) {
        game.board.clear();
        // 0010 - 002F      这32个字节是棋局的开局局面，局面说明见“局面表示”
        // 01 - 16: 依次为红方的车马相士帅士相马车炮炮兵兵兵兵兵
        // 17 - 32: 依次为黑方的车马象士将士象马车炮炮卒卒卒卒卒
        int offset = 0x0010;
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WJU);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WMA);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WXIANG);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WSHI);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WSHUAI);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WSHI);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WXIANG);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WMA);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WJU);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WPAO);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WPAO);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WBING);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WBING);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WBING);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WBING);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.WBING);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BJU);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BMA);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BXIANG);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BSHI);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BJIANG);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BSHI);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BXIANG);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BMA);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BJU);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BPAO);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BPAO);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BZU);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BZU);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BZU);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BZU);
        game.board.setPieceByPosition(getPosFromValue(buffer[offset++]), Piece.BZU);
    }

    private static Position getPosFromValue(int value) {
        // 在XQF文件中，一个棋盘位置用一个字节表示，字节值 = X * 10 + Y
        int y = value % 10;
        int x = (value - y) / 10;
        return new Position(x, y);
    }

    private static void parseMoves(byte[] buffer, XQFGame game, int bytesRead) {
//        B. 棋谱记录 (0400 - 文件尾部)
//                --------------------------------------------------------------------------
//        从0x0400开始存放棋谱记录，每步记录的存放格式为: 8个棋谱记录字节 + 0个或
//        多个字节的棋谱注解文本。其中，8个棋谱记录字节的格式为:
//
//        第 1 字节:  本步棋的开始位置坐标的字节值(X*10+Y) + 24 (十进制)。
//        第 2 字节:  本步棋的到达位置坐标的字节值(X*10+Y) + 32 (十进制)。
//        第 3 字节:  如果不是最后一步棋，为0xF0；如果是最后一步棋，为0x00。
//        第 4 字节:  保留: 必须为0x00。
//        第5-8字节:  为一个32位整数(x86格式,高字节在后)，表明本步注解的大小，
//        如果没有注解，则为0x00000000。
//        如果本步没有评注，即5-8字节全部为0x00，则本步结束，否则，随后存放本
//        步评注文本，文本不以'\0'结束(由于前面已经有大小, 该文本的长度必须等
//                于5-8字节处的整数值)。如此反复，可将所有棋步存入。(暂时不支持变着的
//        保存)。从上面可以看出如果没有注解，一步棋的记录共占8个字节。
//
//        需要说明的是，在真正的对局记录开始前，必须有一步空的着法，称为第0步，该
//        步的第1 - 8字节必须是 0x18, 0x20, 0xF0, 0xFF, 0x00, 0x00, 0x00, 0x00。由于
//        这些字节是固定的，所以真正的棋谱记录是从0x0408处开始的。如果您的棋局只有初
//        始局面而没有任何着法记录(即只有一个残局局势),则文件中棋谱记录只有第0步的记
//        录，并且该记录的8个字节为:0x18,0x20,0x00,0xFF,0x00,0x00,0x00,0x00。 详细的
//        格式请参见本文后面的文件例子。
        int offset = 0x408;
        while (offset + 7 < bytesRead) {
            Position from = getPosFromValue(buffer[offset] - 24);
            Position to = getPosFromValue(buffer[offset + 1] - 32);
            Move m = new Move(from, to);
            game.moves.add(m);
            // 第5-8字节:  为一个32位整数(x86格式,高字节在后)，表明本步注解的大小，
            //                   如果没有注解，则为0x00000000。
            int size = buffer[offset + 4] + (buffer[offset + 5] << 8) + (buffer[offset + 6] << 16) + (buffer[offset + 7] << 24);
            if (size > 0) {
                Log.d("XQFGame", "Comment size: " + buffer[offset + 4] + " " + buffer[offset + 5] + " " + buffer[offset + 6] + " " + buffer[offset + 7] + " " + size);

                // skip the size of the comment
                offset += 8;
                // read the comment, make sure does not exceed the length of the buffer
                if (offset + size > bytesRead) {
                    Log.e("XQFGame", "Invalid XQF file: bytes read:" + bytesRead + " offset: " + offset + " comment size: " + size);
                    break;
                } else {
                    String comment = new String(buffer, offset, size, GB2312);
                    Log.d("XQFGame", "Comment: " + comment);
                    m.setComment(comment);
                    offset += size;
                }
            } else {
                offset += 8;
            }
        }
    }

    private static String readString(byte[] buffer, int start, int end) {
        int length = buffer[start];
        return new String(buffer, start + 1, length, GB2312).trim();
    }

    private static String parseResult(byte result) {
        // 0033             棋局的结果: 0x00-未知,0x01-红胜,0x02-黑胜,0x03-和棋
        switch (result) {
            case 0x01:
                return "Red wins";
            case 0x02:
                return "Black wins";
            case 0x03:
                return "Draw";
            default:
                return "Unknown";
        }
    }

    public String getCommenter() {
        return commenter;
    }

    public void setCommenter(String commenter) {
        this.commenter = commenter;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean validateMoves() {
        Board board = new Board(this.board);
        for (Move move : moves) {
            if (!board.doMove(move)) {
                return false;
            }
        }
        return true;
    }

    // toString method
    @Override
    public String toString() {
        return "XQFGame{" +
                "format='" + format + '\'' +
                ", version=" + version +
                ", result='" + result + '\'' +
                ", category='" + category + '\'' +
                ", title='" + title + '\'' +
                ", match='" + match + '\'' +
                ", date='" + date + '\'' +
                ", place='" + place + '\'' +
                ", red='" + red + '\'' +
                ", black='" + black + '\'' +
                ", redDuration='" + redDuration + '\'' +
                ", blackDuration='" + blackDuration + '\'' +
                ", commenter='" + commenter + '\'' +
                ", author='" + author + '\'' +
                ", moves counts=" + moves.size() +
                '}';
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }
}
