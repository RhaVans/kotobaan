package com.kotoba.app.ui.library;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kotoba.app.R;
import com.kotoba.app.audio.JapaneseSpeechHelper;
import com.kotoba.app.audio.SoundManager;
import com.kotoba.app.data.KotobaDatabase;
import com.kotoba.app.data.PreferencesManager;
import com.kotoba.app.data.model.LearningObject;
import com.kotoba.app.ui.responsive.ResponsiveLayoutSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class LibraryFilterDialog extends Dialog {

    public interface OnLibraryFilterAppliedListener {
        void onFilterApplied(String title, List<LearningObject> pool);
    }

    public interface OnKanjiReadingModeChangedListener {
        void onReadingModeChanged(String newMode);
    }

    private final KotobaDatabase mDatabase;
    private final SoundManager mSound;
    private final JapaneseSpeechHelper mSpeech;
    private final boolean mIsKanjiMode;
    private final OnLibraryFilterAppliedListener mListener;
    private OnKanjiReadingModeChangedListener mReadingModeListener;

    public void setOnKanjiReadingModeChangedListener(OnKanjiReadingModeChangedListener listener) {
        this.mReadingModeListener = listener;
    }

    private TextView mTxtTitle;
    private ImageButton mBtnVoiceSettings;
    private ImageButton mBtnClose;
    private TextView mTxtSummary;
    private EditText mSearchInput;

    private View mScrollTypeChips;
    private Button mChipSemua;
    private Button mChipNoun;
    private Button mChipVerb;
    private Button mChipAdj;
    private Button mChipAdjI;
    private Button mChipAdjNa;
    private Button mChipAdverb;
    private Button mChipConjunction;
    private Button mChipParticle;
    private Button mChipExpr;
    private Button mChipOther;
    private Button mChipWeak;

    private View mContainerKanjiSubChips;
    private Button mChipKanjiSub613;
    private Button mChipKanjiSubAdditional;
    private String mSelectedKanjiSub = "613"; // "613" or "ADDITIONAL"

    private View mContainerKanjiReadingMode;
    private Button mBtnKanjiModeOnyomi;
    private Button mBtnKanjiModeKunyomi;
    private Button mBtnKanjiModeBoth;
    private String mKanjiReadingMode = "BOTH"; // "ONYOMI", "KUNYOMI", "BOTH"

    private LinearLayout mContainerBabChips;
    private ListView mListVocab;
    private LinearLayout mLayoutEmptyState;
    private Button mBtnApply;

    private String mSelectedWordType = "SEMUA";
    private final Set<Integer> mSelectedBabs = new TreeSet<>(); // empty means all babs
    private int mSelectedBatchIndex = 0; // 0 = all items, 1 = 01-50, 2 = 51-100, etc.
    private String mSelectedKanjiGroup = "SEMUA";
    private boolean mWeakOnly = false;
    private String mSearchQuery = "";

    private final List<LearningObject> mCurrentResults = new ArrayList<>();
    private LibraryVocabAdapter mAdapter;
    private final List<Button> mBabChipButtons = new ArrayList<>();

    public LibraryFilterDialog(Context context, KotobaDatabase db, SoundManager sound,
                               JapaneseSpeechHelper speech, boolean isKanjiMode,
                               String initialKanjiSub,
                               OnLibraryFilterAppliedListener listener) {
        super(context, PreferencesManager.getInstance(context).isDarkMode() ? R.style.Theme_Kotoba_Dark : R.style.Theme_Kotoba);
        this.mDatabase = db;
        this.mSound = sound;
        this.mSpeech = speech;
        this.mIsKanjiMode = isKanjiMode;
        this.mSelectedKanjiSub = (initialKanjiSub != null && !initialKanjiSub.isEmpty()) ? initialKanjiSub : "613";
        this.mListener = listener;
    }

    public LibraryFilterDialog(Context context, KotobaDatabase db, SoundManager sound,
                               JapaneseSpeechHelper speech, boolean isKanjiMode,
                               OnLibraryFilterAppliedListener listener) {
        this(context, db, sound, speech, isKanjiMode, "613", listener);
    }

    public LibraryFilterDialog(Context context, KotobaDatabase db, SoundManager sound,
                               boolean isKanjiMode, OnLibraryFilterAppliedListener listener) {
        this(context, db, sound, null, isKanjiMode, "613", listener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_library_filter);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(getWindow().getAttributes());
            lp.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.94);
            getWindow().setAttributes(lp);
        }

        // Restore saved Babs from preferences if in Kotoba mode
        if (!mIsKanjiMode) {
            Set<Integer> savedBabs = PreferencesManager.getInstance(getContext()).getSelectedBabs();
            if (savedBabs != null && !savedBabs.isEmpty() && savedBabs.size() < 25) {
                mSelectedBabs.addAll(savedBabs);
            }
        }

        mKanjiReadingMode = PreferencesManager.getInstance(getContext()).getKanjiReadingMode();

        initViews();
        setupListeners();
        populateSubChips();
        updateWordTypeChipsVisuals();
        updateSubChipsVisuals();
        refreshResults();
    }

    private void initViews() {
        mTxtTitle = findViewById(R.id.txt_dialog_title);
        mBtnVoiceSettings = findViewById(R.id.btn_dialog_voice_settings);
        mBtnClose = findViewById(R.id.btn_dialog_close);
        mTxtSummary = findViewById(R.id.txt_dialog_summary);
        mSearchInput = findViewById(R.id.dialog_search_input);

        mScrollTypeChips = findViewById(R.id.scroll_type_chips);
        mChipSemua = findViewById(R.id.chip_type_semua);
        mChipNoun = findViewById(R.id.chip_type_noun);
        mChipVerb = findViewById(R.id.chip_type_verb);
        mChipAdj = findViewById(R.id.chip_type_adj);
        mChipAdjI = findViewById(R.id.chip_type_adj_i);
        mChipAdjNa = findViewById(R.id.chip_type_adj_na);
        mChipAdverb = findViewById(R.id.chip_type_adverb);
        mChipConjunction = findViewById(R.id.chip_type_conjunction);
        mChipParticle = findViewById(R.id.chip_type_particle);
        mChipExpr = findViewById(R.id.chip_type_expr);
        mChipOther = findViewById(R.id.chip_type_other);
        mChipWeak = findViewById(R.id.chip_filter_weak);

        mContainerKanjiSubChips = findViewById(R.id.container_kanji_sub_chips);
        mChipKanjiSub613 = findViewById(R.id.chip_kanji_sub_613);
        mChipKanjiSubAdditional = findViewById(R.id.chip_kanji_sub_additional);

        mContainerKanjiReadingMode = findViewById(R.id.container_kanji_reading_mode);
        mBtnKanjiModeOnyomi = findViewById(R.id.btn_kanji_mode_onyomi);
        mBtnKanjiModeKunyomi = findViewById(R.id.btn_kanji_mode_kunyomi);
        mBtnKanjiModeBoth = findViewById(R.id.btn_kanji_mode_both);

        mContainerBabChips = findViewById(R.id.container_bab_chips);
        mListVocab = findViewById(R.id.list_library_vocab);
        mLayoutEmptyState = findViewById(R.id.layout_empty_state);
        mBtnApply = findViewById(R.id.btn_dialog_apply);

        mAdapter = new LibraryVocabAdapter();
        mListVocab.setAdapter(mAdapter);

        ResponsiveLayoutSystem rls = ResponsiveLayoutSystem.from(getContext());
        View frameContainer = (View) mListVocab.getParent();
        if (frameContainer != null) {
            int listH = rls.isExtraCompactHeight() ? rls.dpToPx(150) : (rls.isCompactHeight() ? rls.dpToPx(180) : rls.dpToPx(260));
            ViewGroup.LayoutParams lp = frameContainer.getLayoutParams();
            if (lp != null) {
                lp.height = listH;
                frameContainer.setLayoutParams(lp);
            }
        }

        if (mIsKanjiMode) {
            mTxtTitle.setText(R.string.title_select_kanji_group);
            mScrollTypeChips.setVisibility(View.GONE);
            if (mContainerKanjiSubChips != null) {
                mContainerKanjiSubChips.setVisibility(View.VISIBLE);
                updateKanjiSubChipsVisuals();
            }
            if (mContainerKanjiReadingMode != null) {
                mContainerKanjiReadingMode.setVisibility(View.VISIBLE);
                updateKanjiReadingModeVisuals();
            }
        } else {
            mTxtTitle.setText(R.string.title_library);
            mScrollTypeChips.setVisibility(View.VISIBLE);
            if (mContainerKanjiSubChips != null) {
                mContainerKanjiSubChips.setVisibility(View.GONE);
            }
            if (mContainerKanjiReadingMode != null) {
                mContainerKanjiReadingMode.setVisibility(View.GONE);
            }
        }
    }

    private void setupListeners() {
        if (mBtnVoiceSettings != null) {
            mBtnVoiceSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSound != null) mSound.playClickTap();
                    new com.kotoba.app.ui.voice.VoiceSettingsDialog(getContext()).show();
                }
            });
        }

        mBtnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSound != null) mSound.playClickTap();
                dismiss();
            }
        });

        if (mChipKanjiSub613 != null) {
            mChipKanjiSub613.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectedKanjiSub = "613";
                    mSelectedKanjiGroup = "SEMUA";
                    if (mSound != null) mSound.playToggle();
                    updateKanjiSubChipsVisuals();
                    populateSubChips();
                    updateSubChipsVisuals();
                    refreshResults();
                }
            });
        }
        if (mChipKanjiSubAdditional != null) {
            mChipKanjiSubAdditional.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectedKanjiSub = "ADDITIONAL";
                    mSelectedKanjiGroup = "SEMUA";
                    if (mSound != null) mSound.playToggle();
                    updateKanjiSubChipsVisuals();
                    populateSubChips();
                    updateSubChipsVisuals();
                    refreshResults();
                }
            });
        }

        if (mBtnKanjiModeOnyomi != null) {
            mBtnKanjiModeOnyomi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mKanjiReadingMode = "onyomi";
                    PreferencesManager.getInstance(getContext()).setKanjiReadingMode(mKanjiReadingMode);
                    if (mReadingModeListener != null) {
                        mReadingModeListener.onReadingModeChanged(mKanjiReadingMode);
                    }
                    if (mSound != null) mSound.playToggle();
                    updateKanjiReadingModeVisuals();
                    if (mAdapter != null) mAdapter.notifyDataSetChanged();
                }
            });
        }
        if (mBtnKanjiModeKunyomi != null) {
            mBtnKanjiModeKunyomi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mKanjiReadingMode = "kunyomi";
                    PreferencesManager.getInstance(getContext()).setKanjiReadingMode(mKanjiReadingMode);
                    if (mReadingModeListener != null) {
                        mReadingModeListener.onReadingModeChanged(mKanjiReadingMode);
                    }
                    if (mSound != null) mSound.playToggle();
                    updateKanjiReadingModeVisuals();
                    if (mAdapter != null) mAdapter.notifyDataSetChanged();
                }
            });
        }
        if (mBtnKanjiModeBoth != null) {
            mBtnKanjiModeBoth.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mKanjiReadingMode = "both";
                    PreferencesManager.getInstance(getContext()).setKanjiReadingMode(mKanjiReadingMode);
                    if (mReadingModeListener != null) {
                        mReadingModeListener.onReadingModeChanged(mKanjiReadingMode);
                    }
                    if (mSound != null) mSound.playToggle();
                    updateKanjiReadingModeVisuals();
                    if (mAdapter != null) mAdapter.notifyDataSetChanged();
                }
            });
        }

        mSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearchQuery = s.toString();
                refreshResults();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupChipListener(mChipSemua, "SEMUA");
        setupChipListener(mChipNoun, "KATA_BENDA");
        setupChipListener(mChipVerb, "KATA_KERJA");
        setupChipListener(mChipAdj, "KATA_SIFAT");
        setupChipListener(mChipAdjI, "KATA_SIFAT_I");
        setupChipListener(mChipAdjNa, "KATA_SIFAT_NA");
        setupChipListener(mChipAdverb, "KATA_KETERANGAN");
        setupChipListener(mChipConjunction, "KATA_SAMBUNG");
        setupChipListener(mChipParticle, "PARTIKEL");
        setupChipListener(mChipExpr, "UNGKAPAN");
        setupChipListener(mChipOther, "LAINNYA");

        mChipWeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWeakOnly = !mWeakOnly;
                if (mSound != null) mSound.playToggle();
                updateWordTypeChipsVisuals();
                refreshResults();
            }
        });

        mBtnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyAndDismiss();
            }
        });
    }

    private void updateKanjiSubChipsVisuals() {
        if (mChipKanjiSub613 == null || mChipKanjiSubAdditional == null) return;
        boolean is613 = "613".equals(mSelectedKanjiSub);
        mChipKanjiSub613.setBackgroundResource(is613 ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mChipKanjiSub613.setTextColor(getContext().getColor(is613 ? R.color.colorOnPrimary : R.color.colorTextSecondary));

        mChipKanjiSubAdditional.setBackgroundResource(!is613 ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mChipKanjiSubAdditional.setTextColor(getContext().getColor(!is613 ? R.color.colorOnPrimary : R.color.colorTextSecondary));
    }

    private void updateKanjiReadingModeVisuals() {
        if (mBtnKanjiModeOnyomi == null || mBtnKanjiModeKunyomi == null || mBtnKanjiModeBoth == null) return;
        boolean isOnyomi = "onyomi".equalsIgnoreCase(mKanjiReadingMode);
        boolean isKunyomi = "kunyomi".equalsIgnoreCase(mKanjiReadingMode);
        boolean isBoth = "both".equalsIgnoreCase(mKanjiReadingMode);

        mBtnKanjiModeOnyomi.setBackgroundResource(isOnyomi ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnKanjiModeOnyomi.setTextColor(getContext().getColor(isOnyomi ? R.color.colorOnPrimary : R.color.colorTextSecondary));

        mBtnKanjiModeKunyomi.setBackgroundResource(isKunyomi ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnKanjiModeKunyomi.setTextColor(getContext().getColor(isKunyomi ? R.color.colorOnPrimary : R.color.colorTextSecondary));

        mBtnKanjiModeBoth.setBackgroundResource(isBoth ? R.drawable.bg_segmented_active : android.R.color.transparent);
        mBtnKanjiModeBoth.setTextColor(getContext().getColor(isBoth ? R.color.colorOnPrimary : R.color.colorTextSecondary));
    }

    private static String joinStrings(List<String> list, String delimiter) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private void setupChipListener(Button chip, final String type) {
        if (chip == null) return;
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!type.equals(mSelectedWordType)) {
                    mSelectedWordType = type;
                    mSelectedBatchIndex = 0; // reset to "Semua" for this word type
                    if (mSound != null) mSound.playToggle();
                    updateWordTypeChipsVisuals();
                    populateSubChips();
                    refreshResults();
                }
            }
        });
    }

    private void populateSubChips() {
        mContainerBabChips.removeAllViews();
        mBabChipButtons.clear();
        Context ctx = getContext();

        if (mIsKanjiMode) {
            boolean isAdditional = "ADDITIONAL".equalsIgnoreCase(mSelectedKanjiSub);
            String allText = isAdditional ? "Semua Tambahan" : ctx.getString(R.string.label_group_all);
            Button btnAll = createChipButton(allText);
            btnAll.setTag("SEMUA");
            btnAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectedKanjiGroup = "SEMUA";
                    if (mSound != null) mSound.playToggle();
                    updateSubChipsVisuals();
                    refreshResults();
                }
            });
            mContainerBabChips.addView(btnAll);
            mBabChipButtons.add(btnAll);

            List<String> groups = isAdditional ? mDatabase.getAdditionalKanjiGroups() : mDatabase.getFormalKanjiGroups();
            if (groups != null) {
                for (final String groupKey : groups) {
                    if (groupKey == null) continue;
                    Button btn = createChipButton(groupKey);
                    btn.setTag(groupKey);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mSelectedKanjiGroup = groupKey;
                            if (mSound != null) mSound.playToggle();
                            updateSubChipsVisuals();
                            refreshResults();
                        }
                    });
                    mContainerBabChips.addView(btn);
                    mBabChipButtons.add(btn);
                }
            }
        } else if ("SEMUA".equals(mSelectedWordType)) {
            // Kotoba Bab Mode: Multi-selectable Bab chips
            Button btnAll = createChipButton(ctx.getString(R.string.label_bab_all));
            btnAll.setTag(0);
            btnAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectedBabs.clear();
                    if (mSound != null) mSound.playToggle();
                    updateSubChipsVisuals();
                    refreshResults();
                }
            });
            mContainerBabChips.addView(btnAll);
            mBabChipButtons.add(btnAll);

            // Bab 1 to Bab 25 chips
            for (int b = 1; b <= 25; b++) {
                final int babNum = b;
                Button btn = createChipButton(String.format(Locale.US, "Bab %d", babNum));
                btn.setTag(babNum);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mSelectedBabs.contains(babNum)) {
                            mSelectedBabs.remove(babNum);
                        } else {
                            mSelectedBabs.add(babNum);
                        }
                        if (mSound != null) mSound.playToggle();
                        updateSubChipsVisuals();
                        refreshResults();
                    }
                });
                mContainerBabChips.addView(btn);
                mBabChipButtons.add(btn);
            }
        } else {
            // Word-Type Mode: 50-kotoba batches + option to memorize all at once
            int totalCount = mDatabase.getWordTypeCount(mSelectedWordType);
            String labelAll;
            if ("KATA_KERJA".equalsIgnoreCase(mSelectedWordType)) {
                labelAll = "Semua Kata Kerja (" + totalCount + ")";
            } else if ("KATA_SIFAT".equalsIgnoreCase(mSelectedWordType)) {
                labelAll = "Semua Kata Sifat (" + totalCount + ")";
            } else if ("KATA_SIFAT_I".equalsIgnoreCase(mSelectedWordType)) {
                labelAll = "Semua Sifat-い (" + totalCount + ")";
            } else if ("KATA_SIFAT_NA".equalsIgnoreCase(mSelectedWordType)) {
                labelAll = "Semua Sifat-な (" + totalCount + ")";
            } else if ("KATA_BENDA".equalsIgnoreCase(mSelectedWordType)) {
                labelAll = "Semua Kata Benda (" + totalCount + ")";
            } else {
                labelAll = "Semua (" + totalCount + ")";
            }

            Button btnAll = createChipButton(labelAll);
            btnAll.setTag(0);
            btnAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSelectedBatchIndex = 0;
                    if (mSound != null) mSound.playToggle();
                    updateSubChipsVisuals();
                    refreshResults();
                }
            });
            mContainerBabChips.addView(btnAll);
            mBabChipButtons.add(btnAll);

            int batchCount = (totalCount + 49) / 50;
            for (int i = 1; i <= batchCount; i++) {
                final int bIdx = i;
                int start = (i - 1) * 50 + 1;
                int end = Math.min(i * 50, totalCount);
                String batchLabel = String.format(Locale.US, "%02d–%02d", start, end);
                Button btn = createChipButton(batchLabel);
                btn.setTag(bIdx);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSelectedBatchIndex = bIdx;
                        if (mSound != null) mSound.playToggle();
                        updateSubChipsVisuals();
                        refreshResults();
                    }
                });
                mContainerBabChips.addView(btn);
                mBabChipButtons.add(btn);
            }
        }
        updateSubChipsVisuals();
    }

    private Button createChipButton(String text) {
        Button btn = new Button(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (int) (32 * getContext().getResources().getDisplayMetrics().density)
        );
        lp.setMarginEnd((int) (6 * getContext().getResources().getDisplayMetrics().density));
        btn.setLayoutParams(lp);
        btn.setText(text);
        btn.setTextSize(12);
        btn.setAllCaps(false);
        int padH = (int) (12 * getContext().getResources().getDisplayMetrics().density);
        btn.setPadding(padH, 0, padH, 0);
        return btn;
    }

    private void updateWordTypeChipsVisuals() {
        setChipStyle(mChipSemua, "SEMUA".equals(mSelectedWordType));
        setChipStyle(mChipNoun, "KATA_BENDA".equals(mSelectedWordType));
        setChipStyle(mChipVerb, "KATA_KERJA".equals(mSelectedWordType));
        setChipStyle(mChipAdj, "KATA_SIFAT".equals(mSelectedWordType));
        setChipStyle(mChipAdjI, "KATA_SIFAT_I".equals(mSelectedWordType));
        setChipStyle(mChipAdjNa, "KATA_SIFAT_NA".equals(mSelectedWordType));
        setChipStyle(mChipAdverb, "KATA_KETERANGAN".equals(mSelectedWordType));
        setChipStyle(mChipConjunction, "KATA_SAMBUNG".equals(mSelectedWordType));
        setChipStyle(mChipParticle, "PARTIKEL".equals(mSelectedWordType));
        setChipStyle(mChipExpr, "UNGKAPAN".equals(mSelectedWordType));
        setChipStyle(mChipOther, "LAINNYA".equals(mSelectedWordType));

        if (mWeakOnly) {
            mChipWeak.setBackgroundResource(R.drawable.bg_segmented_active);
            mChipWeak.setTextColor(getContext().getColor(R.color.colorOnPrimary));
        } else {
            mChipWeak.setBackgroundResource(R.drawable.bg_button_secondary);
            mChipWeak.setTextColor(getContext().getColor(R.color.colorError));
        }
    }

    private void updateSubChipsVisuals() {
        if (mIsKanjiMode) {
            for (Button btn : mBabChipButtons) {
                Object tag = btn.getTag();
                if (tag instanceof String) {
                    setChipStyle(btn, tag.equals(mSelectedKanjiGroup));
                }
            }
        } else if ("SEMUA".equals(mSelectedWordType)) {
            for (Button btn : mBabChipButtons) {
                Object tag = btn.getTag();
                if (tag instanceof Integer) {
                    int bab = (Integer) tag;
                    if (bab == 0) {
                        setChipStyle(btn, mSelectedBabs.isEmpty());
                    } else {
                        setChipStyle(btn, mSelectedBabs.contains(bab));
                    }
                }
            }
        } else {
            for (Button btn : mBabChipButtons) {
                Object tag = btn.getTag();
                if (tag instanceof Integer) {
                    setChipStyle(btn, mSelectedBatchIndex == (Integer) tag);
                }
            }
        }
    }

    private void setChipStyle(Button chip, boolean active) {
        if (chip == null) return;
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_segmented_active);
            chip.setTextColor(getContext().getColor(R.color.colorOnPrimary));
        } else {
            chip.setBackgroundResource(R.drawable.bg_button_secondary);
            chip.setTextColor(getContext().getColor(R.color.colorTextSecondary));
        }
    }

    private void refreshResults() {
        mCurrentResults.clear();
        if (mIsKanjiMode) {
            List<LearningObject> items = mDatabase.queryKanji(mSelectedKanjiSub, mSelectedKanjiGroup, mSearchQuery);
            if (items != null) {
                mCurrentResults.addAll(items);
            }
        } else {
            Integer bIndex = "SEMUA".equals(mSelectedWordType) ? null : (mSelectedBatchIndex > 0 ? mSelectedBatchIndex : null);
            Collection<Integer> babs = "SEMUA".equals(mSelectedWordType) ? (mSelectedBabs.isEmpty() ? null : mSelectedBabs) : null;
            List<LearningObject> items = mDatabase.queryVocabulary(
                    mSelectedWordType,
                    babs,
                    bIndex,
                    50,
                    mSearchQuery,
                    mWeakOnly
            );
            if (items != null) {
                mCurrentResults.addAll(items);
            }
        }

        mAdapter.notifyDataSetChanged();
        int count = mCurrentResults.size();

        if (count == 0) {
            mListVocab.setVisibility(View.GONE);
            mLayoutEmptyState.setVisibility(View.VISIBLE);
            mBtnApply.setEnabled(false);
        } else {
            mListVocab.setVisibility(View.VISIBLE);
            mLayoutEmptyState.setVisibility(View.GONE);
            mBtnApply.setEnabled(true);
        }

        if (mIsKanjiMode) {
            mTxtSummary.setText(String.format(Locale.US, "%d kanji ditemukan", count));
            mBtnApply.setText(String.format(Locale.US, "Pelajari di Flashcard (%d Kanji)", count));
        } else {
            mTxtSummary.setText(String.format(Locale.US, "%d kata ditemukan", count));
            mBtnApply.setText(String.format(Locale.US, "Pelajari di Flashcard (%d Kata)", count));
        }
    }

    private void applyAndDismiss() {
        if (mCurrentResults.isEmpty()) {
            Toast.makeText(getContext(), R.string.msg_empty_pustaka_title, Toast.LENGTH_SHORT).show();
            return;
        }
        if (mSound != null) mSound.playDeckFilter();

        String title;
        if (mIsKanjiMode) {
            boolean isAdditional = "ADDITIONAL".equalsIgnoreCase(mSelectedKanjiSub);
            if ("SEMUA".equals(mSelectedKanjiGroup)) {
                if (isAdditional) {
                    title = String.format(Locale.US, "Kanji Tambahan • %d huruf", mCurrentResults.size());
                } else {
                    title = String.format(Locale.US, "Kanji JFT & JLPT • %d huruf", mCurrentResults.size());
                }
            } else {
                title = String.format(Locale.US, "Kanji %s • %d huruf", mSelectedKanjiGroup, mCurrentResults.size());
            }
        } else if ("SEMUA".equals(mSelectedWordType)) {
            PreferencesManager.getInstance(getContext()).setSelectedBabs(mSelectedBabs);
            if (mSelectedBabs.isEmpty()) {
                title = String.format(Locale.US, "Bab 1-25 • %d kata", mCurrentResults.size());
            } else if (mSelectedBabs.size() == 1) {
                int b = mSelectedBabs.iterator().next();
                title = String.format(Locale.US, "Bab %d • %d kata", b, mCurrentResults.size());
            } else if (mSelectedBabs.size() <= 3) {
                StringBuilder sb = new StringBuilder("Bab ");
                int i = 0;
                for (int b : mSelectedBabs) {
                    if (i > 0) sb.append(", ");
                    sb.append(b);
                    i++;
                }
                sb.append(" • ").append(mCurrentResults.size()).append(" kata");
                title = sb.toString();
            } else {
                title = String.format(Locale.US, "%d Bab Terpilih • %d kata", mSelectedBabs.size(), mCurrentResults.size());
            }
        } else {
            String typeName;
            if ("KATA_KERJA".equalsIgnoreCase(mSelectedWordType)) {
                typeName = "Kata Kerja";
            } else if ("KATA_SIFAT".equalsIgnoreCase(mSelectedWordType)) {
                typeName = "Kata Sifat";
            } else if ("KATA_SIFAT_I".equalsIgnoreCase(mSelectedWordType)) {
                typeName = "Kata Sifat - い";
            } else if ("KATA_SIFAT_NA".equalsIgnoreCase(mSelectedWordType)) {
                typeName = "Kata Sifat - な";
            } else if ("KATA_BENDA".equalsIgnoreCase(mSelectedWordType)) {
                typeName = "Kata Benda";
            } else {
                typeName = mSelectedWordType.replace('_', ' ');
            }

            if (mSelectedBatchIndex > 0) {
                int start = (mSelectedBatchIndex - 1) * 50 + 1;
                int end = start + mCurrentResults.size() - 1;
                title = String.format(Locale.US, "%s • %02d–%02d (%d kata)", typeName, start, end, mCurrentResults.size());
            } else {
                title = String.format(Locale.US, "%s • Semua (%d kata)", typeName, mCurrentResults.size());
            }
        }

        if (mListener != null) {
            mListener.onFilterApplied(title, mCurrentResults);
        }
        dismiss();
    }

    private class LibraryVocabAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mCurrentResults.size();
        }

        @Override
        public LearningObject getItem(int position) {
            return mCurrentResults.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_library_vocab, parent, false);
                holder = new ViewHolder();
                holder.layoutBadgesRow = convertView.findViewById(R.id.layout_vocab_badges_row);
                holder.txtJapanese = convertView.findViewById(R.id.txt_vocab_japanese);
                holder.txtReading = convertView.findViewById(R.id.txt_vocab_reading);
                holder.txtRomaji = convertView.findViewById(R.id.txt_vocab_romaji);
                holder.txtMeaning = convertView.findViewById(R.id.txt_vocab_meaning);
                holder.txtBadge = convertView.findViewById(R.id.txt_vocab_badge);
                holder.txtTypeBadge = convertView.findViewById(R.id.txt_vocab_type_badge);
                holder.btnSpeak = convertView.findViewById(R.id.btn_vocab_speak);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final LearningObject item = getItem(position);
            holder.txtJapanese.setText(item.getJapanese() != null ? item.getJapanese() : "");

            if (mIsKanjiMode || item.getType() == LearningObject.Type.KANJI) {
                String displayReading;
                String displayRomaji;
                if ("onyomi".equalsIgnoreCase(mKanjiReadingMode)) {
                    displayReading = item.getOnyomiDisplay();
                    List<String> rList = item.getOnyomiRomajiList();
                    displayRomaji = (rList != null && !rList.isEmpty()) ? joinStrings(rList, ", ") : "";
                } else if ("kunyomi".equalsIgnoreCase(mKanjiReadingMode)) {
                    displayReading = item.getKunyomiDisplay();
                    List<String> rList = item.getKunyomiRomajiList();
                    displayRomaji = (rList != null && !rList.isEmpty()) ? joinStrings(rList, ", ") : "";
                } else {
                    displayReading = item.getDualReadingDisplay();
                    displayRomaji = item.getRomaji();
                }

                if (displayReading != null && !displayReading.trim().isEmpty() && !displayReading.trim().equals("—")) {
                    holder.txtReading.setVisibility(View.VISIBLE);
                    holder.txtReading.setText(displayReading.trim());
                } else {
                    holder.txtReading.setVisibility(View.GONE);
                }

                if (displayRomaji != null && !displayRomaji.trim().isEmpty() && !displayRomaji.trim().equals("—")) {
                    holder.txtRomaji.setVisibility(View.VISIBLE);
                    holder.txtRomaji.setText(displayRomaji.trim());
                } else {
                    holder.txtRomaji.setVisibility(View.GONE);
                }
            } else {
                // Hide reading if identical to Japanese (e.g. Katakana/Hiragana words like ハンサム (な) or きれい (な))
                // Only show reading when Kanji has a distinct reading (e.g. 親切 (な) -> しんせつ (な))
                String reading = item.getReading();
                if (reading != null && !reading.trim().isEmpty() && !reading.trim().equals("—")
                        && !reading.trim().equals(item.getJapanese() != null ? item.getJapanese().trim() : "")) {
                    holder.txtReading.setVisibility(View.VISIBLE);
                    holder.txtReading.setText(reading.trim());
                } else {
                    holder.txtReading.setVisibility(View.GONE);
                }

                String romaji = item.getRomaji();
                if (romaji != null && !romaji.trim().isEmpty() && !romaji.trim().equals("—")) {
                    holder.txtRomaji.setVisibility(View.VISIBLE);
                    holder.txtRomaji.setText(romaji.trim());
                } else {
                    holder.txtRomaji.setVisibility(View.GONE);
                }
            }

            holder.txtMeaning.setText(item.getIndonesian() != null ? item.getIndonesian() : "");

            boolean hasBadge = false;
            if (item.getBadgeLabel() != null && !item.getBadgeLabel().trim().isEmpty()) {
                holder.txtBadge.setVisibility(View.VISIBLE);
                holder.txtBadge.setText(item.getBadgeLabel());
                hasBadge = true;
            } else if (item.getBab() != null && item.getBab() > 0) {
                holder.txtBadge.setVisibility(View.VISIBLE);
                holder.txtBadge.setText("Bab " + item.getBab());
                hasBadge = true;
            } else {
                holder.txtBadge.setVisibility(View.GONE);
            }

            if (item.getWordType() != null && item.getWordType() != LearningObject.WordType.LAINNYA) {
                holder.txtTypeBadge.setVisibility(View.VISIBLE);
                holder.txtTypeBadge.setText(item.getWordType().getDisplayName());
                hasBadge = true;
            } else {
                holder.txtTypeBadge.setVisibility(View.GONE);
            }

            if (holder.layoutBadgesRow != null) {
                holder.layoutBadgesRow.setVisibility(hasBadge ? View.VISIBLE : View.GONE);
            }

            View.OnClickListener speakAction = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    JapaneseSpeechHelper speech = (mSpeech != null)
                            ? mSpeech
                            : JapaneseSpeechHelper.getInstance(getContext());
                    if (speech != null) {
                        if (!speech.isAvailable()) {
                            Toast.makeText(getContext(), "Suara bahasa Jepang belum aktif di perangkat Anda.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String textToSpeak = item.getTtsTarget(mKanjiReadingMode);
                        com.kotoba.app.audio.PronunciationTarget target = item.isKatakana()
                                ? com.kotoba.app.audio.PronunciationTarget.KATAKANA
                                : (textToSpeak.equals(item.getJapanese()) ? com.kotoba.app.audio.PronunciationTarget.KANJI : com.kotoba.app.audio.PronunciationTarget.READING);
                        speech.speak(textToSpeak, target, item.getJapanese(), "LIBRARY_LIST", item.getId(), null);
                    }
                }
            };

            holder.btnSpeak.setOnClickListener(speakAction);

            if (mIsKanjiMode || item.getType() == LearningObject.Type.KANJI) {
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new com.kotoba.app.ui.kanji.KanjiDetailDialog(getContext(), item, mSpeech).show();
                    }
                });
            } else {
                convertView.setOnClickListener(speakAction);
            }

            return convertView;
        }

        private class ViewHolder {
            View layoutBadgesRow;
            TextView txtJapanese;
            TextView txtReading;
            TextView txtRomaji;
            TextView txtMeaning;
            TextView txtBadge;
            TextView txtTypeBadge;
            ImageButton btnSpeak;
        }
    }
}
