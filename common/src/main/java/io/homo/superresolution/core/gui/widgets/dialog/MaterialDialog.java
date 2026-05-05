/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.gui.widgets.dialog;

import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.ILayoutContainer;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.core.view.View;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

//TODO:抽象成一个Dialog接口
public class MaterialDialog extends MaterialContainerWidget<MaterialDialog> {
    private static final long FADE_IN_DURATION = 150;
    private static final long FADE_OUT_DURATION = 120;
    private final List<DialogAction> actions = new ArrayList<>();
    private final List<MaterialButton> actionButtons = new ArrayList<>();
    private MaterialSymbol icon;
    private String headline;
    private String supportingText;
    private boolean showDivider = false;
    private AbstractWidget<?> contentWidget;
    private View parentView;
    private boolean showing = false;
    private boolean isFadeIn = false;
    private Consumer<MaterialDialog> onDismiss;
    private boolean built = false;
    private AbstractWidget<?> hoveredInDialog;
    private float dialogX, dialogY;
    private YogaNode layoutWrapper;
    private Animator.FloatAnimator fadeAnimator;
    private boolean dismissPending = false;

    public MaterialDialog() {
        this.style = new MaterialDialogStyle();
        getLayoutNode().setDebugName("MaterialDialog");
    }

    public static MaterialDialog create() {
        return new MaterialDialog();
    }

    @Override
    protected void init() {
    }

    @Override
    public void layouting(RenderContext ctx) {
        super.layouting(ctx);
        for (ILayoutElement child : this.getChildren()) {
            if (child instanceof AbstractWidget<?> childWidget) {
                childWidget.layouting(ctx);
            }
        }
        this.prepareLayout(ctx, ctx.viewportWidth(), ctx.viewportHeight());
        //TODO:如果只计算一次布局，那么Label会莫名其妙错位一次，解决方法没找到，这就当临时的解决方法
        this.prepareLayout(ctx, ctx.viewportWidth(), ctx.viewportHeight());
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        if (!showing && !dismissPending) {
            return;
        }

        if (fadeAnimator != null) {
            fadeAnimator.update();
        }
        float alpha = fadeAnimator != null ? fadeAnimator.get() : 1f;
        float scale = 0.6f + 0.4f * alpha;
        ctx.pushAlpha(alpha);
        float vpW = ctx.viewportWidth();
        float vpH = ctx.viewportHeight();
        ctx.rect(0, 0, vpW, vpH, style().scrimColor(), true);
        ctx.pushTransform();
        float centerX = vpW / 2f;
        float centerY = vpH / 2f;
        ctx.translate(centerX, centerY);
        ctx.scale(scale, scale);
        ctx.translate(-centerX, -centerY);
        renderSelf(ctx, inputState);
        for (ILayoutElement child : this.getChildren()) {
            if (child instanceof AbstractWidget<?> childWidget) {
                childWidget.renderWithChildren(ctx, inputState);
            }
        }
        ctx.popTransform();
        ctx.popAlpha();
    }

    @Override
    protected Rectangle getViewRegion() {
        return getBounds();
    }

    @Override
    protected void renderSelf(RenderContext ctx, UIInputState inputState) {
        ctx.save();
        Rectangle bounds = getBounds();
        ctx.roundedRect(bounds.x, bounds.y, bounds.width, bounds.height,
                style().cornerRadius(), scheme().surfaceContainerHigh(), true);

        ctx.restore();

    }

    @Override
    public MaterialDialogStyle style() {
        return (MaterialDialogStyle) style;
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    public MaterialDialog icon(MaterialSymbol icon) {
        this.icon = icon;
        return this;
    }

    public MaterialDialog headline(String headline) {
        this.headline = headline;
        return this;
    }

    public MaterialDialog supportingText(String supportingText) {
        this.supportingText = supportingText;
        return this;
    }

    public MaterialDialog divider(boolean show) {
        this.showDivider = show;
        return this;
    }

    public MaterialDialog content(AbstractWidget<?> contentWidget) {
        this.contentWidget = contentWidget;
        return this;
    }

    public MaterialDialog addAction(DialogAction action) {
        this.actions.add(action);
        return this;
    }

    public MaterialDialog addAction(String text, Consumer<MaterialDialog> onClick) {
        this.actions.add(DialogAction.of(text, onClick));
        return this;
    }

    public MaterialDialog addAction(String text,
                                    MaterialButtonVariant variant,
                                    Consumer<MaterialDialog> onClick) {
        this.actions.add(DialogAction.of(text, variant, onClick));
        return this;
    }

    public MaterialDialog onDismiss(Consumer<MaterialDialog> onDismiss) {
        this.onDismiss = onDismiss;
        return this;
    }

    public MaterialDialog scrimDismiss(boolean dismiss) {
        style().scrimDismiss(dismiss);
        return this;
    }

    public boolean isShowing() {
        return showing;
    }

    public boolean isDismissing() {
        return dismissPending;
    }

    public boolean isFadeIn() {
        return isFadeIn;
    }

    public void show(View view) {
        this.parentView = view;
        this.showing = true;
        this.isFadeIn = true;
        this.dismissPending = false;
        build();

        fadeAnimator = new Animator.FloatAnimator(0f, 1f);
        fadeAnimator.duration(FADE_IN_DURATION);
        fadeAnimator.timeInterpolator(TimeInterpolator.easeOutCubic());
        fadeAnimator.onLifecycle(new Animator.AnimatorLifecycleListener() {
            @Override
            public void onEnd() {
                isFadeIn = false;
            }
        });
        fadeAnimator.start();
    }

    public void dismiss() {
        if (dismissPending) {
            return;
        }
        dismissPending = true;

        fadeAnimator = new Animator.FloatAnimator(fadeAnimator != null ? fadeAnimator.get() : 1f, 0f);
        fadeAnimator.duration(FADE_OUT_DURATION);
        fadeAnimator.timeInterpolator(TimeInterpolator.easeInCubic());
        fadeAnimator.onLifecycle(new Animator.AnimatorLifecycleListener() {
            @Override
            public void onEnd() {
                performDismiss();
            }
        });
        fadeAnimator.start();
    }

    private void performDismiss() {
        this.showing = false;
        this.dismissPending = false;
        View view = this.parentView;
        if (onDismiss != null) {
            onDismiss.accept(this);
        }
        if (view != null) {
            view.onDialogDismissed(this);
        }
    }

    private void build() {
        if (built) {
            return;
        }
        built = true;

        MaterialDialogStyle s = style();

        layout().setFlexDirection(YogaFlexDirection.COLUMN);
        layout().setPadding(YogaEdge.ALL, s.padding());

        if (icon != null) {
            IconWidget iconWidget = new IconWidget(icon, s.iconSize());
            iconWidget.layout().setAlignSelf(YogaAlign.CENTER);
            iconWidget.layout().setMargin(YogaEdge.BOTTOM, s.sectionSpacing());
            addChild(iconWidget);
        }

        if (headline != null && !headline.isEmpty()) {
            MaterialLabel headlineLabel = MaterialLabel.create()
                    .text(headline)
                    .fontSize(s.headlineFontSize())
                    .lineHeight(s.headlineFontSize() + 4)
                    .color(scheme -> scheme.onSurface());
            if (icon != null) {
                headlineLabel.layout().setAlignSelf(YogaAlign.CENTER);
            } else {
                headlineLabel.layout().setWidthPercent(100);
            }
            headlineLabel.layout().setMargin(YogaEdge.BOTTOM, s.sectionSpacing());
            headlineLabel.style().sizeToContent(true);
            addChild(headlineLabel);
        }

        if (supportingText != null && !supportingText.isEmpty()) {
            MaterialLabel stLabel = MaterialLabel.create()
                    .text(supportingText)
                    .fontSize(s.supportingTextFontSize())
                    .lineHeight(s.supportingTextFontSize() + 4)
                    .color(scheme -> scheme.onSurfaceVariant());
            stLabel.style().wrap(true);
            stLabel.layout().setWidthPercent(100);
            stLabel.layout().setMargin(YogaEdge.BOTTOM, s.sectionSpacing());
            addChild(stLabel);
        }

        if (contentWidget != null) {
            contentWidget.layout().setMargin(YogaEdge.BOTTOM, s.sectionSpacing());
            contentWidget.layout().setWidthPercent(100);
            addChild(contentWidget);
        }

        if (showDivider) {
            DividerWidget divider = new DividerWidget(s.dividerHeight());
            divider.layout().setMargin(YogaEdge.BOTTOM, s.sectionSpacing());
            addChild(divider);
        }

        if (!actions.isEmpty()) {
            ContainerWidget actionsRow = ContainerWidget.create();
            actionsRow.layout().setFlexDirection(YogaFlexDirection.ROW);
            actionsRow.layout().setJustifyContent(YogaJustify.FLEX_END);
            actionsRow.layout().setAlignItems(YogaAlign.CENTER);
            actionsRow.layout().setWidthPercent(100);
            actionsRow.layout().setGap(YogaGutter.COLUMN, s.buttonSpacing());

            for (DialogAction action : actions) {
                MaterialButton btn = MaterialButton.create(MaterialButtonSize.Small)
                        .text(action.getText())
                        .variant(action.getVariant());
                btn.onClick(event -> action.getOnClick().accept(this));
                actionButtons.add(btn);
                actionsRow.addChild(btn);
            }

            addChild(actionsRow);
        }
    }

    private void prepareLayout(RenderContext ctx, float viewportWidth, float viewportHeight) {
        if (layoutWrapper == null) {
            layoutWrapper = new YogaNode();
            layoutWrapper.setDebugName("DialogLayoutWrapper");
            layoutWrapper.addChildAt(getLayoutNode(), 0);
        }

        MaterialDialogStyle s = style();
        float dialogWidth = Math.max(s.minWidth(), Math.min(s.maxWidth(), viewportWidth * 0.8f));

        layoutWrapper.setWidth(viewportWidth);
        layoutWrapper.setHeight(viewportHeight);
        getLayoutNode().setWidth(dialogWidth);
        layoutWidgetsRecursive(this, ctx);
        layoutWrapper.calculateLayout(viewportWidth, viewportHeight);

        float dialogHeight = getLayoutNode().getLayoutHeight();

        dialogX = (viewportWidth - dialogWidth) / 2;
        dialogY = (viewportHeight - dialogHeight) / 2;
        getLayoutNode().setPositionType(YogaPositionType.ABSOLUTE);
        getLayoutNode().setPosition(YogaEdge.LEFT, dialogX);
        getLayoutNode().setPosition(YogaEdge.TOP, dialogY);

        layoutWrapper.calculateLayout(viewportWidth, viewportHeight);
    }

    public float getDialogX() {
        return dialogX;
    }

    public float getDialogY() {
        return dialogY;
    }

    private void layoutWidgetsRecursive(AbstractWidget<?> widget, RenderContext ctx) {
        if (widget != this) {
            widget.layouting(ctx);
        }
        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    layoutWidgetsRecursive(childWidget, ctx);
                }
            }
        }
    }

    public boolean handleMousePress(float x, float y, int button) {
        if (!showing && !dismissPending) {
            return false;
        }
        if (dismissPending) {
            return true;
        }

        Rectangle bounds = getRawBounds();
        if (!bounds.in(x, y)) {
            if (style().scrimDismiss()) {
                dismiss();
            }
            return true;
        }

        AbstractWidget<?> target = findInteractiveWidgetAt(new Vector2f(x, y));
        if (target != null && target != this) {
            target.mousePress(x, y, button);
        }

        return true;
    }

    public boolean handleMouseMove(float x, float y) {
        if (!showing && !dismissPending) {
            return false;
        }
        if (dismissPending) {
            return true;
        }

        AbstractWidget<?> target = findInteractiveWidgetAt(new Vector2f(x, y));

        if (target != hoveredInDialog) {
            if (hoveredInDialog != null) {
                hoveredInDialog.clearHover();
            }
            hoveredInDialog = target;
        }

        if (target != null && target != this) {
            target.mouseMove(x, y);
        }

        return true;
    }

    public boolean handleMouseRelease(float x, float y, int button) {
        if (!showing && !dismissPending) {
            return false;
        }
        if (dismissPending) {
            return true;
        }

        dispatchReleaseRecursive(this, x, y, button);

        return true;
    }

    private void dispatchReleaseRecursive(AbstractWidget<?> widget, float x, float y, int btn) {
        if (!widget.isVisible() || widget.isDisabled()) {
            return;
        }
        widget.mouseRelease(x, y, btn);
        if (widget instanceof ILayoutContainer container) {
            for (ILayoutElement child : container.getChildren()) {
                if (child instanceof AbstractWidget<?> childWidget) {
                    dispatchReleaseRecursive(childWidget, x, y, btn);
                }
            }
        }
    }

    public boolean handleMouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        return showing || dismissPending;
    }

    public boolean handleMouseScroll(float x, float y, double scrollX) {
        return showing || dismissPending;
    }

    public boolean handleKeyPress(int keyCode, int scancode, int modifiers) {
        if (!showing && !dismissPending) {
            return false;
        }
        if (dismissPending) {
            return true;
        }
        if (keyCode == 256) {
            dismiss();
            return true;
        }
        return true;
    }

    public boolean handleKeyRelease(int keyCode, int scancode, int modifiers) {
        return showing || dismissPending;
    }

    public boolean handleCharTyped(char codePoint, int modifiers) {
        return showing || dismissPending;
    }

    private static class IconWidget extends MaterialWidget<IconWidget> {
        private final MaterialSymbol symbol;
        private final float iconSize;

        IconWidget(MaterialSymbol symbol, float iconSize) {
            this.symbol = symbol;
            this.iconSize = iconSize;
            setElementSize(iconSize, iconSize);
        }

        @Override
        protected void init() {
        }

        @Override
        protected boolean isInteractive() {
            return false;
        }

        @Override
        public void render(RenderContext ctx, UIInputState inputState) {
            Rectangle bounds = getBounds();
            symbol.render(ctx, scheme().secondary(), iconSize,
                    new Vector2f(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2));
        }
    }

    private static class DividerWidget extends MaterialWidget<DividerWidget> {
        DividerWidget(float height) {
            layout().setWidthPercent(100);
            layout().setHeight(height);
        }

        @Override
        protected void init() {
        }

        @Override
        protected boolean isInteractive() {
            return false;
        }

        @Override
        public void render(RenderContext ctx, UIInputState inputState) {
            Rectangle bounds = getBounds();
            ctx.rect(bounds.x, bounds.y, bounds.width, bounds.height,
                    scheme().outlineVariant(), true);
        }
    }
}
