package com.kotoba.app.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.kotoba.app.R;
import com.kotoba.app.audio.JapaneseSpeechHelper;
import com.kotoba.app.audio.PronunciationTarget;
import com.kotoba.app.audio.SoundManager;
import com.kotoba.app.data.model.LearningObject;
import com.kotoba.app.ui.responsive.ResponsiveLayoutSystem;

public class FlashcardView extends FrameLayout {

    public enum StartSide {
        KANJI,
        HIRAGANA,
        ARTI
    }

    public interface OnCardFlipListener {
        void onCardFlip(boolean isShowingBack);
    }

    private static final long FLIP_DURATION_MS = 300L;

    private View mFrontView;
    private View mBackView;

    // Front Views
    private TextView mTxtFrontBadge;
    private TextView mTxtFrontHint;
    private TextView mTxtFrontFurigana;
    private TextView mTxtFrontJapanese;
    private TextView mTxtFrontRomaji;
    private ImageButton mBtnFrontSpeech;

    // Back Views
    private TextView mTxtBackBadge;
    private TextView mTxtBackHint;
    private TextView mTxtBackFurigana;
    private TextView mTxtBackJapanese;
    private TextView mTxtBackRomaji;
    private TextView mTxtBackIndonesian;
    private ImageButton mBtnBackSpeech;
    private View mDividerBack;

    private boolean mIsShowingBack = false;
    private boolean mIsFlipping = false;

    private LearningObject mCurrentItem;
    private StartSide mCurrentStartSide = StartSide.KANJI;
    private boolean mFuriganaEnabled = true;
    private boolean mRomajiEnabled = false;

    private SoundManager mSound;
    private JapaneseSpeechHelper mSpeech;
    private OnCardFlipListener mFlipListener;

    public FlashcardView(Context context) {
        super(context);
        init(context);
    }

    public FlashcardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FlashcardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mFrontView = inflater.inflate(R.layout.view_flashcard_front, this, false);
        mBackView = inflater.inflate(R.layout.view_flashcard_back, this, false);

        addView(mBackView);
        addView(mFrontView);

        // Bind Front
        mTxtFrontBadge = mFrontView.findViewById(R.id.txt_front_bab_badge);
        mTxtFrontHint = mFrontView.findViewById(R.id.txt_front_hint);
        mTxtFrontFurigana = mFrontView.findViewById(R.id.txt_front_furigana);
        mTxtFrontJapanese = mFrontView.findViewById(R.id.txt_front_japanese);
        mTxtFrontRomaji = mFrontView.findViewById(R.id.txt_front_romaji);
        mBtnFrontSpeech = mFrontView.findViewById(R.id.card_btn_speech_front);

        // Bind Back
        mTxtBackBadge = mBackView.findViewById(R.id.txt_back_bab_badge);
        mTxtBackHint = mBackView.findViewById(R.id.txt_back_hint);
        mTxtBackFurigana = mBackView.findViewById(R.id.txt_back_furigana);
        mTxtBackJapanese = mBackView.findViewById(R.id.txt_back_japanese);
        mTxtBackRomaji = mBackView.findViewById(R.id.txt_back_romaji);
        mTxtBackIndonesian = mBackView.findViewById(R.id.txt_back_indonesian);
        mBtnBackSpeech = mBackView.findViewById(R.id.card_btn_speech_back);
        mDividerBack = mBackView.findViewById(R.id.divider_back);

        // Set camera distance for clean 3D perspective
        float scale = getResources().getDisplayMetrics().density;
        float cameraDist = 8000 * scale;
        mFrontView.setCameraDistance(cameraDist);
        mBackView.setCameraDistance(cameraDist);

        mBackView.setVisibility(View.GONE);
        mFrontView.setVisibility(View.VISIBLE);
        mIsShowingBack = false;

        // Card tap flips
        OnClickListener flipClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                flipCard();
            }
        };
        mFrontView.setOnClickListener(flipClick);
        mBackView.setOnClickListener(flipClick);

        // Speech listeners
        OnLongClickListener speechLongClick = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Context ctx = getContext();
                if (ctx != null) {
                    new com.kotoba.app.ui.voice.VoiceSettingsDialog(ctx).show();
                    return true;
                }
                return false;
            }
        };
        if (mBtnFrontSpeech != null) {
            mBtnFrontSpeech.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    playFrontAudio();
                }
            });
            mBtnFrontSpeech.setOnLongClickListener(speechLongClick);
        }
        if (mBtnBackSpeech != null) {
            mBtnBackSpeech.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    playBackAudio();
                }
            });
            mBtnBackSpeech.setOnLongClickListener(speechLongClick);
        }
    }

    public void setSoundManager(SoundManager sound) {
        this.mSound = sound;
    }

    public void setSpeechHelper(JapaneseSpeechHelper speech) {
        this.mSpeech = speech;
    }

    public void setOnCardFlipListener(OnCardFlipListener listener) {
        this.mFlipListener = listener;
    }

    public boolean isShowingFront() {
        return !mIsShowingBack;
    }

    public void resetToFront() {
        mIsShowingBack = false;
        mFrontView.setRotationY(0f);
        mBackView.setRotationY(0f);
        mFrontView.setVisibility(View.VISIBLE);
        mBackView.setVisibility(View.GONE);
    }

    public void flipCard() {
        if (mIsFlipping) return;
        mIsFlipping = true;

        if (mSound != null) {
            mSound.playCardFlip();
        }

        final View currentVisible = mIsShowingBack ? mBackView : mFrontView;
        final View currentHidden = mIsShowingBack ? mFrontView : mBackView;
        final boolean targetIsBack = !mIsShowingBack;

        // Animate first half: rotate from 0 to 90 degrees
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(currentVisible, "rotationY", 0f, 90f);
        anim1.setDuration(FLIP_DURATION_MS / 2);
        anim1.setInterpolator(new AccelerateInterpolator());
        anim1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentVisible.setVisibility(View.GONE);
                currentHidden.setVisibility(View.VISIBLE);
                currentHidden.setRotationY(-90f);

                // Animate second half: rotate from -90 to 0 degrees
                ObjectAnimator anim2 = ObjectAnimator.ofFloat(currentHidden, "rotationY", -90f, 0f);
                anim2.setDuration(FLIP_DURATION_MS / 2);
                anim2.setInterpolator(new DecelerateInterpolator());
                anim2.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mIsShowingBack = targetIsBack;
                        mIsFlipping = false;
                        if (mFlipListener != null) {
                            mFlipListener.onCardFlip(mIsShowingBack);
                        }
                    }
                });
                anim2.start();
            }
        });
        anim1.start();
    }

    public void bind(LearningObject item, StartSide startSide, boolean furiganaEnabled, boolean romajiEnabled) {
        if (mCurrentItem == null || (item != null && !item.getId().equals(mCurrentItem.getId()))) {
            JapaneseSpeechHelper speech = (mSpeech != null) ? mSpeech : JapaneseSpeechHelper.getInstance(getContext());
            if (speech != null) {
                speech.stop();
            }
        }
        this.mCurrentItem = item;
        this.mCurrentStartSide = startSide != null ? startSide : StartSide.KANJI;
        this.mFuriganaEnabled = furiganaEnabled;
        this.mRomajiEnabled = romajiEnabled;

        if (item == null) {
            bindEmpty();
            return;
        }

        String badge = item.getBadgeLabel();
        mTxtFrontBadge.setText(badge);
        mTxtBackBadge.setText(badge);

        // Content assignment based on FrontMode
        if (mCurrentStartSide == StartSide.KANJI) {
            // FRONT: Primary Kanji / Ideograph representation
            mTxtFrontJapanese.setText(item.getJapanese());
            if (mFuriganaEnabled && item.hasKanji()) {
                mTxtFrontFurigana.setVisibility(View.VISIBLE);
                mTxtFrontFurigana.setText(item.getReading());
            } else {
                mTxtFrontFurigana.setVisibility(View.GONE);
            }
            if (mRomajiEnabled && item.getRomaji() != null && !item.getRomaji().isEmpty()) {
                mTxtFrontRomaji.setVisibility(View.VISIBLE);
                mTxtFrontRomaji.setText(item.getRomaji());
            } else {
                mTxtFrontRomaji.setVisibility(View.GONE);
            }

            // BACK: Answer face (Reading + Meaning must NEVER disappear)
            if (item.getType() == LearningObject.Type.KANJI) {
                // Prominently show the dual reading (On'yomi / Kun'yomi) as the primary answer to the kanji
                mTxtBackJapanese.setText(item.getDualReadingDisplay());
                // Show the kanji ideograph above as reference
                mTxtBackFurigana.setVisibility(View.VISIBLE);
                mTxtBackFurigana.setText(item.getJapanese());
            } else if (item.hasKanji()) {
                // Prominently show the reading as the primary answer to the kanji
                mTxtBackJapanese.setText(item.getReading());
                // Show the kanji ideograph above as reference
                mTxtBackFurigana.setVisibility(View.VISIBLE);
                mTxtBackFurigana.setText(item.getJapanese());
            } else {
                mTxtBackJapanese.setText(item.getJapanese());
                mTxtBackFurigana.setVisibility(View.GONE);
            }

            if (mRomajiEnabled && item.getRomaji() != null && !item.getRomaji().isEmpty()) {
                mTxtBackRomaji.setVisibility(View.VISIBLE);
                mTxtBackRomaji.setText(item.getRomaji());
            } else {
                mTxtBackRomaji.setVisibility(View.GONE);
            }

            if (item.getType() == LearningObject.Type.GRAMMAR) {
                mTxtBackIndonesian.setText(item.getIndonesian() + "\n\n" + item.getFormula() + "\n" + item.getExplanation());
            } else {
                mTxtBackIndonesian.setText(item.getIndonesian());
            }

        } else if (mCurrentStartSide == StartSide.HIRAGANA) {
            // FRONT: Phonetic Kana reading representation
            mTxtFrontFurigana.setVisibility(View.GONE);
            String frontReading = (item.getReading() != null && !item.getReading().isEmpty())
                    ? item.getReading()
                    : item.getJapanese();
            mTxtFrontJapanese.setText(frontReading);

            if (mRomajiEnabled && item.getRomaji() != null && !item.getRomaji().isEmpty()) {
                mTxtFrontRomaji.setVisibility(View.VISIBLE);
                mTxtFrontRomaji.setText(item.getRomaji());
            } else {
                mTxtFrontRomaji.setVisibility(View.GONE);
            }

            // BACK: Kanji + Meaning
            mTxtBackJapanese.setText(item.getJapanese());
            if (item.getType() == LearningObject.Type.KANJI) {
                mTxtBackFurigana.setVisibility(View.VISIBLE);
                mTxtBackFurigana.setText(item.getDualReadingDisplay());
            } else if (item.hasKanji()) {
                mTxtBackFurigana.setVisibility(View.VISIBLE);
                mTxtBackFurigana.setText(item.getReading());
            } else {
                mTxtBackFurigana.setVisibility(View.GONE);
            }

            if (mRomajiEnabled && item.getRomaji() != null && !item.getRomaji().isEmpty()) {
                mTxtBackRomaji.setVisibility(View.VISIBLE);
                mTxtBackRomaji.setText(item.getRomaji());
            } else {
                mTxtBackRomaji.setVisibility(View.GONE);
            }
            mTxtBackIndonesian.setText(item.getIndonesian());

        } else { // ARTI (Meaning on front)
            // FRONT: Indonesian Meaning
            mTxtFrontFurigana.setVisibility(View.GONE);
            mTxtFrontJapanese.setText(item.getIndonesian());
            mTxtFrontRomaji.setVisibility(View.GONE);

            // BACK: Kanji + Reading + Meaning
            mTxtBackJapanese.setText(item.getJapanese());
            if (item.getType() == LearningObject.Type.KANJI) {
                mTxtBackFurigana.setVisibility(View.VISIBLE);
                mTxtBackFurigana.setText(item.getDualReadingDisplay());
            } else if (item.hasKanji() || (item.getReading() != null && !item.getReading().isEmpty())) {
                mTxtBackFurigana.setVisibility(View.VISIBLE);
                mTxtBackFurigana.setText(item.getReading());
            } else {
                mTxtBackFurigana.setVisibility(View.GONE);
            }

            if (mRomajiEnabled && item.getRomaji() != null && !item.getRomaji().isEmpty()) {
                mTxtBackRomaji.setVisibility(View.VISIBLE);
                mTxtBackRomaji.setText(item.getRomaji());
            } else {
                mTxtBackRomaji.setVisibility(View.GONE);
            }
            mTxtBackIndonesian.setText(item.getIndonesian());
        }

        applyResponsiveTokens();
    }

    private void bindEmpty() {
        mTxtFrontBadge.setText("Kosong");
        mTxtFrontHint.setVisibility(View.GONE);
        mTxtFrontFurigana.setVisibility(View.GONE);
        mTxtFrontJapanese.setText("Tidak ada data");
        mTxtFrontRomaji.setVisibility(View.GONE);

        mTxtBackBadge.setText("Kosong");
        mTxtBackHint.setVisibility(View.GONE);
        mTxtBackFurigana.setVisibility(View.GONE);
        mTxtBackJapanese.setText("Tidak ada data");
        mTxtBackRomaji.setVisibility(View.GONE);
        mTxtBackIndonesian.setText("Pilih Bab atau Materi melalui tombol Filter.");

        applyResponsiveTokens();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0 && (w != oldw || h != oldh)) {
            applyResponsiveTokens();
        }
    }

    public void applyResponsiveTokens() {
        ResponsiveLayoutSystem rls = ResponsiveLayoutSystem.from(getContext());
        int cardPadding = rls.getCardPaddingPx();
        mFrontView.setPadding(cardPadding, cardPadding, cardPadding, cardPadding);
        mBackView.setPadding(cardPadding, cardPadding, cardPadding, cardPadding);

        // Badge & Hint
        float badgeSp = rls.getBadgeTextSizeSp();
        float hintSp = rls.getHintTextSizeSp();
        rls.applyToTextView(mTxtFrontBadge, badgeSp);
        rls.applyToTextView(mTxtBackBadge, badgeSp);
        rls.applyToTextView(mTxtFrontHint, hintSp);
        rls.applyToTextView(mTxtBackHint, hintSp);

        // Front typography
        int frontLen = mTxtFrontJapanese.getText() != null ? mTxtFrontJapanese.getText().length() : 0;
        rls.applyToTextView(mTxtFrontJapanese, rls.getCardJapaneseSizeSp(true, frontLen));
        rls.applyToTextView(mTxtFrontFurigana, rls.getCardFuriganaSizeSp());
        rls.applyToTextView(mTxtFrontRomaji, rls.getCardRomajiSizeSp());

        // Back typography
        int backJapLen = mTxtBackJapanese.getText() != null ? mTxtBackJapanese.getText().length() : 0;
        int meaningLen = mTxtBackIndonesian.getText() != null ? mTxtBackIndonesian.getText().length() : 0;
        rls.applyToTextView(mTxtBackJapanese, rls.getCardJapaneseSizeSp(false, backJapLen));
        rls.applyToTextView(mTxtBackFurigana, rls.getCardFuriganaSizeSp());
        rls.applyToTextView(mTxtBackRomaji, rls.getCardRomajiSizeSp());
        rls.applyToTextView(mTxtBackIndonesian, rls.getCardMeaningSizeSp(meaningLen));

        // Back divider width & vertical margins
        if (mDividerBack != null) {
            int cardW = getWidth() > 0 ? getWidth() : rls.dpToPx(rls.getScreenWidthDp() - 32);
            int dividerW = rls.getDividerWidthPx(cardW);
            int vMargin = rls.getDividerVerticalMarginPx();
            ViewGroup.LayoutParams lp = mDividerBack.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                mlp.width = dividerW;
                mlp.topMargin = vMargin;
                mlp.bottomMargin = vMargin;
                mDividerBack.setLayoutParams(mlp);
            }
        }
    }

    public void playAudio() {
        if (mIsShowingBack) {
            playBackAudio();
        } else {
            playFrontAudio();
        }
    }

    public void playFrontAudio() {
        if (mCurrentItem == null) return;
        JapaneseSpeechHelper speech = (mSpeech != null) ? mSpeech : JapaneseSpeechHelper.getInstance(getContext());
        if (speech == null) return;

        if (!speech.isAvailable()) {
            Toast.makeText(getContext(), "Suara bahasa Jepang belum aktif di perangkat Anda.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Authoritative pronunciation source: strictly use reading to guarantee phonetic fidelity
        // and prevent independent morphological guessing between Front and Back faces
        String textToSpeak = (mCurrentItem.getReading() != null && !mCurrentItem.getReading().trim().isEmpty() && !mCurrentItem.getReading().trim().equals("—"))
                ? mCurrentItem.getReading().trim()
                : mCurrentItem.getJapanese();

        PronunciationTarget target = mCurrentItem.isKatakana()
                ? PronunciationTarget.KATAKANA
                : PronunciationTarget.READING;

        String displayedText = mTxtFrontJapanese.getText().toString();
        speech.speak(textToSpeak, target, displayedText, mCurrentStartSide.name() + "_FRONT", mCurrentItem.getId(), null);
    }

    public void playBackAudio() {
        if (mCurrentItem == null) return;
        JapaneseSpeechHelper speech = (mSpeech != null) ? mSpeech : JapaneseSpeechHelper.getInstance(getContext());
        if (speech == null) return;

        if (!speech.isAvailable()) {
            Toast.makeText(getContext(), "Suara bahasa Jepang belum aktif di perangkat Anda.", Toast.LENGTH_SHORT).show();
            return;
        }

        String textToSpeak = (mCurrentItem.getReading() != null && !mCurrentItem.getReading().trim().isEmpty() && !mCurrentItem.getReading().trim().equals("—"))
                ? mCurrentItem.getReading().trim()
                : mCurrentItem.getJapanese();

        PronunciationTarget target = mCurrentItem.isKatakana()
                ? PronunciationTarget.KATAKANA
                : PronunciationTarget.READING;

        String displayedText = mTxtBackJapanese.getText().toString();
        speech.speak(textToSpeak, target, displayedText, mCurrentStartSide.name() + "_BACK", mCurrentItem.getId(), null);
    }
}
