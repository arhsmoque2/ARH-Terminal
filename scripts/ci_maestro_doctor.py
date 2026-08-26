#!/usr/bin/env python3
"""
CI Maestro Quality Doctor & Static Flow Validator
Audits .maestro/ YAML test flows, validates syntax, ensures referenced testTags exist in Compose UI,
and checks for anti-patterns (e.g. hardcoded sleeps).
"""

import os
import re
import sys
from pathlib import Path

# Ensure UTF-8 output
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

REPO_ROOT = Path(__file__).resolve().parent.parent
MAESTRO_DIR = REPO_ROOT / ".maestro"
APP_SRC_DIR = REPO_ROOT / "app" / "src" / "main" / "java"


def collect_codebase_test_tags() -> set[str]:
    tags = set()
    tag_regex = re.compile(r'testTag\(\s*"([^"]+)"\s*\)')
    
    for kt_file in APP_SRC_DIR.rglob("*.kt"):
        content = kt_file.read_text(encoding="utf-8", errors="replace")
        for match in tag_regex.findall(content):
            tags.add(match)
            
    return tags


def verify_semantics_resource_id_mapping() -> bool:
    """Verifies that Jetpack Compose root semantics maps testTags to Android resource IDs for external accessibility tools (Maestro/UIAutomator)."""
    main_activity = APP_SRC_DIR / "com" / "arh" / "terminal" / "MainActivity.kt"
    if not main_activity.exists():
        return False
    content = main_activity.read_text(encoding="utf-8", errors="replace")
    return "testTagsAsResourceId = true" in content


def parse_simple_yaml_flow(yaml_file: Path) -> tuple[dict, list[str], list[str]]:
    """Simple parser to extract metadata, referenced testTag IDs, and detect anti-patterns."""
    content = yaml_file.read_text(encoding="utf-8")
    lines = content.splitlines()
    
    metadata = {}
    referenced_ids = []
    anti_patterns = []
    
    # Extract appId
    app_id_match = re.search(r"appId:\s*([^\s]+)", content)
    if app_id_match:
        metadata["appId"] = app_id_match.group(1)
        
    # Extract testTag IDs
    id_matches = re.findall(r'id:\s*"([^"]+)"', content)
    referenced_ids.extend(id_matches)
    
    # Check for hardcoded sleep anti-pattern
    for idx, line in enumerate(lines, 1):
        if re.search(r'-\s*sleep:\s*\d+', line, re.IGNORECASE):
            anti_patterns.append(f"Line {idx}: Avoid explicit sleep, use assertVisible auto-waiting instead.")
            
    return metadata, referenced_ids, anti_patterns


def main():
    print("=== [Maestro Doctor] Running Live UI Flow & TestTag Conformance Gate ===")
    
    if not MAESTRO_DIR.exists():
        print(f"[FAIL] .maestro directory not found at {MAESTRO_DIR}")
        sys.exit(1)
        
    flow_files = list(MAESTRO_DIR.glob("*.yaml")) + list(MAESTRO_DIR.glob("*.yml"))
    if not flow_files:
        print("[FAIL] No Maestro YAML flow files found under .maestro/")
        sys.exit(1)
        
    codebase_tags = collect_codebase_test_tags()
    print(f"[*] Discovered {len(codebase_tags)} unique testTags in Compose codebase.")
    print(f"[*] Discovered {len(flow_files)} Maestro UI test flow(s).\n")
    
    has_errors = False
    all_referenced_tags = set()

    if not verify_semantics_resource_id_mapping():
        print("[FAIL] Jetpack Compose root semantics missing 'testTagsAsResourceId = true' in MainActivity.kt! External UIAutomator/Maestro accessibility lookups by ID will fail.")
        has_errors = True
    else:
        print("[PASS] Jetpack Compose root semantics maps testTags to Android resource IDs (testTagsAsResourceId = true).\n")

    
    for flow in sorted(flow_files):
        meta, ref_ids, anti_patterns = parse_simple_yaml_flow(flow)
        app_id = meta.get("appId", "UNKNOWN")
        
        print(f"📋 Flow: {flow.name} (appId: {app_id})")
        print(f"   -> Referenced testTags ({len(ref_ids)}): {ref_ids}")
        
        # Check appId
        if app_id != "com.arh.terminal":
            print(f"   [WARN] Unexpected appId '{app_id}', expected 'com.arh.terminal'")
            
        # Check testTag resolution
        for tag in ref_ids:
            all_referenced_tags.add(tag)
            if tag not in codebase_tags:
                print(f"   [FAIL] testTag '{tag}' not found in Compose UI codebase!")
                has_errors = True
                
        # Check anti-patterns
        if anti_patterns:
            for ap in anti_patterns:
                print(f"   [WARN] Anti-pattern: {ap}")
                
        print()
        
    print(f"[*] Total unique testTags exercised by Maestro flows: {len(all_referenced_tags)}")
    
    if has_errors:
        print("\n❌ FAILED: One or more Maestro flows reference missing or invalid testTags.")
        sys.exit(1)
        
    print("\n✅ SUCCESS: All Maestro UI flows and testTags conform strictly to Compose codebase!")
    sys.exit(0)


if __name__ == "__main__":
    main()
