package io.homo.superresolution.core;

import io.homo.superresolution.common.platform.Arch;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.core.utils.Md5CaculateUtil;
import net.minecraft.SharedConstants;
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


public class NativeLibManager {
    public static final String BASE_PATH = "lib";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-NativeLib");
    private final static ArrayList<NativeLib> libs = new ArrayList<>();
    private static boolean nativeApiAvailable;

    static {
        OS os = new OS();
        if (os.type == OSType.WINDOWS && os.arch == Arch.X86_64) {
            libs.add(new NativeLib("libSuperResolution+win64", "cce9e677e647afbc24a25d92952dba7c", 1));
        }
        if (os.type == OSType.ANDROID && os.arch == Arch.AARCH64) {
            libs.add(new NativeLib("libSuperResolution+android", "d0b33be24e664881e4b66b9dae1f56b1", 1));
        }
        if (os.type == OSType.LINUX && os.arch == Arch.X86_64) {
            libs.add(new NativeLib("libSuperResolution+linux64", "c2c38eede35a0814f9b490979d491b97", 1));
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
        }
        if (!status) {
            LOGGER.error("依赖库提取失败;信息: {}", error != null ? error : "无");
            throw new RuntimeException("依赖库提取失败");
        } else {
            LOGGER.info("依赖库文件已提取到 {}", path);
        }
    }

    public static boolean check(String path) {
        boolean status = true;
        for (NativeLib lib : libs) {
            if (!existsLib(path, lib) || !checkLibMd5(path, lib)) {
                status = false;
            }
        }
        return status;
    }

    private static boolean checkLibMd5(String path, NativeLib lib) {
        if (SharedConstants.IS_RUNNING_IN_IDE) return false;
        return lib.md5.equals(Md5CaculateUtil.getMD5(Paths.get(path, lib.name).toFile()));
    }

    private static boolean existsLib(String path, NativeLib lib) {
        File f = Paths.get(path, lib.name).toFile();
        return f.exists() && f.isFile() && f.canRead() && f.canExecute();
    }

    public static void load(String path) {
        for (NativeLib lib : libs) {
            File f = Paths.get(path, lib.name).toFile();
            LOGGER.info("加载依赖库： {}", f.getAbsolutePath());
            System.load(f.getAbsolutePath());
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
        if (!NativeLibManager.check("I:\\superresolution\\fabric\\run")) {
            LOGGER.info("正在提取依赖库");
            NativeLibManager.extract("I:\\superresolution\\fabric\\run");
        }
        NativeLibManager.load("I:\\superresolution\\fabric\\run");
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
            return name + ".so";
        }
    }
}
