rm -rf buildLinux
cmake -G "Ninja" -S . -B buildLinux -DCMAKE_BUILD_TYPE=Debug -DSR_FSR=ON -DSR_XESS=OFF -DSR_FSROGL=OFF -DSR_DLSS=ON
cmake --build buildLinux --config Debug -- -j$(nproc)

rm -rf buildLinux
cmake -G "Ninja" -S . -B buildLinux -DCMAKE_BUILD_TYPE=Release -DSR_FSR=ON -DSR_XESS=OFF -DSR_FSROGL=OFF -DSR_DLSS=ON
cmake --build buildLinux --config Release -- -j$(nproc)