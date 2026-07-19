/*
 * Anemone Mod
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

package io.homo.superresolution.common.framegeneration;

import io.homo.superresolution.core.streamline.StreamlineTypes;

public enum FrameGenerationMode {
	OFF(StreamlineTypes.DlssGMode.OFF, 0, "anemone.screen.config.options.frame_generation.off"),
	AUTO(StreamlineTypes.DlssGMode.AUTO, 1, "anemone.screen.config.options.frame_generation.auto"),
	X2(StreamlineTypes.DlssGMode.ON, 1, "anemone.screen.config.options.frame_generation.x2"),
	X3(StreamlineTypes.DlssGMode.ON, 2, "anemone.screen.config.options.frame_generation.x3"),
	X4(StreamlineTypes.DlssGMode.ON, 3, "anemone.screen.config.options.frame_generation.x4"),
	X5(StreamlineTypes.DlssGMode.ON, 4, "anemone.screen.config.options.frame_generation.x5"),
	X6(StreamlineTypes.DlssGMode.ON, 5, "anemone.screen.config.options.frame_generation.x6");

	private final int nativeMode;
	private final int generatedFrameCount;
	private final String translationKey;

	FrameGenerationMode(int nativeMode, int generatedFrameCount, String translationKey) {
		this.nativeMode = nativeMode;
		this.generatedFrameCount = generatedFrameCount;
		this.translationKey = translationKey;
	}

	public int nativeMode() {
		return nativeMode;
	}

	public int generatedFrameCount() {
		return generatedFrameCount;
	}

	public boolean isEnabled() {
		return generatedFrameCount > 0;
	}

	public String translationKey() {
		return translationKey;
	}
}
