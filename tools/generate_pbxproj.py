import os
import hashlib

def gid(name):
    return hashlib.md5(name.encode('utf-8')).hexdigest()[:24].upper()

source_files = [
    ("App", "KotobaApp.swift"),
    ("App", "AppState.swift"),
    ("Models", "WordType.swift"),
    ("Models", "PronunciationTarget.swift"),
    ("Models", "LearningObject.swift"),
    ("Models", "UserProgress.swift"),
    ("Models", "Chapter.swift"),
    ("Models", "LearningCycle.swift"),
    ("Services", "DatabaseService.swift"),
    ("Services", "SpeechService.swift"),
    ("Engines", "SrsScheduler.swift"),
    ("Engines", "IngatLupaEngine.swift"),
    ("Engines", "WeaknessDetector.swift"),
    ("ViewModels", "FlashcardViewModel.swift"),
    ("ViewModels", "PustakaViewModel.swift"),
    ("ViewModels", "ReviewViewModel.swift"),
    ("ViewModels", "SettingsViewModel.swift"),
    ("Views", "MainTabView.swift"),
    ("Views/Flashcard", "CardFaceView.swift"),
    ("Views/Flashcard", "FlashcardView.swift"),
    ("Views/Pustaka", "PustakaCardRow.swift"),
    ("Views/Pustaka", "MultiBabSheetView.swift"),
    ("Views/Pustaka", "PustakaView.swift"),
    ("Views/Review", "ReviewView.swift"),
    ("Views/Settings", "SettingsView.swift")
]

resource_files = [
    ("Resources", "Assets.xcassets", "folder.assetcatalog"),
    ("Resources", "kotoba.db", "file"),
    ("Resources", "Info.plist", "text.plist.xml")
]

framework_files = [
    ("AVFoundation.framework", "wrapper.framework", "System/Library/Frameworks/AVFoundation.framework", "SDKROOT"),
    ("AudioToolbox.framework", "wrapper.framework", "System/Library/Frameworks/AudioToolbox.framework", "SDKROOT"),
    ("libsqlite3.tbd", "sourcecode.text-based-dylib-definition", "usr/lib/libsqlite3.tbd", "SDKROOT")
]

lines = []
lines.append("// !$*UTF8*$!")
lines.append("{")
lines.append("\tarchiveVersion = 1;")
lines.append("\tclasses = {")
lines.append("\t};")
lines.append("\tobjectVersion = 56;")
lines.append("\tobjects = {")

# 1. PBXBuildFile
lines.append("\n/* Begin PBXBuildFile section */")
for group, fn in source_files:
    path = f"Kotoba/{group}/{fn}"
    lines.append(f"\t\t{gid('BF_' + path)} /* {fn} in Sources */ = {{isa = PBXBuildFile; fileRef = {gid('FR_' + path)} /* {fn} */; }};")

for group, fn, ftype in resource_files:
    if fn != "Info.plist":
        path = f"Kotoba/{group}/{fn}"
        lines.append(f"\t\t{gid('BF_' + path)} /* {fn} in Resources */ = {{isa = PBXBuildFile; fileRef = {gid('FR_' + path)} /* {fn} */; }};")

for fn, ftype, fpath, fsource in framework_files:
    lines.append(f"\t\t{gid('BF_' + fn)} /* {fn} in Frameworks */ = {{isa = PBXBuildFile; fileRef = {gid('FR_' + fn)} /* {fn} */; }};")
lines.append("/* End PBXBuildFile section */")

# 2. PBXFileReference
lines.append("\n/* Begin PBXFileReference section */")
for group, fn in source_files:
    path = f"Kotoba/{group}/{fn}"
    lines.append(f"\t\t{gid('FR_' + path)} /* {fn} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {fn}; sourceTree = \"<group>\"; }};")

for group, fn, ftype in resource_files:
    path = f"Kotoba/{group}/{fn}"
    lines.append(f"\t\t{gid('FR_' + path)} /* {fn} */ = {{isa = PBXFileReference; lastKnownFileType = {ftype}; path = {fn}; sourceTree = \"<group>\"; }};")

for fn, ftype, fpath, fsource in framework_files:
    lines.append(f"\t\t{gid('FR_' + fn)} /* {fn} */ = {{isa = PBXFileReference; lastKnownFileType = \"{ftype}\"; name = {fn}; path = {fpath}; sourceTree = {fsource}; }};")

lines.append(f"\t\t{gid('FR_Kotoba.app')} /* Kotoba.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = Kotoba.app; sourceTree = BUILT_PRODUCTS_DIR; }};")
lines.append("/* End PBXFileReference section */")

# 3. PBXFrameworksBuildPhase
lines.append("\n/* Begin PBXFrameworksBuildPhase section */")
lines.append(f"\t\t{gid('BP_Frameworks')} /* Frameworks */ = {{")
lines.append("\t\t\tisa = PBXFrameworksBuildPhase;")
lines.append("\t\t\tbuildActionMask = 2147483647;")
lines.append("\t\t\tfiles = (")
for fn, ftype, fpath, fsource in framework_files:
    lines.append(f"\t\t\t\t{gid('BF_' + fn)} /* {fn} in Frameworks */,")
lines.append("\t\t\t);")
lines.append("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
lines.append("\t\t};")
lines.append("/* End PBXFrameworksBuildPhase section */")

# 4. PBXGroup
lines.append("\n/* Begin PBXGroup section */")

# Main group
lines.append(f"\t\t{gid('GRP_Main')} = {{")
lines.append("\t\t\tisa = PBXGroup;")
lines.append("\t\t\tchildren = (")
lines.append(f"\t\t\t\t{gid('GRP_Kotoba')} /* Kotoba */,")
lines.append(f"\t\t\t\t{gid('GRP_Frameworks')} /* Frameworks */,")
lines.append(f"\t\t\t\t{gid('GRP_Products')} /* Products */,")
lines.append("\t\t\t);")
lines.append("\t\t\tsourceTree = \"<group>\";")
lines.append("\t\t};")

# Kotoba root group
lines.append(f"\t\t{gid('GRP_Kotoba')} /* Kotoba */ = {{")
lines.append("\t\t\tisa = PBXGroup;")
lines.append("\t\t\tchildren = (")
lines.append(f"\t\t\t\t{gid('GRP_App')} /* App */,")
lines.append(f"\t\t\t\t{gid('GRP_Models')} /* Models */,")
lines.append(f"\t\t\t\t{gid('GRP_Services')} /* Services */,")
lines.append(f"\t\t\t\t{gid('GRP_Engines')} /* Engines */,")
lines.append(f"\t\t\t\t{gid('GRP_ViewModels')} /* ViewModels */,")
lines.append(f"\t\t\t\t{gid('GRP_Views')} /* Views */,")
lines.append(f"\t\t\t\t{gid('GRP_Resources')} /* Resources */,")
lines.append("\t\t\t);")
lines.append("\t\t\tpath = Kotoba;")
lines.append("\t\t\tsourceTree = \"<group>\";")
lines.append("\t\t};")

# Subgroups
subgroups = {
    "App": [fn for g, fn in source_files if g == "App"],
    "Models": [fn for g, fn in source_files if g == "Models"],
    "Services": [fn for g, fn in source_files if g == "Services"],
    "Engines": [fn for g, fn in source_files if g == "Engines"],
    "ViewModels": [fn for g, fn in source_files if g == "ViewModels"],
    "Resources": [fn for g, fn, ft in resource_files]
}

for sg_name, files in subgroups.items():
    lines.append(f"\t\t{gid('GRP_' + sg_name)} /* {sg_name} */ = {{")
    lines.append("\t\t\tisa = PBXGroup;")
    lines.append("\t\t\tchildren = (")
    for fn in files:
        path = f"Kotoba/{sg_name}/{fn}"
        lines.append(f"\t\t\t\t{gid('FR_' + path)} /* {fn} */,")
    lines.append("\t\t\t);")
    lines.append(f"\t\t\tpath = {sg_name};")
    lines.append("\t\t\tsourceTree = \"<group>\";")
    lines.append("\t\t};")

# Views group (with sub-views)
view_subgroups = ["Flashcard", "Pustaka", "Review", "Settings"]
lines.append(f"\t\t{gid('GRP_Views')} /* Views */ = {{")
lines.append("\t\t\tisa = PBXGroup;")
lines.append("\t\t\tchildren = (")
lines.append(f"\t\t\t\t{gid('FR_Kotoba/Views/MainTabView.swift')} /* MainTabView.swift */,")
for vsg in view_subgroups:
    lines.append(f"\t\t\t\t{gid('GRP_Views_' + vsg)} /* {vsg} */,")
lines.append("\t\t\t);")
lines.append("\t\t\tpath = Views;")
lines.append("\t\t\tsourceTree = \"<group>\";")
lines.append("\t\t};")

for vsg in view_subgroups:
    vsg_files = [fn for g, fn in source_files if g == f"Views/{vsg}"]
    lines.append(f"\t\t{gid('GRP_Views_' + vsg)} /* {vsg} */ = {{")
    lines.append("\t\t\tisa = PBXGroup;")
    lines.append("\t\t\tchildren = (")
    for fn in vsg_files:
        path = f"Kotoba/Views/{vsg}/{fn}"
        lines.append(f"\t\t\t\t{gid('FR_' + path)} /* {fn} */,")
    lines.append("\t\t\t);")
    lines.append(f"\t\t\tpath = {vsg};")
    lines.append("\t\t\tsourceTree = \"<group>\";")
    lines.append("\t\t};")

# Frameworks group
lines.append(f"\t\t{gid('GRP_Frameworks')} /* Frameworks */ = {{")
lines.append("\t\t\tisa = PBXGroup;")
lines.append("\t\t\tchildren = (")
for fn, ftype, fpath, fsource in framework_files:
    lines.append(f"\t\t\t\t{gid('FR_' + fn)} /* {fn} */,")
lines.append("\t\t\t);")
lines.append("\t\t\tname = Frameworks;")
lines.append("\t\t\tsourceTree = \"<group>\";")
lines.append("\t\t};")

# Products group
lines.append(f"\t\t{gid('GRP_Products')} /* Products */ = {{")
lines.append("\t\t\tisa = PBXGroup;")
lines.append("\t\t\tchildren = (")
lines.append(f"\t\t\t\t{gid('FR_Kotoba.app')} /* Kotoba.app */,")
lines.append("\t\t\t);")
lines.append("\t\t\tname = Products;")
lines.append("\t\t\tsourceTree = \"<group>\";")
lines.append("\t\t};")

lines.append("/* End PBXGroup section */")

# 5. PBXNativeTarget
lines.append("\n/* Begin PBXNativeTarget section */")
lines.append(f"\t\t{gid('TARGET_Kotoba')} /* Kotoba */ = {{")
lines.append("\t\t\tisa = PBXNativeTarget;")
lines.append(f"\t\t\tbuildConfigurationList = {gid('BCL_Target')} /* Build configuration list for PBXNativeTarget \"Kotoba\" */;")
lines.append("\t\t\tbuildPhases = (")
lines.append(f"\t\t\t\t{gid('BP_Sources')} /* Sources */,")
lines.append(f"\t\t\t\t{gid('BP_Frameworks')} /* Frameworks */,")
lines.append(f"\t\t\t\t{gid('BP_Resources')} /* Resources */,")
lines.append("\t\t\t);")
lines.append("\t\t\tbuildRules = (")
lines.append("\t\t\t);")
lines.append("\t\t\tdependencies = (")
lines.append("\t\t\t);")
lines.append("\t\t\tname = Kotoba;")
lines.append("\t\t\tproductName = Kotoba;")
lines.append(f"\t\t\tproductReference = {gid('FR_Kotoba.app')} /* Kotoba.app */;")
lines.append("\t\t\tproductType = \"com.apple.product-type.application\";")
lines.append("\t\t};")
lines.append("/* End PBXNativeTarget section */")

# 6. PBXProject
lines.append("\n/* Begin PBXProject section */")
lines.append(f"\t\t{gid('PROJECT')} /* Project object */ = {{")
lines.append("\t\t\tisa = PBXProject;")
lines.append("\t\t\tattributes = {")
lines.append("\t\t\t\tBuildIndependentTargetsInParallel = 1;")
lines.append("\t\t\t\tLastUpgradeCheck = 1540;")
lines.append("\t\t\t\tTargetAttributes = {")
lines.append(f"\t\t\t\t\t{gid('TARGET_Kotoba')} = {{")
lines.append("\t\t\t\t\t\tCreatedOnToolsVersion = 15.4;")
lines.append("\t\t\t\t\t};")
lines.append("\t\t\t\t};")
lines.append("\t\t\t};")
lines.append(f"\t\t\tbuildConfigurationList = {gid('BCL_Project')} /* Build configuration list for PBXProject \"Kotoba\" */;")
lines.append("\t\t\tcompatibilityVersion = \"Xcode 14.0\";")
lines.append("\t\t\tdevelopmentRegion = en;")
lines.append("\t\t\thasScannedForEncodings = 0;")
lines.append("\t\t\tknownRegions = (")
lines.append("\t\t\t\ten,")
lines.append("\t\t\t\tBase,")
lines.append("\t\t\t);")
lines.append(f"\t\t\tmainGroup = {gid('GRP_Main')};")
lines.append(f"\t\t\tproductRefGroup = {gid('GRP_Products')} /* Products */;")
lines.append("\t\t\tprojectDirPath = \"\";")
lines.append("\t\t\tprojectRoot = \"\";")
lines.append("\t\t\ttargets = (")
lines.append(f"\t\t\t\t{gid('TARGET_Kotoba')} /* Kotoba */,")
lines.append("\t\t\t);")
lines.append("\t\t};")
lines.append("/* End PBXProject section */")

# 7. PBXResourcesBuildPhase
lines.append("\n/* Begin PBXResourcesBuildPhase section */")
lines.append(f"\t\t{gid('BP_Resources')} /* Resources */ = {{")
lines.append("\t\t\tisa = PBXResourcesBuildPhase;")
lines.append("\t\t\tbuildActionMask = 2147483647;")
lines.append("\t\t\tfiles = (")
for group, fn, ftype in resource_files:
    if fn != "Info.plist":
        path = f"Kotoba/{group}/{fn}"
        lines.append(f"\t\t\t\t{gid('BF_' + path)} /* {fn} in Resources */,")
lines.append("\t\t\t);")
lines.append("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
lines.append("\t\t};")
lines.append("/* End PBXResourcesBuildPhase section */")

# 8. PBXSourcesBuildPhase
lines.append("\n/* Begin PBXSourcesBuildPhase section */")
lines.append(f"\t\t{gid('BP_Sources')} /* Sources */ = {{")
lines.append("\t\t\tisa = PBXSourcesBuildPhase;")
lines.append("\t\t\tbuildActionMask = 2147483647;")
lines.append("\t\t\tfiles = (")
for group, fn in source_files:
    path = f"Kotoba/{group}/{fn}"
    lines.append(f"\t\t\t\t{gid('BF_' + path)} /* {fn} in Sources */,")
lines.append("\t\t\t);")
lines.append("\t\t\trunOnlyForDeploymentPostprocessing = 0;")
lines.append("\t\t};")
lines.append("/* End PBXSourcesBuildPhase section */")

# 9. XCBuildConfiguration
lines.append("\n/* Begin XCBuildConfiguration section */")

# Project Debug
lines.append(f"\t\t{gid('CFG_Proj_Debug')} /* Debug */ = {{")
lines.append("\t\t\tisa = XCBuildConfiguration;")
lines.append("\t\t\tbuildSettings = {")
lines.append("\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;")
lines.append("\t\t\t\tCLANG_ENABLE_MODULES = YES;")
lines.append("\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;")
lines.append("\t\t\t\tCOPY_PHASE_STRIP = NO;")
lines.append("\t\t\t\tDEBUG_INFORMATION_FORMAT = dwarf;")
lines.append("\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;")
lines.append("\t\t\t\tENABLE_TESTABILITY = YES;")
lines.append("\t\t\t\tGCC_DYNAMIC_NO_PIC = NO;")
lines.append("\t\t\t\tGCC_OPTIMIZATION_LEVEL = 0;")
lines.append("\t\t\t\tGCC_PREPROCESSOR_DEFINITIONS = (")
lines.append("\t\t\t\t\t\"DEBUG=1\",")
lines.append("\t\t\t\t\t\"$(inherited)\",")
lines.append("\t\t\t\t);")
lines.append("\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 16.0;")
lines.append("\t\t\t\tMTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;")
lines.append("\t\t\t\tONLY_ACTIVE_ARCH = YES;")
lines.append("\t\t\t\tSDKROOT = iphoneos;")
lines.append("\t\t\t\tSWIFT_ACTIVE_COMPILATION_CONDITIONS = \"DEBUG $(inherited)\";")
lines.append("\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = \"-Onone\";")
lines.append("\t\t\t};")
lines.append("\t\t\tname = Debug;")
lines.append("\t\t};")

# Project Release
lines.append(f"\t\t{gid('CFG_Proj_Release')} /* Release */ = {{")
lines.append("\t\t\tisa = XCBuildConfiguration;")
lines.append("\t\t\tbuildSettings = {")
lines.append("\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;")
lines.append("\t\t\t\tCLANG_ENABLE_MODULES = YES;")
lines.append("\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;")
lines.append("\t\t\t\tCOPY_PHASE_STRIP = NO;")
lines.append("\t\t\t\tDEBUG_INFORMATION_FORMAT = \"dwarf-with-dsym\";")
lines.append("\t\t\t\tENABLE_NS_ASSERTIONS = NO;")
lines.append("\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;")
lines.append("\t\t\t\tGCC_OPTIMIZATION_LEVEL = s;")
lines.append("\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 16.0;")
lines.append("\t\t\t\tMTL_ENABLE_DEBUG_INFO = NO;")
lines.append("\t\t\t\tSDKROOT = iphoneos;")
lines.append("\t\t\t\tSWIFT_COMPILATION_MODE = \"wholemodule\";")
lines.append("\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = \"-O\";")
lines.append("\t\t\t\tVALIDATE_PRODUCT = YES;")
lines.append("\t\t\t};")
lines.append("\t\t\tname = Release;")
lines.append("\t\t};")

# Target Debug
lines.append(f"\t\t{gid('CFG_Target_Debug')} /* Debug */ = {{")
lines.append("\t\t\tisa = XCBuildConfiguration;")
lines.append("\t\t\tbuildSettings = {")
lines.append("\t\t\t\tASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;")
lines.append("\t\t\t\tASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;")
lines.append("\t\t\t\tCODE_SIGN_STYLE = Automatic;")
lines.append("\t\t\t\tCODE_SIGNING_ALLOWED = NO;")
lines.append("\t\t\t\tCURRENT_PROJECT_VERSION = 1;")
lines.append("\t\t\t\tDEVELOPMENT_TEAM = \"\";")
lines.append("\t\t\t\tENABLE_PREVIEWS = YES;")
lines.append("\t\t\t\tGENERATE_INFOPLIST_FILE = NO;")
lines.append("\t\t\t\tINFOPLIST_FILE = Kotoba/Resources/Info.plist;")
lines.append("\t\t\t\tINFOPLIST_KEY_CFBundleDisplayName = Kotoba;")
lines.append("\t\t\t\tINFOPLIST_KEY_LSApplicationCategoryType = \"public.app-category.education\";")
lines.append("\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 16.0;")
lines.append("\t\t\t\tLD_RUNPATH_SEARCH_PATHS = (")
lines.append("\t\t\t\t\t\"$(inherited)\",")
lines.append("\t\t\t\t\t\"@executable_path/Frameworks\",")
lines.append("\t\t\t\t);")
lines.append("\t\t\t\tMARKETING_VERSION = 1.0;")
lines.append("\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = com.kotoba.app;")
lines.append("\t\t\t\tPRODUCT_NAME = \"$(TARGET_NAME)\";")
lines.append("\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;")
lines.append("\t\t\t\tSWIFT_VERSION = 5.0;")
lines.append("\t\t\t\tTARGETED_DEVICE_FAMILY = \"1,2\";")
lines.append("\t\t\t};")
lines.append("\t\t\tname = Debug;")
lines.append("\t\t};")

# Target Release
lines.append(f"\t\t{gid('CFG_Target_Release')} /* Release */ = {{")
lines.append("\t\t\tisa = XCBuildConfiguration;")
lines.append("\t\t\tbuildSettings = {")
lines.append("\t\t\t\tASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;")
lines.append("\t\t\t\tASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;")
lines.append("\t\t\t\tCODE_SIGN_STYLE = Automatic;")
lines.append("\t\t\t\tCODE_SIGNING_ALLOWED = NO;")
lines.append("\t\t\t\tCURRENT_PROJECT_VERSION = 1;")
lines.append("\t\t\t\tDEVELOPMENT_TEAM = \"\";")
lines.append("\t\t\t\tENABLE_PREVIEWS = YES;")
lines.append("\t\t\t\tGENERATE_INFOPLIST_FILE = NO;")
lines.append("\t\t\t\tINFOPLIST_FILE = Kotoba/Resources/Info.plist;")
lines.append("\t\t\t\tINFOPLIST_KEY_CFBundleDisplayName = Kotoba;")
lines.append("\t\t\t\tINFOPLIST_KEY_LSApplicationCategoryType = \"public.app-category.education\";")
lines.append("\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 16.0;")
lines.append("\t\t\t\tLD_RUNPATH_SEARCH_PATHS = (")
lines.append("\t\t\t\t\t\"$(inherited)\",")
lines.append("\t\t\t\t\t\"@executable_path/Frameworks\",")
lines.append("\t\t\t\t);")
lines.append("\t\t\t\tMARKETING_VERSION = 1.0;")
lines.append("\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = com.kotoba.app;")
lines.append("\t\t\t\tPRODUCT_NAME = \"$(TARGET_NAME)\";")
lines.append("\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;")
lines.append("\t\t\t\tSWIFT_VERSION = 5.0;")
lines.append("\t\t\t\tTARGETED_DEVICE_FAMILY = \"1,2\";")
lines.append("\t\t\t};")
lines.append("\t\t\tname = Release;")
lines.append("\t\t};")

lines.append("/* End XCBuildConfiguration section */")

# 10. XCConfigurationList
lines.append("\n/* Begin XCConfigurationList section */")
lines.append(f"\t\t{gid('BCL_Project')} /* Build configuration list for PBXProject \"Kotoba\" */ = {{")
lines.append("\t\t\tisa = XCConfigurationList;")
lines.append("\t\t\tbuildConfigurations = (")
lines.append(f"\t\t\t\t{gid('CFG_Proj_Debug')} /* Debug */,")
lines.append(f"\t\t\t\t{gid('CFG_Proj_Release')} /* Release */,")
lines.append("\t\t\t);")
lines.append("\t\t\tdefaultConfigurationIsVisible = 0;")
lines.append("\t\t\tdefaultConfigurationName = Release;")
lines.append("\t\t};")

lines.append(f"\t\t{gid('BCL_Target')} /* Build configuration list for PBXNativeTarget \"Kotoba\" */ = {{")
lines.append("\t\t\tisa = XCConfigurationList;")
lines.append("\t\t\tbuildConfigurations = (")
lines.append(f"\t\t\t\t{gid('CFG_Target_Debug')} /* Debug */,")
lines.append(f"\t\t\t\t{gid('CFG_Target_Release')} /* Release */,")
lines.append("\t\t\t);")
lines.append("\t\t\tdefaultConfigurationIsVisible = 0;")
lines.append("\t\t\tdefaultConfigurationName = Release;")
lines.append("\t\t};")
lines.append("/* End XCConfigurationList section */")

lines.append("\t};")
lines.append(f"\trootObject = {gid('PROJECT')} /* Project object */;")
lines.append("}")

output_path = "ios/Kotoba.xcodeproj/project.pbxproj"
os.makedirs(os.path.dirname(output_path), exist_ok=True)
with open(output_path, "w", encoding="utf-8") as f:
    f.write("\n".join(lines) + "\n")

print(f"Generated {output_path} with {len(lines)} lines successfully.")
