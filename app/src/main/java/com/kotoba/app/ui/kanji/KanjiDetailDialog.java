package com.kotoba.app.ui.kanji;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kotoba.app.R;
import com.kotoba.app.audio.JapaneseSpeechHelper;
import com.kotoba.app.data.PreferencesManager;
import com.kotoba.app.data.model.LearningObject;
import com.kotoba.app.data.model.LearningObject.KanjiVocabExample;
import com.kotoba.app.ui.responsive.ResponsiveLayoutSystem;

import java.util.List;

public class KanjiDetailDialog extends Dialog {

    public enum Mode {
        ONYOMI,
        KUNYOMI,
        BOTH
    }

    private final LearningObject mItem;
    private final JapaneseSpeechHelper mSpeech;
    private Mode mCurrentMode = Mode.BOTH;

    private TextView mTxtBadge;
    private TextView mTxtLevel;
    private TextView mTxtGroup;
    private ImageButton mBtnClose;
    private TextView mTxtCharacter;
    private ImageButton mBtnAudio;
    private TextView mTxtMeaning;
    private TextView mTxtRomaji;

    private Button mBtnTabOnyomi;
    private Button mBtnTabKunyomi;
    private Button mBtnTabBoth;

    private View mLayoutSectionOnyomi;
    private LinearLayout mContainerOnyomiReadings;
    private TextView mTxtEmptyOnyomi;

    private View mLayoutSectionKunyomi;
    private LinearLayout mContainerKunyomiReadings;
    private TextView mTxtEmptyKunyomi;

    private View mLayoutSectionVocab;
    private LinearLayout mContainerVocabList;
    private TextView mTxtEmptyVocab;

    private Button mBtnBottomClose;

    public KanjiDetailDialog(Context context, LearningObject item) {
        this(context, item, JapaneseSpeechHelper.getInstance(context));
    }

    public KanjiDetailDialog(Context context, LearningObject item, JapaneseSpeechHelper speech) {
        super(context, PreferencesManager.getInstance(context).isDarkMode() ? R.style.Theme_Kotoba_Dark : R.style.Theme_Kotoba);
        this.mItem = item;
        this.mSpeech = speech != null ? speech : JapaneseSpeechHelper.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_kanji_detail);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(getWindow().getAttributes());
            lp.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.94);
            int maxH = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.88);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getWindow().setAttributes(lp);
        }

        initViews();
        bindData();
        setupListeners();
        setMode(Mode.BOTH);
    }

    private void initViews() {
        mTxtBadge = findViewById(R.id.txt_kanji_badge);
        mTxtLevel = findViewById(R.id.txt_kanji_level);
        mTxtGroup = findViewById(R.id.txt_kanji_group);
        mBtnClose = findViewById(R.id.btn_dialog_kanji_close);
        mTxtCharacter = findViewById(R.id.txt_kanji_character);
        mBtnAudio = findViewById(R.id.btn_kanji_audio);
        mTxtMeaning = findViewById(R.id.txt_kanji_meaning);
        mTxtRomaji = findViewById(R.id.txt_kanji_romaji);

        mBtnTabOnyomi = findViewById(R.id.btn_tab_onyomi);
        mBtnTabKunyomi = findViewById(R.id.btn_tab_kunyomi);
        mBtnTabBoth = findViewById(R.id.btn_tab_both);

        mLayoutSectionOnyomi = findViewById(R.id.layout_section_onyomi);
        mContainerOnyomiReadings = findViewById(R.id.container_onyomi_readings);
        mTxtEmptyOnyomi = findViewById(R.id.txt_empty_onyomi);

        mLayoutSectionKunyomi = findViewById(R.id.layout_section_kunyomi);
        mContainerKunyomiReadings = findViewById(R.id.container_kunyomi_readings);
        mTxtEmptyKunyomi = findViewById(R.id.txt_empty_kunyomi);

        mLayoutSectionVocab = findViewById(R.id.layout_section_vocab);
        mContainerVocabList = findViewById(R.id.container_vocab_list);
        mTxtEmptyVocab = findViewById(R.id.txt_empty_vocab);

        mBtnBottomClose = findViewById(R.id.btn_kanji_bottom_close);
    }

    private void bindData() {
        if (mItem == null) return;

        // Badges
        String badge = mItem.getBadgeLabel();
        if (badge != null && !badge.isEmpty()) {
            mTxtBadge.setText(badge);
            mTxtBadge.setVisibility(View.VISIBLE);
        } else {
            mTxtBadge.setVisibility(View.GONE);
        }

        String level = mItem.getLevel();
        if (level != null && !level.isEmpty()) {
            mTxtLevel.setText(level);
            mTxtLevel.setVisibility(View.VISIBLE);
        } else {
            mTxtLevel.setVisibility(View.GONE);
        }

        String group = mItem.getGroupLabel();
        if (group != null && !group.isEmpty()) {
            mTxtGroup.setText(group);
            mTxtGroup.setVisibility(View.VISIBLE);
        } else {
            mTxtGroup.setVisibility(View.GONE);
        }

        // Main glyph
        mTxtCharacter.setText(mItem.getJapanese());
        mTxtMeaning.setText(mItem.getMeaning() != null ? mItem.getMeaning() : "");

        // Romaji summary
        String romajiDisp = mItem.getRomaji();
        if (romajiDisp != null && !romajiDisp.isEmpty()) {
            mTxtRomaji.setText(romajiDisp);
            mTxtRomaji.setVisibility(View.VISIBLE);
        } else {
            mTxtRomaji.setVisibility(View.GONE);
        }

        populateOnyomiSection();
        populateKunyomiSection();
        populateVocabSection();
    }

    private void populateOnyomiSection() {
        mContainerOnyomiReadings.removeAllViews();
        List<String> onList = mItem.getOnyomiList();
        List<String> onRomaji = mItem.getOnyomiRomajiList();

        if (onList == null || onList.isEmpty()) {
            mTxtEmptyOnyomi.setVisibility(View.VISIBLE);
            return;
        }

        mTxtEmptyOnyomi.setVisibility(View.GONE);
        ResponsiveLayoutSystem rls = ResponsiveLayoutSystem.from(getContext());

        for (int i = 0; i < onList.size(); i++) {
            final String onStr = onList.get(i);
            String romStr = (onRomaji != null && i < onRomaji.size()) ? onRomaji.get(i) : "";

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            row.setPadding(0, rls.dpToPx(4), 0, rls.dpToPx(4));

            LinearLayout textCol = new LinearLayout(getContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.0f
            ));

            TextView txtKana = new TextView(getContext());
            txtKana.setText(onStr);
            txtKana.setTextSize(20f);
            txtKana.setTypeface(Typeface.DEFAULT_BOLD);
            txtKana.setTextColor(getColorCompat(R.color.colorTextPrimary));
            textCol.addView(txtKana);

            if (!romStr.isEmpty()) {
                TextView txtRom = new TextView(getContext());
                txtRom.setText(romStr);
                txtRom.setTextSize(13f);
                txtRom.setTextColor(getColorCompat(R.color.colorTextSecondary));
                textCol.addView(txtRom);
            }

            row.addView(textCol);

            ImageButton btnPlay = new ImageButton(getContext());
            int touchSize = rls.dpToPx(44);
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(touchSize, touchSize);
            btnPlay.setLayoutParams(btnLp);
            btnPlay.setBackgroundResource(R.drawable.bg_button_secondary);
            btnPlay.setImageResource(R.drawable.ic_volume_up);
            btnPlay.setContentDescription("Putar " + onStr);
            btnPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSpeech != null) {
                        mSpeech.speak(onStr);
                    }
                }
            });

            row.addView(btnPlay);
            mContainerOnyomiReadings.addView(row);
        }
    }

    private void populateKunyomiSection() {
        mContainerKunyomiReadings.removeAllViews();
        List<String> kunList = mItem.getKunyomiList();
        List<String> kunRomaji = mItem.getKunyomiRomajiList();

        if (kunList == null || kunList.isEmpty()) {
            mTxtEmptyKunyomi.setVisibility(View.VISIBLE);
            return;
        }

        mTxtEmptyKunyomi.setVisibility(View.GONE);
        ResponsiveLayoutSystem rls = ResponsiveLayoutSystem.from(getContext());

        for (int i = 0; i < kunList.size(); i++) {
            final String kunRaw = kunList.get(i);
            String romStr = (kunRomaji != null && i < kunRomaji.size()) ? kunRomaji.get(i) : "";

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            row.setPadding(0, rls.dpToPx(4), 0, rls.dpToPx(4));

            LinearLayout textCol = new LinearLayout(getContext());
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.0f
            ));

            TextView txtKana = new TextView(getContext());
            // Highlight okurigana if present (e.g. "た.べる" -> "た・べる")
            String displayKun = kunRaw.replace('.', '・');
            txtKana.setText(displayKun);
            txtKana.setTextSize(20f);
            txtKana.setTypeface(Typeface.DEFAULT_BOLD);
            txtKana.setTextColor(getColorCompat(R.color.colorTextPrimary));
            textCol.addView(txtKana);

            if (!romStr.isEmpty()) {
                TextView txtRom = new TextView(getContext());
                txtRom.setText(romStr);
                txtRom.setTextSize(13f);
                txtRom.setTextColor(getColorCompat(R.color.colorTextSecondary));
                textCol.addView(txtRom);
            }

            row.addView(textCol);

            ImageButton btnPlay = new ImageButton(getContext());
            int touchSize = rls.dpToPx(44);
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(touchSize, touchSize);
            btnPlay.setLayoutParams(btnLp);
            btnPlay.setBackgroundResource(R.drawable.bg_button_secondary);
            btnPlay.setImageResource(R.drawable.ic_volume_up);
            btnPlay.setContentDescription("Putar " + displayKun);
            btnPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSpeech != null) {
                        // Strip morphological dot for authentic Japanese TTS pronunciation
                        mSpeech.speak(kunRaw.replace(".", ""));
                    }
                }
            });

            row.addView(btnPlay);
            mContainerKunyomiReadings.addView(row);
        }
    }

    private void populateVocabSection() {
        mContainerVocabList.removeAllViews();
        List<KanjiVocabExample> vocabList = mItem.getVocabExamples();

        if (vocabList == null || vocabList.isEmpty()) {
            mTxtEmptyVocab.setVisibility(View.VISIBLE);
            return;
        }

        mTxtEmptyVocab.setVisibility(View.GONE);
        ResponsiveLayoutSystem rls = ResponsiveLayoutSystem.from(getContext());

        for (final KanjiVocabExample ex : vocabList) {
            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cardLp.setMargins(0, 0, 0, rls.dpToPx(8));
            card.setLayoutParams(cardLp);
            card.setBackgroundResource(R.drawable.bg_card_vocab_item);
            card.setPadding(rls.dpToPx(12), rls.dpToPx(10), rls.dpToPx(12), rls.dpToPx(10));

            // Header row: Word + Badge + Audio button
            LinearLayout headerRow = new LinearLayout(getContext());
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView txtWord = new TextView(getContext());
            txtWord.setText(ex.getWord());
            txtWord.setTextSize(18f);
            txtWord.setTypeface(Typeface.DEFAULT_BOLD);
            txtWord.setTextColor(getColorCompat(R.color.colorTextPrimary));
            headerRow.addView(txtWord);

            if (ex.getReadingType() != null && !ex.getReadingType().isEmpty()) {
                TextView badge = new TextView(getContext());
                badge.setText(ex.getReadingType().equalsIgnoreCase("KUNYOMI") ? "KUN" : "ON");
                badge.setTextSize(10f);
                badge.setTypeface(Typeface.DEFAULT_BOLD);
                badge.setBackgroundResource(R.drawable.bg_badge);
                badge.setTextColor(getColorCompat(R.color.colorBadgeText));
                badge.setPadding(rls.dpToPx(6), rls.dpToPx(2), rls.dpToPx(6), rls.dpToPx(2));
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                bLp.setMargins(rls.dpToPx(8), 0, 0, 0);
                badge.setLayoutParams(bLp);
                headerRow.addView(badge);
            }

            View spacer = new View(getContext());
            spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1.0f));
            headerRow.addView(spacer);

            ImageButton btnPlay = new ImageButton(getContext());
            int touchSize = rls.dpToPx(40);
            btnPlay.setLayoutParams(new LinearLayout.LayoutParams(touchSize, touchSize));
            btnPlay.setBackgroundResource(R.drawable.bg_button_secondary);
            btnPlay.setImageResource(R.drawable.ic_volume_up);
            btnPlay.setContentDescription("Putar " + ex.getWord());
            btnPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSpeech != null) {
                        mSpeech.speak(ex.getReading() != null && !ex.getReading().isEmpty() ? ex.getReading() : ex.getWord());
                    }
                }
            });
            headerRow.addView(btnPlay);

            card.addView(headerRow);

            // Reading + Romaji row
            TextView txtRead = new TextView(getContext());
            String readText = ex.getReading();
            if (ex.getRomaji() != null && !ex.getRomaji().isEmpty()) {
                readText += " (" + ex.getRomaji() + ")";
            }
            txtRead.setText(readText);
            txtRead.setTextSize(13f);
            txtRead.setTextColor(getColorCompat(R.color.colorPrimary));
            card.addView(txtRead);

            // Meaning row
            TextView txtMean = new TextView(getContext());
            txtMean.setText(ex.getMeaning());
            txtMean.setTextSize(13f);
            txtMean.setTextColor(getColorCompat(R.color.colorTextPrimary));
            LinearLayout.LayoutParams meanLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            meanLp.setMargins(0, rls.dpToPx(2), 0, 0);
            txtMean.setLayoutParams(meanLp);
            card.addView(txtMean);

            mContainerVocabList.addView(card);
        }
    }

    private void setupListeners() {
        mBtnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mBtnBottomClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mBtnAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSpeech == null || mItem == null) return;
                // Speak primary reading based on mode
                if (mCurrentMode == Mode.ONYOMI && mItem.hasOnyomi()) {
                    mSpeech.speak(mItem.getOnyomiList().get(0));
                } else if (mCurrentMode == Mode.KUNYOMI && mItem.hasKunyomi()) {
                    mSpeech.speak(mItem.getKunyomiList().get(0).replace(".", ""));
                } else if (mItem.hasKunyomi()) {
                    mSpeech.speak(mItem.getKunyomiList().get(0).replace(".", ""));
                } else if (mItem.hasOnyomi()) {
                    mSpeech.speak(mItem.getOnyomiList().get(0));
                } else {
                    mSpeech.speak(mItem.getReading());
                }
            }
        });

        mBtnTabOnyomi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(Mode.ONYOMI);
            }
        });

        mBtnTabKunyomi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(Mode.KUNYOMI);
            }
        });

        mBtnTabBoth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(Mode.BOTH);
            }
        });
    }

    public void setMode(Mode mode) {
        this.mCurrentMode = mode;

        int activeBg = R.drawable.bg_segmented_active;
        int activeTextColor = 0xFFFFFFFF;
        int inactiveTextColor = getColorCompat(R.color.colorTextSecondary);

        mBtnTabOnyomi.setBackgroundResource(mode == Mode.ONYOMI ? activeBg : android.R.color.transparent);
        mBtnTabOnyomi.setTextColor(mode == Mode.ONYOMI ? activeTextColor : inactiveTextColor);

        mBtnTabKunyomi.setBackgroundResource(mode == Mode.KUNYOMI ? activeBg : android.R.color.transparent);
        mBtnTabKunyomi.setTextColor(mode == Mode.KUNYOMI ? activeTextColor : inactiveTextColor);

        mBtnTabBoth.setBackgroundResource(mode == Mode.BOTH ? activeBg : android.R.color.transparent);
        mBtnTabBoth.setTextColor(mode == Mode.BOTH ? activeTextColor : inactiveTextColor);

        switch (mode) {
            case ONYOMI:
                mLayoutSectionOnyomi.setVisibility(View.VISIBLE);
                mLayoutSectionKunyomi.setVisibility(View.GONE);
                break;
            case KUNYOMI:
                mLayoutSectionOnyomi.setVisibility(View.GONE);
                mLayoutSectionKunyomi.setVisibility(View.VISIBLE);
                break;
            case BOTH:
            default:
                mLayoutSectionOnyomi.setVisibility(View.VISIBLE);
                mLayoutSectionKunyomi.setVisibility(View.VISIBLE);
                break;
        }
    }

    private int getColorCompat(int resId) {
        return getContext().getResources().getColor(resId);
    }
}
