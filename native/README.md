# Super Resolution Native

# 构建

* 使用`-DSRLIB_VERSION=0.0.0`指定版本
* 使用`llvm-strip --strip-all (LIBPATH)` 可减小最终库文件大小
* 请自行构建[Glslang](https://github.com/KhronosGroup/glslang)
  或者使用预构建的[Glslang](https://github.com/187J3X1-114514/glslang-sr/releases)

## 构建Glslang

注意：

* [提交哈希](cpp/glslang-commit-hash)
* 只可使用GCC作为编译器

请按照Glslang库中的描述构建

## Windows

注意：

* 仅在GCC版本15.1.0，Windows11上测试过
* 只可使用MingwGCC作为编译器

```shell
cmake . -B build -G "Ninja" -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER="gcc" -DCMAKE_CXX_COMPILER="g++"
cmake --build build
```

## Android

### 注意：

* 仅在NDK版本28.0.12433566(r28b1)，Windows11上测试过
* ANDROID_ABI必须为arm64-v8a

```shell
cmake -B build -G "Unix Makefiles" -DCMAKE_INSTALL_PREFIX="install" -DANDROID_ABI=arm64-v8a -DCMAKE_BUILD_TYPE=Debug -DANDROID_STL=c++_static -DANDROID_PLATFORM=android-24 -DCMAKE_SYSTEM_NAME=Android -DANDROID_TOOLCHAIN=clang -DANDROID_ARM_MODE=arm -DCMAKE_MAKE_PROGRAM="$env:ANDROID_NDK_HOME\prebuilt\windows-x86_64\bin\make.exe" ` -DCMAKE_TOOLCHAIN_FILE="$env:ANDROID_NDK_HOME\build\cmake\android.toolchain.cmake"
cmake --build build
```

## Linux

注意：

* 仅在GCC版本11.4.0，Windows11+WSL2上测试过
* 只可使用GCC作为编译器

```shell
cmake . -B build -G "Ninja" -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER="gcc" -DCMAKE_CXX_COMPILER="g++"
cmake --build build
```

# 许可证

这部分代码遵循LGPLv3协议