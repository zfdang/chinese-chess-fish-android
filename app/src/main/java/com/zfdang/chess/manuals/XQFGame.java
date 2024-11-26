package com.zfdang.chess.manuals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class XQFGame {
    private String event;
    private String site;
    private String date;
    private String round;
    private String redTeam;
    private String red;
    private String blackTeam;
    private String black;
    private String result;
    private String opening;
    private String fen;
    private String format;
    private List<String> moves;

    public XQFGame() {
        moves = new ArrayList<>();
    }

    // Getters and setters for each field
    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getRedTeam() {
        return redTeam;
    }

    public void setRedTeam(String redTeam) {
        this.redTeam = redTeam;
    }

    public String getRed() {
        return red;
    }

    public void setRed(String red) {
        this.red = red;
    }

    public String getBlackTeam() {
        return blackTeam;
    }

    public void setBlackTeam(String blackTeam) {
        this.blackTeam = blackTeam;
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

    public String getOpening() {
        return opening;
    }

    public void setOpening(String opening) {
        this.opening = opening;
    }

    public String getFen() {
        return fen;
    }

    public void setFen(String fen) {
        this.fen = fen;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<String> getMoves() {
        return moves;
    }

    public void setMoves(List<String> moves) {
        this.moves = moves;
    }

    @Override
    public String toString() {
        return "XQFGame{" +
                "event='" + event + '\'' +
                ", site='" + site + '\'' +
                ", date='" + date + '\'' +
                ", round='" + round + '\'' +
                ", redTeam='" + redTeam + '\'' +
                ", red='" + red + '\'' +
                ", blackTeam='" + blackTeam + '\'' +
                ", black='" + black + '\'' +
                ", result='" + result + '\'' +
                ", opening='" + opening + '\'' +
                ", fen='" + fen + '\'' +
                ", format='" + format + '\'' +
                ", moves=" + moves +
                '}';
    }

    public static List<XQFGame> parse(InputStream inputStream) throws IOException {
        List<XQFGame> games = new ArrayList<>();
        byte[] buffer = new byte[1024];
        int bytesRead = inputStream.read(buffer);

        if (bytesRead < 0x400) {
            throw new IOException("Invalid XQF file");
        }

        XQFGame game = new XQFGame();
        parseHeader(buffer, game);
        parseMoves(buffer, game, bytesRead);

        games.add(game);
        return games;
    }

    private static void parseHeader(byte[] buffer, XQFGame game) {
        game.setEvent(readString(buffer, 0x50, 0x90));
        game.setSite(readString(buffer, 0xD0, 0x110));
        game.setDate(readString(buffer, 0x110, 0x120));
        game.setRedTeam(readString(buffer, 0x130, 0x140));
        game.setRed(readString(buffer, 0x140, 0x150));
        game.setBlackTeam(readString(buffer, 0x150, 0x160));
        game.setBlack(readString(buffer, 0x160, 0x170));
        game.setResult(parseResult(buffer[0x33]));
    }

    private static void parseMoves(byte[] buffer, XQFGame game, int bytesRead) {
        int offset = 0x400;
        while (offset < bytesRead) {
            int from = buffer[offset] - 24;
            int to = buffer[offset + 1] - 32;
            if (from < 0 || to < 0) break;
            game.getMoves().add(String.format("%02d-%02d", from, to));
            offset += 8;
        }
    }

    private static String readString(byte[] buffer, int start, int end) {
        int length = buffer[start];
        return new String(buffer, start + 1, length, StandardCharsets.UTF_8).trim();
    }

    private static String parseResult(byte result) {
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
}