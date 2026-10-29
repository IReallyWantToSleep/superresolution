/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event;

import java.util.Objects;

public final class LayoutData {
    public int layouts;
    public int measures;
    public int maxMeasureCache;
    public int cachedLayouts;
    public int cachedMeasures;
    public int measureCallbacks;
    public int[] measureCallbackReasonsCount;

    public LayoutData(
            int layouts,
            int measures,
            int maxMeasureCache,
            int cachedLayouts,
            int cachedMeasures,
            int measureCallbacks,
            int[] measureCallbackReasonsCount
    ) {
        this.layouts = layouts;
        this.measures = measures;
        this.maxMeasureCache = maxMeasureCache;
        this.cachedLayouts = cachedLayouts;
        this.cachedMeasures = cachedMeasures;
        this.measureCallbacks = measureCallbacks;
        this.measureCallbackReasonsCount = measureCallbackReasonsCount;
    }

    public LayoutData() {
        this(0, 0, 0, 0, 0, 0, new int[LayoutPassReason.COUNT]);
    }

    public LayoutData withLayouts(int layouts) {
        return new LayoutData(
                layouts,
                measures,
                maxMeasureCache,
                cachedLayouts,
                cachedMeasures,
                measureCallbacks,
                measureCallbackReasonsCount
        );
    }

    public LayoutData withMeasures(int measures) {
        return new LayoutData(
                layouts,
                measures,
                maxMeasureCache,
                cachedLayouts,
                cachedMeasures,
                measureCallbacks,
                measureCallbackReasonsCount
        );
    }

    public LayoutData withMaxMeasureCache(int maxMeasureCache) {
        return new LayoutData(
                layouts,
                measures,
                maxMeasureCache,
                cachedLayouts,
                cachedMeasures,
                measureCallbacks,
                measureCallbackReasonsCount
        );
    }

    public LayoutData withCachedLayouts(int cachedLayouts) {
        return new LayoutData(
                layouts,
                measures,
                maxMeasureCache,
                cachedLayouts,
                cachedMeasures,
                measureCallbacks,
                measureCallbackReasonsCount
        );
    }

    public LayoutData withCachedMeasures(int cachedMeasures) {
        return new LayoutData(
                layouts,
                measures,
                maxMeasureCache,
                cachedLayouts,
                cachedMeasures,
                measureCallbacks,
                measureCallbackReasonsCount
        );
    }

    public LayoutData withMeasureCallbacks(int measureCallbacks) {
        return new LayoutData(
                layouts,
                measures,
                maxMeasureCache,
                cachedLayouts,
                cachedMeasures,
                measureCallbacks,
                measureCallbackReasonsCount
        );
    }

    public LayoutData incrementMeasureCallbackReason(LayoutPassReason reason) {
        int[] newReasons = measureCallbackReasonsCount.clone();
        newReasons[reason.ordinal()]++;
        return new LayoutData(
                layouts,
                measures,
                maxMeasureCache,
                cachedLayouts,
                cachedMeasures,
                measureCallbacks,
                newReasons
        );
    }

    public int layouts() {
        return layouts;
    }

    public int measures() {
        return measures;
    }

    public int maxMeasureCache() {
        return maxMeasureCache;
    }

    public int cachedLayouts() {
        return cachedLayouts;
    }

    public int cachedMeasures() {
        return cachedMeasures;
    }

    public int measureCallbacks() {
        return measureCallbacks;
    }

    public int[] measureCallbackReasonsCount() {
        return measureCallbackReasonsCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(layouts, measures, maxMeasureCache, cachedLayouts, cachedMeasures, measureCallbacks, measureCallbackReasonsCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (LayoutData) obj;
        return this.layouts == that.layouts &&
                this.measures == that.measures &&
                this.maxMeasureCache == that.maxMeasureCache &&
                this.cachedLayouts == that.cachedLayouts &&
                this.cachedMeasures == that.cachedMeasures &&
                this.measureCallbacks == that.measureCallbacks &&
                Objects.equals(this.measureCallbackReasonsCount, that.measureCallbackReasonsCount);
    }

    @Override
    public String toString() {
        return "LayoutData[" +
                "layouts=" + layouts + ", " +
                "measures=" + measures + ", " +
                "maxMeasureCache=" + maxMeasureCache + ", " +
                "cachedLayouts=" + cachedLayouts + ", " +
                "cachedMeasures=" + cachedMeasures + ", " +
                "measureCallbacks=" + measureCallbacks + ", " +
                "measureCallbackReasonsCount=" + measureCallbackReasonsCount + ']';
    }

}
