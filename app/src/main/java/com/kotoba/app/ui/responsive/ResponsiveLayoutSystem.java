package com.kotoba.app.ui.responsive;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Centralized Responsive Token & Layout Scaling System for KOTOBAAN.
 *
 * Implements bounded proportional scaling based on a 390dp reference width,
 * reflow protection, and adaptive vertical compaction for constrained screen heights.
 *
 * Scale Formula:
 *   scale = clamp(screenWidthDp / 390dp, 0.82, 1.08)
 */
public class ResponsiveLayoutSystem {

    public static final float REFERENCE_WIDTH_DP = 390.0f;
    public static final float MIN_SCALE = 0.82f;
    public static final float MAX_SCALE = 1.08f;
    public static final int MIN_TOUCH_TARGET_DP = 44;

    private final float mScreenWidthDp;
    private final float mScreenHeightDp;
    private final float mDensity;
    private final float mFontScale;
    private final float mGlobalScale;
    private final boolean mIsCompactWidth;
    private final boolean mIsCompactHeight;
    private final boolean mIsExtraCompactHeight;

    public ResponsiveLayoutSystem(float widthDp, float heightDp, float density, float fontScale) {
        this.mScreenWidthDp = widthDp;
        this.mScreenHeightDp = heightDp;
        this.mDensity = density > 0 ? density : 1.0f;
        this.mFontScale = fontScale > 0 ? fontScale : 1.0f;

        float rawScale = widthDp / REFERENCE_WIDTH_DP;
        this.mGlobalScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, rawScale));

        this.mIsCompactWidth = widthDp < 360.0f;
        this.mIsCompactHeight = heightDp < 680.0f;
        this.mIsExtraCompactHeight = heightDp < 580.0f;
    }

    public static ResponsiveLayoutSystem from(Context context) {
        if (context == null) {
            return new ResponsiveLayoutSystem(390.0f, 800.0f, 2.0f, 1.0f);
        }
        Configuration config = context.getResources().getConfiguration();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float widthDp = config.screenWidthDp > 0 ? config.screenWidthDp : (dm.widthPixels / dm.density);
        float heightDp = config.screenHeightDp > 0 ? config.screenHeightDp : (dm.heightPixels / dm.density);
        return new ResponsiveLayoutSystem(widthDp, heightDp, dm.density, config.fontScale);
    }

    // --- Metrics Accessors ---

    public float getScreenWidthDp() {
        return mScreenWidthDp;
    }

    public float getScreenHeightDp() {
        return mScreenHeightDp;
    }

    public float getGlobalScale() {
        return mGlobalScale;
    }

    public float getDensity() {
        return mDensity;
    }

    public float getFontScale() {
        return mFontScale;
    }

    public boolean isCompactWidth() {
        return mIsCompactWidth;
    }

    public boolean isCompactHeight() {
        return mIsCompactHeight;
    }

    public boolean isExtraCompactHeight() {
        return mIsExtraCompactHeight;
    }

    public int dpToPx(float dp) {
        return Math.round(dp * mDensity);
    }

    public int spToPx(float sp) {
        // Protect layout against unbounded extreme accessibility font explosion (>1.25x)
        float safeFontScale = Math.min(mFontScale, 1.25f);
        return Math.round(sp * mDensity * safeFontScale);
    }

    // --- Bounded Typography Tokens (sp) ---

    public float getCardJapaneseSizeSp(boolean isFront, int charCount) {
        float base = isFront ? (charCount > 4 ? 32.0f : (charCount > 2 ? 38.0f : 44.0f)) : 30.0f;
        if (mIsCompactHeight) {
            base -= 4.0f;
        }
        return clamp(base * mGlobalScale, 24.0f, 44.0f);
    }

    public float getCardReadingSizeSp() {
        float base = mIsCompactHeight ? 22.0f : 26.0f;
        return clamp(base * mGlobalScale, 18.0f, 28.0f);
    }

    public float getCardFuriganaSizeSp() {
        float base = mIsCompactHeight ? 15.0f : 17.0f;
        return clamp(base * mGlobalScale, 13.0f, 18.0f);
    }

    public float getCardMeaningSizeSp(int textLength) {
        float base = textLength > 40 ? 16.0f : (textLength > 20 ? 18.0f : 22.0f);
        if (mIsCompactHeight) {
            base -= 2.0f;
        }
        return clamp(base * mGlobalScale, 14.0f, 24.0f);
    }

    public float getCardRomajiSizeSp() {
        float base = mIsCompactHeight ? 12.0f : 14.0f;
        return clamp(base * mGlobalScale, 11.0f, 15.0f);
    }

    public float getAppTitleSizeSp() {
        float base = mIsCompactHeight ? 20.0f : 24.0f;
        return clamp(base * mGlobalScale, 18.0f, 26.0f);
    }

    public float getCounterSizeSp() {
        return clamp(13.0f * mGlobalScale, 11.0f, 14.0f);
    }

    public float getSegmentedTextSizeSp() {
        float base = mIsCompactWidth ? 11.5f : 13.0f;
        return clamp(base * mGlobalScale, 11.0f, 14.0f);
    }

    public float getNavButtonTextSizeSp() {
        float base = mIsCompactWidth ? 13.5f : 15.0f;
        return clamp(base * mGlobalScale, 12.0f, 16.0f);
    }

    public float getRecallButtonTextSizeSp() {
        float base = mIsCompactWidth ? 13.0f : 14.0f;
        return clamp(base * mGlobalScale, 12.0f, 15.0f);
    }

    public float getBadgeTextSizeSp() {
        return clamp(11.0f * mGlobalScale, 10.0f, 13.0f);
    }

    public float getHintTextSizeSp() {
        float base = mIsCompactHeight ? 11.0f : 12.0f;
        return clamp(base * mGlobalScale, 10.0f, 13.0f);
    }

    // --- Bounded Spacing & Dimension Tokens (dp) ---

    public int getScreenHorizontalPaddingPx() {
        int dp = mIsCompactWidth ? 12 : (mScreenWidthDp >= 600 ? 24 : 16);
        return dpToPx(dp);
    }

    public int getCardPaddingPx() {
        int dp = mIsExtraCompactHeight ? 10 : (mIsCompactHeight ? 14 : 20);
        return dpToPx(dp);
    }

    public int getCardMarginHorizontalPx() {
        int dp = mIsCompactWidth ? 12 : 16;
        return dpToPx(dp);
    }

    public int getCardMarginVerticalPx() {
        int dp = mIsCompactHeight ? 4 : 8;
        return dpToPx(dp);
    }

    public int getDividerWidthPx(int availableCardWidthPx) {
        if (availableCardWidthPx <= 0) {
            availableCardWidthPx = dpToPx(mScreenWidthDp - 32);
        }
        int minPx = dpToPx(70);
        int maxPx = dpToPx(140);
        int targetPx = (int) (availableCardWidthPx * 0.42f);
        return Math.max(minPx, Math.min(maxPx, targetPx));
    }

    public int getDividerVerticalMarginPx() {
        int dp = mIsExtraCompactHeight ? 4 : (mIsCompactHeight ? 8 : 12);
        return dpToPx(dp);
    }

    // --- Component Heights (dp / px) ---

    public int getSectionSwitcherHeightPx() {
        int dp = mIsCompactHeight ? 30 : 36;
        return dpToPx(dp);
    }

    public int getKanjiSubSwitcherHeightPx() {
        int dp = mIsCompactHeight ? 28 : 32;
        return dpToPx(dp);
    }

    public int getModeBarHeightPx() {
        int dp = mIsCompactHeight ? 36 : 42;
        return dpToPx(dp);
    }

    public int getToolbarHeightPx() {
        int dp = mIsCompactHeight ? 44 : 50;
        return dpToPx(dp);
    }

    public int getNavButtonHeightPx() {
        int dp = mIsCompactHeight ? 42 : 46;
        return dpToPx(dp);
    }

    public int getRecallButtonHeightPx() {
        int dp = mIsCompactHeight ? 44 : 48;
        return dpToPx(dp);
    }

    public int getDockMarginBottomPx() {
        int dp = mIsCompactHeight ? 4 : 8;
        return dpToPx(dp);
    }

    // --- View Tree Application Helpers ---

    public void applyToTextView(TextView tv, float sizeSp) {
        if (tv != null) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        }
    }

    public void setViewHeight(View v, int heightPx) {
        if (v != null && v.getLayoutParams() != null) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp.height != heightPx) {
                lp.height = heightPx;
                v.setLayoutParams(lp);
            }
        }
    }

    public void setViewMarginBottom(View v, int marginPx) {
        if (v != null && v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            if (mlp.bottomMargin != marginPx) {
                mlp.bottomMargin = marginPx;
                v.setLayoutParams(mlp);
            }
        }
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
