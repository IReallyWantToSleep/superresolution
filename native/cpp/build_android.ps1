#查找Android NDK路径
Write-Output "Android NDK: $env:ANDROID_NDK_HOME"
Remove-Item -r -Force build
cmake -S . -B build -G "Unix Makefiles" `
-DANDROID_ABI=arm64-v8a `
-DCMAKE_BUILD_TYPE=Release `
-DANDROID_STL=c++_static `
-DANDROID_PLATFORM=android-24 `
-DCMAKE_SYSTEM_NAME=Android `
-DANDROID_TOOLCHAIN=clang `
-DANDROID_ARM_MODE=arm `
-DCMAKE_MAKE_PROGRAM="$env:ANDROID_NDK_HOME/prebuilt/windows-x86_64/bin/make.exe" `
-DCMAKE_TOOLCHAIN_FILE="$env:ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" `
-DSR_FSR=OFF -DSR_XESS=OFF -DSR_DLSS=OFF