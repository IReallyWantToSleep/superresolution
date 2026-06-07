import re
import subprocess
import requests
import boto3
import json
import os
import sys
import argparse
import hashlib
import base64
from pathlib import Path
from dataclasses import dataclass
from Crypto.Cipher import AES

TARGET_BUCKET = "sr-release-distribution"


def get_git_commit_hash(short: bool = False) -> str:
    cmd = (
        ["git", "rev-parse", "--short", "HEAD"]
        if short
        else ["git", "rev-parse", "HEAD"]
    )
    result = subprocess.run(cmd, capture_output=True, text=True)
    result.check_returncode()
    return result.stdout.strip()


def fetch_mc_all_versions() -> list:
    response = requests.get(
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    )
    if not response.ok:
        raise Exception("Failed to fetch minecraft version from mojang`s api")
    data = json.loads(response.text)
    release_versions = []
    for version in data["versions"]:
        if version["type"] == "release":
            release_versions.append(version["id"])
    return release_versions


@dataclass
class ModFileInfo:
    mod_name: str
    loader: str
    verison: str
    mc_versions: list
    label: list
    is_dev: bool


@dataclass
class ModFile:
    info: ModFileInfo
    path: Path


MC_ALL_VERSIONS = fetch_mc_all_versions()


def parse_mc_version_range(rangestr: str) -> list:
    begin = rangestr.split("..")[0]
    end = rangestr.split("..")[1]
    begin_index = MC_ALL_VERSIONS.index(begin)
    end_index = MC_ALL_VERSIONS.index(end)
    return MC_ALL_VERSIONS[end_index : begin_index + 1]


def parse_jar_name(name: str) -> ModFileInfo:
    if name.endswith(".jar"):
        name = name[:-4]

    if "+" in name:
        base, labels_str = name.split("+", 1)
        labels = labels_str.split(".")
    else:
        base = name
        labels = []

    version_match = re.search(r"-(\d+\.\d+\.\d+(?:-[a-z]+(?:\.[\d]+)*)*)$", base)
    if not version_match:
        raise ValueError(f"failed to parse version from version str: {name}")

    version = version_match.group(1)
    remaining = base[: version_match.start()]

    parts = remaining.split("-")
    mc_versions_str = parts[-1]
    loader = parts[-2]
    mod_name = "-".join(parts[:-2])

    if ".." in mc_versions_str:
        mc_versions = parse_mc_version_range(mc_versions_str)
    else:
        mc_versions = [mc_versions_str]
    is_dev = "dev" in labels
    if is_dev:
        labels.remove("dev")
    return ModFileInfo(
        mod_name=mod_name,
        loader=loader,
        verison=version,
        mc_versions=mc_versions,
        label=labels,
        is_dev=is_dev,
    )


def scan_dir_mod_files(target: Path) -> list:
    files = os.listdir(target)
    modfiles = []
    for file in files:
        try:
            modfile = ModFile(parse_jar_name(file), target / file)
            modfiles.append(modfile)
        except:
            pass
    return modfiles


def decrypt_r2_credentials(encrypted_b64: str, token: str) -> dict:
    key = hashlib.sha256(token.encode()).digest()
    combined = base64.b64decode(encrypted_b64)
    iv = combined[:12]
    ciphertext = combined[12:-16]
    tag = combined[-16:]
    cipher = AES.new(key, AES.MODE_GCM, nonce=iv)
    plaintext = cipher.decrypt_and_verify(ciphertext, tag)
    return json.loads(plaintext)


def get_r2_endpoint(endpoint: str, token: str) -> dict:
    response = requests.get(
        f"{endpoint}/get_r2_endpoint",
        headers={"Authorization": f"Bearer {token}"},
    )
    response.raise_for_status()
    data = response.json()
    if not data["error"]["ok"]:
        raise Exception(f"API error: {data['error']['messages']}")
    return decrypt_r2_credentials(data["data"]["data"], token)


def add_commit_hash(commit_hash: str, endpoint: str, token: str):
    response = requests.post(
        f"{endpoint}/versions/add_commit_hash",
        json={"commit_hash": commit_hash},
        headers={"Authorization": f"Bearer {token}"},
    )
    if response.status_code == 409:
        print(f"Commit hash {commit_hash} already exists, continuing...")
        return
    response.raise_for_status()
    data = response.json()
    if not data["error"]["ok"]:
        raise Exception(f"API error: {data['error']['messages']}")


def add_version(
    commit_hash: str, is_nightly: bool, info: ModFileInfo, endpoint: str, token: str
) -> str:
    response = requests.post(
        f"{endpoint}/versions/add_versions",
        json={
            "commit_hash": commit_hash,
            "is_nightly": is_nightly,
            "version": {
                "mod_name": info.mod_name,
                "loader": info.loader,
                "version": info.verison,
                "mc_versions": info.mc_versions,
                "label": info.label,
                "is_dev": info.is_dev,
            },
        },
        headers={"Authorization": f"Bearer {token}"},
    )
    response.raise_for_status()
    data = response.json()
    if not data["error"]["ok"]:
        raise Exception(f"API error: {data['error']['messages']}")
    return data["data"]["r2_object_name"]


def cleanup_versions(endpoint: str, token: str, keep_latest: int = 5):
    response = requests.post(
        f"{endpoint}/versions/cleanup",
        json={"keep_latest": keep_latest},
        headers={"Authorization": f"Bearer {token}"},
    )
    response.raise_for_status()
    data = response.json()
    if not data.get("error", {}).get("ok", True):
        raise Exception(f"API error: {data['error'].get('messages', 'unknown')}")
    result = data.get("data", {})
    print(
        f"Cleanup: {result.get('deleted_commits', 0)} commits, "
        f"{result.get('deleted_versions', 0)} versions, "
        f"{result.get('deleted_r2_objects', 0)} R2 objects deleted"
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Upload Minecraft mods to R2")
    parser = argparse.ArgumentParser(description="Upload Minecraft mods to R2")
    parser.add_argument("--repo_dir", required=True, help="Git repository directory")
    parser.add_argument(
        "--target_dir", required=True, help="Directory containing mod jar files"
    )
    parser.add_argument("--nightly", action="store_true", help="Mark as nightly build")
    parser.add_argument(
        "--api-endpoint",
        default=os.environ.get("SR_API_ENDPOINT"),
        help="API endpoint URL (default: $SR_API_ENDPOINT)",
    )
    parser.add_argument(
        "--api-token",
        default=os.environ.get("SR_API_TOKEN"),
        help="API Bearer token (default: $SR_API_TOKEN)",
    )
    args = parser.parse_args()

    if not args.api_endpoint:
        parser.error("--api-endpoint is required (or set $SR_API_ENDPOINT)")
    if not args.api_token:
        parser.error("--api-token is required (or set $SR_API_TOKEN)")

    cwd = os.getcwd()
    os.chdir(args.repo_dir)
    commit_hash = get_git_commit_hash()
    os.chdir(cwd)
    print(f"Commit hash: {commit_hash}")

    target = Path(args.target_dir)
    mod_files = scan_dir_mod_files(target)
    if not mod_files:
        print("No mod files found.")
        sys.exit(0)
    print(f"Found {len(mod_files)} mod file(s)")

    print("Fetching R2 credentials...")
    creds = get_r2_endpoint(args.api_endpoint, args.api_token)

    s3 = boto3.client(
        service_name="s3",
        endpoint_url=creds["url"],
        aws_access_key_id=creds["aws_access_key_id"],
        aws_secret_access_key=creds["aws_secret_access_key"],
        region_name="auto",
    )

    print("Registering commit hash...")
    add_commit_hash(commit_hash, args.api_endpoint, args.api_token)

    total = len(mod_files)
    for i, mod_file in enumerate(mod_files, 1):
        info = mod_file.info
        file_size = mod_file.path.stat().st_size
        print(
            f"[{i}/{total}] {info.mod_name} {info.verison} ({file_size / 1024 / 1024:.1f} MB)"
        )

        r2_object_name = add_version(
            commit_hash, args.nightly, info, args.api_endpoint, args.api_token
        )
        print(f"  R2 object: {r2_object_name}")

        last_pct = [0]

        def progress_cb(transferred):
            pct = int(transferred / file_size * 100)
            last_pct[0] = pct
            print(f"  Uploading: {pct}% ({transferred}/{file_size})")

        s3.upload_file(
            str(mod_file.path),
            TARGET_BUCKET,
            r2_object_name,
            Callback=progress_cb,
        )
        print(f"  Done.")

    print(f"All {total} file(s) uploaded.")

    print("Cleaning up old versions...")
    cleanup_versions(args.api_endpoint, args.api_token)
