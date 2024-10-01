package io.homo.superresolution.fsr2;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.fsr2.nativelib.FSR2LibManager;
import io.homo.superresolution.fsr2.nativelib.FSR2ApiHelper;
import io.homo.superresolution.fsr2.types.FfxResource;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;
import io.homo.superresolution.utils.FrameBuffer;
import io.homo.superresolution.utils.Texture;
import net.minecraft.client.Minecraft;

import static io.homo.superresolution.fsr2.types.enums.FfxFsr2InitializationFlagBits.*;
import static io.homo.superresolution.fsr2.types.enums.FfxFsr2InitializationFlagBits.FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST;

import org.lwjgl.opengl.GL30;


public class FSR2 implements CanResize, CanDestroy {
    private static Window window = Minecraft.getInstance().getWindow();
    public final FSR2ApiHelper fsr2_api;
    public FSR2Helper helper;
    public Texture TestImg;

    public static FrameBuffer fsr2OutTexture;
    public static FrameBuffer worldFramebuffer;
    public FSR2(){
        RenderSystem.assertOnRenderThread();
        fsr2_api = FSR2LibManager.fsr2api;
        this.helper = new FSR2Helper();
        SuperResolution.LOGGER.info("load test_img");
        TestImg = Texture.loadTexture("I:/superresolution/forge/run/screenshots/world.png");
        fsr2OutTexture = new FrameBuffer(false);
        fsr2OutTexture.setClearColor(0f,0f,0f,1f);
        window = Minecraft.getInstance().getWindow();
        this.resize(window.getScreenWidth(),window.getScreenHeight());
    }

    public void setWorldFramebuffer(FrameBuffer framebuffer) {
        FSR2.worldFramebuffer = framebuffer;
    }

    public void resize(int width, int height){
        RenderSystem.assertOnRenderThread();
        SuperResolution.LOGGER.info("resize {} {}",width,height);
        fsr2OutTexture.resize(width,height,Minecraft.ON_OSX);
        helper.resize(width, height);
        updateFSR2(width, height);
    }

    public FSR2Helper getHelper() {
        return helper;
    }
    public void destroy() {
        getHelper().destroy();
    }
    public void updateFSR2(int width,int height){
        RenderSystem.assertOnRenderThread();
        SuperResolution.LOGGER.info("FSR2 ffxFsr2GetInterfaceGL: {}",
                FFXError.returnErrorText(
                        fsr2_api.ffxFsr2CreateGL(
                                fsr2_api.ffxFsr2GetScratchMemorySizeGL(),
                                Config.getFsr2Ratio(),
                                width,
                                height,
                                FFX_FSR2_ENABLE_DEBUG_CHECKING.getValue() |
                                        FFX_FSR2_ENABLE_AUTO_EXPOSURE.getValue() |
                                        FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST.getValue()// | FFX_FSR2_ENABLE_DEPTH_INFINITE.getValue()
                        )
                )
        );
    }
    public void CallFSR2(float frameTimeDelta){
        RenderSystem.assertOnRenderThread();
        window = Minecraft.getInstance().getWindow();
        helper.updateMotionVectors();
        FfxResource color = fsr2_api.ffxGetTextureResourceGL(
                SuperResolution.mainTarget.getColorTextureId(),
                SuperResolution.mainTarget.width,
                SuperResolution.mainTarget.height,
                GlConst.GL_RGBA8
        );
        FfxResource depth = fsr2_api.ffxGetTextureResourceGL(
                worldFramebuffer.getDepthTextureId(),
                worldFramebuffer.width,
                worldFramebuffer.height,
                6402
        );
        FfxResource out = fsr2_api.ffxGetTextureResourceGL(
                fsr2OutTexture.getColorTextureId(),
                fsr2OutTexture.width,
                fsr2OutTexture.height,
                32856
        );
        SuperResolution.LOGGER.debug("FSR2 ffxFsr2ContextDispatch: {}",
                FFXError.returnErrorText(
                        fsr2_api.ffxFsr2ContextDispatch(
                                color,
                                depth,
                                fsr2_api.ffxGetTextureResourceGL(
                                        helper.getMotionVectorsTex(),
                                        helper.getMotionVectorsBuffer().width,
                                        helper.getMotionVectorsBuffer().height,
                                        GL30.GL_RG16F
                                ),
                                null,
                                null,
                                out,
                                0.1f,
                                0.1f,
                                helper.renderWidth,
                                helper.renderHeight,
                                helper.renderWidth,
                                helper.renderHeight,
                                false,
                                1.0f,
                                frameTimeDelta,
                                1.0f,
                                false,
                                helper.getCameraNear(),
                                helper.getCameraFar(),
                                helper.getCameraFovAngleVertical(),
                                1F,
                                false,
                                helper.screenWidth,
                                helper.screenHeight
                        )
                )
        );

    }
    public void blitToScreen(){
        RenderSystem.assertOnRenderThread();
        fsr2OutTexture.blitToScreen(
                window.getScreenWidth(),
                window.getScreenHeight()
        );
    }
}
