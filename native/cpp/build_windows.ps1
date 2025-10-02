rm -r -Force build
cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug -DSR_FSR=ON -DSR_XESS=ON -DSR_FSROGL=OFF -DSR_DLSS=OFF
cmake --build build --config Debug

rm -r -Force build
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release -DSR_FSR=ON -DSR_XESS=ON -DSR_FSROGL=OFF -DSR_DLSS=OFF
cmake --build build --config Release
