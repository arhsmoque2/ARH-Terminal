#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path

def main():
    root = Path(__file__).resolve().parent.parent
    cap_file = root / "capabilities.json"
    readme_file = root / "README.md"
    registry_file = root / "core" / "core-mcp" / "src" / "main" / "java" / "com" / "arh" / "terminal" / "core" / "mcp" / "tools" / "AndroidToolRegistry.kt"

    print("=== [As-Built Doctor] Running Architecture & Spec Conformance Audit ===")
    
    # 1. Verify capabilities.json vs AndroidToolRegistry.kt
    cap_data = json.loads(cap_file.read_text(encoding="utf-8"))
    declared_tools = set(t["name"] for t in cap_data["tools"])
    
    registry_text = registry_file.read_text(encoding="utf-8")
    registered_tools = set(re.findall(r'register\(\s*"(android_\w+)"', registry_text))
    
    print(f"Spec Declared Tools: {len(declared_tools)}")
    print(f"Code Registered Tools: {len(registered_tools)}")
    
    missing_in_code = declared_tools - registered_tools
    extra_in_code = registered_tools - declared_tools
    
    if missing_in_code:
        print(f"[FAIL] Tools in capabilities.json missing from AndroidToolRegistry: {missing_in_code}")
        sys.exit(1)
        
    if extra_in_code:
        print(f"[FAIL] Tools in AndroidToolRegistry missing from capabilities.json: {extra_in_code}")
        sys.exit(1)
        
    print("[PASS] Tool registry strictly conforms to capabilities.json manifest.")

    # 2. Verify Documentation Claims (README)
    readme_text = readme_file.read_text(encoding="utf-8")
    tool_claim_match = re.search(r"(\d+)\s+Native\s+Tools", readme_text, re.IGNORECASE)
    if tool_claim_match:
        claimed_count = int(tool_claim_match.group(1))
        if claimed_count != len(declared_tools):
            print(f"[FAIL] README.md claims {claimed_count} tools, but capabilities.json has {len(declared_tools)}!")
            sys.exit(1)
        print(f"[PASS] README tool count claim ({claimed_count}) strictly matches manifest.")

    # 3. Check for hollow mock leakage in production code
    mcp_dir = root / "core" / "core-mcp" / "src" / "main"
    for kt_file in mcp_dir.rglob("*.kt"):
        text = kt_file.read_text(encoding="utf-8")
        if "DefaultAccessibilityBridge" in kt_file.name:
            continue
        if 'nodesCount", 12' in text:
            print(f"[FAIL] Suspicious hollow mock found in {kt_file.name}")
            sys.exit(1)

    print("[PASS] All As-Built vs Declared conformance checks PASSED!")

if __name__ == "__main__":
    main()
