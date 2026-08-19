/*
 * Copyright (C) 2026 Nozyx
 *
 * This file is part of StriderLoader.
 *
 * StriderLoader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import net.bytebuddy.asm.Advice;

@StriderLoaderInternal
public final class StriderHooks {

    private StriderHooks() {}

    public static class ReadyHook {

        private ReadyHook() {}

        @Advice.OnMethodEnter
        public static void onEnter() {
            StriderLoader.handleReadyEvent();
        }
    }

    public static class BrandHook {

        private BrandHook() {}

        public static String intercept() {
            return "strider";
        }
    }
}
