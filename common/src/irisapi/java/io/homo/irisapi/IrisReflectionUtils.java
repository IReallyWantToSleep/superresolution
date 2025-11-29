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

import java.lang.reflect.Field;

public class IrisReflectionUtils {

    public static IrisCompositePassType getCompositePassType(Object object) {
        try {
            Class<?> passClazz = Class.forName("net.irisshaders.iris.pipeline.CompositeRenderer$Pass");
            Class<?> computeOnlyPassClazz = Class.forName("net.irisshaders.iris.pipeline.CompositeRenderer$ComputeOnlyPass");
            Class<?> objClazz = object.getClass();
            if (passClazz.isAssignableFrom(objClazz)) {
                Field computesField = passClazz.getDeclaredField("computes");
                computesField.setAccessible(true);
                Object[] computes = (Object[]) computesField.get(object);
                if (computes != null && computes.length > 0) {
                    return IrisCompositePassType.Mixed;
                } else {
                    return IrisCompositePassType.Common;
                }
            } else if (computeOnlyPassClazz.isAssignableFrom(objClazz)) {
                return IrisCompositePassType.ComputeOnly;
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException();
    }
}
