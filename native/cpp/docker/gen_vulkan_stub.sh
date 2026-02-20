#!/bin/bash
set -euo pipefail
# gen_vulkan_stub.sh — 从仓库的 Vulkan 头文件生成 vulkan-1.lib stub
#
# 用法: gen_vulkan_stub.sh <vulkan_header_dir> <output_dir>
#   vulkan_header_dir: 包含 vulkan/*.h 的目录 (例如 third_party)
#   output_dir:        输出 vulkan-1.def + vulkan-1.lib 的目录

VK_HEADER_DIR="${1:?用法: $0 <vulkan_header_dir> <output_dir>}"
OUTPUT_DIR="${2:?用法: $0 <vulkan_header_dir> <output_dir>}"

if [ ! -d "${VK_HEADER_DIR}/vulkan" ]; then
    echo "[error] 未找到 ${VK_HEADER_DIR}/vulkan/ 目录"
    exit 1
fi

mkdir -p "${OUTPUT_DIR}"

{
    echo "LIBRARY vulkan-1.dll"
    echo "EXPORTS"
    grep -hroP 'VKAPI_ATTR\s+\w+\s+VKAPI_CALL\s+\K(vk\w+)' "${VK_HEADER_DIR}/vulkan/" \
        | sort -u \
        | while read -r fn; do echo "  ${fn}"; done
} > "${OUTPUT_DIR}/vulkan-1.def"

NUM_EXPORTS=$(grep -c '^  vk' "${OUTPUT_DIR}/vulkan-1.def" || true)
echo "[vulkan-stub] Generated def with ${NUM_EXPORTS} exports"

if command -v llvm-dlltool >/dev/null 2>&1; then
    llvm-dlltool -m i386:x86-64 -D vulkan-1.dll \
        -d "${OUTPUT_DIR}/vulkan-1.def" \
        -l "${OUTPUT_DIR}/vulkan-1.lib"
    echo "[vulkan-stub] Created ${OUTPUT_DIR}/vulkan-1.lib"
elif command -v dlltool >/dev/null 2>&1; then
    dlltool -m i386:x86-64 -D vulkan-1.dll \
        -d "${OUTPUT_DIR}/vulkan-1.def" \
        -l "${OUTPUT_DIR}/vulkan-1.lib"
    echo "[vulkan-stub] Created ${OUTPUT_DIR}/vulkan-1.lib"
else
    echo "[error] 找不到 llvm-dlltool 或 dlltool"
    exit 1
fi
