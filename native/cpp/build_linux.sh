rm -rf buildLinux
cmake -G "Ninja" -S . -B buildLinux -DCMAKE_BUILD_TYPE=Debug -DSR_FSR=ON -DSR_XESS=OFF -DSR_FSROGL=OFF -DSR_DLSS=OFF
cmake --build buildLinux --config Debug -- -j8

rm -rf buildLinux
cmake -G "Ninja" -S . -B buildLinux -DCMAKE_BUILD_TYPE=Release -DSR_FSR=ON -DSR_XESS=OFF -DSR_FSROGL=OFF -DSR_DLSS=OFF
cmake --build buildLinux --config Release -- -j8
