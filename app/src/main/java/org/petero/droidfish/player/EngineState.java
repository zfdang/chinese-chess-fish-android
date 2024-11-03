package org.petero.droidfish.player;

import android.util.Log;

/**
 * Engine state details.
 */
public class EngineState {

    // engineName不需要主动设置
    // 会在queueStartEngine中的SearchRequest里传递过来，然后在readUCIOption读取到
    String engineName;

    /**
     * Current engine state.
     */
    EngineStateValue state;

    /**
     * ID of current search job.
     */
    int searchId;

    /**
     * Default constructor.
     */
    EngineState() {
        engineName = "";
        setState(EngineStateValue.DEAD);
        searchId = -1;
    }

    final void setState(EngineStateValue s) {
        Log.d("EngineState", "state: " + state + " -> " + s);
        state = s;
    }
}
