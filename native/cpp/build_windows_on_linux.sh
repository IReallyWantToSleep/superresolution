# VULKAN_LIBRARY_WIN64="${VULKAN_LIBRARY_WIN64:-/usr/x86_64-w64-mingw32/lib/libvulkan-1.dll.a}"
# VULKAN_INCLUDE_DIR_WIN64="${VULKAN_INCLUDE_DIR_WIN64:-/usr/x86_64-w64-mingw32/include}"

# if [ ! -f "$VULKAN_LIBRARY_WIN64" ]; then
# 	echo "Missing Vulkan library: $VULKAN_LIBRARY_WIN64"
# 	echo "Set VULKAN_LIBRARY_WIN64 to your Windows Vulkan import library (vulkan-1.lib or libvulkan-1.dll.a)"
# 	exit 1
# fi

# if [ ! -d "$VULKAN_INCLUDE_DIR_WIN64" ]; then
# 	echo "Missing Vulkan include dir: $VULKAN_INCLUDE_DIR_WIN64"
# 	echo "Set VULKAN_INCLUDE_DIR_WIN64 to your Windows Vulkan include directory"
# 	exit 1
# fi

# rm -rf buildWindowsOnLinux
# cmake -G "Ninja" -S . -B buildWindowsOnLinux -DCMAKE_BUILD_TYPE=Debug -DCMAKE_SYSTEM_NAME=Windows -DCMAKE_SYSTEM_PROCESSOR=x86_64 -DCMAKE_C_COMPILER=x86_64-w64-mingw32-gcc -DCMAKE_CXX_COMPILER=x86_64-w64-mingw32-g++ -DCMAKE_RC_COMPILER=x86_64-w64-mingw32-windres -DVulkan_LIBRARY="$VULKAN_LIBRARY_WIN64" -DVulkan_INCLUDE_DIR="$VULKAN_INCLUDE_DIR_WIN64" -DSR_FSR=OFF -DSR_XESS=ON -DSR_FSROGL=OFF -DSR_DLSS=ON
# cmake --build buildWindowsOnLinux --config Debug -- -j$(nproc)

# rm -rf buildWindowsOnLinux
# cmake -G "Ninja" -S . -B buildWindowsOnLinux -DCMAKE_BUILD_TYPE=Release -DCMAKE_SYSTEM_NAME=Windows -DCMAKE_SYSTEM_PROCESSOR=x86_64 -DCMAKE_C_COMPILER=x86_64-w64-mingw32-gcc -DCMAKE_CXX_COMPILER=x86_64-w64-mingw32-g++ -DCMAKE_RC_COMPILER=x86_64-w64-mingw32-windres -DVulkan_LIBRARY="$VULKAN_LIBRARY_WIN64" -DVulkan_INCLUDE_DIR="$VULKAN_INCLUDE_DIR_WIN64" -DSR_FSR=OFF -DSR_XESS=ON -DSR_FSROGL=OFF -DSR_DLSS=ON
# cmake --build buildWindowsOnLinux --config Release -- -j$(nproc)
let Windows build the fucking binaries themselves, I don't want to deal with this.