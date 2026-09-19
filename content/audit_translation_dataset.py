#!/usr/bin/env python3
"""
Kotoba Translation Dataset Auditor
Multi-signal verification engine for detecting English translations in Kotoba databases and JSON assets.
"""

import os
import sys
import re
import json
import sqlite3
import zipfile
import argparse
from pathlib import Path

# Comprehensive English marker words that should NOT appear in Indonesian flashcard meanings
ENGLISH_CORE_WORDS = set('''
the of and to a in is you that it he was for on are as with his they i at be this have from or one had by word but not what all were we when your can said there use an each which she do how their if will up other about out many then them these so some her would make like him into time has look two more write go see number no way could people my than first water been call who oil its now find long down day did get come made may part
person persons woman women man men child children wife wives husband husbands boy boys girl girls body bodies hand hands foot feet head heads eye eyes mouth mouths heart hearts mind minds soul souls life lives death deaths
building buildings house houses room rooms place places street streets road roads town towns village villages city cities state states country countries world worlds earth ground stone stones mountain mountains river rivers ocean oceans sea seas lake lakes tree trees wood woods forest forests plant plants flower flowers grass leaf leaves sky clouds sun moon star stars weather rain snow wind storm fire
dog dogs cat cats horse horses cow cows pig pigs sheep bird birds fish insect insects animal animals
food bread meat fruit fruits vegetable vegetables egg eggs milk tea coffee sugar salt drink drinks eat eats cook cooks
car cars train trains bus buses ship ships boat boats plane planes bicycle bicycles travel trip walk run fly
to be do make go come see hear know think want give take find say tell speak ask work feel try leave call
good bad big small long short high low hot cold warm cool new old young beautiful ugly fast slow hard soft heavy light dark bright strong weak clean dirty sweet sour bitter spicy rich poor easy difficult cheap expensive
red blue green yellow black white brown pink gray orange purple silver
residence poetry assurance firm tight acknowledge witness discern contain looks show simplicity brevity station precipitate sequence sensation
provision preparation limits range convert instruction character explanation suburbs correspond proportionate
appearance surface feature features passion feelings limits boundary boundaries border borders
matter matters thing things substance substances affair affairs rule rules standard standards law laws
government state nation officer official officials public private section chapter unit
measure measurement count counter volume weight degree angle
spirit soul ghost mind thoughts idea ideas principle principles reason reasons cause causes
heaven sky paradise hell underworld earth soil dirt clay dust
gold silver copper iron steel metal lead tin zinc brass bronze
cotton silk wool leather cloth fabric garment clothes dress robe
king queen prince princess lord master emperor empress noble nobles servant servants slave slaves
soldier soldiers army troops weapon weapons sword swords bow arrow gun guns shield
disease illness sickness pain wound harm damage danger disaster evil crime punishment
god gods deity temple shrine prayer priest monk church sacred holy
ancestor family generation line origin source root beginning end
'''.split())

# English morphology patterns (suffixes)
ENGLISH_SUFFIXES = (
    'tion', 'tions', 'ment', 'ments', 'ness', 'nesses', 'ity', 'ities',
    'ance', 'ances', 'ence', 'ences', 'able', 'ible', 'ingly'
)

# English phrases / dictionary markers
ENGLISH_PHRASES = [
    r'\bto\s+[a-z]+',
    r'\bone\'s\b',
    r'\bsomeone\b',
    r'\bsomething\b',
    r'\betc\b',
    r'\be\.g\b',
    r'\bi\.e\b',
    r'\bno\.\b'
]

# Legitimate Indonesian words and loanwords recognized by KBBI / Indonesian usage
# that might overlap with English or be falsely flagged
INDONESIAN_WHITELIST = set('''
air cat volume ambulans bank bus radio hotel supermarket video komputer internet pos televisi
tape recorder rice cooker partikel jft jlpt bab n1 n2 n3 n4 n5
ke- -an nya kah pun lah an man counter pasif transitif intransitif formal informal
nama saya anda dia kami kita mereka ini itu sini situ sana
makan minum tidur bangun pergi pulang datang lihat dengar baca tulis
bicara beli jual belajar mengajar kerja istirahat jalan lari
besar kecil panjang pendek tinggi rendah baru lama tua muda
bagus jelek enak manis asin pahit asam pedas panas dingin hangat sejuk
terang gelap berat ringan cepat lambat mudah susah gampang sulit
banyak sedikit semua sebagian satu dua tiga empat lima enam tujuh delapan sembilan sepuluh
senin selasa rabu kamis jumat sabtu minggu januari februari maret april mei juni juli agustus september oktober november desember
tahun bulan minggu hari jam menit detik pagi siang sore malam
ayah ibu kakek nenek paman bibi kakak adik anak laki perempuan suami istri teman sahabat
rumah kamar dapur sekolah kantor toko stasiun bandara rumah sakit masjid gereja restoran pasar taman jalan mobil kereta sepeda pesawat kapal
uang buku baju sepatu tas kacamata telepon jam meja kursi pintu jendela
warna merah putih hitam biru kuning hijau abu jingga perak cokelat emas
'''.split())

def evaluate_translation(text: str) -> dict:
    """
    Evaluates an Indonesian meaning string for English contamination.
    Returns:
      is_english: True if confirmed English or strong English signal
      is_suspicious: True if possible English or mixed
      matches: list of matched English tokens/patterns
    """
    if not text or not text.strip():
        return {"is_english": True, "is_suspicious": True, "matches": ["EMPTY_TEXT"]}

    text_lower = text.lower()
    matches = []

    # 1. Check dictionary phrases
    for pattern in ENGLISH_PHRASES:
        found = re.findall(pattern, text_lower)
        if found:
            matches.extend(found)

    # 2. Tokenize words
    tokens = re.findall(r'[a-z]+', text_lower)
    english_token_count = 0
    total_tokens = len(tokens)

    for token in tokens:
        if token in INDONESIAN_WHITELIST:
            continue
        if token in ENGLISH_CORE_WORDS:
            matches.append(token)
            english_token_count += 1
        elif any(token.endswith(sfx) for sfx in ENGLISH_SUFFIXES) and len(token) > 5:
            matches.append(f"{token} (suffix)")
            english_token_count += 1

    # Remove duplicates
    unique_matches = list(dict.fromkeys(matches))

    if total_tokens > 0 and english_token_count / total_tokens >= 0.5:
        return {"is_english": True, "is_suspicious": False, "matches": unique_matches}
    elif len(unique_matches) > 0:
        return {"is_english": False, "is_suspicious": True, "matches": unique_matches}
    else:
        return {"is_english": False, "is_suspicious": False, "matches": []}

def audit_database(db_path: Path) -> dict:
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.execute("SELECT id, type, japanese, reading, indonesian FROM learning_objects ORDER BY id ASC")
    rows = cursor.fetchall()
    conn.close()

    total = len(rows)
    valid = 0
    suspicious = 0
    confirmed_english = 0
    flagged_entries = []

    for r in rows:
        oid, otype, jap, reading, indo = r
        res = evaluate_translation(indo)
        if res["is_english"]:
            confirmed_english += 1
            flagged_entries.append((oid, jap, indo, res["matches"], "CONFIRMED_ENGLISH"))
        elif res["is_suspicious"]:
            suspicious += 1
            flagged_entries.append((oid, jap, indo, res["matches"], "SUSPICIOUS"))
        else:
            valid += 1

    return {
        "source": str(db_path),
        "total": total,
        "valid": valid,
        "suspicious": suspicious,
        "confirmed_english": confirmed_english,
        "flagged_entries": flagged_entries
    }

def audit_json_file(json_path: Path) -> dict:
    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    total = len(data)
    valid = 0
    suspicious = 0
    confirmed_english = 0
    flagged_entries = []

    for item in data:
        oid = str(item.get("id", item.get("kanji_id", "")))
        jap = item.get("japanese", item.get("kanji", ""))
        indo = item.get("indonesian", "")
        res = evaluate_translation(indo)
        if res["is_english"]:
            confirmed_english += 1
            flagged_entries.append((oid, jap, indo, res["matches"], "CONFIRMED_ENGLISH"))
        elif res["is_suspicious"]:
            suspicious += 1
            flagged_entries.append((oid, jap, indo, res["matches"], "SUSPICIOUS"))
        else:
            valid += 1

    return {
        "source": str(json_path),
        "total": total,
        "valid": valid,
        "suspicious": suspicious,
        "confirmed_english": confirmed_english,
        "flagged_entries": flagged_entries
    }

def audit_apk(apk_path: Path) -> dict:
    temp_db = Path("/tmp/temp_apk_audit_kotoba.db")
    if temp_db.exists():
        temp_db.unlink()

    with zipfile.ZipFile(apk_path, 'r') as z:
        db_entries = [name for name in z.namelist() if name.endswith("kotoba.db")]
        if not db_entries:
            raise FileNotFoundError("kotoba.db not found inside APK package!")
        with open(temp_db, "wb") as f:
            f.write(z.read(db_entries[0]))

    res = audit_database(temp_db)
    res["source"] = f"{apk_path} ({db_entries[0]})"
    if temp_db.exists():
        temp_db.unlink()
    return res

def print_audit_report(res: dict):
    print("============================================================")
    print(f"TRANSLATION DATASET AUDIT REPORT")
    print(f"Source: {res['source']}")
    print("============================================================")
    print(f"Total entries scanned:         {res['total']:,}")
    print(f"Valid Indonesian translations: {res['valid']:,}")
    print(f"Suspicious translations:       {res['suspicious']:,}")
    print(f"Confirmed English translations:{res['confirmed_english']:,}")
    print("============================================================")

    if res["flagged_entries"]:
        print(f"Sample Flagged Entries ({min(10, len(res['flagged_entries']))} of {len(res['flagged_entries'])}):")
        for f in res["flagged_entries"][:10]:
            print(f"  [{f[4]}] {f[0]}: {f[1]} -> \"{f[2]}\" | matches: {f[3]}")
    else:
        print("RESULT: 100% CLEAN INDONESIAN DATASET — ZERO ENGLISH DETECTED.")
    print("============================================================")

def main():
    parser = argparse.ArgumentParser(description="Audit Kotoba translation datasets")
    parser.add_argument("--target", choices=["json", "db", "apk"], default="db", help="Target type")
    parser.add_argument("--path", type=str, help="Specific file path")
    args = parser.parse_args()

    project_root = Path("/storage/emulated/0/Download/PROJECT/KTB")

    if args.target == "db":
        path = Path(args.path) if args.path else project_root / "app" / "src" / "main" / "assets" / "databases" / "kotoba.db"
        res = audit_database(path)
        print_audit_report(res)
        sys.exit(0 if (res["confirmed_english"] == 0 and res["suspicious"] == 0) else 1)

    elif args.target == "apk":
        path = Path(args.path) if args.path else project_root / "KOTOBAAN.apk"
        res = audit_apk(path)
        print_audit_report(res)
        sys.exit(0 if (res["confirmed_english"] == 0 and res["suspicious"] == 0) else 1)

    elif args.target == "json":
        json_files = [
            project_root / "app" / "src" / "main" / "assets" / "kanji_additional_dataset.json",
            project_root / "app" / "src" / "main" / "assets" / "kotoba_dataset.json",
            project_root / "app" / "src" / "main" / "assets" / "kanji_dataset.json",
            project_root / "app" / "src" / "main" / "assets" / "kata_sifat_dataset.json",
            project_root / "app" / "src" / "main" / "assets" / "kata_kerja_dataset.json"
        ]
        has_failure = False
        for jf in json_files:
            if jf.exists():
                res = audit_json_file(jf)
                print_audit_report(res)
                if res["confirmed_english"] > 0 or res["suspicious"] > 0:
                    has_failure = True
        sys.exit(1 if has_failure else 0)

if __name__ == "__main__":
    main()
