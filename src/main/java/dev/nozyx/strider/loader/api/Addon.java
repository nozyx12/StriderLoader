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
 * Represents an addon entry point for StriderLoader.
 * <p>
 * Addons must implement this interface to receive the load event.
 * </p>
 */
public interface Addon {

    /**
     * Called when the addon is loaded by the loader.
     *
     * @param loader The {@link IStriderLoader} instance.
     */
    void onInitialize(IStriderLoader loader);

    /**
     * Called when the game has reached the phase where its registries are fully
     * initialized, operational, and ready to be modified.
     *
     * @param loader The {@link IStriderLoader} instance.
     */
    void onReady(IStriderLoader loader);
}
