package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVG;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGContext;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGDrawContext;
import io.homo.superresolution.core.gui.core.event.GuiEventListener;
import io.homo.superresolution.core.gui.core.impl.Renderable;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import org.joml.Vector2f;
import io.homo.superresolution.core.utils.MouseCursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
#if MC_VER >MC_1_21_6
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
#endif
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class NanoVGScreen<T> extends Screen {
    protected final NanoVGContext nvg;
    protected final ArrayList<Renderable> renderable = new ArrayList<>();
    protected final ArrayList<GuiEventListener> eventListener = new ArrayList<>();
    protected final ArrayList<AbstractWidget<?>> widget = new ArrayList<>();
    protected boolean transparent = false;

    protected NanoVGScreen(Component title) {
        super(title);
        nvg = NanoVG.context;
        buildWidgets();
    }

    public boolean isTransparent() {
        return transparent;
    }

    public NanoVGScreen setTransparent(boolean transparent) {
        this.transparent = transparent;
        return this;
    }

    protected abstract void buildWidgets();

    @Override
    public void onClose() {
        super.onClose();
        MouseCursor.ARROW.use();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX_, int mouseY_, float partialTick) {
        float mouseX = (float) (Minecraft.getInstance().getWindow().getGuiScale() * mouseX_);
        float mouseY = (float) (Minecraft.getInstance().getWindow().getGuiScale() * mouseY_);
        drawBefore(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
        nvg.begin(transparent);
        nvg.resetGlobalTransform();
        nvg.resetTransform();
        nvg.globalAlpha(1.0f);
        NanoVGDrawContext drawContext = new NanoVGDrawContext(nvg);
        draw(drawContext, new UIInputState(
                new Vector2f(mouseX, mouseY),
                partialTick
        ));
        drawContext.drawAll();
        nvg.end();
        drawAfter(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
    }

    public void draw(IUIDrawContext drawContext, UIInputState inputState) {
        drawBefore(drawContext, inputState);
        drawWidgets(drawContext, inputState);
        drawTooltips(drawContext, inputState);
        drawAfter(drawContext, inputState);
    }

    public void drawTooltips(IUIDrawContext drawContext, UIInputState inputState) {
    }

    public void drawAfter(IUIDrawContext drawContext, UIInputState inputState) {

    }

    public void drawBefore(IUIDrawContext drawContext, UIInputState inputState) {

    }

    public void drawAfter(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {

    }

    public void drawBefore(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        //super.renderBackground(guiGraphics, mouseX, mouseY, delta);
    }

    public void drawWidgets(IUIDrawContext drawContext, UIInputState inputState) {
        Map<Integer, List<Renderable>> layers = Stream.concat(renderable.stream(), widget.stream())
                .collect(Collectors.groupingBy(
                        Renderable::getZIndex,
                        TreeMap::new,
                        Collectors.toList()
                ));

        layers.values().forEach(layer ->
                layer.forEach(r -> r.render(drawContext, inputState))
        );
    }


    @Override
    protected void clearWidgets() {
        super.clearWidgets();
        renderable.clear();
        widget.clear();
        eventListener.clear();
    }

    protected void removeWidget(Object widget) {
        if (widget instanceof Renderable) {
            renderable.remove(widget);
        }
        if (widget instanceof AbstractWidget) {
            this.widget.remove(widget);
        }
        if (widget instanceof GuiEventListener) {
            this.eventListener.remove(widget);
        }
    }

    protected <T extends AbstractWidget<?>> T addWidget(T w) {
        widget.add(w);
        eventListener.add(w);
        return w;
    }

    @Override
    protected void rebuildWidgets() {
    }


    protected <T extends Renderable> T addRenderableOnly(T renderable) {
        this.renderable.add(renderable);
        return renderable;
    }

    protected <T extends AbstractWidget<?>> T addRenderableWidget(T widget) {
        renderable.add(widget);
        eventListener.add(widget);
        return widget;
    }

    protected void invokeEventHandle(Consumer<GuiEventListener> consumer) {
        for (GuiEventListener handle : eventListener) {
            consumer.accept(handle);
        }
    }

    #if MC_VER > MC_1_21_6
    @Override
    public boolean charTyped(CharacterEvent event) {
        invokeEventHandle((handle) -> handle.charTyped(((char) event.codepoint()), event.modifiers()));
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        invokeEventHandle((handle) -> handle.keyPress(event.key(), event.scancode(), event.modifiers()));
        super.keyPressed(event);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        invokeEventHandle((handle) -> handle.keyPress(event.key(), event.scancode(), event.modifiers()));
        super.keyPressed(event);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        invokeEventHandle((handle) -> handle.mouseDrag((float) transformPos(event.x()), (float) transformPos(event.y()), (float) transformPos(dragX), (float) transformPos(dragY), event.button()));
        super.mouseDragged(event, dragX, dragY);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        invokeEventHandle((handle) -> handle.mouseRelease((float) transformPos(event.x()), (float) transformPos(event.y()), event.button()));
        super.mouseReleased(event);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean idk) {
        invokeEventHandle((handle) -> handle.mousePress((float) transformPos(event.x()), (float) transformPos(event.y()), event.button()));
        super.mouseClicked(event, idk);
        return true;
    }
    #else
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        invokeEventHandle((handle) -> handle.mousePress((float) transformPos(mouseX), (float) transformPos(mouseY), button));
        super.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        invokeEventHandle((handle) -> handle.mouseRelease((float) transformPos(mouseX), (float) transformPos(mouseY), button));
        super.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        invokeEventHandle((handle) -> handle.mouseDrag((float) transformPos(mouseX), (float) transformPos(mouseY), (float) transformPos(dragX), (float) transformPos(dragY), button));
        super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        invokeEventHandle((handle) -> handle.keyRelease(keyCode, scanCode, modifiers));
        super.keyReleased(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        invokeEventHandle((handle) -> handle.keyPress(keyCode, scanCode, modifiers));
        super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        invokeEventHandle((handle) -> handle.charTyped(codePoint, modifiers));
        return true;
    }
    #endif

    #if MC_VER > MC_1_20_1
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        invokeEventHandle((handle) -> handle.mouseScroll((float) transformPos(mouseX), (float) transformPos(mouseY), (float) transformPos(scrollY)));
        super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        return true;
    }
    #else
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX) {
        invokeEventHandle((handle) -> handle.mouseScroll((float) transformPos(mouseX), (float) transformPos(mouseY), (float) transformPos(scrollX)));
        super.mouseScrolled(mouseX, mouseY, scrollX);
        return true;
    }
    #endif


    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        invokeEventHandle((handle) -> handle.mouseMove((float) transformPos(mouseX), (float) transformPos(mouseY)));
        super.mouseMoved(mouseX, mouseY);
    }


    protected double transformPos(double pos) {
        return (Minecraft.getInstance().getWindow().getGuiScale() * pos) / nvg.globalScale();
    }

    #if MC_VER <= MC_1_20_1
    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        return;
    }
    #else
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        return;
    }
    #endif

}
