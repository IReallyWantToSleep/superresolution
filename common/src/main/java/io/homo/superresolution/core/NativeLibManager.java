package io.homo.superresolution.core;

import io.homo.superresolution.common.platform.Arch;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.OSType;
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
    public static NativeLib LIB_SUPER_RESOLUTION_FSR = null;
    public static NativeLib LIB_SUPER_RESOLUTION_FSRGL = null;

    static {
        OS os = new OS();

        if (os.type == OSType.WINDOWS && os.arch == Arch.X86_64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true);
            LIB_SUPER_RESOLUTION_FSR = new NativeLib("SuperResolutionFSR", false);
            LIB_SUPER_RESOLUTION_FSRGL = new NativeLib("SuperResolutionFSRGL", false);
            libs.add(new NativeLib("SPIRV-Tools-shared", true));
            libs.add(LIB_SUPER_RESOLUTION);
            libs.add(LIB_SUPER_RESOLUTION_FSR);
            libs.add(LIB_SUPER_RESOLUTION_FSRGL);

        } else if (os.type == OSType.ANDROID && os.arch == Arch.AARCH64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true);
            libs.add(LIB_SUPER_RESOLUTION);

        } else if (os.type == OSType.LINUX && os.arch == Arch.X86_64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true);
            LIB_SUPER_RESOLUTION_FSR = new NativeLib("SuperResolutionFSR", false);
            libs.add(LIB_SUPER_RESOLUTION);
            libs.add(LIB_SUPER_RESOLUTION_FSR);

        } else if (os.type == OSType.MACOS && os.arch == Arch.AARCH64) {
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
                return false;
            }
            if (_writeFile(in, targetPath.toString())) {
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

        public NativeLib(String baseName, boolean loadAtStartup) {
            this.baseName = baseName;
            this.loadAtStartup = loadAtStartup;
            this.fileName = buildFullFileName(baseName);
            this.preExtractPath = Paths.get(BASE_PATH, this.fileName);
        }

        private static String buildFullFileName(String baseName) {
            OS os = new OS();
            StringBuilder sb = new StringBuilder();

            sb.append("lib");
            sb.append(baseName);


            if (os.type == OSType.WINDOWS) sb.append("+win64");
            else if (os.type == OSType.LINUX) sb.append("+linux64");
            else if (os.type == OSType.MACOS) sb.append("+macarm64");
            else if (os.type == OSType.ANDROID) sb.append("+android");


            if (USE_DEBUG_LIB) {
                sb.append("+debug");
            } else {
                sb.append("+release");
            }


            if (os.type == OSType.WINDOWS) sb.append(".dll");
            else if (os.type == OSType.LINUX || os.type == OSType.ANDROID) sb.append(".so");
            else if (os.type == OSType.MACOS) sb.append(".dylib");

            return sb.toString();
        }

        public Path getTargetPath(Path root) {
            this.extractedPath = root.resolve(fileName);
            return this.extractedPath;
        }
    }
}
