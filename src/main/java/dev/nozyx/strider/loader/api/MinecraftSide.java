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

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

/**
 * Defines the side where a mod is intended to run.
 */
public enum MinecraftSide {

    /**
     * Mod runs only on the client side.
     */
    CLIENT,

    /**
     * Mod runs only on the server side.
     */
    SERVER,

    /**
     * Mod runs on both client and server sides.
     */
    COMMON;

    /**
     * Creates a Minecraft side from its string representation.
     *
     * <p>The value is converted to uppercase using the root locale before
     * being converted to the corresponding enum constant.</p>
     *
     * @param value the string representation of the Minecraft side
     * @return the corresponding MinecraftSide enum value
     * @throws IllegalArgumentException if the value does not match any enum constant
     */
    @JsonCreator
    public static MinecraftSide fromString(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
