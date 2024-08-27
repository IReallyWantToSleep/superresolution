package io.homo.superresolution.fsr2.nativelib;

import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.utils.Md5CaculateUtil;

import java.io.*;
import java.nio.file.Paths;
import java.util.Objects;

public class FSR2LibManager {
    public static String ffx_fsr2_api_path;
    public static String ffx_fsr2_api_gl_path;
    public static final String ffx_fsr2_api_NAME = "ffx_fsr2_api_x64.dll";
    public static final String ffx_fsr2_api_gl_NAME = "ffx_fsr2_api_gl_x64.dll";
    public static final String BASE_PATH = "../../../../../lib";
    public static ffx_fsr2_api ffx_fsr2_api;
    public static ffx_fsr2_api_gl ffx_fsr2_api_gl;
    public static final String ffx_fsr2_api_MD5 = "824aa3816700640e8f826071a93a49ab";
    public static final String ffx_fsr2_api_gl_MD5 = "ac639ac510fc91c4446f15197b95d4c6";
    private static String lib_path;
    public static void extract(String path) {
        SuperResolution.LOGGER.info("Start extracting FSR2 library files");
        try {
            try (InputStream in = FSR2LibManager.class.getResourceAsStream(Paths.get(BASE_PATH,ffx_fsr2_api_gl_NAME).toString())) {
                try{
                    if (_writeFile(in, Paths.get(path, ffx_fsr2_api_gl_NAME).toString())){
                        throw new IOException(ffx_fsr2_api_gl_NAME+" extraction failed");
                    }
                }catch (IOException err){
                    SuperResolution.LOGGER.error("{} extraction failed;msg: {}",ffx_fsr2_api_gl_NAME,err.toString());
                    throw new IOException(ffx_fsr2_api_gl_NAME+" extraction failed");
                }
            }
            try (InputStream in = FSR2LibManager.class.getResourceAsStream(Paths.get(BASE_PATH,ffx_fsr2_api_NAME).toString())) {
                try{
                    if (_writeFile(in, Paths.get(path, ffx_fsr2_api_NAME).toString())){
                        throw new IOException(ffx_fsr2_api_NAME+" extraction failed");
                    }
                }catch (IOException err){
                    SuperResolution.LOGGER.error("{} extraction failed;msg: {}",ffx_fsr2_api_NAME,err.toString());
                    throw new IOException(ffx_fsr2_api_NAME+" extraction failed");
                }
            }
        }catch (IOException err){
            SuperResolution.LOGGER.error("FSR2 library extraction failed;msg: {}",err.toString());
        }
        lib_path = path;

    }

    public static boolean exists(String path){
        File ffx_fsr2_api_file = Paths.get(path,ffx_fsr2_api_NAME).toFile();
        File ffx_fsr2_api_gl_file = Paths.get(path,ffx_fsr2_api_gl_NAME).toFile();
        String md5_a = Md5CaculateUtil.getMD5(ffx_fsr2_api_file);
        String md5_b = Md5CaculateUtil.getMD5(ffx_fsr2_api_gl_file);
        boolean status = true;
        if(!(ffx_fsr2_api_file.exists() && ffx_fsr2_api_gl_file.exists())) status = false;
        if(!(ffx_fsr2_api_file.isFile() && ffx_fsr2_api_gl_file.isFile())) status = false;
        if(!(ffx_fsr2_api_file.canRead() && ffx_fsr2_api_gl_file.canRead())) status = false;
        SuperResolution.LOGGER.info("{} {}",ffx_fsr2_api_file.getAbsolutePath(),md5_a);
        SuperResolution.LOGGER.info("{} {}",ffx_fsr2_api_gl_file.getAbsolutePath(),md5_b);
        if (!Objects.equals(md5_a, ffx_fsr2_api_MD5)) status = false;
        if (!Objects.equals(md5_b, ffx_fsr2_api_gl_MD5)) status = false;
        lib_path = path;
        return status;
    }

    public static void load(){
        ffx_fsr2_api_path = Paths.get(lib_path, ffx_fsr2_api_NAME).toAbsolutePath().toString();
        ffx_fsr2_api_gl_path = Paths.get(lib_path, ffx_fsr2_api_gl_NAME).toAbsolutePath().toString();
        SuperResolution.LOGGER.info("FSR2 library path: {} {}",ffx_fsr2_api_path,ffx_fsr2_api_gl_path);
        //ffx_fsr2_api_gl = new ffx_fsr2_api_gl(ffx_fsr2_api_gl_path);
        ffx_fsr2_api = new ffx_fsr2_api("I:/superresolution/fsr2_win64/x64/Release/fsr2_win64.dll");
        ffx_fsr2_api.init();
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
    public void CopyDLLToSystem32(){

    }
}
