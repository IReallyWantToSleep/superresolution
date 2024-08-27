package io.homo.superresolution.fsr2;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.fsr2.nativelib.FSR2LibManager;
import io.homo.superresolution.fsr2.types.impl.FfxFsr2Context;
import io.homo.superresolution.impl.CanDestroy;
import io.homo.superresolution.impl.CanResize;

import java.util.Objects;

import static io.homo.superresolution.fsr2.types.enums.FfxFsr2InitializationFlagBits.*;
import static io.homo.superresolution.fsr2.types.enums.FfxFsr2InitializationFlagBits.FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST;


public class FSR2 implements CanResize, CanDestroy {
    private boolean fsr2FirstInit = true;
    private FfxFsr2Context fsr2Context;
    private final FSR2Helper helper;
    public FSR2(){
        this.helper = new FSR2Helper();

    }
    public void resize(int width,int height){
        if (RenderSystem.isOnRenderThread()){
            SuperResolution.LOGGER.info("resize {} {}",width,height);
            helper.resize(width, height);
            SuperResolution.LOGGER.info("FSR2: {}",FFXError.returnErrorText(FSR2LibManager.ffx_fsr2_api.ffxFsr2GetInterfaceGL(FSR2LibManager.ffx_fsr2_api.ffxFsr2GetScratchMemorySizeGL(), Config.getFsr2Ratio(), width,height,FFX_FSR2_ENABLE_DEBUG_CHECKING.getValue() | FFX_FSR2_ENABLE_AUTO_EXPOSURE.getValue() | FFX_FSR2_ENABLE_HIGH_DYNAMIC_RANGE.getValue() |
                    FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST.getValue())));
            SuperResolution.LOGGER.info("FSR2: {}",FFXError.returnErrorText(FSR2LibManager.ffx_fsr2_api.ffxFsr2CreateContext()));

        }else {
            SuperResolution.LOGGER.warn("not call res  ize in Render thread");
        }
}

    public FSR2Helper getHelper() {
        return helper;
    }

    public void destroy() {
        this.helper.destroy();
    }
}
