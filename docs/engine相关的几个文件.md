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

对其中的文件做个简单的介绍

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
    boolean setUCIOptions(Map<String, String> uciOptions);
    void setOption(String name, int value);
    void setOption(String name, boolean value);
    boolean setOption(String name, String value);
    void setOptionClear(String name);
    void clearAllOptions();
    boolean applyUCIOptions();
    UCIOptions.OptionBase registerOption(String[] tokens);
    UCIOptions getUCIOptions();
```

### 跟engine的交互
```
    String readLineFromEngine(int timeoutMillis);
    void writeLineToEngine(String data);
```

## UCIEngineBase

## ExternalEngine

## ExternalPikafishEngine

## EngineOptions

这个类目前没有任何作用。保留在这里，单纯是因为删掉它要改动的地方太多了。

## UCIOptions

## EngineUtil
