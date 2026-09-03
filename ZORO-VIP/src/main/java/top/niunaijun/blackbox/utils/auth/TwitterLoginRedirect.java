package top.niunaijun.blackbox.utils.auth;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.RuntimeLogger;

/**
 * Mirrors the BGMI Lua "core_patch" behaviour at the BlackBox framework level:
 *  - block the Twitter WebView (system web login) the same way Lua did
 *    (DeeplinkLogin.LoginViaSystemWebview -> false)
 *  - force app-based Twitter login the same way Lua did
 *    (IMSDK_TWITTER_LOGIN_USING_WEB = "false")
 *
 * NOTE: modern Twitter/X app no longer ships the legacy app-OAuth activity,
 * so forcing app login will most likely fail to complete SSO. This is a
 * faithful port of the Lua logic, kept behind a runtime toggle so it can be
 * switched off without rebuilding.
 */
public final class TwitterLoginRedirect {

    private static final String TAG = "TwitterLoginRedirect";
    private static final String LOG_PATH = "/sdcard/twitter_block.log";
    private static final String TWITTER_APP_PKG = "com.twitter.android";
    private static final String MODE_FILE = "/sdcard/twitter_mode.txt";

    private TwitterLoginRedirect() {
    }

    /**
     * Default ON (option 2: block webview + force Twitter app login), matching
     * the Lua core_patch behaviour. To fall back to the WebView auto-login
     * instead, create /sdcard/twitter_mode.txt containing "webview".
     */
    private static boolean appModeEnabled() {
        try {
            File f = new File(MODE_FILE);
            if (!f.exists()) return true;
            java.util.Scanner s = new java.util.Scanner(f).useDelimiter("\\A");
            String v = s.hasNext() ? s.next().trim().toLowerCase(Locale.ROOT) : "";
            s.close();
            return !v.equals("webview");
        } catch (Throwable t) {
            return true;
        }
    }

    public static boolean shouldIntercept(Intent intent) {
        if (!appModeEnabled()) return false;
        if (intent == null) return false;
        try {
            Uri data = intent.getData();
            if (data != null) {
                String s = data.toString().toLowerCase(Locale.ROOT);
                if ((s.contains("twitter.com") || s.contains("api.twitter.com"))
                        && (s.contains("oauth") || s.contains("authorize"))) {
                    log("shouldIntercept: twitter web oauth URL -> " + s);
                    return true;
                }
            }
            ComponentName cn = intent.getComponent();
            if (cn != null) {
                String cls = cn.getClassName().toLowerCase(Locale.ROOT);
                if (cls.contains("oauth")
                        || (cls.contains("twitter")
                        && (cls.contains("webview") || cls.contains("auth") || cls.contains("login")))) {
                    log("shouldIntercept: twitter oauth component -> " + cls);
                    return true;
                }
            }
            if (Intent.ACTION_VIEW.equals(intent.getAction()) && data != null) {
                String host = data.getHost() != null ? data.getHost().toLowerCase(Locale.ROOT) : "";
                if (host.contains("twitter.com") && data.toString().toLowerCase(Locale.ROOT).contains("oauth")) {
                    log("shouldIntercept: twitter VIEW oauth -> " + data);
                    return true;
                }
            }
        } catch (Throwable t) {
            log("shouldIntercept error: " + t);
        }
        return false;
    }

    /**
     * Block the WebView login and attempt to push the user to the Twitter app
     * (app-based SSO), exactly like the Lua patch forced
     * IMSDK_TWITTER_LOGIN_USING_WEB=false.
     */
    public static void intercept(Intent original) {
        log("BLOCKED twitter web login (webview) -- Lua: LoginViaSystemWebview=false");
        try {
            Intent appIntent = new Intent(Intent.ACTION_VIEW);
            if (original != null && original.getData() != null) {
                appIntent.setData(original.getData());
            }
            appIntent.setPackage(TWITTER_APP_PKG);
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            BlackBoxCore.getContext().startActivity(appIntent);
            log("redirected to Twitter app (" + TWITTER_APP_PKG + ")");
        } catch (Throwable t) {
            log("failed to redirect to Twitter app: " + t);
        }
    }

    private static void log(String msg) {
        RuntimeLogger.log("TWITTER_REDIRECT", msg);
        try {
            File f = new File(LOG_PATH);
            FileWriter w = new FileWriter(f, true);
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date());
            w.write("[" + ts + "] " + msg + "\n");
            w.close();
        } catch (Throwable ignored) {
        }
        Log.d(TAG, msg);
    }
}
