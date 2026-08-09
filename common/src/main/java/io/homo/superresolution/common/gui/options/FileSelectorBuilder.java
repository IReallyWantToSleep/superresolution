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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

public class FileSelectorBuilder extends AbstractOptionBuilder<String, FileSelectorOptionEntry, FileSelectorBuilder> {
    protected Text dialogTitle = Text.translatable("superresolution.screen.config.file.dialog.select");
    protected String[] filterPatterns = null;
    protected Text filterDescription = Text.empty();
    protected Predicate<Path> fileValidator = Files::isRegularFile;
    protected boolean allowClear = true;

    public FileSelectorBuilder(Text name, String value) {
        super(name, value == null ? "" : value);
    }

    @Override
    public FileSelectorOptionEntry build() {
        FileSelectorOptionEntry entry = new FileSelectorOptionEntry(
                name,
                value,
                dialogTitle,
                filterPatterns,
                filterDescription,
                fileValidator,
                allowClear
        );
        return finishBuild(entry);
    }

    public FileSelectorBuilder setDialogTitle(Text dialogTitle) {
        this.dialogTitle = dialogTitle == null
                ? Text.translatable("superresolution.screen.config.file.dialog.select")
                : dialogTitle;
        return this;
    }

    public FileSelectorBuilder setFilterPatterns(String... filterPatterns) {
        this.filterPatterns = filterPatterns == null || filterPatterns.length == 0
                ? null
                : filterPatterns.clone();
        return this;
    }

    public FileSelectorBuilder setFilterDescription(Text filterDescription) {
        this.filterDescription = filterDescription == null ? Text.empty() : filterDescription;
        return this;
    }

    public FileSelectorBuilder setFileValidator(Predicate<Path> fileValidator) {
        this.fileValidator = fileValidator == null ? Files::isRegularFile : fileValidator;
        return this;
    }

    public FileSelectorBuilder setAllowClear(boolean allowClear) {
        this.allowClear = allowClear;
        return this;
    }
}
