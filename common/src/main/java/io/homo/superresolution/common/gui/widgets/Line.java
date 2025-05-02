package io.homo.superresolution.common.gui.widgets;

import io.homo.superresolution.core.utils.ColorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;

public class Line {
    public float left;
    public int color;
    public Component text;
    public Vector2f scale = new Vector2f(1.0f, 1.0f);
    public boolean center = false;

    public int width(Font font) {
        return (int) (font.width(text) * scale.x);
    }

    public int height(Font font) {
        return (int) (font.lineHeight * scale.y);
    }

    public Line color(int r, int g, int b, int a) {
        color = ColorUtil.color(a, r, g, b);
        return this;
    }

    public Line color(int rgba) {
        color = rgba;
        return this;
    }

    public Line text(String text) {
        return text(Component.literal(text));
    }

    public Line text(Component text) {
        this.text = text;
        return this;
    }


    public Line scaleX(float x) {
        this.scale.x = x;
        return this;
    }

    public Line scaleY(float y) {
        this.scale.y = y;
        return this;
    }

    public Line scale(float x, float y) {
        this.scale.x = x;
        this.scale.y = y;
        return this;
    }

    public Line scale(float x) {
        this.scale.x = x;
        this.scale.y = x;
        return this;
    }


    public Line left(float left) {
        this.left = left;
        return this;
    }


    public Line center(boolean center) {
        this.center = center;
        return this;
    }

}
