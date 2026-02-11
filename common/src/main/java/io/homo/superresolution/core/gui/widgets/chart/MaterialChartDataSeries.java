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

package io.homo.superresolution.core.gui.widgets.chart;

import io.homo.superresolution.core.utils.Color;

import java.util.Arrays;

public class MaterialChartDataSeries {
    private final String name;
    private final Color color;
    private final MaterialChartType type;
    private float[] data;
    private int dataCount;
    private int maxDataPoints;

    public MaterialChartDataSeries(String name, Color color, MaterialChartType type, int maxDataPoints) {
        this.name = name;
        this.color = color;
        this.type = type;
        this.maxDataPoints = maxDataPoints;
        this.data = new float[maxDataPoints];
        this.dataCount = 0;
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public MaterialChartType getType() {
        return type;
    }

    public int getDataCount() {
        return dataCount;
    }

    public int getMaxDataPoints() {
        return maxDataPoints;
    }

    public float[] getData() {
        return data;
    }

    public void setData(float[] newData) {
        if (newData.length > maxDataPoints) {
            System.arraycopy(newData, newData.length - maxDataPoints, data, 0, maxDataPoints);
            dataCount = maxDataPoints;
        } else {
            System.arraycopy(newData, 0, data, 0, newData.length);
            dataCount = newData.length;
        }
    }

    public void setData(long[] newData) {
        if (newData.length > maxDataPoints) {
            int offset = newData.length - maxDataPoints;
            for (int i = 0; i < maxDataPoints; i++) {
                data[i] = (float) newData[offset + i];
            }
            dataCount = maxDataPoints;
        } else {
            for (int i = 0; i < newData.length; i++) {
                data[i] = (float) newData[i];
            }
            dataCount = newData.length;
        }
    }

    public void addDataPoint(float value) {
        if (dataCount < maxDataPoints) {
            data[dataCount] = value;
            dataCount++;
        } else {
            System.arraycopy(data, 1, data, 0, maxDataPoints - 1);
            data[maxDataPoints - 1] = value;
        }
    }

    public void clear() {
        Arrays.fill(data, 0);
        dataCount = 0;
    }

    public float getAverage() {
        if (dataCount == 0) {
            return 0;
        }
        float sum = 0;
        for (int i = 0; i < dataCount; i++) {
            sum += data[i];
        }
        return sum / dataCount;
    }

    public float getMin() {
        if (dataCount == 0) {
            return 0;
        }
        float min = Float.MAX_VALUE;
        for (int i = 0; i < dataCount; i++) {
            if (data[i] > 0) {
                min = Math.min(min, data[i]);
            }
        }
        return min == Float.MAX_VALUE ? 0 : min;
    }

    public float getMax() {
        if (dataCount == 0) {
            return 0;
        }
        float max = Float.MIN_VALUE;
        for (int i = 0; i < dataCount; i++) {
            max = Math.max(max, data[i]);
        }
        return max == Float.MIN_VALUE ? 0 : max;
    }

    public float getLastValue() {
        if (dataCount == 0) {
            return 0;
        }
        return data[dataCount - 1];
    }
}
