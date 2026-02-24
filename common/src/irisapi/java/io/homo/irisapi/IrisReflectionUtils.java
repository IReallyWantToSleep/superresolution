/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.irisapi;

import com.google.common.collect.ImmutableSet;
import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IrisReflectionUtils {
    private static volatile Class<?> passClazz;
    private static volatile Class<?> computeOnlyPassClazz;
    private static volatile Class<?> customUniformClazz;
    private static volatile Field computesField;
    private static volatile Field stageReadsFromAltField;
    private static volatile Field customUniform_variablesField;
    private static volatile Field customUniform_uniformOrderField;

    private static volatile boolean initialized = false;

    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (IrisReflectionUtils.class) {
                if (!initialized) {
                    try {
                        passClazz = Class.forName("net.irisshaders.iris.pipeline.CompositeRenderer$Pass");
                        computeOnlyPassClazz = Class.forName("net.irisshaders.iris.pipeline.CompositeRenderer$ComputeOnlyPass");
                        customUniformClazz = Class.forName("net.irisshaders.iris.uniforms.custom.CustomUniforms");
                        computesField = passClazz.getDeclaredField("computes");
                        stageReadsFromAltField = passClazz.getDeclaredField("stageReadsFromAlt");
                        customUniform_variablesField = customUniformClazz.getDeclaredField("variables");
                        customUniform_uniformOrderField = customUniformClazz.getDeclaredField("uniformOrder");
                        computesField.setAccessible(true);
                        stageReadsFromAltField.setAccessible(true);
                        customUniform_variablesField.setAccessible(true);
                        customUniform_uniformOrderField.setAccessible(true);
                        initialized = true;
                    } catch (Throwable e) {
                        throw new RuntimeException("Failed to initialize IrisReflectionUtils", e);
                    }
                }
            }
        }
    }

    public static List<CachedUniform> getVariableCustomUniforms(Object customUniforms) {
        ensureInitialized();
        try {
            return new ArrayList<>( ((Map<String,CachedUniform>)customUniform_variablesField.get(customUniforms)).values());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access variables field", e);
        }
    }

    public static  List<CachedUniform> getUniformOrderCustomUniforms(Object customUniforms) {
        ensureInitialized();
        try {
            return (List<CachedUniform>) customUniform_uniformOrderField.get(customUniforms);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access uniformOrder field", e);
        }
    }

    public static ImmutableSet<Integer> getCompositePassStateReadsFromAlt(Object object) {
        ensureInitialized();
        try {
            return (ImmutableSet<Integer>) stageReadsFromAltField.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access stateReadsFromAlt field", e);
        }
    }

    public static IrisCompositePassType getCompositePassType(Object object) {
        ensureInitialized();
        
        try {
            Class<?> objClazz = object.getClass();
            if (computeOnlyPassClazz == objClazz) {
                return IrisCompositePassType.ComputeOnly;
            }
            
            if (passClazz.isAssignableFrom(objClazz)) {
                Object[] computes = (Object[]) computesField.get(object);
                if (computes != null && computes.length > 0) {
                    return IrisCompositePassType.Mixed;
                } else {
                    return IrisCompositePassType.Common;
                }
            }
            
            throw new IllegalArgumentException("Unknown pass type: " + objClazz.getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access computes field", e);
        }
    }
}
