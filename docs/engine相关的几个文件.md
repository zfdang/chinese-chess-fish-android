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

        uciEngine.clearAllOptions();
        uciEngine.loadIniFile();
        uciEngine.applyAllOptions();

```

随后可以通过

uciEngine.setOption以及uciEngine.applyOption来改变设置。


对其中的文件做个简单的介绍。

## UCIEngine

interface. 

主要定义了三类方法：

### engine的启停

```

    void initialize();
    void initOptions(EngineOptions engineOptions);
    boolean optionsOk(EngineOptions engineOptions);
    void shutDown();
```

### ini文件的读写，以及UCIOption的设置

```
    void loadIniFile();
    void saveIniFile();
    boolean setOption(String name, String value);
    void setOptionClear(String name);
    void clearAllOptions();
    boolean applyAllOptions();
    boolean applyOption(String name, String value);
	UCIOptions getUCIOptions();
```

设计思路是：

1. loadIniFile() / saveIniFile把配置文件里的内容读取出来
1. 通过setOption或者setOptionClear来改变UCIOptions的设置
1. 然后通过applyAllOptions或者applyOption的方式，应用到引擎里去

setOption自己并不会讲设置发送给引擎

### 跟engine的交互
```
    String readLineFromEngine(int timeoutMillis);
    void writeLineToEngine(String data);
```

## UCIEngineBase

定义了两个需要子类实现的方法

```
protected abstract void startProcess();
protected abstract File getIniFile();
```

实现的第一组方法是engine的启停

```
public final void initialize()
public void shutDown() {
```

第二组方法是配置文件的读写和apply

```
public final void loadIniFile()
public final void saveIniFile()
public boolean setOption(String name, String value)
public boolean applyOption(String name, String value)
public boolean applyAllOptions()
```

## ExternalEngine

这个类主要实现engine文件的复制，已经engine的启停的函数

```
shutDown
copyFile
```


以及跟engine的具体交互

```
readLineFromEngine
writeLineToEngine
```

## ExternalPikafishEngine

这个实现了跟pikafish引擎相关的几个方法，主要包括

```
copyFile
copyNetworkFiles
copyAssetFile
```

已经重载了 applyOption, 将network文件改成了绝对路径。

## EngineOptions

这个类目前没有任何作用。保留在这里，单纯是因为删掉它要改动的地方太多了。

## UCIOptions

UCI引擎设置的封装类

## EngineUtil

通过JNI的方式，实现了几个功能，包括chmod, reNice等
