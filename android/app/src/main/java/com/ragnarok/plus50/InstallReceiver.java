package com.ragnarok.plus50;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.widget.Toast;

/** Rezultat namestitve posodobitve (PackageInstaller). */
public class InstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // sistem vpraša uporabnika za potrditev namestitve
            Intent confirm = Build.VERSION.SDK_INT >= 33
                    ? intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class)
                    : intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(confirm);
            }
            return;
        }
        if (status == PackageInstaller.STATUS_SUCCESS) return;   // aplikacija se znova zažene z novo različico
        if (status == PackageInstaller.STATUS_FAILURE_ABORTED) return; // uporabnik je preklical

        String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        String reason = describe(status) + (msg == null ? "" : " (" + msg + ")");
        MainActivity a = MainActivity.current();
        if (a != null) a.runOnUiThread(() -> Updater.showInstallError(a, reason));
        else Toast.makeText(ctx, "Posodobitev ni uspela: " + reason, Toast.LENGTH_LONG).show();
    }

    private static String describe(int status) {
        switch (status) {
            case PackageInstaller.STATUS_FAILURE_BLOCKED: return "Namestitev je blokirana (Play Protect ali omejitev telefona).";
            case PackageInstaller.STATUS_FAILURE_CONFLICT: return "Spor z obstoječo aplikacijo (drug podpis).";
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE: return "Aplikacija ni združljiva s tem telefonom.";
            case PackageInstaller.STATUS_FAILURE_INVALID: return "Prenesena datoteka je poškodovana.";
            case PackageInstaller.STATUS_FAILURE_STORAGE: return "Premalo prostora na telefonu.";
            default: return "Aplikacija ni nameščena.";
        }
    }
}
