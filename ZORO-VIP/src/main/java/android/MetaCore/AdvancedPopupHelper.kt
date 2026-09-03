package android.MetaCore

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import java.lang.reflect.Field
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AdvancedPopupHelper {

    private val handler = Handler(Looper.getMainLooper())

    /* ================= GET TOP ACTIVITY ================= */
    private fun getTopActivity(): Activity? {
        return try {
            val atClass = Class.forName("android.app.ActivityThread")
            val currentAT = atClass.getMethod("currentActivityThread").invoke(null)
            val mActivitiesField: Field = atClass.getDeclaredField("mActivities")
            mActivitiesField.isAccessible = true
            val activities = mActivitiesField.get(currentAT) as Map<*, *>

            for (record in activities.values) {
                val rClass = record!!::class.java
                val pausedField = rClass.getDeclaredField("paused")
                pausedField.isAccessible = true
                if (!pausedField.getBoolean(record)) {
                    val activityField = rClass.getDeclaredField("activity")
                    activityField.isAccessible = true
                    return activityField.get(record) as Activity
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    /* ================= ENTRY ================= */
    @JvmStatic
    fun showAuto() {
        val act = getTopActivity() ?: return
        if (act.isFinishing || act.isDestroyed) return
        showPopup(act)
    }

    /* ================= SHOW POPUP ================= */
    private fun showPopup(act: Activity) {
    handler.post {
        try {
            val dialog = Dialog(act, android.R.style.Theme_Translucent_NoTitleBar)
            dialog.setCancelable(false)

            val webView = WebView(act)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.setBackgroundColor(Color.TRANSPARENT)
            webView.webViewClient = WebViewClient()

            val deviceInfo = """
                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                Android: ${Build.VERSION.RELEASE}
                SDK: ${Build.VERSION.SDK_INT}
            """.trimIndent()

            webView.addJavascriptInterface(object {
                @JavascriptInterface
                fun close() {
                    handler.post {
                        if (dialog.isShowing) dialog.dismiss()
                    }
                }
                
                @JavascriptInterface
                fun getDeviceInfo(): String {
                    return deviceInfo
                }
                
                @JavascriptInterface
                fun getExpireStatus(): String {
                    return "@ZOROADMINSERVER Access Revoked"
                }
            }, "Android")

            webView.loadDataWithBaseURL(
                null,
                HTML,
                "text/html",
                "utf-8",
                null
            )

            dialog.setContentView(webView)
            dialog.show()

            dialog.window?.apply {
                // ↑↑↑ FIX: increased width to 300dp, height to 580dp ↑↑↑
                setLayout(dp(act, 300), dp(act, 580))
                setGravity(Gravity.CENTER)
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0.6f)
            }

        } catch (_: Throwable) {
        }
    }
}
    /* ================= DP UTILS ================= */
    private fun dp(act: Activity, v: Int): Int {
        return (v * act.resources.displayMetrics.density).toInt()
    }

    /* ================= HTML (CLEAN BLACK BACKGROUND) ================= */
    private val HTML = """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<script src="https://cdnjs.cloudflare.com/ajax/libs/bodymovin/5.12.2/lottie.min.js"></script>
<link href="https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700;900&family=Share+Tech+Mono&family=Rajdhani:wght@400;600;700&display=swap" rel="stylesheet">
<style>
*{margin:0;padding:0;box-sizing:border-box;}

:root{
  --red:       #ff2244;
  --red-dim:   rgba(255,34,68,0.18);
  --red-glow:  rgba(255,34,68,0.55);
  --gold:      #ffcc00;
  --gold-dim:  rgba(255,204,0,0.12);
  --cyan:      #00e5ff;
  --cyan-dim:  rgba(0,229,255,0.10);
  --bg:        #07070d;
  --surface:   #0e0e1a;
  --border:    rgba(255,34,68,0.25);
  --text:      #dde0f0;
  --muted:     rgba(200,205,230,0.4);
  --mono:      'Share Tech Mono', monospace;
  --display:   'Orbitron', sans-serif;
  --ui:        'Rajdhani', sans-serif;
}

html,body{
  width:100%; height:100%;
  background:var(--bg);
  display:flex;
  justify-content:center;
  align-items:center;
  overflow:hidden;
}

/* ═══════════════════════════════
   ANIMATED BG
═══════════════════════════════ */
.bg-grid{
  position:fixed; inset:0; z-index:0;
  background-image:
    linear-gradient(rgba(255,34,68,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,34,68,0.04) 1px, transparent 1px);
  background-size:28px 28px;
  animation:grid-drift 20s linear infinite;
}
@keyframes grid-drift{
  from{background-position:0 0;}
  to{background-position:28px 28px;}
}

.bg-vignette{
  position:fixed; inset:0; z-index:1;
  background:radial-gradient(ellipse at center, transparent 40%, #07070d 100%);
}

/* ═══════════════════════════════
   CARD SHELL
═══════════════════════════════ */
.card{
  position:relative; z-index:5;
  width:290px;
  background:linear-gradient(160deg, #0f0f1e 0%, #080810 100%);
  border-radius:12px;
  overflow:hidden;
  border:1px solid var(--border);
  box-shadow:
    0 0 0 1px rgba(255,34,68,0.08),
    0 0 40px var(--red-glow),
    0 30px 60px rgba(0,0,0,0.9),
    inset 0 1px 0 rgba(255,255,255,0.04);
  animation:card-in 0.5s cubic-bezier(0.16,1,0.3,1) both;
}

@keyframes card-in{
  from{transform:scale(0.88) translateY(20px);opacity:0;}
  to{transform:scale(1) translateY(0);opacity:1;}
}

/* scanlines */
.card::after{
  content:'';
  position:absolute; inset:0; z-index:20;
  background:repeating-linear-gradient(
    0deg,
    transparent, transparent 3px,
    rgba(0,0,0,0.08) 3px, rgba(0,0,0,0.08) 4px
  );
  pointer-events:none;
  border-radius:12px;
}

/* ═══════════════════════════════
   TOP HAZARD BAR
═══════════════════════════════ */
.hazard-bar{
  height:4px;
  background:repeating-linear-gradient(
    90deg,
    var(--red) 0px,    var(--red) 14px,
    transparent 14px,  transparent 20px
  );
  animation:hazard-scroll 0.8s linear infinite;
}
@keyframes hazard-scroll{
  from{background-position:0 0;}
  to{background-position:20px 0;}
}

/* ═══════════════════════════════
   CORNER ACCENTS
═══════════════════════════════ */
.corner{position:absolute;width:14px;height:14px;z-index:15;}
.corner-tl{top:8px;left:8px;border-top:2px solid var(--red);border-left:2px solid var(--red);}
.corner-tr{top:8px;right:8px;border-top:2px solid var(--red);border-right:2px solid var(--red);}
.corner-bl{bottom:8px;left:8px;border-bottom:2px solid var(--red);border-left:2px solid var(--red);}
.corner-br{bottom:8px;right:8px;border-bottom:2px solid var(--red);border-right:2px solid var(--red);}

/* ═══════════════════════════════
   INNER PADDING
═══════════════════════════════ */
.inner{padding:18px 16px 14px;}

/* ═══════════════════════════════
   HEADER
═══════════════════════════════ */
.header{
  display:flex;
  align-items:center;
  justify-content:space-between;
  margin-bottom:14px;
}

.brand-row{display:flex;align-items:center;gap:8px;}

.logo-icon{
  width:28px; height:28px;
  flex-shrink:0;
}

.brand-text{}
.brand-sub{
  font-family:var(--mono);
  font-size:7px;
  color:var(--muted);
  letter-spacing:2px;
  text-transform:uppercase;
}
.brand-name{
  font-family:var(--display);
  font-size:11px;
  font-weight:700;
  color:var(--gold);
  letter-spacing:1px;
  text-shadow:0 0 12px rgba(255,204,0,0.5);
  line-height:1.1;
}

.status-pill{
  display:flex;
  align-items:center;
  gap:5px;
  padding:4px 8px;
  background:var(--red-dim);
  border:1px solid rgba(255,34,68,0.4);
  border-radius:20px;
}
.pill-dot{
  width:5px; height:5px;
  background:var(--red);
  border-radius:50%;
  box-shadow:0 0 6px var(--red);
  animation:blink 1s ease-in-out infinite;
}
@keyframes blink{0%,100%{opacity:1;}50%{opacity:0.2;}}
.pill-text{
  font-family:var(--mono);
  font-size:7.5px;
  color:var(--red);
  letter-spacing:1.5px;
  text-transform:uppercase;
}

/* ═══════════════════════════════
   HERO — LOCK ICON AREA
═══════════════════════════════ */
.hero{
  position:relative;
  text-align:center;
  padding:10px 0 6px;
  margin-bottom:12px;
}

.hero-ring{
  position:relative;
  width:72px; height:72px;
  margin:0 auto 10px;
}

/* spinning dashed ring */
.hero-ring::before{
  content:'';
  position:absolute; inset:-6px;
  border-radius:50%;
  border:2px dashed rgba(255,34,68,0.35);
  animation:spin-slow 8s linear infinite;
}
.hero-ring::after{
  content:'';
  position:absolute; inset:-12px;
  border-radius:50%;
  border:1px solid rgba(255,34,68,0.12);
  animation:spin-slow 14s linear infinite reverse;
}
@keyframes spin-slow{from{transform:rotate(0deg);}to{transform:rotate(360deg);}}

.hero-lottie{
  width:72px; height:72px;
  position:relative; z-index:2;
}

.hero-glow{
  position:absolute;
  bottom:-10px; left:50%;
  transform:translateX(-50%);
  width:60px; height:20px;
  background:var(--red);
  border-radius:50%;
  filter:blur(14px);
  opacity:0.4;
}

.hero-title{
  font-family:var(--display);
  font-size:17px;
  font-weight:900;
  color:#fff;
  letter-spacing:2px;
  text-transform:uppercase;
  line-height:1;
  animation:glitch-title 5s steps(1) infinite;
}
@keyframes glitch-title{
  0%,93%,100%{text-shadow:0 0 20px rgba(255,34,68,0.6);}
  94%{text-shadow:-3px 0 #0ff,3px 0 #f0f,0 0 20px rgba(255,34,68,0.6);transform:translateX(-1px);}
  96%{text-shadow:3px 0 #0ff,-3px 0 #f0f,0 0 20px rgba(255,34,68,0.6);transform:translateX(1px);}
  98%{text-shadow:none;transform:translateX(0);}
}

.hero-sub{
  font-family:var(--mono);
  font-size:8.5px;
  color:rgba(255,34,68,0.7);
  letter-spacing:3px;
  text-transform:uppercase;
  margin-top:4px;
}

/* ═══════════════════════════════
   DIVIDER
═══════════════════════════════ */
.divider{
  display:flex;
  align-items:center;
  gap:8px;
  margin:10px 0;
}
.divider-line{flex:1;height:1px;background:linear-gradient(90deg,transparent,rgba(255,34,68,0.3),transparent);}
.divider-dot{
  width:4px; height:4px;
  background:var(--red);
  border-radius:50%;
  box-shadow:0 0 6px var(--red);
}

/* ═══════════════════════════════
   DEVICE BLOCK
═══════════════════════════════ */
.device-block{
  background:rgba(255,255,255,0.02);
  border:1px solid rgba(255,255,255,0.06);
  border-radius:8px;
  padding:9px 11px;
  margin-bottom:10px;
  position:relative;
  overflow:hidden;
}
.device-block::before{
  content:'DEVICE INTEL';
  position:absolute;
  top:6px; right:9px;
  font-family:var(--mono);
  font-size:6.5px;
  color:rgba(255,255,255,0.12);
  letter-spacing:2px;
}
.device-block::after{
  content:'';
  position:absolute;
  left:0; top:0; bottom:0;
  width:2px;
  background:linear-gradient(to bottom, transparent, var(--cyan), transparent);
  opacity:0.5;
}

.info-row{
  display:flex;
  justify-content:space-between;
  align-items:center;
  padding:3px 0;
  border-bottom:1px solid rgba(255,255,255,0.04);
}
.info-row:last-child{border-bottom:none;}
.info-label{
  font-family:var(--mono);
  font-size:8px;
  color:var(--muted);
  letter-spacing:0.5px;
  text-transform:uppercase;
}
.info-value{
  font-family:var(--mono);
  font-size:8px;
  color:var(--cyan);
  font-weight:600;
  letter-spacing:0.5px;
}

/* ═══════════════════════════════
   SPECS GRID
═══════════════════════════════ */
.specs-grid{
  display:grid;
  grid-template-columns:1fr 1fr;
  gap:5px;
  margin-bottom:10px;
}

.spec-cell{
  background:rgba(255,255,255,0.025);
  border:1px solid rgba(255,255,255,0.06);
  border-radius:6px;
  padding:7px 8px;
  display:flex;
  align-items:center;
  gap:6px;
}
.spec-icon{width:14px;height:14px;flex-shrink:0;}
.spec-info{}
.spec-key{
  font-family:var(--mono);
  font-size:7px;
  color:var(--muted);
  text-transform:uppercase;
  letter-spacing:0.8px;
  display:block;
}
.spec-val{
  font-family:var(--ui);
  font-size:10px;
  font-weight:700;
  color:var(--text);
  letter-spacing:0.3px;
  display:block;
  line-height:1.2;
}

/* android spans full width */
.spec-cell.full{
  grid-column:1/-1;
  justify-content:center;
  background:var(--gold-dim);
  border-color:rgba(255,204,0,0.15);
}
.spec-cell.full .spec-val{color:var(--gold);}

/* ═══════════════════════════════
   FOOTER
═══════════════════════════════ */
.footer{
  display:flex;
  align-items:center;
  justify-content:space-between;
  padding-top:10px;
  border-top:1px solid rgba(255,255,255,0.06);
}

.footer-left{}
.footer-note{
  font-family:var(--mono);
  font-size:7px;
  color:var(--muted);
  letter-spacing:1px;
  text-transform:uppercase;
  display:block;
  margin-bottom:3px;
}

.countdown-wrap{
  display:inline-flex;
  align-items:center;
  gap:5px;
  padding:4px 10px;
  border-radius:20px;
  border:1px solid rgba(51,255,102,0.3);
  background:rgba(51,255,102,0.06);
  transition:all 0.3s;
}
.countdown-wrap.danger{
  border-color:rgba(255,34,68,0.5);
  background:rgba(255,34,68,0.08);
  animation:blink 0.4s ease-in-out infinite;
}
.cd-label{
  font-family:var(--mono);
  font-size:7.5px;
  color:rgba(51,255,102,0.7);
  letter-spacing:1px;
}
.countdown-wrap.danger .cd-label{color:rgba(255,34,68,0.8);}
.cd-num{
  font-family:var(--display);
  font-size:13px;
  font-weight:700;
  color:#33ff66;
  line-height:1;
  min-width:14px;
  text-align:center;
}
.countdown-wrap.danger .cd-num{color:var(--red);}
.cd-s{
  font-family:var(--mono);
  font-size:7.5px;
  color:rgba(51,255,102,0.7);
}
.countdown-wrap.danger .cd-s{color:rgba(255,34,68,0.8);}

.footer-icon-wrap{
  width:36px; height:36px;
  position:relative;
}
.footer-icon-wrap::before{
  content:'';
  position:absolute; inset:-3px;
  border-radius:50%;
  border:1px dashed rgba(255,204,0,0.3);
  animation:spin-slow 6s linear infinite;
}
#iconPerfect{width:36px;height:36px;}

</style>
</head>
<body>

<div class="bg-grid"></div>
<div class="bg-vignette"></div>

<div class="card">
  <div class="hazard-bar"></div>
  <div class="corner corner-tl"></div>
  <div class="corner corner-tr"></div>
  <div class="corner corner-bl"></div>
  <div class="corner corner-br"></div>

  <div class="inner">

    <!-- HEADER -->
    <div class="header">
      <div class="brand-row">
        <div id="iconLogo" class="logo-icon"></div>
        <div class="brand-text">
          <div class="brand-sub">ZOROADMINSERVER SYSTEM</div>
          <div class="brand-name">ZCORE SDK</div>
        </div>
      </div>
      <div class="status-pill">
        <div class="pill-dot"></div>
        <div class="pill-text">EXPIRED</div>
      </div>
    </div>

    <!-- HERO -->
    <div class="hero">
      <div class="hero-ring">
        <div id="iconHero" class="hero-lottie"></div>
        <div class="hero-glow"></div>
      </div>
      <div class="hero-title">LICENCE REVOKED</div>
      <div class="hero-sub">ACCESS TERMINATED</div>
    </div>

    <div class="divider">
      <div class="divider-line"></div>
      <div class="divider-dot"></div>
      <div class="divider-line"></div>
    </div>

    <!-- DEVICE BLOCK -->
    <div class="device-block">
      <div id="deviceInfo">
        <div class="info-row">
          <span class="info-label">Model</span>
          <span class="info-value" id="dModel">— —</span>
        </div>
        <div class="info-row">
          <span class="info-label">Android</span>
          <span class="info-value" id="dAndroid">— —</span>
        </div>
        <div class="info-row">
          <span class="info-label">Status</span>
          <span class="info-value" style="color:var(--red)">UNAUTHORISED</span>
        </div>
      </div>
    </div>

    <!-- SPECS GRID -->
    <div class="specs-grid">
      <div class="spec-cell">
        <div id="iconSupport" class="spec-icon"></div>
        <div class="spec-info">
          <span class="spec-key">Support</span>
          <span class="spec-val">A8 → A17</span>
        </div>
      </div>
      <div class="spec-cell">
        <div id="iconDev" class="spec-icon"></div>
        <div class="spec-info">
          <span class="spec-key">Developer</span>
          <span class="spec-val">ZOROADMINSERVER ZCORE</span>
        </div>
      </div>
      <div class="spec-cell">
        <div id="iconContact" class="spec-icon"></div>
        <div class="spec-info">
          <span class="spec-key">Contact</span>
          <span class="spec-val">@ZOROADMINSERVER</span>
        </div>
      </div>
      <div class="spec-cell">
        <div id="iconShield" class="spec-icon"></div>
        <div class="spec-info">
          <span class="spec-key">Security</span>
          <span class="spec-val">ACTIVE</span>
        </div>
      </div>
      <div class="spec-cell full">
        <div class="spec-info" style="text-align:center;">
          <span class="spec-key" style="text-align:center;margin-bottom:2px;">Android Compatibility</span>
          <span class="spec-val" style="font-size:12px;letter-spacing:1px;">8.0 ——— 17.0</span>
        </div>
      </div>
    </div>

    <!-- FOOTER -->
    <div class="footer">
      <div class="footer-left">
        <span class="footer-note">Renewal required</span>
        <div class="countdown-wrap" id="cdWrap">
          <span class="cd-label">CLOSE IN</span>
          <span class="cd-num" id="timer">10</span>
          <span class="cd-s">s</span>
        </div>
      </div>
      <div class="footer-icon-wrap">
        <div id="iconPerfect"></div>
      </div>
    </div>

  </div>
</div>

<script>
// ── Lottie icons ──
const L = lottie.loadAnimation;
L({container:document.getElementById('iconLogo'),    renderer:'svg',loop:true,autoplay:true,path:'https://assets10.lottiefiles.com/packages/lf20_j1adxtyb.json'});
L({container:document.getElementById('iconHero'),    renderer:'svg',loop:true,autoplay:true,path:'https://assets4.lottiefiles.com/packages/lf20_jtbfg2nb.json'});
L({container:document.getElementById('iconPerfect'), renderer:'svg',loop:true,autoplay:true,path:'https://assets4.lottiefiles.com/packages/lf20_jtbfg2nb.json'});
L({container:document.getElementById('iconSupport'), renderer:'svg',loop:true,autoplay:true,path:'https://assets2.lottiefiles.com/packages/lf20_jcikwtux.json'});
L({container:document.getElementById('iconDev'),     renderer:'svg',loop:true,autoplay:true,path:'https://assets7.lottiefiles.com/packages/lf20_w51pcehl.json'});
L({container:document.getElementById('iconContact'), renderer:'svg',loop:true,autoplay:true,path:'https://assets3.lottiefiles.com/packages/lf20_5ngs2ksb.json'});
L({container:document.getElementById('iconShield'),  renderer:'svg',loop:true,autoplay:true,path:'https://assets9.lottiefiles.com/packages/lf20_dews3j6m.json'});

// ── Device info from Android bridge ──
function loadDeviceInfo(){
  if(window.Android && Android.getDeviceInfo){
    try{
      const info = Android.getDeviceInfo();
      const lines = info.split('\n');
      const div = document.getElementById('deviceInfo');
      div.innerHTML = '';
      lines.forEach(line => {
        if(!line.trim()) return;
        const idx = line.indexOf(':');
        if(idx < 0) return;
        const label = line.slice(0,idx).trim();
        const value = line.slice(idx+1).trim();
        if(!label || !value) return;
        const row = document.createElement('div');
        row.className = 'info-row';
        // FIX: escape $ so Kotlin doesn't treat it as a template
        row.innerHTML = `<span class="info-label">${'$'}{label}</span><span class="info-value">${'$'}{value}</span>`;
        div.appendChild(row);
      });
      // always add status row
      const statusRow = document.createElement('div');
      statusRow.className = 'info-row';
      statusRow.innerHTML = '<span class="info-label">Status</span><span class="info-value" style="color:var(--red)">UNAUTHORISED</span>';
      div.appendChild(statusRow);
    }catch(e){}
  }
}

// ── Countdown ──
let sec = 10;
const timerEl  = document.getElementById('timer');
const cdWrap   = document.getElementById('cdWrap');

const tick = setInterval(() => {
  sec--;
  timerEl.textContent = sec;
  if(sec <= 5) cdWrap.classList.add('danger');
  if(sec <= 0){
    clearInterval(tick);
    if(window.Android && Android.close) Android.close();
  }
}, 1000);

document.addEventListener('DOMContentLoaded', () => {
  loadDeviceInfo();
  setTimeout(() => { if(window.Android && Android.close) Android.close(); }, 10500);
});
</script>
</body>
</html>

"""
}