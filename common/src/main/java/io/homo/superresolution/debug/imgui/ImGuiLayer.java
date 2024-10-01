package io.homo.superresolution.debug.imgui;

import imgui.ImGui;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.fsr2.FSR2;
import net.minecraft.client.Minecraft;

public class ImGuiLayer {
    public void imgui() {
        int width = 350;
        //SuperResolution.LOGGER.info(SuperResolution.getMinecraftWidth()+" "+SuperResolution.getMinecraftHeight());
        float height = (((float) width) /SuperResolution.getMinecraftWidth())*SuperResolution.getMinecraftHeight();
        ImGui.begin("DEBUG");
        ImGui.text("MotionVectorsTex");
        ImGui.image(SuperResolution.FSR.getHelper().getMotionVectorsTex(),
                width,
                height,
                0,1,1,0);
        if (!(FSR2.fsr2OutTexture==null)){
            ImGui.text("fsr2OutTexture");
            ImGui.image(FSR2.fsr2OutTexture.getColorTextureId(),
                    width,
                    height,
                    0,1,1,0);
        }
        if (!(FSR2.worldFramebuffer==null)){
            ImGui.text("worldFramebuffer");
            ImGui.image(FSR2.worldFramebuffer.getColorTextureId(),
                    width,
                    height,
                    0,1,1,0);
        }
        ImGui.text("MainRenderTarget");
        ImGui.image(Minecraft.getInstance().getMainRenderTarget().getColorTextureId(),
                width,
                height,
                0,1,1,0);

        ImGui.end();
    }
}