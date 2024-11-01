# engine相关的实现

跟pikafish engine的交互，借鉴了droidfish的实现。主要由下面几个文件组成：

```
│       │   │   └── org
│       │   │       └── petero
│       │   │           └── droidfish
│       │   │               ├── engine
│       │   │               │   ├── EngineOptions.java
│       │   │               │   ├── EngineUtil.java
│       │   │               │   ├── ExternalEngine.java
│       │   │               │   ├── ExternalPikafishEngine.java
│       │   │               │   ├── UCIEngine.java
│       │   │               │   ├── UCIEngineBase.java
│       │   │               │   └── UCIOptions.java
```

engine的调用如下代码：

```
        engineName = "Computer";
        uciEngine = UCIEngineBase.getEngine(searchRequest.engineName,
                engineOptions,
                errMsg -> {
                    if (errMsg == null)
                        errMsg = "";
                    engineListener.reportEngineError(errMsg);
                });
        uciEngine.initialize();

        uciEngine.writeLineToEngine("uci");

```

随后可以通过

uciEngine.setOption以及uciEngine.applyOption来改变设置。


对其中的文件做个简单的介绍。

## UCIOptions

### UCI引擎option的封装类

里面定义了option的5种类型：

* CheckOption
* SpinOption
* ComboOption
* ButtonOption
* StringOption

他们都是OptionBase的子类。

OptionBase的方法 setFromString，可以根据类型，给子类设置值：

	public final boolean setFromString(String value)


### pikafish的所有的option

```
pikafish目前的uci命令的输出
id author the Pikafish developers (see AUTHORS file)
option name Debug Log File type string default
option name NumaPolicy type string default auto
option name Threads type spin default 1 min 1 max 1024
option name Hash type spin default 16 min 1 max 33554432
option name Clear Hash type button
option name Ponder type check default false
option name MultiPV type spin default 1 min 1 max 128
option name Move Overhead type spin default 10 min 0 max 5000
option name nodestime type spin default 0 min 0 max 10000
option name Skill Level type spin default 20 min 0 max 20
option name Mate Threat Depth type spin default 10 min 0 max 10
option name Repetition Rule type combo default AsianRule var AsianRule var ChineseRule var SkyRule var ComputerRule var AllowChase var YitianRule
option name Draw Rule type combo default None var None var DrawAsBlackWin var DrawAsRedWin var DrawRepAsBlackWin var DrawRepAsRedWin
option name Rule60MaxPly type spin default 120 min 0 max 120
option name UCI_LimitStrength type check default false
option name UCI_Elo type spin default 1280 min 1280 max 3133
option name ScoreType type combo default Elo var Elo var PawnValueNormalized var Raw
option name LU_Output type check default true
option name EvalFile type string default pikafish.nnue
uciok
```


### 所有option的添加，和缺省值的设置

当解析到engine的返回值后，通过UCIEngine的方法

	UCIOptions.OptionBase registerOption(String[] tokens) 
	
来注册engine的所有option。这个方法会调用UCIOptions的下面的方法：

```
解析uci引擎的返回值，注册某个option的缺省选项
final void addOption(OptionBase p)
```

当后期需要修改值的时候：

```
判断设置里是否包含这个key, 如果包含的话，才允许设置value
public boolean contains(String optName)

然后获取option项
public final OptionBase getOption(String name)

然后通过OptionBase的方法
public final boolean setFromString(String value)
来更新option的值

继而调用OptionBase子类的
public boolean set(String value)
方法。这个方法用来在后期修改option的值。如果新的值和原来的不一致，则返回true. 否则返回false.
这个返回值会用来判断是否需要向engine发送 setoption的命令。
```


## UCIEngine

interface. 

主要定义了三类方法：

### 1.engine的启停

```

    void initialize();
    void initOptions(EngineOptions engineOptions);
    boolean optionsOk(EngineOptions engineOptions);
    void shutDown();
```

### 2.ini文件的读写，以及UCIOption的设置

```
    void applyIniFile();
    void saveIniFile(UCIOptions options);
    boolean setOption(String name, String value);
    boolean setUCIOptions(Map<String, String> uciOptions);
    UCIOptions.OptionBase registerOption(String[] tokens);
    
```

设计思路是：

*   在engine启动的时候，通过给engine发送uci命令，得到engine支持的命令的所有option的集合，然后去解析这个结果，通过方法

```
UCIOptions.OptionBase registerOption(String[] tokens) 
```

把所有的可能option都创建出来。

*  这里有一个问题，pikafish engine在返回的时候，并不包含uci_showwdl这个option, 所以在的ExternalPikafishEngine构造函数里，显式注册了这个token

```
        // uci_showwdl is not included in the uci response, so we need to add it manually
        registerOption("option name uci_showwdl type check default false".split("\\s+"));
```

* 之后通过setOption来改变UCIOptions的设置
* applyIniFile() / saveIniFile 把操作配置文件里的option


### 3.跟engine的交互

```
    String readLineFromEngine(int timeoutMillis);
    void writeLineToEngine(String data);
```

## UCIEngineBase

通过下面的方法来获取engine的实例


```
public static UCIEngine getEngine(String engine,
                                      EngineOptions engineOptions, Report report)
```

实现了engine的启停

```

public final void initialize()
public void shutDown() {
```

实现了option的初始化，option配置文件的读写，及option的修改

```
public final UCIOptions.OptionBase registerOption(String[] tokens)

public final void applyIniFile()
public final void saveIniFile(UCIOptions options)

protected boolean editableOption(String name)
public boolean setOption(String name, String value)

```

setOption 会根据返回值，决定是否向engine发送setoption命令。


定义了两个需要子类实现的方法

```
protected abstract void startProcess();
protected abstract File getIniFile();
```



## ExternalEngine

这个类主要实现engine文件的复制，以及engine的启停的函数

```
protected String copyFile(File from, File exeDir)
protected void startProcess()
public void shutDown()

```

跟engine的具体交互

```
public String readLineFromEngine(int timeoutMillis)
public void writeLineToEngine(String data)
```

## ExternalPikafishEngine


pikafish是externalengine的一个子类，主要实现了跟pikafish引擎文件相关的几个方法

因为pikafish内置在程序的assets里，无法直接读取文件的状态，所以采用checksum来判断是否需要更新。并且使用copyAssetFile的方法来复制文件到工作目录。


```
protected String copyFile(File from, File exeDir) 
private void copyNetworkFiles(File pikaDir)
private void copyAssetFile(String assetName, File targetFile) 

protected String getCheckSumFile(String filename)
private long computeAssetsCheckSum(String sfExe)
private long readCheckSum(File f)

```


pikafish的evalfile option的value, 需要文件的绝对地址，所以这里对setOption做了重载，获取文件的绝对路径

```
public boolean setOption(String name, String value)

"setOption: evalfile /data/user/0/com.zfdang.chess/files/pikafish/pikafish.nnue"

```


## EngineOptions

这个类目前没有任何作用。保留在这里，单纯是因为删掉它要改动的地方太多了。



## EngineUtil

通过JNI的方式，实现了几个功能，包括chmod, reNice等

cpp目录下的几个文件，需要ndk编译
