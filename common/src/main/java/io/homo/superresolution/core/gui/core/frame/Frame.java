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

package io.homo.superresolution.core.gui.core.frame;

import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.ILayoutContainer;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenu;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPositionType;
import org.joml.Vector2f;

import java.util.*;

public class Frame implements IFrame {
    private static final Color DEBUG_LAYOUT_COLOR = Color.rgb(0, 120, 255);
    private static final Color DEBUG_RENDER_COLOR = Color.rgb(255, 50, 50);
    private static final Color DEBUG_HITTEST_COLOR = Color.rgb(255, 220, 0);
    private static final float DEBUG_STROKE_WIDTH = 1.0f;
    private final List<RenderEntry> renderList = new ArrayList<>();
    private AbstractWidget<?> root;
    private float viewportWidth;
    private float viewportHeight;
    private float positionX = 0;
    private float positionY = 0;
    private boolean layoutDirty = true;

    public void setPosition(float x, float y) {
        this.positionX = x;
        this.positionY = y;
    }

    public Vector2f getPosition() {
        return new Vector2f(positionX, positionY);
    }

    private void collectRenderables(AbstractWidget<?> widget, List<RenderEntry> list) {
        if (!widget.isVisible()) {
            return;
        }

        Transform accumulatedTransform = widget.getFullTransform();

        list.add(new RenderEntry(widget, accumulatedTransform, widget.getZIndex()));

        if (widget.managesChildRendering()) {
            return;
        }

        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    collectRenderables(childWidget, list);
                }
            }
        }
    }

    private Rectangle transformBounds(Rectangle bounds, Transform transform) {
        if (transform.isIdentity()) {
            return bounds;
        }

        Vector2f topLeft = transform.transformPoint(new Vector2f(bounds.x, bounds.y));
        Vector2f topRight = transform.transformPoint(new Vector2f(bounds.x + bounds.width, bounds.y));
        Vector2f bottomLeft = transform.transformPoint(new Vector2f(bounds.x, bounds.y + bounds.height));
        Vector2f bottomRight = transform
                .transformPoint(new Vector2f(bounds.x + bounds.width, bounds.y + bounds.height));

        float minX = Math.min(Math.min(topLeft.x, topRight.x), Math.min(bottomLeft.x, bottomRight.x));
        float minY = Math.min(Math.min(topLeft.y, topRight.y), Math.min(bottomLeft.y, bottomRight.y));
        float maxX = Math.max(Math.max(topLeft.x, topRight.x), Math.max(bottomLeft.x, bottomRight.x));
        float maxY = Math.max(Math.max(topLeft.y, topRight.y), Math.max(bottomLeft.y, bottomRight.y));

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private boolean isInViewport(Rectangle bounds) {
        return bounds.x + bounds.width > 0 &&
                bounds.y + bounds.height > 0 &&
                bounds.x < viewportWidth &&
                bounds.y < viewportHeight;
    }

    @Override
    public AbstractWidget<?> getRoot() {
        return root;
    }

    private void renderWidget(RenderContext ctx, UIInputState inputState, RenderEntry entry) {
        AbstractWidget<?> widget = entry.widget();
        Transform transform = entry.accumulatedTransform();

        ctx.save();
        ctx.applyTransform(transform);
        widget.render(ctx, inputState);
        ctx.restore();
    }

    private void collectAncestorChain(AbstractWidget<?> widget, Set<AbstractWidget<?>> chain) {
        ILayoutElement current = widget;
        while (current != null) {
            if (current instanceof AbstractWidget<?> w) {
                chain.add(w);
            }
            current = current.getParent();
        }
    }

    private void dispatchMouseMoveRecursive(AbstractWidget<?> widget, float x, float y,
                                            AbstractWidget<?> topInteractive,
                                            Set<AbstractWidget<?>> ancestorChain) {
        if (!widget.isVisible() || widget.isDisabled()) {
            return;
        }

        Transform accumulatedTransform = widget.getFullTransform();
        Vector2f localPos = accumulatedTransform.inverseTransformPoint(new Vector2f(x, y));

        boolean shouldReceiveEvent = ancestorChain.contains(widget);

        if (shouldReceiveEvent) {
            widget.mouseMove(localPos.x, localPos.y);

            if (widget.managesChildEvents()) {
                return;
            }
        } else {
            if (widget.isHovered()) {
                widget.clearHover();
            }

        }

        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    dispatchMouseMoveRecursive(childWidget, x, y, topInteractive, ancestorChain);
                }
            }
        }
    }

    private void dispatchMouseReleaseRecursive(AbstractWidget<?> widget, float x, float y, int button) {
        if (!widget.isVisible() || widget.isDisabled()) {
            return;
        }

        Transform accumulatedTransform = widget.getFullTransform();
        Vector2f localPos = accumulatedTransform.inverseTransformPoint(new Vector2f(x, y));

        widget.mouseRelease(localPos.x, localPos.y, button);

        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    dispatchMouseReleaseRecursive(childWidget, x, y, button);
                }
            }
        }
    }

    private void dispatchMouseDragRecursive(AbstractWidget<?> widget, float mouseX, float mouseY,
                                            float dragX, float dragY, int button) {
        if (!widget.isVisible() || widget.isDisabled()) {
            return;
        }

        Transform accumulatedTransform = widget.getFullTransform();
        Vector2f localPos = accumulatedTransform.inverseTransformPoint(new Vector2f(mouseX, mouseY));

        Vector2f localDrag = transformDelta(accumulatedTransform, dragX, dragY);

        widget.mouseDrag(localPos.x, localPos.y, localDrag.x, localDrag.y, button);

        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    dispatchMouseDragRecursive(childWidget, mouseX, mouseY, dragX, dragY, button);
                }
            }
        }
    }

    @Override
    public void setRoot(AbstractWidget<?> root) {
        this.root = root;
        markLayoutDirty();
    }

    private Vector2f transformDelta(Transform transform, float dx, float dy) {
        if (transform.isIdentity()) {
            return new Vector2f(dx, dy);
        }

        Vector2f origin = transform.inverseTransformPoint(new Vector2f(0, 0));
        Vector2f delta = transform.inverseTransformPoint(new Vector2f(dx, dy));
        return new Vector2f(delta.x - origin.x, delta.y - origin.y);
    }

    private void dispatchKeyPressRecursive(AbstractWidget<?> widget, int keyCode, int scancode, int modifiers) {
        if (!widget.isVisible() || widget.isDisabled()) {
            return;
        }

        widget.keyPress(keyCode, scancode, modifiers);

        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    dispatchKeyPressRecursive(childWidget, keyCode, scancode, modifiers);
                }
            }
        }
    }

    private void dispatchKeyReleaseRecursive(AbstractWidget<?> widget, int keyCode, int scancode, int modifiers) {
        if (!widget.isVisible() || widget.isDisabled()) {
            return;
        }

        widget.keyRelease(keyCode, scancode, modifiers);

        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    dispatchKeyReleaseRecursive(childWidget, keyCode, scancode, modifiers);
                }
            }
        }
    }

    private void dispatchCharTypedRecursive(AbstractWidget<?> widget, char codePoint, int modifiers) {
        if (!widget.isVisible() || widget.isDisabled()) {
            return;
        }

        widget.charTyped(codePoint, modifiers);

        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    dispatchCharTypedRecursive(childWidget, codePoint, modifiers);
                }
            }
        }
    }

    private AbstractWidget<?> findFloatingWidgetRecursive(AbstractWidget<?> widget, Vector2f pos) {
        if (!widget.isVisible() || widget.isDisabled()) {
            return null;
        }

        if (widget.isFloatingWidget() && widget.hitTest(pos)) {
            AbstractWidget<?> found = widget.findInteractiveWidgetAt(pos);
            if (found != null) {
                return found;
            }
        }

        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    AbstractWidget<?> found = findFloatingWidgetRecursive(childWidget, pos);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void setViewport(float width, float height) {
        if (this.viewportWidth != width || this.viewportHeight != height) {
            this.viewportWidth = width;
            this.viewportHeight = height;
            markLayoutDirty();
        }
    }

    private AbstractWidget<?> findInteractiveWidgetAtRecursive(
            AbstractWidget<?> widget,
            Vector2f absPos,
            AbstractWidget<?> currentBest,
            int currentBestZIndex) {

        if (!widget.isVisible() || widget.isDisabled()) {
            return currentBest;
        }

        Transform accumulatedTransform = widget.getFullTransform();

        Vector2f localPos = accumulatedTransform.inverseTransformPoint(absPos);

        boolean hit = widget.hitTest(absPos);

        if (hit) {
            if (widget.managesChildEvents()) {
                AbstractWidget<?> found = widget.findInteractiveWidgetAt(absPos);
                if (found != null) {
                    int foundZIndex = found.getZIndex();
                    if (foundZIndex >= currentBestZIndex) {
                        currentBest = found;
                        currentBestZIndex = foundZIndex;
                    }
                }
            } else {
                if (widget instanceof ILayoutContainer container) {
                    List<ILayoutElement> children = container.getChildren();
                    for (int i = children.size() - 1; i >= 0; i--) {
                        ILayoutElement child = children.get(i);
                        if (child instanceof AbstractWidget<?> childWidget) {
                            AbstractWidget<?> found = findInteractiveWidgetAtRecursive(
                                    childWidget, absPos, currentBest, currentBestZIndex);
                            if (found != null && found != currentBest) {
                                int foundZIndex = found.getZIndex();
                                if (foundZIndex >= currentBestZIndex) {
                                    currentBest = found;
                                    currentBestZIndex = foundZIndex;
                                }
                            }
                        }
                    }
                }

                if (widget.checkInteractive()) {
                    int widgetZIndex = widget.getZIndex();
                    if (widgetZIndex >= currentBestZIndex) {
                        currentBest = widget;
                        currentBestZIndex = widgetZIndex;
                    }
                }
            }
        }

        return currentBest;
    }

    private Transform calculateAccumulatedTransform(AbstractWidget<?> widget) {
        return widget.getFullTransform();
    }

    @Deprecated
    public void setDebugRenderEnabled(boolean enabled) {
    }

    @Deprecated
    public void setDebugBoundsVisible(boolean layout, boolean render, boolean hitTest) {

    }

    protected void layoutWidgets(AbstractWidget<?> widget, RenderContext ctx) {
        widget.layouting(ctx);

        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    layoutWidgets(childWidget, ctx);
                }
            }
        }
    }

    @Override
    public Rectangle getViewport() {
        return new Rectangle(0, 0, viewportWidth, viewportHeight);
    }

    public void updateHitTestDebug(Vector2f mousePos) {
    }

    private record RenderEntry(AbstractWidget<?> widget,

                               Transform accumulatedTransform,

                               int zIndex) {
    }

    @Override
    public void calculateLayout() {
        if (root == null) {
            return;
        }

        root.getLayoutNode().setWidth(viewportWidth);
        root.getLayoutNode().setHeight(viewportHeight);
        root.getLayoutNode().setPosition(YogaEdge.LEFT, 0);
        root.getLayoutNode().setPosition(YogaEdge.TOP, 0);
        root.getLayoutNode().setPositionType(YogaPositionType.ABSOLUTE);
        root.getLayoutNode().calculateLayout(viewportWidth, viewportHeight);

        layoutDirty = false;
    }


    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        if (root == null) {
            return;
        }

        layoutWidgets(root, ctx);

        calculateLayout();

        renderList.clear();
        collectRenderables(root, renderList);
        renderList.sort(Comparator.comparingInt(RenderEntry::zIndex));
        for (RenderEntry entry : renderList) {
            renderWidget(ctx, inputState, entry);
        }
    }


    @Override
    public void dispatchMouseMove(float x, float y) {
        if (root == null || !root.isVisible()) {
            return;
        }

        Vector2f mousePos = new Vector2f(x, y);

        AbstractWidget<?> topInteractive = findInteractiveWidgetAt(mousePos);

        Set<AbstractWidget<?>> ancestorChain = new HashSet<>();
        if (topInteractive != null) {
            collectAncestorChain(topInteractive, ancestorChain);
        }

        dispatchMouseMoveRecursive(root, x, y, topInteractive, ancestorChain);
    }

    @Override
    public void dispatchMousePress(float x, float y, int button) {
        if (root == null || !root.isVisible()) {
            return;
        }

        Vector2f mousePos = new Vector2f(x, y);

        AbstractWidget<?> topInteractive = findInteractiveWidgetAt(mousePos);

        if (topInteractive != null) {
            Transform accumulatedTransform = calculateAccumulatedTransform(topInteractive);
            Vector2f localPos = accumulatedTransform.inverseTransformPoint(mousePos);
            topInteractive.mousePress(localPos.x, localPos.y, button);
        }

    }

    @Override
    public void dispatchMouseRelease(float x, float y, int button) {
        if (root == null || !root.isVisible()) {
            return;
        }

        dispatchMouseReleaseRecursive(root, x, y, button);
    }

    @Override
    public void dispatchMouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        if (root == null || !root.isVisible()) {
            return;
        }

        dispatchMouseDragRecursive(root, mouseX, mouseY, dragX, dragY, button);
    }

    @Override
    public void dispatchMouseScroll(float x, float y, double scrollX) {
        if (root == null || !root.isVisible()) {
            return;
        }

        Vector2f mousePos = new Vector2f(x, y);

        AbstractWidget<?> topInteractive = findInteractiveWidgetAt(mousePos);

        if (topInteractive != null) {
            Transform accumulatedTransform = calculateAccumulatedTransform(topInteractive);
            Vector2f localPos = accumulatedTransform.inverseTransformPoint(mousePos);
            topInteractive.mouseScroll(localPos.x, localPos.y, scrollX);
        }
    }

    @Override
    public void dispatchKeyPress(int keyCode, int scancode, int modifiers) {
        if (root == null || !root.isVisible()) {
            return;
        }

        dispatchKeyPressRecursive(root, keyCode, scancode, modifiers);
    }

    @Override
    public void dispatchKeyRelease(int keyCode, int scancode, int modifiers) {
        if (root == null || !root.isVisible()) {
            return;
        }

        dispatchKeyReleaseRecursive(root, keyCode, scancode, modifiers);
    }

    @Override
    public void dispatchCharTyped(char codePoint, int modifiers) {
        if (root == null || !root.isVisible()) {
            return;
        }

        dispatchCharTypedRecursive(root, codePoint, modifiers);
    }

    @Override
    public AbstractWidget<?> findInteractiveWidgetAt(Vector2f pos) {
        if (root == null || !root.isVisible()) {
            return null;
        }

        AbstractWidget<?> floatingWidget = findFloatingWidgetRecursive(root, pos);
        if (floatingWidget != null) {
            return floatingWidget;
        }

        return findInteractiveWidgetAtRecursive(root, pos, null, Integer.MIN_VALUE);
    }

    @Override
    public void markLayoutDirty() {
        layoutDirty = true;
    }

    @Override
    public boolean isLayoutDirty() {
        return layoutDirty;
    }

}
