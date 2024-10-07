package io.homo.superresolution.upscale.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;
import io.homo.superresolution.upscale.fsr1.FSR1;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static io.homo.superresolution.render.gl.Gl.*;
import static io.homo.superresolution.render.gl.GlConst.GL_EXTENSIONS;
import static io.homo.superresolution.render.gl.GlConst.GL_NUM_EXTENSIONS;

public class AlgorithmHelper implements CanResize, CanDestroy {
    public int renderWidth;
    public int renderHeight;
    public int screenWidth;
    public int screenHeight;
    public static int[] GLVersion = new int[]{0, 0};
    private final ArrayList<String> GLExtension = new ArrayList<>();

    public AlgorithmHelper(){
        RenderSystem.assertOnRenderThread();
        GLVersion = getVersion();
        int l = glGetInteger(GL_NUM_EXTENSIONS);
        for (int i = 0; i < l; ++i) {
            GLExtension.add(glGetStringi(GL_EXTENSIONS,i));
        }
        this.resize(Minecraft.getInstance().getWindow().getScreenWidth(),Minecraft.getInstance().getWindow().getScreenHeight());

    }

    public static ArrayList<String> readText(String path){
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

    public void updateMotionVectors(){
        RenderSystem.assertOnRenderThread();
    }
    public void resize(int width,int height){
        RenderSystem.assertOnRenderThread();
        screenWidth = width;
        screenHeight = height;
        renderWidth = (int) (width* Config.getRenderScaleFactor());
        renderHeight = (int) (height* Config.getRenderScaleFactor());
    }
    public ArrayList<String> getGLExtension(){
        return GLExtension;
    }
    public boolean hasGLExtension(String name){
        return GLExtension.contains(name);
    }
    public void destroy(){}

}
