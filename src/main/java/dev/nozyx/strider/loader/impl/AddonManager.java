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

import dev.nozyx.strider.loader.api.*;
import dev.nozyx.strider.loader.impl.exceptions.DependencyException;
import dev.nozyx.strider.loader.impl.exceptions.DuplicateAddonIdException;
import dev.nozyx.strider.loader.impl.exceptions.ReservedAddonIdException;
import org.semver4j.Semver;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@StriderLoaderInternal
final class AddonManager {

    private final IStriderLoader loader;

    private final Map<String, AddonContainer> addonMap = new HashMap<>();
    private final List<String> loadOrder = new ArrayList<>();
    private final Map<AddonInfo, Addon> addonClasses = new HashMap<>();

    AddonManager(IStriderLoader loader) {
        this.loader = loader;
    }

    void resolveLoadOrder() {
        Set<String> visited = new HashSet<>();
        Set<String> temp = new HashSet<>();
        addonMap.keySet().forEach(addonId -> visit(addonId, visited, temp));
    }

    private void visit(String addonId, Set<String> visited, Set<String> temp) {
        try {
            if (visited.contains(addonId)) return;
            if (temp.contains(addonId)) throw new DependencyException("Cycle detected with addon: " + addonId);

            temp.add(addonId);
            AddonContainer addon = addonMap.get(addonId);
            AddonInfo info = addon.info();

            info.dependencies().forEach((depId, depVersionRange) -> {
                if (depId.equals("minecraft") || depId.equals("striderloader") || depId.equals("java")) return;
                if (depId.equals(addonId)) throw new DependencyException("Addon '" + addonId + "' cannot depend on itself");
                if (!addonMap.containsKey(depId)) throw new DependencyException("Missing dependency: " + depId + " for " + addonId);

                Semver installedDepSemver = Semver.coerce(addonMap.get(depId).info().version());
                if (!installedDepSemver.satisfies(depVersionRange)) throw new DependencyException("Dependency version mismatch: addon '" + addonId + "' requires '" + depId + "' in version range '" + depVersionRange + "' but found version " + addonMap.get(depId).info().version());
                visit(depId, visited, temp);
            });

            String mcVersion = info.dependencies().get("minecraft");
            String loaderVersion = info.dependencies().get("striderloader");
            String javaVersion = info.dependencies().get("java");

            if (mcVersion != null) {
                Semver semverMinecraftVersion = Semver.coerce(loader.getMinecraftVersion());
                if (!semverMinecraftVersion.satisfies(mcVersion)) throw new DependencyException("Addon requires Minecraft version in range '" + mcVersion + "' but the current version is " + loader.getMinecraftVersion());
            }

            if (loaderVersion != null) {
                Semver semverStriderLoaderVersion = Semver.coerce(loader.getLoaderVersion());
                if (!semverStriderLoaderVersion.satisfies(loaderVersion)) throw new DependencyException("Addon requires StriderLoader version in range '" + loaderVersion + "' but the current version is " + loader.getLoaderVersion());
            }

            if (javaVersion != null) {
                Semver semverJavaVersion = Semver.coerce(Utils.getJavaMajorVersion());
                if (!semverJavaVersion.satisfies(javaVersion)) throw new DependencyException("Addon requires Java version in range '" + javaVersion + "' but the current version is " + Utils.getJavaMajorVersion());
            }

            temp.remove(addonId);
            visited.add(addonId);
            loadOrder.add(addonId);
        } catch (Throwable th) {
            loader.crash("Addon dependency resolution failed:\nFailed to resolve addon dependencies and compute load order for addon '" + addonId + "'", th);
        }
    }

    void analyseAddon(File addonFile) {
        StriderLogger.info("--- Analysing: {} ---", addonFile.getPath());
        try (JarFile jar = new JarFile(addonFile)) {
            JarEntry entry = jar.getJarEntry("strideraddon.json");
            if (entry == null) throw new FileNotFoundException("File 'strideraddon.json' not found");

            try (InputStream is = jar.getInputStream(entry)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) baos.write(buffer, 0, read);

                String json = baos.toString(StandardCharsets.UTF_8);
                AddonInfo info = AddonInfo.fromJson(json);

                if (info.id().equals("minecraft") || info.id().equals("striderloader") || info.id().equals("java")) throw new ReservedAddonIdException(info.id());
                if (addonMap.containsKey(info.id())) throw new DuplicateAddonIdException(info.id());

                StriderLogger.info("ID: {}", info.id());
                StriderLogger.info("Name: {}", info.name());
                StriderLogger.info("Version: {}", info.version());
                StriderLogger.info("Description: {}", info.description());
                StriderLogger.info("Author: {}", info.author());
                StriderLogger.info("Side: {}", info.side().name());
                StriderLogger.info("Addon class: {}", info.addonClass());

                if (!info.side().equals(loader.getMinecraftSide()) && !info.side().equals(MinecraftSide.COMMON)) throw new RuntimeException("Addon with ID '" + info.id() + "' is made for Minecraft " + info.side() + " but the current side is " + loader.getMinecraftSide());

                addonMap.put(info.id(), new AddonContainer(info, addonFile.toPath()));

                StriderLogger.info("> ✔ ADDON ANALYSIS SUCCESS");
            }
        } catch (Throwable th) {
            loader.crash("Addon analysis failed:\nFailed to analyse addon file: " + addonFile.getPath(), th);
        }
    }

    void loadAddons(StriderClassLoader classLoader) {
        for (String addonId : loadOrder) {
            AddonContainer addonContainer = addonMap.get(addonId);
            Path jarPath = addonContainer.jarPath().toAbsolutePath();

            StriderLogger.info("--- Loading: " + jarPath + " ---");

            try {
                classLoader.addUrl(jarPath);
            } catch (MalformedURLException ignored) {}

            StriderLogger.info("> Loading addon class...");

            Addon addonInstance = null;
            try {
                Class<?> clazz = classLoader.loadClass(addonContainer.info().addonClass());

                if (Addon.class.isAssignableFrom(clazz)) {
                    Class<? extends Addon> addonClazz = clazz.asSubclass(Addon.class);
                    addonInstance = addonClazz.getDeclaredConstructor().newInstance();
                } else throw new RuntimeException("Class '" + addonContainer.info().addonClass() + "' does not implement 'dev.nozyx.strider.loader.api.Addon'");
            } catch (Throwable th) {
                if (th instanceof ClassNotFoundException) loader.crash("Addon load failed:\nAddon class not found for addon '" + addonContainer.info().id() + "' : " + addonContainer.info().addonClass(), th);
                loader.crash("Addon load failed:\nAddon class load and instantiation failed for addon '" + addonContainer.info().id() + "' : " + addonContainer.info().addonClass(), th);
            }

            addonClasses.put(addonContainer.info(), addonInstance);

            StriderLogger.info("> Running 'onInitialize' method...");

            try {
                addonInstance.onInitialize(loader);
            } catch (Throwable th) {
                loader.crash("Addon load failed:\nAddon 'onInitialize' method failed for addon '" + addonContainer.info().id() + "'", th);
            }

            StriderLogger.info("> ✔ ADDON LOAD SUCCESS");
        }
    }

    void onReady() {
        StriderLogger.info("Minecraft ready hook called!");
        StriderLogger.info("> Running 'onReady' method for loaded addons...");

        addonClasses.forEach((info, addon) -> {
            try {
                addon.onReady(loader);
            } catch (Throwable th) {
                loader.crash(
                        "Addon ready failed:\n" +
                                "Addon 'onReady' method failed for addon '" +
                                info.id() + "'",
                        th
                );
            }
        });
    }

    List<File> findJARs(File directory) {
        List<File> jarFiles = new ArrayList<>();
        if (directory == null || !directory.isDirectory()) return jarFiles;

        File[] files = directory.listFiles((_, name) -> name.toLowerCase().endsWith(".jar"));
        if (files != null) for (File f : files) if (f.isFile()) jarFiles.add(f);

        return jarFiles;
    }

    List<String> getLoadOrder() {
        return loadOrder;
    }

    Map<String, AddonContainer> getAddonMap() {
        return addonMap;
    }
}
