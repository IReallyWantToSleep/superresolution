package io.homo.superresolution.common.gui;

#if MC_VER < MC_1_21_4
import java.util.List;
#endif


public class ScissorsHandler {
    #if MC_VER < MC_1_21_4
    public static void clearScissors() {
        me.shedaniel.clothconfig2.api.ScissorsHandler.INSTANCE.clearScissors();
    }

    public static List<Rectangle> getScissorsAreas() {
        //懒得实现了，留个坑
        return List.of();
    }

    public static void scissor(Rectangle rectangle) {
        me.shedaniel.clothconfig2.api.ScissorsHandler.INSTANCE.scissor(new me.shedaniel.math.Rectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height));

    }

    public static void removeLastScissor() {
        me.shedaniel.clothconfig2.api.ScissorsHandler.INSTANCE.removeLastScissor();

    }

    public static void applyScissors() {
        me.shedaniel.clothconfig2.api.ScissorsHandler.INSTANCE.applyScissors();
    }
    #endif
}
