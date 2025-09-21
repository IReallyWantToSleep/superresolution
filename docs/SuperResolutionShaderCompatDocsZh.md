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

| 键名           | 说明          | 示例     |
|--------------|-------------|--------|
| `sr.enabled` | 是否启用超分辨率总开关 | `true` |

### 维度配置

| 键名                                          | 说明                                                                                                          | 示例           |
|---------------------------------------------|-------------------------------------------------------------------------------------------------------------|--------------|
| `enabled`                                   | 是否启用该维度的超分辨率功能                                                                                              | `true`       |
| `upscale_config.before_upscale_shader_name` | 指定执行在哪个非 `compute` 类型的`composite`阶段的 shader 之后触发超分                                                          | `composite2` |
| `upscale_config.sr_internal_texture_format` | 指定超分内部的纹理格式，会影响算法中间纹理的格式与临时输入输出纹理的格式，默认`r11b11g10f`，接受 `rgba8`,`rgba16f`,`rgba16`,`r11b11g10f`,`r11b11g10f` | `r11b11g10f` |

### 输入纹理配置（input\_textures）

| 键名        | 说明                                                                               | 示例            |
|-----------|----------------------------------------------------------------------------------|---------------|
| `enabled` | 是否启用该输入纹理                                                                        | `true`        |
| `src`     | 纹理源，支持：`colortexX`、`alttexX`、`depthtex`、`noHandDepthtex`、`noTranslucentDepthtex` | `colortex0`   |
| `region`  | 输入区域数组 `[X, Y, W, H]`，负值含义如下：<br> `-1` 表示缩放后的全图<br> `-2` 表示原始尺寸全图                | `[0,0,-1,-1]` |

### 输出纹理配置（output\_textures）

| 键名        | 说明                                 | 示例            |
|-----------|------------------------------------|---------------|
| `enabled` | 是否启用该输出纹理                          | `true`        |
| `target`  | 输出目标纹理源数组，支持：`colortexX`、`alttexX` | `colortex0`   |
| `region`  | 输出区域数组 `[X, Y, W, H]`，负值含义同输入区域    | `[0,0,-2,-2]` |

注：SR支持把超分结果写入到多个纹理，具体实现参考[GlTextureCopier类](../common/src/main/java/io/homo/superresolution/core/graphics/opengl/utils/GlTextureCopier.java)

---

## 示例配置（JSON）

```json
{
    "sr": {
        "enabled": true,
        "worlds": {
            "*": {
                "enabled": true,
                "upscale_config": {
                    "before_upscale_shader_name": "composite2",
                    "input_textures": {
                        "color": {
                            "enabled": true,
                            "src": "colortex0",
                            "region": [
                                0,
                                0,
                                -1,
                                -1
                            ]
                        },
                        "depth": {
                            "enabled": true,
                            "src": "depthtex",
                            "region": [
                                0,
                                0,
                                -1,
                                -1
                            ]
                        },
                        "motion_vectors": {
                            "enabled": false,
                            "src": "colortex14",
                            "region": [
                                0,
                                0,
                                -1,
                                -1
                            ]
                        }
                    },
                    "output_textures": {
                        "upscaled_color": {
                            "enabled": true,
                            "target": [
                                "colortex0"
                            ],
                            "region": [
                                0,
                                0,
                                -2,
                                -2
                            ]
                        }
                    }
                }
            },
            "-1": {
                "enabled": true,
                "upscale_config": {
                    "before_upscale_shader_name": "composite2",
                    "input_textures": {
                        "color": {
                            "enabled": true,
                            "src": "colortex0",
                            "region": [
                                0,
                                0,
                                -1,
                                -1
                            ]
                        },
                        "depth": {
                            "enabled": true,
                            "src": "depthtex",
                            "region": [
                                0,
                                0,
                                -1,
                                -1
                            ]
                        },
                        "motion_vectors": {
                            "enabled": false,
                            "src": "none",
                            "region": [
                                0,
                                0,
                                -1,
                                -1
                            ]
                        }
                    },
                    "output_textures": {
                        "upscaled_color": {
                            "enabled": true,
                            "target": [
                                "colortex0",
                                "colortex5"
                            ],
                            "region": [
                                0,
                                0,
                                -2,
                                -2
                            ]
                        }
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

| 名称                   | 说明                                 |
|----------------------|------------------------------------|
| `SR_INSTALLED`       | 代表安装了SR模组                          |
| `SR_ENABLE`          | 当启用超分时为1否则为0                       |
| `SR_DISABLE`         | 当禁用超分时为1否则为0                       |
| `SR_ALGO_*`          | 各个算法的标识                            |
| `SR_SUPPORTS_JITTER` | 当当前算法支持抖动时值为1否则为0                  |
| `SR_USING_ALGO`      | 当前使用的超分算法，值为`SR_ALGO_*`或0，当禁用超分时为0 |

注：SR在未安装任何扩展模组时会添加这些算法标识：

* SR_ALGO_FSR2
* SR_ALGO_FSR1
* SR_ALGO_SGSR1
* SR_ALGO_SGSR2
* SR_ALGO_NONE

### Uniforms

| 名称                        | 类型       | 说明                                                                           |
|---------------------------|----------|------------------------------------------------------------------------------|
| `SRRatio`                 | float    | 超分比例（渲染倍率的倒数）                                                                |
| `SRRenderScale`           | float    | 渲染倍率                                                                         |
| `SRRenderScaleLog2`       | float    | 渲染倍率的对数（底数为2）                                                                |
| `SRScaledViewportSize`    | vector2f | 经过缩放的视图大小                                                                    |
| `SROriginalViewportSize`  | vector2f | 未经缩放的视图大小                                                                    |
| `SRScaledViewportSizeI`   | vector2i | 经过缩放的视图大小(整数)                                                                |
| `SROriginalViewportSizeI` | vector2i | 未经缩放的视图大小(整数)                                                                |
| `SRJitterOffset`          | vector2f | 像素空间抖动偏移量(相对于SRScaledViewportSize；范围[-1,1]；当SR_SUPPORTS_JITTER为0时该值始终为[0,0]) |

示例代码

```glsl
#ifdef SR_INSTALLED
    uniform float                           SRRatio; 
    uniform float                           SRRenderScale; 
    uniform float                           SRRenderScaleLog2; 
    uniform vec2                            SRScaledViewportSize; 
    uniform vec2                            SROriginalViewportSize; 
    uniform ivec2                           SRScaledViewportSizeI; 
    uniform ivec2                           SROriginalViewportSizeI; 
    uniform vec2                            SRJitterOffset;

    #define MC_RENDER_SCALE                 (SRRenderScale)            //渲染倍率
    #define MC_RENDER_RATIO                 (SRRatio)                  //等于1/SRRenderScale
    #define MC_RENDER_SCALE_LOG2            (SRRenderScaleLog2)        //等于log2(SRRenderScale)
    #define MC_SCALED_VIEWPORT_SIZE         (SRScaledViewportSize)     //等于vec2(SROriginalViewportSize) * SRRenderScale
    #define MC_ORIGINAL_VIEWPORT_SIZE       (SROriginalViewportSize)   //未经缩放的视图大小
    #define MC_SCALED_VIEWPORT_SIZEI        (SRScaledViewportSizeI)    //等于ivec2(vec2(SROriginalViewportSizeI) * SRRenderScale)
    #define MC_ORIGINAL_VIEWPORT_SIZEI      (SROriginalViewportSizeI)  //未经缩放的视图大小，整数
    #if SR_SUPPORTS_JITTER
        #define MC_JITTER_OFFSET            (SRJitterOffset)
    #else
        #define MC_JITTER_OFFSET            (vec2(0.0))
    #endif
#else
    #define MC_RENDER_SCALE                 (1.0)
    #define MC_RENDER_RATIO                 (1.0)
    #define MC_RENDER_SCALE_LOG2            (0.0)
    #define MC_SCALED_VIEWPORT_SIZE         (vec2(viewWidth,viewHeight))
    #define MC_ORIGINAL_VIEWPORT_SIZE       (vec2(viewWidth,viewHeight))
    #define MC_SCALED_VIEWPORT_SIZEI        (ivec2(viewWidth,viewHeight))
    #define MC_ORIGINAL_VIEWPORT_SIZEI      (ivec2(viewWidth,viewHeight))
    #define MC_JITTER_OFFSET                (vec2(0.0))
#endif

```

---

## 注意事项

* SR配置读取无容错，配置错误会导致SR跳过配置加载，回退普通模式，错误信息见游戏日志。
* SR检测到光影包存在兼容配置文件并且无错误
  时，不会更改游戏帧缓冲区大小，此时需要光影手动变换视图，可以参考[Photon Shader的实现](https://github.com/sixthsurge/photon/blob/iris-stable/shaders/program/gbuffers_all_solid.vsh#L152-L157)。
* 当SR_SUPPORTS_JITTER为1时shader可以直接使用SR提供的抖动，无论SR_SUPPORTS_JITTER为什么，超分的输出始终已去抖动。
* SR模组暂不计划支持OptiFine，若你的光影计划支持OptiFine，那么SR的配置文件不会有其它副作用。

---

## 参考链接

* 经过修改的[Photon Shader](https://github.com/187J3X1-114514/photon-sr)，支持SR模组。