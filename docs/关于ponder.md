# 关于ponder

## 一个典型的交互

engine在最后的时候，返回了bestmove b2e2 ponder h9g7

```
> position startpos
> go infinite
info string NNUE evaluation using pikafish.nnue enabled
info depth 1 seldepth 1 multipv 1 score cp 8 nodes 74 nps 37000 hashfull 0 tbhits 0 time 2 pv b2e2
info depth 2 seldepth 2 multipv 1 score cp 20 nodes 191 nps 95500 hashfull 0 tbhits 0 time 2 pv h2e2
info depth 3 seldepth 2 multipv 1 score cp 21 nodes 267 nps 89000 hashfull 0 tbhits 0 time 3 pv b2e2
info depth 4 seldepth 2 multipv 1 score cp 65 nodes 342 nps 114000 hashfull 0 tbhits 0 time 3 pv b2e2
info depth 5 seldepth 2 multipv 1 score cp 320 nodes 389 nps 129666 hashfull 0 tbhits 0 time 3 pv b2e2
info depth 6 seldepth 6 multipv 1 score cp 30 nodes 925 nps 231250 hashfull 0 tbhits 0 time 4 pv h2e2 h7e7 h0g2
info depth 7 seldepth 5 multipv 1 score cp 35 nodes 1636 nps 327200 hashfull 0 tbhits 0 time 5 pv b2e2 b7e7 b0c2
info depth 8 seldepth 8 multipv 1 score cp 34 nodes 2972 nps 424571 hashfull 1 tbhits 0 time 7 pv b2e2 h7e7 h0g2 b9c7 b0c2 h9g7
info depth 9 seldepth 9 multipv 1 score cp 33 nodes 5284 nps 587111 hashfull 3 tbhits 0 time 9 pv h2e2 b7e7 h0g2 b9c7 i0h0 h9g7 b0c2
> stop
info depth 10 seldepth 10 multipv 1 score cp 22 nodes 12888 nps 758117 hashfull 7 tbhits 0 time 17 pv h2e2 h9g7 h0g2 i9h9 c3c4 b7e7 i0h0 b9c7
bestmove b2e2 ponder h9g7
```

## ponder的意义

一般情况下，engine是一个无状态的实现。但同时也意味着巨大的浪费。

ponder和ponder的设计是这个思路：

我要出着了b2e2，但是我觉得我出了这个招数之后，对手大概会出这个着数h9g7。如果对手真的出了这个着数，那么GUI你可以见到的告诉我，ponderhit, 猜测命中了，那我就接着上次的寻找最优着数了。



## 完整的交互

```
GUI -> engine1: position startpos
GUI -> engine1: go wtime 100000 winc 1000 btime 100000 binc 1000
engine1 -> GUI: bestmove e2e4 ponder e7e6
GUI -> engine1: position startpos moves e2e4 e7e6
GUI -> engine1: go ponder wtime 98123 winc 1000 btime 100000 binc 1000
[user or other engine plays the expected e7e6 move]
GUI -> engine1: ponderhit
[engine keeps thinking]
engine1 -> GUI: bestmove d2d4 ponder d7d5
```
