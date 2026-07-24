/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package io.homo.superresolution.api.registry;

import io.homo.superresolution.api.utils.Requirement;
import io.homo.superresolution.common.config.special.SpecialConfigDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class LowLatencyDescription {
    private final String id;
    private final String displayName;
    private final Requirement requirement;
    private final Supplier<LowLatencyProvider> providerFactory;
    private final List<SpecialConfigDescription<?>> optionDescriptions;

    private LowLatencyDescription(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id cannot be null");
        this.displayName = Objects.requireNonNull(builder.displayName, "displayName cannot be null");
        this.requirement = Objects.requireNonNull(builder.requirement, "requirement cannot be null");
        this.providerFactory = Objects.requireNonNull(builder.providerFactory, "providerFactory cannot be null");
        this.optionDescriptions = List.copyOf(builder.optionDescriptions);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Requirement getRequirement() {
        return requirement;
    }

    public Supplier<LowLatencyProvider> getProviderFactory() {
        return providerFactory;
    }

    public LowLatencyProvider createProvider() {
        return providerFactory.get();
    }

    public List<SpecialConfigDescription<?>> getOptionDescriptions() {
        return optionDescriptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LowLatencyDescription that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Builder {
        private String id;
        private String displayName;
        private Requirement requirement = Requirement.nothing();
        private Supplier<LowLatencyProvider> providerFactory;
        private final List<SpecialConfigDescription<?>> optionDescriptions = new ArrayList<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder requirement(Requirement requirement) {
            this.requirement = requirement;
            return this;
        }

        public Builder providerFactory(Supplier<LowLatencyProvider> providerFactory) {
            this.providerFactory = providerFactory;
            return this;
        }

        public Builder addOptionDescription(SpecialConfigDescription<?> optionDescription) {
            if (optionDescription != null) {
                this.optionDescriptions.add(optionDescription);
            }
            return this;
        }

        public Builder optionDescriptions(List<SpecialConfigDescription<?>> optionDescriptions) {
            this.optionDescriptions.clear();
            if (optionDescriptions != null) {
                this.optionDescriptions.addAll(optionDescriptions);
            }
            return this;
        }

        public LowLatencyDescription build() {
            return new LowLatencyDescription(this);
        }
    }
}
