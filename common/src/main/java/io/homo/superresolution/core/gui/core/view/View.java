/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.gui.core.view;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.TooltipRenderer;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.render.RenderLayer;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import io.homo.superresolution.core.gui.core.layout.ILayoutContainer;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.dialog.MaterialDialog;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MouseCursor;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class View {
    private final List<FrameEntry> frames = new ArrayList<>();
    private final YogaNode rootNode;
    private final TooltipRenderer tooltipRenderer = new TooltipRenderer();
    private float viewportWidth;
    private float viewportHeight;
    private boolean layoutDirty = true;
    private MaterialDialog activeDialog;

    public View() {
        this.rootNode = new YogaNode();
        this.rootNode.setDebugName("ViewRoot");
        this.rootNode.setFlexDirection(YogaFlexDirection.ROW);
    }

    public YogaNode getRootNode() {
        return rootNode;
    }

    public void setViewport(float width, float height) {
        if (this.viewportWidth != width || this.viewportHeight != height) {
            this.viewportWidth = width;
            this.viewportHeight = height;
            markLayoutDirty();
        }
    }

    public Rectangle getViewport() {
        return new Rectangle(0, 0, viewportWidth, viewportHeight);
    }

    public YogaNode addFrame(Frame frame) {
        YogaNode layoutNode = new YogaNode();
        layoutNode.setDebugName("Frame_" + frames.size());

        FrameEntry entry = new FrameEntry(frame, layoutNode);
        frames.add(entry);
        rootNode.addChildAt(layoutNode, rootNode.getChildCount());
        markLayoutDirty();
        return layoutNode;
    }

    public void removeFrame(Frame frame) {
        for (int i = 0; i < frames.size(); i++) {
            FrameEntry entry = frames.get(i);
            if (entry.frame == frame) {
                frames.remove(i);
                rootNode.removeChild(entry.layoutNode);
                markLayoutDirty();
                return;
            }
        }
    }

    public List<Frame> getFrames() {
        return frames.stream().map(e -> e.frame).toList();
    }

    public void setFrameRenderAlpha(Frame frame, float alpha) {
        FrameEntry entry = findFrameEntry(frame);
        if (entry == null) {
            return;
        }
        entry.renderAlpha = Math.max(0f, Math.min(1f, alpha));
    }

    public void setFrameRenderOffsetY(Frame frame, float offsetY) {
        FrameEntry entry = findFrameEntry(frame);
        if (entry == null) {
            return;
        }
        entry.renderOffsetY = offsetY;
    }

    public void resetFrameRenderState(Frame frame) {
        FrameEntry entry = findFrameEntry(frame);
        if (entry == null) {
            return;
        }
        entry.renderAlpha = 1f;
        entry.renderOffsetY = 0f;
    }

    public void markLayoutDirty() {
        this.layoutDirty = true;
    }

    public void calculateLayout() {
        if (layoutDirty) {
            rootNode.setWidth(viewportWidth);
            rootNode.setHeight(viewportHeight);
            rootNode.calculateLayout(viewportWidth, viewportHeight);
            /*CaptureTree.calculateLayoutWithCapture(
                    rootNode,
                    viewportWidth,
                    viewportHeight,
                    rootNode.getStyle().getDirection(),
                    Path.of("test.view.json")
            );*/
            for (FrameEntry entry : frames) {
                YogaNode node = entry.layoutNode;
                float x = node.getLayoutX();
                float y = node.getLayoutY();
                float width = node.getLayoutWidth();
                float height = node.getLayoutHeight();

                entry.frame.setViewport(width, height);
                entry.frame.setPosition(x, y);
            }

            layoutDirty = false;
        }

        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing())) {
            activeDialog.calculateLayout(viewportWidth, viewportHeight);
        }
    }

    public void render(RenderContext ctx, UIInputState inputState) {
        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing())) {
            activeDialog.layouting(ctx);
        }

        calculateLayout();

        for (FrameEntry entry : frames) {
            YogaNode node = entry.layoutNode;
            float x = node.getLayoutX();
            float y = node.getLayoutY();

            ctx.save();
            ctx.translate(x, y + entry.renderOffsetY);
            ctx.pushAlpha(entry.renderAlpha);

            Vector2f frameLocal = new Vector2f(
                    inputState.mousePosition().x - entry.layoutNode.getLayoutX(),
                    inputState.mousePosition().y - entry.layoutNode.getLayoutY() - entry.renderOffsetY
            );
            Vector2f contentPos = entry.frame.screenToContent(frameLocal.x, frameLocal.y);
            entry.frame.render(ctx, new UIInputState(
                    contentPos,
                    inputState.frameTime()
            ));
            ctx.popAlpha();
            ctx.restore();
        }

        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing())) {
            activeDialog.render(ctx, inputState);
        }

        Optional<Tooltip> tooltip = collectTooltip(inputState.mousePosition());

        ctx.deferToLayer(RenderLayer.Tooltip, 114514, (cute) -> {
            renderTooltip(cute, inputState, tooltip);
        });
    }

    public void showDialog(MaterialDialog dialog) {
        this.activeDialog = dialog;
        dialog.show(this);
    }

    public void dismissDialog() {
        if (activeDialog != null) {
            activeDialog.dismiss();
            activeDialog = null;
        }
    }

    public MaterialDialog getActiveDialog() {
        return activeDialog;
    }

    public void onDialogDismissed(MaterialDialog dialog) {
        if (activeDialog == dialog) {
            activeDialog = null;
        }
    }

    public void dispatchMouseMove(float x, float y) {
        AbstractWidget<?> hoveredWidget = findTopHoveredWidget();
        if (hoveredWidget != null && hoveredWidget != activeDialog) {
            MouseCursor.HAND.use();
        } else {
            MouseCursor.ARROW.use();
        }
        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing()) && activeDialog.handleMouseMove(x, y)) {
            return;
        }
        for (FrameEntry entry : frames) {
            YogaNode node = entry.layoutNode;
            float frameX = node.getLayoutX();
            float frameY = node.getLayoutY();

            entry.frame.dispatchMouseMove(x - frameX, y - frameY);

        }
    }

    public void dispatchMousePress(float x, float y, int button) {
        if (activeDialog != null) {
            activeDialog.handleMousePress(x, y, button);
            if (!activeDialog.isFadeIn()) {
                return;
            }
        }
        for (int i = frames.size() - 1; i >= 0; i--) {
            FrameEntry entry = frames.get(i);
            YogaNode node = entry.layoutNode;
            float frameX = node.getLayoutX();
            float frameY = node.getLayoutY();
            float frameWidth = node.getLayoutWidth();
            float frameHeight = node.getLayoutHeight();

            if (x >= frameX && x < frameX + frameWidth && y >= frameY && y < frameY + frameHeight) {
                entry.frame.dispatchMousePress(x - frameX, y - frameY, button);
                return;
            }
        }
    }

    public void dispatchMouseRelease(float x, float y, int button) {
        if (activeDialog != null) {
            activeDialog.handleMouseRelease(x, y, button);
            if (!activeDialog.isFadeIn()) {
                return;
            }
        }
        for (FrameEntry entry : frames) {
            YogaNode node = entry.layoutNode;
            float frameX = node.getLayoutX();
            float frameY = node.getLayoutY();

            entry.frame.dispatchMouseRelease(x - frameX, y - frameY, button);
        }
    }

    public void dispatchMouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing()) && activeDialog.handleMouseDrag(mouseX, mouseY, dragX, dragY, button)) {
            return;
        }
        for (FrameEntry entry : frames) {
            YogaNode node = entry.layoutNode;
            float frameX = node.getLayoutX();
            float frameY = node.getLayoutY();

            entry.frame.dispatchMouseDrag(mouseX - frameX, mouseY - frameY, dragX, dragY, button);
        }
    }

    public void dispatchMouseScroll(float x, float y, double scrollX) {
        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing()) && activeDialog.handleMouseScroll(x, y, scrollX)) {
            return;
        }
        for (int i = frames.size() - 1; i >= 0; i--) {
            FrameEntry entry = frames.get(i);
            YogaNode node = entry.layoutNode;
            float frameX = node.getLayoutX();
            float frameY = node.getLayoutY();
            float frameWidth = node.getLayoutWidth();
            float frameHeight = node.getLayoutHeight();

            if (x >= frameX && x < frameX + frameWidth && y >= frameY && y < frameY + frameHeight) {
                entry.frame.dispatchMouseScroll(x - frameX, y - frameY, scrollX);
                return;
            }
        }
    }

    public void dispatchKeyPress(int keyCode, int scancode, int modifiers) {
        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing()) && activeDialog.handleKeyPress(keyCode, scancode, modifiers)) {
            return;
        }
        for (FrameEntry entry : frames) {
            entry.frame.dispatchKeyPress(keyCode, scancode, modifiers);
        }
    }

    public void dispatchKeyRelease(int keyCode, int scancode, int modifiers) {
        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing()) && activeDialog.handleKeyRelease(keyCode, scancode, modifiers)) {
            return;
        }
        for (FrameEntry entry : frames) {
            entry.frame.dispatchKeyRelease(keyCode, scancode, modifiers);
        }
    }

    public void dispatchCharTyped(char codePoint, int modifiers) {
        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing()) && activeDialog.handleCharTyped(codePoint, modifiers)) {
            return;
        }
        for (FrameEntry entry : frames) {
            entry.frame.dispatchCharTyped(codePoint, modifiers);
        }
    }

    public void setDebugRenderEnabled(boolean enabled) {
        frames.forEach((frameEntry -> frameEntry.frame.setDebugRenderEnabled(enabled)));
    }

    public void setDebugBoundsVisible(boolean layout, boolean render, boolean hitTest) {
        frames.forEach((frameEntry -> frameEntry.frame.setDebugBoundsVisible(layout, render, hitTest)));

    }

    private AbstractWidget<?> findTopHoveredWidgetInTreeAllowDisabledWidget(AbstractWidget<?> widget, Vector2f mousePosition) {
        if (widget == null || !widget.isVisible()) {
            return null;
        }


        if (widget instanceof ILayoutContainer container) {
            List<ILayoutElement> children = container.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                ILayoutElement child = children.get(i);
                if (child instanceof AbstractWidget<?> childWidget) {
                    AbstractWidget<?> found = findTopHoveredWidgetInTreeAllowDisabledWidget(childWidget, mousePosition);
                    if (found != null && found.checkInteractive()) {
                        if (!found.hitTest(mousePosition)) {
                            return null;
                        }
                        return found;
                    }
                }
            }
        }

        return widget.checkInteractive() ? widget : null;
    }

    private AbstractWidget<?> findTopHoveredWidgetInTree(AbstractWidget<?> widget) {
        if (widget == null || !widget.isVisible() || widget.isDisabled()) {
            return null;
        }

        if (widget instanceof ILayoutContainer container) {
            List<ILayoutElement> children = container.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                ILayoutElement child = children.get(i);
                if (child instanceof AbstractWidget<?> childWidget) {
                    AbstractWidget<?> found = findTopHoveredWidgetInTree(childWidget);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }

        return widget.isHovered() ? widget : null;
    }

    private AbstractWidget<?> findTopHoveredWidget() {
        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing())) {
            AbstractWidget<?> dialogHovered = findTopHoveredWidgetInTree(activeDialog);
            return dialogHovered;
        }

        for (int i = frames.size() - 1; i >= 0; i--) {
            FrameEntry entry = frames.get(i);
            AbstractWidget<?> root = entry.frame.getRoot();
            if (root == null || !root.isVisible() || root.isDisabled()) {
                continue;
            }
            AbstractWidget<?> hovered = findTopHoveredWidgetInTree(root);
            if (hovered != null) {
                return hovered;
            }
        }
        return null;
    }

    private Optional<Tooltip> collectTooltip(Vector2f mousePosition) {
        if (activeDialog != null && (activeDialog.isShowing() || activeDialog.isDismissing())) {
            AbstractWidget<?> dialogHovered = findTopHoveredWidgetInTreeAllowDisabledWidget(activeDialog, mousePosition);
            if (dialogHovered != null) {
                return dialogHovered.getTooltip();
            } else {
                return Optional.empty();
            }
        }

        for (int i = frames.size() - 1; i >= 0; i--) {
            FrameEntry entry = frames.get(i);
            AbstractWidget<?> root = entry.frame.getRoot();
            if (root == null || !root.isVisible()) {
                continue;
            }
            Vector2f frameLocal = new Vector2f(
                    mousePosition.x - entry.layoutNode.getLayoutX(),
                    mousePosition.y - entry.layoutNode.getLayoutY() - entry.renderOffsetY
            );
            Vector2f contentPos = entry.frame.screenToContent(frameLocal.x, frameLocal.y);
            AbstractWidget<?> hovered = root.hitWidget(contentPos, (w) -> false);
            if (hovered != null) {
                SuperResolution.LOGGER.info(hovered.toString());
                return hovered.getTooltip();
            }
        }
        return Optional.empty();
    }

    private void renderTooltip(RenderContext ctx, UIInputState inputState, Optional<Tooltip> tooltip) {
        tooltipRenderer.render(ctx, inputState, tooltip.orElse(Tooltip.empty()));
    }

    private void renderDebugLayoutBounds(RenderContext ctx, AbstractWidget<?> widget) {
        if (widget == null || !widget.isVisible()) {
            return;
        }

        Rectangle layoutBounds = widget.getBounds();

        ctx.save();
        ctx.strokeWidth(2);
        ctx.rect(
                layoutBounds.x, layoutBounds.y,
                layoutBounds.width, layoutBounds.height,
                Color.rgba(255, 255, 255, 200), false);

        ctx.restore();

        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    renderDebugLayoutBounds(ctx, childWidget);
                }
            }
        }
    }

    private FrameEntry findFrameEntry(Frame frame) {
        for (FrameEntry entry : frames) {
            if (entry.frame == frame) {
                return entry;
            }
        }
        return null;
    }

    private static class FrameEntry {
        final Frame frame;
        final YogaNode layoutNode;
        float renderAlpha;
        float renderOffsetY;

        FrameEntry(Frame frame, YogaNode layoutNode) {
            this.frame = frame;
            this.layoutNode = layoutNode;
            this.renderAlpha = 1f;
            this.renderOffsetY = 0f;
        }
    }
}
