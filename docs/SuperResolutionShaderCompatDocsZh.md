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

| 名称             | 说明                                                                                                                   | 
|----------------|----------------------------------------------------------------------------------------------------------------------| 
| color          | 输入颜色纹理                                                                                                               | 
| depth          | 输入深度纹理                                                                                                               | 
| motion_vectors | 输入运动矢量纹理（RG通道，R通道X方向，G通道Y方向，详见[FSR2文档](https://github.com/GPUOpen-Effects/FidelityFX-FSR2#providing-motion-vectors)） | 

---

## 参数说明

### 全局

| 键名           | 说明          | 示例     |
|--------------|-------------|--------|
| `sr.enabled` | 是否启用超分辨率总开关 | `true` |

### 维度配置

| 键名                                          | 说明                                                 | 示例           |
|---------------------------------------------|----------------------------------------------------|--------------|
| `enabled`                                   | 是否启用该维度的超分辨率功能                                     | `true`       |
| `upscale_config.before_upscale_shader_name` | 指定执行在哪个非 `compute` 类型的`composite`阶段的 shader 之后触发超分 | `composite2` |

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

```glsl
SRMOD_ENABLED
```

### Uniforms

| 名称                        | 类型       | 说明            |
|---------------------------|----------|---------------|
| `SRRatio`                 | float    | 超分比例（渲染倍率的倒数） |
| `SRRenderScale`           | float    | 渲染倍率          |
| `SRRenderScaleLog2`       | float    | 渲染倍率的对数       |
| `SRScaledViewportSize`    | vector2f | 经过缩放的视图大小     |
| `SROriginalViewportSize`  | vector2f | 未经缩放的视图大小     |
| `SRScaledViewportSizeI`   | vector2i | 经过缩放的视图大小(整数) |
| `SROriginalViewportSizeI` | vector2i | 未经缩放的视图大小(整数) |

示例代码

```glsl
#ifdef SRMOD_ENABLED
    uniform float SRRatio; 
    uniform float SRRenderScale; 
    uniform float SRRenderScaleLog2; 
    uniform vec2 SRScaledViewportSize; 
    uniform vec2 SROriginalViewportSize; 
    uniform vec2 SRScaledViewportSizeI; 
    uniform vec2 SROriginalViewportSizeI; 

    #define SR_RATIO                 (SRRatio)
    #define SR_RENDER_SCALE          (SRRenderScale)
    #define SR_RENDER_SCALE_LOG2     (SRRenderScaleLog2)
    #define SR_SCALED_VIEWPORT_SIZE  (SRScaledViewportSize)
    #define SR_ORIGINAL_VIEWPORT_SIZE (SROriginalViewportSize)
    #define SR_SCALED_VIEWPORT_SIZEI  (SRScaledViewportSizeI)
    #define SR_ORIGINAL_VIEWPORT_SIZEI (SROriginalViewportSizeI)
#else
    #define SR_RATIO                 (1.0)
    #define SR_RENDER_SCALE          (1.0)
    #define SR_RENDER_SCALE_LOG2     (0.0)
    #define SR_SCALED_VIEWPORT_SIZE  (vec2(viewWidth,viewHeight))
    #define SR_ORIGINAL_VIEWPORT_SIZE (vec2(viewWidth,viewHeight))
    #define SR_SCALED_VIEWPORT_SIZE  (ivec2(viewWidth,viewHeight))
    #define SR_ORIGINAL_VIEWPORT_SIZE (ivec2(viewWidth,viewHeight))
#endif

```

---

## 注意事项

* SR配置读取无容错，配置错误会导致SR跳过配置加载，回退普通模式，错误信息见游戏日志。
* SR检测到光影包存在兼容配置文件并且无错误
  时，不会更改游戏帧缓冲区大小，此时需要光影手动变换视图，可以参考[Photon Shader的实现](https://github.com/sixthsurge/photon/blob/iris-stable/shaders/program/gbuffers_all_solid.vsh#L152-L157)。
* SR未来或将支持抖动（Jitter）功能，建议相关代码预留对应支持。预计通过 uniform 向着色器传递抖动数据，并可通过配置文件进行开启和关闭。

---

## 参考链接

* 经过修改的[Photon Shader](https://github.com/187J3X1-114514/photon-sr)，支持SR模组。