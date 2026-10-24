package app.hundreddayshard;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");
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

    class Bridge {
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
