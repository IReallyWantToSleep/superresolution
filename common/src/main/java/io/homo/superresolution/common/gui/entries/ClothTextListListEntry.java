package io.homo.superresolution.common.gui.entries;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import io.homo.superresolution.common.gui.InfoBuilder;
import io.homo.superresolution.common.gui.ScissorsHandler;
import io.homo.superresolution.common.gui.widgets.Line;
import io.homo.superresolution.common.utils.ColorUtil;
import me.shedaniel.clothconfig2.api.animator.ValueAnimator;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.math.Rectangle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class ClothTextListListEntry extends TooltipListEntry<Object> implements InfoBuilder.LineContainer {
    public static final int DISABLED_COLOR;
    protected static final ResourceLocation CONFIG_TEX = ResourceLocation.fromNamespaceAndPath("cloth-config2", "textures/gui/cloth_config.png");


    static {
        DISABLED_COLOR = Objects.requireNonNull(ChatFormatting.DARK_GRAY.getColor());
    }

    protected final Font textRenderer;
    private final ValueAnimator<Double> expandAnimator = ValueAnimator.ofDouble(0.0);
    private final ValueAnimator<Double> rotatingAnimator = ValueAnimator.ofDouble(0.0);
    private final Rectangle mainRectangle = new Rectangle();
    private final List<LineRenderer> lineRenderers = new ArrayList<>();
    private final boolean canExpand;
    protected int savedWidth;
    protected int savedX;
    protected int savedY;
    private boolean expanded = false;

    public ClothTextListListEntry(Component fieldName, Supplier<Optional<Component[]>> tooltipSupplier) {
        this(fieldName, tooltipSupplier, true);
    }

    public ClothTextListListEntry(Component fieldName, Supplier<Optional<Component[]>> tooltipSupplier, boolean canExpand) {
        super(fieldName, tooltipSupplier);
        this.textRenderer = Minecraft.getInstance().font;
        this.canExpand = canExpand;
        if (!this.canExpand) setExpanded(true);
    }

    public void addLine(Line line) {
        this.lineRenderers.add(LineRenderer.of(line));
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        if (this.expanded) {
            expandAnimator.setTo(1.0, 2500);
            rotatingAnimator.setTo(1.0, 500);
        } else {
            expandAnimator.setTo(0.0, 2500);
            rotatingAnimator.setTo(0.0, 500);
        }
    }

    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        expandAnimator.update(delta);
        rotatingAnimator.update(delta);
        mainRectangle.setBounds(x, y, entryWidth, entryHeight);
        this.savedWidth = entryWidth;
        this.savedX = x;
        this.savedY = y;
        int sideWidth = 2;
        if (canExpand) {
            RenderSystem.setShaderTexture(0, CONFIG_TEX);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.pose().pushPose();
            float center = 4.5f;
            graphics.pose().translate(x - 7 + center + entryWidth, y + 5 + center, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(90 + (float) (90 * rotatingAnimator.value())));
            graphics.blit(CONFIG_TEX, (int) -center, (int) -center, 33, isHovered ? 18 : 0, 9, 9);
            graphics.pose().popPose();
        }

        graphics.fillGradient(x, y, x + sideWidth, y + entryHeight, ColorUtil.color(255, 255, 255, 255), ColorUtil.color(255, 255, 255, 255));


        if (expandAnimator.value() != 0.0) {
            ScissorsHandler.scissor(new io.homo.superresolution.common.gui.Rectangle(x, y, entryWidth, (int) ((getItemHeight()) * expandAnimator.value())));
            int yy = y;
            for (LineRenderer line : this.lineRenderers) {
                line.render(graphics, -1, yy, x + sideWidth + 5, entryWidth - (sideWidth + 5), entryHeight, mouseX, mouseY, isHovered, delta, textRenderer);
                Objects.requireNonNull(Minecraft.getInstance().font);
                yy += line.line.height(textRenderer) + 1;
            }
            ScissorsHandler.removeLastScissor();
            Style style = this.getTextAt(mouseX, mouseY);
            AbstractConfigScreen configScreen = this.getConfigScreen();
            if (style != null && configScreen != null) {
                graphics.renderComponentHoverEffect(Minecraft.getInstance().font, style, mouseX, mouseY);
            }
        }
    }

    public int getItemHeight() {
        int height = 0;
        for (LineRenderer lineRenderer : this.lineRenderers) {
            height += lineRenderer.line.height(textRenderer) + 1;
        }
        return (int) (height * expandAnimator.value()) + (isExpanded() ? 3 : 24);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mainRectangle.contains(mouseX, mouseY) && this.canExpand) {
                this.setExpanded(!isExpanded());
            }
            Style style = this.getTextAt(mouseX, mouseY);
            AbstractConfigScreen configScreen = this.getConfigScreen();
            if (configScreen != null && configScreen.handleComponentClicked(style)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected @Nullable Style getTextAt(double x, double y) {
        int lineCount = this.lineRenderers.size();
        if (lineCount > 0) {
            int textX = Mth.floor(x - (double) this.savedX);
            int textY = Mth.floor(y - (double) 7.0F - (double) this.savedY);
            if (textX >= 0 && textY >= 0 && textX <= this.savedWidth && textY < 12 * lineCount + lineCount) {
                int line = textY / 12;
                if (line < this.lineRenderers.size()) {
                    FormattedCharSequence orderedText = this.lineRenderers.get(line).line.text;
                    return orderedText == null ? Style.EMPTY : this.textRenderer.getSplitter().componentStyleAtWidth(orderedText, textX);
                }
            }
        }

        return null;
    }

    public Object getValue() {
        return null;
    }

    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    public @NotNull List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }

    public static class LineRenderer {
        public Line line;

        LineRenderer(Line line) {
            this.line = line;
        }

        public static LineRenderer of(Line line) {
            return new LineRenderer(line);
        }

        public void render(
                GuiGraphics graphics,
                int index,
                int y,
                int x,
                int entryWidth,
                int entryHeight,
                int mouseX,
                int mouseY,
                boolean isHovered,
                float delta,
                Font font
        ) {
            graphics.drawString(font, line.text, (int) (x + (entryWidth * line.left)), y, line.color);
            graphics.fillGradient((int) (x + (entryWidth * line.left)), y, x + entryWidth, y + line.height(font), ColorUtil.color(255, (int) (Math.random() * 255), 255, 255), ColorUtil.color(255, 0, 255, 255));
        }
    }

}
