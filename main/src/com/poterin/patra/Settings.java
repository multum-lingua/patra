package com.poterin.patra;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.Arrays;

public class Settings extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static SharedPreferences preferences;
    public static SharedPreferences.Editor settingsEditor;
    public final static int REQUEST_ID = 1;

    public static boolean deleteBookmarkAfterUse;
    public static boolean deleteBookmarksAfterInsert;

    private SettingsFragment settingsFragment;

    public static void open(Activity activity) {
        Intent intent = new Intent(activity, Settings.class);
        activity.startActivityForResult(intent, REQUEST_ID);
    }

    public static boolean isNightMode() {
        return preferences.getBoolean("isNightMode", false);
    }

    public static int currentTheme() {
        if (isNightMode())
            return android.R.style.Theme_Holo;
        else
            return android.R.style.Theme_Holo_Light;
    }

    public static boolean voiceInTraining() {
        return preferences.getBoolean("voiceInTraining", false);
    }

    public static String primaryLanguage() {
        return preferences.getString("primaryLanguage", "eng");
    }

    public static String secondaryLanguage() {
        return preferences.getString("secondaryLanguage", "rus");
    }

    public static String defaultDictionary() {
        return preferences.getString("defaultDictionary", "google_translator");
    }

    public static boolean scrollOnTap() {
        return preferences.getBoolean("scrollOnTap", false);
    }

    public static boolean scrollOnVolumeKey() {
        return preferences.getBoolean("scrollOnVolumeKey", false);
    }

    public static boolean translateByYandex() {
        return preferences.getBoolean("translateByYandex", true);
    }

    public static int phrasesOnPage() {
        return (int) preferences.getFloat("phrasesOnPage", 3);
    }

    public static int fontSize() {
        return (int) preferences.getFloat("fontSize", 20);
    }

    public static int leftMargin() {
        return (int) preferences.getFloat("leftMargin", 0);
    }

    public static int rightMargin() {
        return (int) preferences.getFloat("rightMargin", 0);
    }

    public static int topMargin() {
        return (int) preferences.getFloat("topMargin", 0);
    }

    public static int bottomMargin() {
        return (int) preferences.getFloat("bottomMargin", 0);
    }

    public static float speechSpeed() {
        return preferences.getFloat("speechSpeed", 1f);
    }

    public static void loadSettings(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        settingsEditor = preferences.edit();
        convertSettings();

        deleteBookmarkAfterUse = preferences.getBoolean("deleteBookmarkAfterUse", false);
        deleteBookmarksAfterInsert = preferences.getBoolean("deleteBookmarksAfterInsert", false);

        LibraryFilterDialog.loadSettings();
        settingsEditor.putInt("settings_version", 2);
        settingsEditor.commit();
    } // loadSettings

    private static void convertSettings() {
        if (preferences.contains("settings_version") || !preferences.contains("phrasesOnPage")) return;

        try {
            float phrasesOnPage = preferences.getInt("phrasesOnPage", 3);
            settingsEditor.remove("phrasesOnPage");
            float fontSize = preferences.getInt("fontSize", 20);
            settingsEditor.remove("fontSize");
            int defaultDictionary = preferences.getInt("defaultDictionary", 0);
            settingsEditor.remove("defaultDictionary");
            settingsEditor.commit();

            settingsEditor.putFloat("phrasesOnPage", phrasesOnPage);
            settingsEditor.putFloat("fontSize", fontSize);
            settingsEditor.putString("defaultDictionary", (defaultDictionary == 0) ? "google_translator" : "color_dict");
            settingsEditor.commit();
        }
        catch (ClassCastException e) {
            Utils.logException(e);
        }
    } // convertSettings

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.currentTheme());

        settingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, settingsFragment)
            .commit();
    } // onCreate

    @Override
    protected void onStart() {
        super.onStart();
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        settingsFragment.setSummary();
        ((BaseAdapter) settingsFragment.getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();

        if (key.equals("isNightMode")) MainActivity.setActualTheme();

        if (
            Arrays.asList("leftMargin", "rightMargin", "topMargin", "bottomMargin", "isNightMode",
                "fontSize", "phrasesOnPage", "primaryLanguage", "secondaryLanguage").contains(key)
            )
            setResult(RESULT_OK);
    } // onSharedPreferenceChanged

    private class SettingsFragment extends PreferenceFragment {
        private ListPreference defaultDictionary;
        private ListPreference primaryLanguage;
        private ListPreference secondaryLanguage;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            defaultDictionary = ((ListPreference) findPreference("defaultDictionary"));
            defaultDictionary.setEntryValues(Translator.dictionariesIds);

            primaryLanguage = ((ListPreference) findPreference("primaryLanguage"));
            primaryLanguage.setEntries(Languages.names);
            primaryLanguage.setEntryValues(Languages.langIds);

            secondaryLanguage = ((ListPreference) findPreference("secondaryLanguage"));
            secondaryLanguage.setEntries(Languages.names);
            secondaryLanguage.setEntryValues(Languages.langIds);

            final PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("pageMargins");
            preferenceScreen.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Window window = preferenceScreen.getDialog().getWindow();
                        ImageView view = (ImageView) window.findViewById(android.R.id.home);
                        if (view != null) {
                            view.setImageResource(R.drawable.menu_settings);
                        }
                        return false;
                    }
                });

            setSummary();
        } // onCreate

        private void setSummary() {
            findPreference("phrasesOnPage").setSummary(String.valueOf(phrasesOnPage()));
            findPreference("fontSize").setSummary(String.valueOf(fontSize()));
            findPreference("speechSpeed").setSummary(String.format("%.1f", speechSpeed()));
            defaultDictionary.setSummary(defaultDictionary.getEntry());
            primaryLanguage.setSummary(primaryLanguage.getEntry());
            secondaryLanguage.setSummary(secondaryLanguage.getEntry());
            findPreference("pageMargins").setSummary(
                leftMargin() + ", " + rightMargin() + ", " + topMargin() + ", " + bottomMargin());
            findPreference("leftMargin").setSummary(String.valueOf(leftMargin()));
            findPreference("rightMargin").setSummary(String.valueOf(rightMargin()));
            findPreference("topMargin").setSummary(String.valueOf(topMargin()));
            findPreference("bottomMargin").setSummary(String.valueOf(bottomMargin()));
        } // setSummary
    } // SettingsFragment
}