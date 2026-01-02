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

import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class View {
    private final List<FrameEntry> frames = new ArrayList<>();

    public YogaNode getRootNode() {
        return rootNode;
    }

    private final YogaNode rootNode;

    private float viewportWidth;
    private float viewportHeight;
    private boolean layoutDirty = true;

    private static class FrameEntry {
        final Frame frame;
        final YogaNode layoutNode;

        FrameEntry(Frame frame, YogaNode layoutNode) {
            this.frame = frame;
            this.layoutNode = layoutNode;
        }
    }

    public View() {
        this.rootNode = new YogaNode();
        this.rootNode.setDebugName("ViewRoot");
        this.rootNode.setFlexDirection(YogaFlexDirection.ROW);
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

    public void markLayoutDirty() {
        this.layoutDirty = true;
    }

    public void calculateLayout() {
        if (!layoutDirty) return;

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

    public void render(RenderContext ctx, UIInputState inputState) {
        if (layoutDirty) {
            calculateLayout();
        }

        for (FrameEntry entry : frames) {
            YogaNode node = entry.layoutNode;
            float x = node.getLayoutX();
            float y = node.getLayoutY();
            
            ctx.save();
            ctx.translate(x, y);

            entry.frame.render(ctx, inputState);

            ctx.restore();
        }
    }

    public void dispatchMouseMove(float x, float y) {
        for (FrameEntry entry : frames) {
            YogaNode node = entry.layoutNode;
            float frameX = node.getLayoutX();
            float frameY = node.getLayoutY();
            float frameWidth = node.getLayoutWidth();
            float frameHeight = node.getLayoutHeight();

            if (x >= frameX && x < frameX + frameWidth && y >= frameY && y < frameY + frameHeight) {
                entry.frame.dispatchMouseMove(x - frameX, y - frameY);
            } else {
                //entry.frame.dispatchMouseMove(-10000, -10000);
            }
        }
    }

    public void dispatchMousePress(float x, float y, int button) {
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
        for (FrameEntry entry : frames) {
            YogaNode node = entry.layoutNode;
            float frameX = node.getLayoutX();
            float frameY = node.getLayoutY();

            entry.frame.dispatchMouseRelease(x - frameX, y - frameY, button);
        }
    }

    public void dispatchMouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        for (FrameEntry entry : frames) {
            YogaNode node = entry.layoutNode;
            float frameX = node.getLayoutX();
            float frameY = node.getLayoutY();

            entry.frame.dispatchMouseDrag(mouseX - frameX, mouseY - frameY, dragX, dragY, button);
        }
    }

    public void dispatchMouseScroll(float x, float y, double scrollX) {
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
        for (FrameEntry entry : frames) {
            entry.frame.dispatchKeyPress(keyCode, scancode, modifiers);
        }
    }

    public void dispatchKeyRelease(int keyCode, int scancode, int modifiers) {
        for (FrameEntry entry : frames) {
            entry.frame.dispatchKeyRelease(keyCode, scancode, modifiers);
        }
    }

    public void dispatchCharTyped(char codePoint, int modifiers) {
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
}
