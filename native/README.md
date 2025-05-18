# Super Resolution Native

# 构建

* 使用`-DSRLIB_VERSION=0.0.0`指定版本

## Windows

注意：

* 仅在GCC版本15.1.0，Windows11上测试过
* 只可使用Mingw作为编译器

```shell
cmake -B build -G "MinGW Makefiles"
cmake --build build
```

## Android

### 注意：

* 仅在NDK版本28.0.12433566(r28b1)，Windows11上测试过
* ANDROID_ABI必须为arm64-v8a

```shell
cmake . -B build -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-21 -DANDROID_NDK=$ANDROID_NDK_HOME -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake -G Ninja
cmake --build build
```

## Linux

注意：

* 仅在GCC版本11.4.0，Windows11+WSL2上测试过
* 只可使用GCC作为编译器

```shell
cmake -B build
cmake --build build
```

# 许可证

这部分代码遵循LGPLv3协议