# 0.7.1-alpha.3

* Fixed UI misalignment issue in versions 1.21.1+
* Added event: AlgorithmDispatchFinishEvent
* Added event: LevelRenderStartEvent
* Added event: LevelRenderEndEvent

# 0.7.1-alpha.2

* New option: PostChain Injection Blacklist
* Add logs to record injected PostChains

# 0.7.1-alpha.1

* The mod now **requires** the `GL_ARB_direct_state_access` OpenGL extension
* Migrated most GL calls from legacy to Direct State Access (DSA) approach, potentially improving performance
* Optimized FSR2 performance
* Fixed crashes on AMD GPUs
* Fixed issues/55
* Fixed issues/34
* Fixed issues/60
* Fixed issues/56

# 0.7.0-alpha.3

* Optimized shader compilation and caching
* Fixed issues/57 (FSR2 exposure issues)

# 0.7.0-alpha.2

* Fixed cache directory creation logic
* Fixed issues/58

# 0.7.0-alpha.1

* The mod now **requires** the `GL_ARB_gl_spirv` OpenGL extension
* Ported FSR2 backend to Java (essentially rewritten)
* Replaced custom shader compiler with Glslang
* Removed `AbstractAlgorithm.blitToScreen` method
* Added config option: Pause game when opening config screen
* Removed config option: Skip loading dependencies
* Added `Linux x64` and `Android armv8` builds for C++ dependencies
* Renamed some classes
* Refactored shader-related classes
* Refactored GlPipeline
* Fixed issues/28

# 0.6.2-alpha.1

* Updated developer API
* Improved configuration system
* Added Vulkan information display
* Removed Classical Chinese (華夏) translation
* FSR2 currently limited to development environments
* Fixed bugs with some options not applying
* Added experimental motion vector generation
* Fixed issues/43

# 0.6.1-alpha.1

* Enhanced developer API
* Added missing license files
* Improved internationalization
* More prominent text colors in info screen
* Automatic algorithm availability checks
* Optimized UI panorama rendering
* Fixed long text rendering in expandable info screen lists
* Fixed issues/42/#41/#40/#39
* Fixed config loss after restart on Forge
* Removed UI blur effects
* Now saves/restores most OpenGL states
* Removed SodiumOptionsAPI dependency

# 0.6.0-alpha.1

Warning: Incomplete internationalization

* New developer API
* ~~Added 1.21.5 support~~ _(temporarily abandoned)_
* Added SGSR1
* Added RenderDoc & ImGui debug options (dev-only)
* Added GL object labels for debugging
* Added 3-channel compute shader variant for SGSR2
* Added blur effects to config screen
* Added panorama rendering to config screen
* Switched framebuffer implementation
* Better Sodium Options API compatibility
* Optimized OpenGL state preservation
* Improved Vulkan/OpenGL info retrieval
* Enhanced config screen text formatting
* Removed redundant code

# 0.5.0-alpha.2

Warning: Incomplete internationalization

* New icon (Thanks [yu](https://center.mcmod.cn/773788/) for the artwork)
* Changed contributor list colors
* Added shader caching _(disabled by default due to performance impact)_
* Added options to skip Vulkan init and dependency loading
* Adjusted config screen scroll speed
* Modified ModelView matrix capture method
* Changed version format _(0.0.5a1 → 0.5.0-alpha.1)_
* Optimized Vulkan compatibility detection
* Improved algorithm failure handling
* Enhanced screen size update logic
* Fixed issues/30
* Fixed issues/29

# 0.0.5a1

Warning: Incomplete internationalization

* Improved null handling
* Experimental SGSR support
* Experimental mobile device support
* Enhanced info screen aesthetics ~~_(by interrupting animations)_~~
* Added more content to info screen
* Improved Vulkan compatibility detection
* Fixed screen size detection errors
* Fixed issues/22
* Fixed issues/24

# 0.0.4a3

* Fixed crash when config file is missing

# 0.0.4a2

* Added 1.20.4 Fabric support
* Added 1.21.4 Fabric/NeoForge support
* Rewrote framebuffer code
* Changed config screen scrollbar style
* Optimized "Apply" button logic
* Fixed rendering issues with Immersive Engineering
* Fixed mixin conflicts with Savage Ender Culling
* Fixed issues/19

# 0.0.4a1

* Rewrote screen capture system
* Rebuilt configuration system
* Redesigned config screen
* Fixed issues/14
* Optimized FSR1 performance
* Significantly reduced FSR1 codebase
* Added capture mode options

# 0.0.3a4

* Restructured project
* Added information display screen
* Modified 1.21.1 requirements (now works on 1.21)
* Improved Vulkan initialization (no crash on failure)
* Removed VK_KHR_format_feature_flags2 requirement (better hardware compatibility)
* Fixed algorithm support misreporting
* Fixed compatibility issues with Fabric TACZ
* Fixed issues/7
* Fixed rendering position after resource pack reload
* Added Classical Chinese (華夏) translation