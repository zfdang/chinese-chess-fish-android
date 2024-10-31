package org.petero.droidfish.player;

import android.util.Log;

/**
 * Engine state details.
 */
public class EngineState {
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
