# Pikafish相关的资料

## 官方网站

	https://github.com/official-pikafish/Pikafish
	
关于pikafish的uci命令介绍：

	https://github.com/official-pikafish/Pikafish/wiki/UCI-&-Commands#standard-commands

关于引擎的优化设置：

	https://github.com/official-pikafish/Pikafish/wiki/Pikafish-FAQ#optimal-settings
	

## 引擎文件的更新

引擎下载：

	https://github.com/official-pikafish/Pikafish/releases

Networks下载：

	https://github.com/official-pikafish/Networks/releases


下载之后放到程序目录：

	app/src/main/assets/ 

## pikafish的setoption

命令格式：setoption name id [ value x ]

This is sent to the engine when the user wants to change the internal parameters of the engine. For the button type no value is needed.

当前所有的options：

```
option name Debug Log File type string default
option name NumaPolicy type string default auto
option name Threads type spin default 1 min 1 max 1024
option name Hash type spin default 16 min 1 max 33554432
option name Clear Hash type button
option name Ponder type check default false
option name MultiPV type spin default 1 min 1 max 128
option name Move Overhead type spin default 10 min 0 max 5000
option name nodestime type spin default 0 min 0 max 10000
option name UCI_ShowWDL type check default false
option name EvalFile type string default pikafish.nnue
```

例子：

```
设置为6线程，一般为cpu核心数-2
> setoption name Threads value 6

在结果里显示approximate WDL statistics 
> setoption name UCI_ShowWDL value true

设置 NNUE evaluation 
> setoption name EvalFile value pikafish.nnue

设置hash表的大小
> setoption name Hash value 128

允许机器在人下棋的时候思考
> setoption name ponder value true

输出多条最优路径，会增加运算量
> setoption name MultiPV value 3

清空哈希表
> setoption name Clear Hash
```

## uci协议

	https://backscattering.de/chess/uci/#gui-isready

## Android上的参考实现

	https://github.com/peterosterlund2/droidfish/tree/master/DroidFishApp
	
##