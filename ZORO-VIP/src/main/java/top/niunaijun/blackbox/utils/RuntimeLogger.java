package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Best-effort persistent SDK diagnostics that survive a process crash. */
public final class RuntimeLogger {
    private static final String TAG = "BlackOpenRuntime";
    private static final long MAX_BYTES = 2L * 1024L * 1024L;
    private static final Object LOCK = new Object();
    private static final SimpleDateFormat FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US);
    private static volatile Context context;

    private RuntimeLogger() {
    }

    public static void initialize(Context baseContext) {
        if (baseContext != null) {
            context = baseContext.getApplicationContext() != null
                    ? baseContext.getApplicationContext() : baseContext;
        }
        log("LOGGER", "initialized; path=" + getLogFile().getAbsolutePath());
    }

    public static String getLogFilePath() {
        return getLogFile().getAbsolutePath();
    }

    public static void log(String event, String message) {
        String line = timestamp() + " pid=" + Process.myPid()
                + " tid=" + Thread.currentThread().getId()
                + " [" + safe(event) + "] " + safe(message);
        Log.d(TAG, line);
        synchronized (LOCK) {
            File file = getLogFile();
            try {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) {
                    return;
                }
                rotateIfNeeded(file);
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
            } catch (Throwable writeFailure) {
                Log.w(TAG, "Could not write " + file, writeFailure);
            }
        }
    }

    public static void logException(String event, Throwable throwable) {
        log(event, throwable == null ? "null throwable" : Log.getStackTraceString(throwable));
    }

    private static String timestamp() {
        synchronized (FORMAT) {
            return FORMAT.format(new Date());
        }
    }

    private static String safe(String value) {
        return value == null ? "null" : value;
    }

    private static void rotateIfNeeded(File file) {
        if (!file.exists() || file.length() <= MAX_BYTES) {
            return;
        }
        File old = new File(file.getParentFile(), "log.txt.1");
        if (old.exists()) {
            //noinspection ResultOfMethodCallIgnored
            old.delete();
        }
        //noinspection ResultOfMethodCallIgnored
        file.renameTo(old);
    }

    private static File getLogFile() {
        Context current = context;
        if (current != null) {
            File external = current.getExternalFilesDir(null);
            if (external != null) {
                return new File(external, "log.txt");
            }
        }
        File publicDirectory = new File("/sdcard/BLACK-OPEN");
        if (publicDirectory.exists() || publicDirectory.mkdirs()) {
            return new File(publicDirectory, "log.txt");
        }
        return new File("/data/local/tmp/black-open-log.txt");
    }
}
