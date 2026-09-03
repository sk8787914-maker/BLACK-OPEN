package top.niunaijun.blackbox.utils.auth;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public final class TwitterCreds {
    private static final String TAG = "TwitterCreds";

    public static String USER = "";
    public static String PASS = "";

    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        loaded = true;
        File f = new File("/data/local/tmp/twitter_creds.txt");
        if (!f.exists()) f = new File("/sdcard/twitter_creds.txt");
        if (f.exists()) {
            try {
                BufferedReader r = new BufferedReader(new FileReader(f));
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.startsWith("user=")) USER = line.substring(5).trim();
                    else if (line.startsWith("pass=")) PASS = line.substring(5).trim();
                }
                r.close();
            } catch (Throwable t) {
                Log.e(TAG, "load creds", t);
            }
        }
    }
}
