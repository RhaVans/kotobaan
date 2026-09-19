import SwiftUI

public struct PustakaCardRow: View {
    public let item: LearningObject
    public let onPlayAudio: () -> Void

    public init(item: LearningObject, onPlayAudio: @escaping () -> Void) {
        self.item = item
        self.onPlayAudio = onPlayAudio
    }

    private var hasBadges: Bool {
        !item.badgeLabel.isEmpty || (item.bab != nil && item.bab! > 0) || item.wordType != .lainnya
    }

    private var shouldShowReading: Bool {
        let trimmed = item.reading.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty || trimmed == "—" {
            return false
        }
        return trimmed != item.japanese.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Row 1: Top Badges Header
            if hasBadges {
                HStack(spacing: 6) {
                    if !item.badgeLabel.isEmpty {
                        Text(item.badgeLabel)
                            .font(.system(size: 11, weight: .medium))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Color.accentColor.opacity(0.12))
                            .foregroundColor(.accentColor)
                            .clipShape(Capsule())
                    } else if let bab = item.bab, bab > 0 {
                        Text("Bab \(bab)")
                            .font(.system(size: 11, weight: .medium))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Color.accentColor.opacity(0.12))
                            .foregroundColor(.accentColor)
                            .clipShape(Capsule())
                    }

                    if item.wordType != .lainnya {
                        Text(item.wordType.displayName)
                            .font(.system(size: 11, weight: .medium))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(Color.accentColor.opacity(0.12))
                            .foregroundColor(.accentColor)
                            .clipShape(Capsule())
                    }

                    Spacer()
                }
            }

            // Row 2: Japanese Title & Reading / Romaji
            HStack(alignment: .center, spacing: 8) {
                Text(item.japanese)
                    .font(.system(size: 18, weight: .bold, design: .serif))
                    .foregroundColor(.primary)

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    if shouldShowReading {
                        Text(item.reading)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundColor(.accentColor)
                    }

                    if !item.romaji.isEmpty && item.romaji != "—" {
                        Text(item.romaji)
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }
                }
            }

            // Subtle Divider
            Divider()
                .padding(.vertical, 2)

            // Row 3: Indonesian Meaning & Speaker Button
            HStack(alignment: .center, spacing: 8) {
                Text(item.indonesian)
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.primary)
                    .lineLimit(2)

                Spacer()

                Button(action: onPlayAudio) {
                    Image(systemName: "speaker.wave.2.circle.fill")
                        .font(.system(size: 26))
                        .foregroundColor(.accentColor)
                }
                .buttonStyle(BorderlessButtonStyle())
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color(UIColor.secondarySystemGroupedBackground))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.primary.opacity(0.08), lineWidth: 1)
        )
        .padding(.vertical, 2)
    }
}
