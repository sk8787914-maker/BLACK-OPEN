package top.niunaijun.blackbox.core;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.RuntimeLogger;

/**
 * Created by Milk on 4/30/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public static void create() {
        new CrashHandler();
    }

    public CrashHandler() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        RuntimeLogger.log("CRASH_HANDLER", "installed");
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        RuntimeLogger.logException("UNCAUGHT_EXCEPTION thread=" + (t == null ? "null" : t.getName()), e);
        try {
            if (BlackBoxCore.get().getExceptionHandler() != null) {
                BlackBoxCore.get().getExceptionHandler().uncaughtException(t, e);
            }
        } catch (Throwable handlerFailure) {
            RuntimeLogger.logException("CUSTOM_EXCEPTION_HANDLER_FAILED", handlerFailure);
        }
        if (mDefaultHandler != null && mDefaultHandler != this) {
            mDefaultHandler.uncaughtException(t, e);
        }
    }
}
