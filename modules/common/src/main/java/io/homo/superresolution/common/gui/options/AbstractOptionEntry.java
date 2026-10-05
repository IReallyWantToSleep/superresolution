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
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.event.GuiEventListener;
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractOptionEntry<VT, SELF> implements ValueHolder<VT>, GuiEventListener {
    protected Text name;
    protected boolean requiresRestartGame;
    protected @Nullable Supplier<VT> defaultValue = null;
    protected @Nullable Function<VT, Optional<Text>> errorSupplier;
    protected @Nullable OptionRequirement enableRequirement = null;
    protected @Nullable OptionRequirement displayRequirement = null;
    protected Function<VT, Boolean> saveConsumer = null;
    protected Runnable saveRunnable = null;
    protected Function<VT, Optional<Tooltip>> tooltipSupplier = (list) -> Optional.of(Tooltip.empty());
    protected Function<VT, Optional<Text[]>> descriptionsSupplier = (v) -> Optional.empty();
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

    protected boolean updateRequirements() {
        if (displayRequirement != null) {
            boolean shouldDisplay = displayRequirement.check();
            container.setVisible(shouldDisplay);
            container.getLayoutNode().getStyle().setDisplay(
                    shouldDisplay ? YogaDisplay.FLEX : YogaDisplay.NONE);
            container.getLayoutNode().markDirtyAndPropagate();
        } else {
            container.setVisible(true);
            container.getLayoutNode().getStyle().setDisplay(YogaDisplay.FLEX);
            container.getLayoutNode().markDirtyAndPropagate();
        }

        if (enableRequirement != null) {
            return enableRequirement.check();
        }
        return true;
    }

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

    public Function<VT, Boolean> getSaveConsumer() {
        return saveConsumer;
    }

    public SELF setSaveConsumer(Consumer<VT> saveConsumer) {
        return setSaveConsumer((VT v) -> {
            saveConsumer.accept(v);
            return true;
        });
    }

    public SELF setSaveConsumer(Function<VT, Boolean> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return (SELF) this;
    }


    public Function<VT, Optional<Tooltip>> getTooltipSupplier() {
        return tooltipSupplier;
    }

    public SELF setTooltipSupplier(Function<VT, Optional<Tooltip>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return (SELF) this;
    }

    public SELF setDescriptionsSupplier(Function<VT, Optional<Text[]>> descriptionsSupplier) {
        this.descriptionsSupplier = descriptionsSupplier;
        return (SELF) this;
    }

    public Function<VT, Optional<Text[]>> getDescriptionsSupplier() {
        return descriptionsSupplier;
    }

    protected Optional<Tooltip> resolveTooltip() {
        return this.tooltipSupplier.apply(value());
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
        if (!container.isVisible()) {
            return;
        }
        container.mousePress(x, y, button);
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        if (!container.isVisible()) {
            return;
        }
        container.mouseRelease(x, y, button);
    }

    @Override
    public void mouseMove(float x, float y) {
        if (!container.isVisible()) {
            return;
        }
        container.mouseMove(x, y);
    }

    @Override
    public void mouseDrag(float mouseX, float mouseY, float dragX, float dragY, int button) {
        if (!container.isVisible()) {
            return;
        }
        container.mouseDrag(mouseX, mouseY, dragX, dragY, button);
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        if (!container.isVisible()) {
            return;
        }
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

    public void tick(RenderContext ctx) {
    }
}
