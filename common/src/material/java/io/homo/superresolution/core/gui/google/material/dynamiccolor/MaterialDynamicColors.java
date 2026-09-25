/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.homo.superresolution.core.gui.google.material.dynamiccolor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Named colors, otherwise known as tokens, or roles, in the Material Design system.
 */
// Prevent lint for Function.apply not being available on Android before API level 14 (4.0.1).
// "AndroidJdkLibsChecker" for Function, "NewApi" for Function.apply().
// A java_library Bazel rule with an Android constraint cannot skip these warnings without this
// annotation; another solution would be to create an android_library rule and supply
// AndroidManifest with an SDK set higher than 14.
@SuppressWarnings({"AndroidJdkLibsChecker", "NewApi"})
public final class MaterialDynamicColors {

    private static final ColorSpec colorSpec = new ColorSpec2025();

    @NotNull
    public DynamicColor highestSurface(@NotNull DynamicScheme s) {
        return colorSpec.highestSurface(s);
    }

    ////////////////////////////////////////////////////////////////
    // Main Palettes                                              //
    ////////////////////////////////////////////////////////////////

    @NotNull
    public DynamicColor primaryPaletteKeyColor() {
        return colorSpec.primaryPaletteKeyColor();
    }

    @NotNull
    public DynamicColor secondaryPaletteKeyColor() {
        return colorSpec.secondaryPaletteKeyColor();
    }

    @NotNull
    public DynamicColor tertiaryPaletteKeyColor() {
        return colorSpec.tertiaryPaletteKeyColor();
    }

    @NotNull
    public DynamicColor neutralPaletteKeyColor() {
        return colorSpec.neutralPaletteKeyColor();
    }

    @NotNull
    public DynamicColor neutralVariantPaletteKeyColor() {
        return colorSpec.neutralVariantPaletteKeyColor();
    }

    @NotNull
    public DynamicColor errorPaletteKeyColor() {
        return colorSpec.errorPaletteKeyColor();
    }

    ////////////////////////////////////////////////////////////////
    // Surfaces [S]                                               //
    ////////////////////////////////////////////////////////////////

    @NotNull
    public DynamicColor background() {
        return colorSpec.background();
    }

    @NotNull
    public DynamicColor onBackground() {
        return colorSpec.onBackground();
    }

    @NotNull
    public DynamicColor surface() {
        return colorSpec.surface();
    }

    @NotNull
    public DynamicColor surfaceDim() {
        return colorSpec.surfaceDim();
    }

    @NotNull
    public DynamicColor surfaceBright() {
        return colorSpec.surfaceBright();
    }

    @NotNull
    public DynamicColor surfaceContainerLowest() {
        return colorSpec.surfaceContainerLowest();
    }

    @NotNull
    public DynamicColor surfaceContainerLow() {
        return colorSpec.surfaceContainerLow();
    }

    @NotNull
    public DynamicColor surfaceContainer() {
        return colorSpec.surfaceContainer();
    }

    @NotNull
    public DynamicColor surfaceContainerHigh() {
        return colorSpec.surfaceContainerHigh();
    }

    @NotNull
    public DynamicColor surfaceContainerHighest() {
        return colorSpec.surfaceContainerHighest();
    }

    @NotNull
    public DynamicColor onSurface() {
        return colorSpec.onSurface();
    }

    @NotNull
    public DynamicColor surfaceVariant() {
        return colorSpec.surfaceVariant();
    }

    @NotNull
    public DynamicColor onSurfaceVariant() {
        return colorSpec.onSurfaceVariant();
    }

    @NotNull
    public DynamicColor inverseSurface() {
        return colorSpec.inverseSurface();
    }

    @NotNull
    public DynamicColor inverseOnSurface() {
        return colorSpec.inverseOnSurface();
    }

    @NotNull
    public DynamicColor outline() {
        return colorSpec.outline();
    }

    @NotNull
    public DynamicColor outlineVariant() {
        return colorSpec.outlineVariant();
    }

    @NotNull
    public DynamicColor shadow() {
        return colorSpec.shadow();
    }

    @NotNull
    public DynamicColor scrim() {
        return colorSpec.scrim();
    }

    @NotNull
    public DynamicColor surfaceTint() {
        return colorSpec.surfaceTint();
    }

    ////////////////////////////////////////////////////////////////
    // Primaries [P]                                              //
    ////////////////////////////////////////////////////////////////

    @NotNull
    public DynamicColor primary() {
        return colorSpec.primary();
    }

    @Nullable
    public DynamicColor primaryDim() {
        return colorSpec.primaryDim();
    }

    @NotNull
    public DynamicColor onPrimary() {
        return colorSpec.onPrimary();
    }

    @NotNull
    public DynamicColor primaryContainer() {
        return colorSpec.primaryContainer();
    }

    @NotNull
    public DynamicColor onPrimaryContainer() {
        return colorSpec.onPrimaryContainer();
    }

    @NotNull
    public DynamicColor inversePrimary() {
        return colorSpec.inversePrimary();
    }

    /////////////////////////////////////////////////////////////////
    // Primary Fixed Colors [PF]                                   //
    /////////////////////////////////////////////////////////////////

    @NotNull
    public DynamicColor primaryFixed() {
        return colorSpec.primaryFixed();
    }

    @NotNull
    public DynamicColor primaryFixedDim() {
        return colorSpec.primaryFixedDim();
    }

    @NotNull
    public DynamicColor onPrimaryFixed() {
        return colorSpec.onPrimaryFixed();
    }

    @NotNull
    public DynamicColor onPrimaryFixedVariant() {
        return colorSpec.onPrimaryFixedVariant();
    }

    ////////////////////////////////////////////////////////////////
    // Secondaries [Q]                                            //
    ////////////////////////////////////////////////////////////////

    @NotNull
    public DynamicColor secondary() {
        return colorSpec.secondary();
    }

    @Nullable
    public DynamicColor secondaryDim() {
        return colorSpec.secondaryDim();
    }

    @NotNull
    public DynamicColor onSecondary() {
        return colorSpec.onSecondary();
    }

    @NotNull
    public DynamicColor secondaryContainer() {
        return colorSpec.secondaryContainer();
    }

    @NotNull
    public DynamicColor onSecondaryContainer() {
        return colorSpec.onSecondaryContainer();
    }

    /////////////////////////////////////////////////////////////////
    // Secondary Fixed Colors [QF]                                 //
    /////////////////////////////////////////////////////////////////

    @NotNull
    public DynamicColor secondaryFixed() {
        return colorSpec.secondaryFixed();
    }

    @NotNull
    public DynamicColor secondaryFixedDim() {
        return colorSpec.secondaryFixedDim();
    }

    @NotNull
    public DynamicColor onSecondaryFixed() {
        return colorSpec.onSecondaryFixed();
    }

    @NotNull
    public DynamicColor onSecondaryFixedVariant() {
        return colorSpec.onSecondaryFixedVariant();
    }

    ////////////////////////////////////////////////////////////////
    // Tertiaries [T]                                             //
    ////////////////////////////////////////////////////////////////

    @NotNull
    public DynamicColor tertiary() {
        return colorSpec.tertiary();
    }

    @Nullable
    public DynamicColor tertiaryDim() {
        return colorSpec.tertiaryDim();
    }

    @NotNull
    public DynamicColor onTertiary() {
        return colorSpec.onTertiary();
    }

    @NotNull
    public DynamicColor tertiaryContainer() {
        return colorSpec.tertiaryContainer();
    }

    @NotNull
    public DynamicColor onTertiaryContainer() {
        return colorSpec.onTertiaryContainer();
    }

    /////////////////////////////////////////////////////////////////
    // Tertiary Fixed Colors [TF]                                  //
    /////////////////////////////////////////////////////////////////

    @NotNull
    public DynamicColor tertiaryFixed() {
        return colorSpec.tertiaryFixed();
    }

    @NotNull
    public DynamicColor tertiaryFixedDim() {
        return colorSpec.tertiaryFixedDim();
    }

    @NotNull
    public DynamicColor onTertiaryFixed() {
        return colorSpec.onTertiaryFixed();
    }

    @NotNull
    public DynamicColor onTertiaryFixedVariant() {
        return colorSpec.onTertiaryFixedVariant();
    }

    ////////////////////////////////////////////////////////////////
    // Errors [E]                                                 //
    ////////////////////////////////////////////////////////////////

    @NotNull
    public DynamicColor error() {
        return colorSpec.error();
    }

    @Nullable
    public DynamicColor errorDim() {
        return colorSpec.errorDim();
    }

    @NotNull
    public DynamicColor onError() {
        return colorSpec.onError();
    }

    @NotNull
    public DynamicColor errorContainer() {
        return colorSpec.errorContainer();
    }

    @NotNull
    public DynamicColor onErrorContainer() {
        return colorSpec.onErrorContainer();
    }

    ////////////////////////////////////////////////////////////////
    // Android-only colors                                        //
    ////////////////////////////////////////////////////////////////

    /**
     * These colors were present in Android framework before Android U, and used by MDC controls. They
     * should be avoided, if possible. It's unclear if they're used on multiple backgrounds, and if
     * they are, they can't be adjusted for contrast.* For now, they will be set with no background,
     * and those won't adjust for contrast, avoiding issues.
     *
     * <p>* For example, if the same color is on a white background _and_ black background, there's no
     * way to increase contrast with either without losing contrast with the other.
     */

    // colorControlActivated documented as colorAccent in M3 & GM3.
    // colorAccent documented as colorSecondary in M3 and colorPrimary in GM3.
    // Android used Material's Container as Primary/Secondary/Tertiary at launch.
    // Therefore, this is a duplicated version of Primary Container.
    @NotNull
    public DynamicColor controlActivated() {
        return colorSpec.controlActivated();
    }

    // colorControlNormal documented as textColorSecondary in M3 & GM3.
    // In Material, textColorSecondary points to onSurfaceVariant in the non-disabled state,
    // which is Neutral Variant T30/80 in light/dark.
    @NotNull
    public DynamicColor controlNormal() {
        return colorSpec.controlNormal();
    }

    // colorControlHighlight documented, in both M3 & GM3:
    // Light mode: #1f000000 dark mode: #33ffffff.
    // These are black and white with some alpha.
    // 1F hex = 31 decimal; 31 / 255 = 12% alpha.
    // 33 hex = 51 decimal; 51 / 255 = 20% alpha.
    // DynamicColors do not support alpha currently, and _may_ not need it for this use case,
    // depending on how MDC resolved alpha for the other cases.
    // Returning black in dark mode, white in light mode.
    @NotNull
    public DynamicColor controlHighlight() {
        return colorSpec.controlHighlight();
    }

    // textColorPrimaryInverse documented, in both M3 & GM3, documented as N10/N90.
    @NotNull
    public DynamicColor textPrimaryInverse() {
        return colorSpec.textPrimaryInverse();
    }

    // textColorSecondaryInverse and textColorTertiaryInverse both documented, in both M3 & GM3, as
    // NV30/NV80
    @NotNull
    public DynamicColor textSecondaryAndTertiaryInverse() {
        return colorSpec.textSecondaryAndTertiaryInverse();
    }

    // textColorPrimaryInverseDisableOnly documented, in both M3 & GM3, as N10/N90
    @NotNull
    public DynamicColor textPrimaryInverseDisableOnly() {
        return colorSpec.textPrimaryInverseDisableOnly();
    }

    // textColorSecondaryInverse and textColorTertiaryInverse in disabled state both documented,
    // in both M3 & GM3, as N10/N90
    @NotNull
    public DynamicColor textSecondaryAndTertiaryInverseDisabled() {
        return colorSpec.textSecondaryAndTertiaryInverseDisabled();
    }

    // textColorHintInverse documented, in both M3 & GM3, as N10/N90
    @NotNull
    public DynamicColor textHintInverse() {
        return colorSpec.textHintInverse();
    }

    ////////////////////////////////////////////////////////////////
    // All Colors                                                 //
    ////////////////////////////////////////////////////////////////

    /**
     * All dynamic colors in Material Design system.
     */
    public final List<Supplier<DynamicColor>> allDynamicColors() {
        return Arrays.asList(
                this::primaryPaletteKeyColor,
                this::secondaryPaletteKeyColor,
                this::tertiaryPaletteKeyColor,
                this::neutralPaletteKeyColor,
                this::neutralVariantPaletteKeyColor,
                this::errorPaletteKeyColor,
                this::background,
                this::onBackground,
                this::surface,
                this::surfaceDim,
                this::surfaceBright,
                this::surfaceContainerLowest,
                this::surfaceContainerLow,
                this::surfaceContainer,
                this::surfaceContainerHigh,
                this::surfaceContainerHighest,
                this::onSurface,
                this::surfaceVariant,
                this::onSurfaceVariant,
                this::outline,
                this::outlineVariant,
                this::inverseSurface,
                this::inverseOnSurface,
                this::shadow,
                this::scrim,
                this::surfaceTint,
                this::primary,
                this::primaryDim,
                this::onPrimary,
                this::primaryContainer,
                this::onPrimaryContainer,
                this::primaryFixed,
                this::primaryFixedDim,
                this::onPrimaryFixed,
                this::onPrimaryFixedVariant,
                this::inversePrimary,
                this::secondary,
                this::secondaryDim,
                this::onSecondary,
                this::secondaryContainer,
                this::onSecondaryContainer,
                this::secondaryFixed,
                this::secondaryFixedDim,
                this::onSecondaryFixed,
                this::onSecondaryFixedVariant,
                this::tertiary,
                this::tertiaryDim,
                this::onTertiary,
                this::tertiaryContainer,
                this::onTertiaryContainer,
                this::tertiaryFixed,
                this::tertiaryFixedDim,
                this::onTertiaryFixed,
                this::onTertiaryFixedVariant,
                this::error,
                this::errorDim,
                this::onError,
                this::errorContainer,
                this::onErrorContainer,
                this::controlActivated,
                this::controlNormal,
                this::controlHighlight,
                this::textPrimaryInverse,
                this::textSecondaryAndTertiaryInverse,
                this::textPrimaryInverseDisableOnly,
                this::textSecondaryAndTertiaryInverseDisabled,
                this::textHintInverse);
    }
}
