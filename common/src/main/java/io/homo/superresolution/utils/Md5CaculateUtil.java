package io.homo.superresolution.utils;

import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;

public class Md5CaculateUtil {
    public static String getMD5(File file) {
        FileInputStream fileInputStream = null;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return new String(Hex.encodeHex(MD5.digest()));
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public static void main(String[] args) {
        String s = """
                if (os.contains("linux")){
                    libs.add(new NativeLib("libffx_fsr2_api_gl_x64","%s"));
                    libs.add(new NativeLib("libffx_fsr2_api_vk_x64","%s"));
                    libs.add(new NativeLib("libffx_fsr2_api_x64","%s"));
                    libs.add(new NativeLib("libfsr2javalib","%s",1));
                }else if (os.contains("windows")){
                    libs.add(new NativeLib("libffx_fsr2_api_gl_x64","%s"));
                    libs.add(new NativeLib("libffx_fsr2_api_vk_x64","%s"));
                    libs.add(new NativeLib("libffx_fsr2_api_x64","%s"));
                    libs.add(new NativeLib("libfsr2javalib","%s",1));
                }
                """
                .formatted(
                        getMD5(new File("N:\\fsr2_opengl_java\\bin\\libffx_fsr2_api_gl_x64.so")),
                        getMD5(new File("N:\\fsr2_opengl_java\\bin\\libffx_fsr2_api_vk_x64.so")),
                        getMD5(new File("N:\\fsr2_opengl_java\\bin\\libffx_fsr2_api_x64.so")),
                        getMD5(new File("N:\\fsr2_opengl_java\\bin\\libfsr2javalib.so")),
                        getMD5(new File("N:\\fsr2_opengl_java\\bin\\libffx_fsr2_api_gl_x64.dll")),
                        getMD5(new File("N:\\fsr2_opengl_java\\bin\\libffx_fsr2_api_vk_x64.dll")),
                        getMD5(new File("N:\\fsr2_opengl_java\\bin\\libffx_fsr2_api_x64.dll")),
                        getMD5(new File("N:\\fsr2_opengl_java\\bin\\libfsr2javalib.dll"))
                );
        System.out.println(s);
    }
}