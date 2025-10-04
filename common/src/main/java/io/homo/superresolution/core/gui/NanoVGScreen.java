package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.graphics.nanovg.NanoVG;
import io.homo.superresolution.core.graphics.nanovg.NanoVGContext;
import io.homo.superresolution.core.gui.core.UIDrawContext;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.event.EventHandle;
import io.homo.superresolution.core.gui.core.event.EventListener;
import io.homo.superresolution.core.gui.core.impl.Renderable;
import io.homo.superresolution.core.gui.widgets.AbstractWidget;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.utils.MouseCursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class NanoVGScreen<T> extends Screen implements EventHandle<T> {
    protected final NanoVGContext nvg;
    protected final ArrayList<Renderable> renderable = new ArrayList<>();
    protected final ArrayList<EventListener> eventListener = new ArrayList<>();
    protected final ArrayList<AbstractWidget<?, ?, ?>> widget = new ArrayList<>();
    protected boolean transparent = false;
    protected Map<String, ArrayList<Consumer<T>>> eventListenerMap = new HashMap<>();

    protected NanoVGScreen(Component title) {
        super(title);
        eventListenerMap.put("resize", new ArrayList<>());
        nvg = NanoVG.context;
        buildWidgets();
    }

    @Override
    public void removeEventListener(String type, Object consumer) {
    }

    @Override
    public void addEventListener(String type, Object consumer) {
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
        UIDrawContext drawContext = new UIDrawContext(nvg);
        draw(drawContext, (int) mouseX, (int) mouseY, partialTick);
        nvg.end();
        drawAfter(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
    }

    @Override
    protected void init() {
        super.init();
        invokeEventListener("resize");
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        invokeEventListener("resize");
    }

    protected void invokeEventListener(String type) {
        if (eventListenerMap.get(type) != null)
            for (Consumer<T> consumer : eventListenerMap.get(type)) consumer.accept((T) this);
    }

    public void draw(UIDrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawWidgets(drawContext, mouseX, mouseY, delta);
        drawTooltips(drawContext, mouseX, mouseY, delta);
    }

    public void drawTooltips(UIDrawContext drawContext, int mouseX, int mouseY, float delta) {
        for (AbstractWidget<?, ?, ?> abstractWidget : widget) {
            //abstractWidget.renderTooltip(drawContext, delta);
        }
    }

    public void drawAfter(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {

    }

    public void drawBefore(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        //super.renderBackground(guiGraphics, mouseX, mouseY, delta);
    }

    public void drawWidgets(UIDrawContext drawContext, int mouseX, int mouseY, float delta) {
        Map<Integer, List<Renderable>> layers = Stream.concat(renderable.stream(), widget.stream())
                .collect(Collectors.groupingBy(
                        Renderable::getZIndex,
                        TreeMap::new,
                        Collectors.toList()
                ));

        layers.values().forEach(layer ->
                layer.forEach(r -> r.render(drawContext, new UIInputState(
                        new Vector2f(mouseX, mouseY),
                        delta
                )))
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
        if (widget instanceof EventListener) {
            this.eventListener.remove(widget);
        }
    }

    protected <T extends AbstractWidget<?, ?, ?>> T addWidget(T w) {
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

    protected <T extends AbstractWidget<?, ?, ?>> T addRenderableWidget(T widget) {
        renderable.add(widget);
        eventListener.add(widget);
        return widget;
    }

    protected void invokeEventHandle(Consumer<EventListener> consumer) {
        for (EventListener handle : eventListener) {
            consumer.accept(handle);
        }
    }

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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        invokeEventHandle((handle) -> handle.mouseScroll((float) transformPos(mouseX), (float) transformPos(mouseY), (float) transformPos(scrollX)));
        super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
    public void mouseMoved(double mouseX, double mouseY) {
        invokeEventHandle((handle) -> handle.mouseMove((float) transformPos(mouseX), (float) transformPos(mouseY)));
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        invokeEventHandle((handle) -> handle.charTyped(codePoint, modifiers));
        return true;
    }

    protected double transformPos(double pos) {
        return (Minecraft.getInstance().getWindow().getGuiScale() * pos) / nvg.globalScale();
    }
}
