#!/usr/bin/env python3
"""
Kotoba.app JFT Resources Extractor
Extracts KATA KERJA JFT .pdf (549 verbs) and KATA SIFAT JFT.pdf (173 adjectives)
from NEW SOURCE into canonical JSON datasets partitioned into groups of 50.
"""

import os
import re
import json
import subprocess
from pathlib import Path

PROJECT_ROOT = Path("/storage/emulated/0/download/PROJECT/KTB")
SOURCE_DIR = PROJECT_ROOT / "NEW SOURCE"
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets"
ASSETS_DIR.mkdir(parents=True, exist_ok=True)

VERB_PDF = SOURCE_DIR / "KATA KERJA JFT .pdf"
ADJ_PDF = SOURCE_DIR / "KATA SIFAT JFT.pdf"

# Japanese Kana to Romaji Mapping
KANA_MAP = {
    "あ": "a", "い": "i", "う": "u", "え": "e", "お": "o",
    "か": "ka", "き": "ki", "く": "ku", "け": "ke", "こ": "ko",
    "さ": "sa", "し": "shi", "す": "su", "せ": "se", "そ": "so",
    "た": "ta", "ち": "chi", "つ": "tsu", "て": "te", "と": "to",
    "な": "na", "に": "ni", "ぬ": "nu", "ね": "ne", "の": "no",
    "は": "ha", "ひ": "hi", "ふ": "fu", "へ": "he", "ほ": "ho",
    "ま": "ma", "み": "mi", "む": "mu", "め": "me", "も": "mo",
    "や": "ya", "ゆ": "yu", "よ": "yo",
    "ら": "ra", "り": "ri", "る": "ru", "れ": "re", "ろ": "ro",
    "わ": "wa", "を": "wo", "ん": "n",
    "が": "ga", "ぎ": "gi", "ぐ": "gu", "げ": "ge", "ご": "go",
    "ざ": "za", "じ": "ji", "ず": "zu", "ぜ": "ze", "ぞ": "zo",
    "だ": "da", "ぢ": "ji", "づ": "zu", "で": "de", "ど": "do",
    "ば": "ba", "び": "bi", "ぶ": "bu", "べ": "be", "ぼ": "bo",
    "ぱ": "pa", "ぴ": "pi", "ぷ": "pu", "ぺ": "pe", "ぽ": "po",
    "きゃ": "kya", "きゅ": "kyu", "きょ": "kyo",
    "しゃ": "sha", "しゅ": "shu", "しょ": "sho",
    "ちゃ": "cha", "ちゅ": "chu", "ちょ": "cho",
    "にゃ": "nya", "にゅ": "nyu", "にょ": "nyo",
    "ひゃ": "hya", "ひゅ": "hyu", "ひょ": "hyo",
    "みゃ": "mya", "みゅ": "myu", "みょ": "myo",
    "りゃ": "rya", "りゅ": "ryu", "りょ": "ryo",
    "ぎゃ": "gya", "ぎゅ": "gyu", "ぎょ": "gyo",
    "じゃ": "ja", "じゅ": "ju", "じょ": "jo",
    "びゃ": "bya", "びゅ": "byu", "びょ": "byo",
    "ぴゃ": "pya", "ぴゅ": "pyu", "ぴょ": "pyo",
    "ア": "a", "イ": "i", "ウ": "u", "エ": "e", "オ": "o",
    "カ": "ka", "キ": "ki", "ク": "ku", "ケ": "ke", "コ": "ko",
    "サ": "sa", "シ": "shi", "ス": "su", "セ": "se", "ソ": "so",
    "タ": "ta", "チ": "chi", "ツ": "tsu", "テ": "te", "ト": "to",
    "ナ": "na", "ニ": "ni", "ヌ": "nu", "ネ": "ne", "ノ": "no",
    "ハ": "ha", "ヒ": "hi", "フ": "fu", "ヘ": "he", "ホ": "ho",
    "マ": "ma", "ミ": "mi", "ム": "mu", "メ": "me", "モ": "mo",
    "ヤ": "ya", "ユ": "yu", "ヨ": "yo",
    "ラ": "ra", "リ": "ri", "ル": "ru", "レ": "re", "ロ": "ro",
    "ワ": "wa", "ヲ": "wo", "ン": "n",
    "ガ": "ga", "ギ": "gi", "グ": "gu", "ゲ": "ge", "ご": "go",
    "ザ": "za", "ジ": "ji", "ズ": "zu", "ゼ": "ze", "ゾ": "zo",
    "ダ": "da", "ヂ": "ji", "ヅ": "zu", "デ": "de", "ド": "do",
    "バ": "ba", "ビ": "bi", "ブ": "bu", "ベ": "be", "ボ": "bo",
    "パ": "pa", "ピ": "pi", "プ": "pu", "ペ": "pe", "ポ": "po",
    "キャ": "kya", "キュ": "kyu", "キョ": "kyo",
    "シャ": "sha", "シュ": "shu", "ショ": "sho",
    "チャ": "cha", "チュ": "chu", "チョ": "cho",
    "ニャ": "nya", "ニュ": "nyu", "ニョ": "nyo",
    "ヒャ": "hya", "ヒュ": "hyu", "ヒョ": "hyo",
    "ミャ": "mya", "ミュ": "myu", "ミョ": "myo",
    "リャ": "rya", "リュ": "ryu", "リョ": "ryo",
    "ギャ": "gya", "ギュ": "gyu", "ギョ": "gyo",
    "ジャ": "ja", "ジュ": "ju", "ジョ": "jo",
    "ビャ": "bya", "ビュ": "byu", "ビョ": "byo",
    "ピャ": "pya", "ピュ": "pyu", "ピョ": "pyo",
    "ー": "-", "っ": "tsu", "ッ": "tsu"
}

def to_romaji(text):
    clean = text.replace(" (な)", "").replace("(な)", "").replace("（な）", "").replace("―", "").strip()
    res = []
    i = 0
    n = len(clean)
    while i < n:
        if i + 1 < n and clean[i:i+2] in KANA_MAP:
            res.append(KANA_MAP[clean[i:i+2]])
            i += 2
        elif clean[i] in KANA_MAP:
            if clean[i] in ["っ", "ッ"]:
                if i + 1 < n:
                    nxt = clean[i+1:i+3] if i + 2 < n and clean[i+1:i+3] in KANA_MAP else clean[i+1]
                    next_c = KANA_MAP.get(nxt, "")
                    if next_c:
                        res.append(next_c[0])
                    else:
                        res.append("t")
                else:
                    res.append("t")
                i += 1
            else:
                res.append(KANA_MAP[clean[i]])
                i += 1
        else:
            res.append(clean[i])
            i += 1
    return "".join(res)


def get_group_label(index, total, chunk_size=50):
    group_num = (index - 1) // chunk_size + 1
    start_num = (group_num - 1) * chunk_size + 1
    end_num = min(group_num * chunk_size, total)
    return group_num, f"{start_num:02d}–{end_num:02d}"


def extract_verbs():
    print(f"Extracting Verbs from {VERB_PDF}...")
    out = subprocess.check_output(["pdftotext", "-layout", str(VERB_PDF), "-"]).decode("utf-8")
    raw_verbs = []
    
    for line in out.splitlines():
        m = re.match(r"^\s*(\d+)\s+(.+)$", line)
        if not m:
            continue
        no = int(m.group(1))
        rest = m.group(2).strip()
        parts = re.split(r"\s{2,}", rest)
        if len(parts) == 2:
            indo, jepang = parts[0].strip(), parts[1].strip()
            kanji = "―"
        elif len(parts) >= 3:
            indo, jepang, kanji = parts[0].strip(), parts[1].strip(), parts[2].strip()
        else:
            continue
        raw_verbs.append((no, indo, jepang, kanji))
        
    raw_verbs.sort(key=lambda x: x[0])
    total_verbs = len(raw_verbs)
    print(f"   -> Parsed {total_verbs} verbs.")
    assert total_verbs == 549, f"Expected 549 verbs, got {total_verbs}"
    
    verb_records = []
    for no, indo, kana, kanji in raw_verbs:
        g_num, g_label = get_group_label(no, total_verbs, 50)
        display_jap = kanji if kanji and kanji != "―" else kana
        romaji = to_romaji(kana)
        verb_records.append({
            "id": no,
            "group": g_num,
            "group_label": g_label,
            "japanese": display_jap,
            "reading": kana,
            "kanji": kanji if kanji != "―" else "",
            "romaji": romaji,
            "indonesian": indo,
            "section": "KATA_KERJA",
            "word_type": "KATA_KERJA"
        })
        
    out_file = ASSETS_DIR / "kata_kerja_dataset.json"
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(verb_records, f, ensure_ascii=False, indent=2)
    print(f"   -> Saved {len(verb_records)} verbs to {out_file}")
    return verb_records


def extract_adjectives():
    print(f"Extracting Adjectives from {ADJ_PDF}...")
    out = subprocess.check_output(["pdftotext", "-layout", str(ADJ_PDF), "-"]).decode("utf-8")
    
    raw_na = []
    raw_i = []
    curr_type = "NA"
    
    for line in out.splitlines():
        if "KATA SIFAT I" in line:
            curr_type = "I"
            continue
        elif "KATA SIFAT NA" in line:
            curr_type = "NA"
            continue
            
        m = re.match(r"^\s*(\d+)\s+(.+)$", line)
        if not m:
            continue
        no = int(m.group(1))
        rest = m.group(2).strip()
        parts = re.split(r"\s{2,}", rest)
        if len(parts) == 2:
            indo, jepang = parts[0].strip(), parts[1].strip()
            kanji = "―"
        elif len(parts) >= 3:
            indo, jepang, kanji = parts[0].strip(), parts[1].strip(), parts[2].strip()
        else:
            continue
            
        if curr_type == "NA":
            raw_na.append((no, indo, jepang, kanji))
        else:
            raw_i.append((no, indo, jepang, kanji))
            
    raw_na.sort(key=lambda x: x[0])
    raw_i.sort(key=lambda x: x[0])
    
    print(f"   -> Parsed {len(raw_na)} NA adjectives, {len(raw_i)} I adjectives.")
    assert len(raw_na) == 64, f"Expected 64 NA adjectives, got {len(raw_na)}"
    assert len(raw_i) == 109, f"Expected 109 I adjectives, got {len(raw_i)}"
    
    adj_records = []
    global_id = 1
    
    # NA Adjectives
    for no, indo, kana, kanji in raw_na:
        g_num, g_label = get_group_label(global_id, 173, 50)
        display_jap = kanji if kanji and kanji != "―" else kana
        romaji = to_romaji(kana)
        adj_records.append({
            "id": global_id,
            "source_no": no,
            "group": g_num,
            "group_label": g_label,
            "subgroup_label": f"NA {(no - 1) // 50 + 1}",
            "japanese": display_jap,
            "reading": kana,
            "kanji": kanji if kanji != "―" else "",
            "romaji": romaji,
            "indonesian": indo,
            "section": "KATA_SIFAT",
            "word_type": "KATA_SIFAT_NA"
        })
        global_id += 1
        
    # I Adjectives
    for no, indo, kana, kanji in raw_i:
        g_num, g_label = get_group_label(global_id, 173, 50)
        display_jap = kanji if kanji and kanji != "―" else kana
        romaji = to_romaji(kana)
        adj_records.append({
            "id": global_id,
            "source_no": no,
            "group": g_num,
            "group_label": g_label,
            "subgroup_label": f"I {(no - 1) // 50 + 1}",
            "japanese": display_jap,
            "reading": kana,
            "kanji": kanji if kanji != "―" else "",
            "romaji": romaji,
            "indonesian": indo,
            "section": "KATA_SIFAT",
            "word_type": "KATA_SIFAT_I"
        })
        global_id += 1
        
    out_file = ASSETS_DIR / "kata_sifat_dataset.json"
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(adj_records, f, ensure_ascii=False, indent=2)
    print(f"   -> Saved {len(adj_records)} adjectives to {out_file}")
    return adj_records


if __name__ == "__main__":
    verbs = extract_verbs()
    adjs = extract_adjectives()
    print("Extraction completed successfully!")
