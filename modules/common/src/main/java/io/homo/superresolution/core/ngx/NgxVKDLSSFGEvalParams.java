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

package io.homo.superresolution.core.ngx;

public final class NgxVKDLSSFGEvalParams {
    public NgxResourceVK backbuffer;
    public NgxResourceVK depth;
    public NgxResourceVK motionVectors;
    public NgxResourceVK hudless;
    public NgxResourceVK ui;
    public NgxResourceVK uiAlpha;
    public NgxResourceVK bidirectionalDistortionField;
    public NgxResourceVK outputInterpolatedFrame;
    public NgxResourceVK outputRealFrame;
    public NgxResourceVK outputDisableInterpolation;
}
