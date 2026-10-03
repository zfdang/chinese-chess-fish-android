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

/** Constants for different piece types.
 * https://www.xqbase.com/protocol/cchess_move.htm
 *
 * 白方(红色)棋子以大写字母表示，黑方棋子以小写字母表示
 * 红方以大写字元来表达兵种, PABNCRK分别代表兵、仕、相、马、炮、车、帅
 * 黑方以小写字元表达,      pabncrk分别代表卒、士、象、马、炮、车、将
 * */
public class Piece {
    public static final int EMPTY = 0;
    public static final Character EMPTY_CHAR = ' ';

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

    /**
     * Return true if p is a white piece, false otherwise.
     * Note that if p is EMPTY, an unspecified value is returned.
     */
    public static boolean isRed(int pType) { return pType <= WBING && pType >= WSHUAI; }
    public static boolean isBlack(int pType) {
        return pType <= BZU && pType >= BJIANG;
    }
    public static boolean isValid(int pType) {
        return pType <= BZU && pType >= WSHUAI;
    }
    public static boolean isDiagonalPiece(int pType) {
        return pType == WXIANG || pType == BXIANG || pType == WSHI || pType == BSHI || pType == WMA || pType == BMA;
    }

    public static int swapColor(int pType) {
        if (pType == EMPTY)
            return EMPTY;
        return isRed(pType) ? pType + (BZU - WBING) : pType - (BZU - WBING);
    }

    // 用数组代替HashMap查找，避免装箱
    private static final char[] CHARS = {' ', 'K', 'A', 'B', 'N', 'R', 'C', 'P', 'k', 'a', 'b', 'n', 'r', 'c', 'p'};
    private static final char[] NAMES = {' ', '帅', '仕', '相', '马', '车', '炮', '兵', '将', '士', '象', '马', '车', '炮', '卒'};
    private static final int[] VALUES_BY_CHAR = new int[128];
    static {
        for (int i = WSHUAI; i <= BZU; i++) {
            VALUES_BY_CHAR[CHARS[i]] = i;
        }
    }

    // 不装箱的版本，供热路径使用
    static char charOf(int i){
        return isValid(i) ? CHARS[i] : ' ';
    }

    // Return piece byte value by piece type
    static public Character getCharByValue(int i){
        return isValid(i) ? CHARS[i] : EMPTY_CHAR;
    }

    // return piece name by piece value
    static public Character getNameByValue(int i){
        return isValid(i) ? NAMES[i] : EMPTY_CHAR;
    }

    // return piece value by piece byte
    static public int getValueByChar(char b){
        return b < VALUES_BY_CHAR.length ? VALUES_BY_CHAR[b] : EMPTY;
    }
}
