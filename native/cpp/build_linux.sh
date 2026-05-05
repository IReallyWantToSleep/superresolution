set -e

export CC=clang
export CXX=clang++

echo "[native] using C compiler: ${CC}"
echo "[native] using CXX compiler: ${CXX}"

rm -rf buildLinux
cmake -G "Ninja" -S . -B buildLinux -DCMAKE_BUILD_TYPE=Debug -DCMAKE_C_COMPILER=${CC} -DCMAKE_CXX_COMPILER=${CXX} -DSR_FSR=OFF -DSR_XESS=OFF -DSR_FSROGL=OFF -DSR_DLSS=ON
cmake --build buildLinux --config Debug -- -j$(nproc)

rm -rf buildLinux
cmake -G "Ninja" -S . -B buildLinux -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER=${CC} -DCMAKE_CXX_COMPILER=${CXX} -DSR_FSR=OFF -DSR_XESS=OFF -DSR_FSROGL=OFF -DSR_DLSS=ON
cmake --build buildLinux --config Release -- -j$(nproc)