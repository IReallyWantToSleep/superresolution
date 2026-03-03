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

package io.homo.superresolution.core.gui;

import io.homo.superresolution.core.gui.google.material.dynamiccolor.DynamicScheme;
import io.homo.superresolution.core.gui.google.material.hct.Hct;
import io.homo.superresolution.core.gui.google.material.scheme.*;
import io.homo.superresolution.core.utils.Color;

public class MaterialScheme {
    public static final MaterialScheme defaultLight = from(
            MaterialTheme.Light,
            Color.from("#6750A4")
    );
    public static final MaterialScheme defaultDark = from(
            MaterialTheme.Dark,
            Color.from("#6750A4")
    );
    private final Color primary;
    private final Color onPrimary;
    private final Color primaryContainer;
    private final Color onPrimaryContainer;
    private final Color secondary;
    private final Color onSecondary;
    private final Color secondaryContainer;
    private final Color onSecondaryContainer;
    private final Color tertiary;
    private final Color onTertiary;
    private final Color tertiaryContainer;
    private final Color onTertiaryContainer;
    private final Color error;
    private final Color onError;
    private final Color errorContainer;
    private final Color onErrorContainer;
    private final Color background;
    private final Color onBackground;
    private final Color surface;
    private final Color onSurface;
    private final Color surfaceVariant;
    private final Color onSurfaceVariant;
    private final Color outline;
    private final Color outlineVariant;
    private final Color shadow;
    private final Color scrim;
    private final Color inverseSurface;
    private final Color inverseOnSurface;
    private final Color inversePrimary;
    private final Color primaryFixed;
    private final Color primaryFixedDim;
    private final Color onPrimaryFixed;
    private final Color onPrimaryFixedVariant;
    private final Color secondaryFixed;
    private final Color secondaryFixedDim;
    private final Color onSecondaryFixed;
    private final Color onSecondaryFixedVariant;
    private final Color tertiaryFixed;
    private final Color tertiaryFixedDim;
    private final Color onTertiaryFixed;
    private final Color onTertiaryFixedVariant;
    private final Color controlActivated;
    private final Color controlNormal;
    private final Color controlHighlight;
    private final Color textPrimaryInverse;
    private final Color textSecondaryAndTertiaryInverse;
    private final Color textPrimaryInverseDisableOnly;
    private final Color textSecondaryAndTertiaryInverseDisabled;
    private final Color textHintInverse;
    private final Color surfaceDim;
    private final Color surfaceBright;
    private final Color surfaceContainerLowest;
    private final Color surfaceContainerLow;
    private final Color surfaceContainer;
    private final Color surfaceContainerHigh;
    private final Color surfaceContainerHighest;
    private final Color surfaceTint;
    private final MaterialTheme theme;

    private MaterialScheme(MaterialTheme theme, DynamicScheme scheme) {
        this.theme = theme;
        primary = Color.rgba(scheme.getPrimary());
        onPrimary = Color.rgba(scheme.getOnPrimary());
        primaryContainer = Color.rgba(scheme.getPrimaryContainer());
        onPrimaryContainer = Color.rgba(scheme.getOnPrimaryContainer());
        secondary = Color.rgba(scheme.getSecondary());
        onSecondary = Color.rgba(scheme.getOnSecondary());
        secondaryContainer = Color.rgba(scheme.getSecondaryContainer());
        onSecondaryContainer = Color.rgba(scheme.getOnSecondaryContainer());
        tertiary = Color.rgba(scheme.getTertiary());
        onTertiary = Color.rgba(scheme.getOnTertiary());
        tertiaryContainer = Color.rgba(scheme.getTertiaryContainer());
        onTertiaryContainer = Color.rgba(scheme.getOnTertiaryContainer());
        error = Color.rgba(scheme.getError());
        onError = Color.rgba(scheme.getOnError());
        errorContainer = Color.rgba(scheme.getErrorContainer());
        onErrorContainer = Color.rgba(scheme.getOnErrorContainer());
        background = Color.rgba(scheme.getBackground());
        onBackground = Color.rgba(scheme.getOnBackground());
        surface = Color.rgba(scheme.getSurface());
        onSurface = Color.rgba(scheme.getOnSurface());
        surfaceVariant = Color.rgba(scheme.getSurfaceVariant());
        onSurfaceVariant = Color.rgba(scheme.getOnSurfaceVariant());
        outline = Color.rgba(scheme.getOutline());
        outlineVariant = Color.rgba(scheme.getOutlineVariant());
        shadow = Color.rgba(scheme.getShadow());
        scrim = Color.rgba(scheme.getScrim());
        inverseSurface = Color.rgba(scheme.getInverseSurface());
        inverseOnSurface = Color.rgba(scheme.getInverseOnSurface());
        inversePrimary = Color.rgba(scheme.getInversePrimary());
        primaryFixed = Color.rgba(scheme.getPrimaryFixed());
        primaryFixedDim = Color.rgba(scheme.getPrimaryFixedDim());
        onPrimaryFixed = Color.rgba(scheme.getOnPrimaryFixed());
        onPrimaryFixedVariant = Color.rgba(scheme.getOnPrimaryFixedVariant());
        secondaryFixed = Color.rgba(scheme.getSecondaryFixed());
        secondaryFixedDim = Color.rgba(scheme.getSecondaryFixedDim());
        onSecondaryFixed = Color.rgba(scheme.getOnSecondaryFixed());
        onSecondaryFixedVariant = Color.rgba(scheme.getOnSecondaryFixedVariant());
        tertiaryFixed = Color.rgba(scheme.getTertiaryFixed());
        tertiaryFixedDim = Color.rgba(scheme.getTertiaryFixedDim());
        onTertiaryFixed = Color.rgba(scheme.getOnTertiaryFixed());
        onTertiaryFixedVariant = Color.rgba(scheme.getOnTertiaryFixedVariant());
        controlActivated = Color.rgba(scheme.getControlActivated());
        controlNormal = Color.rgba(scheme.getControlNormal());
        controlHighlight = Color.rgba(scheme.getControlHighlight());
        textPrimaryInverse = Color.rgba(scheme.getTextPrimaryInverse());
        textSecondaryAndTertiaryInverse = Color.rgba(scheme.getTextSecondaryAndTertiaryInverse());
        textPrimaryInverseDisableOnly = Color.rgba(scheme.getTextPrimaryInverseDisableOnly());
        textSecondaryAndTertiaryInverseDisabled = Color.rgba(scheme.getTextSecondaryAndTertiaryInverseDisabled());
        textHintInverse = Color.rgba(scheme.getTextHintInverse());
        surfaceDim = Color.rgba(scheme.getSurfaceDim());
        surfaceBright = Color.rgba(scheme.getSurfaceBright());
        surfaceContainerLowest = Color.rgba(scheme.getSurfaceContainerLowest());
        surfaceContainerLow = Color.rgba(scheme.getSurfaceContainerLow());
        surfaceContainer = Color.rgba(scheme.getSurfaceContainer());
        surfaceContainerHigh = Color.rgba(scheme.getSurfaceContainerHigh());
        surfaceContainerHighest = Color.rgba(scheme.getSurfaceContainerHighest());
        surfaceTint = Color.rgba(scheme.getSurfaceTint());
    }

    public static MaterialScheme from(MaterialTheme theme, Color color) {
        return from(theme, color, SchemeVariant.CONTENT, 0.0f);
    }

    public static MaterialScheme from(MaterialTheme theme, Color color, SchemeVariant variant, double contrastLevel) {
        Hct sourceColor = Hct.fromInt(color.integer());
        boolean isDark = theme == MaterialTheme.Dark;

        DynamicScheme dynamicScheme = switch (variant) {
            case MONOCHROME -> new SchemeMonochrome(sourceColor, isDark, contrastLevel);
            case NEUTRAL -> new SchemeNeutral(sourceColor, isDark, contrastLevel);
            case TONAL_SPOT -> new SchemeTonalSpot(sourceColor, isDark, contrastLevel);
            case VIBRANT -> new SchemeVibrant(sourceColor, isDark, contrastLevel);
            case EXPRESSIVE -> new SchemeExpressive(sourceColor, isDark, contrastLevel);
            case FIDELITY -> new SchemeFidelity(sourceColor, isDark, contrastLevel);
            case CONTENT -> new SchemeContent(sourceColor, isDark, contrastLevel);
            case RAINBOW -> new SchemeRainbow(sourceColor, isDark, contrastLevel);
            case FRUIT_SALAD -> new SchemeFruitSalad(sourceColor, isDark, contrastLevel);
        };

        return new MaterialScheme(theme, dynamicScheme);
    }

    public Color surfaceDim() {
        return surfaceDim;
    }

    public Color surfaceBright() {
        return surfaceBright;
    }

    public Color surfaceContainerLowest() {
        return surfaceContainerLowest;
    }

    public Color surfaceContainerLow() {
        return surfaceContainerLow;
    }

    public Color surfaceContainer() {
        return surfaceContainer;
    }

    public Color surfaceContainerHigh() {
        return surfaceContainerHigh;
    }

    public Color surfaceContainerHighest() {
        return surfaceContainerHighest;
    }

    public Color surfaceTint() {
        return surfaceTint;
    }

    public Color primaryFixed() {
        return primaryFixed;
    }

    public Color primaryFixedDim() {
        return primaryFixedDim;
    }

    public Color onPrimaryFixed() {
        return onPrimaryFixed;
    }

    public Color onPrimaryFixedVariant() {
        return onPrimaryFixedVariant;
    }

    public Color secondaryFixed() {
        return secondaryFixed;
    }

    public Color secondaryFixedDim() {
        return secondaryFixedDim;
    }

    public Color onSecondaryFixed() {
        return onSecondaryFixed;
    }

    public Color onSecondaryFixedVariant() {
        return onSecondaryFixedVariant;
    }

    public Color tertiaryFixed() {
        return tertiaryFixed;
    }

    public Color tertiaryFixedDim() {
        return tertiaryFixedDim;
    }

    public Color onTertiaryFixed() {
        return onTertiaryFixed;
    }

    public Color onTertiaryFixedVariant() {
        return onTertiaryFixedVariant;
    }

    public Color controlActivated() {
        return controlActivated;
    }

    public Color controlNormal() {
        return controlNormal;
    }

    public Color controlHighlight() {
        return controlHighlight;
    }

    public Color textPrimaryInverse() {
        return textPrimaryInverse;
    }

    public Color textSecondaryAndTertiaryInverse() {
        return textSecondaryAndTertiaryInverse;
    }

    public Color textPrimaryInverseDisableOnly() {
        return textPrimaryInverseDisableOnly;
    }

    public Color textSecondaryAndTertiaryInverseDisabled() {
        return textSecondaryAndTertiaryInverseDisabled;
    }

    public Color textHintInverse() {
        return textHintInverse;
    }

    public MaterialTheme theme() {
        return theme;
    }

    public Color inversePrimary() {
        return inversePrimary;
    }

    public Color inverseOnSurface() {
        return inverseOnSurface;
    }

    public Color inverseSurface() {
        return inverseSurface;
    }

    public Color scrim() {
        return scrim;
    }

    public Color shadow() {
        return shadow;
    }

    public Color outlineVariant() {
        return outlineVariant;
    }

    public Color outline() {
        return outline;
    }

    public Color onSurfaceVariant() {
        return onSurfaceVariant;
    }

    public Color surfaceVariant() {
        return surfaceVariant;
    }

    public Color onSurface() {
        return onSurface;
    }

    public Color surface() {
        return surface;
    }

    public Color onBackground() {
        return onBackground;
    }

    public Color background() {
        return background;
    }

    public Color onErrorContainer() {
        return onErrorContainer;
    }

    public Color errorContainer() {
        return errorContainer;
    }

    public Color onError() {
        return onError;
    }

    public Color error() {
        return error;
    }

    public Color onTertiaryContainer() {
        return onTertiaryContainer;
    }

    public Color tertiaryContainer() {
        return tertiaryContainer;
    }

    public Color onTertiary() {
        return onTertiary;
    }

    public Color tertiary() {
        return tertiary;
    }

    public Color onSecondaryContainer() {
        return onSecondaryContainer;
    }

    public Color secondaryContainer() {
        return secondaryContainer;
    }

    public Color onSecondary() {
        return onSecondary;
    }

    public Color secondary() {
        return secondary;
    }

    public Color onPrimaryContainer() {
        return onPrimaryContainer;
    }

    public Color primaryContainer() {
        return primaryContainer;
    }

    public Color onPrimary() {
        return onPrimary;
    }

    public Color primary() {
        return primary;
    }


}
