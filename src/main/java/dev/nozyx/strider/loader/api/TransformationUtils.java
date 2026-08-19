/*
 * Copyright (C) 2026 Nozyx
 *
 * This file is part of StriderLoader.
 *
 * StriderLoader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * StriderLoader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with StriderLoader. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nozyx.strider.loader.api;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.pool.TypePool;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Provides utility methods for transforming classes using ByteBuddy.
 *
 * <p>This class provides a simplified API for applying straightforward
 * transformations to a class or its members without requiring the more
 * complex configuration of a {@link ClassTransformer}.
 */
public final class TransformationUtils {

    private TransformationUtils() {}

    /**
     * Applies a ByteBuddy transformation to a specific class.
     *
     * <p>The transformation can modify the class itself or any of its members,
     * such as methods and fields. This provides a convenient way to perform
     * common transformations without requiring a custom {@link ClassTransformer}.
     *
     * @param classLoader    the {@link StriderClassLoader} to register the transformation with
     * @param clazz          the fully qualified name of the class to transform
     * @param transformation the transformation to apply to the {@link DynamicType.Builder}
     */
    public static void transformClass(
            StriderClassLoader classLoader,
            String clazz,
            UnaryOperator<DynamicType.Builder<?>> transformation
    ) {
        classLoader.addTransformer((className, bytecode) -> {
            if (!className.equals(clazz)) {
                return bytecode;
            }

            ClassFileLocator locator = new ClassFileLocator.Compound(
                    new ClassFileLocator.Simple(
                            Map.of(className, bytecode)
                    ),
                    ClassFileLocator.ForClassLoader.of(classLoader)
            );

            TypePool typePool = TypePool.Default.of(locator);

            TypeDescription type = typePool
                    .describe(className)
                    .resolve();

            DynamicType.Builder<?> builder = new ByteBuddy()
                    .redefine(type, locator);

            builder = transformation.apply(builder);

            try (DynamicType.Unloaded<?> unloaded = builder.make()) {
                return unloaded.getBytes();
            }
        });
    }
}
