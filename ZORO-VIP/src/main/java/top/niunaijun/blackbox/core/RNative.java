package top.niunaijun.blackbox.core;

import android.os.Binder;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import androidx.annotation.Keep;
import android.content.Context;
import java.io.File;
import java.util.List;
import dalvik.system.DexFile;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.compat.DexFileCompat;

public class RNative {
    
    public static final String TAG = "RNative";
    private static boolean isInjected = false;
    
    // మీ మూడు లైబ్రరీల పేర్లు
    public static String libtarget1 = "libbgmi.so";
    public static String libtarget2 = "libtwttier.so";   // Bypass & AllFix
    public static String libtarget3 = "liballfix.so";    // SKIN LIB
    static {
        System.loadLibrary("RIYAZcore");
        
        // అన్ని లైబ్రరీలను లోడ్ చేయండి
        String[] targets = {libtarget1, libtarget2, libtarget3};
        for (String target : targets) {
            File file = new File(BlackBoxCore.getContext().getFilesDir(), "loader/" + target);
            if (file.exists()) {
                try {
                    System.load(file.getAbsolutePath());
                    Log.i(TAG, "Loaded: " + target);
                } catch (Throwable e) {
                    Log.e(TAG, "Failed to load: " + target, e);
                }
            } else {
                Log.w(TAG, "File not found: " + file.getAbsolutePath());
            }
        }
    }

    public static native void init(int apiLevel);
    public static native void enableIO();
    public static native void addIORule(String targetPath, String relocatePath);
    public static native void hideXposed();
    
    @Keep
    public static int getCallingUid(int origCallingUid) {
        if (origCallingUid > 0 && origCallingUid < Process.FIRST_APPLICATION_UID)
            return origCallingUid;
        if (origCallingUid > Process.LAST_APPLICATION_UID)
            return origCallingUid;
        if (origCallingUid == BlackBoxCore.getHostUid()) {
            if (BActivityThread.getAppPackageName().equals("com.google.android.gms")) {
                return Process.ROOT_UID;
            }
            if (BActivityThread.getAppPackageName().equals("com.google.android.webview")) {
                return Process.myUid();
            }
            return BActivityThread.getCallingBUid();
        }
        return origCallingUid;   // ✅ ఇది తప్పకుండా ఉండాలి
    }

    @Keep
    public static String redirectPath(String path) {
        return RCore.get().redirectPath(path);
    }

    @Keep
    public static File redirectPath(File path) {
        return RCore.get().redirectPath(path);
    }
}