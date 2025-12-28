package io.homo.superresolution.core.gui;

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVG;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGContext;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGDrawContext;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.view.View;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
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

public abstract class NanoVGScreen<T> extends Screen {
    protected final NanoVGContext nvg;
    protected View view = new View();
    protected Frame defaultFrame = new Frame();
    protected YogaNode defaultFrameLayout;

    protected NanoVGScreen(Component title) {
        super(title);
        nvg = NanoVG.context;
        defaultFrameLayout = view.addFrame(defaultFrame);
        defaultFrameLayout.setWidthPercent(100);
        defaultFrameLayout.setHeightPercent(100);
        buildWidgets();
    }

    public NanoVGScreen setTransparent(boolean transparent) {
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
        float mouseX = (float) transformPos(mouseX_);
        float mouseY = (float) transformPos(mouseY_);
        drawBefore(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
        nvg.begin(true);
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
        Vector2f screenSize = MinecraftWindow.getWindowSize();
        view.setViewport(screenSize.x, screenSize.y);

        defaultFrame.updateHitTestDebug(inputState.mousePosition());

        view.render(drawContext, inputState);
    }

    protected void setRoot(AbstractWidget<?> root) {
        this.defaultFrame.setRoot(root);
    }

    protected void enableDebugRender(boolean enabled) {
        view.setDebugRenderEnabled(enabled);
    }

    protected void setDebugBoundsVisible(boolean layout, boolean render, boolean hitTest) {
        view.setDebugBoundsVisible(layout, render, hitTest);
    }

    /**
     * 获取 View 实例，用于添加多个 Frame
     */
    protected View getView() {
        return view;
    }

    /**
     * 获取默认 Frame，用于兼容旧代码
     */
    protected Frame getDefaultFrame() {
        return defaultFrame;
    }

    protected void dispatchMouseMoveToFrame(float x, float y) {
        view.dispatchMouseMove(x, y);
    }

    protected void dispatchMousePressToFrame(float x, float y, int button) {
        view.dispatchMousePress(x, y, button);
    }

    protected void dispatchMouseReleaseToFrame(float x, float y, int button) {
        view.dispatchMouseRelease(x, y, button);
    }

    protected void dispatchMouseScrollToFrame(float x, float y, double scroll) {
        view.dispatchMouseScroll(x, y, scroll);
    }

    protected void dispatchMouseDragToFrame(float mouseX, float mouseY, float dragX, float dragY, int button) {
        view.dispatchMouseDrag(mouseX, mouseY, dragX, dragY, button);
    }

    protected void dispatchKeyPressToFrame(int keyCode, int scancode, int modifiers) {
        view.dispatchKeyPress(keyCode, scancode, modifiers);
    }

    protected void dispatchKeyReleaseToFrame(int keyCode, int scancode, int modifiers) {
        view.dispatchKeyRelease(keyCode, scancode, modifiers);
    }

    protected void dispatchCharTypedToFrame(char codePoint, int modifiers) {
        view.dispatchCharTyped(codePoint, modifiers);
    }

    #if MC_VER > MC_1_21_6
    @Override
    public boolean charTyped(CharacterEvent event) {
        dispatchCharTypedToFrame(((char) event.codepoint()), event.modifiers());
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        dispatchKeyReleaseToFrame(event.key(), event.scancode(), event.modifiers());
        super.keyPressed(event);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        dispatchKeyPressToFrame(event.key(), event.scancode(), event.modifiers());
        super.keyPressed(event);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        dispatchMouseDragToFrame((float) transformPos(event.x()), (float) transformPos(event.y()), (float) transformPos(dragX), (float) transformPos(dragY), event.button());
        super.mouseDragged(event, dragX, dragY);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dispatchMouseReleaseToFrame((float) transformPos(event.x()), (float) transformPos(event.y()), event.button());
        super.mouseReleased(event);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean idk) {
        dispatchMousePressToFrame((float) transformPos(event.x()), (float) transformPos(event.y()), event.button());
        super.mouseClicked(event, idk);
        return true;
    }
    #else
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dispatchMousePressToFrame((float) transformPos(mouseX), (float) transformPos(mouseY), button);
        super.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dispatchMouseReleaseToFrame((float) transformPos(mouseX), (float) transformPos(mouseY), button);
        super.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        dispatchMouseDragToFrame((float) transformPos(mouseX), (float) transformPos(mouseY), (float) transformPos(dragX), (float) transformPos(dragY), button);
        super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        dispatchKeyReleaseToFrame(keyCode, scanCode, modifiers);
        super.keyReleased(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        dispatchKeyPressToFrame(keyCode, scanCode, modifiers);
        super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        dispatchCharTypedToFrame(codePoint, modifiers);
        return true;
    }
    #endif

    #if MC_VER > MC_1_20_1
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        dispatchMouseScrollToFrame((float) transformPos(mouseX), (float) transformPos(mouseY), transformPos(scrollY));
        super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        return true;
    }
    #else
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX) {
        dispatchMouseScrollToFrame((float) transformPos(mouseX), (float) transformPos(mouseY), transformPos(scrollX));
        super.mouseScrolled(mouseX, mouseY, scrollX);
        return true;
    }
    #endif


    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        dispatchMouseMoveToFrame((float) transformPos(mouseX), (float) transformPos(mouseY));
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
