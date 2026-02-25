/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core;

import io.homo.superresolution.api.platform.OperatingSystem;
import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.SystemArchitecture;
import io.homo.superresolution.core.utils.MessageBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class NativeLibManager {
    public static final String BASE_PATH = "lib";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-NativeLib");

    public static final boolean USE_DEBUG_LIB = true;

    private static final List<NativeLib> libs = new ArrayList<>();
    public static NativeLib LIB_SUPER_RESOLUTION = null;
    public static NativeLib LIB_SUPER_RESOLUTION_FSR = null;
    public static NativeLib LIB_SUPER_RESOLUTION_XESS = null;
    public static NativeLib LIB_SUPER_RESOLUTION_DLSS = null;
    public static NativeLib LIB_SUPER_RESOLUTION_FSRGL = null;
    private static boolean nativeApiAvailable;

    static {
        OperatingSystem operatingSystem = new OperatingSystem();
        if (operatingSystem.type == OperatingSystemType.WINDOWS && operatingSystem.arch == SystemArchitecture.X86_64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true, true);
            LIB_SUPER_RESOLUTION_FSR = new NativeLib("SuperResolutionFSR", false, false);
            LIB_SUPER_RESOLUTION_XESS = new NativeLib("SuperResolutionXeSS", false, false);
            LIB_SUPER_RESOLUTION_DLSS = new NativeLib("SuperResolutionDLSS", false, false);
            libs.add(LIB_SUPER_RESOLUTION);
            libs.add(LIB_SUPER_RESOLUTION_FSR);
            libs.add(LIB_SUPER_RESOLUTION_XESS);
            libs.add(LIB_SUPER_RESOLUTION_DLSS);

        } else if (operatingSystem.type == OperatingSystemType.ANDROID && operatingSystem.arch == SystemArchitecture.AARCH64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true, true);
            libs.add(LIB_SUPER_RESOLUTION);

        } else if (operatingSystem.type == OperatingSystemType.LINUX && operatingSystem.arch == SystemArchitecture.X86_64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true, true);
            LIB_SUPER_RESOLUTION_FSR = new NativeLib("SuperResolutionFSR", false, false);
            LIB_SUPER_RESOLUTION_DLSS = new NativeLib("SuperResolutionDLSS", false, false);
            libs.add(LIB_SUPER_RESOLUTION);
            libs.add(LIB_SUPER_RESOLUTION_FSR);
            libs.add(LIB_SUPER_RESOLUTION_DLSS);

        } else if (operatingSystem.type == OperatingSystemType.MACOS && operatingSystem.arch == SystemArchitecture.AARCH64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true, true);
            libs.add(LIB_SUPER_RESOLUTION);
        }
    }

    public static boolean nativeApiAvailable() {
        return nativeApiAvailable;
    }

    public static void createLibraryDir(Path path) {
        File dir = path.toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.error("无法创建目录: {}", dir);
        }
    }

    public static void extract(Path path) {
        LOGGER.info("开始提取依赖库文件");
        createLibraryDir(path);
        List<String> requiredFailures = new ArrayList<>();
        List<String> optionalFailures = new ArrayList<>();

        for (NativeLib lib : libs) {
            try {
                if (!extractLibrary(path, lib)) {
                    if (lib.required) {
                        requiredFailures.add(lib.fileName);
                        LOGGER.error("必要依赖库 {} 提取失败", lib.fileName);
                    } else {
                        optionalFailures.add(lib.fileName);
                        LOGGER.warn("可选依赖库 {} 提取失败，已跳过", lib.fileName);
                    }
                }
            } catch (Exception e) {
                if (lib.required) {
                    requiredFailures.add(lib.fileName);
                    LOGGER.error("必要依赖库 {} 提取失败: {}", lib.fileName, e.getMessage());
                    e.printStackTrace();
                } else {
                    optionalFailures.add(lib.fileName);
                    LOGGER.warn("可选依赖库 {} 提取失败，已跳过: {}", lib.fileName, e.getMessage());
                }
            }
        }

        if (!requiredFailures.isEmpty()) {
            String errorMsg = String.join(", ", requiredFailures);
            LOGGER.error("必要依赖库提取失败: {}", errorMsg);
            MessageBox.createError(
                    "SuperResolution在提取必要依赖库时失败，失败的库：%s".formatted(errorMsg),
                    "Error"
            );
            throw new RuntimeException("必要依赖库提取失败: " + errorMsg);
        }

        if (!optionalFailures.isEmpty()) {
            LOGGER.info("已跳过以下可选依赖库: {}", String.join(", ", optionalFailures));
        }

        LOGGER.info("依赖库文件已提取到 {}", path);
    }

    public static void load(Path path) {
        createLibraryDir(path);
        for (NativeLib lib : libs) {
            if (lib.extractedPath == null) {
                if (lib.required) {
                    LOGGER.error("必要依赖库 {} 未提取，无法加载", lib.fileName);
                    throw new RuntimeException("必要依赖库 " + lib.fileName + " 未提取");
                } else {
                    LOGGER.warn("可选依赖库 {} 未提取，已跳过加载", lib.fileName);
                    continue;
                }
            }

            File f = lib.getTargetPath(path).toFile();
            if (lib.loadAtStartup) {
                try {
                    LOGGER.info("加载依赖库： {}", f.getAbsolutePath());
                    System.load(f.getAbsolutePath());
                    lib.available = true;
                } catch (Exception e) {
                    if (lib.required) {
                        LOGGER.error("必要依赖库 {} 加载失败: {}", lib.fileName, e.getMessage());
                        throw new RuntimeException("必要依赖库加载失败: " + lib.fileName, e);
                    } else {
                        LOGGER.warn("可选依赖库 {} 加载失败，已跳过: {}", lib.fileName, e.getMessage());
                        lib.available = false;
                    }
                }
            }
        }
        nativeApiAvailable = true;
    }

    private static boolean _writeFile(InputStream in, String path) throws IOException {
        if (in == null) {
            return false;
        }
        Path filePath = Path.of(path);
        Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private static boolean extractLibrary(Path path, NativeLib library) throws IOException {
        Path sourcePath = Paths.get(BASE_PATH, library.fileName);
        Path targetPath = library.getTargetPath(path);

        try (
                InputStream in = NativeLibManager.class.getClassLoader()
                        .getResourceAsStream(sourcePath.toString().replace("\\", "/"))
        ) {
            if (in == null) {
                if (library.required) {
                    LOGGER.error("必要依赖库 {} 提取失败：资源未找到", sourcePath);
                } else {
                    LOGGER.warn("可选依赖库 {} 提取失败：资源未找到", sourcePath);
                }
                return false;
            }
            if (_writeFile(in, targetPath.toString())) {
                library.extractedPath = targetPath;
                LOGGER.info("{} 提取成功", library.fileName);
                return true;
            } else {
                if (library.required) {
                    throw new IOException("必要依赖库 " + library.fileName + " 提取失败");
                } else {
                    LOGGER.warn("可选依赖库 {} 提取失败", library.fileName);
                    return false;
                }
            }
        } catch (IOException e) {
            if (library.required) {
                LOGGER.error("必要依赖库 {} 提取失败; 信息: {}", library.fileName, e.toString());
                throw e;
            } else {
                LOGGER.warn("可选依赖库 {} 提取失败; 信息: {}", library.fileName, e.toString());
                return false;
            }
        }
    }

    public static class NativeLib {
        public final String baseName;
        public final String fileName;
        public final boolean loadAtStartup;
        public final boolean required;
        public final Path preExtractPath;
        public Path extractedPath;
        public boolean available;
        public boolean nameIsPath;
        public Path targetPath;

        public NativeLib(String baseName, boolean loadAtStartup, boolean required) {
            this(baseName, loadAtStartup, required, false);
        }

        public NativeLib(String baseName, boolean loadAtStartup, boolean required, boolean nameIsPath) {
            this(baseName, loadAtStartup, required, nameIsPath, null);
        }

        public NativeLib(String baseName, boolean loadAtStartup, boolean required, boolean nameIsPath, Path targetPath) {
            this.baseName = baseName;
            this.loadAtStartup = loadAtStartup;
            this.required = required;
            this.fileName = buildFullFileName(baseName, nameIsPath);
            this.preExtractPath = Paths.get(BASE_PATH, this.fileName);
            this.nameIsPath = nameIsPath;
            this.targetPath = targetPath;
        }

        private static String buildFullFileName(String baseName, boolean nameIsPath) {
            OperatingSystem operatingSystem = new OperatingSystem();
            StringBuilder sb = new StringBuilder();
            if (!nameIsPath) {
                sb.append("lib");
                sb.append(baseName);

                if (operatingSystem.type == OperatingSystemType.WINDOWS) {
                    sb.append("+win64");
                } else if (operatingSystem.type == OperatingSystemType.LINUX) {
                    sb.append("+linux64");
                } else if (operatingSystem.type == OperatingSystemType.MACOS) {
                    sb.append("+macarm64");
                } else if (operatingSystem.type == OperatingSystemType.ANDROID) {
                    sb.append("+android");
                }

                if (USE_DEBUG_LIB) {
                    sb.append("+debug");
                } else {
                    sb.append("+release");
                }
            } else {
                sb.append(baseName);
            }

            if (operatingSystem.type == OperatingSystemType.WINDOWS) {
                sb.append(".dll");
            } else if (operatingSystem.type == OperatingSystemType.LINUX || operatingSystem.type == OperatingSystemType.ANDROID) {
                sb.append(".so");
            } else if (operatingSystem.type == OperatingSystemType.MACOS) {
                sb.append(".dylib");
            }

            return sb.toString();
        }

        public Path getTargetPath(Path root) {
            if (targetPath != null) {
                this.extractedPath = targetPath.resolve(fileName);
            } else {
                this.extractedPath = root.resolve(fileName);
            }
            return this.extractedPath;
        }
    }
}