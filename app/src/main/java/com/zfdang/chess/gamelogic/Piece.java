/*
    DroidFish - An Android chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com
    Copyright (C) 2012  Leo Mayer

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.zfdang.chess.gamelogic;

import java.util.HashMap;

/** Constants for different piece types.
 * https://www.xqbase.com/protocol/cchess_move.htm
 *
 * 白方(红色)棋子以大写字母表示，黑方棋子以小写字母表示
 * 红方以大写字元来表达兵种, PABNCRK分别代表兵、仕、相、马、炮、车、帅
 * 黑方以小写字元表达,      pabncrk分别代表卒、士、象、马、炮、车、将
 * */
public class Piece {
    public static final int EMPTY = 0;
    public static final byte EMPTY_BYTE = ' ';

    public static final int WSHUAI = 1; // K, 帅
    public static final int WSHI = 2; // A, 仕
    public static final int WXIANG = 3; // B, 相
    public static final int WMA = 4; // N, 马
    public static final int WJU = 5; // R, 车
    public static final int WPAO = 6; // C, 炮
    public static final int WBING = 7; // P, 兵

    public static final int BJIANG = 8; // k, 将
    public static final int BSHI = 9; // a, 士
    public static final int BXIANG = 10; // b, 象
    public static final int BMA = 11; // n, 马
    public static final int BJU = 12;   // r, 车
    public static final int BPAO = 13;  // c, 炮
    public static final int BZU = 14;   // p, 卒

    public static final int nPieceTypes = 15;

    // Create HashMap for piece names and piece values
    public static final HashMap<Integer, Byte> pieceByteMap = new HashMap<>();
    static {
        pieceByteMap.put(WSHUAI, (byte) 'K');
        pieceByteMap.put(WSHI, (byte) 'R');
        pieceByteMap.put(WXIANG, (byte) 'C');
        pieceByteMap.put(WMA, (byte) 'N');
        pieceByteMap.put(WJU, (byte) 'B');
        pieceByteMap.put(WPAO, (byte) 'A');
        pieceByteMap.put(WBING, (byte) 'P');

        pieceByteMap.put(BJIANG, (byte) 'k');
        pieceByteMap.put(BSHI, (byte) 'r');
        pieceByteMap.put(BXIANG, (byte) 'c');
        pieceByteMap.put(BMA, (byte) 'n');
        pieceByteMap.put(BJU, (byte) 'b');
        pieceByteMap.put(BPAO, (byte) 'a');
        pieceByteMap.put(BZU, (byte) 'p');
    }

    public static final HashMap<Byte, Integer> pieceValueMap = new HashMap<>();
    static {
        pieceValueMap.put((byte) 'K', WSHUAI);
        pieceValueMap.put((byte) 'R', WSHI);
        pieceValueMap.put((byte) 'C', WXIANG);
        pieceValueMap.put((byte) 'N', WMA);
        pieceValueMap.put((byte) 'B', WJU);
        pieceValueMap.put((byte) 'A', WPAO);
        pieceValueMap.put((byte) 'P', WBING);

        pieceValueMap.put((byte) 'k', BJIANG);
        pieceValueMap.put((byte) 'r', BSHI);
        pieceValueMap.put((byte) 'c', BXIANG);
        pieceValueMap.put((byte) 'n', BMA);
        pieceValueMap.put((byte) 'b', BJU);
        pieceValueMap.put((byte) 'a', BPAO);
        pieceValueMap.put((byte) 'p', BZU);
    }


    /**
     * Return true if p is a white piece, false otherwise.
     * Note that if p is EMPTY, an unspecified value is returned.
     */
    public static boolean isRed(int pType) { return pType <= WBING && pType >= WSHUAI; }
    public static boolean isBlack(int pType) {
        return pType <= BZU && pType >= BJIANG;
    }
    public static int swapColor(int pType) {
        if (pType == EMPTY)
            return EMPTY;
        return isRed(pType) ? pType + (BZU - WBING) : pType - (BZU - WBING);
    }

    // Return piece byte value by piece type
    static public byte getByteByValue(int i){
        // find in pieceByteMap
        if(pieceByteMap.containsKey(i)){
            return pieceByteMap.get(i);
        } else {
            return EMPTY_BYTE;
        }
    }

    // return piece value by piece byte
    static public int getValueByByte(byte b){
        // find in pieceValueMap
        if(pieceValueMap.containsKey(b)){
            return pieceValueMap.get(b);
        } else {
            return EMPTY;
        }
    }
}
