# Pikafish相关的资料

## 官方网站

	https://github.com/official-pikafish/Pikafish
	
关于pikafish的uci命令介绍：

	https://github.com/official-pikafish/Pikafish/wiki/UCI-&-Commands#standard-commands

关于引擎的优化设置：

	https://github.com/official-pikafish/Pikafish/wiki/Pikafish-FAQ#optimal-settings
	
	
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

## uci协议

	https://backscattering.de/chess/uci/#gui-isready

## Android上的参考实现

	https://github.com/peterosterlund2/droidfish/tree/master/DroidFishApp
	
##