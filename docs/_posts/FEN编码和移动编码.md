# 中国象棋相关编码


## 棋盘编码

	https://www.xqbase.com/protocol/cchess_move.htm

![](board.png)

**棋盘的编号：从左到右为a-i，从下到上为0-9**


## 棋子编码

	https://www.xqbase.com/protocol/pgnfen2.htm
	https://www.xqbase.com/protocol/cchess_move.htm

```
白方(红色)棋子以大写字母表示
黑方棋子以小写字母表示

红方以大写字元来表达兵种。
PABNCRK分别代表兵、仕、相、马、炮、车、帅；

黑方以小写字元表达。
pabncrk分别代表卒、士、象、马、炮、车、将

```



## FEN编码

```
按白方(红色)视角，描述由上至下、由左至右的盘面，以/符号来分隔相邻横列。横列的连续空格以阿拉伯数字表示，例如5即代表连续5个空格。

白方(红色)大写字母、黑方小写字母。

棋盘的编号：从左到右为a-i，从下到上为0-9。

FEN棋子位置编码为：

a9-i9/a8-i8/ .../a1-i1/a0-i0
```


##动作编码

	https://www.xqbase.com/protocol/cchess_fen.htm
	https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation
	

