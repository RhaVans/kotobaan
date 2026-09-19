package com.kotoba.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class PreferencesManager {
    private static final String PREF_NAME = "kotoba_prefs";

    private static final String KEY_ACTIVE_NAV = "active_nav";
    private static final String KEY_CURRENT_BAB = "current_bab";
    private static final String KEY_SELECTED_BABS = "pref_selected_babs";
    private static final String KEY_LEARN_DOMAIN = "learn_domain";
    private static final String KEY_IS_DARK_MODE = "is_dark_mode";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_FURIGANA_ENABLED = "furigana_enabled";
    private static final String KEY_ROMAJI_ENABLED = "romaji_enabled";
    private static final String KEY_SHUFFLE_ENABLED = "shuffle_enabled";
    private static final String KEY_FRONT_MODE = "front_mode";
    private static final String KEY_TTS_VOICE_NAME = "pref_tts_voice_name";
    private static final String KEY_TTS_SPEED = "pref_tts_speed";
    private static final String KEY_TTS_PITCH = "pref_tts_pitch";

    private static PreferencesManager sInstance;
    private final SharedPreferences mPrefs;

    public static synchronized PreferencesManager getInstance(Context context) {
        if (sInstance == null && context != null) {
            Context appCtx = context.getApplicationContext();
            sInstance = new PreferencesManager(appCtx != null ? appCtx : context);
        }
        return sInstance;
    }

    private PreferencesManager(Context context) {
        this.mPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getActiveNav() {
        return mPrefs.getInt(KEY_ACTIVE_NAV, 0); // 0: Home, 1: Learn, 2: Review, 3: Progress
    }

    public void setActiveNav(int navIndex) {
        mPrefs.edit().putInt(KEY_ACTIVE_NAV, navIndex).apply();
    }

    public int getCurrentBab() {
        return mPrefs.getInt(KEY_CURRENT_BAB, 1);
    }

    public void setCurrentBab(int bab) {
        mPrefs.edit().putInt(KEY_CURRENT_BAB, bab).apply();
    }

    public Set<Integer> getSelectedBabs() {
        Set<String> raw = mPrefs.getStringSet(KEY_SELECTED_BABS, null);
        Set<Integer> result = new TreeSet<>();
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw) {
                try {
                    result.add(Integer.parseInt(s));
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    public void setSelectedBabs(Collection<Integer> babs) {
        Set<String> set = new HashSet<>();
        if (babs != null) {
            for (Integer b : babs) {
                if (b != null && b > 0) {
                    set.add(String.valueOf(b));
                }
            }
        }
        mPrefs.edit().putStringSet(KEY_SELECTED_BABS, set).apply();
    }

    public String getLearnDomain() {
        return mPrefs.getString(KEY_LEARN_DOMAIN, "VOCABULARY");
    }

    public void setLearnDomain(String domain) {
        mPrefs.edit().putString(KEY_LEARN_DOMAIN, domain).apply();
    }

    public boolean isDarkMode() {
        return mPrefs.getBoolean(KEY_IS_DARK_MODE, false);
    }

    public void setDarkMode(boolean darkMode) {
        mPrefs.edit().putBoolean(KEY_IS_DARK_MODE, darkMode).apply();
    }

    public boolean isSoundEnabled() {
        return mPrefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    public void setSoundEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    public boolean isFuriganaEnabled() {
        return mPrefs.getBoolean(KEY_FURIGANA_ENABLED, true);
    }

    public void setFuriganaEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_FURIGANA_ENABLED, enabled).apply();
    }

    public boolean isRomajiEnabled() {
        return mPrefs.getBoolean(KEY_ROMAJI_ENABLED, false);
    }

    public void setRomajiEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_ROMAJI_ENABLED, enabled).apply();
    }

    public boolean isShuffleEnabled() {
        return mPrefs.getBoolean(KEY_SHUFFLE_ENABLED, false);
    }

    public void setShuffleEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_SHUFFLE_ENABLED, enabled).apply();
    }

    public String getFrontMode() {
        return mPrefs.getString(KEY_FRONT_MODE, "KANJI");
    }

    public void setFrontMode(String mode) {
        mPrefs.edit().putString(KEY_FRONT_MODE, mode).apply();
    }

    public String getTtsVoiceName() {
        return mPrefs.getString(KEY_TTS_VOICE_NAME, "");
    }

    public void setTtsVoiceName(String voiceName) {
        mPrefs.edit().putString(KEY_TTS_VOICE_NAME, voiceName != null ? voiceName : "").apply();
    }

    public float getTtsSpeed() {
        return mPrefs.getFloat(KEY_TTS_SPEED, 0.90f);
    }

    public void setTtsSpeed(float speed) {
        mPrefs.edit().putFloat(KEY_TTS_SPEED, speed).apply();
    }

    public float getTtsPitch() {
        return mPrefs.getFloat(KEY_TTS_PITCH, 0.95f);
    }

    public void setTtsPitch(float pitch) {
        mPrefs.edit().putFloat(KEY_TTS_PITCH, pitch).apply();
    }
}
