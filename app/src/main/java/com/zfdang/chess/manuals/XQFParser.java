package com.zfdang.chess.manuals;

import android.util.Log;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.jbbp.model.JBBPFieldUByte;
import com.zfdang.chess.gamelogic.Move;
import com.zfdang.chess.gamelogic.Piece;
import com.zfdang.chess.gamelogic.Position;

import java.io.IOException;
import java.nio.charset.Charset;

public class XQFParser {

    private static final String cszEncStreamMask = "[(C) Copyright Mr. Dong Shiwei.]";

    private static int square54Plus221(int x) {
        return x * x * 54 + 221;
    }

    static int nPieceOff; // // 局面初始位置的加密偏移值
    static int nSrcOff; // 着法起点的加密偏移值
    static int nDstOff; // 着法终点的加密偏移值
    static int nCommentOff; // 注释的加密偏移值
    static int[] nEncStream = new int[32]; // 密钥流
    static int nEncIndex; // 密钥流索引号
    static long dwEccoIndex;
    static long[] dwFileMove = new long[20];

//    private static void readAndDecrypt(InputStream inputStream, byte[] buffer, int nLen, int[] nEncStream, int[] nEncIndex) throws IOException {
//        inputStream.read(buffer, 0, nLen);
//        for (int i = 0; i < nLen; i++) {
//            buffer[i] -= nEncStream[nEncIndex[0]];
//            nEncIndex[0] = (nEncIndex[0] + 1) % 32;
//        }
//    }

    private static final Charset GB18030 = Charset.forName("GB18030");
    private static String readString(byte[] buffer) {
        int length = buffer[0];
        if(length < 0) {
            Log.e("XQFGame", "Invalid string length: " + length);
            return "";
        }
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

            if(manual.getVersion() <= 0x0A){
                // 非加密文件
                nPieceOff = nSrcOff = nDstOff = nCommentOff = 0;
                for (int i = 0; i < 32; i++) {
                    nEncStream[i] = 0;
                }
            } else {
                // 加密文件
//                nPieceOff = (square54Plus221(szTag[13] & 0xFF) * szTag[13] & 0xFF) & 0xFF;
//                nSrcOff = (square54Plus221(szTag[14] & 0xFF) * nPieceOff) & 0xFF;
//                nDstOff = (square54Plus221(szTag[15] & 0xFF) * nSrcOff) & 0xFF;
//                nCommentOff = ((szTag[12] & 0xFF) * 256 + (szTag[13] & 0xFF)) % 32000 + 767;
//                // 基本掩码
//                int nArg0 = szTag[3];
//                int[] nArgs = new int[4];
//                // 密钥 = 前段密钥 | (后段密钥 & 基本掩码)
//                for (int i = 0; i < 4; i ++) {
//                    nArgs[i] = szTag[8 + i] | (szTag[12 + i] & nArg0);
//                }
//                // 密钥流 = 密钥 & 密钥流掩码
//                for (int i = 0; i < 32; i ++) {
//                    nEncStream[i] = (byte) (nArgs[i % 4] & cszEncStreamMask.charAt(i));
//                }
            }
            nEncIndex = 0;

            // now to process board info
            // 0010 - 002F      这32个字节是棋局的开局局面，局面说明见“局面表示”
            // szPiecePos
            // 当版本号达到12时，还要进一步解密局面初始位置
            int[] nPiecePos = new int[32];
            if (manual.getVersion() < 12) {
                for (int i = 0; i < 32; i ++) {
                    nPiecePos[i] = (byte) (szPiecePos[i]);
                }
            } else {
                for (int i = 0; i < 32; i ++) {
                    nPiecePos[(nPieceOff + 1 + i) % 32] = (byte) (szPiecePos[i] - nPieceOff);
                }
            }

            manual.board.clear();
            // 01 - 16: 依次为红方的车马相士帅士相马车炮炮兵兵兵兵兵
            // 17 - 32: 依次为黑方的车马象士将士象马车炮炮卒卒卒卒卒
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[0]), Piece.WJU);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[1]), Piece.WMA);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[2]), Piece.WXIANG);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[3]), Piece.WSHI);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[4]), Piece.WSHUAI);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[5]), Piece.WSHI);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[6]), Piece.WXIANG);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[7]), Piece.WMA);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[8]), Piece.WJU);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[9]), Piece.WPAO);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[10]), Piece.WPAO);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[11]), Piece.WBING);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[12]), Piece.WBING);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[13]), Piece.WBING);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[14]), Piece.WBING);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[15]), Piece.WBING);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[16]), Piece.BJU);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[17]), Piece.BMA);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[18]), Piece.BXIANG);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[19]), Piece.BSHI);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[20]), Piece.BJIANG);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[21]), Piece.BSHI);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[22]), Piece.BXIANG);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[23]), Piece.BMA);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[24]), Piece.BJU);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[25]), Piece.BPAO);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[26]), Piece.BPAO);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[27]), Piece.BZU);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[28]), Piece.BZU);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[29]), Piece.BZU);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[30]), Piece.BZU);
            manual.board.setPieceByPosition(getPosFromValue(nPiecePos[31]), Piece.BZU);

            // 现在开始解析着法
            parseMoves(buffer, manual, buffer.length);

        } catch (IOException e) {
            Log.e("XQFParser", "Error parsing header", e);
        }
        return manual;
    }

    private static void init_crypt_keys(byte[] buffer) {
        if(buffer.length != 13) {
            Log.e("XQFParser", "Invalid key length: " + buffer.length);
        }
        // 加密key的格式
        //    # KeyMask   : dTByte;                         // 加密掩码
        //    # ProductId : dTDWord;                        // 产品号(厂商的产品号)
        //    # KeyOrA    : dTByte;
        //    # KeyOrB    : dTByte;
        //    # KeyOrC    : dTByte;
        //    # KeyOrD    : dTByte;
        //    # KeysSum   : dTByte;                         // 加密的钥匙
        //    # KeyXY     : dTByte;                         // 棋子布局位置钥匙
        //    # KeyXYf    : dTByte;                         // 棋谱起点钥匙
        //    # KeyXYt    : dTByte;                         // 棋谱终点钥匙

        int Head_keyMask = buffer[0] & 0xFF;
        int Head_keyOrA = buffer[5] & 0xFF;
        int Head_keyOrB = buffer[6] & 0xFF;
        int Head_keyOrC = buffer[7] & 0xFF;
        int Head_keyOrD = buffer[8] & 0xFF;
        int Head_keysSum = buffer[9] & 0xFF;
        int Head_keyXY = buffer[10] & 0xFF;
        int Head_keyXYf = buffer[11] & 0xFF;
        int Head_keyXYt = buffer[12] & 0xFF;

        // 棋子32个位置加密因子
        int KeyXY = ((((((Head_keyXY * Head_keyXY) * 3 + 9) * 3 + 8) * 2 + 1) * 3 + 8) * Head_keyXY) & 0xFF;

        // 棋谱加密因子(起点)
        int KeyXYf = ((((((Head_keyXYf * Head_keyXYf) * 3 + 9) * 3 + 8) * 2 + 1) * 3 + 8) * KeyXY) & 0xFF;

        // 棋谱加密因子(终点)
        int KeyXYt = ((((((Head_keyXYt * Head_keyXYt) * 3 + 9) * 3 + 8) * 2 + 1) * 3 + 8) * KeyXYf) & 0xFF;

        // 注解长度加密因子
        int KeyRMKSize = (((Head_keysSum * 256 + Head_keyXY) % 32000) + 767) & 0xFFFF;

        int[] B = new int[4];
        B[0] = (Head_keysSum & Head_keyMask) | Head_keyOrA;
        B[1] = (Head_keyXY & Head_keyMask) | Head_keyOrB;
        B[2] = (Head_keyXYf & Head_keyMask) | Head_keyOrC;
        B[3] = (Head_keyXYt & Head_keyMask) | Head_keyOrD;

        int[] F32Keys = new int[]{Byte.parseByte("[(C) Copyright Mr. Dong Shiwei.]")};
        for (int i = 0; i < 32; i++) {
            F32Keys[i] = (B[i % 4] & F32Keys[i]);
        }
    }

    private static void DecryptBuffer(byte[] buffer, int offset, int nLen) {
        for (int i = 0; i < nLen; i++) {
            buffer[offset + i] -= nEncStream[nEncIndex];
            nEncIndex = (nEncIndex + 1) % 32;
        }
    }

    private static void parseMoves(byte[] buffer, XQFManual manual, int bytesRead) {
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
        boolean hasNext = true;
        while (offset + 7 < bytesRead && hasNext) {
            // show buffer[] in 0x format
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02X ", buffer[offset + i]));
            }

            byte ucSrc, ucDst, ucFlag;
            int nCommentLen = 0;
            if(manual.getVersion() < 11) {
                // 未加密
                ucSrc = buffer[offset];
                ucDst = buffer[offset + 1];
                ucFlag = buffer[offset + 2];

                nCommentLen = buffer[offset + 4] + (buffer[offset + 5] << 8) + (buffer[offset + 6] << 16) + (buffer[offset + 7] << 24);
                if((ucFlag &  0xf0) == 0){
                    hasNext = false;
                }
            } else {
                // 加密的版本，需要按照加密的方法来处理
                // 先解密4字节，找到着法和flag
                DecryptBuffer(buffer, offset, 4);
                ucSrc = buffer[offset];
                ucDst = buffer[offset + 1];
                ucFlag = buffer[offset + 2];

                if((ucFlag & 0x20) != 0) {
                    // 表示有注释
                    DecryptBuffer(buffer, offset+4, 4);
                    nCommentLen = buffer[offset + 4] + (buffer[offset + 5] << 8) + (buffer[offset + 6] << 16) + (buffer[offset + 7] << 24);
                    nCommentLen -= nCommentOff;
                }
                if ((ucFlag & 0x80) == 0) {
                    hasNext = false;
                }
            }

            sb.append("; After decrypt: ");
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02X ", buffer[offset + i]));
                Log.d("XQFGame", "Move: " + sb.toString());
            }

            Position from = getPosFromValue(ucSrc - 24 - nSrcOff);
            Position to = getPosFromValue(ucDst - 32 - nDstOff);
            Move m = new Move(from, to);
            manual.moves.add(m);

            if (nCommentLen > 0) {
                Log.d("XQFGame", "Comment size:  size =  " + nCommentLen);

                offset += 8;
                // read the comment, make sure does not exceed the length of the buffer
                if (offset + nCommentLen > bytesRead) {
                    Log.e("XQFGame", "Invalid XQF file: bytes read:" + bytesRead + " offset: " + offset + " comment size: " + nCommentLen);
                    break;
                } else {
                    if(manual.getVersion() >= 11) {
                        DecryptBuffer(buffer, offset, nCommentLen);
                    }
                    String comment = new String(buffer, offset, nCommentLen, GB18030);
                    m.setComment(comment);
                    offset += nCommentLen;
                }
            } else {
                offset += 8;
            }
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
        int y = value % 10;
        int x = (value - y) / 10;
        return new Position(x, y);
    }

}
