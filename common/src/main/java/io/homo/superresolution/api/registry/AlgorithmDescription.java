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

package io.homo.superresolution.api.registry;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.QualityPreset;
import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.common.SuperResolution;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AlgorithmDescription<T extends AbstractAlgorithm> {
    public final String briefName;
    public final String codeName;
    public final String displayName;
    public final Requirement requirement;
    public final ExtraResources extraResources;
    public final boolean supportJitter;
    public final List<QualityPreset> qualityPresets;
    public final boolean customUpscaleRatio;
    protected final Class<T> clazz;
    private final String uuid = UUID.randomUUID().toString();

    private AlgorithmDescription(Builder<T> builder) {
        this.clazz = builder.clazz;
        this.briefName = builder.briefName;
        this.codeName = builder.codeName;
        this.displayName = builder.displayName;
        this.requirement = builder.requirement;
        this.extraResources = builder.extraResources;
        this.supportJitter = builder.supportJitter;
        this.qualityPresets = List.copyOf(builder.qualityPresets);
        this.customUpscaleRatio = builder.customUpscaleRatio;
    }

    public static <T extends AbstractAlgorithm> Builder<T> builder(Class<T> clazz) {
        return new Builder<>(clazz);
    }

    public ExtraResources getExtraResources() {
        return extraResources;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public String getBriefName() {
        return briefName;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getId() {
        return uuid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSupportJitter() {
        return supportJitter;
    }

    public List<QualityPreset> getQualityPresets() {
        return qualityPresets;
    }

    public boolean isCustomUpscaleRatio() {
        return customUpscaleRatio;
    }

    public T createNewInstance() {
        try {
            return this.clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            SuperResolution.LOGGER.error("算法创建失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AlgorithmDescription<?> that = (AlgorithmDescription<?>) o;
        return Objects.equals(uuid, that.uuid);
    }

    public static class Builder<T extends AbstractAlgorithm> {
        private final Class<T> clazz;
        private String briefName;
        private String codeName;
        private String displayName;
        private Requirement requirement = Requirement.nothing();
        private ExtraResources extraResources = ExtraResources.builder().build();
        private boolean supportJitter = false;
        private List<QualityPreset> qualityPresets = List.of();
        private boolean customUpscaleRatio = true;

        private Builder(Class<T> clazz) {
            this.clazz = clazz;
        }

        public Builder<T> briefName(String briefName) {
            this.briefName = briefName;
            return this;
        }

        public Builder<T> codeName(String codeName) {
            this.codeName = codeName;
            return this;
        }

        public Builder<T> displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder<T> requirement(Requirement requirement) {
            this.requirement = requirement;
            return this;
        }

        public Builder<T> extraResources(ExtraResources extraResources) {
            this.extraResources = extraResources;
            return this;
        }

        public Builder<T> supportJitter(boolean supportJitter) {
            this.supportJitter = supportJitter;
            return this;
        }

        public Builder<T> qualityPresets(List<QualityPreset> qualityPresets) {
            this.qualityPresets = qualityPresets;
            return this;
        }

        public Builder<T> customUpscaleRatio(boolean customUpscaleRatio) {
            this.customUpscaleRatio = customUpscaleRatio;
            return this;
        }

        public AlgorithmDescription<T> build() {
            return new AlgorithmDescription<>(this);
        }
    }
}
