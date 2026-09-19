import Foundation

public final class WeaknessDetector {
    public enum LatencyProfile {
        case impulsive
        case fluent
        case hesitant
        case struggling

        public var label: String {
            switch self {
            case .impulsive: return "Sangat Cepat (<0.8s)"
            case .fluent: return "Lancar (0.8s - 4.0s)"
            case .hesitant: return "Ragu (4.0s - 10.0s)"
            case .struggling: return "Kesulitan (>10.0s)"
            }
        }
    }

    public static func profileLatency(_ responseTimeMs: Int) -> LatencyProfile {
        if responseTimeMs < 800 {
            return .impulsive
        } else if responseTimeMs <= 4000 {
            return .fluent
        } else if responseTimeMs <= 10000 {
            return .hesitant
        } else {
            return .struggling
        }
    }

    public static func calculateWeaknessScore(_ progress: UserProgress?) -> Double {
        guard let p = progress else { return 0.0 }
        var score = 0.0

        // Lapse contribution (up to 0.5)
        score += min(0.5, Double(p.lapses) * 0.15)

        // Low ease factor contribution (up to 0.3)
        if p.easeFactor < 2.0 {
            score += (2.0 - p.easeFactor) * 0.3
        }

        // High latency contribution (up to 0.2)
        if p.lastResponseTimeMs > 6000 {
            score += min(0.2, Double(p.lastResponseTimeMs - 6000) / 20000.0)
        }

        return min(1.0, score)
    }

    public static func getWeaknessExplanation(_ progress: UserProgress?) -> String {
        guard let p = progress, p.masteryState == .weak else {
            return "Kondisi retensi normal."
        }

        if p.lapses >= 2 {
            return "Mengalami \(p.lapses) kali kesalahan berulang. Memerlukan peninjauan terfokus."
        } else if p.lastResponseTimeMs > 10000 {
            let sec = Double(p.lastResponseTimeMs) / 1000.0
            return String(format: "Waktu respons tinggi (%.1f detik). Mengindikasikan keraguan mengingat.", sec)
        } else {
            return "Baru mengalami kesalahan pada peninjauan terakhir."
        }
    }
}
