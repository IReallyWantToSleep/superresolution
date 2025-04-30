package io.homo.superresolution.common.render.gl.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.utils.ShaderCache;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import static io.homo.superresolution.common.render.gl.Gl.*;
import static io.homo.superresolution.common.render.gl.GlConst.*;

public class GlGeneralShaderProgram extends AbstractGlShaderProgram {
    protected GlGeneralShaderProgram() {
    }

    public static GeneralShaderProgramBuilder create() {
        return new GeneralShaderProgramBuilder();
    }

    public GlGeneralShaderProgram compileShader() {
        if (compiled) return this;
        RenderSystem.assertOnRenderThread();
        int FRAGMENT_SHADER = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(FRAGMENT_SHADER, getFragShaderText());
        glCompileShader(FRAGMENT_SHADER);
        if (glGetShaderi(FRAGMENT_SHADER, 35713) == 0) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream("ERROR_FRAGMENT_SHADER_SRC.glsl"))) {
                out.println(this.getFragShaderText());
            } catch (IOException e) {
                SuperResolution.LOGGER.error(e.toString());
            }
            throw new RuntimeException("FRAGMENT_SHADER " + this.shaderName + " 无法编译着色器：" + glGetShaderInfoLog(FRAGMENT_SHADER, 32768));
        }

        int VERTEX_SHADER = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(VERTEX_SHADER, getVertShaderText());
        glCompileShader(VERTEX_SHADER);
        if (glGetShaderi(VERTEX_SHADER, 35713) == 0) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream("ERROR_VERTEX_SHADER_SRC.glsl"))) {
                out.println(this.getVertShaderText());
            } catch (IOException e) {
                SuperResolution.LOGGER.error(e.toString());
            }
            throw new RuntimeException("VERTEX_SHADER " + this.shaderName + " 无法编译着色器：" + glGetShaderInfoLog(VERTEX_SHADER, 32768));
        }

        this.shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, FRAGMENT_SHADER);
        glAttachShader(shaderProgram, VERTEX_SHADER);
        glLinkProgram(shaderProgram);
        this.checkProgram();
        glDeleteShader(FRAGMENT_SHADER);
        glDeleteShader(VERTEX_SHADER);
        if (enableCache) {
            ShaderCache.saveProgramBinary(this);
        }
        compiled = true;
        updateDebugLabel(getDebugLabel());
        return this;
    }
    
    public void setTexture(String name, int textureId, int texture) {
        glActiveTexture(GL_TEXTURE0 + texture);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(getUniformLocation(name), texture);
    }

    public static class ShaderInclude {
        public String name;
        public ArrayList<String> textList;

        private ShaderInclude() {
        }

        public static ShaderInclude create(Collection<String> textList, String name) {
            ShaderInclude i = new ShaderInclude();
            i.textList = new ArrayList<>();
            i.textList.addAll(textList);
            i.name = name;
            return i;
        }
    }

    public static class GeneralShaderProgramBuilder extends AbstractShaderProgramBuilder<GlGeneralShaderProgram> {
        @Override
        public GlGeneralShaderProgram build() {
            return checkShaderCache() ? updateShader(new GlGeneralShaderProgram().fromBin(getShaderCache())) : updateShader(new GlGeneralShaderProgram());
        }
    }

}
