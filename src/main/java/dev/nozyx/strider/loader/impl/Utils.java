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

package dev.nozyx.strider.loader.impl;

import dev.nozyx.strider.loader.api.StriderLoaderInternal;

@StriderLoaderInternal
final class Utils {

    private Utils() {}

    static String getJavaMajorVersion() {
        String version = System.getProperty("java.version");
        String major;

        if (version.startsWith("1.")) major = version.split("\\.")[1];
        else major = version.split("\\.")[0];

        return major;
    }
}
