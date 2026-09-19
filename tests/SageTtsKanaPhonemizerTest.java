package tests;

import com.kotoba.app.audio.TtsKanaPhonemizer;
import java.util.List;

public class SageTtsKanaPhonemizerTest {

    public static void main(String[] args) {
        System.out.println("Running SageTtsKanaPhonemizerTest...");

        testBasicVowels();
        testConsonants();
        testSokuonGemination();
        testChouonpuLongVowels();
        testYoonPalatalization();
        testPunctuationAndGreetings();

        System.out.println("SageTtsKanaPhonemizerTest: All assertions passed cleanly!");
    }

    private static void testBasicVowels() {
        List<String> p = TtsKanaPhonemizer.kanaToPhonemes("あいうえお");
        assert p.size() == 5 : "Expected 5 phonemes, got " + p.size();
        assert p.get(0).equals("a") : "Expected 'a', got " + p.get(0);
        assert p.get(1).equals("i") : "Expected 'i', got " + p.get(1);
        assert p.get(2).equals("ɯ") : "Expected 'ɯ', got " + p.get(2);
        assert p.get(3).equals("e") : "Expected 'e', got " + p.get(3);
        assert p.get(4).equals("o") : "Expected 'o', got " + p.get(4);
    }

    private static void testConsonants() {
        // かきくけこ
        List<String> p = TtsKanaPhonemizer.kanaToPhonemes("かきくけこ");
        assert p.get(0).equals("k") && p.get(1).equals("a");
        assert p.get(2).equals("k") && p.get(3).equals("i");
        assert p.get(4).equals("k") && p.get(5).equals("ɯ");

        // がぎぐげご
        List<String> g = TtsKanaPhonemizer.kanaToPhonemes("がぎぐげご");
        assert g.get(0).equals("ɡ") && g.get(1).equals("a");
    }

    private static void testSokuonGemination() {
        // きって (k i t t e)
        List<String> p = TtsKanaPhonemizer.kanaToPhonemes("きって");
        assert p.size() == 5 : "Expected 5 phonemes for kitte, got " + p.size();
        assert p.get(0).equals("k") : "Expected 'k', got " + p.get(0);
        assert p.get(1).equals("i") : "Expected 'i', got " + p.get(1);
        assert p.get(2).equals("t") : "Expected 't' (geminated), got " + p.get(2);
        assert p.get(3).equals("t") : "Expected 't', got " + p.get(3);
        assert p.get(4).equals("e") : "Expected 'e', got " + p.get(4);

        // がっこう (ɡ a k k o ɯ)
        List<String> g = TtsKanaPhonemizer.kanaToPhonemes("がっこう");
        assert g.get(2).equals("k") : "Expected 'k' (geminated), got " + g.get(2);
        assert g.get(3).equals("k") : "Expected 'k', got " + g.get(3);
    }

    private static void testChouonpuLongVowels() {
        // コーヒー -> k o ː ç i ː
        List<String> p = TtsKanaPhonemizer.kanaToPhonemes("コーヒー");
        assert p.contains("ː") : "Expected long vowel symbol ː";
        assert p.get(0).equals("k");
        assert p.get(1).equals("o");
        assert p.get(2).equals("ː");
        assert p.get(3).equals("ç");
        assert p.get(4).equals("i");
        assert p.get(5).equals("ː");
    }

    private static void testYoonPalatalization() {
        // とうきょう -> t o ɯ k j o ɯ
        List<String> p = TtsKanaPhonemizer.kanaToPhonemes("とうきょう");
        assert p.contains("j") : "Expected glide 'j' in Yoon compound";

        // しゃしん -> ɕ a ɕ i n
        List<String> sha = TtsKanaPhonemizer.kanaToPhonemes("しゃしん");
        assert sha.get(0).equals("ɕ") && sha.get(1).equals("a");
    }

    private static void testPunctuationAndGreetings() {
        // こんにちは。 -> こんにちわ。 -> k o n ɲ i t ɕ i w a .
        List<String> p = TtsKanaPhonemizer.kanaToPhonemes("こんにちは。");
        assert p.get(p.size() - 1).equals(".") : "Expected period '.' at end";
        assert p.contains("w") : "Expected 'w' for shifted particle wa";
    }
}
