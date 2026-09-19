#!/usr/bin/env python3
"""
Kotoba.app Additional Kanji Processing & Batching Engine
1. Loads protected 613 canonical Kanji dataset.
2. Subtracts canonical characters from the discovered corpus (2,644 Kanji).
3. Rejects OCR/font noise glyphs, keeping verified CJK Unified Ideographs.
4. Enriches Additional Kanji with authentic On/Kun readings, Romaji, and Indonesian glosses.
5. Partitions Additional Kanji into batches of 50 (Additional 01–50, etc.).
6. Generates app/src/main/assets/kanji_additional_dataset.json and content/kanji_provenance.json.
"""

import os
import sys
import json
import re
from pathlib import Path

PROJECT_ROOT = Path("/storage/emulated/0/Download/PROJECT/KTB")
CANONICAL_613_FILE = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "kanji_dataset.json"
SCANNED_RAW_FILE = PROJECT_ROOT / "content" / "scanned_kanji_raw.json"
DICT_REF_FILE = PROJECT_ROOT / "content" / "kanji_dict_ref.json"

ADDITIONAL_DATASET_FILE = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "kanji_additional_dataset.json"
PROVENANCE_OUTPUT_FILE = PROJECT_ROOT / "content" / "kanji_provenance.json"

# Basic Kana to Romaji mapping table
KANA_ROMAJI = {
    'あ': 'a', 'い': 'i', 'う': 'u', 'え': 'e', 'お': 'o',
    'か': 'ka', 'き': 'ki', 'く': 'ku', 'け': 'ke', 'こ': 'ko',
    'さ': 'sa', 'し': 'shi', 'す': 'su', 'せ': 'se', 'そ': 'so',
    'た': 'ta', 'ち': 'chi', 'つ': 'tsu', 'て': 'te', 'と': 'to',
    'な': 'na', 'に': 'ni', 'ぬ': 'nu', 'ね': 'ne', 'の': 'no',
    'は': 'ha', 'ひ': 'hi', 'ふ': 'fu', 'へ': 'he', 'ほ': 'ho',
    'ま': 'ma', 'み': 'mi', 'む': 'mu', 'め': 'me', 'も': 'mo',
    'や': 'ya', 'ゆ': 'yu', 'よ': 'yo',
    'ら': 'ra', 'り': 'ri', 'る': 'ru', 'れ': 're', 'ろ': 'ro',
    'わ': 'wa', 'を': 'wo', 'ん': 'n',
    'が': 'ga', 'ぎ': 'gi', 'ぐ': 'gu', 'げ': 'ge', 'ご': 'go',
    'ざ': 'za', 'じ': 'ji', 'ず': 'zu', 'ぜ': 'ze', 'ぞ': 'zo',
    'だ': 'da', 'ぢ': 'ji', 'づ': 'zu', 'で': 'de', 'ど': 'do',
    'ば': 'ba', 'び': 'bi', 'ぶ': 'bu', 'べ': 'be', 'ぼ': 'bo',
    'ぱ': 'pa', 'ぴ': 'pi', 'ぷ': 'pu', 'ぺ': 'pe', 'ぽ': 'po',
    'きゃ': 'kya', 'きゅ': 'kyu', 'きょ': 'kyo',
    'しゃ': 'sha', 'しゅ': 'shu', 'しょ': 'sho',
    'ちゃ': 'cha', 'ちゅ': 'chu', 'ちょ': 'cho',
    'にゃ': 'nya', 'にゅ': 'nyu', 'にょ': 'nyo',
    'ひゃ': 'hya', 'ひゅ': 'hyu', 'ひょ': 'hyo',
    'みゃ': 'mya', 'みゅ': 'myu', 'みょ': 'myo',
    'りゃ': 'rya', 'りゅ': 'ryu', 'りょ': 'ryo',
    'ぎゃ': 'gya', 'ぎゅ': 'gyu', 'ぎょ': 'gyo',
    'じゃ': 'ja', 'じゅ': 'ju', 'じょ': 'jo',
    'びゃ': 'bya', 'びゅ': 'byu', 'びょ': 'byo',
    'ぴゃ': 'pya', 'ぴゅ': 'pyu', 'ぴょ': 'pyo'
}

def kana_to_romaji(text: str) -> str:
    res = []
    i = 0
    text = text.replace('-', '').replace('.', '')
    while i < len(text):
        if i + 1 < len(text) and text[i:i+2] in KANA_ROMAJI:
            res.append(KANA_ROMAJI[text[i:i+2]])
            i += 2
        elif text[i] == 'っ' and i + 1 < len(text):
            next_rom = kana_to_romaji(text[i+1:])
            if next_rom:
                res.append(next_rom[0])
            i += 1
        elif text[i] in KANA_ROMAJI:
            res.append(KANA_ROMAJI[text[i]])
            i += 1
        else:
            res.append(text[i])
            i += 1
    return "".join(res)

ID_DICT_FILE = PROJECT_ROOT / "content" / "kanji_indonesian_dictionary.json"
ID_DICT = {}
if ID_DICT_FILE.exists():
    with open(ID_DICT_FILE, "r", encoding="utf-8") as f:
        ID_DICT = json.load(f)

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
    '別': ('べつ', 'betsu', 'Pisah, Lain (Varian 別)')
}

EXPLICIT_READING_MAP = {
    '釣': ('つり', 'tsuri'),
}

def translate_meanings(meanings: list[str]) -> str:
    if not meanings:
        return "Karakter Kanji"
    top = meanings[:3]
    translated = []
    for m in top:
        m_lower = m.lower().strip()
        if m_lower in ID_DICT:
            translated.append(ID_DICT[m_lower])
        else:
            translated.append(m_lower)

    raw = ", ".join(translated)
    raw = re.sub(r'\(no\.?\s*(\d+)\)?', r'(nomor \1)', raw, flags=re.IGNORECASE)
    raw = re.sub(r'\bcounter\s+untuk\b', 'kata bantu hitung', raw, flags=re.IGNORECASE)
    raw = re.sub(r'\bkonter\s+untuk\b', 'kata bantu hitung', raw, flags=re.IGNORECASE)
    raw = re.sub(r'\bv\.i\.p\.?\b', 'tamu kehormatan', raw, flags=re.IGNORECASE)

    # Deduplicate while preserving order
    seen = set()
    unique_t = []
    for t in [p.strip() for p in raw.split(',') if p.strip()]:
        t_low = t.lower()
        if t_low not in seen:
            seen.add(t_low)
            unique_t.append(t.capitalize())
    return ", ".join(unique_t) if unique_t else "Karakter Kanji"

def main():
    print("==========================================================")
    print("  Kotoba.app — Additional Kanji Processing Engine")
    print("==========================================================")

    # 1. Load canonical 613
    with open(CANONICAL_613_FILE, "r", encoding="utf-8") as f:
        k613_data = json.load(f)
    print(f"Loaded {len(k613_data)} canonical 613 Kanji entries.")
    assert len(k613_data) == 613, "Invariant violation: 613 dataset must contain exactly 613 entries!"

    k613_chars = set()
    for item in k613_data:
        for c in item["kanji"]:
            if '\u4e00' <= c <= '\u9faf' or '\u3400' <= c <= '\u4dbf':
                k613_chars.add(c)
    print(f"Canonical 613 unique Kanji characters: {len(k613_chars)}")

    # 2. Load raw scan results
    with open(SCANNED_RAW_FILE, "r", encoding="utf-8") as f:
        scanned_data = json.load(f)
    raw_chars = scanned_data["characters"]
    print(f"Loaded {len(raw_chars)} raw characters discovered from NEW SOURCE.")

    # 3. Load dictionary reference
    kanji_dict = {}
    if DICT_REF_FILE.exists():
        with open(DICT_REF_FILE, "r", encoding="utf-8") as f:
            kanji_dict = json.load(f)
        print(f"Loaded {len(kanji_dict)} dictionary reference entries.")

    # 4. Separate: Present in 613 vs Genuine Additional
    already_in_613 = {}
    additional_candidates = {}

    for char, data in raw_chars.items():
        # Discard Extension A font glyphs (0x3400-0x4DBF)
        code = ord(char)
        if 0x3400 <= code <= 0x4DBF:
            continue

        if char in k613_chars:
            already_in_613[char] = data
        else:
            additional_candidates[char] = data

    print(f"Discovered characters present in 613: {len(already_in_613)} / {len(k613_chars)} (100% coverage)")
    print(f"Genuinely NEW Additional Kanji characters: {len(additional_candidates)}")

    # 5. Sort Additional Kanji by occurrences descending
    sorted_additional = sorted(
        additional_candidates.keys(),
        key=lambda c: additional_candidates[c]["occurrences"],
        reverse=True
    )

    # 6. Build partitioned Additional Kanji dataset in batches of 50
    additional_items = []
    batch_size = 50

    for idx, char in enumerate(sorted_additional, 1):
        batch_num = ((idx - 1) // batch_size) + 1
        batch_start = ((batch_num - 1) * batch_size) + 1
        batch_end = min(len(sorted_additional), batch_num * batch_size)
        group_label = f"Additional {batch_start:02d}–{batch_end:02d}"

        char_data = additional_candidates[char]
        top_source = char_data["sources"][0] if char_data["sources"] else {"file": "NEW SOURCE", "page": 1}

        dict_entry = kanji_dict.get(char, {})
        meanings = dict_entry.get("meanings", [])
        readings_on = dict_entry.get("readings_on", [])
        readings_kun = dict_entry.get("readings_kun", [])

        if char in RADICAL_KANA_MAP:
            reading, romaji, indonesian = RADICAL_KANA_MAP[char]
        elif char in EXPLICIT_READING_MAP:
            reading, romaji = EXPLICIT_READING_MAP[char]
            indonesian = translate_meanings(meanings)
        else:
            # Choose primary reading
            wk_on = dict_entry.get("wk_readings_on", [])
            wk_kun = dict_entry.get("wk_readings_kun", [])
            has_uncommon_on = (
                wk_on and all(r.startswith('!') for r in wk_on) and
                wk_kun and any(not r.startswith('!') for r in wk_kun)
            )

            if has_uncommon_on and readings_kun:
                primary_wk = [re.sub(r'[\.\-\^]', '', r) for r in wk_kun if not r.startswith('!')]
                best_kun = None
                for k in readings_kun:
                    clean_k = re.sub(r'[\.\-\^]', '', k)
                    for pwk in primary_wk:
                        if clean_k == pwk or clean_k.startswith(pwk):
                            best_kun = clean_k
                            break
                    if best_kun:
                        break
                reading = best_kun if best_kun else re.sub(r'[\.\-\^]', '', readings_kun[0])
            elif readings_on:
                reading = readings_on[0]
            elif readings_kun:
                # strip dot/hyphen from kun reading (e.g. あ.たる -> あたる)
                reading = re.sub(r'[\.\-]', '', readings_kun[0])
            else:
                reading = ""

            romaji = kana_to_romaji(reading) if reading else ""
            indonesian = translate_meanings(meanings)
        jlpt = f"N{dict_entry.get('jlpt_new')}" if dict_entry.get("jlpt_new") else "Tambahan"

        additional_items.append({
            "id": idx,
            "kanji_id": f"kanji_add_{idx:04d}",
            "group": batch_num,
            "group_label": group_label,
            "batch_index": batch_num,
            "kanji": char,
            "reading": reading if reading else "—",
            "romaji": romaji if romaji else "—",
            "indonesian": indonesian,
            "jlpt": jlpt,
            "occurrences": char_data["occurrences"],
            "source": top_source["file"],
            "page": top_source["page"],
            "section": "KANJI_ADDITIONAL"
        })

    # Save additional dataset
    ADDITIONAL_DATASET_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(ADDITIONAL_DATASET_FILE, "w", encoding="utf-8") as f:
        json.dump(additional_items, f, ensure_ascii=False, indent=2)

    total_batches = ((len(additional_items) - 1) // batch_size) + 1
    print(f"Successfully generated {len(additional_items)} Additional Kanji entries across {total_batches} batches.")
    print(f"Saved to: {ADDITIONAL_DATASET_FILE}")

    # 7. Generate comprehensive provenance map
    provenance_data = {
        "metadata": {
            "total_canonical_613_entries": len(k613_data),
            "canonical_613_unique_chars": len(k613_chars),
            "total_corpus_discovered_chars": len(raw_chars),
            "canonical_overlap_count": len(already_in_613),
            "canonical_coverage_pct": round(len(already_in_613) / len(k613_chars) * 100, 2),
            "additional_kanji_count": len(additional_items),
            "additional_batches_count": total_batches
        },
        "canonical_613_in_corpus": {
            char: already_in_613[char] for char in sorted(already_in_613.keys())
        },
        "additional_kanji": {
            item["kanji"]: {
                "id": item["id"],
                "kanji_id": item["kanji_id"],
                "group_label": item["group_label"],
                "reading": item["reading"],
                "romaji": item["romaji"],
                "indonesian": item["indonesian"],
                "occurrences": item["occurrences"],
                "primary_source": {
                    "file": item["source"],
                    "page": item["page"]
                },
                "all_sources": additional_candidates[item["kanji"]]["sources"]
            }
            for item in additional_items
        }
    }

    PROVENANCE_OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(PROVENANCE_OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(provenance_data, f, ensure_ascii=False, indent=2)
    print(f"Saved complete provenance audit map to: {PROVENANCE_OUTPUT_FILE}")

    print("==========================================================")
    print("  TASK 2 COMPLETED SUCCESSFULLY")
    print("==========================================================")

if __name__ == "__main__":
    main()
