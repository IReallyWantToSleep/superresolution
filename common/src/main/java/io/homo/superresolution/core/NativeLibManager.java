/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import io.homo.superresolution.api.platform.SystemArchitecture;
import io.homo.superresolution.api.platform.OperatingSystem;
import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.core.utils.MessageBox;
import org.lwjgl.system.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class NativeLibManager {

    public static final String BASE_PATH = "lib";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-NativeLib");


    public static final boolean USE_DEBUG_LIB = true;

    private static final List<NativeLib> libs = new ArrayList<>();
    private static boolean nativeApiAvailable;


    public static NativeLib LIB_SUPER_RESOLUTION = null;
    public static NativeLib LIB_NANOVG = null;
    public static NativeLib LIB_SUPER_RESOLUTION_FSR = null;
    public static NativeLib LIB_SUPER_RESOLUTION_XESS = null;
    public static NativeLib LIB_SUPER_RESOLUTION_FSRGL = null;

    public interface ExtendedKernel32 extends com.sun.jna.platform.win32.Kernel32 {
        class DllDirectoryCookie extends PointerType {
            public DllDirectoryCookie() {
                super();
            }

            public DllDirectoryCookie(Pointer p) {
                super(p);
            }
        }

        DllDirectoryCookie AddDllDirectory(String lpPathName);

    }

    private static ExtendedKernel32 kernel32Instance;

    static {
        OperatingSystem operatingSystem = new OperatingSystem();

        if (operatingSystem.type == OperatingSystemType.WINDOWS && operatingSystem.arch == SystemArchitecture.X86_64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true);
            LIB_SUPER_RESOLUTION_FSR = new NativeLib("SuperResolutionFSR", false);
            LIB_SUPER_RESOLUTION_XESS = new NativeLib("SuperResolutionXeSS", false);
            LIB_NANOVG = new NativeLib(
                    "lwjgl_nanovg",
                    false,
                    true,
                    Configuration.LIBRARY_PATH.get() == null ?
                            null :
                            Path.of(Configuration.LIBRARY_PATH.get())
            );
            libs.add(LIB_NANOVG);
            libs.add(LIB_SUPER_RESOLUTION);
            //libs.add(LIB_SUPER_RESOLUTION_FSR);
            //libs.add(LIB_SUPER_RESOLUTION_XESS);

        } else if (operatingSystem.type == OperatingSystemType.ANDROID && operatingSystem.arch == SystemArchitecture.AARCH64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true);
            libs.add(LIB_SUPER_RESOLUTION);

        } else if (operatingSystem.type == OperatingSystemType.LINUX && operatingSystem.arch == SystemArchitecture.X86_64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true);
            LIB_SUPER_RESOLUTION_FSR = new NativeLib("SuperResolutionFSR", false);
            LIB_NANOVG = new NativeLib(
                    "liblwjgl_nanovg",
                    false,
                    true,
                    Configuration.LIBRARY_PATH.get() == null ?
                            null :
                            Path.of(Configuration.LIBRARY_PATH.get())
            );
            libs.add(LIB_NANOVG);
            libs.add(LIB_SUPER_RESOLUTION);
            libs.add(LIB_SUPER_RESOLUTION_FSR);

        } else if (operatingSystem.type == OperatingSystemType.MACOS && operatingSystem.arch == SystemArchitecture.AARCH64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true);
            libs.add(LIB_SUPER_RESOLUTION);
        }
    }

    public static boolean nativeApiAvailable() {
        return nativeApiAvailable;
    }

    public static void extract(String path) {
        LOGGER.info("开始提取依赖库文件");
        boolean status = true;
        String error = null;
        try {
            for (NativeLib lib : libs) {
                if (!extractLibrary(path, lib)) {
                    status = false;
                }
            }
        } catch (Exception e) {
            status = false;
            error = e.toString();
            e.printStackTrace();
        }
        if (!status) {
            LOGGER.error("依赖库提取失败; 信息: {}", error != null ? error : "无");
            MessageBox.createError(
                    "SuperResolution在提取必要依赖库时失败，错误原因：%s".formatted(error),
                    "Error"
            );
            throw new RuntimeException("依赖库提取失败");
        } else {
            LOGGER.info("依赖库文件已提取到 {}", path);
        }
    }

    public static void load(String path) {
        for (NativeLib lib : libs) {
            File f = lib.getTargetPath(Path.of(path)).toFile();
            if (lib.loadAtStartup) {
                LOGGER.info("加载依赖库： {}", f.getAbsolutePath());
                System.load(f.getAbsolutePath());
            }
        }
        nativeApiAvailable = true;
    }

    private static boolean _writeFile(InputStream in, String path) throws IOException {
        if (in == null) return false;
        Path filePath = Path.of(path);
        Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private static boolean extractLibrary(String path, NativeLib library) throws IOException {
        Path sourcePath = Paths.get(BASE_PATH, library.fileName);
        Path targetPath = library.getTargetPath(Path.of(path));

        try (InputStream in = NativeLibManager.class.getClassLoader()
                .getResourceAsStream(sourcePath.toString().replace("\\", "/"))) {
            if (in == null) {
                LOGGER.error("{} 提取失败", sourcePath);
                return false;
            }
            if (_writeFile(in, targetPath.toString())) {
                library.extractedPath = targetPath;
                LOGGER.info("{} 提取成功", library.fileName);
            } else {
                throw new IOException(library.fileName + " 提取失败");
            }
        } catch (IOException e) {
            LOGGER.error("{} 提取失败; 信息: {}", library.fileName, e.toString());
            throw e;
        }
        return true;
    }

    public static class NativeLib {
        public final String baseName;
        public final String fileName;
        public final boolean loadAtStartup;
        public final Path preExtractPath;
        public Path extractedPath;
        public boolean available;
        public boolean nameIsPath;
        public Path targetPath;

        public NativeLib(String baseName, boolean loadAtStartup) {
            this(baseName, loadAtStartup, false);
        }

        public NativeLib(String baseName, boolean loadAtStartup, boolean nameIsPath) {
            this(baseName, loadAtStartup, false, null);
        }

        public NativeLib(String baseName, boolean loadAtStartup, boolean nameIsPath, Path targetPath) {
            this.baseName = baseName;
            this.loadAtStartup = loadAtStartup;
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

                if (operatingSystem.type == OperatingSystemType.WINDOWS) sb.append("+win64");
                else if (operatingSystem.type == OperatingSystemType.LINUX) sb.append("+linux64");
                else if (operatingSystem.type == OperatingSystemType.MACOS) sb.append("+macarm64");
                else if (operatingSystem.type == OperatingSystemType.ANDROID) sb.append("+android");

                if (USE_DEBUG_LIB) {
                    sb.append("+debug");
                } else {
                    sb.append("+release");
                }
            } else {
                sb.append(baseName);
            }

            if (operatingSystem.type == OperatingSystemType.WINDOWS) sb.append(".dll");
            else if (operatingSystem.type == OperatingSystemType.LINUX || operatingSystem.type == OperatingSystemType.ANDROID)
                sb.append(".so");
            else if (operatingSystem.type == OperatingSystemType.MACOS) sb.append(".dylib");

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
