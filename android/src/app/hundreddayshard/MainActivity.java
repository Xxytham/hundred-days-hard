package app.hundreddayshard;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.genai.common.DownloadCallback;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.common.GenAiException;
import com.google.mlkit.genai.prompt.GenerateContentRequest;
import com.google.mlkit.genai.prompt.GenerateContentResponse;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.TextPart;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQ_HEALTH = 1;
    private static final int REQ_RESTORE = 2;
    private static final String BACKUP_NAME = "100-days-hard-backup.json";
    private static final String BACKUP_DIR = Environment.DIRECTORY_DOWNLOADS + "/100 Days Hard";

    private WebView web;
    private FrameLayout root;
    private SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean loaded = false;
    private String shortcut = ""; // workout type from a home-screen shortcut, waiting for the page

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (getActionBar() != null) getActionBar().hide();
        getWindow().setDecorFitsSystemWindows(false);
        prefs = getSharedPreferences("app", MODE_PRIVATE);

        root = new FrameLayout(this);
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        applyTheme();
        // Keep the launch screen (the plate) up until the page has loaded, at most 2 seconds.
        final long shownAt = SystemClock.uptimeMillis();
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!loaded && SystemClock.uptimeMillis() - shownAt < 2000) return false;
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });

        // Keep the page clear of the status bar, navigation bar and keyboard.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsets.CONSUMED;
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) { loaded = true; }
        });
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);

        readShortcut(getIntent());
        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");
    }

    // Long-pressing the app icon offers "Log Push day" and so on (res/xml/shortcuts.xml).
    private void readShortcut(Intent i) {
        Uri d = i == null ? null : i.getData();
        if (d == null || !"hundreddayshard".equals(d.getScheme()) || !"log".equals(d.getHost())) return;
        String type = d.getLastPathSegment();
        if ("push".equals(type) || "pull".equals(type) || "legs".equals(type)) shortcut = type;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (ACTION_INSTALLED.equals(intent.getAction())) { installStatus(intent); return; }
        readShortcut(intent);
        if (loaded && !shortcut.isEmpty()) {
            String type = shortcut;
            shortcut = "";
            js("window.__shortcut && window.__shortcut(" + JSONObject.quote(type) + ")");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loaded) js("window.__onResume && window.__onResume()");
    }

    private void js(String code) {
        runOnUiThread(() -> web.evaluateJavascript(code, null));
    }

    private boolean isDark() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyTheme() {
        boolean dark = isDark();
        int bg = dark ? Color.parseColor("#0F1215") : Color.parseColor("#EDF0F3");
        root.setBackgroundColor(bg);
        web.setBackgroundColor(bg);
        WindowInsetsController c = getWindow().getInsetsController();
        if (c != null) {
            int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            c.setSystemBarsAppearance(dark ? 0 : light, light);
        }
    }

    // ---------- backup file in Download/100 Days Hard ----------

    private synchronized Uri writeBackup(String json) {
        ContentResolver cr = getContentResolver();
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String saved = prefs.getString("backupUri", null);
        if (saved != null) {
            Uri uri = Uri.parse(saved);
            try (OutputStream os = cr.openOutputStream(uri, "wt")) {
                if (os != null) {
                    os.write(bytes);
                    markBackup();
                    return uri;
                }
            } catch (Exception ignored) {
                // The file was deleted or moved; make a new one below.
            }
        }
        try {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, BACKUP_NAME);
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, BACKUP_DIR);
            Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) return null;
            try (OutputStream os = cr.openOutputStream(uri, "wt")) {
                if (os == null) return null;
                os.write(bytes);
            }
            prefs.edit().putString("backupUri", uri.toString()).apply();
            markBackup();
            return uri;
        } catch (Exception e) {
            return null;
        }
    }

    private void markBackup() {
        prefs.edit().putLong("backupAt", System.currentTimeMillis()).apply();
    }

    private void shareFile(Uri uri) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/json");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_SUBJECT, "100 Days Hard backup");
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(send, "Save your backup"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_RESTORE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        io.execute(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                String text = out.toString("UTF-8");
                js("window.__restoreText(" + JSONObject.quote(text) + ")");
            } catch (Exception e) {
                js("window.__restoreText('')");
            }
        });
    }

    // ---------- Samsung Health via Health Connect ----------

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQ_HEALTH) return;
        String status = HealthReader.status(this);
        js("window.__healthPermission && window.__healthPermission(" + JSONObject.quote(status) + ")");
    }

    // ---------- updates: download inside the app, then Android's own "Update this app?" prompt ----------
    private static final String ACTION_INSTALLED = "app.hundreddayshard.INSTALL_STATUS";
    private volatile boolean updating = false;

    private void updateState(String state, int pct, String error) {
        try {
            JSONObject o = new JSONObject();
            o.put("state", state);
            o.put("pct", pct);
            if (error != null) o.put("error", error);
            js("window.__updateState && window.__updateState(" + o + ")");
        } catch (Exception ignored) { }
    }

    private void downloadAndInstall(String url) {
        if (updating) return;
        updating = true;
        io.execute(() -> {
            File dir = new File(getCacheDir(), "updates");
            File apk = new File(dir, "update.apk");
            try {
                dir.mkdirs();
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection(); // follows GitHub's redirects
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                long total = c.getContentLengthLong(), got = 0;
                int last = -1;
                try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(apk)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        got += n;
                        int pct = total > 0 ? (int) (got * 100 / total) : -1;
                        if (pct != last && (pct - last >= 2 || pct == 100)) { last = pct; updateState("downloading", pct, null); }
                    }
                }
                // Only hand over a newer version of this same app.
                PackageInfo next = getPackageManager().getPackageArchiveInfo(apk.getPath(), 0);
                PackageInfo now = getPackageManager().getPackageInfo(getPackageName(), 0);
                if (next == null || !getPackageName().equals(next.packageName)) throw new Exception("not this app");
                if (next.getLongVersionCode() <= now.getLongVersionCode()) { updating = false; updateState("current", 100, null); return; }
                updateState("installing", 100, null);
                PackageInstaller pi = getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(getPackageName());
                int id = pi.createSession(params);
                try (PackageInstaller.Session session = pi.openSession(id)) {
                    try (InputStream in = new FileInputStream(apk); OutputStream out = session.openWrite("base.apk", 0, apk.length())) {
                        byte[] buf = new byte[65536];
                        int n;
                        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                        session.fsync(out);
                    }
                    Intent done = new Intent(this, MainActivity.class).setAction(ACTION_INSTALLED);
                    PendingIntent pi2 = PendingIntent.getActivity(this, 7, done, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                    session.commit(pi2.getIntentSender());
                }
            } catch (Exception e) {
                updating = false;
                updateState("failed", 0, String.valueOf(e.getMessage()));
            }
        });
    }

    private void installStatus(Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
            if (confirm != null) { updateState("confirm", 100, null); startActivity(confirm); return; }
        }
        updating = false;
        if (status == PackageInstaller.STATUS_SUCCESS) updateState("done", 100, null); // Android restarts the app with the new version
        else updateState("failed", 0, status == PackageInstaller.STATUS_FAILURE_ABORTED ? "cancelled" : intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
    }

    // ---------- coach: Gemini Nano on the phone (AICore), through ML Kit's Prompt API ----------
    private GenerativeModelFutures model;

    private GenerativeModelFutures model() {
        if (model == null) model = GenerativeModelFutures.from(Generation.INSTANCE.getClient());
        return model;
    }

    private void aiStatusTo(String status) {
        js("window.__aiStatus && window.__aiStatus(" + JSONObject.quote(status) + ")");
    }

    class Bridge {
        /** Downloads the newest release and asks Android to install it. Answers through window.__updateState. */
        @JavascriptInterface
        public void updateApp(String url) {
            if (url == null || !url.startsWith("https://")) return;
            if (!getPackageManager().canRequestPackageInstalls()) {
                // One time only: let this app install its own updates.
                updateState("permission", 0, null);
                runOnUiThread(() -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
                    } catch (Exception ignored) { }
                });
                return;
            }
            downloadAndInstall(url);
        }

        @JavascriptInterface
        public boolean canInstallUpdates() {
            return getPackageManager().canRequestPackageInstalls();
        }

        /** Answers through window.__aiStatus: "available", "downloadable", "downloading" or "unavailable". */
        @JavascriptInterface
        public void aiStatus() {
            try {
                ListenableFuture<Integer> f = model().checkStatus();
                f.addListener(() -> {
                    String st = "unavailable";
                    try {
                        int v = f.get();
                        st = v == FeatureStatus.AVAILABLE ? "available" : v == FeatureStatus.DOWNLOADABLE ? "downloadable"
                                : v == FeatureStatus.DOWNLOADING ? "downloading" : "unavailable";
                    } catch (Exception ignored) { }
                    aiStatusTo(st);
                }, io);
            } catch (Throwable t) { // no AICore or Play services on this phone
                aiStatusTo("unavailable");
            }
        }

        /** Fetches Gemini Nano when the phone has it but hasn't downloaded it yet. */
        @JavascriptInterface
        public void aiDownload() {
            try {
                model().download(new DownloadCallback() {
                    @Override public void onDownloadStarted(long bytes) { aiStatusTo("downloading"); }
                    @Override public void onDownloadCompleted() { aiStatusTo("available"); }
                    @Override public void onDownloadFailed(GenAiException e) { aiStatusTo("downloadable"); }
                });
            } catch (Throwable t) {
                aiStatusTo("unavailable");
            }
        }

        /** Writes a short reply to the prompt; answers through window.__aiResult({ok, text}). */
        @JavascriptInterface
        public void aiGenerate(String prompt) {
            try {
                GenerateContentRequest.Builder b = new GenerateContentRequest.Builder(new TextPart(prompt));
                b.setTemperature(0.4f);
                b.setTopK(16);
                b.setMaxOutputTokens(160);
                ListenableFuture<GenerateContentResponse> f = model().generateContent(b.build());
                f.addListener(() -> {
                    JSONObject o = new JSONObject();
                    try {
                        String text = f.get().getCandidates().get(0).getText();
                        o.put("ok", text != null && !text.trim().isEmpty());
                        o.put("text", text == null ? "" : text.trim());
                    } catch (Exception e) {
                        try { o.put("ok", false); o.put("error", String.valueOf(e.getMessage())); } catch (Exception ignored) { }
                    }
                    js("window.__aiResult && window.__aiResult(" + o + ")");
                }, io);
            } catch (Throwable t) {
                js("window.__aiResult && window.__aiResult({ok:false,error:'unavailable'})");
            }
        }

        // The page asks once it's ready, in case the app was opened from a shortcut.
        @JavascriptInterface
        public String takeShortcut() {
            String type = shortcut;
            shortcut = "";
            return type;
        }

        // A light buzz through the phone's own haptics (no vibration permission needed).
        @JavascriptInterface
        public void haptic(String kind) {
            int k;
            switch (kind == null ? "" : kind) {
                case "confirm": k = HapticFeedbackConstants.CONFIRM; break;
                case "start": k = HapticFeedbackConstants.GESTURE_START; break;
                case "end": k = HapticFeedbackConstants.GESTURE_END; break;
                case "reject": k = HapticFeedbackConstants.REJECT; break;
                default: k = HapticFeedbackConstants.CLOCK_TICK;
            }
            final int effect = k;
            runOnUiThread(() -> web.performHapticFeedback(effect));
        }

        @JavascriptInterface
        public void saveBackup(String json) {
            io.execute(() -> writeBackup(json));
        }

        @JavascriptInterface
        public void shareBackup(String json) {
            io.execute(() -> {
                Uri uri = writeBackup(json);
                if (uri != null) runOnUiThread(() -> shareFile(uri));
            });
        }

        @JavascriptInterface
        public String backupInfo() {
            try {
                JSONObject o = new JSONObject();
                o.put("at", prefs.getLong("backupAt", 0));
                o.put("path", "Download/100 Days Hard");
                return o.toString();
            } catch (Exception e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public void restoreBackup() {
            runOnUiThread(() -> {
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("*/*");
                startActivityForResult(pick, REQ_RESTORE);
            });
        }

        @JavascriptInterface
        public String appVersion() {
            try {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public void openUrl(String url) {
            if (url == null || !url.startsWith("https://")) return;
            runOnUiThread(() -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception ignored) { }
            });
        }

        @JavascriptInterface
        public void shareText(String text) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(send, "Share 100 Days Hard"));
            });
        }

        @JavascriptInterface
        public String healthMissing() {
            return HealthReader.missing(MainActivity.this);
        }

        @JavascriptInterface
        public String healthStatus() {
            return HealthReader.status(MainActivity.this);
        }

        @JavascriptInterface
        public void healthConnect() {
            runOnUiThread(() -> requestPermissions(HealthReader.PERMISSIONS, REQ_HEALTH));
        }

        @JavascriptInterface
        public void healthSettings() {
            runOnUiThread(() -> {
                try {
                    Intent i = new Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS");
                    i.putExtra(Intent.EXTRA_PACKAGE_NAME, getPackageName());
                    startActivity(i);
                } catch (Exception e) {
                    try {
                        startActivity(new Intent("android.health.connect.action.HEALTH_HOME_SETTINGS"));
                    } catch (Exception ignored) { }
                }
            });
        }

        @JavascriptInterface
        public void healthSync(String fromIso, String toIso) {
            HealthReader.read(MainActivity.this, fromIso, toIso, io,
                    json -> js("window.__healthResult && window.__healthResult(" + json + ")"));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyTheme();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.__appBack ? window.__appBack() : false", value -> {
            if (!"true".equals(value)) finish();
        });
    }
}
