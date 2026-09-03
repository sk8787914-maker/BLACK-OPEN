package top.niunaijun.blackbox.utils.auth;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class TwitterWebViewAutoLogin {
    private static final String TAG = "TwitterWebViewAutoLogin";
    private static final Set<WebView> handled = new HashSet<>();

    public static void startIfNeeded(Activity activity) {
        String name = activity.getClass().getName().toLowerCase();
        if (name.contains("twitter") || name.contains("oauth")) {
            Log.d(TAG, "Twitter activity detected: " + activity.getClass().getName());
            TwitterCreds.load();
            new Handler(Looper.getMainLooper()).postDelayed(() -> scan(new WeakReference<>(activity)), 800);
        }
    }

    private static void scan(WeakReference<Activity> ref) {
        Activity activity = ref.get();
        if (activity == null) return;
        Set<WebView> views = new HashSet<>();
        try {
            View root = activity.getWindow().getDecorView();
            findWebViews(root, views);
        } catch (Throwable t) {
            Log.e(TAG, "scan", t);
        }
        for (WebView wv : views) {
            if (!handled.contains(wv)) {
                handled.add(wv);
                schedule(wv);
            }
        }
        if (views.isEmpty()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> scan(ref), 1000);
        }
    }

    private static void findWebViews(View v, Set<WebView> out) {
        if (v == null) return;
        String cn = v.getClass().getName();
        if (v instanceof WebView || cn.contains("WebView")) {
            try {
                out.add((WebView) v);
            } catch (Throwable ignored) {
            }
            return;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                findWebViews(g.getChildAt(i), out);
            }
        }
    }

    private static void schedule(WebView wv) {
        final int[] ticks = {0};
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (ticks[0]++ > 60) return;
                inject(wv);
                wv.postDelayed(this, 1200);
            }
        };
        wv.postDelayed(r, 600);
    }

    private static void inject(WebView wv) {
        try {
            wv.evaluateJavascript(js(), null);
        } catch (Throwable t) {
            Log.e(TAG, "inject", t);
        }
    }

    private static String js() {
        String u = TwitterCreds.USER.replace("'", "\\'");
        String p = TwitterCreds.PASS.replace("'", "\\'");
        return "(function(){"
                + "function setVal(sel,val){var el=document.querySelector(sel);if(!el)return false;"
                + "el.value=val;el.dispatchEvent(new Event('input',{bubbles:true}));"
                + "el.dispatchEvent(new Event('change',{bubbles:true}));return true;}"
                + "function clickText(t){var bs=document.querySelectorAll('button,div[role=button],a');"
                + "for(var i=0;i<bs.length;i++){if(bs[i].textContent&&bs[i].textContent.toLowerCase().indexOf(t)>=0){bs[i].click();return true;}}return false;}"
                + "var u='" + u + "';var p='" + p + "';"
                + "if(document.querySelector('input[name=text]')){setVal('input[name=text]',u);clickText('next');clickText('log in');}"
                + "else if(document.querySelector('input[name=pass]')){setVal('input[name=pass]',p);clickText('log in');}"
                + "else{clickText('authorize');clickText('allow');clickText('sign in');}"
                + "})();";
    }
}
