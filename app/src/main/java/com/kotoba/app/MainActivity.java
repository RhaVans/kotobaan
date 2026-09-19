package com.kotoba.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kotoba.app.audio.JapaneseSpeechHelper;
import com.kotoba.app.audio.SoundManager;
import com.kotoba.app.data.KotobaDatabase;
import com.kotoba.app.data.PreferencesManager;
import com.kotoba.app.data.model.LearningCycle;
import com.kotoba.app.data.model.LearningObject;
import com.kotoba.app.engine.IngatLupaEngine;
import com.kotoba.app.ui.FlashcardView;
import com.kotoba.app.ui.library.LibraryFilterDialog;
import com.kotoba.app.ui.responsive.ResponsiveLayoutSystem;
import com.kotoba.app.ui.voice.VoiceSettingsDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private KotobaDatabase mDatabase;
    private PreferencesManager mPrefs;
    private SoundManager mSound;
    private JapaneseSpeechHelper mSpeech;

    // Top Bar
    private TextView mTxtTitle;
    private TextView mTxtCardCounter;
    private TextView mTxtAuthor;
    private Button mBtnSectionKotoba;
    private Button mBtnSectionKanji;
    private TextView mTxtDeckStatus;
    private ProgressBar mProgressDeck;

    // Card
    private FlashcardView mFlashcardView;

    // Segmented Front Mode
    private Button mBtnModeKanji;
    private Button mBtnModeHiragana;
    private Button mBtnModeArti;

    // Toolbar
    private ImageButton mBtnFuriganaToggle;
    private ImageButton mBtnRomajiToggle;
    private ImageButton mBtnFilterBab;
    private ImageButton mBtnShuffle;
    private ImageButton mBtnThemeToggle;
    private ImageButton mBtnSoundToggle;

    // Navigation Dock
    private Button mBtnPrev;
    private Button mBtnFlip;
    private Button mBtnNext;

    // Ingat / Lupa Active Recall Dock
    private View mBtnLupa;
    private View mBtnIngat;

    // Deck & Engine State
    private String mActiveSection = "KOTOBA"; // "KOTOBA" or "KANJI"
    private String mActiveKanjiSub = "613"; // "613" or "ADDITIONAL"
    private View mKanjiSubContainer;
    private Button mBtnKanjiSub613;
    private Button mBtnKanjiSubAdditional;
    private View mKanjiReadingModeContainer;
    private Button mBtnMainKanjiModeOnyomi;
    private Button mBtnMainKanjiModeKunyomi;
    private Button mBtnMainKanjiModeBoth;
    private String mKanjiReadingMode = "both";
    private FlashcardView.StartSide mCurrentFrontMode = FlashcardView.StartSide.KANJI;
    private List<LearningObject> mActiveDeck = new ArrayList<>();
    private IngatLupaEngine mEngine;
    private int mCurrentIndex = 0;
    private String mCurrentDeckTitle = "Bab 1-25 • 863 kata";
    private long mCardShownTimeMs = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        PreferencesManager prefs = PreferencesManager.getInstance(newBase);
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        int nightMode = (prefs != null && prefs.isDarkMode())
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightMode;
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            int nightMode = PreferencesManager.getInstance(this).isDarkMode()
                    ? Configuration.UI_MODE_NIGHT_YES
                    : Configuration.UI_MODE_NIGHT_NO;
            overrideConfiguration.uiMode = (overrideConfiguration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightMode;
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPrefs = PreferencesManager.getInstance(this);
        if (mPrefs.isDarkMode()) {
            setTheme(R.style.Theme_Kotoba_Dark);
        } else {
            setTheme(R.style.Theme_Kotoba);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = KotobaDatabase.getInstance(this);
        mSound = SoundManager.getInstance(this);
        mSpeech = JapaneseSpeechHelper.getInstance(this);

        initViews();
        applyResponsiveLayout();
        setupListeners();
        restoreSavedPreferences();

        if (savedInstanceState != null) {
            mActiveSection = savedInstanceState.getString("saved_active_section", "KOTOBA");
            mActiveKanjiSub = savedInstanceState.getString("saved_kanji_sub", "613");
            String modeStr = savedInstanceState.getString("saved_front_mode", null);
            if (modeStr != null) {
                try {
                    mCurrentFrontMode = FlashcardView.StartSide.valueOf(modeStr);
                } catch (Exception ignored) {}
            }
            mCurrentDeckTitle = savedInstanceState.getString("saved_deck_title", "Bab 1-25 • 863 kata");
            mCurrentIndex = savedInstanceState.getInt("saved_current_index", 0);
            ArrayList<String> savedIds = savedInstanceState.getStringArrayList("saved_deck_ids");
            if (savedIds != null && !savedIds.isEmpty()) {
                mActiveDeck = mDatabase.getLearningObjectsByIds(savedIds);
            }
            if (mActiveDeck == null || mActiveDeck.isEmpty()) {
                rebuildDeck();
            } else {
                LearningCycle.MaterialType matType = "KANJI".equals(mActiveSection)
                        ? LearningCycle.MaterialType.KANJI
                        : LearningCycle.MaterialType.KOTOBA;
                LearningCycle cycle = new LearningCycle(
                        "restored_" + System.currentTimeMillis(),
                        LearningCycle.Source.LIBRARY,
                        matType,
                        mActiveDeck.size(),
                        LearningCycle.DisplayMode.KANJI,
                        "{}",
                        System.currentTimeMillis() / 1000L
                );
                mEngine = new IngatLupaEngine(cycle, mActiveDeck);
                updateSectionVisuals();
                updateFrontModeVisuals();
                updateToolbarVisuals();
                mFlashcardView.resetToFront();
                mBtnFlip.setText(R.string.action_flip);
                bindCurrentCard();
            }
        } else {
            rebuildDeck();
        }
    }

    private void initViews() {
        mTxtTitle = findViewById(R.id.txt_app_title);
        mTxtCardCounter = findViewById(R.id.txt_card_counter);
        mTxtAuthor = findViewById(R.id.txt_app_author);
        mBtnSectionKotoba = findViewById(R.id.btn_section_kotoba);
        mBtnSectionKanji = findViewById(R.id.btn_section_kanji);
        mKanjiSubContainer = findViewById(R.id.kanji_subsection_container);
        mBtnKanjiSub613 = findViewById(R.id.btn_kanji_sub_613);
        mBtnKanjiSubAdditional = findViewById(R.id.btn_kanji_sub_additional);
        mKanjiReadingModeContainer = findViewById(R.id.kanji_reading_mode_container);
        mBtnMainKanjiModeOnyomi = findViewById(R.id.btn_main_kanji_mode_onyomi);
        mBtnMainKanjiModeKunyomi = findViewById(R.id.btn_main_kanji_mode_kunyomi);
        mBtnMainKanjiModeBoth = findViewById(R.id.btn_main_kanji_mode_both);
        mTxtDeckStatus = findViewById(R.id.txt_deck_status);
        mProgressDeck = findViewById(R.id.progress_deck);

        mFlashcardView = findViewById(R.id.flashcard_view);
        mFlashcardView.setSoundManager(mSound);
        mFlashcardView.setSpeechHelper(mSpeech);

        mBtnModeKanji = findViewById(R.id.btn_mode_kanji);
        mBtnModeHiragana = findViewById(R.id.btn_mode_hiragana);
        mBtnModeArti = findViewById(R.id.btn_mode_arti);

        mBtnFuriganaToggle = findViewById(R.id.btn_furigana_toggle);
        mBtnRomajiToggle = findViewById(R.id.btn_romaji_toggle);
        mBtnFilterBab = findViewById(R.id.btn_filter_bab);
        mBtnShuffle = findViewById(R.id.btn_shuffle);
        mBtnThemeToggle = findViewById(R.id.btn_theme_toggle);
        mBtnSoundToggle = findViewById(R.id.btn_sound_toggle);

        mBtnPrev = findViewById(R.id.btn_prev);
        mBtnFlip = findViewById(R.id.btn_flip);
        mBtnNext = findViewById(R.id.btn_next);

        mBtnLupa = findViewById(R.id.btn_lupa);
        mBtnIngat = findViewById(R.id.btn_ingat);
    }

    private void setupListeners() {
        // Section switcher
        mBtnSectionKotoba.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchSection("KOTOBA");
            }
        });
        mBtnSectionKanji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchSection("KANJI");
            }
        });

        // Kanji sub-section switcher
        mBtnKanjiSub613.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchKanjiSubSection("613");
            }
        });
        mBtnKanjiSubAdditional.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchKanjiSubSection("ADDITIONAL");
            }
        });

        // Kanji Reading Mode switcher
        if (mBtnMainKanjiModeOnyomi != null) {
            mBtnMainKanjiModeOnyomi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchKanjiReadingMode("onyomi");
                }
            });
        }
        if (mBtnMainKanjiModeKunyomi != null) {
            mBtnMainKanjiModeKunyomi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchKanjiReadingMode("kunyomi");
                }
            });
        }
        if (mBtnMainKanjiModeBoth != null) {
            mBtnMainKanjiModeBoth.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchKanjiReadingMode("both");
                }
            });
        }

        // Front Mode buttons
        mBtnModeKanji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFrontMode(FlashcardView.StartSide.KANJI);
            }
        });
        mBtnModeHiragana.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFrontMode(FlashcardView.StartSide.HIRAGANA);
            }
        });
        mBtnModeArti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFrontMode(FlashcardView.StartSide.ARTI);
            }
        });

        // Toolbar buttons
        mBtnFuriganaToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean next = !mPrefs.isFuriganaEnabled();
                mPrefs.setFuriganaEnabled(next);
                if (mSound != null) mSound.playToggle();
                updateToolbarVisuals();
                bindCurrentCard();
            }
        });

        mBtnRomajiToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean next = !mPrefs.isRomajiEnabled();
                mPrefs.setRomajiEnabled(next);
                if (mSound != null) mSound.playToggle();
                updateToolbarVisuals();
                bindCurrentCard();
            }
        });

        mBtnFilterBab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLibraryFilterDialog();
            }
        });

        mTxtDeckStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLibraryFilterDialog();
            }
        });

        mBtnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean next = !mPrefs.isShuffleEnabled();
                mPrefs.setShuffleEnabled(next);
                if (mSound != null) mSound.playToggle();
                updateToolbarVisuals();
                rebuildDeck();
            }
        });

        mBtnThemeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean nextDark = !mPrefs.isDarkMode();
                mPrefs.setDarkMode(nextDark);
                if (Build.VERSION.SDK_INT >= 31) {
                    try {
                        Object uiModeManager = getSystemService(Context.UI_MODE_SERVICE);
                        if (uiModeManager != null) {
                            java.lang.reflect.Method method = uiModeManager.getClass().getMethod("setApplicationNightMode", int.class);
                            method.invoke(uiModeManager, nextDark ? 2 : 1);
                        }
                    } catch (Exception ignored) {}
                }
                if (mSound != null) mSound.playToggle();
                recreate();
            }
        });

        mBtnSoundToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean next = !mPrefs.isSoundEnabled();
                mPrefs.setSoundEnabled(next);
                updateToolbarVisuals();
            }
        });

        mBtnSoundToggle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showVoiceSettingsDialog();
                return true;
            }
        });

        // Navigation dock
        mBtnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigatePrev();
            }
        });

        mBtnFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFlashcardView.flipCard();
            }
        });

        mBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleIngatResponse();
            }
        });

        // Ingat / Lupa active recall dock
        mBtnLupa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLupaResponse();
            }
        });

        mBtnIngat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleIngatResponse();
            }
        });

        mFlashcardView.setOnCardFlipListener(new FlashcardView.OnCardFlipListener() {
            @Override
            public void onCardFlip(boolean isShowingBack) {
                mBtnFlip.setText(isShowingBack ? "Tutup" : getString(R.string.action_flip));
            }
        });
    }

    private void restoreSavedPreferences() {
        String modeStr = mPrefs.getFrontMode();
        if ("HIRAGANA".equalsIgnoreCase(modeStr)) {
            mCurrentFrontMode = FlashcardView.StartSide.HIRAGANA;
        } else if ("ARTI".equalsIgnoreCase(modeStr)) {
            mCurrentFrontMode = FlashcardView.StartSide.ARTI;
        } else {
            mCurrentFrontMode = FlashcardView.StartSide.KANJI;
        }
        mKanjiReadingMode = mPrefs.getKanjiReadingMode();
        if (mFlashcardView != null) {
            mFlashcardView.setKanjiReadingMode(mKanjiReadingMode);
        }
        updateFrontModeVisuals();
        updateToolbarVisuals();
        updateKanjiReadingModeVisuals();
    }

    private void switchSection(String section) {
        if (mActiveSection.equals(section)) return;
        mActiveSection = section;
        if (mSound != null) mSound.playDeckFilter();
        updateSectionVisuals();
        rebuildDeck();
    }

    private void updateSectionVisuals() {
        boolean isKotoba = "KOTOBA".equals(mActiveSection);
        mBtnSectionKotoba.setBackgroundResource(isKotoba ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnSectionKotoba.setTextColor(getColor(isKotoba ? R.color.colorOnPrimary : R.color.colorTextSecondary));

        mBtnSectionKanji.setBackgroundResource(!isKotoba ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnSectionKanji.setTextColor(getColor(!isKotoba ? R.color.colorOnPrimary : R.color.colorTextSecondary));

        if (mKanjiSubContainer != null) {
            mKanjiSubContainer.setVisibility(isKotoba ? View.GONE : View.VISIBLE);
            if (!isKotoba) {
                updateKanjiSubVisuals();
            }
        }
        if (mKanjiReadingModeContainer != null) {
            mKanjiReadingModeContainer.setVisibility(isKotoba ? View.GONE : View.VISIBLE);
            if (!isKotoba) {
                updateKanjiReadingModeVisuals();
            }
        }
    }

    private void switchKanjiReadingMode(String mode) {
        if (mode == null || mKanjiReadingMode.equalsIgnoreCase(mode)) return;
        mKanjiReadingMode = mode.toLowerCase();
        mPrefs.setKanjiReadingMode(mKanjiReadingMode);
        if (mSound != null) mSound.playToggle();
        updateKanjiReadingModeVisuals();
        if (mFlashcardView != null) {
            mFlashcardView.setKanjiReadingMode(mKanjiReadingMode);
        }
        bindCurrentCard();
    }

    private void updateKanjiReadingModeVisuals() {
        if (mBtnMainKanjiModeOnyomi == null || mBtnMainKanjiModeKunyomi == null || mBtnMainKanjiModeBoth == null) return;
        boolean isOnyomi = "onyomi".equalsIgnoreCase(mKanjiReadingMode);
        boolean isKunyomi = "kunyomi".equalsIgnoreCase(mKanjiReadingMode);
        boolean isBoth = "both".equalsIgnoreCase(mKanjiReadingMode);

        mBtnMainKanjiModeOnyomi.setBackgroundResource(isOnyomi ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnMainKanjiModeOnyomi.setTextColor(getColor(isOnyomi ? R.color.colorOnPrimary : R.color.colorTextSecondary));

        mBtnMainKanjiModeKunyomi.setBackgroundResource(isKunyomi ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnMainKanjiModeKunyomi.setTextColor(getColor(isKunyomi ? R.color.colorOnPrimary : R.color.colorTextSecondary));

        mBtnMainKanjiModeBoth.setBackgroundResource(isBoth ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnMainKanjiModeBoth.setTextColor(getColor(isBoth ? R.color.colorOnPrimary : R.color.colorTextSecondary));
    }

    private void switchKanjiSubSection(String sub) {
        if (mActiveKanjiSub.equals(sub)) return;
        mActiveKanjiSub = sub;
        if (mSound != null) mSound.playDeckFilter();
        updateKanjiSubVisuals();
        rebuildDeck();
    }

    private void updateKanjiSubVisuals() {
        if (mBtnKanjiSub613 == null || mBtnKanjiSubAdditional == null) return;
        boolean is613 = "613".equals(mActiveKanjiSub);
        mBtnKanjiSub613.setBackgroundResource(is613 ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnKanjiSub613.setTextColor(getColor(is613 ? R.color.colorOnPrimary : R.color.colorTextSecondary));

        mBtnKanjiSubAdditional.setBackgroundResource(!is613 ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnKanjiSubAdditional.setTextColor(getColor(!is613 ? R.color.colorOnPrimary : R.color.colorTextSecondary));
    }

    private void setFrontMode(FlashcardView.StartSide mode) {
        mCurrentFrontMode = mode;
        mPrefs.setFrontMode(mode.name());
        if (mSound != null) mSound.playToggle();
        updateFrontModeVisuals();
        bindCurrentCard();
    }

    private void updateFrontModeVisuals() {
        mBtnModeKanji.setBackgroundResource(mCurrentFrontMode == FlashcardView.StartSide.KANJI ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnModeKanji.setTextColor(getColor(mCurrentFrontMode == FlashcardView.StartSide.KANJI ? R.color.colorOnPrimary : R.color.colorTextSecondary));

        mBtnModeHiragana.setBackgroundResource(mCurrentFrontMode == FlashcardView.StartSide.HIRAGANA ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnModeHiragana.setTextColor(getColor(mCurrentFrontMode == FlashcardView.StartSide.HIRAGANA ? R.color.colorOnPrimary : R.color.colorTextSecondary));

        mBtnModeArti.setBackgroundResource(mCurrentFrontMode == FlashcardView.StartSide.ARTI ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnModeArti.setTextColor(getColor(mCurrentFrontMode == FlashcardView.StartSide.ARTI ? R.color.colorOnPrimary : R.color.colorTextSecondary));
    }

    private void updateToolbarVisuals() {
        mBtnFuriganaToggle.setAlpha(mPrefs.isFuriganaEnabled() ? 1.0f : 0.45f);
        mBtnRomajiToggle.setAlpha(mPrefs.isRomajiEnabled() ? 1.0f : 0.45f);
        mBtnShuffle.setAlpha(mPrefs.isShuffleEnabled() ? 1.0f : 0.45f);
        mBtnThemeToggle.setAlpha(mPrefs.isDarkMode() ? 1.0f : 0.6f);
        mBtnSoundToggle.setImageResource(mPrefs.isSoundEnabled() ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
        mBtnSoundToggle.setAlpha(mPrefs.isSoundEnabled() ? 1.0f : 0.45f);
    }

    private void rebuildDeck() {
        if ("KANJI".equals(mActiveSection)) {
            if ("ADDITIONAL".equals(mActiveKanjiSub)) {
                mActiveDeck = mDatabase.getAdditionalKanji();
                mCurrentDeckTitle = "Kanji Tambahan • " + mActiveDeck.size() + " huruf";
            } else {
                mActiveDeck = mDatabase.getFormalKanji();
                mCurrentDeckTitle = "Kanji JFT & JLPT • 613 huruf";
            }
        } else {
            Set<Integer> selectedBabs = mPrefs.getSelectedBabs();
            if (selectedBabs != null && !selectedBabs.isEmpty() && selectedBabs.size() < 25) {
                mActiveDeck = mDatabase.getVocabularyForBabs(selectedBabs);
                if (selectedBabs.size() == 1) {
                    mCurrentDeckTitle = "Bab " + selectedBabs.iterator().next() + " • " + mActiveDeck.size() + " kata";
                } else if (selectedBabs.size() <= 3) {
                    StringBuilder sb = new StringBuilder("Bab ");
                    int i = 0;
                    for (int b : selectedBabs) {
                        if (i > 0) sb.append(", ");
                        sb.append(b);
                        i++;
                    }
                    sb.append(" • ").append(mActiveDeck.size()).append(" kata");
                    mCurrentDeckTitle = sb.toString();
                } else {
                    mCurrentDeckTitle = selectedBabs.size() + " Bab Terpilih • " + mActiveDeck.size() + " kata";
                }
            } else {
                mActiveDeck = mDatabase.getAllChapterVocabulary();
                mCurrentDeckTitle = "Bab 1-25 • " + mActiveDeck.size() + " kata";
            }
        }

        if (mPrefs.isShuffleEnabled()) {
            Collections.shuffle(mActiveDeck);
        }

        LearningCycle.MaterialType matType = "KANJI".equals(mActiveSection)
                ? LearningCycle.MaterialType.KANJI
                : LearningCycle.MaterialType.KOTOBA;

        LearningCycle cycle = new LearningCycle(
                "main_" + mActiveSection + "_" + System.currentTimeMillis(),
                LearningCycle.Source.CURRICULUM,
                matType,
                mActiveDeck.size(),
                LearningCycle.DisplayMode.KANJI,
                "{}",
                System.currentTimeMillis() / 1000L
        );
        mEngine = new IngatLupaEngine(cycle, mActiveDeck);
        mCurrentIndex = 0;
        mFlashcardView.resetToFront();
        mBtnFlip.setText(R.string.action_flip);
        bindCurrentCard();
    }

    public void loadCustomPool(String title, List<LearningObject> pool) {
        if (pool == null || pool.isEmpty()) return;
        mActiveDeck = new ArrayList<>(pool);
        mCurrentDeckTitle = title;

        if (mPrefs.isShuffleEnabled()) {
            Collections.shuffle(mActiveDeck);
        }

        LearningCycle.MaterialType matType = "KANJI".equals(mActiveSection)
                ? LearningCycle.MaterialType.KANJI
                : LearningCycle.MaterialType.KOTOBA;

        LearningCycle cycle = new LearningCycle(
                "custom_" + System.currentTimeMillis(),
                LearningCycle.Source.LIBRARY,
                matType,
                mActiveDeck.size(),
                LearningCycle.DisplayMode.KANJI,
                "{\"title\":\"" + (title != null ? title.replace("\"", "\\\"") : "") + "\"}",
                System.currentTimeMillis() / 1000L
        );
        mEngine = new IngatLupaEngine(cycle, mActiveDeck);
        mCurrentIndex = 0;
        mFlashcardView.resetToFront();
        mBtnFlip.setText(R.string.action_flip);
        bindCurrentCard();
    }

    private void bindCurrentCard() {
        LearningObject item = (mEngine != null && mEngine.getCurrentItem() != null)
                ? mEngine.getCurrentItem()
                : ((mActiveDeck != null && mCurrentIndex >= 0 && mCurrentIndex < mActiveDeck.size()) ? mActiveDeck.get(mCurrentIndex) : null);

        if (item == null) {
            mTxtCardCounter.setText("0 / 0");
            mTxtDeckStatus.setText("Deck kosong");
            mProgressDeck.setProgress(0);
            mFlashcardView.bind(null, mCurrentFrontMode, mPrefs.isFuriganaEnabled(), mPrefs.isRomajiEnabled());
            return;
        }

        mCardShownTimeMs = System.currentTimeMillis();

        if (mEngine != null && mEngine.isRecoveryRound()) {
            int currentPassNum = mEngine.getCurrentItemIndex() + 1;
            int totalPass = mEngine.getCurrentPassTotal();
            mTxtCardCounter.setText(currentPassNum + " / " + totalPass);
            mTxtDeckStatus.setText("Putaran Pemulihan #" + mEngine.getCycleIteration() + " • " + totalPass + " kata diulang");
            mTxtDeckStatus.setTextColor(getColor(R.color.colorError));
            mProgressDeck.setProgress((int) (((float) currentPassNum / totalPass) * 100));
        } else {
            int current = mCurrentIndex + 1;
            int total = mActiveDeck.size();
            mTxtCardCounter.setText(current + " / " + total);
            mTxtDeckStatus.setText(mCurrentDeckTitle);
            mTxtDeckStatus.setTextColor(getColor(R.color.colorTextSecondary));
            mProgressDeck.setProgress(total > 0 ? (int) (((float) current / total) * 100) : 0);
        }

        mFlashcardView.bind(item, mCurrentFrontMode, mPrefs.isFuriganaEnabled(), mPrefs.isRomajiEnabled(), mKanjiReadingMode);
    }

    private void navigatePrev() {
        if (mEngine == null || mActiveDeck == null || mActiveDeck.isEmpty()) return;
        int prevIndex = mEngine.getCurrentItemIndex() - 1;
        if (prevIndex < 0) return; // already at first card — silent no-op, no toast
        mEngine.setCurrentItemIndex(prevIndex);
        mCurrentIndex = prevIndex;
        mFlashcardView.resetToFront();
        mBtnFlip.setText(R.string.action_flip);
        bindCurrentCard();
        if (mSound != null) mSound.playClickTap();
    }

    private void handleIngatResponse() {
        if (mActiveDeck == null || mActiveDeck.isEmpty() || mEngine == null) return;
        int latency = (int) Math.min(60000, Math.max(100, System.currentTimeMillis() - mCardShownTimeMs));
        if (mSound != null) mSound.playClickTap();

        boolean complete = mEngine.markIngat(latency);
        if (complete) {
            handleCycleCompleted();
        } else {
            mCurrentIndex = mEngine.getCurrentItemIndex();
            mFlashcardView.resetToFront();
            mBtnFlip.setText(R.string.action_flip);
            bindCurrentCard();
        }
    }

    private void handleLupaResponse() {
        if (mActiveDeck == null || mActiveDeck.isEmpty() || mEngine == null) return;
        int latency = (int) Math.min(60000, Math.max(100, System.currentTimeMillis() - mCardShownTimeMs));
        if (mSound != null) mSound.playClickTap();

        boolean complete = mEngine.markLupa(latency);
        if (complete) {
            handleCycleCompleted();
        } else {
            mCurrentIndex = mEngine.getCurrentItemIndex();
            mFlashcardView.resetToFront();
            mBtnFlip.setText(R.string.action_flip);
            bindCurrentCard();
        }
    }

    private void handleCycleCompleted() {
        mTxtDeckStatus.setText(R.string.msg_cycle_complete);
        mTxtDeckStatus.setTextColor(getColor(R.color.colorPrimary));
        Toast.makeText(this, R.string.msg_cycle_complete, Toast.LENGTH_SHORT).show();
        if (mSound != null) mSound.playToggle();

        mTxtTitle.postDelayed(new Runnable() {
            @Override
            public void run() {
                rebuildDeck();
            }
        }, 1600);
    }

    private void showLibraryFilterDialog() {
        LibraryFilterDialog dialog = new LibraryFilterDialog(
                this,
                mDatabase,
                mSound,
                mSpeech,
                "KANJI".equals(mActiveSection),
                mActiveKanjiSub,
                new LibraryFilterDialog.OnLibraryFilterAppliedListener() {
                    @Override
                    public void onFilterApplied(String title, List<LearningObject> pool) {
                        loadCustomPool(title, pool);
                    }
                }
        );
        dialog.setOnKanjiReadingModeChangedListener(new LibraryFilterDialog.OnKanjiReadingModeChangedListener() {
            @Override
            public void onReadingModeChanged(String newMode) {
                mKanjiReadingMode = newMode;
                if (mFlashcardView != null) {
                    mFlashcardView.setKanjiReadingMode(newMode);
                }
                updateKanjiReadingModeVisuals();
                bindCurrentCard();
            }
        });
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String savedMode = mPrefs.getKanjiReadingMode();
        if (savedMode != null && !savedMode.equalsIgnoreCase(mKanjiReadingMode)) {
            mKanjiReadingMode = savedMode;
            if (mFlashcardView != null) {
                mFlashcardView.setKanjiReadingMode(mKanjiReadingMode);
            }
            updateKanjiReadingModeVisuals();
            bindCurrentCard();
        }
    }

    private void showVoiceSettingsDialog() {
        VoiceSettingsDialog dialog = new VoiceSettingsDialog(this);
        dialog.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("saved_active_section", mActiveSection);
        outState.putString("saved_kanji_sub", mActiveKanjiSub);
        if (mCurrentFrontMode != null) {
            outState.putString("saved_front_mode", mCurrentFrontMode.name());
        }
        outState.putString("saved_deck_title", mCurrentDeckTitle);
        outState.putInt("saved_current_index", mCurrentIndex);
        if (mActiveDeck != null && !mActiveDeck.isEmpty()) {
            ArrayList<String> ids = new ArrayList<>(mActiveDeck.size());
            for (LearningObject obj : mActiveDeck) {
                if (obj != null && obj.getId() != null) {
                    ids.add(obj.getId());
                }
            }
            outState.putStringArrayList("saved_deck_ids", ids);
        }
    }

    private void applyResponsiveLayout() {
        ResponsiveLayoutSystem rls = ResponsiveLayoutSystem.from(this);

        // Scale typography for headers & buttons
        rls.applyToTextView(mTxtTitle, rls.getAppTitleSizeSp());
        rls.applyToTextView(mTxtCardCounter, rls.getCounterSizeSp());
        rls.applyToTextView(mBtnSectionKotoba, rls.getSegmentedTextSizeSp());
        rls.applyToTextView(mBtnSectionKanji, rls.getSegmentedTextSizeSp());
        rls.applyToTextView(mBtnKanjiSub613, rls.getSegmentedTextSizeSp());
        rls.applyToTextView(mBtnKanjiSubAdditional, rls.getSegmentedTextSizeSp());
        rls.applyToTextView(mBtnModeKanji, rls.getSegmentedTextSizeSp());
        rls.applyToTextView(mBtnModeHiragana, rls.getSegmentedTextSizeSp());
        rls.applyToTextView(mBtnModeArti, rls.getSegmentedTextSizeSp());
        rls.applyToTextView(mBtnPrev, rls.getNavButtonTextSizeSp());
        rls.applyToTextView(mBtnFlip, rls.getNavButtonTextSizeSp() + 1.0f);
        rls.applyToTextView(mBtnNext, rls.getNavButtonTextSizeSp());

        // Dynamic Vertical Compaction for constrained heights (< 680dp)
        View sectionSwitcher = findViewById(R.id.section_switcher_container);
        rls.setViewHeight(sectionSwitcher, rls.getSectionSwitcherHeightPx());

        if (mKanjiSubContainer != null) {
            rls.setViewHeight(mKanjiSubContainer, rls.getKanjiSubSwitcherHeightPx());
        }

        View segmentedMode = findViewById(R.id.segmented_mode_container);
        rls.setViewHeight(segmentedMode, rls.getModeBarHeightPx());
        rls.setViewMarginBottom(segmentedMode, rls.getDockMarginBottomPx());

        View bottomToolbar = findViewById(R.id.bottom_toolbar);
        rls.setViewHeight(bottomToolbar, rls.getToolbarHeightPx());
        rls.setViewMarginBottom(bottomToolbar, rls.getDockMarginBottomPx());

        View navDock = findViewById(R.id.bottom_nav_dock);
        rls.setViewMarginBottom(navDock, rls.getDockMarginBottomPx());
        rls.setViewHeight(mBtnPrev, rls.getNavButtonHeightPx());
        rls.setViewHeight(mBtnFlip, rls.getNavButtonHeightPx());
        rls.setViewHeight(mBtnNext, rls.getNavButtonHeightPx());

        View recallDock = findViewById(R.id.layout_ingat_lupa_dock);
        rls.setViewHeight(recallDock, rls.getRecallButtonHeightPx());

        // Notify flashcard view
        if (mFlashcardView != null) {
            mFlashcardView.applyResponsiveTokens();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyResponsiveLayout();
    }

    @Override
    protected void onDestroy() {
        if (mSpeech != null) {
            mSpeech.shutdown();
        }
        super.onDestroy();
    }
}
