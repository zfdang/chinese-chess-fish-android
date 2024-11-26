package com.zfdang.chess.manuals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

//[Game "Chinese Chess"]
//[Event "1998年全国象棋个人赛"]
//[Site "-"]
//[Date "1998-12-13"]
//[Round "-"]
//[RedTeam "黑龙江"]
//[Red "黑龙江 郭莉萍"]
//[BlackTeam "上海"]
//[Black "上海 单霞丽"]
//[Result "1-0"]
//[Opening "-"]
//[FEN "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1"]
//[Format "ICCS"]
//1. C3-C4 C9-E7
//2. B2-D2 G6-G5
//3. B0-C2 B9-C7
//4. A0-B0 A9-B9
//5. H2-H6 H9-G7
//6. H6-C6 I9-I8
//7. H0-G2 G7-H5
//8. G0-E2 I8-F8
//9. G3-G4 G5-G4
//10. E2-G4 E6-E5
//11. B0-B6 F8-F6
//12. F0-E1 B7-A7
//13. B6-B9 C7-B9
//14. C2-B4 F6-G6
//15. G4-E2 B9-D8
//16. C6-C9 D9-E8
//17. C9-A9 D8-B7
//18. A9-A8 H7-G7
//19. G2-H4 G7-H7
//20. H4-G2 G6-B6
//21. B4-C2 B7-C9
//22. A8-A9 C9-D7
//23. I0-F0 B6-G6
//24. C2-D4 D7-B6
//25. F0-F5 H5-G3
//26. F5-E5 A7-A8
//27. E5-E6 G6-E6
//28. D4-E6 A8-C8
//29. D2-B2 E9-D9
//30. E6-D4 H7-G7
//31. D4-C6 E7-G5
//32. B2-D2 D9-E9
//33. E0-F0 E8-F7
//34. C6-D8 B6-D7
//35. D8-F7 C8-F8
//36. F7-G5 G3-F5
//37. F0-E0 G7-G2
//38. D2-G2 F5-E3
//39. G2-G9 F9-E8
//40. G9-G6 E3-D5
//41. G6-C6 E8-F7
//42. G5-F7 D5-F4
//43. A9-B9 F4-H5
//44. E1-F2 H5-I3
//45. B9-B1 I3-H5
//46. B1-E1 E9-D9
//47. C6-D6 D7-F6
//48. C4-C5 H5-G3
//49. D6-D3 F6-E4
//50. F7-D6
//1-0

public class PGNGame {
    public String event;
    public String site;
    public String date;
    public String round;
    public String redTeam;
    public String red;
    public String blackTeam;
    public String black;
    public String result;
    public String opening;
    public String fen;
    public String format;
    public List<String> moves;

    public PGNGame() {
        moves = new ArrayList<>();
    }

    public static List<PGNGame> parse(InputStream inputStream) throws IOException {
        List<PGNGame> games = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        PGNGame currentGame = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("[")) {
                if (currentGame == null) {
                    currentGame = new PGNGame();
                }
                parseTag(line, currentGame);
            } else {
                if (currentGame != null) {
                    parseMoves(line, currentGame);
                }
            }

            if (line.equals("1-0") || line.equals("0-1") || line.equals("1/2-1/2")) {
                if (currentGame != null) {
                    currentGame.result = line;
                    games.add(currentGame);
                    currentGame = null;
                }
            }
        }

        return games;
    }

    private static void parseTag(String line, PGNGame game) {
        String[] parts = line.split("\"");
        if (parts.length < 2) return;

        String tag = parts[0].substring(1).trim();
        String value = parts[1].trim();

        switch (tag) {
            case "Event":
                game.event = value;
                break;
            case "Site":
                game.site = value;
                break;
            case "Date":
                game.date = value;
                break;
            case "Round":
                game.round = value;
                break;
            case "RedTeam":
                game.redTeam = value;
                break;
            case "Red":
                game.red = value;
                break;
            case "BlackTeam":
                game.blackTeam = value;
                break;
            case "Black":
                game.black = value;
                break;
            case "Result":
                game.result = value;
                break;
            case "Opening":
                game.opening = value;
                break;
            case "FEN":
                game.fen = value;
                break;
            case "Format":
                game.format = value;
                break;
        }
    }

    private static void parseMoves(String line, PGNGame game) {
        String[] moves = line.split("\\s+");
        for (String move : moves) {
            if (!move.matches("\\d+\\.")) {
                game.moves.add(move);
            }
        }
    }

    @Override
    public String toString() {
        return "PGNGame{" +
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
}