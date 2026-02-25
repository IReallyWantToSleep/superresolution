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
import io.homo.superresolution.api.utils.Requirement;

import java.util.Objects;
import java.util.UUID;

public class AlgorithmDescription<T extends AbstractAlgorithm> {
    public final String briefName;
    public final String codeName;
    public final String displayName;
    public final Requirement requirement;
    public final ExtraResources extraResources;
    protected final Class<T> clazz;
    private final String uuid = UUID.randomUUID().toString();

    public AlgorithmDescription(
            Class<T> clazz,
            String briefName,
            String codeName,
            String displayName,
            Requirement requirement
    ) {
        this(
                clazz,
                briefName,
                codeName,
                displayName,
                requirement,
                ExtraResources.builder().build()
        );
    }

    public AlgorithmDescription(
            Class<T> clazz,
            String briefName,
            String codeName,
            String displayName,
            Requirement requirement,
            ExtraResources extraResources
    ) {
        this.clazz = clazz;
        this.briefName = briefName;
        this.codeName = codeName;
        this.displayName = displayName;
        this.requirement = requirement;
        this.extraResources = extraResources;
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

    public T createNewInstance() {
        try {
            return this.clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
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
}
