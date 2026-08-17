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

/**
 * Represents a class bytecode transformer.
 * Provides support for modifying classes as they are loaded by StriderLoader.
 */
@FunctionalInterface
public interface ClassTransformer {

    /**
     * Transforms the class bytecode.
     *
     * @param className binary class name
     * @param bytecode original class bytecode
     * @return transformed bytecode
     */
    byte[] transform(String className, byte[] bytecode);
}
