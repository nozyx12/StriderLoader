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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@StriderLoaderInternal
final class StriderLogger {

    private static final Logger LOGGER =
            LogManager.getLogger("StriderLoader");

    private StriderLogger() {}

    static void info(String message) {
        LOGGER.info(message);
    }

    static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }

    static void warn(String message) {
        LOGGER.warn(message);
    }

    static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    static void error(String message) {
        LOGGER.error(message);
    }

    static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }

    static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }

    static void error(
            String message,
            Throwable throwable,
            Object... args
    ) {
        LOGGER.error(message, args, throwable);
    }

    static void debug(String message) {
        LOGGER.debug(message);
    }

    static void debug(String message, Object... args) {
        LOGGER.debug(message, args);
    }
}
