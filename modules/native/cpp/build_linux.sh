#!/usr/bin/env bash
set -euo pipefail

export CC=clang
export CXX=clang++

echo "[native] using C compiler: ${CC}"
echo "[native] using CXX compiler: ${CXX}"

NPROC=$(nproc)
if [ "${NPROC}" -gt 12 ]; then
  JOBS=$((NPROC - 2))
else
  JOBS=${NPROC}
fi
echo "[native] building with ${JOBS} jobs (nproc=${NPROC})"

# Shader-compile concurrency budget.
#
# ffx_sc runs one glslangValidator subprocess per worker thread, and Ninja runs several
# ffx_sc invocations at once, so the two multiply. Keep one thread per invocation by
# default and let Ninja's -j be the single throttle; if FFX_SC_THREADS is raised, shrink
# the pool so processes x threads still lands on the same JOBS budget.
FFX_SC_THREADS="${FFX_SC_THREADS:-1}"
if [ "${FFX_SC_THREADS}" -le 0 ]; then
  # 0 means "let ffx_sc use hardware_concurrency", so only one may run at a time.
  FFX_SC_POOL=1
else
  FFX_SC_POOL=$(( JOBS / FFX_SC_THREADS ))
  if [ "${FFX_SC_POOL}" -lt 1 ]; then
    FFX_SC_POOL=1
  fi
fi
echo "[native] shader compile: ${FFX_SC_POOL} process(es) x ${FFX_SC_THREADS} thread(s)"

# Prebuild FidelityFX_SC once into the SDK binary store, mirroring the prebuilt
# FidelityFX_SC.exe that Windows ships. Inside the main configure it would be a serial
# barrier -- no shader permutation can start until it links -- and since the build
# directory is wiped below it would be rebuilt on every run. The store lives in the
# source tree, so it survives.
FFX_SC_STORE_DIR="SRNativeFSR/SDK/tools/binary_store"
FFX_SC_BIN="${FFX_SC_STORE_DIR}/FidelityFX_SC"
FFX_SC_SRC_DIR="SRNativeFSR/SDK/tools/ffx_shader_compiler"

if [ ! -x "${FFX_SC_BIN}" ] && [ -f "${FFX_SC_SRC_DIR}/CMakeLists.txt" ]; then
  echo "[native] building FidelityFX_SC once -> ${FFX_SC_BIN}"
  rm -rf buildLinuxFfxSc
  cmake -G "Ninja" -S "${FFX_SC_SRC_DIR}" -B buildLinuxFfxSc \
    -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER=${CC} -DCMAKE_CXX_COMPILER=${CXX}
  cmake --build buildLinuxFfxSc -- -j"${JOBS}"
  # The target pins RUNTIME_OUTPUT_DIRECTORY to <source>/bin, so search both that and
  # the build tree rather than assuming either.
  FFX_SC_BUILT="$(find "${FFX_SC_SRC_DIR}/bin" buildLinuxFfxSc -name FidelityFX_SC -type f 2>/dev/null | head -n1 || true)"
  if [ -z "${FFX_SC_BUILT}" ]; then
    echo "[native] ERROR: FidelityFX_SC was built but could not be located" >&2
    exit 1
  fi
  mkdir -p "${FFX_SC_STORE_DIR}"
  install -m 0755 "${FFX_SC_BUILT}" "${FFX_SC_BIN}"
  rm -rf buildLinuxFfxSc
fi

if [ -x "${FFX_SC_BIN}" ]; then
  echo "[native] using prebuilt FidelityFX_SC: ${FFX_SC_BIN}"
fi

CMAKE_COMMON_ARGS=(
  -G "Ninja" -S . -B buildLinux
  -DCMAKE_C_COMPILER=${CC} -DCMAKE_CXX_COMPILER=${CXX}
  -DSR_FSR=ON -DSR_XESS=OFF -DSR_NGX=ON
  -DFFX_SC_NUM_THREADS="${FFX_SC_THREADS}"
  -DFFX_SC_JOB_POOL_SIZE="${FFX_SC_POOL}"
)

rm -rf buildLinux
cmake "${CMAKE_COMMON_ARGS[@]}" -DCMAKE_BUILD_TYPE=Debug
cmake --build buildLinux --config Debug -- -j${JOBS}

rm -rf buildLinux
cmake "${CMAKE_COMMON_ARGS[@]}" -DCMAKE_BUILD_TYPE=Release
cmake --build buildLinux --config Release -- -j${JOBS}
