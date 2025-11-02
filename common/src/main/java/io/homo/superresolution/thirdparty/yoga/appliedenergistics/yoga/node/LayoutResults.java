package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.node;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaConstants;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDimension;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPhysicalEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.node.CachedMeasurement;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.numeric.FloatOptional;

import java.util.Arrays;
import java.util.Objects;

/**
 * Holds the results of a layout calculation.
 */
public class LayoutResults {
    // This value was chosen based on empirical data:
    // 98% of analyzed layouts require less than 8 entries.
    public static final int MAX_CACHED_MEASUREMENTS = 8;

    // Layout properties
    private YogaDirection direction = YogaDirection.INHERIT;
    private boolean hadOverflow = false;

    // Layout values
    private final float[] dimensions = new float[]{YogaConstants.UNDEFINED, YogaConstants.UNDEFINED};
    private final float[] measuredDimensions = new float[]{YogaConstants.UNDEFINED, YogaConstants.UNDEFINED};
    private final float[] position = new float[4]; // left, top, right, bottom
    private final float[] margin = new float[4];
    private final float[] border = new float[4];
    private final float[] padding = new float[4];

    // Caching related fields
    public long computedFlexBasisGeneration = 0;
    public FloatOptional computedFlexBasis = FloatOptional.of(YogaConstants.UNDEFINED);

    public int generationCount = 0;
    public int configVersion = 0;
    public YogaDirection lastOwnerDirection = YogaDirection.INHERIT;

    public int nextCachedMeasurementsIndex = 0;
    public final CachedMeasurement[] cachedMeasurements = new CachedMeasurement[MAX_CACHED_MEASUREMENTS];

    public CachedMeasurement cachedLayout = new CachedMeasurement();

    public LayoutResults() {
        // Initialize cached measurements array
        for (int i = 0; i < MAX_CACHED_MEASUREMENTS; i++) {
            cachedMeasurements[i] = new CachedMeasurement();
        }
    }

    /**
     * Creates a new LayoutResults that is a copy of the specified LayoutResults.
     *
     * @param other The LayoutResults to copy
     */
    public LayoutResults(LayoutResults other) {
        // Copy layout properties
        this.direction = other.direction;
        this.hadOverflow = other.hadOverflow;

        // Copy layout values (arrays)
        System.arraycopy(other.dimensions, 0, this.dimensions, 0, this.dimensions.length);
        System.arraycopy(other.measuredDimensions, 0, this.measuredDimensions, 0, this.measuredDimensions.length);
        System.arraycopy(other.position, 0, this.position, 0, this.position.length);
        System.arraycopy(other.margin, 0, this.margin, 0, this.margin.length);
        System.arraycopy(other.border, 0, this.border, 0, this.border.length);
        System.arraycopy(other.padding, 0, this.padding, 0, this.padding.length);

        // Copy caching related fields
        this.computedFlexBasisGeneration = other.computedFlexBasisGeneration;
        this.computedFlexBasis = other.computedFlexBasis;

        this.generationCount = other.generationCount;
        this.configVersion = other.configVersion;
        this.lastOwnerDirection = other.lastOwnerDirection;

        this.nextCachedMeasurementsIndex = other.nextCachedMeasurementsIndex;

        // Deep copy of cached measurements array
        for (int i = 0; i < MAX_CACHED_MEASUREMENTS; i++) {
            if (other.cachedMeasurements[i] != null) {
                this.cachedMeasurements[i] = new CachedMeasurement(other.cachedMeasurements[i]);
            }
        }

        // Deep copy of cached layout
        this.cachedLayout = new CachedMeasurement(other.cachedLayout);
    }

    // Direction methods
    public YogaDirection direction() {
        return direction;
    }

    public void setDirection(YogaDirection direction) {
        this.direction = direction;
    }

    // Overflow methods
    public boolean hadOverflow() {
        return hadOverflow;
    }

    public void setHadOverflow(boolean hadOverflow) {
        this.hadOverflow = hadOverflow;
    }

    // Dimension methods
    public float dimension(YogaDimension axis) {
        return dimensions[axis.ordinal()];
    }

    public void setDimension(YogaDimension axis, float dimension) {
        dimensions[axis.ordinal()] = dimension;
    }

    // Measured dimension methods
    public float measuredDimension(YogaDimension axis) {
        return measuredDimensions[axis.ordinal()];
    }

    public void setMeasuredDimension(YogaDimension axis, float dimension) {
        measuredDimensions[axis.ordinal()] = dimension;
    }

    // Position methods
    public float position(YogaPhysicalEdge physicalEdge) {
        return position[physicalEdge.ordinal()];
    }

    public void setPosition(YogaPhysicalEdge physicalEdge, float dimension) {
        position[physicalEdge.ordinal()] = dimension;
    }

    // Margin methods
    public float margin(YogaPhysicalEdge physicalEdge) {
        return margin[physicalEdge.ordinal()];
    }

    public void setMargin(YogaPhysicalEdge physicalEdge, float dimension) {
        margin[physicalEdge.ordinal()] = dimension;
    }

    // Border methods
    public float border(YogaPhysicalEdge physicalEdge) {
        return border[physicalEdge.ordinal()];
    }

    public void setBorder(YogaPhysicalEdge physicalEdge, float dimension) {
        border[physicalEdge.ordinal()] = dimension;
    }

    // Padding methods
    public float padding(YogaPhysicalEdge physicalEdge) {
        return padding[physicalEdge.ordinal()];
    }

    public void setPadding(YogaPhysicalEdge physicalEdge, float dimension) {
        padding[physicalEdge.ordinal()] = dimension;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        LayoutResults other = (LayoutResults) obj;

        // Check primitive properties
        boolean isEqual =
                direction == other.direction &&
                        hadOverflow == other.hadOverflow &&
                        Arrays.equals(position, other.position) &&
                        Arrays.equals(dimensions, other.dimensions) &&
                        Arrays.equals(margin, other.margin) &&
                        Arrays.equals(border, other.border) &&
                        Arrays.equals(padding, other.padding) &&
                        lastOwnerDirection == other.lastOwnerDirection &&
                        configVersion == other.configVersion &&
                        nextCachedMeasurementsIndex == other.nextCachedMeasurementsIndex &&
                        Objects.equals(cachedLayout, other.cachedLayout) &&
                        Objects.equals(computedFlexBasis, other.computedFlexBasis);

        // Check cached measurements
        for (int i = 0; i < MAX_CACHED_MEASUREMENTS && isEqual; ++i) {
            isEqual = isEqual && Objects.equals(cachedMeasurements[i], other.cachedMeasurements[i]);
        }

        // Check measured dimensions with special undefined handling
        if (!YogaConstants.isUndefined(measuredDimensions[0]) ||
                !YogaConstants.isUndefined(other.measuredDimensions[0])) {
            isEqual = isEqual && (measuredDimensions[0] == other.measuredDimensions[0]);
        }

        if (!YogaConstants.isUndefined(measuredDimensions[1]) ||
                !YogaConstants.isUndefined(other.measuredDimensions[1])) {
            isEqual = isEqual && (measuredDimensions[1] == other.measuredDimensions[1]);
        }

        return isEqual;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
                direction,
                hadOverflow,
                computedFlexBasis,
                generationCount,
                configVersion,
                lastOwnerDirection,
                nextCachedMeasurementsIndex,
                cachedLayout);

        result = 31 * result + Arrays.hashCode(dimensions);
        result = 31 * result + Arrays.hashCode(measuredDimensions);
        result = 31 * result + Arrays.hashCode(position);
        result = 31 * result + Arrays.hashCode(margin);
        result = 31 * result + Arrays.hashCode(border);
        result = 31 * result + Arrays.hashCode(padding);
        result = 31 * result + Arrays.hashCode(cachedMeasurements);

        return result;
    }

    // TODO: It's unclear from C++ whether it default initializes, or zeros?
    public void reset() {
        direction = YogaDirection.INHERIT;
        hadOverflow = false;

        // Layout values
        Arrays.fill(dimensions, YogaConstants.UNDEFINED);
        Arrays.fill(measuredDimensions, YogaConstants.UNDEFINED);
        Arrays.fill(position, 0);
        Arrays.fill(margin, 0);
        Arrays.fill(border, 0);
        Arrays.fill(padding, 0);

        // Caching related fields
        computedFlexBasisGeneration = 0;
        computedFlexBasis = FloatOptional.of(YogaConstants.UNDEFINED);

        generationCount = 0;
        configVersion = 0;
        lastOwnerDirection = YogaDirection.INHERIT;

        nextCachedMeasurementsIndex = 0;
        for (int i = 0; i < MAX_CACHED_MEASUREMENTS; i++) {
            cachedMeasurements[i] = new CachedMeasurement();
        }
    }
}
