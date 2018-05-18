package net.pietu1998.wordbasehacker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public final class HudUtils {

    private HudUtils() {
    }

    public static boolean isHudEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.pref_key_hud), true);
    }

    public static boolean canShowHud(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void requestHudPermission(final Activity context, boolean fromSettings) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(R.string.allow_draw_on_top_title);
        dialog.setMessage(fromSettings ? R.string.allow_draw_on_top_settings : R.string.allow_draw_on_top);
        dialog.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            context.startActivity(intent);
        });
        if (fromSettings) {
            dialog.setNegativeButton(R.string.cancel, null);
        } else {
            dialog.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.pref_key_hud), false).apply();
                ((HackerApplication) context.getApplication()).stopHudService();
            });
        }
        dialog.setCancelable(fromSettings);
        dialog.show();
    }

    public static void showHudInfo(Context context, int hidePrefKey, int message, final Runnable after) {
        final String prefKey = context.getString(hidePrefKey);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(prefKey, false)) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            after.run();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final View layout = View.inflate(context, R.layout.info_dialog, null);
            builder.setView(layout);
            //builder.setMessage(message);
            ((TextView) layout.findViewById(R.id.info_content)).setText(message);
            builder.setNeutralButton(R.string.ok, (dialogInterface, i) -> {
                if (((CheckBox) layout.findViewById(R.id.dont_show_again)).isChecked())
                    prefs.edit().putBoolean(prefKey, true).apply();
                after.run();
            });
            builder.show();
        }
    }
}
