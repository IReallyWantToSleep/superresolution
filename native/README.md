# Super Resolution Native

# 构建

* 使用`-DSRLIB_VERSION=0.0.0`指定版本
* 使用`llvm-strip --strip-all (LIBPATH)` 可减小最终库文件大小

## Windows

注意：

* 仅在Clang版本18.1.8，Windows11上测试过
* 只可使用Clang作为编译器
* [使用的LLVM工具链](https://github.com/llvm/llvm-project/releases/tag/llvmorg-18.1.8)

```shell
cmake . -B build -G "Ninja" -DCMAKE_BUILD_TYPE=Release
cmake --build build
```

## Android

### 注意：

* 仅在NDK版本28.0.12433566(r28b1)，Windows11上测试过
* ANDROID_ABI必须为arm64-v8a
* 在Linux上编译时你可能需要更改CMAKE_MAKE_PROGRAM

```shell
cmake -B build -G "Unix Makefiles" -DCMAKE_INSTALL_PREFIX="install" -DANDROID_ABI=arm64-v8a -DCMAKE_BUILD_TYPE=Release -DANDROID_STL=c++_static -DANDROID_PLATFORM=android-24 -DCMAKE_SYSTEM_NAME=Android -DANDROID_TOOLCHAIN=clang -DANDROID_ARM_MODE=arm -DCMAKE_MAKE_PROGRAM="$env:ANDROID_NDK_HOME\prebuilt\windows-x86_64\bin\make.exe" ` -DCMAKE_TOOLCHAIN_FILE="$env:ANDROID_NDK_HOME\build\cmake\android.toolchain.cmake"
cmake --build build
```

## Linux

注意：

* 仅在Clang版本14.0.0，Windows11+WSL2上测试过
* 只可使用Clang作为编译器
* 

```shell
cmake . -B build -G "Ninja" -DCMAKE_BUILD_TYPE=Release
cmake --build build
```

# 许可证

这部分代码遵循LGPLv3协议