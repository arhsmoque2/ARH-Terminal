#!/usr/bin/env python3
"""
Remote APK Builder & CI Orchestrator
Enables sandboxed cloud agents to trigger GitHub Actions builds, stream logs on failure,
and download compiled APK artifacts without needing local network access to dl.google.com.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
import zipfile
from io import BytesIO
from pathlib import Path
from urllib.parse import urlparse

# Ensure UTF-8 output even on legacy Windows code page consoles (cp1252)
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

REPO_ROOT = Path(__file__).resolve().parent.parent


class SafeRedirectHandler(urllib.request.HTTPRedirectHandler):
    """Redirect handler that strips Authorization headers when redirected off api.github.com (e.g. Azure Blob)."""
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        new_req = super().redirect_request(req, fp, code, msg, headers, newurl)
        if new_req is not None:
            old_host = urlparse(req.full_url).hostname
            new_host = urlparse(newurl).hostname
            if old_host != new_host and new_req.has_header("Authorization"):
                new_req.remove_header("Authorization")
        return new_req


def get_token() -> str:
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_PAT")
    if not token:
        try:
            res = subprocess.run(["gh", "auth", "token"], capture_output=True, text=True, check=True)
            token = res.stdout.strip()
        except Exception:
            pass
    if not token:
        print("[ERROR] GitHub token not found. Set GITHUB_TOKEN, GH_TOKEN, GITHUB_PAT, or authenticate with `gh auth login`.", file=sys.stderr)
        sys.exit(1)
    return token


def get_repo_slug(explicit_repo: str | None = None) -> str:
    if explicit_repo:
        return explicit_repo
    try:
        remote_url = subprocess.run(
            ["git", "-C", str(REPO_ROOT), "config", "--get", "remote.origin.url"],
            capture_output=True, text=True, check=True
        ).stdout.strip()
    except subprocess.CalledProcessError:
        print("[ERROR] Could not resolve git remote.origin.url. Run inside a git repository or specify --repo.", file=sys.stderr)
        sys.exit(1)

    match = re.search(r"github\.com[:/](?P<owner>[^/]+)/(?P<repo>[^/.]+)(?:\.git)?", remote_url)
    if not match:
        print(f"[ERROR] Could not parse owner/repo from remote URL: {remote_url}", file=sys.stderr)
        sys.exit(1)
    return f"{match.group('owner')}/{match.group('repo')}"


def github_api_request(url: str, token: str, method: str = "GET", data: dict = None) -> dict | bytes:
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "User-Agent": "ARH-Terminal-Remote-APK-Builder/1.0",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    payload = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=payload, headers=headers, method=method)

    opener = urllib.request.build_opener(SafeRedirectHandler())
    try:
        with opener.open(req) as resp:
            content_type = resp.headers.get("Content-Type", "")
            body = resp.read()
            if "application/json" in content_type:
                return json.loads(body.decode("utf-8"))
            return body
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")
        print(f"[ERROR] HTTP {e.code} on {url}: {err_body}", file=sys.stderr)
        raise


def get_current_ref() -> str:
    try:
        branch = subprocess.run(
            ["git", "-C", str(REPO_ROOT), "rev-parse", "--abbrev-ref", "HEAD"],
            capture_output=True, text=True, check=True
        ).stdout.strip()
        if branch and branch != "HEAD":
            return branch
        return subprocess.run(
            ["git", "-C", str(REPO_ROOT), "rev-parse", "HEAD"],
            capture_output=True, text=True, check=True
        ).stdout.strip()
    except Exception:
        return "main"


def trigger_workflow(repo: str, token: str, workflow_file: str, ref: str) -> None:
    url = f"https://api.github.com/repos/{repo}/actions/workflows/{workflow_file}/dispatches"
    payload = {"ref": ref}
    print(f"[*] Dispatching workflow '{workflow_file}' on repo '{repo}' (ref: {ref})...")
    github_api_request(url, token, method="POST", data=payload)
    print("[+] Workflow dispatch accepted by GitHub.")


def find_workflow_run(repo: str, token: str, workflow_file: str, triggered_after_ts: float) -> dict | None:
    url = f"https://api.github.com/repos/{repo}/actions/workflows/{workflow_file}/runs?per_page=5"
    data = github_api_request(url, token)
    for run in data.get("workflow_runs", []):
        created_at = time.mktime(time.strptime(run["created_at"], "%Y-%m-%dT%H:%M:%SZ"))
        if created_at >= (triggered_after_ts - 30):
            return run
    return None


def get_latest_successful_run(repo: str, token: str, workflow_file: str) -> dict | None:
    url = f"https://api.github.com/repos/{repo}/actions/workflows/{workflow_file}/runs?status=success&per_page=1"
    data = github_api_request(url, token)
    runs = data.get("workflow_runs", [])
    return runs[0] if runs else None


def fetch_failed_logs(repo: str, token: str, run_id: int) -> None:
    print("\n" + "=" * 60)
    print(f"[*] Fetching failure diagnostic logs for Run #{run_id}...")
    print("=" * 60)
    jobs_url = f"https://api.github.com/repos/{repo}/actions/runs/{run_id}/jobs"
    jobs_data = github_api_request(jobs_url, token)
    for job in jobs_data.get("jobs", []):
        if job.get("conclusion") == "failure":
            print(f"\n[FAILED JOB]: {job.get('name')} (ID: {job.get('id')})")
            for step in job.get("steps", []):
                if step.get("conclusion") == "failure":
                    print(f"  -> Failed Step: {step.get('name')} (Number: {step.get('number')})")
    
    print(f"\nTip: View complete run logs at: https://github.com/{repo}/actions/runs/{run_id}")


def download_apk_artifacts(repo: str, token: str, run_id: int, output_dir: Path, target_type: str = "all") -> list[Path]:
    url = f"https://api.github.com/repos/{repo}/actions/runs/{run_id}/artifacts"
    data = github_api_request(url, token)
    artifacts = data.get("artifacts", [])

    if not artifacts:
        print("[!] No artifacts found for this run.", file=sys.stderr)
        return []

    output_dir.mkdir(parents=True, exist_ok=True)
    downloaded_files = []

    type_filters = {
        "debug": ["debug"],
        "release": ["release"],
        "all": ["debug", "release", "apk"]
    }
    keywords = type_filters.get(target_type, ["apk"])

    for art in artifacts:
        art_id = art["id"]
        art_name = art["name"]

        if not any(k in art_name.lower() for k in keywords):
            continue

        download_url = f"https://api.github.com/repos/{repo}/actions/artifacts/{art_id}/zip"
        print(f"[*] Downloading artifact '{art_name}' ({art['size_in_bytes']} bytes)...")

        zip_bytes = github_api_request(download_url, token)
        with zipfile.ZipFile(BytesIO(zip_bytes)) as z:
            for zip_info in z.infolist():
                if zip_info.filename.endswith(".apk"):
                    extracted_path = output_dir / Path(zip_info.filename).name
                    with z.open(zip_info) as src, open(extracted_path, "wb") as dst:
                        dst.write(src.read())
                    print(f"[+] Saved APK: {extracted_path.resolve()}")
                    downloaded_files.append(extracted_path)

    return downloaded_files


def run_doctor_verification(apk_path: Path, is_release: bool = False) -> bool:
    doctor_script = Path(__file__).resolve().parent / "ci_apk_signing_doctor.py"
    if not doctor_script.exists():
        return True

    cmd = [sys.executable, str(doctor_script), str(apk_path)]
    if is_release:
        cmd.append("--release")
    else:
        cmd.extend(["--max-size-mb", "35"])

    print(f"[*] Running signing doctor on {apk_path.name}...")
    res = subprocess.run(cmd)
    return res.returncode == 0


def main():
    parser = argparse.ArgumentParser(description="Trigger, monitor, and download remote Android APK builds from GitHub Actions.")
    parser.add_argument("--workflow", default="ci.yml", help="Workflow YAML filename (default: ci.yml)")
    parser.add_argument("--repo", default=None, help="Target GitHub repo slug (owner/repo). Autodetected from git remote if omitted.")
    parser.add_argument("--ref", default=None, help="Git branch/tag/commit SHA to build (default: current HEAD/branch)")
    parser.add_argument("--type", choices=["debug", "release", "all"], default="all", help="Artifact variant to download (default: all)")
    parser.add_argument("--output-dir", default="./build-outputs", help="Directory to save downloaded APKs (default: ./build-outputs)")
    parser.add_argument("--timeout", type=int, default=1200, help="Polling timeout in seconds (default: 1200s / 20m)")
    parser.add_argument("--from-run-id", type=int, default=None, help="Skip dispatch and download artifacts directly from an existing run ID")
    parser.add_argument("--from-latest", action="store_true", help="Skip dispatch and download artifacts from the latest successful CI run")
    parser.add_argument("--verify", action="store_true", help="Run local APK signing doctor verification on downloaded artifacts")
    args = parser.parse_args()

    token = get_token()
    repo = get_repo_slug(args.repo)
    output_dir = Path(args.output_dir)

    if args.from_run_id or args.from_latest:
        run_id = args.from_run_id
        if args.from_latest:
            latest_run = get_latest_successful_run(repo, token, args.workflow)
            if not latest_run:
                print(f"[ERROR] No successful runs found for workflow '{args.workflow}' on '{repo}'.", file=sys.stderr)
                sys.exit(1)
            run_id = latest_run["id"]
            print(f"[+] Found latest successful run: #{run_id} ({latest_run['html_url']})")

        print(f"[*] Downloading artifacts for Run #{run_id}...")
        apks = download_apk_artifacts(repo, token, run_id, output_dir, args.type)
        if not apks:
            print("[!] No matching APKs downloaded.", file=sys.stderr)
            sys.exit(1)

        print("\n[+] Download complete:")
        for apk in apks:
            print(f"  - {apk.resolve()} ({apk.stat().st_size / (1024 * 1024):.2f} MB)")
            if args.verify:
                is_release = "release" in apk.name.lower()
                run_doctor_verification(apk, is_release)
        sys.exit(0)

    ref = args.ref or get_current_ref()
    trigger_ts = time.time()
    trigger_workflow(repo, token, args.workflow, ref)

    print("[*] Waiting for GitHub Actions run to initialize...")
    run = None
    for _ in range(30):
        time.sleep(3)
        run = find_workflow_run(repo, token, args.workflow, trigger_ts)
        if run:
            break

    if not run:
        print("[ERROR] Timed out waiting for workflow run to register.", file=sys.stderr)
        sys.exit(1)

    run_id = run["id"]
    html_url = run["html_url"]
    print(f"[+] Workflow run detected: ID {run_id}")
    print(f"    URL: {html_url}")

    start_time = time.time()
    while True:
        elapsed = int(time.time() - start_time)
        if elapsed > args.timeout:
            print(f"\n[ERROR] Build timed out after {args.timeout}s.", file=sys.stderr)
            sys.exit(1)

        run_status_url = f"https://api.github.com/repos/{repo}/actions/runs/{run_id}"
        run_data = github_api_request(run_status_url, token)
        status = run_data.get("status")
        conclusion = run_data.get("conclusion")

        print(f"\r[*] [{elapsed}s] Run #{run_id} status: {status} (conclusion: {conclusion or 'in-progress'})...", end="", flush=True)

        if status == "completed":
            print()
            if conclusion == "success":
                print(f"[SUCCESS] Remote build completed successfully in {elapsed}s!")
                apks = download_apk_artifacts(repo, token, run_id, output_dir, args.type)
                if apks:
                    print("\n[+] Build artifacts ready:")
                    for apk in apks:
                        print(f"  - {apk.resolve()} ({apk.stat().st_size / (1024 * 1024):.2f} MB)")
                        if args.verify:
                            is_release = "release" in apk.name.lower()
                            run_doctor_verification(apk, is_release)
                sys.exit(0)
            else:
                print(f"\n[FAILURE] Build failed with conclusion: {conclusion}")
                fetch_failed_logs(repo, token, run_id)
                sys.exit(1)

        time.sleep(8)


if __name__ == "__main__":
    main()
