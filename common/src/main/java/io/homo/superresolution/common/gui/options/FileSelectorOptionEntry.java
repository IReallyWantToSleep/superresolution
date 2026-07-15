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

import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDisplay;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class FileSelectorOptionEntry extends AbstractOptionEntry<String, FileSelectorOptionEntry> {
    private final Text dialogTitle;
    private final String[] filterPatterns;
    private final Text filterDescription;
    private final Predicate<Path> fileValidator;
    private final boolean allowClear;
    private ContainerWidget controls;
    private MaterialButton selectButton;
    private MaterialButton clearButton;

    public FileSelectorOptionEntry(
            Text name,
            String value,
            Text dialogTitle,
            String[] filterPatterns,
            Text filterDescription,
            Predicate<Path> fileValidator,
            boolean allowClear
    ) {
        super(name, value == null ? "" : value);
        this.dialogTitle = dialogTitle;
        this.filterPatterns = filterPatterns == null ? null : filterPatterns.clone();
        this.filterDescription = filterDescription;
        this.fileValidator = fileValidator;
        this.allowClear = allowClear;
    }

    @Override
    protected void init() {
        container = new OptionContainerWidget(this);
        initLayout();
        initWidget();
    }

    @Override
    protected void initLayout() {
    }

    @Override
    protected void initWidget() {
        controls = new ContainerWidget();
        controls.layout().setFlexDirection(YogaFlexDirection.ROW);
        controls.layout().setAlignItems(YogaAlign.CENTER);
        controls.layout().setGap(YogaGutter.ROW, 8);

        clearButton = MaterialButton.create(MaterialButtonSize.ExtraSmall)
                .variant(MaterialButtonVariant.Text)
                .text("")
                .icon(MaterialSymbols.iconClose());
        clearButton.setTooltip(Tooltip.withContext(
                Text.translatable("superresolution.screen.config.file.clear").getString()
        ));
        clearButton.onClick(event -> updateValue(""));

        selectButton = MaterialButton.create(MaterialButtonSize.ExtraSmall)
                .variant(MaterialButtonVariant.Outlined)
                .text(() -> Text.translatable(
                        value.isBlank()
                                ? "superresolution.screen.config.file.select"
                                : "superresolution.screen.config.file.change"
                ).getString())
                .icon(MaterialSymbols.iconFileOpen());
        selectButton.setTooltipSupplier(this::resolveTooltip);
        selectButton.onClick(event -> openFileDialog());

        if (allowClear) {
            controls.addChild(clearButton);
        }
        controls.addChild(selectButton);
        container.addControl(controls);
        updateClearButtonVisibility();
    }

    private void openFileDialog() {
        String defaultPath = value.isBlank() ? null : value;
        String filterText = filterDescription == null || filterDescription.getString().isBlank()
                ? null
                : filterDescription.getString();
        String selected;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = null;
            if (filterPatterns != null && filterPatterns.length > 0) {
                filters = stack.mallocPointer(filterPatterns.length);
                for (String pattern : filterPatterns) {
                    filters.put(stack.UTF8(pattern));
                }
                filters.flip();
            }
            selected = TinyFileDialogs.tinyfd_openFileDialog(
                    dialogTitle.getString(),
                    defaultPath,
                    filters,
                    filterText,
                    false
            );
        }
        if (selected == null || selected.isBlank()) {
            return;
        }

        try {
            updateValue(Path.of(selected).toAbsolutePath().normalize().toString());
        } catch (InvalidPathException ignored) {
        }
    }

    private void updateValue(String newValue) {
        String normalizedValue = newValue == null ? "" : newValue;
        if (Objects.equals(value, normalizedValue)) {
            return;
        }

        String oldValue = value;
        value = normalizedValue;
        if (saveConsumer != null && !saveConsumer.apply(value)) {
            value = oldValue;
            return;
        }
        if (saveRunnable != null) {
            saveRunnable.run();
        }
        updateClearButtonVisibility();
    }

    private void updateClearButtonVisibility() {
        if (!allowClear || clearButton == null) {
            return;
        }
        boolean visible = !value.isBlank();
        clearButton.setVisible(visible);
        clearButton.getLayoutNode().getStyle().setDisplay(visible ? YogaDisplay.FLEX : YogaDisplay.NONE);
        clearButton.getLayoutNode().markDirtyAndPropagate();
    }

    private boolean isValidFile(String pathValue) {
        if (pathValue == null || pathValue.isBlank()) {
            return false;
        }
        try {
            return fileValidator.test(Path.of(pathValue));
        } catch (InvalidPathException | SecurityException ignored) {
            return false;
        }
    }

    private Text statusText(String pathValue) {
        if (pathValue == null || pathValue.isBlank()) {
            return Text.translatable("superresolution.screen.config.file.none");
        }
        String key = isValidFile(pathValue)
                ? "superresolution.screen.config.file.valid"
                : "superresolution.screen.config.file.missing";
        return Text.literal(Text.translatable(key).getString().formatted(pathValue));
    }

    @Override
    public Function<String, Optional<Text[]>> getDescriptionsSupplier() {
        return pathValue -> {
            List<Text> descriptions = new ArrayList<>();
            Optional<Text[]> configuredDescriptions = descriptionsSupplier.apply(pathValue);
            configuredDescriptions.ifPresent(texts -> descriptions.addAll(Arrays.asList(texts)));
            descriptions.add(statusText(pathValue));
            return Optional.of(descriptions.toArray(Text[]::new));
        };
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public void tick(RenderContext ctx) {
        boolean enabled = updateRequirements();
        selectButton.setDisabled(!enabled);
        if (clearButton != null) {
            clearButton.setDisabled(!enabled);
        }
        updateClearButtonVisibility();
    }
}
