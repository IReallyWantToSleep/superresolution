/*
 * Copyright 2025 Google LLC
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
import io.homo.superresolution.core.gui.google.material.dynamiccolor.DynamicScheme.Platform;
import io.homo.superresolution.core.gui.google.material.hct.Hct;
import io.homo.superresolution.core.gui.google.material.palettes.TonalPalette;
import java.util.Optional;

/** An interface defining all the necessary methods that could be different between specs. */
public interface ColorSpec {

  /** All available spec versions. */
  public enum SpecVersion {
    SPEC_2021,
    SPEC_2025,
    SPEC_2026,
  }

  ////////////////////////////////////////////////////////////////
  // Main Palettes                                              //
  ////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor primaryPaletteKeyColor();

  @NotNull
  public DynamicColor secondaryPaletteKeyColor();

  @NotNull
  public DynamicColor tertiaryPaletteKeyColor();

  @NotNull
  public DynamicColor neutralPaletteKeyColor();

  @NotNull
  public DynamicColor neutralVariantPaletteKeyColor();

  @NotNull
  public DynamicColor errorPaletteKeyColor();

  ////////////////////////////////////////////////////////////////
  // Surfaces [S]                                               //
  ////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor background();

  @NotNull
  public DynamicColor onBackground();

  @NotNull
  public DynamicColor surface();

  @NotNull
  public DynamicColor surfaceDim();

  @NotNull
  public DynamicColor surfaceBright();

  @NotNull
  public DynamicColor surfaceContainerLowest();

  @NotNull
  public DynamicColor surfaceContainerLow();

  @NotNull
  public DynamicColor surfaceContainer();

  @NotNull
  public DynamicColor surfaceContainerHigh();

  @NotNull
  public DynamicColor surfaceContainerHighest();

  @NotNull
  public DynamicColor onSurface();

  @NotNull
  public DynamicColor surfaceVariant();

  @NotNull
  public DynamicColor onSurfaceVariant();

  @NotNull
  public DynamicColor inverseSurface();

  @NotNull
  public DynamicColor inverseOnSurface();

  @NotNull
  public DynamicColor outline();

  @NotNull
  public DynamicColor outlineVariant();

  @NotNull
  public DynamicColor shadow();

  @NotNull
  public DynamicColor scrim();

  @NotNull
  public DynamicColor surfaceTint();

  ////////////////////////////////////////////////////////////////
  // Primaries [P]                                              //
  ////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor primary();

  @Nullable
  public DynamicColor primaryDim();

  @NotNull
  public DynamicColor onPrimary();

  @NotNull
  public DynamicColor primaryContainer();

  @NotNull
  public DynamicColor onPrimaryContainer();

  @NotNull
  public DynamicColor inversePrimary();

  ////////////////////////////////////////////////////////////////
  // Secondaries [Q]                                            //
  ////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor secondary();

  @Nullable
  public DynamicColor secondaryDim();

  @NotNull
  public DynamicColor onSecondary();

  @NotNull
  public DynamicColor secondaryContainer();

  @NotNull
  public DynamicColor onSecondaryContainer();

  ////////////////////////////////////////////////////////////////
  // Tertiaries [T]                                             //
  ////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor tertiary();

  @Nullable
  public DynamicColor tertiaryDim();

  @NotNull
  public DynamicColor onTertiary();

  @NotNull
  public DynamicColor tertiaryContainer();

  @NotNull
  public DynamicColor onTertiaryContainer();

  ////////////////////////////////////////////////////////////////
  // Errors [E]                                                 //
  ////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor error();

  @Nullable
  public DynamicColor errorDim();

  @NotNull
  public DynamicColor onError();

  @NotNull
  public DynamicColor errorContainer();

  @NotNull
  public DynamicColor onErrorContainer();

  ////////////////////////////////////////////////////////////////
  // Primary Fixed Colors [PF]                                  //
  ////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor primaryFixed();

  @NotNull
  public DynamicColor primaryFixedDim();

  @NotNull
  public DynamicColor onPrimaryFixed();

  @NotNull
  public DynamicColor onPrimaryFixedVariant();

  ////////////////////////////////////////////////////////////////
  // Secondary Fixed Colors [QF]                                //
  ////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor secondaryFixed();

  @NotNull
  public DynamicColor secondaryFixedDim();

  @NotNull
  public DynamicColor onSecondaryFixed();

  @NotNull
  public DynamicColor onSecondaryFixedVariant();

  ////////////////////////////////////////////////////////////////
  // Tertiary Fixed Colors [TF]                                 //
  ////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor tertiaryFixed();

  @NotNull
  public DynamicColor tertiaryFixedDim();

  @NotNull
  public DynamicColor onTertiaryFixed();

  @NotNull
  public DynamicColor onTertiaryFixedVariant();

  //////////////////////////////////////////////////////////////////
  // Android-only Colors                                          //
  //////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor controlActivated();

  @NotNull
  public DynamicColor controlNormal();

  @NotNull
  public DynamicColor controlHighlight();

  @NotNull
  public DynamicColor textPrimaryInverse();

  @NotNull
  public DynamicColor textSecondaryAndTertiaryInverse();

  @NotNull
  public DynamicColor textPrimaryInverseDisableOnly();

  @NotNull
  public DynamicColor textSecondaryAndTertiaryInverseDisabled();

  @NotNull
  public DynamicColor textHintInverse();

  ////////////////////////////////////////////////////////////////
  // Other                                                      //
  ////////////////////////////////////////////////////////////////

  @NotNull
  public DynamicColor highestSurface(@NotNull DynamicScheme s);

  /////////////////////////////////////////////////////////////////
  // Color value calculations                                    //
  /////////////////////////////////////////////////////////////////

  Hct getHct(DynamicScheme scheme, DynamicColor color);

  double getTone(DynamicScheme scheme, DynamicColor color);

  //////////////////////////////////////////////////////////////////
  // Scheme Palettes                                              //
  //////////////////////////////////////////////////////////////////

  @NotNull
  public TonalPalette getPrimaryPalette(
      Variant variant, Hct sourceColorHct, boolean isDark, Platform platform, double contrastLevel);

  @NotNull
  public TonalPalette getSecondaryPalette(
      Variant variant, Hct sourceColorHct, boolean isDark, Platform platform, double contrastLevel);

  @NotNull
  public TonalPalette getTertiaryPalette(
      Variant variant, Hct sourceColorHct, boolean isDark, Platform platform, double contrastLevel);

  @NotNull
  public TonalPalette getNeutralPalette(
      Variant variant, Hct sourceColorHct, boolean isDark, Platform platform, double contrastLevel);

  @NotNull
  public TonalPalette getNeutralVariantPalette(
      Variant variant, Hct sourceColorHct, boolean isDark, Platform platform, double contrastLevel);

  @NotNull
  public Optional<TonalPalette> getErrorPalette(
      Variant variant, Hct sourceColorHct, boolean isDark, Platform platform, double contrastLevel);
}
