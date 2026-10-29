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

import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;

import java.util.function.Consumer;

public class DialogAction {
    private final String text;
    private final Consumer<MaterialDialog> onClick;
    private final MaterialButtonVariant variant;

    public DialogAction(String text, MaterialButtonVariant variant, Consumer<MaterialDialog> onClick) {
        this.text = text;
        this.onClick = onClick;
        this.variant = variant;
    }

    public static DialogAction of(String text, Consumer<MaterialDialog> onClick) {
        return new DialogAction(text, MaterialButtonVariant.Text, onClick);
    }

    public static DialogAction of(String text, MaterialButtonVariant variant, Consumer<MaterialDialog> onClick) {
        return new DialogAction(text, variant, onClick);
    }

    public String getText() {
        return text;
    }

    public Consumer<MaterialDialog> getOnClick() {
        return onClick;
    }

    public MaterialButtonVariant getVariant() {
        return variant;
    }
}
