import SwiftUI

public struct FlashcardView: View {
    @StateObject private var viewModel = FlashcardViewModel()
    @EnvironmentObject private var appState: AppState
    @State private var showBabPickerSheet: Bool = false

    public init() {}

    // MARK: - Computed helpers

    private var canGoPrev: Bool {
        (viewModel.engine?.currentIndex ?? 0) > 0
    }

    // MARK: - Body

    public var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                // Top Status Bar
                if let engine = viewModel.engine {
                    VStack(spacing: 8) {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(viewModel.deckTitle)
                                    .font(.headline)
                                    .foregroundColor(.primary)

                                if engine.isRecoveryRound {
                                    Text("Putaran Pemulihan #\(engine.cycleIteration)")
                                        .font(.caption)
                                        .fontWeight(.bold)
                                        .foregroundColor(.orange)
                                } else {
                                    Text("Kartu \(min(engine.currentIndex + 1, engine.currentPassTotal)) dari \(engine.currentPassTotal)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }

                            Spacer()

                            // Ingat / Lupa counter badges
                            HStack(spacing: 8) {
                                Label("\(engine.totalIngatCount)", systemImage: "checkmark.circle.fill")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(.green)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.green.opacity(0.12))
                                    .clipShape(Capsule())

                                Label("\(engine.totalLupaCount)", systemImage: "xmark.circle.fill")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(.red)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.red.opacity(0.12))
                                    .clipShape(Capsule())
                            }
                        }
                        .padding(.horizontal, 24)

                        // Linear progress bar
                        GeometryReader { geo in
                            let total = max(1, engine.currentPassTotal)
                            let progress = CGFloat(engine.currentIndex) / CGFloat(total)
                            ZStack(alignment: .leading) {
                                Capsule()
                                    .fill(Color(.tertiarySystemFill))
                                    .frame(height: 6)

                                Capsule()
                                    .fill(engine.isRecoveryRound ? Color.orange : Color.accentColor)
                                    .frame(width: max(6, geo.size.width * progress), height: 6)
                                    .animation(.easeInOut(duration: 0.2), value: progress)
                            }
                        }
                        .frame(height: 6)
                        .padding(.horizontal, 24)
                    }
                    .padding(.top, 8)
                }

                Spacer()

                // Main Flashcard with 3D Flip & Drag
                if let item = viewModel.engine?.currentItem {
                    CardFaceView(
                        item: item,
                        isBack: viewModel.isFlipped,
                        frontMode: appState.frontMode,
                        showFurigana: appState.showFurigana,
                        showRomaji: appState.showRomaji,
                        onFrontSpeakerTap: {
                            viewModel.playFrontAudio()
                        },
                        onBackSpeakerTap: {
                            viewModel.playBackAudio()
                        }
                    )
                    .offset(viewModel.cardOffset)
                    .rotationEffect(.degrees(Double(viewModel.cardOffset.width / 20)))
                    .rotation3DEffect(
                        .degrees(viewModel.isFlipped ? 180 : 0),
                        axis: (x: 0.0, y: 1.0, z: 0.0)
                    )
                    .onTapGesture {
                        viewModel.flipCard()
                    }
                    .gesture(
                        DragGesture()
                            .onChanged { gesture in
                                viewModel.cardOffset = gesture.translation
                            }
                            .onEnded { gesture in
                                if gesture.translation.width > 120 {
                                    viewModel.handleIngat()
                                } else if gesture.translation.width < -120 {
                                    viewModel.handleLupa()
                                } else {
                                    withAnimation(.spring()) {
                                        viewModel.cardOffset = .zero
                                    }
                                }
                            }
                    )
                } else if viewModel.engine?.isComplete == true {
                    VStack(spacing: 16) {
                        Image(systemName: "checkmark.seal.fill")
                            .font(.system(size: 64))
                            .foregroundColor(.green)
                        Text("Selesai Belajar!")
                            .font(.title2)
                            .fontWeight(.bold)
                        Text("Semua kotoba dalam putaran ini telah berhasil diingat.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                        Button(action: { viewModel.restartSession() }) {
                            Text("Ulangi Putaran")
                                .fontWeight(.semibold)
                                .frame(minWidth: 160)
                                .padding()
                                .background(Color.accentColor)
                                .foregroundColor(.white)
                                .cornerRadius(12)
                        }
                    }
                    .padding()
                }

                Spacer()

                // Bottom Action Controls
                VStack(spacing: 12) {

                    // Row 1: Previous + Furigana/Romaji quick toggles
                    HStack(spacing: 0) {
                        // ← Previous button
                        Button(action: { viewModel.handlePrev() }) {
                            HStack(spacing: 4) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 13, weight: .semibold))
                                Text("Prev")
                                    .font(.caption)
                                    .fontWeight(.medium)
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(canGoPrev ? Color(.secondarySystemFill) : Color.clear)
                            .foregroundColor(canGoPrev ? .primary : Color(.tertiaryLabel))
                            .clipShape(Capsule())
                            .overlay(
                                Capsule()
                                    .stroke(canGoPrev ? Color(.separator) : Color.clear, lineWidth: 1)
                            )
                        }
                        .disabled(!canGoPrev)
                        .animation(.easeInOut(duration: 0.15), value: canGoPrev)

                        Spacer()

                        // Furigana & Romaji toggles
                        HStack(spacing: 12) {
                            Button(action: {
                                withAnimation { appState.showFurigana.toggle() }
                            }) {
                                HStack(spacing: 6) {
                                    Image(systemName: "textformat.size")
                                    Text("Furigana")
                                        .font(.caption)
                                        .fontWeight(.medium)
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(appState.showFurigana ? Color.accentColor.opacity(0.15) : Color(.tertiarySystemFill))
                                .foregroundColor(appState.showFurigana ? .accentColor : .secondary)
                                .clipShape(Capsule())
                            }

                            Button(action: {
                                withAnimation { appState.showRomaji.toggle() }
                            }) {
                                HStack(spacing: 6) {
                                    Image(systemName: "a.magnify")
                                    Text("Romaji")
                                        .font(.caption)
                                        .fontWeight(.medium)
                                }
                                .padding(.horizontal, 12)
                                .padding(.vertical, 6)
                                .background(appState.showRomaji ? Color.accentColor.opacity(0.15) : Color(.tertiarySystemFill))
                                .foregroundColor(appState.showRomaji ? .accentColor : .secondary)
                                .clipShape(Capsule())
                            }
                        }
                    }
                    .padding(.horizontal, 24)

                    // Row 2: Lupa | Ingat primary buttons
                    HStack(spacing: 20) {
                        Button(action: { viewModel.handleLupa() }) {
                            HStack {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.title3)
                                Text("Lupa")
                                    .fontWeight(.bold)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(Color.red.opacity(0.12))
                            .foregroundColor(.red)
                            .cornerRadius(16)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(Color.red.opacity(0.3), lineWidth: 1.5)
                            )
                        }

                        Button(action: { viewModel.handleIngat() }) {
                            HStack {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.title3)
                                Text("Ingat")
                                    .fontWeight(.bold)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(Color.green)
                            .foregroundColor(.white)
                            .cornerRadius(16)
                            .shadow(color: Color.green.opacity(0.3), radius: 6, x: 0, y: 3)
                        }
                    }
                    .padding(.horizontal, 24)
                }
                .padding(.bottom, 16)
            }
            .navigationTitle("Belajar")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Menu {
                        ForEach(CardFrontMode.allCases) { mode in
                            Button(action: {
                                withAnimation {
                                    appState.frontMode = mode
                                }
                            }) {
                                HStack {
                                    Text(mode.rawValue)
                                    if appState.frontMode == mode {
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack(spacing: 4) {
                            Image(systemName: "rectangle.stack.badge.play")
                            Text(appState.frontMode.rawValue)
                                .font(.caption)
                                .fontWeight(.medium)
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color(.secondarySystemFill))
                        .clipShape(Capsule())
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showBabPickerSheet = true }) {
                        Label("Pilih Bab", systemImage: "list.bullet.rectangle.portrait")
                    }
                }
            }
            .sheet(isPresented: $showBabPickerSheet) {
                NavigationStack {
                    List(viewModel.availableChapters) { chapter in
                        Button(action: {
                            viewModel.loadDeck(forBab: chapter.babNumber)
                            showBabPickerSheet = false
                        }) {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(chapter.displayLabel)
                                        .font(.headline)
                                        .foregroundColor(.primary)
                                    Text(chapter.theme)
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                                Text("\(chapter.vocabCount) kata")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color(.secondarySystemBackground))
                                    .cornerRadius(6)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                    .navigationTitle("Pilih Bab")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button("Tutup") { showBabPickerSheet = false }
                        }
                    }
                }
                .presentationDetents([.medium, .large])
            }
            .sheet(isPresented: $viewModel.showSummarySheet) {
                SummarySheetView(
                    deckTitle: viewModel.deckTitle,
                    ingatCount: viewModel.engine?.totalIngatCount ?? 0,
                    lupaCount: viewModel.engine?.totalLupaCount ?? 0,
                    accuracy: viewModel.engine?.accuracyPercentage ?? 100.0,
                    onRepeat: {
                        viewModel.restartSession()
                    },
                    onDismiss: {
                        viewModel.showSummarySheet = false
                    }
                )
                .presentationDetents([.fraction(0.45)])
            }
        }
    }
}

public struct SummarySheetView: View {
    public let deckTitle: String
    public let ingatCount: Int
    public let lupaCount: Int
    public let accuracy: Double
    public let onRepeat: () -> Void
    public let onDismiss: () -> Void

    public var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 52))
                .foregroundColor(.green)
                .padding(.top, 16)

            VStack(spacing: 6) {
                Text("Selesai Menghafal")
                    .font(.title2)
                    .fontWeight(.bold)

                Text(deckTitle)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            HStack(spacing: 24) {
                VStack {
                    Text("\(ingatCount)")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.green)
                    Text("Ingat")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Divider().frame(height: 32)

                VStack {
                    Text("\(lupaCount)")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.red)
                    Text("Lupa")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Divider().frame(height: 32)

                VStack {
                    Text(String(format: "%.0f%%", accuracy))
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.accentColor)
                    Text("Akurasi")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.vertical, 8)

            HStack(spacing: 16) {
                Button(action: {
                    onDismiss()
                }) {
                    Text("Selesai")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(.secondarySystemBackground))
                        .foregroundColor(.primary)
                        .cornerRadius(12)
                }

                Button(action: {
                    onRepeat()
                }) {
                    Text("Ulangi Sesi")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 16)
        }
    }
}
