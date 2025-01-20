package io.homo.superresolution.debug;

import net.minecraft.Util;

public class DebugInfo {
    private static final int updateTime = 50;
    private static float frameTimeDelta = 16.6f;
    private static float frameTimeDeltaAlgo = 16.6f;
    private static String textFrameTimeDelta = "";
    private static String textFrameTimeDeltaAlgo = "";
    private static long lastUpdateTime = Util.getMillis();
    private static float _frameTimeDelta = 16.6f;
    private static float _frameTimeDeltaAlgo = 16.6f;

    public static float getFrameTimeDelta() {
        return frameTimeDelta;
    }

    public static void setFrameTimeDelta(float frameTimeDelta) {
        _frameTimeDelta = frameTimeDelta;
        update();
    }

    public static float getFrameTimeDeltaAlgo() {
        return frameTimeDeltaAlgo;
    }

    public static void setFrameTimeDeltaAlgo(float frameTimeDelta_fsr) {
        _frameTimeDeltaAlgo = frameTimeDelta_fsr;
        update();
    }

    public static String getTextFrameTimeDelta() {
        return textFrameTimeDelta;
    }

    public static String getTextFrameTimeDeltaAlgo() {
        return textFrameTimeDeltaAlgo;
    }

   private static void update(){
        if (Util.getMillis()-lastUpdateTime > updateTime){
            frameTimeDelta = _frameTimeDelta;
            frameTimeDeltaAlgo = _frameTimeDeltaAlgo;
            textFrameTimeDelta = "世界渲染用时 "+ frameTimeDelta+"ms "+Math.round(1000/frameTimeDelta)+"fps";
            textFrameTimeDeltaAlgo = "超分辨率算法用时 "+ frameTimeDeltaAlgo +"ms "+Math.round(1000/ frameTimeDeltaAlgo)+"fps";
            lastUpdateTime = Util.getMillis();
        }
   }
}
