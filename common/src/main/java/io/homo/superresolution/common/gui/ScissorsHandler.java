package io.homo.superresolution.common.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;


public class ScissorsHandler {

    private static final List<Rectangle> scissorsAreas = Lists.newArrayList();

    public static void clearScissors() {
        scissorsAreas.clear();
        applyScissors();
    }

    public static List<Rectangle> getScissorsAreas() {
        return Collections.unmodifiableList(scissorsAreas);
    }

    public static void scissor(Rectangle rectangle) {
        scissorsAreas.add(rectangle);
        applyScissors();
    }

    public static void removeLastScissor() {
        if (!scissorsAreas.isEmpty())
            scissorsAreas.remove(scissorsAreas.size() - 1);
        applyScissors();
    }

    public static void applyScissors() {
        if (!scissorsAreas.isEmpty()) {

            Rectangle r = scissorsAreas.get(0).clone();
            for (int i = 1; i < scissorsAreas.size(); i++) {
                Rectangle r1 = scissorsAreas.get(i);
                if (r.intersects(r1)) {
                    r.setBounds(r.intersection(r1));
                } else {
                    _applyScissor(new Rectangle());
                    return;
                }
            }
            r.setBounds(Math.min(r.x, r.x + r.width), Math.min(r.y, r.y + r.height), Math.abs(r.width), Math.abs(r.height));
            _applyScissor(r);
        } else {
            _applyScissor(null);
        }
    }

    public static void _applyScissor(Rectangle rect) {
        if (rect != null) {
            GlStateManager._enableScissorTest();
            if (rect.isEmpty()) {
                GlStateManager._scissorBox(0, 0, 0, 0);
            } else {
                Window window = Minecraft.getInstance().getWindow();
                double scaleFactor = window.getGuiScale();
                GlStateManager._scissorBox((int) (rect.x * scaleFactor), (int) ((window.getGuiScaledHeight() - rect.height - rect.y) * scaleFactor), (int) (rect.width * scaleFactor), (int) (rect.height * scaleFactor));
            }
        } else {
            GlStateManager._disableScissorTest();
        }
    }
}
