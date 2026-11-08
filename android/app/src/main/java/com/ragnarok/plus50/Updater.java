package com.ragnarok.plus50;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Posodobitve iz GitHub Releases: najnovejša izdaja ima oznako "v<številka>" in datoteko plus50.apk.
 * Če je številka višja od trenutne versionCode, ponudi prenos in namestitev.
 */
final class Updater {
    private static File pendingApk;   // čaka, da uporabnik dovoli nameščanje iz te aplikacije

    private Updater() { }

    static void check(MainActivity a, boolean manual) {
        if (manual) a.notifyWeb("Preverjam posodobitve…");
        new Thread(() -> {
            try {
                String body = get("https://api.github.com/repos/" + BuildConfig.UPDATE_REPO + "/releases/latest");
                JSONObject rel = new JSONObject(body);
                int latest = parseVersion(rel.optString("tag_name"));
                String apkUrl = null;
                JSONArray assets = rel.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject as = assets.getJSONObject(i);
                        if (as.optString("name").endsWith(".apk")) { apkUrl = as.optString("browser_download_url"); break; }
                    }
                }
                final String url = apkUrl;
                final String notes = rel.optString("body", "").trim();
                a.runOnUiThread(() -> {
                    if (a.isFinishing()) return;
                    if (latest > BuildConfig.VERSION_CODE && url != null) {
                        new AlertDialog.Builder(a)
                                .setTitle("Nova različica 1." + latest)
                                .setMessage("Imaš 1." + BuildConfig.VERSION_CODE + ". Podatki ostanejo shranjeni."
                                        + (notes.isEmpty() ? "" : "\n\n" + clip(notes)))
                                .setPositiveButton("Posodobi", (d, w) -> download(a, url))
                                .setNegativeButton("Kasneje", null)
                                .show();
                    } else if (manual) {
                        a.notifyWeb("Imaš najnovejšo različico (" + BuildConfig.VERSION_NAME + ").");
                    }
                });
            } catch (Exception e) {
                if (manual) a.notifyWeb("Preverjanje ni uspelo – si povezan z internetom?");
            }
        }).start();
    }

    private static void download(MainActivity a, String url) {
        final AlertDialog dlg = new AlertDialog.Builder(a)
                .setTitle("Prenašam posodobitev")
                .setMessage("0 %")
                .setCancelable(false)
                .show();
        new Thread(() -> {
            File dir = new File(a.getCacheDir(), "updates");
            File out = new File(dir, "plus50.apk");
            try {
                if (!dir.exists() && !dir.mkdirs()) throw new IOException("mkdir");
                HttpURLConnection c = open(url);
                long total = c.getContentLengthLong(), done = 0;
                int lastPct = -1;
                try (InputStream in = c.getInputStream(); FileOutputStream fo = new FileOutputStream(out)) {
                    byte[] buf = new byte[32 * 1024];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        fo.write(buf, 0, n);
                        done += n;
                        if (total > 0) {
                            int pct = (int) (done * 100 / total);
                            if (pct != lastPct) {
                                lastPct = pct;
                                a.runOnUiThread(() -> dlg.setMessage(pct + " %"));
                            }
                        }
                    }
                } finally {
                    c.disconnect();
                }
                a.runOnUiThread(() -> { dlg.dismiss(); install(a, out); });
            } catch (Exception e) {
                a.runOnUiThread(() -> {
                    dlg.dismiss();
                    a.notifyWeb("Prenos posodobitve ni uspel.");
                });
            }
        }).start();
    }

    private static void install(Activity a, File apk) {
        if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
            pendingApk = apk;
            new AlertDialog.Builder(a)
                    .setTitle("Dovoli posodobitve")
                    .setMessage("Android mora enkrat dovoliti, da Program +50 namesti posodobitev. Vklopi »Dovoli iz tega vira« in se vrni.")
                    .setPositiveButton("Odpri nastavitve", (d, w) -> a.startActivity(
                            new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName()))))
                    .setNegativeButton("Prekliči", (d, w) -> pendingApk = null)
                    .show();
            return;
        }
        pendingApk = null;
        Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".files", apk);
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        a.startActivity(i);
    }

    /** Ko se uporabnik vrne iz nastavitev z dovoljenjem, namestitev nadaljuj. */
    static void resumePendingInstall(Activity a) {
        if (pendingApk == null || !pendingApk.exists()) return;
        if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) return;
        install(a, pendingApk);
    }

    private static int parseVersion(String tag) {
        String digits = tag.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try { return Integer.parseInt(digits); } catch (NumberFormatException e) { return 0; }
    }

    private static String clip(String s) { return s.length() > 400 ? s.substring(0, 400) + "…" : s; }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Plus50Android/" + BuildConfig.VERSION_NAME);
        int code = c.getResponseCode();
        if (code != 200) { c.disconnect(); throw new IOException("HTTP " + code); }
        return c;
    }

    private static String get(String url) throws IOException {
        HttpURLConnection c = open(url);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toString(StandardCharsets.UTF_8.name());
        } finally {
            c.disconnect();
        }
    }
}
