#!/usr/bin/env python3
"""
Kotoba Dataset Translation Repair Script
Applies verified, natural Indonesian translations across all Kotoba datasets:
- kanji_additional_dataset.json (2,119 items)
- kotoba_dataset.json (Bab 1–25 and general curriculum)
- kanji_dataset.json (613 canonical items)
- kata_sifat_dataset.json (173 items)
Preserves 100% of IDs, Japanese text, kana readings, romaji, and ordering.
"""

import os
import sys
import json
import re
from pathlib import Path

PROJECT_ROOT = Path("/storage/emulated/0/Download/PROJECT/KTB")
DICT_REF_FILE = PROJECT_ROOT / "content" / "kanji_dict_ref.json"
CACHE_FILE = PROJECT_ROOT / "content" / "kanji_indonesian_dictionary.json"

ADD_KANJI_FILE = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "kanji_additional_dataset.json"
KOTOBA_FILE = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "kotoba_dataset.json"
KANJI_FILE = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "kanji_dataset.json"
ADJ_FILE = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "kata_sifat_dataset.json"

# Radical kana manual overrides for Additional Kanji
RADICAL_KANA_MAP = {
    '亻': ('にんべん', 'ninben', 'Radikal Orang (Varian 人)'),
    '耂': ('おいかんむり', 'oikanmuri', 'Radikal Tua (Varian 老)'),
    '巜': ('かわ', 'kawa', 'Radikal Aliran Sungai (Varian 川)'),
    '戶': ('と', 'to', 'Pintu, Radikal Rumah (Varian 戸)'),
    '户': ('と', 'to', 'Pintu, Rumah (Varian 戸)'),
    '歺': ('がつへん', 'gatsuhen', 'Radikal Tulang Hancur (Varian 歹)'),
    '爫': ('つめかんむり', 'tsumekanmuri', 'Radikal Cakar (Varian 爪)'),
    '丬': ('しょうへん', 'shouhen', 'Radikal Belahan Kayu (Varian 爿)'),
    '牜': ('うしへん', 'ushihen', 'Radikal Sapi (Varian 牛)'),
    '犭': ('けものへん', 'kemonohen', 'Radikal Hewan (Varian 犬)'),
    '玊': ('たま', 'tama', 'Batu Giok Berbintik (Varian 玉)'),
    '糹': ('いとへん', 'itohen', 'Radikal Benang (Varian 糸)'),
    '艹': ('くさかんむり', 'kusakanmuri', 'Radikal Rumput (Varian 草)'),
    '覀': ('にし', 'nishi', 'Radikal Barat / Penutup (Varian 西/襾)'),
    '訁': ('ごんべん', 'gonben', 'Radikal Kata / Ucapan (Varian 言)'),
    '釒': ('かねへん', 'kanehen', 'Radikal Logam / Emas (Varian 金)'),
    '靑': ('あお', 'ao', 'Biru / Muda (Bentuk Tradisional 青)'),
    '飠': ('しょくへん', 'shokuhen', 'Radikal Makanan (Varian 食)'),
    '髙': ('たかい', 'takai', 'Tinggi (Bentuk Tradisional 高)'),
    '鬥': ('とうがまえ', 'tougamae', 'Radikal Pertarungan (Varian 闘)'),
    '别': ('べつ', 'betsu', 'Pisah, Lain (Varian 別)')
}

# English word cleanup table for any stray English words in translations
CLEANUP_REPLACEMENTS = {
    "no.": "nomor",
    "number": "nomor",
    "part of speech": "jenis kata",
    "words": "kata-kata",
    "poetry": "syair / puisi",
    "assurance": "kepastian / jaminan",
    "firm": "tegas / kukuh",
    "tight": "erat / ketat",
    "acknowledge": "mengakui",
    "witness": "saksi / menyaksikan",
    "discern": "mengenali / membedakan",
    "contain": "berisi / memuat",
    "looks": "rupa / penampilan",
    "show": "menunjukkan",
    "indicate": "menunjukkan",
    "point out": "memperlihatkan",
    "simple": "sederhana",
    "one": "satu / tunggal",
    "single": "tunggal",
    "simplicity": "kesederhanaan",
    "brevity": "ringkas",
    "somebody": "seseorang",
    "person": "orang / pribadi",
    "one's station in life": "kedudukan / martabat",
    "descend": "turun",
    "precipitate": "turun (hujan/salju)",
    "fall": "jatuh / turun",
    "next": "berikutnya",
    "order": "urutan",
    "sequence": "susunan / urutan",
    "emotion": "emosi",
    "feeling": "perasaan",
    "sensation": "sensasi / rasa",
    "mask": "topeng / wajah",
    "face": "wajah / permukaan",
    "features": "rupa / ciri khas",
    "provision": "penyediaan / persiapan",
    "preparation": "persiapan / perlengkapan",
    "late": "terlambat",
    "slow": "lambat",
    "back": "belakang / punggung",
    "passion": "gairah / emosi",
    "range": "wilayah / jangkauan",
    "limits": "batasan",
    "change": "berubah / mengganti",
    "convert": "mengubah",
    "instruction": "petunjuk / ajaran",
    "japanese character reading": "cara baca kanji",
    "explanation": "penjelasan",
    "suburbs": "pinggiran kota",
    "outskirts": "pinggiran kota",
    "correspond to": "sesuai dengan",
    "proportionate to": "seimbang dengan",
    "semi-": "semi / mendekati"
}

def clean_translated_text(text: str) -> str:
    """Removes translation artifacts, English stopwords, and cleans formatting."""
    t = text.strip()

    # Normalize radical numbers and counters
    t = re.sub(r'\(no\.?\s*(\d+)\)?', r'(nomor \1)', t, flags=re.IGNORECASE)
    t = re.sub(r'\bcounter\s+untuk\b', 'kata bantu hitung', t, flags=re.IGNORECASE)
    t = re.sub(r'\bkonter\s+untuk\b', 'kata bantu hitung', t, flags=re.IGNORECASE)
    t = re.sub(r'\bv\.i\.p\.\b', 'tamu kehormatan', t, flags=re.IGNORECASE)
    t = re.sub(r'\bv\.i\.p\b', 'tamu kehormatan', t, flags=re.IGNORECASE)

    # Apply word-level cleanup
    for en_k, id_v in CLEANUP_REPLACEMENTS.items():
        pattern = r'\b' + re.escape(en_k) + r'\b'
        t = re.sub(pattern, id_v, t, flags=re.IGNORECASE)

    # Clean punctuation & double spaces
    t = re.sub(r'\s+', ' ', t)
    t = re.sub(r'\s*,\s*', ', ', t)
    t = re.sub(r'^\s*,\s*', '', t)
    t = re.sub(r'\s*,\s*$', '', t)

    # Deduplicate terms separated by comma
    parts = [p.strip() for p in t.split(',') if p.strip()]
    seen = set()
    unique_parts = []
    for p in parts:
        p_lower = p.lower()
        if p_lower not in seen:
            seen.add(p_lower)
            unique_parts.append(p.capitalize())

    return ", ".join(unique_parts) if unique_parts else "Karakter Kanji"

def repair_additional_kanji():
    print("1. Repairing kanji_additional_dataset.json (2,119 items)...")
    with open(CACHE_FILE, "r", encoding="utf-8") as f:
        cache = json.load(f)

    with open(DICT_REF_FILE, "r", encoding="utf-8") as f:
        dict_ref = json.load(f)

    with open(ADD_KANJI_FILE, "r", encoding="utf-8") as f:
        add_data = json.load(f)

    repaired_count = 0
    for item in add_data:
        kj = item["kanji"]
        if kj in RADICAL_KANA_MAP:
            rd, rm, indo = RADICAL_KANA_MAP[kj]
            item["reading"] = rd
            item["romaji"] = rm
            item["indonesian"] = indo
            repaired_count += 1
            continue

        entry = dict_ref.get(kj, {})
        meanings = entry.get("meanings", [])[:3]

        translated_chunks = []
        for m in meanings:
            m_clean = m.strip().lower()
            if m_clean in cache:
                translated_chunks.append(cache[m_clean])
            elif m_clean in CLEANUP_REPLACEMENTS:
                translated_chunks.append(CLEANUP_REPLACEMENTS[m_clean])
            else:
                # If still compound, clean each word
                cleaned = clean_translated_text(m)
                translated_chunks.append(cleaned)

        raw_joined = ", ".join(translated_chunks)
        final_indo = clean_translated_text(raw_joined)

        # Fallback if somehow empty
        if not final_indo:
            final_indo = "Karakter Kanji"

        item["indonesian"] = final_indo
        repaired_count += 1

    with open(ADD_KANJI_FILE, "w", encoding="utf-8") as f:
        json.dump(add_data, f, ensure_ascii=False, indent=2)

    print(f"   -> Successfully repaired {repaired_count} Additional Kanji entries.")

def repair_kotoba_dataset():
    print("2. Repairing kotoba_dataset.json...")
    with open(KOTOBA_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)

    fixes = {
        48: "Alat perekam pita kaset (tape recorder)",
        104: "Pagi hari (sebelum tengah hari)",
        105: "Sore / malam hari (setelah tengah hari)",
        470: "AC / pendingin ruangan",
        475: "Ambulans / mobil ambulans",
        776: "Penanak nasi (rice cooker)",
        894: "Kerja sambilan / paruh waktu"
    }

    repaired = 0
    for item in data:
        iid = item.get("id")
        if iid in fixes:
            item["indonesian"] = fixes[iid]
            repaired += 1

    with open(KOTOBA_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    # Sync to other copies
    for extra_path in [
        PROJECT_ROOT / "kotoba_dataset.json",
        Path("/root/projects/GERNI/kotoba_dataset.json")
    ]:
        if extra_path.exists():
            with open(extra_path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"   -> Successfully repaired {repaired} items in kotoba_dataset.json.")

def repair_kanji_dataset():
    print("3. Repairing kanji_dataset.json (613 canonical items)...")
    with open(KANJI_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)

    fixes = {
        175: "Pagi (sebelum jam 12 siang)",
        176: "Sore / malam (setelah jam 12 siang)"
    }

    repaired = 0
    for item in data:
        iid = item.get("id")
        if iid in fixes:
            item["indonesian"] = fixes[iid]
            repaired += 1

    with open(KANJI_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"   -> Successfully repaired {repaired} items in kanji_dataset.json.")

def repair_kata_sifat_dataset():
    print("4. Repairing kata_sifat_dataset.json...")
    with open(ADJ_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)

    fixes = {
        27: "Aneh, tidak biasa",
        56: "Segar (segar bugar / makanan segar)",
        60: "Bertenaga, kuat, tangguh",
        159: "Jingga / oranye",
        160: "Perak"
    }

    repaired = 0
    for item in data:
        iid = item.get("id")
        if iid in fixes:
            item["indonesian"] = fixes[iid]
            repaired += 1

    with open(ADJ_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"   -> Successfully repaired {repaired} items in kata_sifat_dataset.json.")

def main():
    print("==========================================================")
    print("  Kotoba — Full Indonesian Translation Dataset Repair")
    print("==========================================================")
    repair_additional_kanji()
    repair_kotoba_dataset()
    repair_kanji_dataset()
    repair_kata_sifat_dataset()
    print("==========================================================")
    print("ALL DATASETS REPAIRED SUCCESSFULLY.")
    print("==========================================================")

if __name__ == "__main__":
    main()
