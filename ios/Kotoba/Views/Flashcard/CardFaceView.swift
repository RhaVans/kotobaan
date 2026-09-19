import SwiftUI

public struct CardFaceView: View {
    public let item: LearningObject
    public let isBack: Bool
    public let showFurigana: Bool
    public let showRomaji: Bool
    public let onFrontSpeakerTap: () -> Void
    public let onBackSpeakerTap: () -> Void

    public init(
        item: LearningObject,
        isBack: Bool,
        showFurigana: Bool,
        showRomaji: Bool,
        onSpeakerTap: @escaping () -> Void
    ) {
        self.item = item
        self.isBack = isBack
        self.showFurigana = showFurigana
        self.showRomaji = showRomaji
        self.onFrontSpeakerTap = onSpeakerTap
        self.onBackSpeakerTap = onSpeakerTap
    }

    public init(
        item: LearningObject,
        isBack: Bool,
        showFurigana: Bool,
        showRomaji: Bool,
        onFrontSpeakerTap: @escaping () -> Void,
        onBackSpeakerTap: @escaping () -> Void
    ) {
        self.item = item
        self.isBack = isBack
        self.showFurigana = showFurigana
        self.showRomaji = showRomaji
        self.onFrontSpeakerTap = onFrontSpeakerTap
        self.onBackSpeakerTap = onBackSpeakerTap
    }

    public var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(Color(.secondarySystemBackground))
                .shadow(color: Color.black.opacity(0.12), radius: 12, x: 0, y: 6)

            VStack(spacing: 20) {
                // Top badge row
                HStack {
                    if !item.badgeLabel.isEmpty {
                        Text(item.badgeLabel)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color.accentColor.opacity(0.15))
                            .foregroundColor(.accentColor)
                            .clipShape(Capsule())
                    }

                    Spacer()

                    Text(item.wordType.displayName)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(.tertiarySystemBackground))
                        .clipShape(Capsule())
                }
                .padding(.horizontal, 24)
                .padding(.top, 24)

                Spacer()

                if !isBack {
                    // FRONT FACE: Kanji / Primary Ideograph
                    VStack(spacing: 12) {
                        if showFurigana && item.hasKanji && !item.reading.isEmpty {
                            Text(item.reading)
                                .font(.title3)
                                .foregroundColor(.secondary)
                        }

                        Text(item.japanese)
                            .font(.system(size: item.japanese.count > 6 ? 36 : 48, weight: .bold, design: .serif))
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 16)

                        if showRomaji && !item.romaji.isEmpty {
                            Text(item.romaji)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }

                    Button(action: onFrontSpeakerTap) {
                        Image(systemName: "speaker.wave.2.circle.fill")
                            .font(.system(size: 38))
                            .foregroundColor(.accentColor)
                    }
                    .padding(.top, 8)

                } else {
                    // BACK FACE: Answer face (Reading + Meaning NEVER disappear)
                    VStack(spacing: 12) {
                        if item.hasKanji {
                            // Prominently display the reading as the primary answer
                            Text(item.reading)
                                .font(.system(size: 36, weight: .bold, design: .serif))
                                .foregroundColor(.primary)
                                .multilineTextAlignment(.center)

                            // Kanji ideograph shown as reference
                            Text(item.japanese)
                                .font(.title3)
                                .foregroundColor(.secondary)
                        } else {
                            Text(item.japanese)
                                .font(.system(size: 36, weight: .bold, design: .serif))
                                .foregroundColor(.primary)
                                .multilineTextAlignment(.center)
                        }

                        if showRomaji && !item.romaji.isEmpty {
                            Text(item.romaji)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }

                        Divider()
                            .frame(width: 140)
                            .padding(.vertical, 4)

                        Text(item.indonesian)
                            .font(.system(size: 24, weight: .semibold, design: .default))
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 20)

                        if !item.formula.isEmpty {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Pola Kalimat:")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(.secondary)
                                Text(item.formula)
                                    .font(.footnote)
                                    .foregroundColor(.primary)
                            }
                            .padding(10)
                            .background(Color(.tertiarySystemBackground))
                            .cornerRadius(8)
                            .padding(.horizontal, 24)
                        }

                        if !item.explanation.isEmpty {
                            Text(item.explanation)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 24)
                        }

                        Button(action: onBackSpeakerTap) {
                            Image(systemName: "speaker.wave.2.circle.fill")
                                .font(.system(size: 32))
                                .foregroundColor(.accentColor)
                        }
                        .padding(.top, 4)
                    }
                    .rotation3DEffect(.degrees(180), axis: (x: 0, y: 1, z: 0))
                }

                Spacer()

                // Tap hint
                Text(isBack ? "Ketuk untuk balik ke depan" : "Ketuk untuk melihat arti")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .padding(.bottom, 16)
                    .rotation3DEffect(.degrees(isBack ? 180 : 0), axis: (x: 0, y: 1, z: 0))
            }
        }
        .frame(maxWidth: .infinity, maxHeight: 420)
        .padding(.horizontal, 20)
    }
}
