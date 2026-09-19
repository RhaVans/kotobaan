package com.kotoba.app.audio;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import com.kotoba.app.data.PreferencesManager;

import java.util.Locale;
import java.util.Set;

/**
 * Centralized Japanese Text-To-Speech service.
 * Configured specifically for an elegant, calm, intelligent female system-assistant character.
 * Uses native Android TextToSpeech voices without requiring large external models.
 */
public class JapaneseSpeechHelper {
    private static final String TAG = "JapaneseSpeechHelper";

    public interface SpeechInitListener {
        void onSpeechReady(boolean isAvailable);
    }

    public interface SpeechProgressListener {
        void onSpeechStarted();
        void onSpeechDone();
        void onSpeechError(String error);
    }

    private static JapaneseSpeechHelper sInstance;

    private final Context mContext;
    private final PreferencesManager mPrefs;
    private TextToSpeech mTextToSpeech;
    private SageTtsEngine mSageEngine;
    private boolean mIsReady = false;
    private boolean mIsLanguageAvailable = false;
    private String mSelectedVoiceName = "System Default";
    private SpeechInitListener mInitListener;

    public static synchronized JapaneseSpeechHelper getInstance(Context context) {
        if (sInstance == null && context != null) {
            sInstance = new JapaneseSpeechHelper(context.getApplicationContext(), null);
        }
        return sInstance;
    }

    public JapaneseSpeechHelper(Context context, SpeechInitListener listener) {
        this.mContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        this.mPrefs = PreferencesManager.getInstance(this.mContext);
        this.mInitListener = listener;

        // Initialize embedded offline Sage TTS Neural Engine
        try {
            mSageEngine = SageTtsEngine.getInstance(this.mContext);
            mSageEngine.initialize(new SageTtsEngine.InitCallback() {
                @Override
                public void onInitialized(boolean success, String message) {
                    Log.i(TAG, "Sage TTS Neural Engine initialized: " + success + " (" + message + ")");
                    if (mInitListener != null) {
                        mInitListener.onSpeechReady(isAvailable());
                    }
                }
            });
        } catch (Throwable t) {
            Log.w(TAG, "Could not initialize Sage TTS, will rely on system TTS: " + t.getMessage());
        }

        initializeTts();
    }

    private void initializeTts() {
        mTextToSpeech = new TextToSpeech(mContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int langResult = mTextToSpeech.setLanguage(Locale.JAPANESE);
                    if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                        langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Japanese TTS voice data is missing or unsupported on this device.");
                        mIsReady = false;
                        mIsLanguageAvailable = false;
                    } else {
                        mIsReady = true;
                        mIsLanguageAvailable = true;
                        applyVoiceConfiguration();
                        Log.i(TAG, "Japanese TTS initialized successfully. Voice: " + mSelectedVoiceName);
                    }
                } else {
                    Log.e(TAG, "TTS initialization failed with status: " + status);
                    mIsReady = false;
                    mIsLanguageAvailable = false;
                }

                if (mInitListener != null) {
                    mInitListener.onSpeechReady(mIsReady && mIsLanguageAvailable);
                }
            }
        });
    }

    public void applyVoiceConfiguration() {
        if (mTextToSpeech == null || !mIsReady) return;

        try {
            Voice bestVoice = findBestFemaleJapaneseVoice();
            if (bestVoice != null) {
                mTextToSpeech.setVoice(bestVoice);
                mSelectedVoiceName = bestVoice.getName();
            } else {
                mTextToSpeech.setLanguage(Locale.JAPANESE);
                mSelectedVoiceName = "Default Japanese";
            }
        } catch (Throwable t) {
            try {
                mTextToSpeech.setLanguage(Locale.JAPANESE);
            } catch (Throwable ignored) {}
            mSelectedVoiceName = "Default Japanese";
        }

        // Apply calibrated system-assistant parameters
        float speed = mPrefs != null ? mPrefs.getTtsSpeed() : 0.90f;
        float pitch = mPrefs != null ? mPrefs.getTtsPitch() : 0.95f;

        try {
            mTextToSpeech.setSpeechRate(speed);
            mTextToSpeech.setPitch(pitch);
        } catch (Throwable ignored) {}
    }

    public void updateVoiceParameters(float speed, float pitch) {
        if (mPrefs != null) {
            mPrefs.setTtsSpeed(speed);
            mPrefs.setTtsPitch(pitch);
        }
        if (mTextToSpeech != null && mIsReady) {
            try {
                mTextToSpeech.setSpeechRate(speed);
                mTextToSpeech.setPitch(pitch);
            } catch (Throwable ignored) {}
        }
    }

    private Voice findBestFemaleJapaneseVoice() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || mTextToSpeech == null) {
            return null;
        }

        try {
            Set<Voice> voices = mTextToSpeech.getVoices();
            if (voices == null || voices.isEmpty()) return null;

            String userPrefVoice = (mPrefs != null) ? mPrefs.getTtsVoiceName() : "";
            Voice bestCandidate = null;
            int highestScore = Integer.MIN_VALUE;

            for (Voice v : voices) {
                if (v == null || v.getLocale() == null) continue;
                String lang = v.getLocale().getLanguage();
                if (!"ja".equalsIgnoreCase(lang)) continue;

                // If user specifically saved this voice name, prefer it directly
                if (!userPrefVoice.isEmpty() && userPrefVoice.equals(v.getName())) {
                    return v;
                }

                int score = scoreVoice(v);
                if (score > highestScore) {
                    highestScore = score;
                    bestCandidate = v;
                }
            }

            return bestCandidate;
        } catch (Throwable t) {
            Log.w(TAG, "Could not enumerate voices: " + t.getMessage());
            return null;
        }
    }

    public static int scoreVoice(Voice v) {
        if (v == null) return -999;
        int score = 0;
        String name = (v.getName() != null) ? v.getName().toLowerCase(Locale.ROOT) : "";
        Set<String> features = v.getFeatures();
        String featStr = (features != null) ? features.toString().toLowerCase(Locale.ROOT) : "";

        // 1. Google TTS known female voices: htm, jaf, hce, jbb
        if (name.contains("htm") || name.contains("jaf") || name.contains("hce") || name.contains("jbb")) {
            score += 100;
        }

        // 2. Female gender markers in name or metadata features
        if (name.contains("female") || name.contains("woman") || name.contains("fem") || name.contains("f0") ||
            featStr.contains("female") || featStr.contains("gender=female")) {
            score += 80;
        }

        // 3. Penalty for male voices
        if (name.contains("jab") || name.contains("jad") || name.contains("hcb") || name.contains("male") ||
            name.contains("man") || featStr.contains("gender=male")) {
            score -= 100;
        }

        // 4. Offline / Local voice bonus (eliminates network delay & guarantees instant response)
        if (!v.isNetworkConnectionRequired()) {
            score += 30;
        }

        // 5. High quality bonus
        if (v.getQuality() >= Voice.QUALITY_HIGH) {
            score += 15;
        }

        return score;
    }

    public boolean isAvailable() {
        return (mSageEngine != null && mSageEngine.isInitialized()) ||
               (mIsReady && mIsLanguageAvailable && mTextToSpeech != null);
    }

    public String getSelectedVoiceName() {
        if (mSageEngine != null && mSageEngine.isInitialized()) {
            return "Sage Neural Voice (Embedded)";
        }
        return mSelectedVoiceName != null ? mSelectedVoiceName : "System Default";
    }

    public String getActiveVoiceDescription() {
        if (mSageEngine != null && mSageEngine.isInitialized()) {
            return "Female · Sage Neural (Offline)";
        }
        String voice = getSelectedVoiceName();
        if (voice.contains("htm") || voice.contains("jaf") || voice.contains("hce") || voice.contains("jbb") || voice.toLowerCase(Locale.ROOT).contains("female")) {
            return "Female · System (" + voice + ")";
        }
        return "Female · System";
    }

    public void speak(String rawText) {
        speak(rawText, PronunciationTarget.CUSTOM, rawText, "DEFAULT", "", null);
    }

    public void speak(String rawText, final SpeechProgressListener listener) {
        speak(rawText, PronunciationTarget.CUSTOM, rawText, "DEFAULT", "", listener);
    }

    public void speak(String rawText, PronunciationTarget target, String displayedText, String cardMode, String itemId, final SpeechProgressListener listener) {
        final String text = TtsTextPreprocessor.preprocess(rawText);
        if (text == null || text.isEmpty()) {
            return;
        }

        // Development debug log requested for TTS fidelity audit
        Log.d(TAG, "\n" +
                "==================== [TTS DEBUG] ====================\n" +
                "DISPLAYED:    " + (displayedText != null ? displayedText : "") + "\n" +
                "TTS INPUT:    " + text + "\n" +
                "SOURCE FIELD: " + (target != null ? target.name() : "CUSTOM") + "\n" +
                "CARD MODE:    " + (cardMode != null ? cardMode : "") + "\n" +
                "ITEM ID:      " + (itemId != null ? itemId : "") + "\n" +
                "====================================================");

        stop();

        // 1. Primary: Try embedded Sage TTS Neural Engine
        if (mSageEngine != null && mSageEngine.isInitialized()) {
            mSageEngine.speak(text, new SageTtsEngine.SpeechCallback() {
                @Override
                public void onSpeechStarted() {
                    if (listener != null) listener.onSpeechStarted();
                }

                @Override
                public void onSpeechCompleted(long latencyMs, float durationSec, float rtf) {
                    if (listener != null) listener.onSpeechDone();
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Sage TTS error, falling back to System TTS: " + error);
                    speakWithSystemTts(text, listener);
                }
            });
            return;
        }

        // 2. Fallback: Android System TTS
        speakWithSystemTts(text, listener);
    }

    private void speakWithSystemTts(String text, final SpeechProgressListener listener) {
        if (mTextToSpeech == null || !mIsReady || !mIsLanguageAvailable) {
            if (listener != null) {
                listener.onSpeechError("Suara bahasa Jepang belum terpasang di perangkat ini.");
            }
            return;
        }

        String utteranceId = "kotoba_" + System.currentTimeMillis();
        mTextToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (listener != null) listener.onSpeechStarted();
            }

            @Override
            public void onDone(String utteranceId) {
                if (listener != null) listener.onSpeechDone();
            }

            @Override
            public void onError(String utteranceId) {
                if (listener != null) listener.onSpeechError("Gagal memutar audio.");
            }
        });

        try {
            mTextToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, (Bundle) null, utteranceId);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to speak text via system TTS: " + t.getMessage());
            if (listener != null) {
                listener.onSpeechError("Gagal memutar suara.");
            }
        }
    }

    public void testVoice(String phrase) {
        String testPhrase = (phrase != null && !phrase.trim().isEmpty())
                ? phrase
                : "こんにちは。日本語を一緒に勉強しましょう。";
        speak(testPhrase, PronunciationTarget.CUSTOM, testPhrase, "TEST", "test", null);
    }

    public void stop() {
        if (mSageEngine != null) {
            try {
                mSageEngine.stop();
            } catch (Throwable ignored) {}
        }
        if (mTextToSpeech != null) {
            try {
                // Always call stop unconditionally to flush speech queue & eliminate stale utterances
                mTextToSpeech.stop();
            } catch (Throwable ignored) {}
        }
    }

    public void openTtsSettings(Context context) {
        try {
            Intent intent = new Intent();
            intent.setAction("com.android.settings.TTS_SETTINGS");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Could not open TTS settings", e);
        }
    }

    public void shutdown() {
        if (mSageEngine != null) {
            try {
                mSageEngine.release();
            } catch (Throwable ignored) {}
            mSageEngine = null;
        }
        if (mTextToSpeech != null) {
            try {
                mTextToSpeech.stop();
                mTextToSpeech.shutdown();
            } catch (Throwable ignored) {}
            mTextToSpeech = null;
        }
        mIsReady = false;
        mIsLanguageAvailable = false;
        if (sInstance == this) {
            sInstance = null;
        }
    }
}
