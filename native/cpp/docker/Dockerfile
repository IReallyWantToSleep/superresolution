# ============================================================================
# SuperResolution Windows cross-build environment
#
# 预装 clang/lld + msvc-wine，CI / 本地 docker 直接使用此镜像编译。
#
# 构建镜像 (在 native/cpp 目录下):
#   docker build -t sr-cross-win -f docker/Dockerfile .
#
# 使用:
#   docker run --rm -v "$PWD":/src sr-cross-win ./build_windows_on_linux.sh
#   docker run --rm -v "$PWD":/src sr-cross-win ./build_windows_on_linux.sh Debug
#
# 接受 Microsoft License: https://go.microsoft.com/fwlink/?LinkId=2086102
# ============================================================================

FROM debian:bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive

# ---- Step 1: 系统依赖 ----
RUN apt update && apt install -y --no-install-recommends \
        clang-19 \
        lld-19 \
        llvm-19 \
        cmake \
        ninja-build \
        python3 \
        python3-yaml \
        python3-simplejson \
        msitools \
        git \
        ca-certificates \
        wine \
    && rm -rf /var/lib/apt/lists/*

# 某些发行版只提供 clang/lld，不提供 clang-cl/lld-link 命令名
RUN set -eux; \
    if ! command -v clang-cl >/dev/null 2>&1; then \
        ln -sf "$(command -v clang-19)" /usr/bin/clang-cl; \
    fi; \
    if ! command -v lld-link >/dev/null 2>&1; then \
        lld_bin="$(command -v lld-19 || command -v ld.lld)"; \
        ln -sf "${lld_bin}" /usr/bin/lld-link; \
    fi; \
    if ! command -v llvm-dlltool >/dev/null 2>&1; then \
        ln -sf "$(command -v llvm-dlltool-19)" /usr/bin/llvm-dlltool; \
    fi

# ---- Step 2: msvc-wine (clang+lld native mode, 无需 Wine) ----
ENV MSVC_WINE_REPO=/opt/msvc-wine \
    MSVC_WINE_ROOT=/opt/my_msvc

RUN git clone https://hub.locyancs.cn/github.com/mstorsjo/msvc-wine.git "${MSVC_WINE_REPO}" \
    && python3 "${MSVC_WINE_REPO}/vsdownload.py" --accept-license --dest "${MSVC_WINE_REPO}" \
    && bash "${MSVC_WINE_REPO}/install.sh" "${MSVC_WINE_REPO}" \
    && rm -rf /tmp/* /root/.cache

ENV MSVC_WINE_BIN=${MSVC_WINE_REPO}/bin/x64 \
    MSVCENV_SCRIPT=${MSVC_WINE_REPO}/msvcenv-native.sh

# ---- Step 3: Vulkan stub 生成脚本 + ENTRYPOINT ----
COPY docker/gen_vulkan_stub.sh /opt/gen_vulkan_stub.sh
COPY docker/entrypoint.sh      /opt/entrypoint.sh
RUN chmod +x /opt/gen_vulkan_stub.sh /opt/entrypoint.sh

# ---- Step 4: 验证工具链 ----
RUN clang-cl --version \
    && lld-link --version \
    && cmake --version \
    && ninja --version \
    && python3 --version \
    && test -d "${MSVC_WINE_BIN}" \
    && echo "=== Toolchain verified ==="

WORKDIR /src
ENTRYPOINT ["/opt/entrypoint.sh"]
