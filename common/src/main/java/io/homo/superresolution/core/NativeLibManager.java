package io.homo.superresolution.core;

import io.homo.superresolution.common.platform.Arch;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.core.graphics.glslang.GlslangCompileShaderResult;
import io.homo.superresolution.core.graphics.glslang.GlslangShaderCompiler;
import io.homo.superresolution.core.graphics.glslang.enums.*;
import io.homo.superresolution.core.utils.Md5CaculateUtil;
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
    private final static ArrayList<NativeLib> libs = new ArrayList<>();
    private static boolean nativeApiAvailable;
    public static final NativeLib LIB_SUPER_RESOLUTION;
    public static final NativeLib LIB_SUPER_RESOLUTION_FSR;

    static {
        OS os = new OS();
        if (os.type == OSType.WINDOWS && os.arch == Arch.X86_64) {
            LIB_SUPER_RESOLUTION = new NativeLib("libSuperResolution+win64+debug", "", 1);
            LIB_SUPER_RESOLUTION_FSR = new NativeLib("libSuperResolutionFSR+win64+debug", "", 0);

            libs.add(new NativeLib("SPIRV-Tools-sharedd", "", 1));
            libs.add(LIB_SUPER_RESOLUTION);
            libs.add(LIB_SUPER_RESOLUTION_FSR);
        } else if (os.type == OSType.ANDROID && os.arch == Arch.AARCH64) {
            LIB_SUPER_RESOLUTION = new NativeLib("libSuperResolution+android", "", 1);
            LIB_SUPER_RESOLUTION_FSR = null;

            libs.add(LIB_SUPER_RESOLUTION);
        } else if (os.type == OSType.LINUX && os.arch == Arch.X86_64) {
            LIB_SUPER_RESOLUTION = new NativeLib("libSuperResolution+linux64+release", "", 1);
            LIB_SUPER_RESOLUTION_FSR = new NativeLib("libSuperResolutionFSR+linux64+release", "", 0);

            libs.add(LIB_SUPER_RESOLUTION);
            libs.add(LIB_SUPER_RESOLUTION_FSR);
        } else if (os.type == OSType.MACOS && os.arch == Arch.AARCH64) {
            LIB_SUPER_RESOLUTION = new NativeLib("libSuperResolution+macarm64", "", 1);
            LIB_SUPER_RESOLUTION_FSR = null;

            libs.add(LIB_SUPER_RESOLUTION);
        } else {
            LIB_SUPER_RESOLUTION = null;
            LIB_SUPER_RESOLUTION_FSR = null;
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
            LOGGER.error("依赖库提取失败;信息: {}", error != null ? error : "无");
            throw new RuntimeException("依赖库提取失败");
        } else {
            LOGGER.info("依赖库文件已提取到 {}", path);
        }
    }

    public static boolean check(String path) {
        /*
        boolean status = true;
        for (NativeLib lib : libs) {
            if (!existsLib(path, lib) || !checkLibMd5(path, lib)) {
                status = false;
            }
        }*/
        /*
        懒得更新MD5了，每次进游戏解压一遍得了
        * */
        return false;
    }

    private static boolean checkLibMd5(String path, NativeLib lib) {
        if (Platform.currentPlatform.isDevelopmentEnvironment()) return false;
        return lib.md5.equals(Md5CaculateUtil.getMD5(Paths.get(path, lib.name).toFile()));
    }

    private static boolean existsLib(String path, NativeLib lib) {
        File f = Paths.get(path, lib.name).toFile();
        return f.exists() && f.isFile() && f.canRead() && f.canExecute();
    }

    public static void load(String path) {
        for (NativeLib lib : libs) {
            File f = Paths.get(path, lib.name).toFile();
            if (lib.type == 1) {
                LOGGER.info("加载依赖库： {}", f.getAbsolutePath());
                System.load(f.getAbsolutePath());
            }
        }
        nativeApiAvailable = true;
        LOGGER.info("依赖库版本： {}", SuperResolutionNative.getVersionInfo());
    }

    private static boolean _writeFile(InputStream in, String path) throws IOException {
        if (in == null) return false;

        Path filePath = Path.of(path);

        try {
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean extractLibrary(String path, NativeLib library) throws IOException {
        Path sourcePath = Paths.get(BASE_PATH, library.name);
        Path targetPath = Paths.get(path, library.name);
        try (InputStream in = NativeLibManager.class.getClassLoader().getResourceAsStream(sourcePath.toFile().toString().replace("\\", "/"))) {
            if (in == null) {
                return false;
            }
            if (_writeFile(in, targetPath.toString())) {
                LOGGER.info("{} 提取成功", library.name);
            } else {
                throw new IOException(library.name + " 提取失败");
            }
        } catch (IOException e) {
            LOGGER.error("{} 提取失败;信息: {}", library.name, e.toString());
            throw e;
        }
        return true;
    }

    public static void main(String[] args) {
        if (!NativeLibManager.check("runs/temp")) {
            LOGGER.info("正在提取依赖库");
            NativeLibManager.extract("runs/temp");
        }
        NativeLibManager.load("runs/temp");
        SuperResolutionNative.initGlslang();
        System.out.println(SuperResolutionNative.getVersionInfo());
        GlslangCompileShaderResult result = GlslangShaderCompiler.compileShaderToSpirv(
                "",
                EShLanguage.EShLangVertex,
                EShSource.EShSourceGlsl,
                EShClient.EShClientOpenGL,
                EShTargetClientVersion.EShTargetOpenGL_450,
                EShTargetLanguage.EShTargetSpv,
                EShTargetLanguageVersion.EShTargetSpv_1_4,
                460,
                EProfile.ENoProfile,
                true,
                false
        );
        System.out.println(result);
        SuperResolutionNative.destroyGlslang();
    }

    public static class NativeLib {
        public String name;
        public String md5;
        public int type = 0;

        public NativeLib(String name, String md5, int type) {
            this.name = formatLibName(name);
            this.md5 = md5;
            this.type = type;
        }

        public NativeLib(String name, String md5) {
            this.name = formatLibName(name);
            this.md5 = md5;
        }

        protected static String formatLibName(String name) {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                return name + ".dll";
            }
            if (os.contains("linux")) {
                return name + ".so";
            }
            return name + ".dylib";
        }

        public Path getTargetPath(Path root) {
            return Path.of(root.toString(), this.name);
        }
    }
}
