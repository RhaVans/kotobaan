package com.kotoba.app.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import com.kotoba.app.R;
import com.kotoba.app.data.PreferencesManager;

public class SoundManager {
    private static final String TAG = "SoundManager";
    private static SoundManager sInstance;

    private final Context mContext;
    private SoundPool mSoundPool;
    private int mSoundFlip;
    private int mSoundClick;
    private int mSoundFilter;
    private int mSoundToggle;
    private boolean mLoaded = false;

    public static synchronized SoundManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SoundManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private SoundManager(Context context) {
        this.mContext = context;
        initSoundPool();
    }

    private void initSoundPool() {
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            mSoundPool = new SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(attrs)
                    .build();

            mSoundFlip = mSoundPool.load(mContext, R.raw.snd_card_flip, 1);
            mSoundClick = mSoundPool.load(mContext, R.raw.snd_click_tap, 1);
            mSoundFilter = mSoundPool.load(mContext, R.raw.snd_deck_filter, 1);
            mSoundToggle = mSoundPool.load(mContext, R.raw.snd_toggle, 1);

            mLoaded = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SoundPool", e);
            mLoaded = false;
        }
    }

    public void playCardFlip() {
        playSound(mSoundFlip);
    }

    public void playClickTap() {
        playSound(mSoundClick);
    }

    public void playDeckFilter() {
        playSound(mSoundFilter);
    }

    public void playToggle() {
        playSound(mSoundToggle);
    }

    public void playCorrect() {
        playSound(mSoundClick);
    }

    public void playIncorrect() {
        playSound(mSoundToggle);
    }

    public void playSuccessFanfare() {
        playSound(mSoundFlip);
    }

    private void playSound(int soundId) {
        if (!mLoaded || mSoundPool == null) return;
        PreferencesManager prefs = PreferencesManager.getInstance(mContext);
        if (prefs.isSoundEnabled()) {
            try {
                mSoundPool.play(soundId, 0.7f, 0.7f, 1, 0, 1.0f);
            } catch (Exception e) {
                Log.e(TAG, "Error playing sound effect", e);
            }
        }
    }

    public void release() {
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
            mLoaded = false;
        }
    }
}
