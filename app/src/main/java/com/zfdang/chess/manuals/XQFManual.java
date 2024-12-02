package com.zfdang.chess.manuals;

import android.util.Log;

import com.zfdang.chess.gamelogic.Board;
import com.zfdang.chess.gamelogic.Move;

import java.nio.charset.Charset;
import java.util.ArrayList;

// XQF format
// https://github.com/zfdang/chinese-chess-fish-android/blob/master/%E6%A3%8B%E8%B0%B1/XQF%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F%E8%AF%B4%E6%98%8E.TXT
public class XQFManual {
    private String format;
    private int version;

    private String annotation;

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

    public MoveNode getHeadMove() {
        return headMove;
    }

    // create public class MoveNode
    public static class MoveNode {
        public Move move;
        public ArrayList<MoveNode> nextMoves;

        public MoveNode(Move move) {
            this.move = move;
            nextMoves = new ArrayList<>();
        }

        public void addNextMove(MoveNode moveNode) {
            nextMoves.add(moveNode);
        }
    }

    private MoveNode headMove;

    private static final Charset GB18030 = Charset.forName("GB18030");

    public XQFManual() {
        headMove = new MoveNode(null);
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

    public boolean hasEmptyMove(){
        if(headMove.move == null && headMove.nextMoves.size() == 0)
            return true;
        return false;
    }

    public boolean validateAllMoves(){
        return validateMove(this.board, this.headMove);
    }

    private boolean validateMove(Board _board, MoveNode node) {
        if(node == null){
            return true;
        }

        Board b = new Board(_board);

        // 验证move自己是否正确
        if(node.move != null) {
            if(!b.doMove(node.move)) {
                Log.e("XQFManual", "Invalid move: " + node.move);
                return false;
            }
        }

        // 验证后续moves时候正确
        for(MoveNode nextNode : node.nextMoves) {
            boolean result = validateMove(b, nextNode);
            if(!result) {
                return false;
            }
        }
        return true;
    }

    private String printAllMoveNodes() {
        return iterateMoveNode(headMove, 0);
    }

    private String iterateMoveNode(MoveNode node, int depth) {
        StringBuilder sb = new StringBuilder();
        if(node.move == null) {
            if(node.nextMoves.size() > 1) {
                sb.append(" root{");
            } else {
                sb.append(" root ");
            }
        } else {
            if(node.nextMoves.size() > 1) {
                // 有分叉
                sb.append(" " + node.move.getUCCIString() + "{" );
            } else {
                sb.append(" " + node.move.getUCCIString() + " ");
            }
        }

        for(int i = 0; i < node.nextMoves.size(); i++) {
            MoveNode nextNode = node.nextMoves.get(i);
            if(i == 0){
                sb.append(iterateMoveNode(nextNode, depth + 1));
            } else {
                sb.append("\n");
                for(int j = 0; j <= depth; j++) {
                    sb.append("      ");
                }
                sb.append(iterateMoveNode(nextNode, depth + 1));
            }
        }

        return sb.toString();
    }

    // toString method
    @Override
    public String toString() {
        // iterate all MoveNode, and generate a string
        String moves = printAllMoveNodes();

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
                ", moves=\n" + moves + "\n" +
                '}';


    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        if(annotation != null) {
            annotation = annotation.strip();
            annotation = annotation.replace("&nbsp;", " ");
        }
        this.annotation = annotation;
    }
}
