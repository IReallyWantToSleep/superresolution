package io.homo.superresolution.common.gui.entries;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import io.homo.superresolution.common.gui.InfoBuilder;
import io.homo.superresolution.common.gui.ScissorsHandler;
import io.homo.superresolution.common.gui.widgets.Line;
import io.homo.superresolution.core.utils.ColorUtil;
import me.shedaniel.clothconfig2.api.animator.ValueAnimator;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.math.Color;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class ClothTextListListEntry extends TooltipListEntry<Object> implements InfoBuilder.LineContainer {
    public static final int DISABLED_COLOR;
    #if MC_VER > MC_1_20_6
    protected static final ResourceLocation CONFIG_TEX = ResourceLocation.fromNamespaceAndPath("cloth-config2", "textures/gui/cloth_config.png");
    #else
    protected static final ResourceLocation CONFIG_TEX = new ResourceLocation("cloth-config2", "textures/gui/cloth_config.png");
    #endif


    static {
        DISABLED_COLOR = Objects.requireNonNull(ChatFormatting.DARK_GRAY.getColor());
    }

    protected final Font textRenderer;
    private final ValueAnimator<Double> expandAnimator = ValueAnimator.ofDouble(0.0);
    private final ValueAnimator<Double> rotatingAnimator = ValueAnimator.ofDouble(0.0);
    private final ValueAnimator<Double> backgroundAnimator = ValueAnimator.ofDouble(0.0);

    private final Rectangle mainRectangle = new Rectangle();
    private final List<LineRenderer> lineRenderers = new ArrayList<>();
    private final boolean canExpand;
    protected int savedWidth;
    protected int savedX;
    protected int savedY;
    private int top = 0;
    private int bottom = 0;
    private boolean expanded = false;
    private boolean showExpandButton = true;
    private boolean hoverdText = false;


    public ClothTextListListEntry(Component fieldName, Supplier<Optional<Component[]>> tooltipSupplier) {
        this(fieldName, tooltipSupplier, true);
    }

    public ClothTextListListEntry(Component fieldName, Supplier<Optional<Component[]>> tooltipSupplier, boolean canExpand) {
        super(fieldName, tooltipSupplier);
        this.textRenderer = Minecraft.getInstance().font;
        this.canExpand = canExpand;
        if (!this.canExpand) {
            this.expanded = true;
            expandAnimator.setAs(1.0);
            rotatingAnimator.setAs(1.0);
        }
    }

    public ClothTextListListEntry setTop(int top) {
        this.top = top;
        return this;
    }

    public ClothTextListListEntry setBottom(int bottom) {
        this.bottom = bottom;
        return this;
    }

    public ClothTextListListEntry setShowExpandButton(boolean showExpandButton) {
        this.showExpandButton = showExpandButton;
        return this;
    }

    public void addLine(Line line) {
        this.lineRenderers.add(LineRenderer.of(line));
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (!canExpand) return;
        this.expanded = expanded;
        if (this.expanded) {
            expandAnimator.setTo(1.0, 1800);
            rotatingAnimator.setTo(1.0, 400);
        } else {
            expandAnimator.setTo(0.0, 1800);
            rotatingAnimator.setTo(0.0, 400);
        }
    }

    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        if (isHovered) {
            backgroundAnimator.setTo(1.0, 300);
        } else {
            backgroundAnimator.setTo(0.0, 300);
        }

        expandAnimator.update(delta);
        rotatingAnimator.update(delta);
        backgroundAnimator.update(delta);
        mainRectangle.setBounds(x, y, entryWidth, entryHeight);
        this.savedWidth = entryWidth;
        this.savedX = x;
        this.savedY = y;
        int sideWidth = 2;
        int backgroundAlpha = (int) (30 * backgroundAnimator.value()) + 20;
        Style style = this.getTextAt(mouseX, mouseY);
        hoverdText = style != null;
        graphics.fillGradient(x, y, x + entryWidth - 1, y + entryHeight, ColorUtil.color(backgroundAlpha, 255, 255, 255), ColorUtil.color(backgroundAlpha, 255, 255, 255));

        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

        if (canExpand && showExpandButton) {
            #if MC_VER < MC_1_21_6
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.pose().pushPose();
            float center = 4.5f;
            graphics.pose().translate(x - 8 + center + entryWidth - 7, y + 5 + center, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(90 + (float) (180 * rotatingAnimator.value())));
            #if MC_VER > MC_1_21_1
            graphics.blit(net.minecraft.client.renderer.RenderType::guiTextured, CONFIG_TEX, (int) -center, (int) -center, 33f, isHovered ? 18f : 0f, 9, 9, 256, 256);
            #else
            graphics.blit(CONFIG_TEX, (int) -center, (int) -center, 33, isHovered ? 18 : 0, 9, 9);
            #endif
            graphics.pose().popPose();
            #else
            graphics.pose().pushMatrix();
            float center = 4.5f;
            graphics.pose().translate(x - 8 + center + entryWidth - 7, y + 5 + center);
            graphics.pose().rotate(Axis.ZP.rotationDegrees(90 + (float) (180 * rotatingAnimator.value())).angle());


            #if MC_VER > MC_1_21_1
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, CONFIG_TEX, (int) -center, (int) -center, 33f, isHovered ? 18f : 0f, 9, 9, 256, 256);
            #else
            graphics.blit(CONFIG_TEX, (int) -center, (int) -center, 33, isHovered ? 18 : 0, 9, 9);
            #endif
            graphics.pose().popMatrix();
            #endif
        }
        graphics.fillGradient(x, y, x + sideWidth, y + entryHeight, ColorUtil.color(255, 255, 255, 255), ColorUtil.color(255, 255, 255, 255));


        if (expandAnimator.value() != 0.0) {
            #if MC_VER < MC_1_21_4
            ScissorsHandler.scissor(new io.homo.superresolution.common.gui.Rectangle(x, y, entryWidth, (int) ((getItemHeight()) * expandAnimator.value())));
            #else
            graphics.enableScissor(x, y, entryWidth + x, (int) ((getItemHeight()) * expandAnimator.value()) + y);
            #endif
            int yy = y + top;
            for (LineRenderer line : this.lineRenderers) {
                int lineHeight = line.render(graphics, -1, yy, x + sideWidth + 5, entryWidth - (sideWidth + 5), entryHeight, mouseX, mouseY, isHovered, delta, textRenderer);
                Objects.requireNonNull(Minecraft.getInstance().font);
                yy += lineHeight + 1;
            }
            #if MC_VER < MC_1_21_4
            ScissorsHandler.removeLastScissor();
            #else
            graphics.disableScissor();
            #endif
            AbstractConfigScreen configScreen = this.getConfigScreen();
            if (style != null && configScreen != null) {
                graphics.renderComponentHoverEffect(Minecraft.getInstance().font, style, mouseX, mouseY);
            }
        }
        if (expandAnimator.value() < 0.1) {
            graphics.drawCenteredString(textRenderer, getFieldName(), (int) (x + (entryWidth * 0.5)), y + 6, ColorUtil.color((int) (255 - (255 * (expandAnimator.value() * 10))), 255, 255, 255));
        }
    }

    public int getItemHeight() {
        int height = 0;
        for (LineRenderer lineRenderer : this.lineRenderers) {
            if (lineRenderer.saveRectangle != null) {
                height += lineRenderer.saveRectangle.height + 1;
                continue;
            }
            height += lineRenderer.line.height(textRenderer) + 1;
        }
        return Math.min(Math.max((int) (height * expandAnimator.value()) + (expandAnimator.value() == 1.0 ? 3 + bottom : 24), 24), height + 3 + bottom);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Style style = this.getTextAt(mouseX, mouseY);
            AbstractConfigScreen configScreen = this.getConfigScreen();
            if (configScreen != null && configScreen.handleComponentClicked(style)) {
                return true;
            }
            if (mainRectangle.contains(mouseX, mouseY) && this.canExpand && !hoverdText) {
                this.setExpanded(!isExpanded());
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected @Nullable Style getTextAt(double x, double y) {
        if (mainRectangle.contains(x, y) && isExpanded()) {
            List<LineRenderer> lines = this.lineRenderers;
            int size = lines.size();
            if (size == 0) {
                return null;
            }
            int low = 0;
            int high = size - 1;
            int candidate = -1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                LineRenderer line = lines.get(mid);
                Rectangle rect = line.saveRectangle;
                if (rect == null) {
                    break;
                }
                double lineY = rect.y;
                if (lineY <= y) {
                    candidate = mid;
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }

            if (candidate != -1) {
                LineRenderer line = lines.get(candidate);
                Rectangle rect = line.saveRectangle;
                if (rect != null && rect.contains(x, y)) {
                    FormattedCharSequence text = line.line.text.getVisualOrderText();
                    return text != FormattedCharSequence.EMPTY ?
                            this.textRenderer.getSplitter().componentStyleAtWidth(text, rect.x) :
                            Style.EMPTY;
                }
            }
            return null;
        }
        return null;
    }

    @Override
    public boolean isMouseInside(int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight) {
        return false;
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
        public Rectangle saveRectangle;

        LineRenderer(Line line) {
            this.line = line;
        }

        public static LineRenderer of(Line line) {
            return new LineRenderer(line);
        }

        public int render(
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
            List<FormattedCharSequence> splitLines = font.split(line.text, entryWidth - 8);
            int lineHeight = splitLines.size() * (line.height(font)) + (splitLines.size() - 1);
            saveRectangle = new Rectangle(x, y, line.width(font), lineHeight);
            int yOffset = 0;
            for (FormattedCharSequence text : splitLines) {
                #if MC_VER < MC_1_21_6
                graphics.pose().pushPose();
                graphics.pose().translate(x + (line.center ? -7 : 0), y + yOffset, 0);
                graphics.pose().scale(line.scale.x, line.scale.y, 1.0f);
                if (line.center) {
                    graphics.drawCenteredString(
                            font,
                            text,
                            (int) ((entryWidth + 7) * 0.5 / line.scale.x),
                            0,
                            line.color
                    );
                } else {
                    graphics.drawString(
                            font,
                            text,
                            (int) (entryWidth * line.left),
                            0,
                            line.color, false
                    );
                }
                yOffset = yOffset + line.height(font);
                graphics.pose().popPose();
                #else
                graphics.pose().pushMatrix();
                graphics.pose().translate(x + (line.center ? -7 : 0), y + yOffset);
                graphics.pose().scale(line.scale.x, line.scale.y);
                if (line.center) {
                    graphics.drawCenteredString(
                            font,
                            text,
                            (int) ((entryWidth + 7) * 0.5 / line.scale.x),
                            0,
                            ColorUtil.color(255, 255, 255, 255)
                    );
                } else {
                    graphics.drawString(
                            font,
                            text,
                            (int) (entryWidth * line.left),
                            0,
                            ColorUtil.color(255, 255, 255, 255),
                            false
                    );
                }
                yOffset = yOffset + line.height(font);
                graphics.pose().popMatrix();
                #endif
            }
            return lineHeight;
        }
    }
}
