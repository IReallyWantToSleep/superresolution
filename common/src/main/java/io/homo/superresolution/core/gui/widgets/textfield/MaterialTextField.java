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

package io.homo.superresolution.core.gui.widgets.textfield;

import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.MaterialWidgetOverlay;
import io.homo.superresolution.core.gui.core.MouseButton;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.event.events.MouseEvent;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.input.ClipboardAdapter;
import io.homo.superresolution.core.gui.core.input.KeyCode;
import io.homo.superresolution.core.gui.core.input.KeyInput;
import io.homo.superresolution.core.gui.core.input.MinecraftClipboardAdapter;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MouseCursor;
import org.joml.Vector2f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

public class MaterialTextField extends MaterialWidget<MaterialTextField> {
    private static final long ANIMATION_DURATION_MS = 150L;
    private static final int NO_SELECTION = -1;
    private static final int MAX_HISTORY_SIZE = 100;

    private final Animator.FloatAnimator focusAnimator = Animator.ofFloat(0f, 0f)
            .duration(ANIMATION_DURATION_MS)
            .timeInterpolator(TimeInterpolator.easeOutCubic());
    private final Animator.FloatAnimator labelAnimator = Animator.ofFloat(0f, 0f)
            .duration(ANIMATION_DURATION_MS)
            .timeInterpolator(TimeInterpolator.easeOutCubic());
    private final MaterialWidgetOverlay<MaterialTextField> trailingIconOverlay = new MaterialWidgetOverlay<>(this) {
        @Override
        protected void drawShape(RenderContext ctx, MaterialTextField widget, Color color) {
            Rectangle bounds = getOverlayBounds();
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    bounds.width / 2f,
                    color,
                    true
            );
        }

        @Override
        protected void drawShape(RenderContext ctx, MaterialTextField widget, IPaint paint) {
            Rectangle bounds = getOverlayBounds();
            ctx.beginPath();
            ctx.paint(paint);
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    bounds.width / 2f
            );
            ctx.endPath();
        }

        @Override
        protected Rectangle getOverlayBounds() {
            return trailingIconBounds();
        }
    };
    private final Deque<TextEditState> undoHistory = new ArrayDeque<>();
    private final Deque<TextEditState> redoHistory = new ArrayDeque<>();

    private String label = "";
    private String placeholder = "";
    private String supportingText = "";
    private String errorText = "";
    private String value = "";
    private MaterialSymbol leadingIcon;
    private MaterialSymbol trailingIcon;
    private Consumer<MaterialTextField> trailingIconAction;
    private ClipboardAdapter clipboard = MinecraftClipboardAdapter.INSTANCE;
    private float width = 280f;
    private float horizontalScrollOffset;
    private float renderedInputStartX;
    private float renderedInputEndX;
    private float[] renderedCharacterOffsets = new float[]{0f};
    private int cursorIndex;
    private int selectionAnchor = NO_SELECTION;
    private int dragSelectionAnchor = NO_SELECTION;
    private KeyCode pendingShortcutCharacter;
    private int maxLength = Integer.MAX_VALUE;
    private long caretBlinkStartMs = System.currentTimeMillis();
    private boolean error;
    private boolean readOnly;
    private boolean showCharacterCount;
    private boolean showErrorIcon = true;
    private boolean trailingIconHovered;

    public MaterialTextField() {
        this.style = new MaterialTextFieldStyle();
        getLayoutNode().setDebugName("MaterialTextField");
        updateSize();
    }

    public static MaterialTextField create() {
        return new MaterialTextField();
    }

    public static MaterialTextField filled() {
        return create().variant(MaterialTextFieldVariant.Filled);
    }

    public static MaterialTextField outlined() {
        return create().variant(MaterialTextFieldVariant.Outlined);
    }

    @Override
    protected void init() {
        onMousePress(this::onMousePress);
        onMouseDrag(this::onMouseDrag);
        onMouseRelease(event -> dragSelectionAnchor = NO_SELECTION);
        onMouseMove(this::onMouseMove);
    }

    @Override
    public void layouting(RenderContext ctx) {
        updateSize();
    }

    @Override
    public MaterialTextFieldStyle style() {
        return (MaterialTextFieldStyle) style;
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    @Override
    public MouseCursor getMouseCursor() {
        if (isDisabled()) {
            return MouseCursor.NOT_ALLOWED;
        }
        if (hasTrailingIconAction() && trailingIconHovered) {
            return MouseCursor.HAND;
        }
        return MouseCursor.IBEAM;
    }

    @Override
    public MaterialTextField setFocused(boolean focused) {
        if (focused == isFocused()) {
            return this;
        }

        super.setFocused(focused);
        focusAnimator.fromTo(focusAnimator.get(), focused ? 1f : 0f).start();
        updateLabelState();
        resetCaretBlink();
        return this;
    }

    @Override
    public MaterialTextField setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        if (disabled) {
            setFocused(false);
        }
        return this;
    }

    @Override
    public void keyPress(KeyInput input) {
        if (!isFocused() || isDisabled()) {
            return;
        }

        KeyCode keyCode = input.code();
        if (keyCode == KeyCode.ESCAPE) {
            clearSelection();
            if (getFrame() != null) {
                getFrame().requestFocus(null);
            }
            return;
        }

        if (input.controlDown() || input.superDown()) {
            switch (keyCode) {
                case A -> {
                    pendingShortcutCharacter = keyCode;
                    selectAll();
                    return;
                }
                case C -> {
                    pendingShortcutCharacter = keyCode;
                    copySelection();
                    return;
                }
                case X -> {
                    pendingShortcutCharacter = keyCode;
                    if (!readOnly) {
                        cutSelection();
                    }
                    return;
                }
                case V -> {
                    pendingShortcutCharacter = keyCode;
                    if (!readOnly) {
                        pasteClipboard();
                    }
                    return;
                }
                case Z -> {
                    pendingShortcutCharacter = keyCode;
                    if (!readOnly) {
                        if (input.shiftDown()) {
                            redo();
                        } else {
                            undo();
                        }
                    }
                    return;
                }
                case Y -> {
                    pendingShortcutCharacter = keyCode;
                    if (!readOnly) {
                        redo();
                    }
                    return;
                }
                default -> {
                }
            }
        }

        switch (keyCode) {
            case LEFT -> moveCursorLeft(input.controlDown(), input.shiftDown());
            case RIGHT -> moveCursorRight(input.controlDown(), input.shiftDown());
            case HOME -> moveCursor(0, input.shiftDown());
            case END -> moveCursor(value.length(), input.shiftDown());
            case BACKSPACE -> {
                if (!readOnly) {
                    backspace(input.controlDown());
                }
            }
            case DELETE -> {
                if (!readOnly) {
                    delete(input.controlDown());
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void keyRelease(KeyInput input) {
        if (input.code() == pendingShortcutCharacter) {
            pendingShortcutCharacter = null;
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (pendingShortcutCharacter != null) {
            pendingShortcutCharacter = null;
            return;
        }
        if (!isFocused() || isDisabled() || readOnly || Character.isISOControl(codePoint)) {
            return;
        }
        replaceSelection(String.valueOf(codePoint));
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        focusAnimator.update();
        labelAnimator.update();

        Rectangle componentBounds = getRawBounds();
        MaterialTextFieldSize size = style().size();
        Rectangle fieldBounds = new Rectangle(
                componentBounds.x,
                componentBounds.y,
                componentBounds.width,
                size.containerHeight()
        );
        TextFieldColors colors = resolveColors();
        float focusProgress = focusAnimator.get();
        float labelProgress = labelAnimator.get();

        ctx.beginGroup(style().zIndex());
        ctx.save();

        if (style().variant() == MaterialTextFieldVariant.Filled) {
            drawFilledContainer(ctx, fieldBounds, size, colors, focusProgress);
        } else {
            drawOutlinedContainer(ctx, fieldBounds, size, colors, focusProgress, labelProgress);
        }

        float contentStartX = fieldBounds.x + size.horizontalPadding();
        float contentEndX = fieldBounds.getLimitX() - size.horizontalPadding();
        MaterialSymbol currentLeadingIcon = leadingIcon;
        MaterialSymbol currentTrailingIcon = resolvedTrailingIcon();
        float centerY = fieldBounds.getCenterY();

        if (currentLeadingIcon != null) {
            float iconCenterX = contentStartX + size.iconSize() / 2f;
            currentLeadingIcon.render(
                    ctx,
                    colors.iconColor,
                    size.iconSize(),
                    new Vector2f(iconCenterX, centerY)
            );
            contentStartX += size.iconSize() + size.iconTextGap();
        }

        if (currentTrailingIcon != null) {
            float iconCenterX = contentEndX - size.iconSize() / 2f;
            if (hasTrailingIconAction()) {
                trailingIconOverlay.update();
                trailingIconOverlay.render(
                        ctx,
                        colors.trailingIconColor,
                        colors.trailingIconColor
                );
            }
            currentTrailingIcon.render(
                    ctx,
                    colors.trailingIconColor,
                    size.iconSize(),
                    new Vector2f(iconCenterX, centerY)
            );
            contentEndX -= size.iconSize() + size.iconTextGap();
        }

        drawLabel(ctx, fieldBounds, size, colors, contentStartX, contentEndX, labelProgress);
        drawInput(ctx, fieldBounds, size, colors, contentStartX, contentEndX, labelProgress);
        drawSupportingRow(ctx, fieldBounds, size, colors);

        ctx.restore();
        ctx.endGroup();
    }

    @Override
    public void destroy() {
        if (focusAnimator.isRunning()) {
            focusAnimator.cancel();
        }
        if (labelAnimator.isRunning()) {
            labelAnimator.cancel();
        }
        trailingIconOverlay.destroy();
    }

    public MaterialTextField variant(MaterialTextFieldVariant variant) {
        style().variant(variant);
        updateSize();
        return this;
    }

    public MaterialTextField size(MaterialTextFieldSize size) {
        style().size(size);
        updateSize();
        return this;
    }

    public MaterialTextField width(float width) {
        this.width = Math.max(0f, width);
        updateSize();
        return this;
    }

    public float width() {
        return width;
    }

    public MaterialTextField label(String label) {
        this.label = nullToEmpty(label);
        updateLabelState();
        return this;
    }

    public String label() {
        return label;
    }

    public MaterialTextField placeholder(String placeholder) {
        this.placeholder = nullToEmpty(placeholder);
        return this;
    }

    public String placeholder() {
        return placeholder;
    }

    public MaterialTextField supportingText(String supportingText) {
        this.supportingText = nullToEmpty(supportingText);
        updateSize();
        return this;
    }

    public String supportingText() {
        return supportingText;
    }

    public MaterialTextField errorText(String errorText) {
        this.errorText = nullToEmpty(errorText);
        if (!this.errorText.isEmpty()) {
            this.error = true;
        }
        updateSize();
        return this;
    }

    public String errorText() {
        return errorText;
    }

    public MaterialTextField setError(boolean error) {
        this.error = error;
        updateSize();
        return this;
    }

    public boolean hasError() {
        return error;
    }

    public MaterialTextField leadingIcon(MaterialSymbol icon) {
        this.leadingIcon = icon;
        return this;
    }

    public MaterialTextField trailingIcon(MaterialSymbol icon) {
        this.trailingIcon = icon;
        return this;
    }

    public MaterialTextField trailingIconAction(Consumer<MaterialTextField> action) {
        this.trailingIconAction = action;
        return this;
    }

    public MaterialTextField clipboard(ClipboardAdapter clipboard) {
        this.clipboard = Objects.requireNonNull(clipboard, "clipboard");
        return this;
    }

    public MaterialTextField showErrorIcon(boolean showErrorIcon) {
        this.showErrorIcon = showErrorIcon;
        return this;
    }

    public MaterialTextField setValue(String value) {
        String sanitizedValue = truncate(nullToEmpty(value));
        if (Objects.equals(this.value, sanitizedValue)) {
            return this;
        }

        String oldValue = this.value;
        this.value = sanitizedValue;
        cursorIndex = sanitizedValue.length();
        clearSelection();
        undoHistory.clear();
        redoHistory.clear();
        resetCaretBlink();
        updateLabelState();
        eventBus.post(new WidgetEvent.ChangeEvent<>(oldValue, sanitizedValue));
        return this;
    }

    public String getValue() {
        return value;
    }

    public MaterialTextField maxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        if (value.length() > this.maxLength) {
            setValue(value.substring(0, this.maxLength));
        }
        return this;
    }

    public int maxLength() {
        return maxLength;
    }

    public MaterialTextField readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public MaterialTextField showCharacterCount(boolean showCharacterCount) {
        this.showCharacterCount = showCharacterCount;
        updateSize();
        return this;
    }

    public boolean showsCharacterCount() {
        return showCharacterCount;
    }

    public int cursorIndex() {
        return cursorIndex;
    }

    public MaterialTextField selectAll() {
        selectionAnchor = 0;
        cursorIndex = value.length();
        resetCaretBlink();
        return this;
    }

    public MaterialTextField clear() {
        replaceRange(0, value.length(), "");
        return this;
    }

    private void drawFilledContainer(
            RenderContext ctx,
            Rectangle bounds,
            MaterialTextFieldSize size,
            TextFieldColors colors,
            float focusProgress
    ) {
        ctx.roundedRectComplex(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                0f,
                0f,
                size.cornerRadius(),
                size.cornerRadius(),
                colors.containerColor,
                true
        );

        float indicatorHeight = size.indicatorHeight()
                + (size.focusedIndicatorHeight() - size.indicatorHeight()) * focusProgress;
        ctx.rect(
                bounds.x,
                bounds.getLimitY() - indicatorHeight,
                bounds.width,
                indicatorHeight,
                colors.indicatorColor,
                true
        );
    }

    private void drawOutlinedContainer(
            RenderContext ctx,
            Rectangle bounds,
            MaterialTextFieldSize size,
            TextFieldColors colors,
            float focusProgress,
            float labelProgress
    ) {
        float outlineWidth = size.outlineWidth()
                + (size.focusedOutlineWidth() - size.outlineWidth()) * focusProgress;
        float radius = Math.min(size.cornerRadius(), Math.min(bounds.width, bounds.height) / 2f);
        float left = bounds.x;
        float right = bounds.getLimitX();
        float top = bounds.y;
        float bottom = bounds.getLimitY();
        float topLeft = left + radius;
        float topRight = right - radius;

        float gapStart = topLeft;
        float gapEnd = topLeft;
        if (!label.isEmpty() && labelProgress > 0f) {
            float labelFontSize = lerp(size.labelCenterFontSize(), size.labelFontSize(), labelProgress);
            float labelWidth = ctx.measureTextWidth(label, labelFontSize, labelFontSize + 2f);
            float gapHalfWidth = (labelWidth + 8f) * Math.min(1f, labelProgress) / 2f;
            float gapCenter = labelStartX(bounds, size) + labelWidth / 2f;
            gapStart = Math.max(topLeft, Math.min(topRight, gapCenter - gapHalfWidth));
            gapEnd = Math.max(gapStart, Math.min(topRight, gapCenter + gapHalfWidth));
        }

        ctx.beginPath();
        ctx.strokeWidth(outlineWidth);
        ctx.strokeColor(colors.outlineColor);

        if (gapStart == topLeft && gapEnd == topLeft) {
            ctx.roundedRect(left, top, bounds.width, bounds.height, radius);
        } else {
            // RenderContext.arc() exposes NanoVG's counter-clockwise direction,
            // so trace the outline counter-clockwise to keep each corner arc short.
            ctx.move(topRight, top);
            ctx.lineTo(gapEnd, top);
            ctx.move(gapStart, top);
            ctx.lineTo(topLeft, top);
            ctx.arc(left + radius, top + radius, radius, -(float) (Math.PI / 2f), -(float) Math.PI);
            ctx.lineTo(left, bottom - radius);
            ctx.arc(left + radius, bottom - radius, radius, -(float) Math.PI, -(float) (Math.PI * 1.5f));
            ctx.lineTo(right - radius, bottom);
            ctx.arc(right - radius, bottom - radius, radius, -(float) (Math.PI * 1.5f), -(float) (Math.PI * 2f));
            ctx.lineTo(right, top + radius);
            ctx.arc(right - radius, top + radius, radius, 0f, -(float) (Math.PI / 2f));
        }

        ctx.endPath(false);
    }

    private float labelStartX(Rectangle bounds, MaterialTextFieldSize size) {
        float startX = bounds.x + size.horizontalPadding();
        if (leadingIcon != null) {
            startX += size.iconSize() + size.iconTextGap();
        }
        return startX;
    }

    private void drawLabel(
            RenderContext ctx,
            Rectangle bounds,
            MaterialTextFieldSize size,
            TextFieldColors colors,
            float contentStartX,
            float contentEndX,
            float labelProgress
    ) {
        if (label.isEmpty()) {
            return;
        }

        float labelFontSize = lerp(size.labelCenterFontSize(), size.labelFontSize(), labelProgress);
        float restingY = bounds.getCenterY();
        float floatingY = style().variant() == MaterialTextFieldVariant.Filled
                ? bounds.y + 16f
                : bounds.y;
        float labelY = lerp(restingY, floatingY, labelProgress);

        if (style().variant() == MaterialTextFieldVariant.Outlined && labelProgress > 0f) {
            float labelWidth = ctx.measureTextWidth(label, labelFontSize, labelFontSize + 2f);
            float backgroundAlpha = 255f * Math.min(1f, labelProgress);
            ctx.rect(
                    contentStartX - 4f,
                    labelY - labelFontSize / 2f,
                    labelWidth + 8f,
                    labelFontSize,
                    scheme().surface().copy().alpha((int) backgroundAlpha),
                    true
            );
        }

        ctx.drawAlignedText(
                ctx.font(),
                labelFontSize,
                label,
                contentStartX,
                labelY,
                Math.max(0f, contentEndX - contentStartX),
                labelFontSize,
                colors.labelColor,
                TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                false
        );
    }

    private void drawInput(
            RenderContext ctx,
            Rectangle bounds,
            MaterialTextFieldSize size,
            TextFieldColors colors,
            float contentStartX,
            float contentEndX,
            float labelProgress
    ) {
        float visibleWidth = Math.max(0f, contentEndX - contentStartX);
        String displayedText = value.isEmpty() ? placeholder : value;
        boolean drawPlaceholder = value.isEmpty() && labelProgress > 0.5f;
        boolean drawValue = !value.isEmpty();
        if (!drawValue && !drawPlaceholder) {
            cacheTextLayout(ctx, contentStartX, contentEndX);
            return;
        }

        cacheTextLayout(ctx, contentStartX, contentEndX);
        updateHorizontalScroll(visibleWidth);
        float floatingInputOffset = style().variant() == MaterialTextFieldVariant.Filled
                ? size.floatingInputVerticalOffset() * labelProgress
                : 0f;
        float inputY = bounds.getCenterY() + floatingInputOffset;

        ctx.save();
        ctx.scissor(contentStartX, bounds.y, visibleWidth, bounds.height);

        if (drawValue && hasSelection()) {
            float selectionStart = contentStartX + renderedCharacterOffsets[selectionStart()] - horizontalScrollOffset;
            float selectionEnd = contentStartX + renderedCharacterOffsets[selectionEnd()] - horizontalScrollOffset;
            ctx.rect(
                    selectionStart,
                    inputY - size.inputFontSize() / 2f,
                    Math.max(0f, selectionEnd - selectionStart),
                    size.inputFontSize(),
                    colors.selectionColor,
                    true
            );
        }

        ctx.drawAlignedText(
                ctx.font(),
                size.inputFontSize(),
                displayedText,
                contentStartX - horizontalScrollOffset,
                inputY,
                Math.max(visibleWidth + horizontalScrollOffset, visibleWidth),
                size.inputFontSize(),
                drawValue ? colors.inputTextColor : colors.placeholderColor,
                TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                false
        );

        if (isFocused() && isCaretVisible()) {
            float caretX = contentStartX + renderedCharacterOffsets[cursorIndex] - horizontalScrollOffset;
            ctx.rect(
                    caretX,
                    inputY - size.inputFontSize() / 2f,
                    1f,
                    size.inputFontSize(),
                    colors.caretColor,
                    true
            );
        }

        ctx.restore();
    }

    private void drawSupportingRow(
            RenderContext ctx,
            Rectangle fieldBounds,
            MaterialTextFieldSize size,
            TextFieldColors colors
    ) {
        String supportText = activeSupportingText();
        if (!needsSupportingRow()) {
            return;
        }

        float supportingY = fieldBounds.getLimitY() + size.supportingTextTopMargin();
        float textWidth = Math.max(0f, fieldBounds.width - size.horizontalPadding() * 2f);
        float counterWidth = showCharacterCount ? 96f : 0f;
        if (!supportText.isEmpty()) {
            ctx.drawAlignedText(
                    ctx.font(),
                    size.supportingTextFontSize(),
                    supportText,
                    fieldBounds.x + size.horizontalPadding(),
                    supportingY,
                    Math.max(0f, textWidth - counterWidth),
                    size.supportingTextFontSize() + 2f,
                    colors.supportingTextColor,
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                    false
            );
        }

        if (showCharacterCount) {
            String counterText = maxLength == Integer.MAX_VALUE
                    ? String.valueOf(value.length())
                    : value.length() + "/" + maxLength;
            ctx.drawAlignedText(
                    ctx.font(),
                    size.supportingTextFontSize(),
                    counterText,
                    fieldBounds.getLimitX() - size.horizontalPadding() - counterWidth,
                    supportingY,
                    counterWidth,
                    size.supportingTextFontSize() + 2f,
                    colors.supportingTextColor,
                    TextAlign.of(TextAlignType.ALIGN_RIGHT, TextAlignType.ALIGN_TOP),
                    false
            );
        }
    }

    private void onMousePress(MouseEvent.MousePressEvent event) {
        if (event.getButton() != MouseButton.Left.id() || isDisabled()) {
            return;
        }

        if (hasTrailingIconAction() && trailingIconBounds().in(event.getMousePosition())) {
            trailingIconAction.accept(this);
            return;
        }

        cursorIndex = cursorIndexAt(event.getMousePosition().x);
        clearSelection();
        dragSelectionAnchor = cursorIndex;
        resetCaretBlink();
    }

    private void onMouseDrag(MouseEvent.MouseDragEvent event) {
        if (!isFocused() || !isPressed() || isDisabled()) {
            return;
        }

        if (dragSelectionAnchor == NO_SELECTION) {
            dragSelectionAnchor = cursorIndex;
        }
        selectionAnchor = dragSelectionAnchor;
        cursorIndex = cursorIndexAt(event.getMousePosition().x);
        resetCaretBlink();
    }

    private void onMouseMove(MouseEvent.MouseMoveEvent event) {
        trailingIconHovered = hasTrailingIconAction() && trailingIconBounds().in(event.getMousePosition());
    }

    private TextFieldColors resolveColors() {
        TextFieldColors colors = new TextFieldColors();
        boolean activeError = error;
        Color activeColor = activeError ? scheme().error() : scheme().primary();
        Color inactiveIndicatorColor = activeError ? scheme().error() : scheme().onSurfaceVariant();
        Color inactiveOutlineColor = activeError ? scheme().error() : scheme().outline();

        colors.containerColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.04f))
                : style().variant() == MaterialTextFieldVariant.Filled
                ? scheme().surfaceContainerHighest()
                : scheme().surface();
        colors.outlineColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.12f))
                : isFocused() ? activeColor : inactiveOutlineColor;
        colors.indicatorColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.12f))
                : isFocused() ? activeColor : inactiveIndicatorColor;
        colors.inputTextColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.38f))
                : scheme().onSurface();
        colors.placeholderColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.38f))
                : scheme().onSurfaceVariant();
        colors.labelColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.38f))
                : isFocused() || activeError ? activeColor : scheme().onSurfaceVariant();
        colors.iconColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.38f))
                : activeError ? scheme().error() : scheme().onSurfaceVariant();
        colors.trailingIconColor = colors.iconColor;
        colors.supportingTextColor = isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.38f))
                : activeError ? scheme().error() : scheme().onSurfaceVariant();
        colors.caretColor = activeColor;
        colors.selectionColor = scheme().primary().copy().alpha((int) (255 * 0.24f));
        return colors;
    }

    private void cacheTextLayout(RenderContext ctx, float contentStartX, float contentEndX) {
        renderedInputStartX = contentStartX;
        renderedInputEndX = contentEndX;
        renderedCharacterOffsets = new float[value.length() + 1];
        for (int index = 1; index <= value.length(); index++) {
            renderedCharacterOffsets[index] = ctx.measureTextWidth(
                    value.substring(0, index),
                    style().size().inputFontSize(),
                    style().size().inputFontSize() + 2f
            );
        }
    }

    private void updateHorizontalScroll(float visibleWidth) {
        if (renderedCharacterOffsets.length == 0) {
            horizontalScrollOffset = 0f;
            return;
        }

        float cursorX = renderedCharacterOffsets[cursorIndex];
        if (cursorX - horizontalScrollOffset > visibleWidth - 1f) {
            horizontalScrollOffset = cursorX - visibleWidth + 1f;
        } else if (cursorX < horizontalScrollOffset) {
            horizontalScrollOffset = cursorX;
        }

        float totalWidth = renderedCharacterOffsets[renderedCharacterOffsets.length - 1];
        horizontalScrollOffset = Math.max(0f, Math.min(horizontalScrollOffset, Math.max(0f, totalWidth - visibleWidth)));
    }

    private Rectangle trailingIconBounds() {
        MaterialTextFieldSize size = style().size();
        Rectangle bounds = getRawBounds();
        float right = bounds.getLimitX() - size.horizontalPadding();
        return new Rectangle(
                right - size.iconSize(),
                bounds.y + (size.containerHeight() - size.iconSize()) / 2f,
                size.iconSize(),
                size.iconSize()
        );
    }

    private MaterialSymbol resolvedTrailingIcon() {
        if (trailingIcon != null) {
            return trailingIcon;
        }
        return error && showErrorIcon ? MaterialSymbols.iconError() : null;
    }

    private boolean hasTrailingIconAction() {
        return trailingIcon != null && trailingIconAction != null;
    }

    private int cursorIndexAt(float mouseX) {
        if (renderedCharacterOffsets.length != value.length() + 1) {
            return value.length();
        }

        float contentX = mouseX - renderedInputStartX + horizontalScrollOffset;
        if (contentX <= 0f) {
            return 0;
        }

        for (int index = 1; index < renderedCharacterOffsets.length; index++) {
            float midpoint = (renderedCharacterOffsets[index - 1] + renderedCharacterOffsets[index]) / 2f;
            if (contentX < midpoint) {
                return index - 1;
            }
        }
        return value.length();
    }

    private void moveCursorLeft(boolean byWord, boolean extendSelection) {
        if (!extendSelection && hasSelection()) {
            moveCursor(selectionStart(), false);
            return;
        }
        moveCursor(byWord ? previousWordIndex(cursorIndex) : cursorIndex - 1, extendSelection);
    }

    private void moveCursorRight(boolean byWord, boolean extendSelection) {
        if (!extendSelection && hasSelection()) {
            moveCursor(selectionEnd(), false);
            return;
        }
        moveCursor(byWord ? nextWordIndex(cursorIndex) : cursorIndex + 1, extendSelection);
    }

    private void moveCursor(int targetIndex, boolean extendSelection) {
        targetIndex = Math.max(0, Math.min(targetIndex, value.length()));
        if (extendSelection) {
            if (selectionAnchor == NO_SELECTION) {
                selectionAnchor = cursorIndex;
            }
        } else {
            clearSelection();
        }
        cursorIndex = targetIndex;
        resetCaretBlink();
    }

    private void backspace(boolean byWord) {
        if (deleteSelection()) {
            return;
        }
        if (cursorIndex == 0) {
            return;
        }

        int targetIndex = byWord ? previousWordIndex(cursorIndex) : cursorIndex - 1;
        replaceRange(targetIndex, cursorIndex, "");
    }

    private void delete(boolean byWord) {
        if (deleteSelection()) {
            return;
        }
        if (cursorIndex >= value.length()) {
            return;
        }

        int targetIndex = byWord ? nextWordIndex(cursorIndex) : cursorIndex + 1;
        replaceRange(cursorIndex, targetIndex, "");
    }

    private boolean deleteSelection() {
        if (!hasSelection()) {
            return false;
        }
        replaceRange(selectionStart(), selectionEnd(), "");
        return true;
    }

    private void replaceSelection(String replacement) {
        replaceRange(selectionStart(), selectionEnd(), replacement);
    }

    private void replaceRange(int start, int end, String replacement) {
        String safeReplacement = nullToEmpty(replacement);
        int remainingLength = Math.max(0, maxLength - (value.length() - (end - start)));
        if (safeReplacement.length() > remainingLength) {
            safeReplacement = safeReplacement.substring(0, remainingLength);
        }

        String updatedValue = value.substring(0, start) + safeReplacement + value.substring(end);
        if (updatedValue.equals(value)) {
            return;
        }

        rememberForUndo();
        applyEditedValue(updatedValue, start + safeReplacement.length(), NO_SELECTION);
    }

    private void copySelection() {
        if (hasSelection()) {
            clipboard.write(value.substring(selectionStart(), selectionEnd()));
        }
    }

    private void cutSelection() {
        if (!hasSelection()) {
            return;
        }
        copySelection();
        replaceRange(selectionStart(), selectionEnd(), "");
    }

    private void pasteClipboard() {
        String clipboardText = sanitizeClipboardText(clipboard.read());
        if (!clipboardText.isEmpty()) {
            replaceRange(selectionStart(), selectionEnd(), clipboardText);
        }
    }

    private void undo() {
        if (undoHistory.isEmpty()) {
            return;
        }
        redoHistory.push(captureEditState());
        restoreEditState(undoHistory.pop());
    }

    private void redo() {
        if (redoHistory.isEmpty()) {
            return;
        }
        undoHistory.push(captureEditState());
        restoreEditState(redoHistory.pop());
    }

    private void rememberForUndo() {
        undoHistory.push(captureEditState());
        while (undoHistory.size() > MAX_HISTORY_SIZE) {
            undoHistory.removeLast();
        }
        redoHistory.clear();
    }

    private TextEditState captureEditState() {
        return new TextEditState(value, cursorIndex, selectionAnchor);
    }

    private void restoreEditState(TextEditState state) {
        applyEditedValue(state.value(), state.cursorIndex(), state.selectionAnchor());
    }

    private void applyEditedValue(String updatedValue, int updatedCursorIndex, int updatedSelectionAnchor) {
        String oldValue = value;
        value = updatedValue;
        cursorIndex = Math.max(0, Math.min(updatedCursorIndex, value.length()));
        selectionAnchor = updatedSelectionAnchor == NO_SELECTION
                ? NO_SELECTION
                : Math.max(0, Math.min(updatedSelectionAnchor, value.length()));
        dragSelectionAnchor = NO_SELECTION;
        resetCaretBlink();
        updateLabelState();
        eventBus.post(new WidgetEvent.InputEvent<>(oldValue, updatedValue));
        eventBus.post(new WidgetEvent.ChangeEvent<>(oldValue, updatedValue));
    }

    private int previousWordIndex(int index) {
        int result = Math.max(0, Math.min(index, value.length()));
        while (result > 0 && Character.isWhitespace(value.charAt(result - 1))) {
            result--;
        }
        while (result > 0 && !Character.isWhitespace(value.charAt(result - 1))) {
            result--;
        }
        return result;
    }

    private int nextWordIndex(int index) {
        int result = Math.max(0, Math.min(index, value.length()));
        while (result < value.length() && Character.isWhitespace(value.charAt(result))) {
            result++;
        }
        while (result < value.length() && !Character.isWhitespace(value.charAt(result))) {
            result++;
        }
        return result;
    }

    private boolean hasSelection() {
        return selectionAnchor != NO_SELECTION && selectionAnchor != cursorIndex;
    }

    private int selectionStart() {
        return hasSelection() ? Math.min(selectionAnchor, cursorIndex) : cursorIndex;
    }

    private int selectionEnd() {
        return hasSelection() ? Math.max(selectionAnchor, cursorIndex) : cursorIndex;
    }

    private void clearSelection() {
        selectionAnchor = NO_SELECTION;
    }

    private boolean needsSupportingRow() {
        return !activeSupportingText().isEmpty() || showCharacterCount;
    }

    private String activeSupportingText() {
        return error && !errorText.isEmpty() ? errorText : supportingText;
    }

    private void updateLabelState() {
        boolean shouldFloat = isFocused() || !value.isEmpty();
        labelAnimator.fromTo(labelAnimator.get(), shouldFloat ? 1f : 0f).start();
    }

    private void updateSize() {
        MaterialTextFieldSize size = style().size();
        float supportingHeight = needsSupportingRow()
                ? size.supportingTextTopMargin() + size.supportingTextFontSize() + 2f
                : 0f;
        setElementSize(width, size.containerHeight() + supportingHeight);
    }

    private void resetCaretBlink() {
        caretBlinkStartMs = System.currentTimeMillis();
    }

    private boolean isCaretVisible() {
        return ((System.currentTimeMillis() - caretBlinkStartMs) % 1_000L) < 500L;
    }

    private String truncate(String text) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private static String sanitizeClipboardText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder sanitized = new StringBuilder(text.length());
        boolean previousWasWhitespace = false;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\r' || character == '\n' || character == '\t') {
                if (!previousWasWhitespace) {
                    sanitized.append(' ');
                    previousWasWhitespace = true;
                }
            } else if (!Character.isISOControl(character)) {
                sanitized.append(character);
                previousWasWhitespace = Character.isWhitespace(character);
            }
        }
        return sanitized.toString();
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    private static class TextFieldColors {
        Color containerColor;
        Color outlineColor;
        Color indicatorColor;
        Color inputTextColor;
        Color placeholderColor;
        Color labelColor;
        Color iconColor;
        Color trailingIconColor;
        Color supportingTextColor;
        Color caretColor;
        Color selectionColor;
    }

    private record TextEditState(
            String value,

            int cursorIndex,

            int selectionAnchor
    ) {
    }
}
