package tests;

import com.kotoba.app.ui.responsive.ResponsiveLayoutSystem;

public class ResponsiveLayoutSystemTest {

    private static final float EPSILON = 0.001f;

    public static void main(String[] args) {
        System.out.println("Running ResponsiveLayoutSystemTest...");

        testScaleFormulaAndClamping();
        testWidthCategoryDetection();
        testHeightCompactionThresholds();
        testFontScaleBounding();
        testJapaneseTypographyTokens();
        testMeaningTypographyTokens();
        testProportionalDividerCalculations();
        testMinimumTouchTargetGuarantee();

        System.out.println("ResponsiveLayoutSystemTest: ALL ASSERTIONS PASSED!");
    }

    private static void testScaleFormulaAndClamping() {
        // 390dp reference -> scale 1.0
        ResponsiveLayoutSystem rls390 = new ResponsiveLayoutSystem(390.0f, 800.0f, 2.0f, 1.0f);
        assertTrue(Math.abs(rls390.getGlobalScale() - 1.0f) < EPSILON, "390dp must produce exact scale 1.0");

        // 320dp compact phone -> scale clamp 0.82
        ResponsiveLayoutSystem rls320 = new ResponsiveLayoutSystem(320.0f, 600.0f, 2.0f, 1.0f);
        float expected320 = Math.max(0.82f, Math.min(1.08f, 320.0f / 390.0f));
        assertTrue(Math.abs(rls320.getGlobalScale() - expected320) < EPSILON, "320dp scale must match formula");
        assertTrue(rls320.getGlobalScale() >= 0.82f, "320dp scale must not drop below 0.82");

        // 360dp phone -> scale 360/390 ~ 0.923
        ResponsiveLayoutSystem rls360 = new ResponsiveLayoutSystem(360.0f, 740.0f, 2.0f, 1.0f);
        float expected360 = 360.0f / 390.0f;
        assertTrue(Math.abs(rls360.getGlobalScale() - expected360) < EPSILON, "360dp scale must match 360/390");

        // 375dp (iPhone-like width) -> scale 375/390 ~ 0.9615
        ResponsiveLayoutSystem rls375 = new ResponsiveLayoutSystem(375.0f, 812.0f, 3.0f, 1.0f);
        float expected375 = 375.0f / 390.0f;
        assertTrue(Math.abs(rls375.getGlobalScale() - expected375) < EPSILON, "375dp scale must match 375/390");

        // 412dp (Pixel standard) -> scale 412/390 ~ 1.0564
        ResponsiveLayoutSystem rls412 = new ResponsiveLayoutSystem(412.0f, 915.0f, 2.625f, 1.0f);
        float expected412 = 412.0f / 390.0f;
        assertTrue(Math.abs(rls412.getGlobalScale() - expected412) < EPSILON, "412dp scale must match 412/390");

        // 480dp large phone / foldable -> scale clamp 1.08
        ResponsiveLayoutSystem rls480 = new ResponsiveLayoutSystem(480.0f, 800.0f, 2.0f, 1.0f);
        assertTrue(Math.abs(rls480.getGlobalScale() - 1.08f) < EPSILON, "480dp must clamp to 1.08");

        // 600dp+ tablet -> scale clamp 1.08
        ResponsiveLayoutSystem rls600 = new ResponsiveLayoutSystem(600.0f, 1024.0f, 2.0f, 1.0f);
        assertTrue(Math.abs(rls600.getGlobalScale() - 1.08f) < EPSILON, "600dp tablet must clamp to 1.08");
    }

    private static void testWidthCategoryDetection() {
        ResponsiveLayoutSystem rls320 = new ResponsiveLayoutSystem(320.0f, 600.0f, 2.0f, 1.0f);
        assertTrue(rls320.isCompactWidth(), "320dp must be detected as compact width (<360dp)");

        ResponsiveLayoutSystem rls360 = new ResponsiveLayoutSystem(360.0f, 740.0f, 2.0f, 1.0f);
        assertTrue(!rls360.isCompactWidth(), "360dp must NOT be detected as compact width");
    }

    private static void testHeightCompactionThresholds() {
        // Standard height (>= 680dp)
        ResponsiveLayoutSystem rlsTall = new ResponsiveLayoutSystem(390.0f, 800.0f, 2.0f, 1.0f);
        assertTrue(!rlsTall.isCompactHeight(), "800dp height must not be compact");
        assertTrue(!rlsTall.isExtraCompactHeight(), "800dp height must not be extra compact");
        assertTrue(rlsTall.getSectionSwitcherHeightPx() == rlsTall.dpToPx(36), "Tall section switcher must be 36dp");
        assertTrue(rlsTall.getModeBarHeightPx() == rlsTall.dpToPx(42), "Tall mode bar must be 42dp");
        assertTrue(rlsTall.getCardPaddingPx() == rlsTall.dpToPx(20), "Tall card padding must be 20dp");

        // Compact height (< 680dp, e.g. 640dp)
        ResponsiveLayoutSystem rlsCompact = new ResponsiveLayoutSystem(390.0f, 640.0f, 2.0f, 1.0f);
        assertTrue(rlsCompact.isCompactHeight(), "640dp height must be compact (<680dp)");
        assertTrue(!rlsCompact.isExtraCompactHeight(), "640dp height must not be extra compact");
        assertTrue(rlsCompact.getSectionSwitcherHeightPx() == rlsCompact.dpToPx(30), "Compact section switcher must be 30dp (compacted)");
        assertTrue(rlsCompact.getModeBarHeightPx() == rlsCompact.dpToPx(36), "Compact mode bar must be 36dp (compacted)");
        assertTrue(rlsCompact.getCardPaddingPx() == rlsCompact.dpToPx(14), "Compact card padding must be 14dp");

        // Extra compact height (< 580dp, e.g. landscape or small phones)
        ResponsiveLayoutSystem rlsExtraCompact = new ResponsiveLayoutSystem(390.0f, 520.0f, 2.0f, 1.0f);
        assertTrue(rlsExtraCompact.isCompactHeight(), "520dp must be compact");
        assertTrue(rlsExtraCompact.isExtraCompactHeight(), "520dp must be extra compact (<580dp)");
        assertTrue(rlsExtraCompact.getCardPaddingPx() == rlsExtraCompact.dpToPx(10), "Extra compact card padding must be 10dp");
    }

    private static void testFontScaleBounding() {
        float density = 2.0f;
        // Test font scales: 1.0, 1.15, 1.30, 1.50
        ResponsiveLayoutSystem rls10 = new ResponsiveLayoutSystem(390.0f, 800.0f, density, 1.0f);
        ResponsiveLayoutSystem rls115 = new ResponsiveLayoutSystem(390.0f, 800.0f, density, 1.15f);
        ResponsiveLayoutSystem rls130 = new ResponsiveLayoutSystem(390.0f, 800.0f, density, 1.30f);
        ResponsiveLayoutSystem rls150 = new ResponsiveLayoutSystem(390.0f, 800.0f, density, 1.50f);

        // Standard 16sp -> 16 * 2 * 1.0 = 32px
        assertTrue(rls10.spToPx(16.0f) == 32, "16sp at 1.0 font scale should be 32px");

        // 16sp at 1.15 -> 16 * 2 * 1.15 = 36.8 -> 37px
        assertTrue(rls115.spToPx(16.0f) == Math.round(16.0f * density * 1.15f), "16sp at 1.15 font scale must scale proportionally");

        // Font scale 1.30 and 1.50 should be clamped to 1.25 to prevent layout destruction
        int px130 = rls130.spToPx(16.0f);
        int px150 = rls150.spToPx(16.0f);
        int expectedCapped = Math.round(16.0f * density * 1.25f);
        assertTrue(px130 == expectedCapped, "Font scale 1.30 must be safely bounded to 1.25x max");
        assertTrue(px150 == expectedCapped, "Font scale 1.50 must be safely bounded to 1.25x max");
    }

    private static void testJapaneseTypographyTokens() {
        ResponsiveLayoutSystem rls = new ResponsiveLayoutSystem(390.0f, 800.0f, 2.0f, 1.0f);

        // Front card Japanese sizes
        float sizeShort = rls.getCardJapaneseSizeSp(true, 2);
        float sizeMedium = rls.getCardJapaneseSizeSp(true, 4);
        float sizeLong = rls.getCardJapaneseSizeSp(true, 6);

        assertTrue(sizeShort >= sizeMedium, "Shorter kanji should have >= font size than medium kanji");
        assertTrue(sizeMedium >= sizeLong, "Medium kanji should have >= font size than long kanji");
        assertTrue(sizeLong >= 24.0f && sizeLong <= 44.0f, "Long kanji must be within [24, 44] sp");
        assertTrue(sizeShort <= 44.0f, "Short kanji must not exceed 44sp");

        // Furigana and reading sizes
        float furigana = rls.getCardFuriganaSizeSp();
        assertTrue(furigana >= 13.0f && furigana <= 18.0f, "Furigana must be within [13, 18] sp");

        float reading = rls.getCardReadingSizeSp();
        assertTrue(reading >= 18.0f && reading <= 28.0f, "Reading must be within [18, 28] sp");

        // Compact height adjustments
        ResponsiveLayoutSystem rlsCompact = new ResponsiveLayoutSystem(390.0f, 600.0f, 2.0f, 1.0f);
        assertTrue(rlsCompact.getCardJapaneseSizeSp(true, 2) < sizeShort, "Compact height must slightly scale down Japanese size");
    }

    private static void testMeaningTypographyTokens() {
        ResponsiveLayoutSystem rls = new ResponsiveLayoutSystem(390.0f, 800.0f, 2.0f, 1.0f);

        float shortMeaning = rls.getCardMeaningSizeSp(10); // "Musim"
        float longMeaning = rls.getCardMeaningSizeSp(50); // very long explanation

        assertTrue(shortMeaning > longMeaning, "Short meaning should have larger font than lengthy explanation");
        assertTrue(shortMeaning <= 24.0f && shortMeaning >= 14.0f, "Short meaning must be in [14, 24] sp");
        assertTrue(longMeaning >= 14.0f, "Long meaning must not shrink below 14sp to remain readable");
    }

    private static void testProportionalDividerCalculations() {
        ResponsiveLayoutSystem rls = new ResponsiveLayoutSystem(390.0f, 800.0f, 2.0f, 1.0f);

        // Divider width target is 42% of card width, clamped [70dp, 140dp]
        int cardWidthPx = rls.dpToPx(350);
        int dividerPx = rls.getDividerWidthPx(cardWidthPx);
        int minDividerPx = rls.dpToPx(70);
        int maxDividerPx = rls.dpToPx(140);

        assertTrue(dividerPx >= minDividerPx, "Divider must not be smaller than 70dp");
        assertTrue(dividerPx <= maxDividerPx, "Divider must not exceed 140dp");

        // Tiny card width -> clamps to 70dp
        int tinyCardWidth = rls.dpToPx(100);
        assertTrue(rls.getDividerWidthPx(tinyCardWidth) == minDividerPx, "Tiny card must clamp divider to 70dp");

        // Massive card width (tablet) -> clamps to 140dp
        int hugeCardWidth = rls.dpToPx(600);
        assertTrue(rls.getDividerWidthPx(hugeCardWidth) == maxDividerPx, "Huge card must clamp divider to 140dp");
    }

    private static void testMinimumTouchTargetGuarantee() {
        assertTrue(ResponsiveLayoutSystem.MIN_TOUCH_TARGET_DP >= 44, "Minimum touch target must be at least 44dp");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("TEST FAILURE: " + message);
        }
    }
}
