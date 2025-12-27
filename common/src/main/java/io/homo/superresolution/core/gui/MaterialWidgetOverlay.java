/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.event.events.MouseEvent;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.gui.core.animator.BezierInterpolator;
import io.homo.superresolution.core.gui.core.animator.Animator;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Vector2f;

public abstract class MaterialWidgetOverlay<T extends AbstractWidget<?>> {
    private final MaterialRipple ripple;
    private Animator.FloatAnimator hoverAnimator;
    private boolean shouldFadeOutAfterRipple = false;
    private boolean isHovered = false;
    private Vector2f lastPressPosition = null;

    private long hoverEnterDuration = 200;
    private long hoverExitDuration = 200;
    private float hoverNormalAlpha = 0.08f;
    private float hoverPressedAlpha = 0.10f;

    private final T widget;

    public MaterialWidgetOverlay(T widget) {
        this.ripple = new MaterialRipple();
        this.widget = widget;
        initAnimators();
        this.widget.getEventBus().addListener(this::onMouseMove);
        this.widget.getEventBus().addListener(this::onMousePress);
        this.widget.getEventBus().addListener(this::onMouseRelease);
    }

    private void initAnimators() {
        hoverAnimator = new Animator.FloatAnimator(0f, 0f);
        hoverAnimator.duration(hoverEnterDuration);
        hoverAnimator.timeInterpolator(new BezierInterpolator(0.2f, 0, 0, 1));
    }

    protected abstract void drawShape(IUIDrawContext drawContext, T widget, Color color);

    protected abstract void drawShape(IUIDrawContext drawContext, T widget, IPaint paint);

    public void render(IUIDrawContext drawContext, Color hoverColor, Color rippleColor) {
        renderHoverOverlay(drawContext, hoverColor);
        renderRippleOverlay(drawContext, rippleColor);
    }

    public void update() {
        if (hoverAnimator != null) hoverAnimator.update();
        ripple.update();
        checkDeferredFadeOut();
    }

    public void renderHoverOverlay(IUIDrawContext drawContext, Color hoverColor) {
        if (shouldRenderHover()) {
            float currentAlpha = hoverAnimator.get();
            if (currentAlpha > 0.001f) {
                Color overlayColor = hoverColor.copy().alpha((int) (255 * currentAlpha * hoverNormalAlpha));
                drawShape(drawContext, widget, overlayColor);
            }
        }
    }

    public void renderRippleOverlay(IUIDrawContext drawContext, Color rippleColor) {
        if (shouldRenderRipple()) {
            IPaint[] ripplePaints = ripple.getPaints(
                    rippleColor,
                    drawContext,
                    widget.getBounds().getPosition(),
                    widget.getBounds().getSize()
            );
            for (IPaint paint : ripplePaints) {
                if (paint != null) {
                    drawShape(drawContext, widget, paint);
                }
            }
        }
    }

    public void onMouseMove(MouseEvent.MouseMoveEvent event) {
        boolean isHovering = widget.getBounds().in(event.getMousePosition());
        if (isHovering != this.isHovered) {
            if (isHovering) {
                onMouseEnter();
            } else {
                onMouseLeave();
            }
        }
    }

    public void onMousePress(MouseEvent.MousePressEvent event) {
        lastPressPosition = new Vector2f(event.getMousePosition());
        animateHoverTo(1.25f, hoverEnterDuration);
        ripple.setPressed(true, lastPressPosition, widget.getBounds());
    }

    public void onMouseRelease(MouseEvent.MouseReleaseEvent event) {
        if (ripple.shouldRender() && !isHovered) {
            shouldFadeOutAfterRipple = true;
        } else {
            float targetHoverValue = isHovered ? 1.0f : 0f;
            animateHoverTo(targetHoverValue, hoverExitDuration);
            shouldFadeOutAfterRipple = false;
        }

        ripple.setPressed(false, lastPressPosition, widget.getBounds());
    }

    public void onMouseEnter() {
        isHovered = true;
        shouldFadeOutAfterRipple = false;
        animateHoverTo(1.0f, hoverEnterDuration);
    }

    public void onMouseLeave() {
        isHovered = false;
        if (ripple.shouldRender()) {
            shouldFadeOutAfterRipple = true;
        } else {
            shouldFadeOutAfterRipple = false;
            animateHoverTo(0f, hoverExitDuration);
        }
    }


    private void checkDeferredFadeOut() {
        if (shouldFadeOutAfterRipple && !ripple.shouldRender() && !isHovered) {
            shouldFadeOutAfterRipple = false;
            animateHoverTo(0f, hoverExitDuration);
        }
    }

    private void animateHoverTo(float targetValue, long duration) {
        if (hoverAnimator.isRunning()) {
            hoverAnimator.cancel();
        }

        hoverAnimator.fromTo(
                hoverAnimator.get(),
                targetValue
        );
        hoverAnimator.duration(duration);
        hoverAnimator.start();
    }

    private boolean shouldRenderHover() {
        return hoverAnimator.isRunning() || hoverAnimator.get() > 0.001f;
    }

    private boolean shouldRenderRipple() {
        return ripple.shouldRender() || ripple.isPressed();
    }

    public float getHoverProgress() {
        return hoverAnimator.get();
    }

    public MaterialRipple getRipple() {
        return ripple;
    }

    public MaterialWidgetOverlay setHoverEnterDuration(long duration) {
        this.hoverEnterDuration = duration;
        return this;
    }

    public MaterialWidgetOverlay setHoverExitDuration(long duration) {
        this.hoverExitDuration = duration;
        return this;
    }

    public MaterialWidgetOverlay setHoverNormalAlpha(float alpha) {
        this.hoverNormalAlpha = alpha;
        return this;
    }

    public MaterialWidgetOverlay setHoverPressedAlpha(float alpha) {
        this.hoverPressedAlpha = alpha;
        return this;
    }

    public void destroy() {
        if (hoverAnimator != null) {
            if (hoverAnimator.isRunning()) {
                hoverAnimator.cancel();
            }
            hoverAnimator = null;
        }

        if (ripple != null) {
            ripple.destroy();
        }
    }

    public void reset() {
        shouldFadeOutAfterRipple = false;
        isHovered = false;
        lastPressPosition = null;

        if (hoverAnimator != null && hoverAnimator.isRunning()) {
            hoverAnimator.cancel();
        }

        if (ripple != null) {
            ripple.clearAllRipples();
        }
    }
}