set -e

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

rm -rf buildLinux
cmake -G "Ninja" -S . -B buildLinux -DCMAKE_BUILD_TYPE=Debug -DCMAKE_C_COMPILER=${CC} -DCMAKE_CXX_COMPILER=${CXX} -DSR_FSR=ON -DSR_XESS=OFF -DSR_FSROGL=OFF -DSR_DLSS=ON
cmake --build buildLinux --config Debug -- -j${JOBS}

rm -rf buildLinux
cmake -G "Ninja" -S . -B buildLinux -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER=${CC} -DCMAKE_CXX_COMPILER=${CXX} -DSR_FSR=ON -DSR_XESS=OFF -DSR_FSROGL=OFF -DSR_DLSS=ON
cmake --build buildLinux --config Release -- -j${JOBS}