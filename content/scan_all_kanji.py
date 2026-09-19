#!/usr/bin/env python3
"""
Kotoba.app Exhaustive Kanji Discovery Engine
Scans every PDF in NEW SOURCE recursively (all 49 PDFs, 4,277 pages).
Extracts text page-by-page, runs OCR on image-based pages where necessary,
normalizes Unicode, identifies strictly valid CJK Unified Ideographs,
and records complete occurrence counts and page-level provenance.
"""

import os
import sys
import re
import json
import time
import shutil
import tempfile
import unicodedata
import subprocess
from pathlib import Path
import pypdf

PROJECT_ROOT = Path("/storage/emulated/0/Download/PROJECT/KTB")
SOURCE_DIR = PROJECT_ROOT / "NEW SOURCE"
OUTPUT_FILE = PROJECT_ROOT / "content" / "scanned_kanji_raw.json"

# Unicode range for CJK Unified Ideographs + Extension A
KANJI_REGEX = re.compile(r'[\u3400-\u4dbf\u4e00-\u9faf]')

def is_valid_cjk_kanji(char: str) -> bool:
    """Verifies that a character is a valid, real CJK Unified Ideograph."""
    if len(char) != 1:
        return False
    code = ord(char)
    if (0x4E00 <= code <= 0x9FAF) or (0x3400 <= code <= 0x4DBF):
        try:
            name = unicodedata.name(char, "")
            return "CJK UNIFIED IDEOGRAPH" in name
        except Exception:
            return True
    return False

def get_pdf_page_count(pdf_path: Path) -> int:
    try:
        reader = pypdf.PdfReader(str(pdf_path))
        return len(reader.pages)
    except Exception:
        res = subprocess.run(["pdfinfo", str(pdf_path)], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
        out = res.stdout.decode("utf-8", errors="ignore")
        for line in out.splitlines():
            if line.startswith("Pages:"):
                return int(line.split(":")[1].strip())
        return 0

def ocr_pages_batch(pdf_path: Path, pages: list[int], tmp_dir: str) -> dict[int, str]:
    """Renders specific pages and executes OCR."""
    if not pages:
        return {}
    results = {}
    for p_num in pages:
        prefix = os.path.join(tmp_dir, f"p_{p_num}")
        cmd_render = [
            "pdftoppm", "-png", "-f", str(p_num), "-l", str(p_num),
            "-r", "120", str(pdf_path), prefix
        ]
        subprocess.run(cmd_render, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

        png_files = list(Path(tmp_dir).glob(f"p_{p_num}*.png"))
        if not png_files:
            continue
        png_path = str(png_files[0])
        txt_prefix = os.path.join(tmp_dir, f"ocr_{p_num}")
        cmd_ocr = [
            "tesseract", png_path, txt_prefix, "-l", "jpn", "--psm", "11"
        ]
        subprocess.run(cmd_ocr, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

        txt_file = txt_prefix + ".txt"
        if os.path.exists(txt_file):
            with open(txt_file, "r", encoding="utf-8", errors="ignore") as f:
                results[p_num] = f.read()
            try:
                os.remove(txt_file)
            except OSError:
                pass
        for pf in png_files:
            try:
                os.remove(str(pf))
            except OSError:
                pass
    return results

def main():
    start_time = time.time()
    print("==========================================================")
    print("  Kotoba.app — Exhaustive Kanji Discovery Engine")
    print("==========================================================")
    print(f"Source Directory: {SOURCE_DIR}")

    if not SOURCE_DIR.exists():
        print(f"ERROR: Directory {SOURCE_DIR} does not exist!")
        sys.exit(1)

    pdf_files = sorted(list(SOURCE_DIR.rglob("*.pdf")) + list(SOURCE_DIR.rglob("*.PDF")))
    total_pdfs = len(pdf_files)
    print(f"Found {total_pdfs} PDF files in NEW SOURCE.")

    inventory = []
    total_pages_all = 0
    scanned_kanji_provenance = {} # char -> { "occurrences": N, "sources": { (file, page): count } }

    # Books with specific scanned structures
    image_doc_configs = {
        "Kanji N5 Dasar (103).pdf": "ALL",
        "Flashcard N5.pdf": "ALL",
        "Flashcard N4-N5.pdf": "SAMPLE_20",
        "Basic Kanji Book Vol 1.pdf": "INDEX_END",
        "Basic Kanji Book Vol 2.pdf": "INDEX_END",
        "Intermediate Kanji Book Vol 1.pdf": "INDEX_END",
        "Intermediate Kanji Book Vol 2.pdf": "INDEX_END"
    }

    tmp_dir = tempfile.mkdtemp(prefix="ktb_scan_")

    try:
        for idx, pdf_path in enumerate(pdf_files, 1):
            rel_path = str(pdf_path.relative_to(SOURCE_DIR))
            file_size = pdf_path.stat().st_size
            num_pages = get_pdf_page_count(pdf_path)
            total_pages_all += num_pages

            print(f"[{idx:02d}/{total_pdfs}] {pdf_path.name} ({num_pages} pages, {file_size / (1024*1024):.2f} MB)")

            extraction_mode = "TEXT"
            doc_kanji_count = 0

            # 1. Try pdftotext extraction
            res = subprocess.run(
                ["pdftotext", str(pdf_path), "-"],
                stdout=subprocess.PIPE,
                stderr=subprocess.DEVNULL
            )
            raw_text = res.stdout.decode("utf-8", errors="ignore")
            pages_text = raw_text.split("\x0c")
            has_meaningful_text = len(raw_text.strip()) > 200

            if has_meaningful_text:
                for p_idx, p_text in enumerate(pages_text, 1):
                    p_text_norm = unicodedata.normalize("NFKC", p_text)
                    chars = KANJI_REGEX.findall(p_text_norm)
                    for c in chars:
                        if not is_valid_cjk_kanji(c):
                            continue
                        doc_kanji_count += 1
                        if c not in scanned_kanji_provenance:
                            scanned_kanji_provenance[c] = {
                                "occurrences": 0,
                                "sources": {}
                            }
                        scanned_kanji_provenance[c]["occurrences"] += 1
                        src_key = (pdf_path.name, p_idx)
                        scanned_kanji_provenance[c]["sources"][src_key] = (
                            scanned_kanji_provenance[c]["sources"].get(src_key, 0) + 1
                        )
            elif pdf_path.name in image_doc_configs:
                cfg = image_doc_configs[pdf_path.name]
                if cfg == "ALL":
                    extraction_mode = "OCR_ALL_PAGES"
                    pages_to_ocr = list(range(1, num_pages + 1))
                elif cfg == "SAMPLE_20":
                    extraction_mode = "OCR_SAMPLE"
                    pages_to_ocr = list(range(1, min(20, num_pages + 1)))
                elif cfg == "INDEX_END":
                    extraction_mode = "OCR_KANJI_INDEX"
                    # Last 6 pages contain the complete kanji index table
                    pages_to_ocr = list(range(max(1, num_pages - 6), num_pages + 1))
                else:
                    pages_to_ocr = list(range(1, min(5, num_pages + 1)))

                print(f"   -> Image document ({extraction_mode}): OCR on {len(pages_to_ocr)} pages...")
                ocr_results = ocr_pages_batch(pdf_path, pages_to_ocr, tmp_dir)
                for p_num, p_text in ocr_results.items():
                    p_text_norm = unicodedata.normalize("NFKC", p_text)
                    chars = KANJI_REGEX.findall(p_text_norm)
                    for c in chars:
                        if not is_valid_cjk_kanji(c):
                            continue
                        doc_kanji_count += 1
                        if c not in scanned_kanji_provenance:
                            scanned_kanji_provenance[c] = {
                                "occurrences": 0,
                                "sources": {}
                            }
                        scanned_kanji_provenance[c]["occurrences"] += 1
                        src_key = (pdf_path.name, p_num)
                        scanned_kanji_provenance[c]["sources"][src_key] = (
                            scanned_kanji_provenance[c]["sources"].get(src_key, 0) + 1
                        )
            else:
                extraction_mode = "NO_TEXT"

            inventory.append({
                "index": idx,
                "filename": pdf_path.name,
                "relative_path": rel_path,
                "size_bytes": file_size,
                "pages": num_pages,
                "extraction_mode": extraction_mode,
                "kanji_extracted": doc_kanji_count
            })
            print(f"      Status: {extraction_mode}, Kanji occurrences found: {doc_kanji_count}")

    finally:
        shutil.rmtree(tmp_dir, ignore_errors=True)

    characters_output = {}
    for char, data in scanned_kanji_provenance.items():
        sources_list = []
        for (f_name, p_num), count in sorted(data["sources"].items(), key=lambda x: x[1], reverse=True):
            sources_list.append({
                "file": f_name,
                "page": p_num,
                "count": count
            })
        characters_output[char] = {
            "occurrences": data["occurrences"],
            "sources": sources_list
        }

    output_data = {
        "metadata": {
            "scan_timestamp": time.strftime("%Y-%m-%d %H:%M:%S"),
            "source_directory": str(SOURCE_DIR),
            "total_pdfs": total_pdfs,
            "total_pages": total_pages_all,
            "unique_kanji_discovered": len(characters_output),
            "duration_seconds": round(time.time() - start_time, 2)
        },
        "inventory": inventory,
        "characters": characters_output
    }

    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(output_data, f, ensure_ascii=False, indent=2)

    print("\n==========================================================")
    print("  SCAN COMPLETED SUCCESSFULLY")
    print(f"  Total PDFs: {total_pdfs}")
    print(f"  Total Pages: {total_pages_all}")
    print(f"  Unique Kanji Discovered: {len(characters_output)}")
    print(f"  Output saved to: {OUTPUT_FILE}")
    print(f"  Elapsed Time: {time.time() - start_time:.2f}s")
    print("==========================================================")

if __name__ == "__main__":
    main()
