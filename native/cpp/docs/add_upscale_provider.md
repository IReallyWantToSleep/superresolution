# 如何添加一个新的超分辨率提供器

本文档将指导你如何向SR模组添加一个新的超分辨率提供器。

Note: 除非你要添加的算法需要与C++本机库交互（截止0.8.2-alpha.1，SR模组的Vulkan后端仍不完善，若该算法没有OpenGL实现，仍需要编写C++代码），否则通常只需在Java端实现新的`AbstractAlgorithm`并注册到`AlgorithmRegistry`中即可，无需修改本机库。



## 1. 核心

SR模组通过一个统一的 `SRAPI` 接口进行抽象。每个超分辨率算法（如 FSR, XeSS）都是一个独立的“提供器”（Provider）。SR模组通过加载这些提供器模块（动态链接库），并调用其标准化的函数来实现超分辨率功能。

Note: `SRAPI`接口设计时完全不考虑D3D，所有提供器均假设运行在OpenGL或Vulkan环境中。 ~~*你问为什么就是懒*~~

### 关键接口和结构体 (`sr_api.h`)

- `SRUpscaleProvider`: 代表一个超分辨率提供器，包含一个唯一的 `providerId` 和一个 `callbacks` 结构体。
- `SRUpscaleContextCallbacks`: 一个函数指针结构体，包含了提供器需要实现的五个功能：
    - `pCreate`: 创建上下文。
    - `pInit`: 初始化上下文。
    - `pDestroy`: 销毁上下文。
    - `pQuery`: 查询上下文的信息（如版本号，是否兼容当前硬件等）。
    - `pDispatchUpscale`: 执行超分辨率。
- `SRUpscaleContext`: 超分辨率的上下文，包含了提供器的回调函数、创建参数和私有数据 (`userContext`)。
- `SRUpscaleProviderSupplierFunc`: 一个函数原型，用于从模块中获取提供器列表。
- `SRUpscaleProviderSupplierCountFunc`: 一个函数原型，用于获取模块中提供器的数量。

Note: `pCreate`函数中请勿执行需要特定硬件的初始化操作，这些操作应放在 `pInit` 函数中完成。另外，`pCreate`函数调用完毕后模组会用`pQuery`来查询是否支持该算法。

## 2. 添加新的提供器

假设要添加一个名为 `MySuperScaler` 的新提供器。

### 步骤 1: 创建模块目录和文件结构

1.  在 `cpp` 目录下创建一个新的模块目录，例如 `SRNativeMySuperScaler`。
2.  在该目录下创建以下结构：

    ```
    SRNativeMySuperScaler/
    ├── CMakeLists.txt
    ├── include/
    │   └── sr/
    │       └── mysuperscaler/
    │           ├── mysuperscaler.h      // 内部实现头文件
    │           └── sr_provider.h        // 导出提供器的头文件
    └── src/
        ├── mysuperscaler.cpp            // 核心功能实现
        └── sr_provider.cpp              // 导出提供器
    ```

### 步骤 2: 定义新的提供器 ID

在 `SRNativeMain/include/sr/sr_modules.h` 文件中为你的提供器添加一个唯一的ID。

```cpp
// sr_modules.h
#define SR_MODULES_FSR2_ID 0x8000002
#define SR_MODULES_XeSS_ID 0x8000004
// ... 其他ID ...

#define SR_MODULES_MYSUPERSCALER_ID 0x8000006 // 新增ID
```

### 步骤 3: 实现核心回调函数

在 `mysuperscaler.cpp` 中，你需要实现 `SRUpscaleContextCallbacks` 中定义的五个函数。

#### a. `mysuperscaler.h` (内部头文件)

```cpp
// SRNativeMySuperScaler/include/sr/mysuperscaler/mysuperscaler.h
#pragma once
#include "sr/sr_api.h"

#ifdef __cplusplus
extern "C" {
#endif

// 声明五个核心函数的实现
SR_API SRReturnCode srMySuperScalerCreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc);
SR_API SRReturnCode srMySuperScalerInitUpscaleContext(SRUpscaleContext *context);
SR_API SRReturnCode srMySuperScalerDestroyUpscaleContext(SRUpscaleContext *context);
SR_API SRReturnCode srMySuperScalerQueryUpscale(SRUpscaleContextQueryResult *result, SRUpscaleContext *context, SRUpscaleContextQueryType queryType);
SR_API SRReturnCode srMySuperScalerDispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc);

// 声明一个函数用于获取这五个函数指针的集合
SR_API SRUpscaleContextCallbacks srGetMySuperScalerUpscaleCallbacks();

#ifdef __cplusplus
}
#endif
```

#### b. `mysuperscaler.cpp` (核心实现)

```cpp
// SRNativeMySuperScaler/src/mysuperscaler.cpp
#include "sr/mysuperscaler/mysuperscaler.h"
#include <cstdlib>

// 定义一个私有数据结构来存储该提供器特有的状态和对象
struct SRMySuperScalerPrivateData {
    // 例如: 算法的上下文句柄、内部状态等
    void* myScalerContext;
    SRMessageCallback messageCallback;
};

SR_API SRReturnCode srMySuperScalerCreateUpscaleContext(SRUpscaleContext *context, const SRCreateUpscaleContextDesc *desc) {
    // 1. 分配私有数据结构
    SRMySuperScalerPrivateData* privateData = new SRMySuperScalerPrivateData();
    
    // 2. 调用算法的上下文创建函数...

    // 3. 保存创建参数和私有数据到 SRUpscaleContext
    context->desc = *desc;
    privateData->messageCallback = desc->messageCallback;
    context->userContext = privateData;

    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srMySuperScalerInitUpscaleContext(SRUpscaleContext *context) {
    // 初始化上下文
    // ...
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srMySuperScalerDestroyUpscaleContext(SRUpscaleContext *context) {
    SRMySuperScalerPrivateData* privateData = (SRMySuperScalerPrivateData*)context->userContext;
    // 释放资源
    delete privateData;
    context->userContext = nullptr;
    // ...
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srMySuperScalerQueryUpscale(SRUpscaleContextQueryResult *result, SRUpscaleContext *context, SRUpscaleContextQueryType queryType) {
    //处理查询类型...
    return SR_RETURN_CODE_OK;
}

SR_API SRReturnCode srMySuperScalerDispatchUpscale(SRUpscaleContext *context, const SRDispatchUpscaleDesc *desc) {
    // 调用执行函数...
    return SR_RETURN_CODE_OK;
}

// 将实现的函数打包成 SRUpscaleContextCallbacks 结构体
SR_API SRUpscaleContextCallbacks srGetMySuperScalerUpscaleCallbacks() {
    static SRUpscaleContextCallbacks callbacks = {
        .pCreate = srMySuperScalerCreateUpscaleContext,
        .pInit = srMySuperScalerInitUpscaleContext,
        .pDestroy = srMySuperScalerDestroyUpscaleContext,
        .pQuery = srMySuperScalerQueryUpscale,
        .pDispatchUpscale = srMySuperScalerDispatchUpscale,
    };
    return callbacks;
}
```

### 步骤 4: 导出提供器

现在，创建 `sr_provider.h` 和 `sr_provider.cpp` 来导出 `SRUpscaleProvider`。这是主程序动态加载模块时需要查找的入口点。

#### a. `sr_provider.h`

```cpp
// SRNativeMySuperScaler/include/sr/mysuperscaler/sr_provider.h
#pragma once
#include "sr/sr_api.h"
#include "sr/sr_modules.h"
#include "mysuperscaler.h"

extern "C" {
    // 声明导出函数
    SR_API SRReturnCode srGetMySuperScalerUpscaleProviders(SRUpscaleProvider *outProvider);
    SR_API SRReturnCode srGetMySuperScalerUpscaleProvidersCount(uint32_t *outCount);
}
```

#### b. `sr_provider.cpp`

```cpp
// SRNativeMySuperScaler/src/sr_provider.cpp
#include "sr/mysuperscaler/sr_provider.h"

// 定义一个静态的 Provider 数组
static SRUpscaleProvider g_providers[1];
static bool g_initialized = false;

static void ensureInitialized() {
    if (!g_initialized) {
        g_providers[0].providerId = SR_MODULES_MYSUPERSCALER_ID;
        g_providers[0].callbacks = srGetMySuperScalerUpscaleCallbacks();
        g_initialized = true;
    }
}

extern "C" {
    // 实现导出函数，SRAPI将通过 dlsym/GetProcAddress 调用它们
    SR_API SRReturnCode srGetMySuperScalerUpscaleProviders(SRUpscaleProvider *outProvider) {
        ensureInitialized();
        outProvider[0] = g_providers[0];
        return SR_RETURN_CODE_OK;
    }

    SR_API SRReturnCode srGetMySuperScalerUpscaleProvidersCount(uint32_t *outCount) {
        ensureInitialized();
        *outCount = 1;
        return SR_RETURN_CODE_OK;
    }
}
```

### 步骤 5: 配置 CMake

为新模块 `SRNativeMySuperScaler` 创建 `CMakeLists.txt`。可以参考 `SRNativeXeSS/CMakeLists.txt`。

```cmake
# SRNativeMySuperScaler/CMakeLists.txt
cmake_minimum_required(VERSION 3.15)
project(SuperResolutionNativeMySuperScaler)

set(CMAKE_CXX_STANDARD 20)

# 添加头文件目录
include_directories(
    ${PROJECT_SOURCE_DIR}/include
    ${PROJECT_SOURCE_DIR}/../SRNativeMain/include
    # 添加其它头文件目录
    include 
)

# 添加源文件
aux_source_directory(${PROJECT_SOURCE_DIR}/src ALL_SRC)

# 创建共享库
add_library(SR_MYSUPERSCALER_LIB SHARED ${ALL_SRC})

# 设置输出名称
set_target_properties(SR_MYSUPERSCALER_LIB PROPERTIES OUTPUT_NAME "libSuperResolutionMySuperScaler+${LIB_PLATFORM}+${SR_BUILD_TYPE}")

# 链接依赖库
target_link_libraries(SR_MYSUPERSCALER_LIB
    SR_MAIN_LIB
    # 链接其它的库
)
```

最后，在根目录的 `CMakeLists.txt` 中添加你的新模块：

```cmake
# /CMakeLists.txt
if(SR_DLSS)
    add_subdirectory("SRNativeDLSS")
endif()

# ...
if(SR_AAAA)
add_subdirectory(SRNativeMySuperScaler) # 添加新模块
endif()
# ...
``` 

完成以上步骤后，我们已经将一个新的超分辨率提供器集成到了SR模组的本机库中，最后只需要在Java端实现AbstractAlgorithm并注册到AlgorithmRegistry中（可参考io.homo.superresolution.common.upscale.xess.XeSS），你就成功为SR模组增加了一个新的算法。
