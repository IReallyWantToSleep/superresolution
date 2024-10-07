package io.homo.superresolution.debug;

import net.minecraft.Util;

public class DebugInfo {
    private static float frameTimeDelta = 16.6f;
    private static float frameTimeDelta_algo = 16.6f;

    private static String text_frameTimeDelta = "";
    private static String text_frameTimeDelta_algo = "";

    private static final int updateTime = 50;
    private static long lastUpdateTime = Util.getMillis();

    private static float _frameTimeDelta = 16.6f;
    private static float _frameTimeDelta_algo = 16.6f;

    public static String getTextFrameTimeDelta() {
        return text_frameTimeDelta;
    }

    public static String getTextFrameTimeDeltaAlgo() {
        return text_frameTimeDelta_algo;
    }

    public static void setFrameTimeDelta(float frameTimeDelta) {
        _frameTimeDelta = frameTimeDelta;
        update();
    }

    public static void setFrameTimeDelta_algo(float frameTimeDelta_fsr) {
        _frameTimeDelta_algo = frameTimeDelta_fsr;
        update();
    }

   private static void update(){
        if (Util.getMillis()-lastUpdateTime > updateTime){
            frameTimeDelta = _frameTimeDelta;
            frameTimeDelta_algo = _frameTimeDelta_algo;
            text_frameTimeDelta = "世界渲染用时 "+ frameTimeDelta+"ms "+Math.round(1000/frameTimeDelta)+"fps";
            text_frameTimeDelta_algo = "升采样算法计算用时 "+ frameTimeDelta_algo +"ms "+Math.round(1000/ frameTimeDelta_algo)+"fps";
            lastUpdateTime = Util.getMillis();
        }
   }
}
