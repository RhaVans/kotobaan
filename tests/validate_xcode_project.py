import os
import re
import unittest

class TestXcodeProjectValidation(unittest.TestCase):
    def setUp(self):
        self.pbx_path = "ios/Kotoba.xcodeproj/project.pbxproj"
        self.assertTrue(os.path.exists(self.pbx_path), f"{self.pbx_path} does not exist")
        with open(self.pbx_path, "r", encoding="utf-8") as f:
            self.content = f.read()

    def test_pbx_syntax_markers(self):
        self.assertTrue(self.content.startswith("// !$*UTF8*$!"))
        self.assertIn("archiveVersion = 1;", self.content)
        self.assertIn("objectVersion = 56;", self.content)
        self.assertIn("rootObject =", self.content)

    def test_all_referenced_source_files_exist_on_disk(self):
        # Match lines like: path = KotobaApp.swift;
        pattern = re.compile(r'/\* ([A-Za-z0-9_+\-]+\.swift) \*/ = \{isa = PBXFileReference;.*?path = ([A-Za-z0-9_+\-]+\.swift);', re.DOTALL)
        matches = pattern.findall(self.content)
        self.assertTrue(len(matches) > 0, "No Swift files found in PBXFileReference")

        # Check each file on disk
        for comment_name, path in matches:
            # Look for the file anywhere in ios/Kotoba
            found = False
            for root, _, files in os.walk("ios/Kotoba"):
                if path in files:
                    found = True
                    break
            self.assertTrue(found, f"Referenced file {path} not found in ios/Kotoba")

    def test_resources_exist_on_disk(self):
        self.assertTrue(os.path.exists("ios/Kotoba/Resources/kotoba.db"))
        self.assertTrue(os.path.exists("ios/Kotoba/Resources/Info.plist"))
        self.assertTrue(os.path.exists("ios/Kotoba/Resources/Assets.xcassets"))
        self.assertTrue(os.path.exists("ios/Kotoba/Resources/Assets.xcassets/AppIcon.appiconset/Contents.json"))
        self.assertTrue(os.path.exists("ios/Kotoba/Resources/Assets.xcassets/AccentColor.colorset/Contents.json"))

    def test_target_configurations(self):
        self.assertIn("PRODUCT_BUNDLE_IDENTIFIER = com.kotoba.app;", self.content)
        self.assertIn("IPHONEOS_DEPLOYMENT_TARGET = 16.0;", self.content)
        self.assertIn("SWIFT_VERSION = 5.0;", self.content)
        self.assertIn("INFOPLIST_FILE = Kotoba/Resources/Info.plist;", self.content)

    def test_framework_dependencies(self):
        self.assertIn("AVFoundation.framework", self.content)
        self.assertIn("AudioToolbox.framework", self.content)
        self.assertIn("libsqlite3.tbd", self.content)

if __name__ == "__main__":
    unittest.main()
