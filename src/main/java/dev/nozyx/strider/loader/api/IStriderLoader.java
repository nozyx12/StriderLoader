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

import java.util.Map;

/**
 * Represents the core mod loader interface.
 * Provides access to loader information, mods, and game transformation utilities.
 */
public interface IStriderLoader {

    /**
     * Forces the loader to crash with a custom message.
     *
     * @param msg the crash message
     */
    void crash(String msg);

    /**
     * Forces the loader to crash with an exception.
     *
     * @param th the throwable causing the crash
     */
    void crash(Throwable th);

    /**
     * Forces the loader to crash with a custom message and an exception.
     *
     * @param msg the crash message
     * @param th the throwable causing the crash
     */
    void crash(String msg, Throwable th);

    /**
     * Returns the version of StriderLoader.
     *
     * @return the loader version string
     */
    String getLoaderVersion();

    /**
     * Returns the version of Minecraft the loader is running on.
     *
     * @return the Minecraft version string
     */
    String getMinecraftVersion();

    /**
     * Returns the side (client/server) the loader is running on.
     *
     * @return the MinecraftSide enum value
     */
    MinecraftSide getMinecraftSide();

    /**
     * Returns whether the loader UI is enabled.
     *
     * @return true if the UI is enabled, false otherwise
     */
    boolean isUiEnabled();

    /**
     * Returns the class loader used by the game.
     *
     * @return the {@link StriderClassLoader} instance
     */
    StriderClassLoader getClassLoader();

    /**
     * Returns an immutable map of all loaded addons, indexed by their addon IDs.
     *
     * @return a map of mod ID to {@link AddonContainer}
     */
    Map<String, AddonContainer> getAddons();
}
