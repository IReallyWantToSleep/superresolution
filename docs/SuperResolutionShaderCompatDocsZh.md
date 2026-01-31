# 光影包与SR模组兼容指 ~~北~~ 南

1. 在与 `shader.properties` 同级的目录创建 `superresolution.json`

2. 在该文件中声明各个维度超分辨率的执行位置与输入输出资源，参考下文。

---

## 配置结构说明

### 顶层结构

```json
{
    "sr": {
        "enabled": true,
        "worlds": {
            "<维度标识>": {
                "enabled": true,
                "upscale_config": {
                    "before_upscale_shader_name": "<shader名称>",
                    "sr_internal_texture_format": "r11b11g10f",
                    "input_textures": {
                        "<纹理名称>": {
                            "enabled": true/false,
                            "src": "<纹理源>",
                            "region": [
                                X,
                                Y,
                                W,
                                H
                            ]
                        },
                        ...
                    },
                    "output_textures": {
                        "<输出纹理名称>": {
                            "enabled": true/false,
                            "target": [
                                "<目标纹理源>",
                                "<目标纹理源>"
                            ],
                            "region": [
                                X,
                                Y,
                                W,
                                H
                            ]
                        }
                    }
                }
            }
        }
    }
}
```

### 维度标识说明

* 使用字符串表示维度，比如 `"*"` 表示所有维度通用配置，`"1"` 表示末地，`"-1"` 表示下界，`"0"` 表示主世界。
* 允许使用通配符 `*`，以实现默认配置。

### 纹理名称说明

| 名称             | 说明                                                                                                                     | 
|----------------|------------------------------------------------------------------------------------------------------------------------| 
| color          | 输入颜色纹理                                                                                                                 | 
| depth          | 输入深度纹理                                                                                                                 | 
| motion_vectors | 输入运动矢量纹理（RG通道，R通道X方向，G通道Y方向，格式详见[FSR2文档](https://github.com/GPUOpen-Effects/FidelityFX-FSR2#providing-motion-vectors)） | 

---

## 参数说明

### 全局

| 键名               | 说明              | 示例 |
|------------------|-----------------|-----|
| `schema_version` | 配置文件的 schema 版本 | `1` |

### 维度配置（profiles）

每个 profile 配置项包含以下字段：

#### 抖动配置（jitter）

| 键名        | 说明             | 示例     |
|-----------|----------------|--------|
| `enabled` | 是否启用该维度的抖动功能 | `true` |

#### 超分配置（upscale）

| 键名                | 说明                                                                                                            | 示例           |
|-------------------|---------------------------------------------------------------------------------------------------------------|--------------|
| `enabled`         | 是否启用该维度的超分辨率功能                                                                                                | `true`       |
| `internal_format` | 指定超分内部的纹理格式，会影响算法中间纹理的格式与临时输入输出纹理的格式，默认`r11g11b10f`，支持 `rgb8`,`rgba8`,`rgba16f`,`rgba16`,`rgb16f`,`r11g11b10f` | `r11g11b10f` |

#### 触发器配置（trigger）

| 键名     | 说明                                                                              | 示例           |
|--------|---------------------------------------------------------------------------------|--------------|
| `type` | 触发时机，`BEFORE` 表示在指定 pass 之前触发，`AFTER` 表示在指定 pass 之后触发（不区分大小写）                | `AFTER`      |
| `pass` | 指定在哪个非 `compute` 类型的 `composite` 阶段 shader 触发超分，如 `composite1`, `composite2` 等 | `composite1` |

### 输入纹理配置（inputs）

配置中的 `inputs` 字段是一个对象，键名为纹理类型（如 `color`、`depth`、`motion_vectors`），值为纹理配置对象：

| 键名        | 说明                                                                               | 示例            |
|-----------|----------------------------------------------------------------------------------|---------------|
| `enabled` | 是否启用该输入纹理                                                                        | `true`        |
| `src`     | 纹理源，支持：`colortexX`、`alttexX`、`depthtex`、`noHandDepthtex`、`noTranslucentDepthtex` | `colortex0`   |
| `region`  | 输入区域数组 `[X, Y, W, H]`，负值含义如下：<br> `-1` 表示缩放后的全图<br> `-2` 表示原始尺寸全图                | `[0,0,-1,-1]` |

### 输出纹理配置（outputs）

配置中的 `outputs` 字段是一个对象，键名为输出纹理类型（如 `upscaled_color`），值为纹理配置对象：

| 键名        | 说明                                  | 示例                        |
|-----------|-------------------------------------|---------------------------|
| `enabled` | 是否启用该输出纹理                           | `true`                    |
| `target`  | 输出目标纹理源数组，支持：`colortexX`、`alttexX` | `["colortex0", "alttex0"]` |
| `region`  | 输出区域数组 `[X, Y, W, H]`，负值含义同输入区域     | `[0,0,-2,-2]`             |

注：SR支持把超分结果写入到多个纹理，具体实现参考[GlTextureCopier类](../common/src/main/java/io/homo/superresolution/core/graphics/opengl/utils/GlTextureCopier.java)

---

## 示例配置（JSON）

```json
{
    "schema_version": 1,
    "profiles": {
        "*": {
            "jitter": {
                "enabled": true
            },
            "upscale": {
                "enabled": true,
                "internal_format": "r11g11b10f",
                "trigger": {
                    "type": "AFTER",
                    "pass": "composite1"
                },
                "inputs": {
                    "color": {
                        "enabled": true,
                        "src": "colortex0",
                        "region": [0, 0, -1, -1]
                    },
                    "depth": {
                        "enabled": true,
                        "src": "depthtex",
                        "region": [0, 0, -1, -1]
                    },
                    "motion_vectors": {
                        "enabled": true,
                        "src": "colortex16",
                        "region": [0, 0, -1, -1]
                    }
                },
                "outputs": {
                    "upscaled_color": {
                        "enabled": true,
                        "target": ["colortex0", "alttex0"],
                        "region": [0, 0, -2, -2]
                    }
                }
            }
        },
        "0": {
            "jitter": {
                "enabled": true
            },
            "upscale": {
                "enabled": true,
                "internal_format": "rgba16f",
                "trigger": {
                    "type": "AFTER",
                    "pass": "composite2"
                },
                "inputs": {
                    "color": {
                        "enabled": true,
                        "src": "colortex0",
                        "region": [0, 0, -1, -1]
                    },
                    "depth": {
                        "enabled": true,
                        "src": "depthtex",
                        "region": [0, 0, -1, -1]
                    }
                },
                "outputs": {
                    "upscaled_color": {
                        "enabled": true,
                        "target": ["colortex0"],
                        "region": [0, 0, -2, -2]
                    }
                }
            }
        }
    }
}
```

---

## 着色器内的 SR 支持

SR自动注入如下宏和uniform：

### Define

| 名称                         | 说明                                                   |
|----------------------------|------------------------------------------------------|
| `SR_INSTALLED`             | 始终为 `1`，表示安装了SR模组                                    |
| `SR_ENABLE`                | 当启用超分时为 `1`，否则为 `0`                                  |
| `SR_DISABLE`               | 当禁用超分时为 `1`，否则为 `0`                                  |
| `SR_ALGO_<CODENAME>`       | 各个算法的唯一标识符（整数值），如 `SR_ALGO_FSR2`、`SR_ALGO_DLSS` 等 |
| `SR_ALGO_SUPPORTS_JITTER`  | 当前算法支持抖动时为 `1`，否则为 `0`                               |
| `SR_USING_ALGO`            | 当前使用的超分算法ID（对应某个 `SR_ALGO_*` 的值），禁用超分时为 `0`        |
| `SR_SHOULD_APPLY_SCALE`    | 当启用超分时为 `1`，否则为 `0`，用于判断是否应该应用缩放                   |
| `SR_SHOULD_APPLY_JITTER`   | 当启用超分时为 `1`，否则为 `0`，用于判断是否应该应用抖动                   |

**注**：算法标识宏的具体可用值取决于已安装的SR扩展模组。常见算法包括：
* `SR_ALGO_FSR2` - AMD FidelityFX Super Resolution 2
* `SR_ALGO_FSR1` - AMD FidelityFX Super Resolution 1
* `SR_ALGO_SGSR1` - Scalable GPU Super Resolution 1
* `SR_ALGO_SGSR2` - Scalable GPU Super Resolution 2
* `SR_ALGO_DLSS` - NVIDIA Deep Learning Super Sampling （测试）
* `SR_ALGO_XESS` - Intel Xe Super Sampling （测试）
* `SR_ALGO_NONE` - 无超分算法

### Uniforms

| 名称                        | 类型       | 说明                                                               |
|---------------------------|----------|------------------------------------------------------------------|
| `SRRenderScale`           | float    | 渲染缩放因子（render scale），当禁用超分时为 `1.0`                             |
| `SRRatio`                 | float    | 超分比例（upscale ratio），等于 `1 / SRRenderScale`，当禁用超分时为 `1.0`      |
| `SRRenderScaleLog2`       | float    | 渲染缩放的对数值，计算公式：`log2(renderWidth / screenWidth)`，当禁用超分时为 `0.0` |
| `SRScaledViewportSize`    | vec2     | 缩放后的视口大小（渲染分辨率），单位：像素                                          |
| `SROriginalViewportSize`  | vec2     | 原始视口大小（屏幕分辨率），单位：像素                                            |
| `SRScaledViewportSizeI`   | ivec2    | 缩放后的视口大小（整数）                                                 |
| `SROriginalViewportSizeI` | ivec2    | 原始视口大小（整数）                                                   |
| `SRJitterOffset`          | vec2     | 抖动偏移量（像素空间），当算法不支持抖动时始终为 `vec2(0.0)`                          |


#### 示例代码

```glsl
#ifdef SR_INSTALLED
    uniform float SRRatio; 
    uniform float SRRenderScale; 
    uniform float SRRenderScaleLog2; 
    uniform vec2 SRScaledViewportSize; 
    uniform vec2 SROriginalViewportSize; 
    uniform ivec2 SRScaledViewportSizeI; 
    uniform ivec2 SROriginalViewportSizeI; 
    uniform vec2 SRJitterOffset;

    #define MC_RENDER_SCALE (SRRenderScale)
    #define MC_RENDER_RATIO (SRRatio)
    #define MC_RENDER_SCALE_LOG2 (SRRenderScaleLog2)
    #define MC_SCALED_VIEWPORT_SIZE (SRScaledViewportSize)
    #define MC_ORIGINAL_VIEWPORT_SIZE (SROriginalViewportSize)
    #define MC_SCALED_VIEWPORT_SIZEI (SRScaledViewportSizeI)
    #define MC_ORIGINAL_VIEWPORT_SIZEI (SROriginalViewportSizeI)
    #if (SR_ALGO_SUPPORTS_JITTER == 1) && (SR_SHOULD_APPLY_JITTER == 1)
        #define MC_JITTER_OFFSET (SRJitterOffset)
    #else
        #define MC_JITTER_OFFSET (vec2(0.0))
    #endif
#else
    // SR模组未安装时的回退
    #define MC_RENDER_SCALE (1.0)
    #define MC_RENDER_RATIO (1.0)
    #define MC_RENDER_SCALE_LOG2 (0.0)
    #define MC_SCALED_VIEWPORT_SIZE (vec2(viewWidth, viewHeight))
    #define MC_ORIGINAL_VIEWPORT_SIZE (vec2(viewWidth, viewHeight))
    #define MC_SCALED_VIEWPORT_SIZEI (ivec2(viewWidth, viewHeight))
    #define MC_ORIGINAL_VIEWPORT_SIZEI (ivec2(viewWidth, viewHeight))
    #define MC_JITTER_OFFSET (vec2(0.0))
#endif
```

---

## 注意事项

* SR配置读取无容错，配置错误会导致SR跳过配置加载，回退普通模式，错误信息见游戏日志。
* SR检测到光影包存在兼容配置文件并且无错误时，不会更改游戏帧缓冲区大小，此时需要光影手动变换视图，可以参考[Photon Shader的实现](https://github.com/sixthsurge/photon/blob/iris-stable/shaders/program/gbuffers_all_solid.vsh#L152-L157)。
* 当 `SR_ALGO_SUPPORTS_JITTER` 和 `SR_SHOULD_APPLY_JITTER` 均为 `1` 时，shader可以直接使用SR提供的抖动；无论这些宏值为多少，超分的输出始终已去抖动。
* SR模组暂不计划支持OptiFine，若你的光影计划支持OptiFine，那么SR的配置文件不会有其它副作用。

---

## 参考链接

* 经过修改的[Photon Shader](https://github.com/187J3X1-114514/photon-sr)，支持SR模组。