# 非GUI线程无法修改界面

## 问题

如果从非UI线程去访问UI上的控件，会出现下面的问题：

```
android.view.ViewRoot$CalledFromWrongThreadException: 
Only the original thread that created a view hierarchy can touch its views.
```


## 解决方法


把要运行的代码放到UI线程去执行

```
runOnUiThread(new Runnable() {

    @Override
    public void run() {

        // Stuff that updates the UI

    }
});
```

对应的android文档：

[runOnUiThread](https://developer.android.com/reference/android/app/Activity.html#runOnUiThread(java.lang.Runnable))

## 代码示例


### 定义接口

	https://github.com/zfdang/chinese-chess-android/blob/master/app/src/main/java/com/zfdang/chess/controllers/GameControllerListener.java

```
public interface GameControllerListener {

    // create fun to send game over event
    public void onGameEvent(GameStatus event, String message);

    public void onGameEvent(GameStatus event);

    /** Run code on the GUI thread. */
    // https://stackoverflow.com/questions/5161951/android-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-vi
    // Android "Only the original thread that created a view hierarchy can touch its views."
    void runOnUIThread(Runnable runnable);
}
```

### 实现接口

	https://github.com/zfdang/chinese-chess-android/blob/master/app/src/main/java/com/zfdang/chess/GameActivity.kt

```
    override fun runOnUIThread(runnable: Runnable?) {
            runOnUiThread(runnable);
        }
```

	
	
### 运行代码在Ui线程

	https://github.com/zfdang/chinese-chess-android/blob/master/app/src/main/java/com/zfdang/chess/controllers/GameController.java
	
```
private GameControllerListener gui = null;
public GameController(GameControllerListener cListener) {
    gui = cListener;
}

gui.runOnUIThread(() -> playerMovePiece(bestMove));

```
	

