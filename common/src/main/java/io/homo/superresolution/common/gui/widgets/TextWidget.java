package io.homo.superresolution.common.gui.widgets;

import io.homo.superresolution.common.gui.Rect;
import io.homo.superresolution.common.gui.Vec2;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class TextWidget extends AbstractWidget {
    public final ArrayList<Line> lines = new ArrayList<>();
    public Rect rect;
    public Vec2 border = new Vec2(0, 0);
    public double offsetX = 0.0;
    public double offsetY = 0.0;
    public int maxWidth = 0;
    public int maxHeight = 0;

    public TextWidget(int x, int y, int width, int height, ArrayList<Line> lines) {
        this.rect = new Rect(x, y, width, height);
        this.lines.addAll(lines);
    }

    public TextWidget(int x, int y, int width, int height) {
        this.rect = new Rect(x, y, width, height);
    }

    private void recalculateDimensions() {
        maxWidth = 0;
        maxHeight = 0;
        for (Line line : lines) {
            int lineWidth = (int) (line.width(font) * line.scale.x);
            if (lineWidth > maxWidth) maxWidth = lineWidth;
            maxHeight += (int) (line.height(font) * line.scale.y);
        }
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (rect.in(mouseX, mouseY)) {
            if (canScrollX()) this.offsetX -= dragX * 1.7;
            if (canScrollY()) this.offsetY -= dragY * 1.7;
            clampOffsets();
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (rect.in(mouseX, mouseY)) {
            if (canScrollY()) {
                this.offsetY -= scrollY * 30;
            } else if (canScrollX()) {
                this.offsetX -= scrollY * 30;
            }
            clampOffsets();
        }
        return true;
    }

    private void clampOffsets() {
        int visibleWidth = (int) (rect.width - border.x - border.x);
        if (visibleWidth > 0) {
            offsetX = Math.max(0, Math.min(offsetX, maxWidth - visibleWidth));
        } else {
            offsetX = 0;
        }

        int visibleHeight = (int) (rect.height - border.y - border.y);
        if (visibleHeight > 0) {
            offsetY = Math.max(0, Math.min(offsetY, maxHeight - visibleHeight));
        } else {
            offsetY = 0;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.rect.in(mouseX, mouseY)) {
            this.focused = true;
            return true;
        } else {
            this.focused = false;
            return false;
        }
    }

    public boolean canScrollX() {
        return maxWidth > (rect.width - border.x - border.x);
    }

    public boolean canScrollY() {
        return maxHeight > (rect.height - border.y - border.y);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.hovered = this.rect.in(mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.rect.in(mouseX, mouseY);
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.focused = true;
        this.hovered = true;
        clampOffsets();
        double baseX = rect.x - offsetX + border.x;
        double baseY = rect.y - offsetY + border.y;
        guiGraphics.enableScissor(
                (int) (rect.x + border.x),
                (int) (rect.y + border.y),
                (int) (rect.getLimitX() - (border.x * 4)),
                (int) (rect.getLimitY() - (border.y * 7))
        );

        int lineHeight = 0;
        int dividerCount = 0;
        for (Line line : lines) {
            int lineWidth = line.width(font);
            if (lineWidth > maxWidth) maxWidth = lineWidth;
            guiGraphics.pose().pushPose();
            if (line.type == Line.LineType.Text) {
                guiGraphics.pose().translate(
                        baseX + (rect.width * line.left) + (line.center ? ((double) (rect.width - (border.x * 2)) / 2) - ((double) lineWidth * 0.5) : 0),
                        baseY + lineHeight,
                        0.0f
                );
                guiGraphics.pose().scale(line.scale.x, line.scale.y, 1.0f);
                guiGraphics.drawString(this.font, line.text, 0, 0, line.color);
                lineHeight += line.height(this.font);
            } else {
                dividerCount++;
                guiGraphics.pose().translate(
                        baseX + (rect.width * line.left),
                        baseY + lineHeight,
                        0.0f
                );
                guiGraphics.fill(
                        0,
                        1,
                        (int) (rect.width - (border.x * 2)),
                        2,
                        line.color
                );
                lineHeight += 3;
            }
            guiGraphics.pose().popPose();
        }
        guiGraphics.disableScissor();
        maxHeight = lineHeight + (int) (dividerCount * 3.5);
    }

    public TextWidget addLine(Line line) {
        this.lines.add(line);
        return this;
    }
}