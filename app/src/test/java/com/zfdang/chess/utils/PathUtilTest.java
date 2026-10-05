package com.zfdang.chess.utils;

import android.content.Context;
import android.content.ContextWrapper;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class PathUtilTest {
    @Test public void externalFilesOverloadsDelegateToContextWithRequestedType() {
        AtomicReference<String> requestedType = new AtomicReference<>();
        File directory = new File("/storage/emulated/0/Android/data/test/files");
        Context context = new ContextWrapper(null) {
            @Override public File getExternalFilesDir(String type) {
                requestedType.set(type);
                return directory;
            }
        };
        assertEquals(directory.getAbsolutePath(), PathUtil.getExternalStorageAppFileDir(context, "XQF"));
        assertEquals("XQF", requestedType.get());
        assertEquals(directory.getAbsolutePath(), PathUtil.getExternalStorageAppFileDir(context));
        assertNull(requestedType.get());
    }

    @Test public void unavailableExternalStorageHasAnExplicitFailure() {
        Context context = new ContextWrapper(null) {
            @Override public File getExternalFilesDir(String type) { return null; }
        };
        assertThrows(IllegalStateException.class, () -> PathUtil.getExternalStorageAppFileDir(context, "XQF"));
        assertThrows(IllegalStateException.class, () -> PathUtil.getExternalStorageAppFileDir(context));
    }
}
