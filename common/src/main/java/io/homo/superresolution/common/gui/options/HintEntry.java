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
import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.widgets.hint.MaterialHintPane;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaJustify;

import java.util.function.Supplier;

public class HintEntry extends AbstractOptionEntry<Void, HintEntry> {
    private MaterialHintPane hintPane;
    private Supplier<MaterialSymbol> iconSupplier = MaterialSymbols::iconInfo;
    private Supplier<String> titleSupplier = () -> "";
    private Supplier<String> textSupplier = () -> "";

    public HintEntry(Text name) {
        super(name, null);
    }

    public HintEntry setIconProvider(Supplier<MaterialSymbol> iconSupplier) {
        this.iconSupplier = iconSupplier;
        return this;
    }

    public HintEntry setTitleProvider(Supplier<String> titleSupplier) {
        this.titleSupplier = titleSupplier;
        return this;
    }

    public HintEntry setTextProvider(Supplier<String> textSupplier) {
        this.textSupplier = textSupplier;
        return this;
    }

    @Override
    protected void init() {
        this.container = new HintContainerWidget(this);
        initLayout();
        initWidget();
    }

    @Override
    protected void initLayout() {
    }

    @Override
    protected void initWidget() {
        hintPane = MaterialHintPane.create()
                .iconProvider(iconSupplier)
                .titleProvider(titleSupplier)
                .textProvider(textSupplier);
        hintPane.style().iconSize(20f);
        hintPane.style().textHorizontalPadding(0f);
        hintPane.layout().setWidthPercent(100);
        container.addChild(hintPane);
    }

    @Override
    public Void value() {
        return null;
    }

    @Override
    public void tick(RenderContext ctx) {
        updateRequirements();
    }

    private static class HintContainerWidget extends OptionContainerWidget {

        HintContainerWidget(HintEntry entry) {
            super(entry);
            layout().setFlexDirection(YogaFlexDirection.COLUMN);
            layout().setAlignItems(YogaAlign.FLEX_START);
            layout().setJustifyContent(YogaJustify.FLEX_START);
            layout().setPadding(YogaEdge.ALL, 0);
            layout().setMinHeight(0);
            this.removeChild(nameLabel);
            this.removeChild(leftContainer);
            this.removeChild(rightContainer);
            this.removeChild(descriptionLabel);
        }
    }
}