package com.kotoba.app.engine;

import com.kotoba.app.data.model.UserProgress;

public class WeaknessDetector {
    public enum LatencyProfile {
        IMPULSIVE, // < 800 ms
        FLUENT,    // 800 - 4000 ms
        HESITANT,  // 4000 - 10000 ms
        STRUGGLING // > 10000 ms
    }

    public static LatencyProfile profileLatency(int responseTimeMs) {
        if (responseTimeMs < 800) {
            return LatencyProfile.IMPULSIVE;
        } else if (responseTimeMs <= 4000) {
            return LatencyProfile.FLUENT;
        } else if (responseTimeMs <= 10000) {
            return LatencyProfile.HESITANT;
        } else {
            return LatencyProfile.STRUGGLING;
        }
    }

    public static double calculateWeaknessScore(UserProgress p) {
        if (p == null) return 0.0;
        double score = 0.0;

        // Lapse contribution (up to 0.5)
        score += Math.min(0.5, p.getLapses() * 0.15);

        // Low ease factor contribution (up to 0.3)
        if (p.getEaseFactor() < 2.0) {
            score += (2.0 - p.getEaseFactor()) * 0.3;
        }

        // High latency contribution (up to 0.2)
        if (p.getLastResponseTimeMs() > 6000) {
            score += Math.min(0.2, (p.getLastResponseTimeMs() - 6000) / 20000.0);
        }

        return Math.min(1.0, score);
    }

    public static String getWeaknessExplanation(UserProgress p) {
        if (p == null || p.getMasteryState() != UserProgress.MasteryState.WEAK) {
            return "Kondisi retensi normal.";
        }
        if (p.getLapses() >= 2) {
            return "Mengalami " + p.getLapses() + " kali kesalahan berulang. Memerlukan peninjauan terfokus.";
        } else if (p.getLastResponseTimeMs() > 10000) {
            return "Waktu respons tinggi (" + String.format("%.1f", p.getLastResponseTimeMs() / 1000.0) + " detik). Mengindikasikan keraguan mengingat.";
        } else {
            return "Baru mengalami kesalahan pada peninjauan terakhir.";
        }
    }
}
