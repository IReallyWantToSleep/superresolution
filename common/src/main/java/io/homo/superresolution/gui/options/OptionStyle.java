package io.homo.superresolution.gui.options;

import net.minecraft.util.FastColor;


public class OptionStyle {
    public int mainColor;
    public int mainDisabledColor;
    public int bgColor;
    public int bgDisabledColor;
    public int textColor;
    public int textDisabledColor;
    public int hoveredColor;

    public static OptionStyle defaultStyle() {
        OptionStyle style = new OptionStyle();
        style.mainColor = FastColor.ARGB32.color(255, 148, 228, 211);
        style.mainDisabledColor = FastColor.ARGB32.color(80, 255, 255, 255);
        style.bgColor = FastColor.ARGB32.color(255, 205, 201, 201);
        style.bgDisabledColor = FastColor.ARGB32.color(50, 105, 105, 105);
        style.textColor = FastColor.ARGB32.color(255, 255, 255, 255);
        style.textDisabledColor = FastColor.ARGB32.color(80, 255, 255, 255);
        style.hoveredColor = FastColor.ARGB32.color(150, 148, 228, 211);
        return style;
    }
}
