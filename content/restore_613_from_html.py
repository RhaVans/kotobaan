#!/usr/bin/env python3
"""
restore_613_from_html.py
Restores the 613 canonical kanji to their original state using
kanji_flashcards-1.html as the source of truth.

Fields REMOVED from each canonical kanji entry:
  onyomi, onyomi_romaji, kunyomi, kunyomi_romaji,
  dual_reading, vocab_examples,
  primary_onyomi, primary_onyomi_romaji,
  primary_kunyomi, primary_kunyomi_romaji, primary_kunyomi_example

Fields RESTORED from HTML:
  reading   ← html entry["reading"]
  indonesian ← html entry["meaning"]

Fields PRESERVED (not touched):
  id, group, group_label, kanji, romaji, section
"""

import json
import re
import sys
import os

# ── Paths ────────────────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)
HTML_FILE = os.path.join(PROJECT_DIR, "kanji_flashcards-1.html")
JSON_FILE = os.path.join(PROJECT_DIR, "app", "src", "main", "assets", "kanji_dataset.json")

# Fields injected by the dual-reading sessions — all must be removed
SYNTHETIC_FIELDS = {
    "onyomi", "onyomi_romaji",
    "kunyomi", "kunyomi_romaji",
    "dual_reading", "vocab_examples",
    "primary_onyomi", "primary_onyomi_romaji",
    "primary_kunyomi", "primary_kunyomi_romaji",
    "primary_kunyomi_example",
}

# ── Load HTML data ────────────────────────────────────────────────────────────
def load_html_data(html_path: str) -> dict[int, dict]:
    with open(html_path, "r", encoding="utf-8") as f:
        content = f.read()
    m = re.search(r"const KANJI_DATA\s*=\s*(\[.*?\]);", content, re.DOTALL)
    if not m:
        print("ERROR: Could not find KANJI_DATA in HTML file.", file=sys.stderr)
        sys.exit(1)
    data = json.loads(m.group(1))
    by_no = {entry["no"]: entry for entry in data}
    print(f"HTML:  {len(by_no)} records loaded from {os.path.basename(html_path)}")
    return by_no


# ── Restore ───────────────────────────────────────────────────────────────────
def restore(json_path: str, html_by_no: dict[int, dict]) -> list[dict]:
    with open(json_path, "r", encoding="utf-8") as f:
        entries = json.load(f)

    matched = 0
    unmatched = []
    indonesian_changed = []
    reading_changed = []
    fields_removed_counts = {f: 0 for f in SYNTHETIC_FIELDS}

    restored_entries = []
    for entry in entries:
        entry_id = entry.get("id")

        # Identify numeric sequence number from id (int or string like "kanji_0001" or 1)
        if isinstance(entry_id, int):
            no = entry_id
        elif isinstance(entry_id, str) and entry_id.startswith("kanji_") and not entry_id.startswith("kanji_add_"):
            try:
                no = int(entry_id.replace("kanji_", "").lstrip("0") or "0")
            except ValueError:
                no = None
        else:
            no = None

        html_entry = html_by_no.get(no) if no else None

        if html_entry:
            matched += 1
            # Track changes before applying
            old_reading = entry.get("reading", "")
            old_indonesian = entry.get("indonesian", "")
            new_reading = html_entry["reading"]
            new_meaning = html_entry["meaning"]

            if old_reading != new_reading:
                reading_changed.append((no, old_reading, new_reading))
            if old_indonesian != new_meaning:
                indonesian_changed.append((no, entry.get("kanji",""), old_indonesian, new_meaning))

            # Apply restorations
            entry["reading"] = new_reading
            entry["indonesian"] = new_meaning

            # Remove synthetic fields
            for field in SYNTHETIC_FIELDS:
                if field in entry:
                    del entry[field]
                    fields_removed_counts[field] += 1
        else:
            # Additional kanji or unrecognized — pass through unchanged
            unmatched.append(entry_id)

        restored_entries.append(entry)

    # ── Report ────────────────────────────────────────────────────────────────
    print(f"\n=== RESTORE VALIDATION REPORT ===")
    print(f"JSON entries:           {len(entries)}")
    print(f"HTML records matched:   {matched} / {len(entries)}")
    print(f"Unmatched:              {len(unmatched)}")
    if unmatched:
        print(f"  Unmatched IDs: {unmatched[:10]}")
    print(f"\nFields removed:")
    for field, count in fields_removed_counts.items():
        if count > 0:
            print(f"  - {field}: {count} entries")
    print(f"\nreading restored:       {matched} entries")
    if reading_changed:
        print(f"  !! reading CHANGED in {len(reading_changed)} entries:")
        for no, old, new in reading_changed:
            print(f"     no={no}  OLD={old!r}  NEW={new!r}")
    else:
        print(f"  All reading values already matched HTML (0 changes)")
    print(f"\nindonesian restored:    {len(indonesian_changed)} entries changed")
    for no, kanji, old, new in indonesian_changed:
        print(f"  no={no} {kanji}  OLD={old!r}  → NEW={new!r}")
    print(f"\nAdditional kanji (kanji_add_*) modified: 0")
    print(f"=================================\n")

    return restored_entries


# ── Main ──────────────────────────────────────────────────────────────────────
def main():
    print(f"Source of truth:  {HTML_FILE}")
    print(f"Target JSON:      {JSON_FILE}")
    print()

    if not os.path.exists(HTML_FILE):
        print(f"ERROR: HTML file not found: {HTML_FILE}", file=sys.stderr)
        sys.exit(1)
    if not os.path.exists(JSON_FILE):
        print(f"ERROR: JSON file not found: {JSON_FILE}", file=sys.stderr)
        sys.exit(1)

    html_by_no = load_html_data(HTML_FILE)
    restored = restore(JSON_FILE, html_by_no)

    # Write back
    with open(JSON_FILE, "w", encoding="utf-8") as f:
        json.dump(restored, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print(f"Written: {JSON_FILE}")
    print("Done.")


if __name__ == "__main__":
    main()
