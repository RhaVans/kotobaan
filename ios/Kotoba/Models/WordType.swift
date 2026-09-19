import Foundation

public enum WordType: String, CaseIterable, Identifiable, Codable {
    case kataBenda = "KATA_BENDA"
    case kataKerja = "KATA_KERJA"
    case kataSifatI = "KATA_SIFAT_I"
    case kataSifatNa = "KATA_SIFAT_NA"
    case kataKeterangan = "KATA_KETERANGAN"
    case kataSambung = "KATA_SAMBUNG"
    case partikel = "PARTIKEL"
    case ungkapan = "UNGKAPAN"
    case lainnya = "LAINNYA"

    public var id: String { rawValue }

    public var displayName: String {
        switch self {
        case .kataBenda:
            return "Kata Benda"
        case .kataKerja:
            return "Kata Kerja"
        case .kataSifatI:
            return "Kata Sifat - い"
        case .kataSifatNa:
            return "Kata Sifat - な"
        case .kataKeterangan:
            return "Kata Keterangan"
        case .kataSambung:
            return "Kata Sambung"
        case .partikel:
            return "Partikel"
        case .ungkapan:
            return "Ungkapan"
        case .lainnya:
            return "Lainnya"
        }
    }

    public static func fromString(_ val: String?) -> WordType {
        guard let val = val?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() else {
            return .lainnya
        }
        return WordType(rawValue: val) ?? .lainnya
    }
}
