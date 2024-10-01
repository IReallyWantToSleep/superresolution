package io.homo.superresolution.fsr2.nativelib;

import io.homo.superresolution.utils.Md5CaculateUtil;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static io.homo.superresolution.SuperResolution.LOGGER;

public class FSR2LibManager {
    public static String ffx_fsr2_api_path;
    public static String ffx_fsr2_api_gl_path;
    public static final String ffx_fsr2_api_NAME = "libffx_fsr2_api_x64.dll";
    public static final String ffx_fsr2_api_gl_NAME = "libffx_fsr2_api_gl_x64.dll";
    public static final String BASE_PATH = "/lib";
    public static FSR2ApiHelper fsr2api;
    public static final String ffx_fsr2_api_MD5 = "6845a25c74cb8fb22db7ac0db6263813";
    public static final String ffx_fsr2_api_gl_MD5 = "e97fd8e551384eb6334e8485dd30c6dc";
    private static String lib_path;
    public static void extract(String path) {
        LOGGER.info("开始提取FSR2库文件");
        try {
            extractLibrary(path, ffx_fsr2_api_gl_NAME);
            extractLibrary(path, ffx_fsr2_api_NAME);
        } catch (IOException e) {
            LOGGER.error("FSR2库提取失败;信息: {}", e.toString());
            throw new RuntimeException("FSR2库提取失败");
        }
        LOGGER.info("FSR2库文件已提取到 {}",path);
        lib_path = path;
    }

    public static boolean exists(String path){
        File ffx_fsr2_api_file = Paths.get(path,ffx_fsr2_api_NAME).toFile();
        File ffx_fsr2_api_gl_file = Paths.get(path,ffx_fsr2_api_gl_NAME).toFile();
        String md5_a = Md5CaculateUtil.getMD5(ffx_fsr2_api_file);
        String md5_b = Md5CaculateUtil.getMD5(ffx_fsr2_api_gl_file);
        boolean status = ffx_fsr2_api_file.exists() && ffx_fsr2_api_gl_file.exists();
        if(!(ffx_fsr2_api_file.isFile() && ffx_fsr2_api_gl_file.isFile())) status = false;
        if(!(ffx_fsr2_api_file.canRead() && ffx_fsr2_api_gl_file.canRead())) status = false;
        LOGGER.info("{} {}",ffx_fsr2_api_file.getAbsolutePath(),md5_a);
        LOGGER.info("{} {}",ffx_fsr2_api_gl_file.getAbsolutePath(),md5_b);
        if (!Objects.equals(md5_a, ffx_fsr2_api_MD5)) status = false;
        if (!Objects.equals(md5_b, ffx_fsr2_api_gl_MD5)) status = false;
        lib_path = path;
        return status;
    }

    public static void load(){
        ffx_fsr2_api_path = Paths.get(lib_path, ffx_fsr2_api_NAME).toAbsolutePath().toString();
        ffx_fsr2_api_gl_path = Paths.get(lib_path, ffx_fsr2_api_gl_NAME).toAbsolutePath().toString();
        LOGGER.info("FSR2库路径: {} {}",ffx_fsr2_api_path,ffx_fsr2_api_gl_path);
        System.load(ffx_fsr2_api_path);
        System.load(ffx_fsr2_api_gl_path);
        fsr2api = new FSR2ApiHelper("N:\\fsr2_opengl_java\\bin\\libfsr2javalib.dll");
        //fsr2api.init();
    }

    private static boolean _writeFile(InputStream in ,String path) throws IOException {
        if (in == null) return true;
        File file = new File(path);
        if (file.exists()){
            if(!file.delete()){
                return true;
            }
        }
        if (!file.createNewFile()){
            return true;
        }
        try(OutputStream out = new FileOutputStream(file)){
            byte[] bytes = new byte[1];
            while (in.read(bytes) != -1) {
                out.write(bytes);
            }
            out.flush();
            return false;
        }
    }
    private static void extractLibrary(String path, String libraryName) throws IOException {
        Path sourcePath = Paths.get(BASE_PATH, libraryName);
        Path targetPath = Paths.get(path, libraryName);
        try (InputStream in = FSR2LibManager.class.getClassLoader().getResourceAsStream(sourcePath.toFile().toString())) {
            if (in == null) {
                throw new RuntimeException(libraryName);
            }
            if (!_writeFile(in, targetPath.toString())) {
                LOGGER.info("{} 提取成功", libraryName);
            } else {
                throw new IOException(libraryName + " 提取失败");
            }
        } catch (IOException e) {
            LOGGER.error("{} 提取失败;信息: {}", libraryName, e.toString());
            throw e;
        }
    }

    public static void main(String[] args) {
        if (FSR2LibManager.exists("I:\\superresolution\\fabric\\run")){
            LOGGER.info("FSR2库存在无需提取");
        }else {
            LOGGER.info("FSR2库不存在，正在提取");
            FSR2LibManager.extract("I:\\superresolution\\fabric\\run");
        }
        FSR2LibManager.load();
        FSR2LibManager.fsr2api.ffxFsr2GetScratchMemorySizeGL();
    }
}
