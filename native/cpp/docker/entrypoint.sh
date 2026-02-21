#!/bin/bash
set -euo pipefail
# entrypoint.sh — Docker 容器入口
# 初始化 msvc-wine 环境 + 生成 Vulkan stub，然后执行用户命令

MSVC_WINE_BIN="${MSVC_WINE_BIN:-/opt/my_msvc/bin/x64}"
MSVCENV_SCRIPT="${MSVCENV_SCRIPT:-/opt/my_msvc/msvcenv-native.sh}"

# 1) 初始化 MSVC INCLUDE/LIB 环境变量
if [ -f "${MSVCENV_SCRIPT}" ]; then
    # shellcheck disable=SC1090
    BIN="${MSVC_WINE_BIN}" . "${MSVCENV_SCRIPT}"
    export PATH="${MSVC_WINE_BIN}:${PATH}"
else
    echo "[error] msvcenv-native.sh not found: ${MSVCENV_SCRIPT}"
    exit 1
fi

# 2) 生成 Vulkan stub (.lib) — 需要挂载的源码中的 third_party/vulkan 头文件
VULKAN_STUB_DIR="/tmp/vulkan-stub"
if [ -d "/src/third_party/vulkan" ]; then
    /opt/gen_vulkan_stub.sh "/src/third_party" "${VULKAN_STUB_DIR}"
else
    echo "[warn] /src/third_party/vulkan not found, skipping Vulkan stub generation"
fi

# 3) 执行用户传入的命令
exec "$@"
