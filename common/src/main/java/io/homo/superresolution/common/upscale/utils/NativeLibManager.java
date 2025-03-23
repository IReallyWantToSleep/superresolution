package io.homo.superresolution.common.upscale.utils;

import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import io.homo.superresolution.common.utils.Md5CaculateUtil;
import io.netty.util.internal.UnstableApi;
import net.minecraft.SharedConstants;
import oiiaio.fsr.fsr2.FfxFSR2;
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
    @UnstableApi
    public static FfxFSR2 nativeApi;

    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            libs.add(new NativeLib("libffx_fsr2_api_gl_x64", "adcb741d12279be5a7c7dcbe274ccab1"));
            libs.add(new NativeLib("libffx_fsr2_api_vk_x64", "77b3fc7b7ce5880d55279d3cf9784363"));
            libs.add(new NativeLib("libffx_fsr2_api_x64", "ec1baf03bf7ad3b696e411e9187e5d2d"));
            libs.add(new NativeLib("libfsr2javalib", "abd6d67844c0370d3792946e8b47b9f0", 1));
        } else if (os.contains("windows")) {
            libs.add(new NativeLib("libwinpthread-1", "59a112452ceccba063cc85d0513fd0a7"));
            libs.add(new NativeLib("libgcc_s_seh-1", "97ba9d2d7090a7d265cefed7fa40b1bf"));
            libs.add(new NativeLib("libstdc++-6", "b1852a4962ea7185dc353fe6f0ab4553"));
            libs.add(new NativeLib("libffx_fsr2_api_gl_x64", "4020c64716878e181ffa4125b8c16f65"));
            libs.add(new NativeLib("libffx_fsr2_api_vk_x64", "d21472d9390fb0bac6dc4c1b47f6b66a"));
            libs.add(new NativeLib("libffx_fsr2_api_x64", "8fabdd0b3455f34f3338ccf4be0d2102"));
            libs.add(new NativeLib("libfsr2javalib", "5c8d1a057783f0bc6f6c46d718b6d57b", 1));
        }
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
            if (lib.type != 1) {
                System.load(f.getAbsolutePath());
            } else {
                nativeApi = new FfxFSR2(f.getAbsolutePath());
            }
        }
        LOGGER.info("依赖库版本： {}", nativeApi.getVersionInfo());
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
        NativeLibManager.nativeApi.ffxFsr2GetScratchMemorySizeGL();
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
            if (os.contains("linux")) {
                return name + ".so";
            } else if (os.contains("windows")) {
                return name + ".dll";
            }
            return null;
        }
    }
}
