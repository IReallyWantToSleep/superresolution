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

package io.homo.superresolution.core.streamline;

public final class StreamlineFeature {
    public static final int DLSS = 0;
    public static final int NIS = 2;
    public static final int REFLEX = 3;
    public static final int PCL = 4;
    public static final int DEEP_DVC = 5;
    public static final int LATE_WARP = 6;
    public static final int DLSS_G = 1000;
    public static final int DLSS_RR = 1001;
    public static final int NV_PERF = 1002;
    public static final int DIRECT_SR = 1003;

    private StreamlineFeature() {
    }
}
