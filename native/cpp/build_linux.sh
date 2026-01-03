rm -rf build
cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug -DSR_FSR=ON -DSR_XESS=OFF -DSR_FSROGL=OFF -DSR_DLSS=OFF
cmake --build build --config Debug -- -j4

rm -rf build
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release -DSR_FSR=ON -DSR_XESS=OFF -DSR_FSROGL=OFF -DSR_DLSS=OFF
cmake --build build --config Release -- -j4
