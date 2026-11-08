package com.ragnarok.plus50;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Lupina za spletno aplikacijo Program +50.
 * index.html je zapakiran v APK in se streže z https://appassets.androidplatform.net,
 * zato localStorage (podatki) ostane med posodobitvami APK-ja.
 */
public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/assets/www/index.html";
    private static final int REQ_OPEN = 1;
    private static final int REQ_SAVE = 2;

    private WebView web;
    private boolean pageReady = false;
    private String pendingImport;          // podatki iz plus50://import, dokler se stran ne naloži
    private String pendingSave;            // backup, ki čaka na izbiro mesta shranjevanja
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web = new WebView(this);
        web.setBackgroundColor(0xFF0D0F13);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setUserAgentString(s.getUserAgentString() + " Plus50Android/" + BuildConfig.VERSION_NAME);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                return loader.shouldInterceptRequest(req.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (HOST.equals(u.getHost())) return false;
                // zunanje povezave (GitHub ipd.) odpri v brskalniku
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException ignored) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                deliverImport();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Izberi backup"), REQ_OPEN);
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "Plus50Android");

        handleIntent(getIntent());
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(START_URL);

        Updater.check(this, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Updater.resumePendingInstall(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        deliverImport();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        Uri u = intent.getData();
        if (u == null || !"plus50".equals(u.getScheme())) return;
        String d = u.getQueryParameter("d");
        if (d == null || d.isEmpty()) return;
        try {
            byte[] raw = Base64.decode(d, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            pendingImport = new String(raw, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            notifyWeb("Prenos podatkov ni uspel – poskusi z Izvozi/Uvozi backup.");
        }
    }

    private void deliverImport() {
        if (!pageReady || pendingImport == null) return;
        String js = "window.plus50Import&&window.plus50Import(" + JSONObject.quote(pendingImport) + ")";
        pendingImport = null;
        web.evaluateJavascript(js, null);
    }

    /** Kratko obvestilo v spletni aplikaciji (toast). */
    void notifyWeb(String msg) {
        if (web == null) return;
        web.post(() -> web.evaluateJavascript(
                "window.plus50Native&&window.plus50Native(" + JSONObject.quote(msg) + ")", null));
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_OPEN) {
            if (fileCallback == null) return;
            Uri u = (res == RESULT_OK && data != null) ? data.getData() : null;
            fileCallback.onReceiveValue(u != null ? new Uri[]{u} : null);
            fileCallback = null;
        } else if (req == REQ_SAVE) {
            String json = pendingSave;
            pendingSave = null;
            if (res != RESULT_OK || data == null || data.getData() == null || json == null) return;
            try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                notifyWeb("Backup izvožen.");
            } catch (Exception e) {
                notifyWeb("Shranjevanje ni uspelo.");
            }
        }
    }

    /** Funkcije, ki jih kliče index.html (window.Plus50Android). */
    private class Bridge {
        @JavascriptInterface
        public String version() { return BuildConfig.VERSION_NAME; }

        @JavascriptInterface
        public void checkUpdate() { runOnUiThread(() -> Updater.check(MainActivity.this, true)); }

        @JavascriptInterface
        public void saveBackup(String fileName, String json) {
            runOnUiThread(() -> {
                pendingSave = json;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/json")
                        .putExtra(Intent.EXTRA_TITLE, fileName);
                try { startActivityForResult(i, REQ_SAVE); }
                catch (ActivityNotFoundException e) { pendingSave = null; notifyWeb("Shranjevanje ni na voljo."); }
            });
        }
    }
}
