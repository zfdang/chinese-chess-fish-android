package com.zfdang.chess.manuals;

import android.util.Log;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.Piece;
import com.zfdang.chess.gamelogic.Position;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class XQFParser {
    private static final Charset GB18030 = Charset.forName("GB18030");

    private static String readString(byte[] buffer) {
        int length = buffer[0] & 0xFF;
        if(length < 0) {
            Log.e("XQFGame", "Invalid string length: " + length);
            return "";
        }
        length = Math.min(length, buffer.length - 1);
        return new String(buffer, 1, length, GB18030).trim();
    }


    static public XQFManual parse(byte[] buffer){
        XQFManual manual = new XQFManual();

        final JBBPParser headerParser = JBBPParser.prepare(
                "byte[2] szMagic;"
                        + "byte[1] szVersion;"
                        + "byte[13] szKeys;"
                        + "byte[32] szPiecePos;"
                        + "byte[16] szResult;"
                        + "byte[16] szSetUp;"
                        + "byte[64] szTitle;"
                        + "byte[64] szReserved1;"
                        + "byte[64] szEvent;"
                        + "byte[16] szDate;"
                        + "byte[16] szSite;"
                        + "byte[16] szRed;"
                        + "byte[16] szBlack;"
                        + "byte[64] szRuleTime;"
                        + "byte[16] szRedTime;"
                        + "byte[16] szBlackTime;"
                        + "byte[32] szReserved2;"
                        + "byte[16] szAnnotator;"
                        + "byte[16] szAuthor;"
                        + "byte[16] szReserved3;"
        );

        try {
            // use JBBP to unpack header
            final JBBPFieldStruct header =  headerParser.parse(buffer);
            byte[] szMagic = header.findFieldForNameAndType("szMagic", JBBPFieldArrayByte.class).getArray();
            byte[] szVersion = header.findFieldForNameAndType("szVersion", JBBPFieldArrayByte.class).getArray();
            byte[] szKeys = header.findFieldForNameAndType("szKeys", JBBPFieldArrayByte.class).getArray();
            byte[] szPiecePos = header.findFieldForNameAndType("szPiecePos", JBBPFieldArrayByte.class).getArray();
            byte[] szResult = header.findFieldForNameAndType("szResult", JBBPFieldArrayByte.class).getArray();
            byte[] szSetUp = header.findFieldForNameAndType("szSetUp", JBBPFieldArrayByte.class).getArray();
            byte[] szTitle = header.findFieldForNameAndType("szTitle", JBBPFieldArrayByte.class).getArray();
            byte[] szEvent = header.findFieldForNameAndType("szEvent", JBBPFieldArrayByte.class).getArray();
            byte[] szDate = header.findFieldForNameAndType("szDate", JBBPFieldArrayByte.class).getArray();
            byte[] szSite = header.findFieldForNameAndType("szSite", JBBPFieldArrayByte.class).getArray();
            byte[] szRed = header.findFieldForNameAndType("szRed", JBBPFieldArrayByte.class).getArray();
            byte[] szBlack = header.findFieldForNameAndType("szBlack", JBBPFieldArrayByte.class).getArray();
            byte[] szRuleTime = header.findFieldForNameAndType("szRuleTime", JBBPFieldArrayByte.class).getArray();
            byte[] szRedTime = header.findFieldForNameAndType("szRedTime", JBBPFieldArrayByte.class).getArray();
            byte[] szBlackTime = header.findFieldForNameAndType("szBlackTime", JBBPFieldArrayByte.class).getArray();
            byte[] szAnnotator = header.findFieldForNameAndType("szAnnotator", JBBPFieldArrayByte.class).getArray();
            byte[] szAuthor = header.findFieldForNameAndType("szAuthor", JBBPFieldArrayByte.class).getArray();

            // format
            if(szMagic[0] == 'X' && szMagic[1] == 'Q'){
                manual.setFormat("XQ");
            } else {
                Log.e("XQFParser", "Invalid XQF file format");
                return null;
            }

            // version
            manual.setVersion(szVersion[0]);

            // process other head fields
            manual.setTitle(readString(szTitle));
            manual.setEvent(readString(szEvent));
            manual.setSite(readString(szSite));
            manual.setDate(readString(szDate));
            manual.setRed(readString(szRed));
            manual.setBlack(readString(szBlack));
            manual.setRedDuration(readString(szRedTime));
            manual.setBlackDuration(readString(szBlackTime));
            manual.setAnnotator(readString(szAnnotator));
            manual.setAuthor(readString(szAuthor));
            manual.setResult(parseResult(szResult[3]));
            manual.setCategory(parseCategory(szSetUp[0]));

            XQFKey keys = manual.getVersion() <= 0x0A ? null : initDecryptKey(szKeys);
//            Log.d("XQFParser", "Keys: " + keys);

            // now to process board info
            // 0010 - 002F      这32个字节是棋局的开局局面，局面说明见“局面表示”
            // szPiecePos
            // 当版本号达到12时，还要进一步解密局面初始位置
            byte[] piecePos = decryptPiecePos(szPiecePos, manual.getVersion(), keys);
//            Log.d("XQFParser", "PiecePos: " + Arrays.toString(piecePos));

            // 01 - 16: 依次为红方的车马相士帅士相马车炮炮兵兵兵兵兵
            // 17 - 32: 依次为黑方的车马象士将士象马车炮炮卒卒卒卒卒
            int[] pieceKinds = {
                    Piece.WJU, Piece.WMA, Piece.WXIANG, Piece.WSHI, Piece.WSHUAI, Piece.WSHI, Piece.WXIANG, Piece.WMA, Piece.WJU,
                    Piece.WPAO, Piece.WPAO,
                    Piece.WBING, Piece.WBING, Piece.WBING, Piece.WBING, Piece.WBING,
                    Piece.BJU, Piece.BMA, Piece.BXIANG, Piece.BSHI, Piece.BJIANG, Piece.BSHI, Piece.BXIANG, Piece.BMA, Piece.BJU,
                    Piece.BPAO, Piece.BPAO, Piece.BZU, Piece.BZU, Piece.BZU, Piece.BZU, Piece.BZU
            };

            // 读取原棋盘
            manual.board.clear();
            for (int i = 0; i < 32; i++) {
                int value = piecePos[i] & 0xFF;
                if (value == 0xFF) {
                    // 0xFF表示没有棋子
                    continue;
                }
                manual.board.setPieceByPosition(getPosFromValue(value), pieceKinds[i]);
            }
            Log.d("XQFParser", "Board: " + manual.board.toFENString());

//        B. 棋谱记录 (0400 - 文件尾部)
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
//        0x0400 - 0x0407: 第一步棋的记录；没有着法，但是注解是整个棋局的注解，需要读取出来
            // 先解密
            XQFBufferDecoder stepBaseBuff;
            if (manual.getVersion() <= 0x0A) {
                stepBaseBuff = new XQFBufferDecoder(Arrays.copyOfRange(buffer, 0x400, buffer.length));
            } else {
                stepBaseBuff = new XQFBufferDecoder(decodeBuff(keys, Arrays.copyOfRange(buffer, 0x400, buffer.length)));
            }
//            Log.d("XQFParser", "StepBaseBuff: " + stepBaseBuff);

            // 0x0400 - 0x0407: 第一步棋的记录；没有着法，但是注解是整个棋局的注解，需要读取出来
            String gameAnnotation = readAnnotationInfo(stepBaseBuff, manual.getVersion(), keys);
            manual.setAnnotation(gameAnnotation);
            Log.d("XQFParser", "Game Annotation: " + manual.getAnnotation());

            // 开始读取剩下的着法和注解
            readSteps(stepBaseBuff, keys, manual, manual.getHeadMove());
        } catch (IOException e) {
            Log.e("XQFParser", "Error parsing header", e);
        }
        return manual;
    }

    private static XQFKey initDecryptKey(byte[] cryptKeys) {
        XQFKey keys = new XQFKey();

        byte headKeyMask = cryptKeys[0];
        int headProductId = ((cryptKeys[4] & 0xFF) << 24) |
                ((cryptKeys[3] & 0xFF) << 16) |
                ((cryptKeys[2] & 0xFF) << 8) |
                (cryptKeys[1] & 0xFF);

        int headKeyOrA = cryptKeys[5] & 0xFF;
        int headKeyOrB = cryptKeys[6] & 0xFF;
        int headKeyOrC = cryptKeys[7] & 0xFF;
        int headKeyOrD = cryptKeys[8] & 0xFF;
        int headKeysSum = cryptKeys[9] & 0xFF;
        int headKeyXY = cryptKeys[10] & 0xFF;
        int headKeyXYf = cryptKeys[11] & 0xFF;
        int headKeyXYt = cryptKeys[12] & 0xFF;

        // Calculate KeyXY
        int bKey = headKeyXY & 0xFF;
        int keyXY = (((((bKey * bKey * 3 + 9) * 3 + 8) * 2 + 1) * 3 + 8) * bKey) & 0xFF;
        keys.setKeyXY(keyXY);

        // Calculate KeyXYf
        bKey = headKeyXYf & 0xFF;
        int keyXYf = (((((bKey * bKey * 3 + 9) * 3 + 8) * 2 + 1) * 3 + 8) * keyXY) & 0xFF;
        keys.setKeyXYf(keyXYf);

        // Calculate KeyXYt
        bKey = headKeyXYt & 0xFF;
        int keyXYt = (((((bKey * bKey * 3 + 9) * 3 + 8) * 2 + 1) * 3 + 8) * keyXYf) & 0xFF;
        keys.setKeyXYt(keyXYt);

        // Calculate KeyRMKSize
        int keyRMKSize = (((headKeysSum * 256 + headKeyXY) % 32000) + 767) & 0xFFFF;
        keys.setKeyRMKSize(keyRMKSize);

        // Calculate FKeyBytes
        byte b1 = (byte)((headKeysSum & headKeyMask) | headKeyOrA);
        byte b2 = (byte)((headKeyXY & headKeyMask) | headKeyOrB);
        byte b3 = (byte)((headKeyXYf & headKeyMask) | headKeyOrC);
        byte b4 = (byte)((headKeyXYt & headKeyMask) | headKeyOrD);

        keys.setFKeyBytes(new byte[]{b1, b2, b3, b4});
        keys.initF32Keys();

        return keys;
    }

    private static byte[] decryptPiecePos(byte[] manStr, int version, XQFKey keys) {
        byte[] tmpMan = new byte[32];

        if (keys == null) {
            System.arraycopy(manStr, 0, tmpMan, 0, 32);
            return tmpMan;
        }

        // 棋子的顺序是乱的，先调整顺序
        for (int i = 0; i < 32; i++) {
            if (version >= 12) {
                tmpMan[(keys.getKeyXY() + i + 1) & 0x1F] = manStr[i];
            } else {
                tmpMan[i] = manStr[i];
            }
        }

        // 再调整每个棋子的位置
        for (int i = 0; i < 32; i++) {
            tmpMan[i] = (byte)((tmpMan[i] - keys.getKeyXY()) & 0xFF);
            if ((tmpMan[i] & 0xFF) > 89) {
                tmpMan[i] = (byte)0xFF;
            }
        }

        return tmpMan;
    }

    private static byte[] decodeBuff(XQFKey keys, byte[] buff) {
        int nPos = 0x400;
        byte[] deBuff = Arrays.copyOf(buff, buff.length);

        for (int i = 0; i < buff.length; i++) {
            int value = deBuff[i] & 0xFF;
            int keyByte = keys.getF32Keys()[(nPos + i) % 32] & 0xFF;
            deBuff[i] = (byte)((value - keyByte) & 0xFF);
        }

        return deBuff;
    }

    private static String readAnnotationInfo(XQFBufferDecoder buffDecoder, int version, XQFKey keys) {
        byte[] stepInfo = buffDecoder.readBytes(4);
        int annoteLen = 0;

        if (version <= 0x0A) {
            annoteLen = buffDecoder.readInt();
        } else {
            stepInfo[2] &= 0xE0;
            if ((stepInfo[2] & 0x20) != 0) {
                annoteLen = buffDecoder.readInt() - keys.getKeyRMKSize();
            }
        }

        return annoteLen > 0 ? buffDecoder.readString(annoteLen, GB18030) : null;
    }

    private static void readSteps(XQFBufferDecoder buffDecoder, XQFKey keys,
                                  XQFManual manual, XQFManual.MoveNode node) {
        byte[] stepInfo = buffDecoder.readBytes(4);
        if (stepInfo.length == 0) return;

        // print stepInfo in hex format
//        StringBuilder sb = new StringBuilder();
//        for (byte b : stepInfo) {
//            sb.append(Integer.toHexString(b & 0xFF)).append(" ");
//        }
//        Log.d("XQFParser", "StepInfo: " + sb.toString());

        int annoteLen = 0;
        boolean hasNextStep = false;
        boolean hasVarStep = false;

        int moveFrom, moveTo;
        if (manual.getVersion() <= 0x0A) {
            // 低版本在走子数据后紧跟着注释长度，长度为0则没有注释
            if ((stepInfo[2] & 0xF0) != 0) hasNextStep = true;
            if ((stepInfo[2] & 0x0F) != 0) hasVarStep = true;
            annoteLen = buffDecoder.readInt();

            // moveFrom = stepInfo[0] & 0xFF - 0x18; // this does not work !!!!!!, we need the parentheses
            moveFrom =  ((stepInfo[0] & 0xFF) - 0x18) & 0xFF;
            moveTo =  ((stepInfo[1] & 0xFF) - 0x20) & 0xFF;
        } else {
            // 高版本通过flag来标记有没有注释，有则紧跟着注释长度和注释字段
            stepInfo[2] &= 0xE0;
            if ((stepInfo[2] & 0x80) != 0) hasNextStep = true; // #有后续
            if ((stepInfo[2] & 0x40) != 0) hasVarStep = true; // 有变招
            if ((stepInfo[2] & 0x20) != 0) { // 有注释
                annoteLen = buffDecoder.readInt() - keys.getKeyRMKSize();
            }

            moveFrom = ((((stepInfo[0] & 0xFF) - 0x18) & 0xFF ) - keys.getKeyXYf()) & 0xFF;
            moveTo = ((((stepInfo[1] & 0xFF) - 0x20) & 0xFF ) - keys.getKeyXYt()) & 0xFF;
        }

//        Log.d("XQFParser", "Move: " + moveFrom + " -> " + moveTo);
//        Log.d("XQFParser", "HasNextStep: " + hasNextStep + ", HasVarStep: " + hasVarStep + ", AnnoteLen: " + annoteLen);

        Position from = getPosFromValue(moveFrom);
        Position to = getPosFromValue(moveTo);
        Move move = new Move(from, to);
        String annote = annoteLen > 0 ? buffDecoder.readString(annoteLen, GB18030) : null;
        move.setComment(annote);
//        Log.d("XQFParser", "Move: " + move);

        // add movenode
        XQFManual.MoveNode nextNode = new XQFManual.MoveNode(move);
        node.addNextMove(nextNode);

        if (hasNextStep) {
            readSteps(buffDecoder, keys, manual, nextNode);
        }

        if (hasVarStep) {
            readSteps(buffDecoder, keys, manual, node);
        }
    }

    private static String parseResult(byte result) {
        // 0033             棋局的结果: 0x00-未知,0x01-红胜,0x02-黑胜,0x03-和棋
        switch (result) {
            case 0x01:
                return "红胜";
            case 0x02:
                return "黑胜";
            case 0x03:
                return "平局";
            default:
                return "未知";
        }
    }

    private static String parseCategory(byte b) {
        //     0040             棋局的类型: 0x00-全局文件, 0x01-布局文件,
        //                                 0x02-中局文件, 0x03-残局文件
        if (b == 0x00) {
            return "全局";
        } else if (b == 0x01) {
            return "布局";
        } else if (b == 0x02) {
            return "中局";
        } else if (b == 0x03) {
            return "残局";
        }
        return null;
    }

    private static Position getPosFromValue(int value) {
        // 在XQF文件中，一个棋盘位置用一个字节表示，字节值 = X * 10 + Y
        // 其中X坐标从0到8,Y坐标从0到9，坐标的原点(0,0)在棋盘的左下角。
        // 我们的棋盘，坐标原点在左上角，所以需要转换一下
        int y = value % 10;
        int x = (value - y) / 10;
        return new Position(x, 9 - y);
    }

}
