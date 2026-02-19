#!/bin/bash
set -euo pipefail

#
#这是给Github Actions用的，不是给你
#


# ============================================================================
# Build Windows DLLs on Linux using local msvc-wine (clang + lld mode)
#
# This follows msvc-wine README "Use with Clang/LLD in MSVC mode":
#   BIN=<msvc-root>/bin/x64 . <msvc-root>/msvcenv-native.sh
# Then build with clang-cl/lld-link directly (no Docker).
#
# Usage:
#   ./build_windows_on_linux.sh                        # Build both Debug and Release
#   ./build_windows_on_linux.sh Debug                  # Build Debug only
#   ./build_windows_on_linux.sh Release                # Build Release only
#
# Environment variables:
#   MSVC_WINE_ROOT  - MSVC installation dir      (default: ~/my_msvc)
#   MSVC_WINE_REPO  - msvc-wine git clone dir     (default: ~/msvc-wine)
#   MSVC_WINE_BIN   - bin/x64 override
#   MSVCENV_SCRIPT  - msvcenv-native.sh override
#
# Prerequisites (host packages):
#   clang lld llvm       (clang-cl, lld-link, llvm-dlltool, llvm-mt)
#   cmake ninja-build python3 msitools git
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Build config — match build_windows.ps1 defaults
SR_FSR="${SR_FSR:-ON}"
SR_XESS="${SR_XESS:-ON}"
SR_DLSS="${SR_DLSS:-ON}"
SR_FSROGL="${SR_FSROGL:-OFF}"
BUILD_TYPES="${1:-Debug Release}"

# Local msvc-wine layout (after ./install.sh <dest>)
MSVC_WINE_ROOT="${MSVC_WINE_ROOT:-${HOME}/my_msvc}"
MSVC_WINE_REPO="${MSVC_WINE_REPO:-${HOME}/msvc-wine}"

# Vulkan header fallback in this repo
VULKAN_INCLUDE_DIR="${VULKAN_INCLUDE_DIR:-${SCRIPT_DIR}/third_party}"
VULKAN_STUB_DIR="${SCRIPT_DIR}/buildWindowsOnLinux-vulkan-stub"

# --- Step 0: Check host prerequisites ---
setup() {
    echo "[setup] Checking prerequisites..."
    if [ "$ID" = "debian" ] || [[ "$ID_LIKE" == *debian* ]]; then
        echo "Debian or Debian-based system"
        apt update
        apt install -y clang lld llvm cmake ninja-build python3 msitools wine winbind
    else
        echo "Unsupported Linux distribution. Please install the following packages manually:"
        echo "  clang lld llvm cmake ninja-build python3 msitools wine winbind"
        exit 1
    fi
}
# --- Step 1: Setup msvc-wine (clang+lld mode, no Wine needed) ---
setup_msvc_wine() {
    # 1a. Clone msvc-wine repo if not present
    if [ ! -d "${MSVC_WINE_REPO}" ]; then
        echo "[setup] Cloning msvc-wine into ${MSVC_WINE_REPO} ..."
        git clone https://github.com/mstorsjo/msvc-wine.git "${MSVC_WINE_REPO}"
    else
        echo "[setup] msvc-wine repo already exists at ${MSVC_WINE_REPO}"
    fi

    # 1b. Check for msitools (needed by vsdownload.py)
    if ! command -v msiextract >/dev/null 2>&1; then
        echo "[error] msitools not found. Install it first:"
        echo "        sudo apt-get install -y msitools"
        exit 1
    fi

    # 1c. Download and unpack MSVC/WinSDK (clang+lld mode: no Wine needed)
    if [ ! -f "${MSVC_WINE_ROOT}/msvcenv-native.sh" ]; then
        echo "[setup] Downloading MSVC toolchain into ${MSVC_WINE_ROOT} ..."
        echo "        (This accepts the Microsoft license at https://go.microsoft.com/fwlink/?LinkId=2086102)"
        python3 "${MSVC_WINE_REPO}/vsdownload.py" --accept-license --dest "${MSVC_WINE_ROOT}"

        echo "[setup] Running install.sh to clean up headers and generate env scripts ..."
        bash "${MSVC_WINE_REPO}/install.sh" "${MSVC_WINE_ROOT}"
    else
        echo "[setup] MSVC installation already present at ${MSVC_WINE_ROOT}"
    fi
}

setup_msvc_wine

for cmd in cmake ninja python3; do
    if ! command -v "${cmd}" >/dev/null 2>&1; then
        echo "[error] Missing required command: ${cmd}"
        exit 1
    fi
done

if ! command -v clang-cl >/dev/null 2>&1; then
    echo "[error] clang-cl not found in PATH."
    echo "        Install LLVM clang tools, or add a symlink named clang-cl as suggested by msvc-wine README."
    exit 1
fi

if ! command -v lld-link >/dev/null 2>&1; then
    echo "[error] lld-link not found in PATH."
    echo "        Install LLVM lld tools, or add a symlink named lld-link as suggested by msvc-wine README."
    exit 1
fi

# Resolve paths after setup
MSVC_WINE_BIN="${MSVC_WINE_BIN:-${MSVC_WINE_ROOT}/bin/x64}"
MSVCENV_SCRIPT="${MSVCENV_SCRIPT:-${MSVC_WINE_ROOT}/msvcenv-native.sh}"

if [ ! -f "${MSVCENV_SCRIPT}" ]; then
    echo "[error] msvc-wine env script not found after setup: ${MSVCENV_SCRIPT}"
    exit 1
fi

if [ ! -d "${MSVC_WINE_BIN}" ]; then
    echo "[error] msvc-wine bin dir not found after setup: ${MSVC_WINE_BIN}"
    exit 1
fi

# Initialize INCLUDE/LIB using msvc-wine native environment script.
# shellcheck disable=SC1090
BIN="${MSVC_WINE_BIN}" . "${MSVCENV_SCRIPT}"
export PATH="${MSVC_WINE_BIN}:${PATH}"

create_vulkan_stub() {
    mkdir -p "${VULKAN_STUB_DIR}"

    # Auto-generate a complete vulkan-1.def from the repo's Vulkan headers.
    # The FFX backend links Vulkan functions directly, so we need every symbol.
    local VK_HEADER_DIR="${SCRIPT_DIR}/third_party/vulkan"
    {
        echo "LIBRARY vulkan-1.dll"
        echo "EXPORTS"
        grep -hroP 'VKAPI_ATTR\s+\w+\s+VKAPI_CALL\s+\K(vk\w+)' "${VK_HEADER_DIR}/" \
            | sort -u \
            | while read -r fn; do echo "  ${fn}"; done
    } > "${VULKAN_STUB_DIR}/vulkan-1.def"

    local num_exports
    num_exports=$(grep -c '^  vk' "${VULKAN_STUB_DIR}/vulkan-1.def" || true)
    echo "[vulkan-stub] Generated def with ${num_exports} exports"

    if command -v llvm-dlltool >/dev/null 2>&1; then
        llvm-dlltool -m i386:x86-64 -D vulkan-1.dll -d "${VULKAN_STUB_DIR}/vulkan-1.def" -l "${VULKAN_STUB_DIR}/vulkan-1.lib"
        return 0
    fi

    if command -v dlltool >/dev/null 2>&1; then
        dlltool -m i386:x86-64 -D vulkan-1.dll -d "${VULKAN_STUB_DIR}/vulkan-1.def" -l "${VULKAN_STUB_DIR}/vulkan-1.lib"
        return 0
    fi

    echo "[warn] Neither llvm-dlltool nor dlltool found; Vulkan stub library will not be generated."
    return 1
}

echo "=== SuperResolution Windows cross-build (Linux -> Windows, msvc-wine clang+lld, no Docker) ==="
echo "  MSVC_WINE_ROOT=${MSVC_WINE_ROOT}"
echo "  SR_FSR=${SR_FSR}  SR_XESS=${SR_XESS}  SR_DLSS=${SR_DLSS}"
echo "  Build types: ${BUILD_TYPES}"
echo ""

VULKAN_ARGS=("-DVulkan_INCLUDE_DIR=${VULKAN_INCLUDE_DIR}")
if create_vulkan_stub; then
    VULKAN_ARGS+=("-DVulkan_LIBRARY=${VULKAN_STUB_DIR}/vulkan-1.lib")
fi

for BUILD_TYPE in ${BUILD_TYPES}; do
    echo "=== Building ${BUILD_TYPE} ==="

    rm -rf buildWindowsOnLinux
    cmake -G Ninja -S . -B buildWindowsOnLinux \
        -DCMAKE_BUILD_TYPE="${BUILD_TYPE}" \
        -DCMAKE_SYSTEM_NAME=Windows \
        -DCMAKE_C_COMPILER=clang-cl \
        -DCMAKE_CXX_COMPILER=clang-cl \
        -DCMAKE_LINKER=lld-link \
        -DSR_FSR="${SR_FSR}" \
        -DSR_XESS="${SR_XESS}" \
        -DSR_DLSS="${SR_DLSS}" \
        -DSR_FSROGL="${SR_FSROGL}" \
        "${VULKAN_ARGS[@]}"

    cmake --build buildWindowsOnLinux --config "${BUILD_TYPE}" -- -j"$(nproc)"
    echo "[${BUILD_TYPE}] Done."
    echo ""
done

echo "=== All builds complete ==="
echo "Output DLLs:"
find "${SCRIPT_DIR}/output" -name '*.dll' 2>/dev/null | sort || echo "(none found)"
