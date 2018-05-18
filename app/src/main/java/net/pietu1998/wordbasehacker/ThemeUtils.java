package net.pietu1998.wordbasehacker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ThemeUtils {

    private ThemeUtils() {}

    public static void setTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean dark = prefs.getBoolean(context.getString(R.string.pref_key_darktheme), false);
        context.setTheme(dark ? R.style.Theme_AppCompat : R.style.Theme_AppCompat_Light);
    }

}
