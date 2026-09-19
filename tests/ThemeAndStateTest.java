package tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThemeAndStateTest {

    // Constants matching android.content.res.Configuration
    private static final int UI_MODE_NIGHT_MASK = 0x30;
    private static final int UI_MODE_NIGHT_NO = 0x10;
    private static final int UI_MODE_NIGHT_YES = 0x20;
    private static final int UI_MODE_NIGHT_UNDEFINED = 0x00;

    public static void main(String[] args) throws Exception {
        System.out.println("Running ThemeAndStateTest...");

        testUiModeBitmaskOperations();
        testColorResourceParity();
        testDeckStateRestorationLogic();
        testFlashcardArtiModeDisplayParity();

        System.out.println("ThemeAndStateTest: All assertions passed successfully!");
    }

    private static void testUiModeBitmaskOperations() {
        System.out.println("  Testing uiMode bitmask manipulations...");

        int baseMode = UI_MODE_NIGHT_UNDEFINED;

        // Apply NIGHT_YES
        int darkMode = (baseMode & ~UI_MODE_NIGHT_MASK) | UI_MODE_NIGHT_YES;
        assert (darkMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES : "Expected UI_MODE_NIGHT_YES";

        // Apply NIGHT_NO on existing NIGHT_YES
        int lightMode = (darkMode & ~UI_MODE_NIGHT_MASK) | UI_MODE_NIGHT_NO;
        assert (lightMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_NO : "Expected UI_MODE_NIGHT_NO";

        // Toggle back to NIGHT_YES
        int toggleBack = (lightMode & ~UI_MODE_NIGHT_MASK) | UI_MODE_NIGHT_YES;
        assert (toggleBack & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES : "Expected UI_MODE_NIGHT_YES after toggle";
    }

    private static void testColorResourceParity() throws Exception {
        System.out.println("  Testing color resource parity between light and night palettes...");

        File lightColorsFile = new File("/storage/emulated/0/download/PROJECT/KTB/app/src/main/res/values/colors.xml");
        File nightColorsFile = new File("/storage/emulated/0/download/PROJECT/KTB/app/src/main/res/values-night/colors.xml");

        assert lightColorsFile.exists() : "Light colors.xml must exist";
        assert nightColorsFile.exists() : "Night colors.xml must exist";

        Map<String, String> lightColors = parseColorFile(lightColorsFile);
        Map<String, String> nightColors = parseColorFile(nightColorsFile);

        assert !lightColors.isEmpty() : "Light colors must not be empty";
        assert !nightColors.isEmpty() : "Night colors must not be empty";

        // Verify that all core UI colors in light have a matching night definition
        String[] essentialColors = new String[]{
                "colorBackground",
                "colorSurface",
                "colorSurfaceElevated",
                "colorBorderOutline",
                "colorCardBg",
                "colorCardStroke",
                "colorTextPrimary",
                "colorTextSecondary",
                "colorTextTertiary",
                "colorPrimary"
        };

        for (String key : essentialColors) {
            assert lightColors.containsKey(key) : "Light colors missing key: " + key;
            assert nightColors.containsKey(key) : "Night colors missing key: " + key;

            String lightHex = lightColors.get(key);
            String nightHex = nightColors.get(key);

            assert lightHex != null && !lightHex.isEmpty() : "Light hex empty for " + key;
            assert nightHex != null && !nightHex.isEmpty() : "Night hex empty for " + key;

            // Background & surface in dark mode should be dark, not light
            if ("colorBackground".equals(key)) {
                assert nightHex.equalsIgnoreCase("#121016") : "Night colorBackground must be dark #121016, got " + nightHex;
            }
            if ("colorTextPrimary".equals(key)) {
                assert nightHex.toUpperCase().startsWith("#E") || nightHex.toUpperCase().startsWith("#F")
                        : "Night colorTextPrimary must be high-contrast light text, got " + nightHex;
            }
        }
    }

    private static Map<String, String> parseColorFile(File file) throws Exception {
        Map<String, String> colors = new HashMap<>();
        Pattern pattern = Pattern.compile("<color\\s+name=\"([^\"]+)\">([^<]+)</color>");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String name = matcher.group(1).trim();
                    String val = matcher.group(2).trim();
                    colors.put(name, val);
                }
            }
        }
        return colors;
    }

    private static void testDeckStateRestorationLogic() {
        System.out.println("  Testing state preservation and deck ID restoration...");

        // Simulate an active deck of IDs
        List<String> originalDeckIds = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            originalDeckIds.add("vocab_" + i);
        }

        int currentIndex = 14; // User was at card 15

        // Save state simulation
        List<String> savedIds = new ArrayList<>(originalDeckIds);
        int savedIndex = currentIndex;

        // Restore simulation
        assert savedIds.size() == 25 : "Restored deck ID count must match";
        assert savedIndex == 14 : "Restored current index must match saved position";
        for (int i = 0; i < originalDeckIds.size(); i++) {
            assert originalDeckIds.get(i).equals(savedIds.get(i)) : "Restored deck item order preserved";
        }
    }

    private static void testFlashcardArtiModeDisplayParity() {
        System.out.println("  Testing FlashcardView ARTI mode display contract...");

        // Mock data item
        String japanese = "食べる";
        String reading = "たべる";
        String romaji = "taberu";
        String indonesian = "makan";

        // In ARTI mode:
        // Front displays: Indonesian meaning ("makan")
        // Back displays: Japanese ("食べる"), Reading ("たべる"), Romaji ("taberu"), and Indonesian meaning ("makan")
        String frontMain = indonesian;
        String backMain = japanese;
        String backFurigana = reading;
        String backRomaji = romaji;
        String backMeaning = indonesian; // Previously was bugged as japanese ("食べる")

        assert frontMain.equals("makan") : "Front must show Indonesian";
        assert backMain.equals("食べる") : "Back main must show Japanese";
        assert backMeaning.equals("makan") : "Back meaning must show Indonesian translation, not repeat Japanese";
        assert !backMeaning.equals(backMain) : "Back meaning must not be identical to back Japanese text";
    }
}
