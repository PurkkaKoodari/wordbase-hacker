package net.pietu1998.wordbasehacker;


import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatActivity {

    private int shouldReload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.setTheme(this);
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        shouldReload = GameListActivity.SHOULD_NOT_RELOAD;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(shouldReload);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(shouldReload);
        super.onBackPressed();
    }

    public static class SettingsFragment extends PreferenceFragment {
        private SharedPreferences.OnSharedPreferenceChangeListener listener;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            listener = (preferences, key) -> {
                if (key.equals(getString(R.string.pref_key_dbpath)))
                    ((SettingsActivity) getActivity()).shouldReload = GameListActivity.SHOULD_RELOAD;
            };
            prefs.registerOnSharedPreferenceChangeListener(listener);

            final Preference nativeParamsPreference = findPreference(getString(R.string.pref_key_native_params));
            nativeParamsPreference.setOnPreferenceClickListener(preference -> {
                new NativeParamsDialog(getActivity()).show();
                return true;
            });

            final Preference nativePreference = findPreference(getString(R.string.pref_key_native));
            nativePreference.setOnPreferenceChangeListener((preference, value) -> {
                nativePreference.setSummary((Boolean) value ? R.string.pref_desc_native_on : R.string.pref_desc_native_off);
                nativeParamsPreference.setEnabled((Boolean) value);
                return true;
            });
            boolean useNative = prefs.getBoolean(nativePreference.getKey(), true);
            nativePreference.setSummary(useNative ? R.string.pref_desc_native_on : R.string.pref_desc_native_off);
            nativeParamsPreference.setEnabled(useNative);

            Preference themePreference = findPreference(getString(R.string.pref_key_darktheme));
            themePreference.setOnPreferenceChangeListener((preference, o) -> {
                getActivity().setResult(GameListActivity.SHOULD_RESTART);
                getActivity().finish();
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                return true;
            });

            Preference dbPathPreference = findPreference(getString(R.string.pref_key_dbpath));
            Preference.OnPreferenceChangeListener pathChangeListener = (preference, value) -> {
                String stringValue = value.toString();
                preference.setSummary(stringValue.isEmpty() ? getString(R.string.pref_desc_dbpath) : stringValue);
                return true;
            };
            dbPathPreference.setOnPreferenceChangeListener(pathChangeListener);
            String currentDbPath = prefs.getString(dbPathPreference.getKey(), "");
            pathChangeListener.onPreferenceChange(dbPathPreference, currentDbPath);

            final Preference hudEditPreference = findPreference(getString(R.string.pref_key_hudedit));
            hudEditPreference.setOnPreferenceClickListener(preference -> {
                HudUtils.showHudInfo(getActivity(), R.string.pref_key_hide_edit_info, R.string.hud_edit_info, () -> {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.wordbaseapp", "com.wordbaseapp.BoardActivity"));
                    startActivity(intent);
                    ((HackerApplication) getActivity().getApplication()).editHudSettings();
                });
                return true;
            });

            Preference.OnPreferenceChangeListener hudEnableListener = (preference, value) -> {
                boolean enabled = (boolean) value;
                if (enabled && !HudUtils.canShowHud(getActivity())) {
                    HudUtils.requestHudPermission(getActivity(), true);
                    return false;
                }
                hudEditPreference.setEnabled(enabled);
                if (enabled)
                    ((HackerApplication) getActivity().getApplication()).startHudService();
                else
                    ((HackerApplication) getActivity().getApplication()).stopHudService();
                return true;
            };
            Preference hudPreference = findPreference(getString(R.string.pref_key_hud));
            hudPreference.setOnPreferenceChangeListener(hudEnableListener);
            hudEnableListener.onPreferenceChange(hudPreference, prefs.getBoolean(hudPreference.getKey(), true));

            try {
                String version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
                if (BuildConfig.DEBUG)
                    version = getString(R.string.debug_version, version);
                findPreference(getString(R.string.pref_key_version)).setSummary(version);
            } catch (PackageManager.NameNotFoundException ignore) {
                findPreference(getString(R.string.pref_key_version)).setSummary(R.string.internal_error);
            }

            findPreference(getString(R.string.pref_key_source)).setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.bitbucket_url)));
                startActivity(intent);
                return true;
            });
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
}
