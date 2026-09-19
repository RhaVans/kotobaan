#!/usr/bin/env bash
set -e

PROJECT_ROOT="/storage/emulated/0/download/PROJECT/KTB"
cd "$PROJECT_ROOT"

echo "============================================================"
echo "KOTOBA.APP COMPREHENSIVE TEST SUITE"
echo "============================================================"

echo ""
echo "[1/2] Executing Python Curriculum & Database Integrity Validator..."
python3 "$PROJECT_ROOT/content/validate_curriculum.py"

echo ""
echo "[2/2] Executing Java Spaced Repetition, Weakness & Review Tests..."
bash "$PROJECT_ROOT/tests/run_java_tests.sh"

echo ""
echo "============================================================"
echo "ALL TEST SUITES PASSED SUCCESSFULLY (100% VERIFIED)"
echo "============================================================"
