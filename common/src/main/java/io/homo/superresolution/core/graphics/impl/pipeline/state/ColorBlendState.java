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

package io.homo.superresolution.core.graphics.impl.pipeline.state;

import java.util.ArrayList;
import java.util.List;

public class ColorBlendState {
    private final List<ColorBlendAttachment> attachments;
    private final boolean logicOpEnable;

    public ColorBlendState(List<ColorBlendAttachment> attachments, boolean logicOpEnable) {
        this.attachments = new ArrayList<>(attachments);
        this.logicOpEnable = logicOpEnable;
    }

    public static ColorBlendState defaults() {
        List<ColorBlendAttachment> attachments = new ArrayList<>();
        attachments.add(ColorBlendAttachment.noBlend());
        return new ColorBlendState(attachments, false);
    }

    public List<ColorBlendAttachment> attachments() {
        return attachments;
    }

    public boolean logicOpEnable() {
        return logicOpEnable;
    }

    public static class Builder {
        private final List<ColorBlendAttachment> attachments = new ArrayList<>();
        private boolean logicOpEnable = false;

        public Builder addAttachment(ColorBlendAttachment attachment) {
            attachments.add(attachment);
            return this;
        }

        public Builder logicOpEnable(boolean enable) {
            this.logicOpEnable = enable;
            return this;
        }

        public ColorBlendState build() {
            if (attachments.isEmpty()) {
                attachments.add(ColorBlendAttachment.noBlend());
            }
            return new ColorBlendState(attachments, logicOpEnable);
        }
    }
}
