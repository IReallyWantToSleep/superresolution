package io.homo.superresolution.debug;

import net.minecraft.Util;

public class DebugInfo {
    private static float frameTimeDelta = 16.6f;
    private static float frameTimeDelta_fsr = 16.6f;

    private static String text_frameTimeDelta = "";
    private static String text_frameTimeDelta_fsr = "";

    private static final int updateTime = 50;
    private static long lastUpdateTime = Util.getMillis();

    private static float _frameTimeDelta = 16.6f;
    private static float _frameTimeDelta_fsr = 16.6f;

    public static String getTextFrameTimeDelta() {
        return text_frameTimeDelta;
    }

    public static String getTextFrameTimeDeltaFSR2() {
        return text_frameTimeDelta_fsr;
    }

    public static void setFrameTimeDelta(float frameTimeDelta) {
        _frameTimeDelta = frameTimeDelta;
        update();
    }

    public static void setFrameTimeDelta_fsr(float frameTimeDelta_fsr) {
        _frameTimeDelta_fsr = frameTimeDelta_fsr;
        update();
    }

   private static void update(){
        if (Util.getMillis()-lastUpdateTime > updateTime){
            frameTimeDelta = _frameTimeDelta;
            frameTimeDelta_fsr = _frameTimeDelta_fsr;
            text_frameTimeDelta = "世界渲染用时 "+ frameTimeDelta+"ms "+Math.round(1000/frameTimeDelta)+"fps";
            text_frameTimeDelta_fsr = "FSR2计算用时 "+ frameTimeDelta_fsr+"ms "+Math.round(1000/frameTimeDelta_fsr)+"fps";
            lastUpdateTime = Util.getMillis();
        }
   }
}
