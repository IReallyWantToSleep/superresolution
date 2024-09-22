package io.homo.superresolution.fsr2;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.fsr2.nativelib.FSR2LibManager;
import io.homo.superresolution.fsr2.nativelib.ffx_fsr2_api;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;
import io.homo.superresolution.utils.FrameBuffer;
import net.minecraft.client.Minecraft;

import static io.homo.superresolution.fsr2.types.enums.FfxFsr2InitializationFlagBits.*;
import static io.homo.superresolution.fsr2.types.enums.FfxFsr2InitializationFlagBits.FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST;

import org.lwjgl.opengl.GL30;


public class FSR2 implements CanResize, CanDestroy {
    private boolean hasFsr2Context = false;
    private static Window window = Minecraft.getInstance().getWindow();
    private static final ffx_fsr2_api fsr2_api = FSR2LibManager.ffx_fsr2_api;
    private final FSR2Helper helper;


    public static MainTarget fsr2OutFramebuffer;
    public static FrameBuffer worldFramebuffer;
    public FSR2(){
        this.helper = new FSR2Helper();
        fsr2OutFramebuffer = new MainTarget(854,480);
        fsr2OutFramebuffer.setClearColor(0f,0f,0f,1f);
        window = Minecraft.getInstance().getWindow();
    }

    public void setWorldFramebuffer(FrameBuffer framebuffer) {
        FSR2.worldFramebuffer = framebuffer;
    }

    public void resize(int width, int height){
        if (RenderSystem.isOnRenderThread()){
                SuperResolution.LOGGER.debug("resize {} {}",width,height);
                fsr2OutFramebuffer.resize(width,height,Minecraft.ON_OSX);
                helper.resize(width, height);
                updateFSR2(width, height);

        }else {
            SuperResolution.LOGGER.warn("not call resize in Render thread");
        }
    }

    public FSR2Helper getHelper() {
        return helper;
    }
    public void destroy() {
        getHelper().destroy();
    }
    public void updateFSR2(int width,int height){
        RenderSystem.assertOnRenderThread();
        if (hasFsr2Context) {
            hasFsr2Context = false;
            SuperResolution.LOGGER.debug("FSR2 ffxFsr2ContextDestroy: {}",
                    FFXError.returnErrorText(
                            fsr2_api.ffxFsr2ContextDestroy()
                    )
            );
        }
        SuperResolution.LOGGER.debug("FSR2 ffxFsr2GetInterfaceGL: {}",
                FFXError.returnErrorText(
                        fsr2_api.ffxFsr2GetInterfaceGL(
                                fsr2_api.ffxFsr2GetScratchMemorySizeGL(),
                                Config.getFsr2Ratio(),
                                width,
                                height,
                                FFX_FSR2_ENABLE_DEBUG_CHECKING.getValue() |
                                        FFX_FSR2_ENABLE_AUTO_EXPOSURE.getValue() |
                                        FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST.getValue() | FFX_FSR2_ENABLE_DEPTH_INFINITE.getValue()
                        )
                )
        );
        if (!hasFsr2Context) {
            SuperResolution.LOGGER.debug("FSR2 ffxFsr2CreateContext: {}",
                    FFXError.returnErrorText(
                            fsr2_api.ffxFsr2CreateContext()
                    )
            );
        }
        hasFsr2Context = true;

    }
    public void CallFSR2(float frameTimeDelta){
        RenderSystem.assertOnRenderThread();
        window = Minecraft.getInstance().getWindow();
        //fsr2OutFramebuffer.clear(Minecraft.ON_OSX);
        helper.updateMotionVectors();
        SuperResolution.LOGGER.info("FSR2 ffxFsr2ContextDispatch: {}",
                FFXError.returnErrorText(
                        fsr2_api.ffxFsr2ContextDispatch(
                                fsr2_api.ffxGetTextureResourceGL(
                                        worldFramebuffer.getColorTextureId(),
                                        worldFramebuffer.width,
                                        worldFramebuffer.height,
                                        GL30.GL_R11F_G11F_B10F
                                ),
                                fsr2_api.ffxGetTextureResourceGL(
                                        worldFramebuffer.getDepthTextureId(),
                                        worldFramebuffer.width,
                                        worldFramebuffer.height,
                                        GL30.GL_DEPTH_COMPONENT32F
                                ),
                                fsr2_api.ffxGetTextureResourceGL(
                                        helper.getMotionVectorsTex(),
                                        helper.getMotionVectorsBuffer().width,
                                        helper.getMotionVectorsBuffer().height,
                                        GL30.GL_RG16F
                                ),
                                null,
                                null,
                                fsr2_api.ffxGetTextureResourceGL(
                                        fsr2OutFramebuffer.getColorTextureId(),
                                        fsr2OutFramebuffer.width,
                                        fsr2OutFramebuffer.height,
                                        GL30.GL_R11F_G11F_B10F
                                ),
                                0f,
                                0f,
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
        //GLFW.glfwGetProcAddress()
        fsr2OutFramebuffer.blitToScreen(
                window.getScreenWidth(),
                window.getScreenHeight()
        );
    }
}
