package com.kotoba.app.ui.voice;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.kotoba.app.R;
import com.kotoba.app.audio.JapaneseSpeechHelper;
import com.kotoba.app.data.PreferencesManager;

public class VoiceSettingsDialog extends Dialog {

    private final JapaneseSpeechHelper mSpeech;
    private final PreferencesManager mPrefs;

    private TextView mTxtVoiceName;
    private TextView mTxtVoiceDetails;
    private ImageButton mBtnClose;

    private Button mBtnSpeed075;
    private Button mBtnSpeed085;
    private Button mBtnSpeed090;
    private Button mBtnSpeed100;
    private Button mBtnSpeed110;

    private Button mBtnPitchLower;
    private Button mBtnPitchNormal;
    private Button mBtnPitchHigher;

    private Button mBtnTestVoice;
    private Button mBtnSystemSettings;

    private float mCurrentSpeed = 0.90f;
    private float mCurrentPitch = 0.95f;

    public VoiceSettingsDialog(Context context) {
        super(context, PreferencesManager.getInstance(context).isDarkMode()
                ? R.style.Theme_Kotoba_Dark
                : R.style.Theme_Kotoba);
        this.mPrefs = PreferencesManager.getInstance(context);
        this.mSpeech = JapaneseSpeechHelper.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_voice_settings);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(getWindow().getAttributes());
            lp.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.92);
            getWindow().setAttributes(lp);
        }

        mCurrentSpeed = mPrefs.getTtsSpeed();
        mCurrentPitch = mPrefs.getTtsPitch();

        initViews();
        setupListeners();
        updateVisuals();
    }

    private void initViews() {
        mTxtVoiceName = findViewById(R.id.txt_voice_name);
        mTxtVoiceDetails = findViewById(R.id.txt_voice_details);
        mBtnClose = findViewById(R.id.btn_dialog_voice_close);

        mBtnSpeed075 = findViewById(R.id.btn_speed_075);
        mBtnSpeed085 = findViewById(R.id.btn_speed_085);
        mBtnSpeed090 = findViewById(R.id.btn_speed_090);
        mBtnSpeed100 = findViewById(R.id.btn_speed_100);
        mBtnSpeed110 = findViewById(R.id.btn_speed_110);

        mBtnPitchLower = findViewById(R.id.btn_pitch_lower);
        mBtnPitchNormal = findViewById(R.id.btn_pitch_normal);
        mBtnPitchHigher = findViewById(R.id.btn_pitch_higher);

        mBtnTestVoice = findViewById(R.id.btn_test_voice);
        mBtnSystemSettings = findViewById(R.id.btn_system_tts_settings);

        if (mSpeech != null) {
            mTxtVoiceName.setText(mSpeech.getActiveVoiceDescription());
            mTxtVoiceDetails.setText("Suara: " + mSpeech.getSelectedVoiceName() + " • Tenang, presisi & sistematis");
        }
    }

    private void setupListeners() {
        mBtnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        // Speed listeners
        View.OnClickListener speedClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.btn_speed_075) mCurrentSpeed = 0.75f;
                else if (id == R.id.btn_speed_085) mCurrentSpeed = 0.85f;
                else if (id == R.id.btn_speed_090) mCurrentSpeed = 0.90f;
                else if (id == R.id.btn_speed_100) mCurrentSpeed = 1.00f;
                else if (id == R.id.btn_speed_110) mCurrentSpeed = 1.10f;

                applyParameters();
                updateVisuals();
            }
        };
        mBtnSpeed075.setOnClickListener(speedClick);
        mBtnSpeed085.setOnClickListener(speedClick);
        mBtnSpeed090.setOnClickListener(speedClick);
        mBtnSpeed100.setOnClickListener(speedClick);
        mBtnSpeed110.setOnClickListener(speedClick);

        // Pitch listeners
        View.OnClickListener pitchClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.btn_pitch_lower) mCurrentPitch = 0.88f;
                else if (id == R.id.btn_pitch_normal) mCurrentPitch = 0.95f;
                else if (id == R.id.btn_pitch_higher) mCurrentPitch = 1.05f;

                applyParameters();
                updateVisuals();
            }
        };
        mBtnPitchLower.setOnClickListener(pitchClick);
        mBtnPitchNormal.setOnClickListener(pitchClick);
        mBtnPitchHigher.setOnClickListener(pitchClick);

        // Test Voice button
        mBtnTestVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpeech != null) {
                    mSpeech.testVoice("こんにちは。日本語を一緒に勉強しましょう。");
                }
            }
        });

        // Open system TTS settings
        mBtnSystemSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpeech != null) {
                    mSpeech.openTtsSettings(getContext());
                }
            }
        });
    }

    private void applyParameters() {
        if (mSpeech != null) {
            mSpeech.updateVoiceParameters(mCurrentSpeed, mCurrentPitch);
        }
    }

    private void updateVisuals() {
        // Update speed chips
        setChipState(mBtnSpeed075, Math.abs(mCurrentSpeed - 0.75f) < 0.02f);
        setChipState(mBtnSpeed085, Math.abs(mCurrentSpeed - 0.85f) < 0.02f);
        setChipState(mBtnSpeed090, Math.abs(mCurrentSpeed - 0.90f) < 0.02f);
        setChipState(mBtnSpeed100, Math.abs(mCurrentSpeed - 1.00f) < 0.02f);
        setChipState(mBtnSpeed110, Math.abs(mCurrentSpeed - 1.10f) < 0.02f);

        // Update pitch chips
        setChipState(mBtnPitchLower, Math.abs(mCurrentPitch - 0.88f) < 0.02f);
        setChipState(mBtnPitchNormal, Math.abs(mCurrentPitch - 0.95f) < 0.02f);
        setChipState(mBtnPitchHigher, Math.abs(mCurrentPitch - 1.05f) < 0.02f);
    }

    private void setChipState(Button button, boolean active) {
        if (button == null) return;
        button.setBackgroundResource(active ? R.drawable.bg_segmented_active : android.R.color.transparent);
        button.setTextColor(getContext().getColor(active ? R.color.colorOnPrimary : R.color.colorTextSecondary));
    }
}
