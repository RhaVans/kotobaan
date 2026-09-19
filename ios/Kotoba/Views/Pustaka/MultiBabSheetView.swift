import SwiftUI

public struct MultiBabSheetView: View {
    @Binding public var selectedBabs: Set<Int>
    public let onApply: () -> Void
    @Environment(\.dismiss) private var dismiss

    @State private var localSelection: Set<Int> = []

    public init(selectedBabs: Binding<Set<Int>>, onApply: @escaping () -> Void) {
        self._selectedBabs = selectedBabs
        self.onApply = onApply
    }

    private let columns = [
        GridItem(.adaptive(minimum: 70), spacing: 10)
    ]

    public var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                // Header action bar
                HStack {
                    Button("Pilih Semua") {
                        localSelection = Set(1...25)
                    }
                    .font(.subheadline)

                    Spacer()

                    Button("Hapus Pilihan") {
                        localSelection.removeAll()
                    }
                    .font(.subheadline)
                    .foregroundColor(.red)
                }
                .padding(.horizontal, 20)
                .padding(.top, 12)

                Divider()

                ScrollView {
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(1...25, id: \.self) { bab in
                            let isSelected = localSelection.contains(bab)
                            Button(action: {
                                if isSelected {
                                    localSelection.remove(bab)
                                } else {
                                    localSelection.insert(bab)
                                }
                            }) {
                                Text("Bab \(bab)")
                                    .font(.subheadline)
                                    .fontWeight(isSelected ? .bold : .regular)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(isSelected ? Color.accentColor : Color(.secondarySystemBackground))
                                    .foregroundColor(isSelected ? .white : .primary)
                                    .cornerRadius(12)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(isSelected ? Color.clear : Color(.tertiarySystemFill), lineWidth: 1)
                                    )
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 8)
                }

                Divider()

                // Apply button
                Button(action: {
                    selectedBabs = localSelection
                    onApply()
                    dismiss()
                }) {
                    Text(localSelection.isEmpty ? "Tampilkan Semua Bab" : "Terapkan (\(localSelection.count) Bab Terpilih)")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(14)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 16)
            }
            .navigationTitle("Filter Bab")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Batal") {
                        dismiss()
                    }
                }
            }
            .onAppear {
                localSelection = selectedBabs
            }
        }
    }
}
