#!/usr/bin/env python3
"""
Kotoba Translation Dictionary Builder
Fetches and curates high-quality Indonesian translations for all Kanji glosses.
Saves to content/kanji_indonesian_dictionary.json.
"""

import os
import sys
import json
import time
import urllib.request
import urllib.parse
from pathlib import Path

PROJECT_ROOT = Path("/storage/emulated/0/Download/PROJECT/KTB")
DICT_REF_FILE = PROJECT_ROOT / "content" / "kanji_dict_ref.json"
ADD_DATASET_FILE = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "kanji_additional_dataset.json"
CACHE_FILE = PROJECT_ROOT / "content" / "kanji_indonesian_dictionary.json"

# High-priority contextual Japanese Kanji term overrides (Japanese context -> Natural Indonesian)
CONTEXT_OVERRIDES = {
    "ordinal": "ke- (urutan)",
    "no.": "nomor",
    "part of speech": "jenis kata",
    "single": "tunggal, satu",
    "order": "urutan, aturan",
    "looks": "rupa, penampilan",
    "fall": "jatuh, turun (hujan/salju)",
    "features": "rupa, ciri khas",
    "one's station in life": "kedudukan, diri sendiri",
    "provision": "penyediaan, persiapan",
    "preparation": "persiapan, perlengkapan",
    "simplicity": "kesederhanaan, ringkas",
    "brevity": "singkat, ringkas",
    "firm": "tegas, kukuh",
    "tight": "erat, ketat",
    "assurance": "kepastian, jaminan",
    "witness": "saksi, menyaksikan",
    "discern": "mengenali, membedakan",
    "contain": "berisi, memuat, menampung",
    "station": "kedudukan, tempat, stasiun",
    "precipitate": "turun (hujan/salju)",
    "sequence": "urutan, susunan",
    "sensation": "perasaan, sensasi rasa",
    "suburbs": "pinggiran kota",
    "outskirts": "pinggiran kota",
    "correspond to": "sesuai dengan, sebanding",
    "proportionate to": "seimbang dengan",
    "semi-": "semi-, mendekati",
    "words": "kata-kata, bahasa",
    "poetry": "syair, puisi",
    "point out": "menunjukkan",
    "simple": "sederhana, mudah",
    "somebody": "seseorang, orang",
    "person": "orang, pribadi",
    "descend": "turun, merosot",
    "late": "terlambat, lambat",
    "slow": "lambat, pelan",
    "back": "belakang, punggung",
    "passion": "gairah, emosi",
    "range": "jangkauan, rentang, wilayah",
    "limits": "batasan, batas",
    "change": "berubah, mengganti",
    "convert": "mengubah, konversi",
    "instruction": "petunjuk, ajaran",
    "japanese character reading": "cara baca kanji",
    "explanation": "penjelasan, keterangan",
    "beauty": "keindahan, kecantikan",
    "beautiful": "indah, cantik",
    "exist": "ada, berada",
    "mask": "topeng, masker, wajah",
    "face": "wajah, muka, permukaan"
}

def translate_batch(phrases: list[str]) -> list[str]:
    """Translates a batch of phrases from English to Indonesian via endpoint with retry."""
    query = "\n".join(phrases)
    url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=id&dt=t&q=" + urllib.parse.quote(query)
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Linux; Android)"})

    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=12) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                translated = "".join([s[0] for s in data[0]])
                lines = translated.split("\n")
                if len(lines) == len(phrases):
                    return [l.strip().lower() for l in lines]
                elif len(lines) > 0:
                    # Pad or adjust if slight line mismatch
                    res = [l.strip().lower() for l in lines if l.strip()]
                    if len(res) == len(phrases):
                        return res
        except Exception as e:
            time.sleep(1.5 * (attempt + 1))
    
    # Fallback: translate individually
    res = []
    for p in phrases:
        try:
            url_single = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=id&dt=t&q=" + urllib.parse.quote(p)
            req_single = urllib.request.Request(url_single, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req_single, timeout=8) as r:
                d = json.loads(r.read().decode("utf-8"))
                res.append("".join([s[0] for s in d[0]]).strip().lower())
        except Exception:
            res.append(p)
    return res

def main():
    print("==========================================================")
    print("  Kotoba — Building Translation Cache for Kanji Glosses")
    print("==========================================================")

    # 1. Load existing cache if available
    cache = {}
    if CACHE_FILE.exists():
        with open(CACHE_FILE, "r", encoding="utf-8") as f:
            cache = json.load(f)
        print(f"Loaded {len(cache)} existing cached translations.")

    # Apply manual overrides
    for k, v in CONTEXT_OVERRIDES.items():
        cache[k.lower()] = v.lower()

    # 2. Extract all unique phrases from Kanji dataset
    with open(DICT_REF_FILE, "r", encoding="utf-8") as f:
        dict_ref = json.load(f)

    with open(ADD_DATASET_FILE, "r", encoding="utf-8") as f:
        add_data = json.load(f)

    unique_phrases = set()
    for item in add_data:
        kj = item["kanji"]
        entry = dict_ref.get(kj, {})
        for m in entry.get("meanings", [])[:3]:
            cleaned = m.strip().lower()
            if cleaned and cleaned not in cache:
                unique_phrases.add(cleaned)

    missing = sorted(list(unique_phrases))
    print(f"Total unique phrases missing from cache: {len(missing)}")

    # 3. Batch translate missing phrases
    batch_size = 40
    total_batches = (len(missing) + batch_size - 1) // batch_size if missing else 0

    for b in range(total_batches):
        batch = missing[b * batch_size : (b + 1) * batch_size]
        print(f"Translating batch {b + 1}/{total_batches} ({len(batch)} phrases)...")
        translated = translate_batch(batch)
        for orig, trans in zip(batch, translated):
            # Clean translation artifacts
            cleaned_trans = trans.strip().replace("tidak.", "nomor").replace("lajang", "tunggal")
            cache[orig] = cleaned_trans
        time.sleep(0.5)

    # Always ensure overrides win
    for k, v in CONTEXT_OVERRIDES.items():
        cache[k.lower()] = v.lower()

    with open(CACHE_FILE, "w", encoding="utf-8") as f:
        json.dump(cache, f, ensure_ascii=False, indent=2)

    print(f"Saved {len(cache)} translations to {CACHE_FILE}")

if __name__ == "__main__":
    main()
