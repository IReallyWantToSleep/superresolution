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

package io.homo.superresolution.core.utils;

import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    public static String getMD5(String string) {
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
            MD5.update(bytes);
            return new String(Hex.encodeHex(MD5.digest()));
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println(getMD5(new File("I:/super_resolution_moddev/superresolution/common/src/main/resources/lib/libSuperResolution+win64.dll")));
        System.out.println(getMD5(new File("I:/super_resolution_moddev/superresolution/common/src/main/resources/lib/libSuperResolution+android.so")));
        System.out.println(getMD5(new File("I:/super_resolution_moddev/superresolution/common/src/main/resources/lib/libSuperResolution+linux64.so")));


    }
}