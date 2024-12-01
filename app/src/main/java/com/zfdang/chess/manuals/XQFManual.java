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
public class XQFManual {
    private String format;
    private int version;

    public Board board;

    private String result;

    private String category;

    private String title;
    private String event;
    private String date;
    private String site;
    private String red;
    private String black;
    private String redDuration;
    private String blackDuration;

    private String annotator;
    private String author;

    public List<Move> moves;

    private static final Charset GB18030 = Charset.forName("GB18030");

    public XQFManual() {
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

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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



    public String getAnnotator() {
        return annotator;
    }

    public void setAnnotator(String annotator) {
        this.annotator = annotator;
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
                ", event='" + event + '\'' +
                ", date='" + date + '\'' +
                ", site='" + site + '\'' +
                ", red='" + red + '\'' +
                ", black='" + black + '\'' +
                ", redDuration='" + redDuration + '\'' +
                ", blackDuration='" + blackDuration + '\'' +
                ", annotator='" + annotator + '\'' +
                ", author='" + author + '\'' +
                ", moves counts=" + moves.size() +
                '}';
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

}
