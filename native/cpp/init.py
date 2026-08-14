import os
import shutil
import sys
import tempfile
import time
import urllib.request
import zipfile
from pathlib import Path


GLSLANG_COMMIT_HASH = "a8d28b"
GLSLANG_RELEASE_BASE_URL = (
    "https://github.com/187J3X1-114514/glslang-prebuilt/releases/download"
)
GLSLANG_PACKAGES = (
    ("windows-x86_64", "release", "glslang-windows-release.zip"),
    ("windows-x86_64", "debug", "glslang-windows-debug.zip"),
    ("linux-x86_64", "release", "glslang-linux-release.zip"),
    ("linux-x86_64", "debug", "glslang-linux-debug.zip"),
)

SCRIPT_DIR = Path(__file__).resolve().parent
GLSLANG_PREBUILT_ROOT = SCRIPT_DIR / "prebuilt" / "glslang"
GLSLANG_COMMIT_HASH_FILE = GLSLANG_PREBUILT_ROOT / "commit-hash.txt"

GLSLANG_LIBRARIES = (
    "GenericCodeGen",
    "glslang-default-resource-limits",
    "glslang",
    "MachineIndependent",
    "OSDependent",
    "SPIRV-Tools-opt",
    "SPIRV-Tools",
    "SPIRV",
)


def expected_files(platform: str, config: str) -> tuple[str, ...]:
    if platform.startswith("windows-"):
        debug_suffix = "d" if config == "debug" else ""
        libraries = tuple(
            f"lib/{library}{debug_suffix}.lib"
            for library in GLSLANG_LIBRARIES
        )
    else:
        libraries = tuple(
            f"lib/lib{library}.a"
            for library in GLSLANG_LIBRARIES
        )
    return libraries


def missing_files(
    install_dir: Path, platform: str, config: str
) -> list[str]:
    return [
        relative_path
        for relative_path in expected_files(platform, config)
        if not (install_dir / relative_path).is_file()
    ]


def download_file(url: str, destination: Path) -> None:
    request = urllib.request.Request(
        url, headers={"User-Agent": "SuperResolution-init"}
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        total_size = int(response.headers.get("Content-Length", 0))
        downloaded_size = 0
        next_report_size = 64 * 1024 * 1024

        with destination.open("wb") as output:
            while True:
                chunk = response.read(1024 * 1024)
                if not chunk:
                    break
                output.write(chunk)
                downloaded_size += len(chunk)

                if downloaded_size >= next_report_size:
                    if total_size:
                        print(
                            f"  Downloaded "
                            f"{downloaded_size / 1024 / 1024:.0f}/"
                            f"{total_size / 1024 / 1024:.0f} MiB"
                        )
                    else:
                        print(
                            f"  Downloaded "
                            f"{downloaded_size / 1024 / 1024:.0f} MiB"
                        )
                    next_report_size += 64 * 1024 * 1024


def download_with_retries(url: str, destination: Path) -> None:
    for attempt in range(1, 4):
        try:
            download_file(url, destination)
            return
        except Exception:
            destination.unlink(missing_ok=True)
            if attempt == 3:
                raise
            delay = 2**attempt
            print(f"  Download failed, retrying in {delay} seconds...")
            time.sleep(delay)


def extract_archive(archive_path: Path, destination: Path) -> None:
    destination_root = destination.resolve()
    with zipfile.ZipFile(archive_path) as archive:
        for member in archive.infolist():
            member_path = (destination / member.filename).resolve()
            if os.path.commonpath(
                (str(destination_root), str(member_path))
            ) != str(destination_root):
                raise RuntimeError(
                    f"Archive contains an unsafe path: {member.filename}"
                )
        archive.extractall(destination)


def install_package(
    platform: str, config: str, package_name: str
) -> None:
    install_dir = (
        GLSLANG_PREBUILT_ROOT
        / f"{GLSLANG_COMMIT_HASH}-{platform}-{config}"
    )
    missing = missing_files(install_dir, platform, config)
    if not missing:
        print(f"{package_name}: already installed")
        return

    url = (
        f"{GLSLANG_RELEASE_BASE_URL}/"
        f"{GLSLANG_COMMIT_HASH}/{package_name}"
    )
    archive_path = GLSLANG_PREBUILT_ROOT / f".{package_name}.download"
    extract_dir = Path(
        tempfile.mkdtemp(
            prefix=f".{package_name}.",
            dir=GLSLANG_PREBUILT_ROOT,
        )
    )

    try:
        print(f"Downloading {url}")
        download_with_retries(url, archive_path)
        print(f"Extracting {package_name}")
        extract_archive(archive_path, extract_dir)

        missing = missing_files(extract_dir, platform, config)
        if missing:
            formatted_missing = "\n  ".join(missing)
            raise RuntimeError(
                f"{package_name} is missing required files:\n"
                f"  {formatted_missing}"
            )

        if install_dir.exists():
            shutil.rmtree(install_dir)
        extract_dir.replace(install_dir)
        print(f"Installed {install_dir}")
    finally:
        archive_path.unlink(missing_ok=True)
        if extract_dir.exists():
            shutil.rmtree(extract_dir)


def write_commit_hash_file() -> None:
    temporary_commit_hash_file = GLSLANG_COMMIT_HASH_FILE.with_suffix(
        ".txt.tmp"
    )
    temporary_commit_hash_file.write_text(
        f"{GLSLANG_COMMIT_HASH}\n", encoding="utf-8"
    )
    temporary_commit_hash_file.replace(GLSLANG_COMMIT_HASH_FILE)


def main() -> int:
    print(f"Initializing glslang commit {GLSLANG_COMMIT_HASH}...")
    GLSLANG_PREBUILT_ROOT.mkdir(parents=True, exist_ok=True)

    try:
        for platform, config, package_name in GLSLANG_PACKAGES:
            install_package(platform, config, package_name)
        write_commit_hash_file()
    except Exception as error:
        print(f"Failed to initialize glslang: {error}", file=sys.stderr)
        return 1

    print("glslang initialization complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
