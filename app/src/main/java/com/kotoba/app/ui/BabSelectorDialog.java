package com.kotoba.app.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kotoba.app.R;
import com.kotoba.app.data.KotobaDatabase;
import com.kotoba.app.data.model.Chapter;

import java.util.List;

public class BabSelectorDialog extends Dialog {
    public interface OnBabSelectedListener {
        void onBabSelected(int bab);
    }

    private final KotobaDatabase mDatabase;
    private final int mCurrentBab;
    private final OnBabSelectedListener mListener;

    public BabSelectorDialog(Context context, KotobaDatabase db, int currentBab, OnBabSelectedListener listener) {
        super(context);
        this.mDatabase = db;
        this.mCurrentBab = currentBab;
        this.mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_bab_selector);

        LinearLayout listContainer = findViewById(R.id.dialog_bab_list_container);
        Button btnClose = findViewById(R.id.dialog_btn_close);

        List<Chapter> chapters = mDatabase.getAllChapters();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (final Chapter ch : chapters) {
            View itemView = inflater.inflate(R.layout.item_bab_chip, listContainer, false);
            TextView txt = (TextView) itemView;
            txt.setText("Bab " + ch.getBabNumber() + ": " + ch.getTitleJa() + " (" + ch.getVocabCount() + " kata)");

            if (ch.getBabNumber() == mCurrentBab) {
                txt.setBackgroundResource(R.drawable.bg_chip_selected);
                txt.setTextColor(getContext().getColor(R.color.colorActiveChipText));
            } else {
                txt.setBackgroundResource(R.drawable.bg_chip_unselected);
                txt.setTextColor(getContext().getColor(R.color.colorTextPrimary));
            }

            txt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onBabSelected(ch.getBabNumber());
                    }
                    dismiss();
                }
            });

            listContainer.addView(itemView);
        }

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}
