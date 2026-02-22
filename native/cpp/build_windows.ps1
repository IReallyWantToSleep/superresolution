rm -r -Force buildWindows
cmake -S . -B buildWindows -DCMAKE_BUILD_TYPE=Debug -DSR_FSR=ON -DSR_XESS=ON -DSR_FSROGL=OFF -DSR_DLSS=ON
cmake --build buildWindows --config Debug

rm -r -Force buildWindows
cmake -S . -B buildWindows -DCMAKE_BUILD_TYPE=Release -DSR_FSR=ON -DSR_XESS=ON -DSR_FSROGL=OFF -DSR_DLSS=ON
cmake --build buildWindows --config Release
