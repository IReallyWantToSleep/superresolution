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

package io.homo.superresolution.common.gui.options;

import io.homo.superresolution.common.gui.impl.OptionRequirement;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.impl.ValueHolder;
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.core.AbstractContainerWidget;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.event.GuiEventListener;
import io.homo.superresolution.core.gui.core.impl.Renderable;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractOptionEntry<VT, SELF> implements Renderable, ValueHolder<VT>, GuiEventListener {
    protected Text name;
    protected boolean requiresRestartGame;
    protected @Nullable Supplier<VT> defaultValue = null;
    protected @Nullable Function<VT, Optional<Text>> errorSupplier;
    protected @Nullable OptionRequirement enableRequirement = null;
    protected @Nullable OptionRequirement displayRequirement = null;
    protected Consumer<VT> saveConsumer = null;
    protected Runnable saveRunnable = null;
    protected Function<VT, Optional<Text[]>> tooltipSupplier = (list) -> Optional.empty();
    protected VT value;
    protected OptionContainerWidget container;

    public AbstractOptionEntry(Text name, VT value) {
        this.name = name;
        this.value = value;
    }

    protected AbstractOptionEntry<VT, SELF> setSaveRunnable(Runnable saveRunnable) {
        this.saveRunnable = saveRunnable;
        return this;
    }

    protected abstract void init();

    protected abstract void initLayout();

    protected abstract void initWidget();

    public Text getName() {
        return name;
    }

    public SELF setName(Text name) {
        this.name = name;
        return (SELF) this;
    }

    @Override
    public VT value() {
        return value;
    }

    public Consumer<VT> getSaveConsumer() {
        return saveConsumer;
    }

    public SELF setSaveConsumer(Consumer<VT> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return (SELF) this;
    }

    public Function<VT, Optional<Text[]>> getTooltipSupplier() {
        return tooltipSupplier;
    }

    public SELF setTooltipSupplier(Function<VT, Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return (SELF) this;
    }

    public boolean isRequiresRestartGame() {
        return requiresRestartGame;
    }

    public SELF setRequiresRestartGame(boolean requiresRestartGame) {
        this.requiresRestartGame = requiresRestartGame;
        return (SELF) this;
    }

    public Optional<VT> getDefaultValue() {
        return defaultValue == null ? Optional.empty() : Optional.ofNullable(defaultValue.get());
    }

    public SELF setDefaultValue(@Nullable Supplier<VT> defaultValue) {
        this.defaultValue = defaultValue;
        return (SELF) this;
    }

    public @Nullable Function<VT, Optional<Text>> getErrorSupplier() {
        return errorSupplier;
    }

    public SELF setErrorSupplier(@Nullable Function<VT, Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
        return (SELF) this;
    }

    public @Nullable OptionRequirement getEnableRequirement() {
        return enableRequirement;
    }

    public SELF setEnableRequirement(@Nullable OptionRequirement enableRequirement) {
        this.enableRequirement = enableRequirement;
        return (SELF) this;
    }

    public @Nullable OptionRequirement getDisplayRequirement() {
        return displayRequirement;
    }

    public SELF setDisplayRequirement(@Nullable OptionRequirement displayRequirement) {
        this.displayRequirement = displayRequirement;
        return (SELF) this;
    }

    @Override
    public void mousePress(float x, float y, int button) {
        container.mousePress(x, y, button);
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        container.mouseRelease(x, y, button);
    }

    @Override
    public void mouseMove(float x, float y) {
        container.mouseMove(x, y);
    }

    @Override
    public void mouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        container.mouseDrag(mouseX, mouseY, dragX, dragY, button);
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        container.mouseScroll(x, y, scrollX);
    }

    @Override
    public void keyPress(int keyCode, int scancode, int modifiers) {
        container.keyPress(keyCode, scancode, modifiers);
    }

    @Override
    public void keyRelease(int keyCode, int scancode, int modifiers) {
        container.keyRelease(keyCode, scancode, modifiers);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        container.charTyped(codePoint, modifiers);
    }

    public OptionContainerWidget getContainer() {
        return container;
    }
}
