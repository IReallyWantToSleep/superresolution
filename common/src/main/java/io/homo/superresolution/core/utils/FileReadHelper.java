package io.homo.superresolution.core.utils;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.upscale.fsr1.FSR1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class FileReadHelper {
    public static ArrayList<String> readText(String path) {
        InputStream inputStream = FSR1.class.getResourceAsStream(path);
        ArrayList<String> lines = new ArrayList<>();

        if (inputStream != null) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                SuperResolution.LOGGER.error(e.toString());
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    SuperResolution.LOGGER.error(e.toString());
                }
            }
        }
        return lines;
    }
}
