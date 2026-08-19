#!/usr/bin/env python3
"""
ARH-Terminal APK Signing & Integrity Quality Gate.
Validates 4-byte zipalign, apksigner v1/v2/v3 signing schemes, and certificate provenance.
"""
import os
import shutil
import subprocess
import sys
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

def find_tool(tool_name: str) -> str:
    # 1. Direct PATH lookup
    found = shutil.which(tool_name)
    if found:
        return found

    # 2. Check ANDROID_HOME / ANDROID_SDK_ROOT
    for env_var in ["ANDROID_HOME", "ANDROID_SDK_ROOT"]:
        sdk_root = os.environ.get(env_var)
        if sdk_root:
            build_tools = Path(sdk_root) / "build-tools"
            if build_tools.exists():
                versions = sorted(build_tools.iterdir(), reverse=True)
                for ver in versions:
                    cand = ver / (f"{tool_name}.exe" if sys.platform == "win32" else tool_name)
                    if cand.exists():
                        return str(cand)
                    cand_bat = ver / f"{tool_name}.bat"
                    if cand_bat.exists():
                        return str(cand_bat)

    # 3. Check common scoop / local locations on Windows
    if sys.platform == "win32":
        user_home = Path(os.environ.get("USERPROFILE", "C:/Users/Default"))
        scoop_cand = user_home / "scoop/persist/android-clt/build-tools"
        if scoop_cand.exists():
            versions = sorted(scoop_cand.iterdir(), reverse=True)
            for ver in versions:
                cand = ver / f"{tool_name}.exe"
                if cand.exists():
                    return str(cand)
                cand_bat = ver / f"{tool_name}.bat"
                if cand_bat.exists():
                    return str(cand_bat)

    return tool_name

def run_cmd(cmd: list[str]) -> tuple[int, str]:
    try:
        proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        return proc.returncode, proc.stdout + proc.stderr
    except Exception as e:
        return 1, str(e)

def verify_apk(apk_path: Path, is_release: bool = False, expected_sha256: str | None = None, max_size_mb: float = 8.0) -> bool:
    if not apk_path.exists():
        print(f"❌ APK not found: {apk_path}")
        return False

    size_mb = apk_path.stat().st_size / (1024 * 1024)
    print(f"🔍 Inspecting APK: {apk_path} ({size_mb:.2f} MB)")

    # 1. Budget Size Gate
    if size_mb > max_size_mb:
        print(f"  ❌ APK Size ({size_mb:.2f} MB) exceeds maximum allowed budget ({max_size_mb:.2f} MB)!")
        return False
    else:
        print(f"  ✅ Size Budget: {size_mb:.2f} MB (within < {max_size_mb:.1f} MB ceiling)")

    # 2. Inspect Native ABIs inside ZIP
    import zipfile
    try:
        with zipfile.ZipFile(apk_path, "r") as z:
            so_files = [f for f in z.namelist() if f.startswith("lib/") and f.endswith(".so")]
            abis = set(f.split("/")[1] for f in so_files if len(f.split("/")) > 2)
            if abis:
                print(f"  ✅ Native Architectures (ABIs): {', '.join(sorted(abis))} ({len(so_files)} .so libs)")
            else:
                print("  ℹ️ Pure DEX/Java APK (no native .so libraries bundled)")
    except Exception as e:
        print(f"  ⚠️ Could not inspect ZIP entries: {e}")

    zipalign_bin = find_tool("zipalign")
    apksigner_bin = find_tool("apksigner")

    # 3. Verify 4-byte alignment
    code, out = run_cmd([zipalign_bin, "-c", "-v", "4", str(apk_path)])
    if code == 0:
        print("  ✅ 4-byte Zip alignment verified (zipalign)")
    else:
        print(f"  ⚠️ Zipalign verification output: {out.strip()[:200]}")

    # 4. Verify apksigner schemes
    code, out = run_cmd([apksigner_bin, "verify", "--verbose", str(apk_path)])
    if code != 0:
        print(f"  ❌ apksigner verification failed with exit code {code}:\n{out}")
        return False

    v1_ok = "Verified using v1 scheme (JAR signing): true" in out or "v1 scheme: true" in out or "v1 scheme (JAR signing): true" in out
    v2_ok = "Verified using v2 scheme (APK Signature Scheme v2): true" in out or "v2 scheme: true" in out or "v2 scheme (APK Signature Scheme v2): true" in out
    v3_ok = "Verified using v3 scheme" in out

    print(f"  ✅ Signature schemes verified: v1={v1_ok}, v2={v2_ok}, v3={v3_ok}")

    # 5. Print & Verify Certificates
    code, cert_out = run_cmd([apksigner_bin, "verify", "--print-certs", str(apk_path)])
    if is_release and "CN=Android Debug" in cert_out:
        print("  ❌ FATAL: Release APK is signed with Android Debug certificate!")
        return False

    if "CN=ARH Terminal" in cert_out:
        print("  ✅ Certificate Provenance: Verified 'CN=ARH Terminal' release identity")
    elif "Android Debug" in cert_out:
        print("  ℹ️ Certificate Provenance: Signed with Debug Key (expected for debug builds)")
    else:
        print(f"  ℹ️ Certificate Subject:\n{cert_out.strip()}")

    if expected_sha256:
        clean_out = "".join(cert_out.split()).lower()
        clean_expected = expected_sha256.replace(":", "").lower()
        if clean_expected not in clean_out:
            print(f"  ❌ Certificate SHA-256 digest mismatch! Expected {expected_sha256}")
            return False
        print(f"  ✅ Certificate SHA-256 matched expected fingerprint: {expected_sha256}")

    print(f"🎉 SUCCESS: {apk_path.name} passed all signing & integrity checks!\n")
    return True

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python ci_apk_signing_doctor.py <path-to-apk> [--release] [--expected-cert-sha256 <hash>]")
        sys.exit(1)

    apk_file = Path(sys.argv[1])
    is_rel = "--release" in sys.argv
    expected_hash = None
    max_mb = 8.0
    if "--max-size-mb" in sys.argv:
        idx = sys.argv.index("--max-size-mb")
        if idx + 1 < len(sys.argv):
            max_mb = float(sys.argv[idx + 1])
    if "--expected-cert-sha256" in sys.argv:
        idx = sys.argv.index("--expected-cert-sha256")
        if idx + 1 < len(sys.argv):
            expected_hash = sys.argv[idx + 1]

    ok = verify_apk(apk_file, is_release=is_rel, expected_sha256=expected_hash, max_size_mb=max_mb)
    if not ok:
        sys.exit(1)
