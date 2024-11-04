# ComputerPlayer的逻辑

## UCI engine相关代码

这组代码是从Droidfish借鉴过来的，包括以下代码：

```
└── petero
    └── droidfish
        ├── EngineOptions.java
        ├── FileUtil.java
        └── engine
            ├── ComputerPlayer.java
            ├── ExternalEngine.java
            ├── ExternalPikafishEngine.java
            ├── LocalPipe.java
            ├── NativeUtil.java
            ├── UCIEngine.java
            ├── UCIEngineBase.java
            └── UCIOptions.java
```


## ComputerPlayer调用逻辑
 the chain to start engine

```
ComputerPlayer.queueStartEngine
      -> ComputerPlayer.handleQueue
          -> ComputerPlayer.handleIdleState
              -> ComputerPlayer.startEngine
                  -> UCIEngineBase.initialize
                      -> ExternalEngine.startProcess
                          -> ExternalPikafishEngine.copyFile
                          -> ProcessBuilder pb = new ProcessBuilder(exePath);
```