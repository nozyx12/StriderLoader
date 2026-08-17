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

import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Represents the metadata of an addon loaded by StriderLoader.
 *
 * <p>Contains information such as addon ID, version, description, author,
 * targeted Minecraft side, implementation class and dependencies.</p>
 *
 * <p>This class is primarily used by addons and the loader to query
 * addon information.</p>
 *
 * @param id the unique identifier of the addon
 * @param name the human-readable name of the addon
 * @param version the version of the addon
 * @param description the description of the addon
 * @param author the author of the addon
 * @param side the Minecraft side targeted by the addon
 * @param addonClass the fully qualified implementation class of the addon
 * @param dependencies the addon's dependencies and their required versions
 */
public record AddonInfo(
        String id,
        String name,
        String version,
        String description,
        String author,
        MinecraftSide side,

        @StriderLoaderInternal
        String addonClass,

        @StriderLoaderInternal
        Map<String, String> dependencies
) {
    /**
     * Parses an AddonInfo instance from JSON.
     *
     * @param json the JSON string
     * @return the parsed AddonInfo
     * @throws IllegalArgumentException if the JSON cannot be parsed
     */
    @StriderLoaderInternal
    public static AddonInfo fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, AddonInfo.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse addon metadata",
                    e
            );
        }
    }

    /**
     * Serializes this AddonInfo instance to JSON.
     *
     * @return the JSON representation
     * @throws IllegalStateException if serialization fails
     */
    @StriderLoaderInternal
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to serialize addon metadata",
                    e
            );
        }
    }
}
