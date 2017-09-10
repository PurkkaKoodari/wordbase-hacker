package net.pietu1998.wordbasehacker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;

public final class HudUtils {

    private HudUtils() {
    }

    public static boolean isHudEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.pref_key_hud), true);
    }

    public static boolean isHudAutoEnabled(Context context) {
        return isHudEnabled(context) &&
                PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.pref_key_hudauto), true);
    }

    public static boolean canShowHud(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static void requestHudPermission(final Activity context, boolean fromSettings) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(R.string.allow_draw_on_top_title);
        dialog.setMessage(fromSettings ? R.string.allow_draw_on_top_settings : R.string.allow_draw_on_top);
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                context.startActivity(intent);
            }
        });
        if (fromSettings) {
            dialog.setNegativeButton(R.string.cancel, null);
        } else {
            dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.pref_key_hud), false).apply();
                    ((HackerApplication) context.getApplication()).stopHudService();
                }
            });
        }
        dialog.setCancelable(fromSettings);
        dialog.show();
    }
}
