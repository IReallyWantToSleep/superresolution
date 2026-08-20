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

package io.homo.superresolution.common.gui;

import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.minecraft.MinecraftUtils;
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.MaterialUI;
import io.homo.superresolution.core.gui.NanoVGScreen;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import io.homo.superresolution.core.gui.widgets.SpacerWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonShape;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChart;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartDataSeries;
import io.homo.superresolution.core.gui.widgets.chart.MaterialChartType;
import io.homo.superresolution.core.gui.widgets.chip.MaterialChip;
import io.homo.superresolution.core.gui.widgets.dialog.MaterialDialog;
import io.homo.superresolution.core.gui.widgets.hint.MaterialHintPane;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenu;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenuItem;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenuSelectionMode;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenuSize;
import io.homo.superresolution.core.gui.widgets.navigation.drawer.MaterialNavigationDrawer;
import io.homo.superresolution.core.gui.widgets.progress.MaterialCircularProgressIndicator;
import io.homo.superresolution.core.gui.widgets.progress.MaterialLinearProgressIndicator;
import io.homo.superresolution.core.gui.widgets.progress.MaterialProgressShape;
import io.homo.superresolution.core.gui.widgets.select.MaterialSelect;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSlider;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSliderSize;
import io.homo.superresolution.core.gui.widgets.switchs.MaterialSwitch;
import io.homo.superresolution.core.gui.widgets.textfield.MaterialTextField;
import io.homo.superresolution.core.gui.widgets.textfield.MaterialTextFieldSize;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MouseCursor;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaJustify;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaWrap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WidgetShowcaseScreen extends NanoVGScreen<WidgetShowcaseScreen> {
    private static final String DEFAULT_PAGE = "label";
    private static final float CONTENT_MAX_WIDTH = 920f;

    private final Screen parentScreen;
    private Map<String, Frame> contentFrames;
    private MaterialScheme materialScheme;
    private MaterialNavigationDrawer drawer;
    private YogaNode navigationDrawerLayout;
    private YogaNode contentLayout;
    private Frame currentContentFrame;
    private String currentContentKey = DEFAULT_PAGE;

    public WidgetShowcaseScreen(Screen parentScreen) {
        super(Component.literal("Widget Showcase"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void buildWidgets() {
        MaterialUI.setScheme(MaterialScheme.from(
                SuperResolutionConfig.getTheme(),
                SuperResolutionConfig.getThemeColor(),
                SuperResolutionConfig.getThemeSchemeVariant(),
                SuperResolutionConfig.getThemeContrastLevel()
        ));
        materialScheme = MaterialUI.Scheme;
        contentFrames = new HashMap<>();
        currentContentKey = DEFAULT_PAGE;

        getView().removeFrame(getDefaultFrame());

        navigationDrawerLayout = getView().addFrame(createNavigationFrame());
        navigationDrawerLayout.setFlexShrink(0f);
        navigationDrawerLayout.setHeightPercent(100f);
        navigationDrawerLayout.setPadding(YogaEdge.ALL, 0f);

        currentContentFrame = getOrCreateContentFrame(currentContentKey);
        contentLayout = getView().addFrame(currentContentFrame);
        configureContentLayout(contentLayout);
    }

    @Override
    public void draw(RenderContext ctx, UIInputState inputState) {
        ctx.rect(0f, 0f, ctx.viewportWidth(), ctx.viewportHeight(), materialScheme.background(), true);

        float preferredWidth = drawer.getPreferredWidth(ctx);
        float responsiveWidth = Math.max(preferredWidth, ctx.viewportWidth() * 0.19f);
        float maxWidth = Math.max(preferredWidth, ctx.viewportWidth() * 0.42f);
        navigationDrawerLayout.setWidth(Math.min(responsiveWidth, maxWidth));
        drawer.layout().setMinHeight(ctx.viewportHeight());
        view.markLayoutDirty();

        super.draw(ctx, inputState);
    }

    @Override
    public void onClose() {
        MinecraftUtils.setScreen(parentScreen);
        MouseCursor.ARROW.use();
    }

    private Frame createNavigationFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);

        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100f);

        drawer = MaterialNavigationDrawer.create()
                .addHeader("Widget Showcase", MaterialSymbols.iconWidgets())
                .addSectionHeader("Widgets")
                .addItem("Label", MaterialSymbols.iconTitle(), "label")
                .addItem("Button", MaterialSymbols.iconSmartButton(), "button")
                .addItem("Chip", MaterialSymbols.iconLabel(), "chip")
                .addItem("Switch", MaterialSymbols.iconToggleOn(), "switch")
                .addItem("Slider", MaterialSymbols.iconTune(), "slider")
                .addItem("Select", MaterialSymbols.iconArrowDropDown(), "select")
                .addItem("Text Field", MaterialSymbols.iconTextFields(), "textfield")
                .addItem("Menu", MaterialSymbols.iconMenu(), "menu")
                .addItem("Hint Pane", MaterialSymbols.iconInfo(), "hint")
                .addItem("Tooltip", MaterialSymbols.iconInfo(), "tooltip")
                .addItem("Progress", MaterialSymbols.iconDataUsage(), "progress")
                .addItem("Chart", MaterialSymbols.iconShowChart(), "chart")
                .addItem("Dialog", MaterialSymbols.iconWidgets(), "dialog")
                .addItem("Navigation Drawer", MaterialSymbols.iconViewList(), "navigation")
                .addItem("Spacer", MaterialSymbols.iconSpaceBar(), "spacer")
                .addFlexibleSpacer()
                .addDivider()
                .addItem("Back to Config", MaterialSymbols.iconSettings(), "back")
                .onItemSelected(item -> {
                    String key = String.valueOf(item.getValue());
                    if ("back".equals(key)) {
                        onClose();
                        return;
                    }
                    switchContentFrame(key);
                })
                .setSelectedByValue(DEFAULT_PAGE);
        drawer.layout().setWidthPercent(100f);
        drawer.layout().setHeightPercent(100f);
        container.addChild(drawer);

        frame.setRoot(container);
        return frame;
    }

    private Frame getOrCreateContentFrame(String key) {
        Frame cached = contentFrames.get(key);
        if (cached != null) {
            return cached;
        }

        Frame frame = switch (key) {
            case "button" -> createButtonFrame();
            case "chip" -> createChipFrame();
            case "switch" -> createSwitchFrame();
            case "slider" -> createSliderFrame();
            case "select" -> createSelectFrame();
            case "textfield" -> createTextFieldFrame();
            case "menu" -> createMenuFrame();
            case "hint" -> createHintFrame();
            case "tooltip" -> createTooltipFrame();
            case "progress" -> createProgressFrame();
            case "chart" -> createChartFrame();
            case "dialog" -> createDialogFrame();
            case "navigation" -> createNavigationDrawerShowcaseFrame();
            case "spacer" -> createSpacerFrame();
            default -> createLabelFrame();
        };
        contentFrames.put(key, frame);
        return frame;
    }

    private void switchContentFrame(String key) {
        if (key.equals(currentContentKey)) {
            return;
        }
        if (currentContentFrame != null) {
            getView().removeFrame(currentContentFrame);
        }
        currentContentKey = key;
        currentContentFrame = getOrCreateContentFrame(key);
        contentLayout = getView().addFrame(currentContentFrame);
        configureContentLayout(contentLayout);
        view.markLayoutDirty();
    }

    private void configureContentLayout(YogaNode layout) {
        layout.setFlexGrow(1f);
        layout.setHeightPercent(100f);
        layout.setPadding(YogaEdge.ALL, 0f);
    }

    private Frame createLabelFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Label",
                "Typography examples covering hierarchy, wrapping, dynamic values and disabled state."
        );

        addSectionTitle(container, "Type hierarchy");
        MaterialLabel displayLabel = MaterialLabel.create()
                .text("Display label")
                .fontSize(28f)
                .weight(600f)
                .color(materialScheme.primary());
        container.addChild(displayLabel);
        container.addChild(MaterialLabel.create()
                .text("Body text supports wrapping and follows the active Super Resolution color scheme.")
                .fontSize(16f)
                .lineHeight(22f));

        MaterialLabel wrappedLabel = MaterialLabel.create()
                .text("Wrapped labels keep longer descriptions readable when the content area becomes narrow.")
                .fontSize(14f)
                .lineHeight(20f)
                .color(materialScheme.onSurfaceVariant());
        wrappedLabel.style().wrap(true);
        wrappedLabel.layout().setWidthPercent(100f);
        container.addChild(wrappedLabel);

        addSectionTitle(container, "States");
        MaterialLabel disabledLabel = MaterialLabel.create()
                .text("Disabled label")
                .fontSize(14f);
        disabledLabel.setDisabled(true);
        container.addChild(disabledLabel);

        addSectionTitle(container, "Dynamic text");
        final int[] value = {0};
        MaterialLabel dynamicLabel = MaterialLabel.create()
                .text(() -> "Current value: " + value[0])
                .fontSize(16f)
                .color(materialScheme.primary());
        MaterialButton updateValue = MaterialButton.tonal("Increment value");
        updateValue.onClick(event -> value[0]++);
        container.addChild(dynamicLabel);
        container.addChild(updateValue);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createButtonFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Buttons",
                "Material button variants, sizes, icons, disabled state and click interaction."
        );

        addSectionTitle(container, "Variants");
        ContainerWidget variants = createWrappingRow();
        variants.addChild(MaterialButton.filled("Filled"));
        variants.addChild(MaterialButton.elevated("Elevated"));
        variants.addChild(MaterialButton.tonal("Tonal"));
        variants.addChild(MaterialButton.outlined("Outlined"));
        variants.addChild(MaterialButton.textButton("Text"));
        container.addChild(variants);

        addSectionTitle(container, "Sizes");
        ContainerWidget sizes = createWrappingRow();
        sizes.addChild(MaterialButton.filled("Extra small").size(MaterialButtonSize.ExtraSmall));
        sizes.addChild(MaterialButton.filled("Small").size(MaterialButtonSize.Small));
        sizes.addChild(MaterialButton.filled("Medium").size(MaterialButtonSize.Medium));
        sizes.addChild(MaterialButton.tonal("Large").size(MaterialButtonSize.Large));
        sizes.addChild(MaterialButton.outlined("Extra large").size(MaterialButtonSize.ExtraLarge));
        container.addChild(sizes);

        addSectionTitle(container, "Shapes");
        ContainerWidget shapes = createWrappingRow();
        shapes.addChild(MaterialButton.filled("Round").shape(MaterialButtonShape.Round));
        shapes.addChild(MaterialButton.filled("Square").shape(MaterialButtonShape.Square));
        shapes.addChild(MaterialButton.outlined("Square icon")
                .icon(MaterialSymbols.iconSettings())
                .shape(MaterialButtonShape.Square));
        container.addChild(shapes);

        addSectionTitle(container, "Icons and states");
        ContainerWidget icons = createWrappingRow();
        icons.addChild(MaterialButton.filled("Add").icon(MaterialSymbols.iconAdd()));
        icons.addChild(MaterialButton.outlined("Edit").icon(MaterialSymbols.iconEdit()));
        icons.addChild(MaterialButton.tonal("Delete").icon(MaterialSymbols.iconDelete()));
        MaterialButton disabledButton = MaterialButton.filled("Disabled");
        disabledButton.setDisabled(true);
        icons.addChild(disabledButton);
        container.addChild(icons);

        addSectionTitle(container, "Interaction");
        final int[] clickCount = {0};
        MaterialButton clickCounter = MaterialButton.elevated("Click count: 0")
                .text(() -> "Click count: " + clickCount[0]);
        clickCounter.onClick(event -> clickCount[0]++);
        container.addChild(clickCounter);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createChipFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Chip",
                "Assist, filter, input and suggestion chips with icons, selection, elevation, disabled state and removal."
        );

        addSectionTitle(container, "Chip types");
        ContainerWidget types = createWrappingRow();
        types.addChild(MaterialChip.assist("Run optimizer")
                .leadingIcon(MaterialSymbols.iconAutoAwesome()));
        types.addChild(MaterialChip.filter("High quality")
                .leadingIcon(MaterialSymbols.iconTune()));
        types.addChild(MaterialChip.input("Player one")
                .avatar(MaterialSymbols.iconPerson())
                .trailingIcon(MaterialSymbols.iconClose()));
        types.addChild(MaterialChip.suggestion("Use DLSS")
                .leadingIcon(MaterialSymbols.iconLightbulb()));
        container.addChild(types);

        addSectionTitle(container, "Selection and surfaces");
        MaterialChip filterChip = MaterialChip.filter("Vulkan backend")
                .leadingIcon(MaterialSymbols.iconFilterAlt())
                .selected(true);
        MaterialLabel filterState = createValueLabel(
                () -> filterChip.isSelected() ? "Selected" : "Unselected"
        );
        ContainerWidget selection = createWrappingRow();
        selection.addChild(filterChip);
        selection.addChild(filterState);
        selection.addChild(MaterialChip.filter("Flat filter"));
        selection.addChild(MaterialChip.suggestion("Elevated suggestion")
                .leadingIcon(MaterialSymbols.iconAutoAwesome())
                .elevated(true));
        container.addChild(selection);

        addSectionTitle(container, "Input chip actions");
        final boolean[] removed = {false};
        MaterialChip removable = MaterialChip.input("Temporary tag")
                .avatar(MaterialSymbols.iconPerson())
                .trailingIcon(MaterialSymbols.iconClose())
                .onRemove(chip -> {
                    removed[0] = true;
                    chip.setVisible(false);
                });
        MaterialLabel removalState = createValueLabel(
                () -> removed[0] ? "Removed from the input list" : "Click the close icon to remove this chip"
        );
        ContainerWidget inputActions = createWrappingRow();
        inputActions.addChild(removable);
        inputActions.addChild(removalState);
        container.addChild(inputActions);

        addSectionTitle(container, "Disabled");
        ContainerWidget disabled = createWrappingRow();
        disabled.addChild(MaterialChip.assist("Disabled assist")
                .leadingIcon(MaterialSymbols.iconInfo())
                .setDisabled(true));
        disabled.addChild(MaterialChip.filter("Disabled selected")
                .selected(true)
                .setDisabled(true));
        disabled.addChild(MaterialChip.input("Disabled input")
                .avatar(MaterialSymbols.iconPerson())
                .trailingIcon(MaterialSymbols.iconClose())
                .setDisabled(true));
        container.addChild(disabled);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createSwitchFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Switch",
                "Checked, unchecked, icon-enabled, disabled and linked switch states."
        );

        addSectionTitle(container, "Basic states");
        MaterialSwitch enabledSwitch = MaterialSwitch.create().setChecked(true);
        MaterialLabel enabledValue = createValueLabel(() -> enabledSwitch.isChecked() ? "On" : "Off");
        container.addChild(createSettingRow("Checked", "Interactive checked state", enabledValue, enabledSwitch));

        MaterialSwitch uncheckedSwitch = MaterialSwitch.create().setChecked(false);
        MaterialLabel uncheckedValue = createValueLabel(() -> uncheckedSwitch.isChecked() ? "On" : "Off");
        container.addChild(createSettingRow("Unchecked", "Interactive unchecked state", uncheckedValue, uncheckedSwitch));

        addSectionTitle(container, "Icons");
        MaterialSwitch iconSwitch = MaterialSwitch.create().setChecked(false);
        iconSwitch.style().showCheckedIconWhenEnable(true);
        iconSwitch.style().showUncheckedIconWhenEnable(true);
        MaterialLabel iconValue = createValueLabel(() -> iconSwitch.isChecked() ? "On" : "Off");
        container.addChild(createSettingRow("With icons", "Shows checked and unchecked symbols", iconValue, iconSwitch));

        MaterialSwitch disabledSwitch = MaterialSwitch.create().setChecked(true);
        disabledSwitch.setDisabled(true);
        container.addChild(createSettingRow("Disabled", "Non-interactive switch state", null, disabledSwitch));

        MaterialSwitch disabledUncheckedSwitch = MaterialSwitch.create().setChecked(false);
        disabledUncheckedSwitch.setDisabled(true);
        container.addChild(createSettingRow(
                "Disabled unchecked",
                "Disabled state without selection",
                null,
                disabledUncheckedSwitch
        ));

        addSectionTitle(container, "Linked behavior");
        MaterialSwitch masterSwitch = MaterialSwitch.create().setChecked(true);
        MaterialSwitch dependentSwitch = MaterialSwitch.create().setChecked(true);
        masterSwitch.onChange(event -> dependentSwitch.setDisabled(!masterSwitch.isChecked()));
        container.addChild(createSettingRow(
                "Master control",
                "Turns the dependent control on or off",
                createValueLabel(() -> masterSwitch.isChecked() ? "Available" : "Locked"),
                masterSwitch
        ));
        container.addChild(createSettingRow(
                "Dependent control",
                "Its enabled state follows the master control",
                createValueLabel(() -> dependentSwitch.isChecked() ? "On" : "Off"),
                dependentSwitch
        ));

        finishFrame(frame, container);
        return frame;
    }

    private Frame createSliderFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Slider",
                "Continuous, stepped, signed, differently sized and disabled slider examples."
        );

        addSectionTitle(container, "Continuous");
        MaterialSlider percentageSlider = MaterialSlider.of(0d, 1d, 0.65d, 0.05d, 520f)
                .usePercentageFormatter();
        configureSlider(percentageSlider);
        container.addChild(createLabeledControl(
                "Percentage: ",
                createValueLabel(() -> String.format("%.0f%%", percentageSlider.value().doubleValue() * 100d)),
                percentageSlider
        ));

        addSectionTitle(container, "Stepped");
        MaterialSlider steppedSlider = MaterialSlider.create(MaterialSliderSize.Small, 520f)
                .range(0, 10)
                .setStep(1)
                .setValue(4)
                .useIntegerFormatter();
        configureSlider(steppedSlider);
        container.addChild(createLabeledControl(
                "Stepped value: ",
                createValueLabel(() -> String.valueOf(steppedSlider.value().intValue())),
                steppedSlider
        ));

        addSectionTitle(container, "Sizes and ranges");
        MaterialSlider largeSlider = MaterialSlider.create(MaterialSliderSize.Large, 520f)
                .range(-1d, 1d)
                .setStep(0.1d)
                .setValue(0.2d)
                .useDecimalFormatter(1);
        configureSlider(largeSlider);
        container.addChild(createLabeledControl(
                "Large slider: ",
                createValueLabel(() -> String.format("%.1f", largeSlider.value().doubleValue())),
                largeSlider
        ));

        MaterialSlider extraSmallSlider = MaterialSlider.create(MaterialSliderSize.ExtraSmall, 520f)
                .range(0, 100)
                .setStep(5)
                .setValue(35)
                .useIntegerFormatter();
        configureSlider(extraSmallSlider);
        container.addChild(createLabeledControl(
                "Extra small: ",
                createValueLabel(() -> String.valueOf(extraSmallSlider.value().intValue())),
                extraSmallSlider
        ));

        addSectionTitle(container, "Disabled");
        MaterialSlider disabledSlider = MaterialSlider.of(0d, 1d, 0.5d, 0.1d, 520f)
                .usePercentageFormatter();
        disabledSlider.setDisabled(true);
        configureSlider(disabledSlider);
        container.addChild(createLabeledControl(
                "Fixed value: ",
                createValueLabel(() -> "50%"),
                disabledSlider
        ));

        finishFrame(frame, container);
        return frame;
    }

    private Frame createSelectFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Select",
                "Standard, compact, icon, typed-value and disabled select field examples."
        );

        addSectionTitle(container, "Standard and compact");
        ContainerWidget selects = createWrappingRow();

        MaterialSelect<String> standardSelect = MaterialSelect.<String>create()
                .label("Algorithm")
                .placeholder("Choose an algorithm")
                .supportingText("Single selection")
                .leadingIcon(MaterialSymbols.iconTune())
                .width(330f)
                .addOption("dlss", "NVIDIA DLSS")
                .addOption("fsr", "AMD FSR")
                .addOption("xess", "Intel XeSS")
                .setValue("fsr");
        configureSelect(standardSelect);
        selects.addChild(standardSelect);

        MaterialSelect<String> smallSelect = MaterialSelect.<String>create()
                .label("Quality")
                .placeholder("Choose a preset")
                .width(280f)
                .addOption("quality", "Quality", MaterialSymbols.iconCheck())
                .addOption("balanced", "Balanced", MaterialSymbols.iconTune())
                .addOption("performance", "Performance", MaterialSymbols.iconSpeed())
                .setValue("quality");
        smallSelect.style().size(MaterialTextFieldSize.Compact);
        configureSelect(smallSelect);
        selects.addChild(smallSelect);
        container.addChild(selects);

        MaterialLabel selectedValue = createValueLabel(
                () -> "Selected algorithm: " + String.valueOf(standardSelect.getValue())
        );
        container.addChild(selectedValue);

        addSectionTitle(container, "Typed values");
        MaterialSelect<Integer> numericSelect = MaterialSelect.<Integer>create()
                .label("Sample count")
                .placeholder("Choose a count")
                .supportingText("Integer-backed options with display formatting")
                .leadingIcon(MaterialSymbols.iconDataUsage())
                .width(330f)
                .displayFormatter(value -> value + " samples")
                .addOption(16, "16 samples")
                .addOption(32, "32 samples")
                .addOption(64, "64 samples")
                .setValue(32);
        configureSelect(numericSelect);
        container.addChild(numericSelect);

        addSectionTitle(container, "Disabled");
        MaterialSelect<String> disabledSelect = MaterialSelect.<String>create()
                .label("Disabled")
                .width(260f)
                .addOption("fixed", "Fixed value")
                .setValue("fixed")
                .setDisabled(true);
        configureSelect(disabledSelect);
        container.addChild(disabledSelect);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createTextFieldFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Text Field",
                "MD3 filled and outlined text fields with floating labels, supporting text, validation and keyboard editing."
        );

        addSectionTitle(container, "Filled and outlined");
        ContainerWidget fields = createWrappingRow();

        MaterialTextField filledField = MaterialTextField.filled()
                .label("Upscaling profile")
                .placeholder("Name this profile")
                .supportingText("The label floats while editing")
                .leadingIcon(MaterialSymbols.iconTune())
                .trailingIcon(MaterialSymbols.iconClear())
                .trailingIconAction(MaterialTextField::clear)
                .width(330f);
        fields.addChild(filledField);

        MaterialTextField outlinedField = MaterialTextField.outlined()
                .label("Search shaders")
                .placeholder("Type a shader name")
                .leadingIcon(MaterialSymbols.iconSearch())
                .width(330f)
                .setValue("Complementary");
        fields.addChild(outlinedField);
        container.addChild(fields);
        container.addChild(createValueLabel(() -> "Filled value: " + filledField.getValue()));

        addSectionTitle(container, "Validation and character count");
        MaterialTextField validationField = MaterialTextField.filled()
                .label("Preset name")
                .placeholder("Up to 16 characters")
                .supportingText("Names are local to this showcase")
                .maxLength(16)
                .showCharacterCount(true)
                .width(360f);
        validationField.onInput(event -> validationField.setError(((String) event.getNewValue()).isBlank()));
        validationField.errorText("A preset name is required");
        validationField.setError(false);
        container.addChild(validationField);

        addSectionTitle(container, "Disabled");
        MaterialTextField disabledField = MaterialTextField.outlined()
                .label("Locked setting")
                .supportingText("Disabled text field")
                .width(330f)
                .setValue("Balanced")
                .setDisabled(true);
        container.addChild(disabledField);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createMenuFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Menu",
                "Standard and compact menus with single, multiple and non-selectable item behavior."
        );

        addSectionTitle(container, "Selection modes");
        ContainerWidget menus = createWrappingRow();

        MaterialMenu singleMenu = MaterialMenu.create()
                .selectionMode(MaterialMenuSelectionMode.SingleAtLeastOne)
                .addItem(createMenuItem("Line chart", "line", MaterialSymbols.iconShowChart()))
                .addItem(createMenuItem("Bar chart", "bar", MaterialSymbols.iconDataUsage()))
                .addItem(createMenuItem("Curve chart", "curve", MaterialSymbols.iconScience()))
                .selectItemQuietly("line");
        configureMenu(singleMenu, 300f);
        menus.addChild(createMenuColumn("Single selection", singleMenu));

        MaterialMenu multipleMenu = MaterialMenu.create()
                .selectionMode(MaterialMenuSelectionMode.Multiple)
                .addItem(createMenuItem("CPU timing", "cpu", MaterialSymbols.iconSpeed()))
                .addItem(createMenuItem("GPU timing", "gpu", MaterialSymbols.iconShowChart()))
                .addItem(createMenuItem("Frame pacing", "pacing", MaterialSymbols.iconTune()))
                .selectItemQuietly("cpu")
                .selectItemQuietly("gpu");
        multipleMenu.style().size(MaterialMenuSize.Compact);
        configureMenu(multipleMenu, 260f);
        menus.addChild(createMenuColumn("Multiple selection", multipleMenu));
        container.addChild(menus);

        addSectionTitle(container, "Actions and disabled items");
        MaterialMenu actionMenu = MaterialMenu.create()
                .selectionMode(MaterialMenuSelectionMode.None)
                .addItem(MaterialMenuItem.create()
                        .text("Open settings")
                        .icon(MaterialSymbols.iconSettings())
                        .rightIcon(MaterialSymbols.iconArrowDropDown()))
                .addItem(MaterialMenuItem.create()
                        .text("Refresh data")
                        .icon(MaterialSymbols.iconDataUsage()))
                .addItem(MaterialMenuItem.create()
                        .text("Unavailable action")
                        .icon(MaterialSymbols.iconInfo())
                        .setDisabled(true));
        configureMenu(actionMenu, 320f);
        container.addChild(actionMenu);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createHintFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Hint Pane",
                "Informational panes with different icons, text lengths and dynamic content."
        );

        addSectionTitle(container, "Information");
        MaterialHintPane information = MaterialHintPane.create()
                .icon(MaterialSymbols.iconInfo())
                .title("Responsive showcase")
                .text("Resize the window or change GUI scale. The pane measures wrapped text and expands vertically.");
        information.layout().setWidthPercent(100f);
        container.addChild(information);

        addSectionTitle(container, "Guidance");
        MaterialHintPane guidance = MaterialHintPane.create()
                .icon(MaterialSymbols.iconTune())
                .title("Interactive controls")
                .text("Use the navigation drawer to inspect each widget independently. Controls on every page remain fully interactive.");
        guidance.layout().setWidthPercent(100f);
        container.addChild(guidance);

        addSectionTitle(container, "Dynamic content");
        final boolean[] detailed = {false};
        MaterialHintPane dynamic = MaterialHintPane.create()
                .iconProvider(() -> detailed[0] ? MaterialSymbols.iconShowChart() : MaterialSymbols.iconDataUsage())
                .titleProvider(() -> detailed[0] ? "Detailed status" : "Compact status")
                .textProvider(() -> detailed[0]
                        ? "Detailed mode can hold a longer wrapped explanation without requiring a fixed widget height."
                        : "Compact status is active.");
        dynamic.layout().setWidthPercent(100f);
        MaterialButton toggleHint = MaterialButton.tonal("Toggle content");
        toggleHint.onClick(event -> detailed[0] = !detailed[0]);
        container.addChild(dynamic);
        container.addChild(toggleHint);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createTooltipFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Tooltip",
                "Hover interactive controls to inspect short, wrapped, dynamic and option-level tooltips."
        );

        addSectionTitle(container, "Text lengths");
        ContainerWidget textLengths = createWrappingRow();
        MaterialButton shortTooltip = MaterialButton.filled("Short tooltip")
                .setTooltip(Tooltip.withContext("A concise tooltip."));
        MaterialButton wrappedTooltip = MaterialButton.outlined("Wrapped tooltip")
                .setTooltip(Tooltip.withContext(
                        "Long tooltip text wraps automatically and remains inside the visible viewport near screen edges."
                ));
        textLengths.addChild(shortTooltip);
        textLengths.addChild(wrappedTooltip);
        container.addChild(textLengths);

        addSectionTitle(container, "Icon controls");
        ContainerWidget iconControls = createWrappingRow();
        iconControls.addChild(MaterialButton.tonal("Settings")
                .icon(MaterialSymbols.iconSettings())
                .setTooltip(Tooltip.withContext("Open widget settings.")));
        iconControls.addChild(MaterialButton.tonal("Performance")
                .icon(MaterialSymbols.iconSpeed())
                .setTooltip(Tooltip.withContext("Inspect current performance metrics.")));
        iconControls.addChild(MaterialButton.tonal("Information")
                .icon(MaterialSymbols.iconInfo())
                .setTooltip(Tooltip.withContext("Show additional context.")));
        container.addChild(iconControls);

        addSectionTitle(container, "Dynamic tooltip");
        MaterialSwitch detailedTooltip = MaterialSwitch.create().setChecked(false);
        MaterialButton dynamicTooltip = MaterialButton.elevated("Hover for current mode")
                .setTooltipSupplier(() -> Optional.of(Tooltip.withContext(
                        detailedTooltip.isChecked()
                                ? "Detailed mode is enabled. This tooltip is supplied dynamically from current widget state."
                                : "Compact mode is enabled."
                )));
        container.addChild(createSettingRow(
                "Detailed tooltip",
                "Changes the tooltip content on the button below",
                createValueLabel(() -> detailedTooltip.isChecked() ? "Detailed" : "Compact"),
                detailedTooltip
        ));
        container.addChild(dynamicTooltip);

        addSectionTitle(container, "Select option tooltips");
        MaterialSelect<String> tooltipSelect = MaterialSelect.<String>create()
                .label("Rendering backend")
                .placeholder("Choose a backend")
                .supportingText("Hover menu options to inspect their descriptions")
                .leadingIcon(MaterialSymbols.iconTune())
                .width(360f)
                .addOption(
                        "opengl",
                        "OpenGL",
                        () -> Optional.of(Tooltip.withContext("Use the OpenGL rendering backend."))
                )
                .addOption(
                        "vulkan",
                        "Vulkan",
                        () -> Optional.of(Tooltip.withContext("Use the Vulkan rendering backend when available."))
                )
                .setValue("opengl")
                .setTooltip(Tooltip.withContext("Open the field to view option-specific tooltips."));
        configureSelect(tooltipSelect);
        container.addChild(tooltipSelect);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createProgressFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Progress Indicator",
                "M3 Expressive flat and wavy progress indicators in linear and circular variants."
        );

        addSectionTitle(container, "Linear flat - 4dp track");
        container.addChild(createLabeledControl("25 percent", createProgressIndicator(0.25f)));
        container.addChild(createLabeledControl("65 percent", createProgressIndicator(0.65f)));
        container.addChild(createLabeledControl("Complete", createProgressIndicator(1f)));

        addSectionTitle(container, "Ranged value");
        MaterialLinearProgressIndicator ranged = new MaterialLinearProgressIndicator()
                .setProgress(0.25f, 0.75f);
        configureProgressIndicator(ranged);
        container.addChild(createLabeledControl("Range 25-75 percent", ranged));

        addSectionTitle(container, "Linear flat - 8dp track");
        container.addChild(createLabeledControl("65 percent", createThickProgressIndicator(0.65f, MaterialProgressShape.FLAT)));

        addSectionTitle(container, "Linear wavy - 4dp track");
        container.addChild(createLabeledControl("65 percent", createWavyProgressIndicator(0.65f, MaterialLinearProgressIndicator.DEFAULT_TRACK_THICKNESS)));

        addSectionTitle(container, "Linear wavy - 8dp track");
        container.addChild(createLabeledControl("65 percent", createWavyProgressIndicator(0.65f, 8f)));

        addSectionTitle(container, "Indeterminate");
        container.addChild(createLabeledControl("Flat", createIndeterminateLinear(MaterialProgressShape.FLAT, MaterialLinearProgressIndicator.DEFAULT_TRACK_THICKNESS)));
        container.addChild(createLabeledControl("Wavy", createIndeterminateLinear(MaterialProgressShape.WAVY, MaterialLinearProgressIndicator.DEFAULT_TRACK_THICKNESS)));

        addSectionTitle(container, "Disabled");
        MaterialLinearProgressIndicator disabled = createProgressIndicator(0.55f);
        disabled.setDisabled(true);
        container.addChild(createLabeledControl("Disabled flat at 55 percent", disabled));
        MaterialLinearProgressIndicator disabledWavy = createWavyProgressIndicator(0.55f, MaterialLinearProgressIndicator.DEFAULT_TRACK_THICKNESS);
        disabledWavy.setDisabled(true);
        container.addChild(createLabeledControl("Disabled wavy at 55 percent", disabledWavy));

        addSectionTitle(container, "Circular determinate");
        ContainerWidget circularRow = createCircularRow(
                createCircularIndicator("40dp flat", 0.65f, MaterialProgressShape.FLAT, MaterialCircularProgressIndicator.DEFAULT_TRACK_THICKNESS, MaterialCircularProgressIndicator.SIZE_FLAT_DEFAULT),
                createCircularIndicator("44dp thick flat", 0.65f, MaterialProgressShape.FLAT, 8f, MaterialCircularProgressIndicator.SIZE_FLAT_THICK),
                createCircularIndicator("48dp wavy", 0.65f, MaterialProgressShape.WAVY, MaterialCircularProgressIndicator.DEFAULT_TRACK_THICKNESS, MaterialCircularProgressIndicator.SIZE_WAVY_DEFAULT),
                createCircularIndicator("52dp thick wavy", 0.65f, MaterialProgressShape.WAVY, 8f, MaterialCircularProgressIndicator.SIZE_WAVY_THICK)
        );
        container.addChild(circularRow);

        addSectionTitle(container, "Circular indeterminate");
        ContainerWidget circularIndeterminateRow = createCircularRow(
                createIndeterminateCircular("Flat", MaterialProgressShape.FLAT, MaterialCircularProgressIndicator.DEFAULT_TRACK_THICKNESS, MaterialCircularProgressIndicator.SIZE_FLAT_DEFAULT),
                createIndeterminateCircular("Wavy", MaterialProgressShape.WAVY, MaterialCircularProgressIndicator.DEFAULT_TRACK_THICKNESS, MaterialCircularProgressIndicator.SIZE_WAVY_DEFAULT)
        );
        container.addChild(circularIndeterminateRow);

        addSectionTitle(container, "Interactive value");
        MaterialLinearProgressIndicator progress = createProgressIndicator(0.4f);
        MaterialLinearProgressIndicator wavyProgress = createWavyProgressIndicator(0.4f, MaterialLinearProgressIndicator.DEFAULT_TRACK_THICKNESS);
        MaterialCircularProgressIndicator circularProgress = new MaterialCircularProgressIndicator().setProgress(0.4f);
        circularProgress.setElementWidth(MaterialCircularProgressIndicator.SIZE_FLAT_DEFAULT);
        circularProgress.setElementHeight(MaterialCircularProgressIndicator.SIZE_FLAT_DEFAULT);
        MaterialSlider progressSlider = MaterialSlider.of(0d, 1d, 0.4d, 0.05d, 520f)
                .usePercentageFormatter();
        configureSlider(progressSlider);
        progressSlider.onChange(event -> {
            float value = ((Number) event.getNewValue()).floatValue();
            progress.setProgress(value);
            wavyProgress.setProgress(value);
            circularProgress.setProgress(value);
        });
        container.addChild(createLabeledControl("Controlled by the slider below", progress));
        container.addChild(createLabeledControl("Wavy follows the same value", wavyProgress));
        container.addChild(createLabeledControl("Circular follows the same value", circularProgress));
        container.addChild(progressSlider);

        finishFrame(frame, container);
        return frame;
    }

    private MaterialLinearProgressIndicator createThickProgressIndicator(float progress, MaterialProgressShape shape) {
        MaterialLinearProgressIndicator indicator = new MaterialLinearProgressIndicator()
                .setProgress(progress)
                .setTrackThickness(8f)
                .setShape(shape);
        configureProgressIndicator(indicator);
        return indicator;
    }

    private MaterialLinearProgressIndicator createWavyProgressIndicator(float progress, float trackThickness) {
        MaterialLinearProgressIndicator indicator = new MaterialLinearProgressIndicator()
                .setProgress(progress)
                .setTrackThickness(trackThickness)
                .setShape(MaterialProgressShape.WAVY);
        configureProgressIndicator(indicator);
        return indicator;
    }

    private MaterialLinearProgressIndicator createIndeterminateLinear(MaterialProgressShape shape, float trackThickness) {
        MaterialLinearProgressIndicator indicator = new MaterialLinearProgressIndicator()
                .setIndeterminate(true)
                .setTrackThickness(trackThickness)
                .setShape(shape);
        configureProgressIndicator(indicator);
        return indicator;
    }

    private ContainerWidget createCircularIndicator(String label, float progress, MaterialProgressShape shape,
                                                    float trackThickness, float size) {
        MaterialCircularProgressIndicator indicator = new MaterialCircularProgressIndicator()
                .setProgress(progress)
                .setTrackThickness(trackThickness)
                .setShape(shape);
        indicator.setElementWidth(size);
        indicator.setElementHeight(size);
        return createCircularLabeledControl(label, indicator);
    }

    private ContainerWidget createIndeterminateCircular(String label, MaterialProgressShape shape,
                                                        float trackThickness, float size) {
        MaterialCircularProgressIndicator indicator = new MaterialCircularProgressIndicator()
                .setIndeterminate(true)
                .setTrackThickness(trackThickness)
                .setShape(shape);
        indicator.setElementWidth(size);
        indicator.setElementHeight(size);
        return createCircularLabeledControl(label, indicator);
    }

    private ContainerWidget createCircularLabeledControl(String label, AbstractWidget<?> control) {
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setGap(YogaGutter.COLUMN, 12f);
        container.layout().setMargin(YogaEdge.BOTTOM, 8f);
        container.layout().setAlignItems(YogaAlign.CENTER);
        container.addChild(MaterialLabel.create().text(label).fontSize(14f));
        container.addChild(control);
        return container;
    }

    private ContainerWidget createCircularRow(ContainerWidget... items) {
        ContainerWidget row = new ContainerWidget();
        row.layout().setFlexDirection(YogaFlexDirection.ROW);
        row.layout().setGap(YogaGutter.ROW, 24f);
        row.layout().setAlignItems(YogaAlign.FLEX_END);
        row.layout().setMargin(YogaEdge.BOTTOM, 8f);
        for (ContainerWidget item : items) {
            row.addChild(item);
        }
        return row;
    }

    private Frame createChartFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Chart",
                "Curve, line and bar series with legends, grids, averages and interactive samples."
        );

        addSectionTitle(container, "Frame time series");
        MaterialChartDataSeries cpuSeries = new MaterialChartDataSeries(
                "CPU (ms)",
                Color.from("#4FC3F7"),
                MaterialChartType.Curve,
                32
        );
        MaterialChartDataSeries gpuSeries = new MaterialChartDataSeries(
                "GPU (ms)",
                Color.from("#BA53FF"),
                MaterialChartType.Line,
                32
        );
        seedChartData(cpuSeries, gpuSeries);

        MaterialChart chart = MaterialChart.create()
                .title("Frame time samples")
                .addSeries(cpuSeries)
                .addSeries(gpuSeries)
                .range(0f, 24f)
                .valueFormatter(value -> String.format("%.1f ms", value));
        chart.style()
                .showAverage(true)
                .showGrid(true)
                .showLegend(true);
        chart.layout().setWidthPercent(100f);
        chart.setElementHeight(240f);
        container.addChild(chart);

        final int[] sampleIndex = {12};
        ContainerWidget chartActions = createWrappingRow();
        MaterialButton addSample = MaterialButton.tonal("Add sample")
                .icon(MaterialSymbols.iconAdd());
        addSample.onClick(event -> {
            float cpu = 9f + (float) Math.sin(sampleIndex[0] * 0.55f) * 3f;
            float gpu = 12f + (float) Math.cos(sampleIndex[0] * 0.42f) * 4f;
            cpuSeries.addDataPoint(cpu);
            gpuSeries.addDataPoint(gpu);
            sampleIndex[0]++;
        });
        MaterialButton clearSamples = MaterialButton.outlined("Clear");
        clearSamples.onClick(event -> chart.clearAllData());
        chartActions.addChild(addSample);
        chartActions.addChild(clearSamples);
        container.addChild(chartActions);

        addSectionTitle(container, "Bar series");
        MaterialChartDataSeries usageSeries = new MaterialChartDataSeries(
                "GPU usage",
                Color.from("#81C784"),
                MaterialChartType.Bar,
                8
        );
        usageSeries.setData(new float[]{32f, 48f, 63f, 57f, 74f, 69f, 82f, 76f});
        MaterialChart usageChart = MaterialChart.create()
                .title("GPU utilization")
                .addSeries(usageSeries)
                .range(0f, 100f)
                .valueFormatter(value -> String.format("%.0f%%", value));
        usageChart.style()
                .showAverage(true)
                .showGrid(true)
                .showLegend(true);
        usageChart.layout().setWidthPercent(100f);
        usageChart.setElementHeight(220f);
        container.addChild(usageChart);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createDialogFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Dialog",
                "Modal dialog examples covering information, confirmation and custom content."
        );

        addSectionTitle(container, "Information dialog");
        MaterialButton openInformation = MaterialButton.filled("Open information dialog")
                .icon(MaterialSymbols.iconInfo());
        openInformation.onClick(event -> {
            MaterialDialog dialog = MaterialDialog.create()
                    .icon(MaterialSymbols.iconInfo())
                    .headline("Information")
                    .supportingText("This modal demonstrates the scrim, headline, supporting text and a single action.")
                    .addAction("Close", MaterialButtonVariant.Tonal, MaterialDialog::dismiss);
            getView().showDialog(dialog);
        });
        container.addChild(openInformation);

        addSectionTitle(container, "Confirmation dialog");
        MaterialButton openConfirmation = MaterialButton.tonal("Open confirmation dialog")
                .icon(MaterialSymbols.iconCheck());
        openConfirmation.onClick(event -> {
            MaterialDialog dialog = MaterialDialog.create()
                    .icon(MaterialSymbols.iconCheck())
                    .headline("Apply changes?")
                    .supportingText("The confirmation variant presents two actions and can be dismissed from the scrim.")
                    .scrimDismiss(true)
                    .addAction("Cancel", MaterialButtonVariant.Text, MaterialDialog::dismiss)
                    .addAction("Apply", MaterialButtonVariant.Tonal, MaterialDialog::dismiss);
            getView().showDialog(dialog);
        });
        container.addChild(openConfirmation);

        addSectionTitle(container, "Custom content");
        MaterialButton openCustom = MaterialButton.outlined("Open content dialog")
                .icon(MaterialSymbols.iconWidgets());
        openCustom.onClick(event -> {
            ContainerWidget content = new ContainerWidget();
            content.layout().setFlexDirection(YogaFlexDirection.COLUMN);
            content.layout().setGap(YogaGutter.COLUMN, 12f);
            content.addChild(MaterialLabel.create()
                    .text("Dialogs can host arbitrary widget content.")
                    .fontSize(15f));
            MaterialLinearProgressIndicator dialogProgress =
                    new MaterialLinearProgressIndicator().setProgress(0.72f);
            configureProgressIndicator(dialogProgress);
            content.addChild(dialogProgress);
            MaterialDialog dialog = MaterialDialog.create()
                    .icon(MaterialSymbols.iconWidgets())
                    .headline("Custom content")
                    .content(content)
                    .divider(true)
                    .addAction("Done", MaterialButtonVariant.Tonal, MaterialDialog::dismiss);
            getView().showDialog(dialog);
        });
        container.addChild(openCustom);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createNavigationDrawerShowcaseFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Material Navigation Drawer",
                "The screen drawer is the primary live example; this compact drawer demonstrates sections and selection."
        );

        final String[] selected = {"General"};
        MaterialNavigationDrawer exampleDrawer = MaterialNavigationDrawer.create()
                .addHeader("Example Drawer", MaterialSymbols.iconMenu())
                .addSectionHeader("Pages")
                .addItem("General", MaterialSymbols.iconSettings(), "General")
                .addItem("Performance", MaterialSymbols.iconSpeed(), "Performance")
                .addItem("Appearance", MaterialSymbols.iconPalette(), "Appearance")
                .addDivider()
                .addItem("About", MaterialSymbols.iconInfo(), "About")
                .onItemSelected(item -> selected[0] = String.valueOf(item.getValue()))
                .setSelectedByValue("General");
        exampleDrawer.layout().setWidth(340f);
        exampleDrawer.layout().setMaxWidthPercent(100f);
        exampleDrawer.layout().setHeight(390f);

        container.addChild(createValueLabel(() -> "Selected item: " + selected[0]));
        container.addChild(exampleDrawer);

        finishFrame(frame, container);
        return frame;
    }

    private Frame createSpacerFrame() {
        ScrollableFrame frame = createScrollableFrame();
        ContainerWidget container = createPageContainer();
        addPageHeader(
                container,
                "Spacer Widget",
                "Invisible layout widgets creating deterministic horizontal, vertical and square spacing."
        );

        addSectionTitle(container, "Horizontal spacing");
        ContainerWidget horizontal = new ContainerWidget();
        horizontal.layout().setFlexDirection(YogaFlexDirection.ROW);
        horizontal.layout().setWrap(YogaWrap.WRAP);
        horizontal.layout().setAlignItems(YogaAlign.CENTER);
        horizontal.layout().setWidthPercent(100f);
        horizontal.addChild(MaterialButton.tonal("Left"));
        horizontal.addChild(SpacerWidget.horizontal(48f));
        horizontal.addChild(MaterialButton.tonal("48 px gap"));
        horizontal.addChild(SpacerWidget.horizontal(24f));
        horizontal.addChild(MaterialButton.tonal("24 px gap"));
        container.addChild(horizontal);

        addSectionTitle(container, "Vertical spacing");
        ContainerWidget vertical = new ContainerWidget();
        vertical.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        vertical.layout().setWidthPercent(100f);
        vertical.addChild(MaterialLabel.create().text("First row").fontSize(16f));
        vertical.addChild(SpacerWidget.vertical(24f));
        vertical.addChild(MaterialLabel.create().text("24 px below").fontSize(16f));
        vertical.addChild(SpacerWidget.vertical(48f));
        vertical.addChild(MaterialLabel.create().text("48 px below").fontSize(16f));
        container.addChild(vertical);

        addSectionTitle(container, "Square spacer");
        ContainerWidget squareRow = createWrappingRow();
        squareRow.addChild(MaterialButton.outlined("Before"));
        squareRow.addChild(SpacerWidget.square(56f));
        squareRow.addChild(MaterialButton.outlined("After 56 x 56"));
        container.addChild(squareRow);

        finishFrame(frame, container);
        return frame;
    }

    private ScrollableFrame createScrollableFrame() {
        ScrollableFrame frame = new ScrollableFrame();
        frame.setContentPadding(28f, 0f, 28f, 0f);
        frame.setVerticalScrollEnabled(true);
        frame.setHorizontalScrollEnabled(false);
        return frame;
    }

    private ContainerWidget createPageContainer() {
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100f);
        container.layout().setMaxWidth(CONTENT_MAX_WIDTH);
        container.layout().setGap(YogaGutter.COLUMN, 24f);
        container.layout().setAlignItems(YogaAlign.FLEX_START);
        container.layout().setAlignSelf(YogaAlign.CENTER);
        return container;
    }

    private void addPageHeader(ContainerWidget container, String title, String description) {
        container.addChild(SpacerWidget.vertical(24f));
        MaterialLabel titleLabel = MaterialLabel.create()
                .text(title)
                .fontSize(28f)
                .lineHeight(34f)
                .weight(600f)
                .color(materialScheme.primary());
        titleLabel.layout().setWidthPercent(100f);
        container.addChild(titleLabel);

        MaterialLabel descriptionLabel = MaterialLabel.create()
                .text(description)
                .fontSize(15f)
                .lineHeight(21f)
                .color(materialScheme.onSurfaceVariant());
        descriptionLabel.style().wrap(true);
        descriptionLabel.layout().setWidthPercent(100f);
        descriptionLabel.layout().setMargin(YogaEdge.BOTTOM, 18f);
        container.addChild(descriptionLabel);
    }

    private void addSectionTitle(ContainerWidget container, String title) {
        MaterialLabel label = MaterialLabel.create()
                .text(title)
                .fontSize(19f)
                .lineHeight(24f)
                .weight(600f)
                .color(materialScheme.onSurface());
        label.layout().setMargin(YogaEdge.TOP, 18f);
        label.layout().setMargin(YogaEdge.BOTTOM, 4f);
        label.layout().setWidthPercent(100f);
        container.addChild(label);
    }

    private ContainerWidget createWrappingRow() {
        ContainerWidget row = new ContainerWidget();
        row.layout().setFlexDirection(YogaFlexDirection.ROW);
        row.layout().setWrap(YogaWrap.WRAP);
        row.layout().setGap(YogaGutter.ALL, 16f);
        row.layout().setWidthPercent(100f);
        row.layout().setAlignItems(YogaAlign.CENTER);
        return row;
    }

    private ContainerWidget createSettingRow(
            String label,
            String description,
            MaterialLabel value,
            MaterialSwitch materialSwitch
    ) {
        ContainerWidget row = new ContainerWidget();
        row.layout().setFlexDirection(YogaFlexDirection.ROW);
        row.layout().setWidthPercent(100f);
        row.layout().setAlignItems(YogaAlign.CENTER);
        row.layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
        row.layout().setGap(YogaGutter.ROW, 20f);
        row.layout().setPadding(YogaEdge.VERTICAL, 10f);

        ContainerWidget text = new ContainerWidget();
        text.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        text.layout().setFlexGrow(1f);
        text.layout().setGap(YogaGutter.COLUMN, 3f);
        MaterialLabel labelWidget = MaterialLabel.create().text(label).fontSize(16f);
        MaterialLabel descriptionWidget = MaterialLabel.create()
                .text(description)
                .fontSize(13f)
                .lineHeight(18f)
                .color(materialScheme.onSurfaceVariant());
        descriptionWidget.style().wrap(true);
        descriptionWidget.layout().setWidthPercent(100f);
        text.addChild(labelWidget);
        text.addChild(descriptionWidget);
        row.addChild(text);

        if (value != null) {
            row.addChild(value);
        }
        row.addChild(materialSwitch);
        return row;
    }

    private ContainerWidget createLabeledControl(String label, AbstractWidget<?> control) {
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100f);
        container.layout().setGap(YogaGutter.COLUMN, 12f);
        container.layout().setMargin(YogaEdge.BOTTOM, 8f);
        container.addChild(MaterialLabel.create().text(label).fontSize(14f));
        container.addChild(control);
        return container;
    }

    private ContainerWidget createLabeledControl(String prefix, MaterialLabel value, MaterialSlider slider) {
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100f);
        container.layout().setGap(YogaGutter.COLUMN, 12f);
        container.layout().setMargin(YogaEdge.BOTTOM, 8f);

        ContainerWidget labelRow = new ContainerWidget();
        labelRow.layout().setFlexDirection(YogaFlexDirection.ROW);
        labelRow.layout().setGap(YogaGutter.ROW, 4f);
        labelRow.addChild(MaterialLabel.create().text(prefix).fontSize(14f));
        labelRow.addChild(value);
        container.addChild(labelRow);
        container.addChild(slider);
        return container;
    }

    private ContainerWidget createMenuColumn(String title, MaterialMenu menu) {
        ContainerWidget column = new ContainerWidget();
        column.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        column.layout().setGap(YogaGutter.COLUMN, 12f);
        column.layout().setFlexGrow(1f);
        column.addChild(MaterialLabel.create().text(title).fontSize(14f));
        column.addChild(menu);
        return column;
    }

    private MaterialLabel createValueLabel(java.util.function.Supplier<String> valueSupplier) {
        return MaterialLabel.create()
                .text(valueSupplier)
                .fontSize(14f)
                .color(materialScheme.primary());
    }

    private MaterialLinearProgressIndicator createProgressIndicator(float progress) {
        MaterialLinearProgressIndicator indicator = new MaterialLinearProgressIndicator().setProgress(progress);
        configureProgressIndicator(indicator);
        return indicator;
    }

    private void configureProgressIndicator(MaterialLinearProgressIndicator indicator) {
        indicator.layout().setWidthPercent(100f);
        float height = indicator.getTrackThickness();
        if (indicator.getShape() == MaterialProgressShape.WAVY) {
            height += 2f * indicator.getWaveAmplitude();
        }
        indicator.setElementHeight(height);
    }

    private void configureSlider(MaterialSlider slider) {
        slider.layout().setWidthPercent(100f);
        slider.layout().setMaxWidth(520f);
    }

    private void configureSelect(MaterialSelect<?> select) {
        select.layout().setMaxWidth(360f);
    }

    private void configureMenu(MaterialMenu menu, float width) {
        menu.layout().setWidth(width);
        menu.layout().setMaxWidthPercent(100f);
    }

    private MaterialMenuItem createMenuItem(String text, Object value, io.homo.superresolution.core.gui.MaterialSymbol icon) {
        return MaterialMenuItem.create()
                .text(text)
                .icon(icon)
                .value(value)
                .selectable(true);
    }

    private void seedChartData(MaterialChartDataSeries cpu, MaterialChartDataSeries gpu) {
        for (int i = 0; i < 12; i++) {
            cpu.addDataPoint(9f + (float) Math.sin(i * 0.55f) * 3f);
            gpu.addDataPoint(12f + (float) Math.cos(i * 0.42f) * 4f);
        }
    }

    private void finishFrame(ScrollableFrame frame, ContainerWidget container) {
        container.addChild(SpacerWidget.vertical(24f));
        frame.setRoot(container);
    }
}
