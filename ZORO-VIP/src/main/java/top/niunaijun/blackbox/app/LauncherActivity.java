package top.niunaijun.blackbox.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.webkit.WebView;
import androidx.annotation.Nullable;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.R;
import top.niunaijun.blackbox.utils.Slog;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.OvershootInterpolator;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class LauncherActivity extends Activity {
    
    public static final String TAG = "SplashScreen";
    public static final String KEY_INTENT = "launch_intent";
    public static final String KEY_PKG = "launch_pkg";
    public static final String KEY_USER_ID = "launch_user_id";
    private boolean isRunning = false;

    public static void launch(Intent intent, int userId) {
        Intent splash = new Intent();
        splash.setClass(BlackBoxCore.getContext(), LauncherActivity.class);
        splash.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        splash.putExtra(KEY_INTENT, intent);
        splash.putExtra(KEY_PKG, intent.getPackage());
        splash.putExtra(KEY_USER_ID, userId);
        BlackBoxCore.getContext().startActivity(splash);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        Intent launchIntent = intent.getParcelableExtra(KEY_INTENT);
        String packageName = intent.getStringExtra(KEY_PKG);
        int userId = intent.getIntExtra(KEY_USER_ID, 0);

        if (launchIntent == null) {
            Slog.e(TAG, "launchIntent is null! Cannot launch app.");
            finish();
            return;
        }

        if (packageName == null) {
            packageName = launchIntent.getPackage();
            if (packageName == null) {
                Slog.e(TAG, "Package name is null! Cannot launch app.");
                finish();
                return;
            }
        }

        PackageInfo packageInfo = BlackBoxCore.getBPackageManager().getPackageInfo(packageName, 0, userId);
        if (packageInfo == null) {
            Slog.e(TAG, packageName + " not installed!");
            finish();
            return;
        }

        setContentView(R.layout.activity_launcher);

        // ===== Premium Loading WebView =====
        WebView web = findViewById(R.id.web_loading);
        web.getSettings().setJavaScriptEnabled(true);
        web.setBackgroundColor(Color.TRANSPARENT);
        String html = getPremiumLoadingHtml();
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);

        // ===== App Icon =====
        Drawable icon = packageInfo.applicationInfo.loadIcon(BlackBoxCore.getPackageManager());
        ImageView iconView = findViewById(R.id.iv_icon);
        iconView.setImageDrawable(icon);

        // --- Gold border, no cropping ---
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int borderWidth = (int) (2 * getResources().getDisplayMetrics().density + 0.5f);
        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setStroke(borderWidth, Color.parseColor("#ffcc00"));
        borderDrawable.setColor(Color.TRANSPARENT);
        int padding = borderWidth * 2;
        iconView.setPadding(padding, padding, padding, padding);
        iconView.setBackground(borderDrawable);

        // ===== App Name =====
        TextView nameView = findViewById(R.id.tv_app_name);
        if (nameView != null) {
            CharSequence label = packageInfo.applicationInfo.loadLabel(BlackBoxCore.getPackageManager());
            nameView.setText(label);
            nameView.setAlpha(0f);
            nameView.animate().alpha(1f).setDuration(400).start();
        }

        // ===== Icon Animation =====
        iconView.setScaleX(0.7f);
        iconView.setScaleY(0.7f);
        iconView.setAlpha(0f);
        iconView.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(450)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // ===== Launch App =====
        new Thread(() -> BlackBoxCore.getBActivityManager().startActivity(launchIntent, userId)).start();
    }

    private String getPremiumLoadingHtml() {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta charset='UTF-8'>" +
               "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
               "<link href='https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Share+Tech+Mono&family=Rajdhani:wght@400;600;700&display=swap' rel='stylesheet'>" +
               "<style>" +
               "*{margin:0;padding:0;box-sizing:border-box;}" +
               ":root{" +
               "  --red: #ff2244;" +
               "  --red-dim: rgba(255,34,68,0.18);" +
               "  --red-glow: rgba(255,34,68,0.55);" +
               "  --gold: #ffcc00;" +
               "  --gold-dim: rgba(255,204,0,0.12);" +
               "  --cyan: #00e5ff;" +
               "  --cyan-dim: rgba(0,229,255,0.10);" +
               "  --bg: #07070d;" +
               "  --border: rgba(255,34,68,0.25);" +
               "  --text: #dde0f0;" +
               "  --muted: rgba(200,205,230,0.4);" +
               "  --mono: 'Share Tech Mono', monospace;" +
               "  --display: 'Orbitron', sans-serif;" +
               "  --ui: 'Rajdhani', sans-serif;" +
               "}" +
               "body{" +
               "  margin:0;padding:0;" +
               "  background:transparent;" +
               "  display:flex;" +
               "  justify-content:center;" +
               "  align-items:center;" +
               "  height:100vh;" +
               "  font-family:var(--ui);" +
               "  overflow:hidden;" +
               "}" +
               ".container{" +
               "  width:85%;" +
               "  max-width:320px;" +
               "  text-align:center;" +
               "  position:relative;" +
               "  z-index:2;" +
               "}" +
               ".glitch-border{" +
               "  position:relative;" +
               "  padding:4px;" +
               "  background:linear-gradient(135deg, var(--red), var(--gold), var(--red));" +
               "  border-radius:12px;" +
               "  box-shadow:0 0 30px var(--red-glow), inset 0 0 20px rgba(255,34,68,0.2);" +
               "}" +
               ".inner{" +
               "  background:rgba(7,7,13,0.85);" +
               "  border-radius:10px;" +
               "  padding:24px 16px 20px;" +
               "  backdrop-filter:blur(4px);" +
               "}" +
               ".title{" +
               "  font-family:var(--display);" +
               "  font-size:16px;" +
               "  font-weight:700;" +
               "  color:var(--gold);" +
               "  text-shadow:0 0 20px rgba(255,204,0,0.6);" +
               "  letter-spacing:3px;" +
               "  text-transform:uppercase;" +
               "  margin-bottom:16px;" +
               "}" +
               ".title small{" +
               "  display:block;" +
               "  font-family:var(--mono);" +
               "  font-size:10px;" +
               "  color:var(--muted);" +
               "  letter-spacing:2px;" +
               "  margin-top:2px;" +
               "}" +
               ".progress-wrap{" +
               "  position:relative;" +
               "  width:100%;" +
               "  height:6px;" +
               "  background:rgba(255,255,255,0.06);" +
               "  border-radius:10px;" +
               "  overflow:hidden;" +
               "  border:1px solid rgba(255,204,0,0.15);" +
               "  box-shadow:0 0 30px rgba(255,204,0,0.1);" +
               "}" +
               ".progress-fill{" +
               "  width:0%;" +
               "  height:100%;" +
               "  background:linear-gradient(90deg, #ffcc00, #ff8800, #ffcc00);" +
               "  background-size:200% 100%;" +
               "  animation:shimmer 1.5s infinite ease-in-out;" +
               "  border-radius:10px;" +
               "  transition:width 0.1s linear;" +
               "  box-shadow:0 0 25px #ffcc00;" +
               "}" +
               "@keyframes shimmer{" +
               "  0%{background-position:-200% 0;}" +
               "  100%{background-position:200% 0;}" +
               "}" +
               ".percentage{" +
               "  font-family:var(--display);" +
               "  font-size:32px;" +
               "  font-weight:900;" +
               "  color:#fff;" +
               "  text-shadow:0 0 30px rgba(255,204,0,0.7);" +
               "  margin:12px 0 6px;" +
               "  letter-spacing:2px;" +
               "}" +
               ".subtitle{" +
               "  font-family:var(--mono);" +
               "  font-size:11px;" +
               "  color:var(--muted);" +
               "  letter-spacing:2px;" +
               "  text-transform:uppercase;" +
               "}" +
               ".corner{" +
               "  position:absolute;" +
               "  width:12px;height:12px;" +
               "  z-index:10;" +
               "}" +
               ".corner-tl{top:8px;left:8px;border-top:2px solid var(--red);border-left:2px solid var(--red);}" +
               ".corner-tr{top:8px;right:8px;border-top:2px solid var(--red);border-right:2px solid var(--red);}" +
               ".corner-bl{bottom:8px;left:8px;border-bottom:2px solid var(--red);border-left:2px solid var(--red);}" +
               ".corner-br{bottom:8px;right:8px;border-bottom:2px solid var(--red);border-right:2px solid var(--red);}" +
               "</style>" +
               "</head>" +
               "<body>" +
               "<div class='container'>" +
               "  <div class='glitch-border'>" +
               "    <div class='inner'>" +
               "      <div class='title'>" +
               "        ✦ ZCORE SDK LAUNCHER ✦" +
               "        <small>WINNER ONLY ONES</small>" +
               "      </div>" +
               "      <div class='progress-wrap'>" +
               "        <div class='progress-fill' id='progressFill'></div>" +
               "      </div>" +
               "      <div class='percentage' id='percent'>0%</div>" +
               "      <div class='subtitle' id='status'>initializing secure session...</div>" +
               "      <div class='corner corner-tl'></div>" +
               "      <div class='corner corner-tr'></div>" +
               "      <div class='corner corner-bl'></div>" +
               "      <div class='corner corner-br'></div>" +
               "    </div>" +
               "  </div>" +
               "</div>" +
               "<script>" +
               "var fill = document.getElementById('progressFill');" +
               "var percent = document.getElementById('percent');" +
               "var status = document.getElementById('status');" +
               "var width = 0;" +
               "var target = 100;" +
               "var duration = 3000;" +
               "var interval = 30;" +
               "var step = 100 / (duration / interval);" +
               "var timer = setInterval(function() {" +
               "  width += step;" +
               "  if (width > target) width = target;" +
               "  fill.style.width = width + '%';" +
               "  percent.textContent = Math.floor(width) + '%';" +
               "  if (width >= target) {" +
               "    clearInterval(timer);" +
               "    status.textContent = '✓ ready to launch';" +
               "    status.style.color = '#33ff66';" +
               "  }" +
               "}, interval);" +
               "</script>" +
               "</body>" +
               "</html>";
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRunning) {
            finish();
        }
    }
}