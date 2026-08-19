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
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;

@StriderLoaderInternal
public final class StriderLoader implements IStriderLoader {

    static final String LOADER_VERSION = "1.1.1";

    private static final StriderLoader INSTANCE = new StriderLoader();

    private final File addonsFolder = new File("addons");

    private final StriderClassLoader classLoader = new StriderClassLoader();

    private final AddonManager addonManager = new AddonManager(this);

    private String minecraftVersion;
    private MinecraftSide minecraftSide;

    private boolean uiEnabled;

    private GameConfig.SideConfig gameConfig;

    private final StriderUI ui = new StriderUI();

    private StriderLoader() {}

    private void launch(String[] args) {
        try {
            StriderLogger.info("Starting StriderLoader v{}", LOADER_VERSION);

            setupEnvironment();

            if (uiEnabled) ui.start();

            displayEnvironmentInfo();

            doAddonLoading();

            StriderLogger.info("Launching game...");

            if (uiEnabled) {
                ui.setStatus("Launching game");
            }

            Class<?> clazz = classLoader.loadClass(gameConfig.entryPoint());
            clazz.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (Throwable th) {
            crash("An uncaught exception occurred during StriderLoader initialization", th);
        }
    }

    private void doAddonLoading() {
        StriderLogger.info("Searching for addons...");
        if (uiEnabled) ui.setStatus("Searching for addons");
        addonsFolder.mkdir();
        List<File> addonJars = addonManager.findJARs(addonsFolder);

        StriderLogger.info("Found {} addons", addonJars.size());

        if (!addonJars.isEmpty()) {
            StriderLogger.info("Starting to load addons!");

            StriderLogger.info("Analysing addons...");
            if (uiEnabled) ui.setStatus("Analysing addons");

            for (File addonJar : addonJars) addonManager.analyseAddon(addonJar);

            StriderLogger.info("Resolving load order and dependencies...");
            if (uiEnabled) ui.setStatus("Resolving load order and dependencies");
            addonManager.resolveLoadOrder();

            StriderLogger.info("Loading addons...");
            if (uiEnabled) ui.setStatus("Loading addons");

            addonManager.loadAddons(classLoader);
        }
    }

    private void displayEnvironmentInfo() {
        StriderLogger.info("Environment info:");
        StriderLogger.info("- Minecraft {} {}", minecraftSide.name(), minecraftVersion);
        StriderLogger.info("- UI enabled: {}", uiEnabled);
        StriderLogger.info("- OS: {} {} ({})", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
        StriderLogger.info("- JVM: {} {} (Vendor: {})", System.getProperty("java.vm.name"), System.getProperty("java.vm.version"), System.getProperty("java.vendor"));
        StriderLogger.info("- Java Home: {}", System.getProperty("java.home"));
    }

    private void setupEnvironment() {
        StriderLogger.info("Setting up environment...");

        String mcVersion = System.getProperty("striderloader.mcVersion", null);
        if (mcVersion == null) crash("JVM property 'striderloader.mcVersion' is not set");

        String mcSideStr = System.getProperty("striderloader.mcSide", null);
        if (mcSideStr == null) crash("JVM property 'striderloader.mcSide' is not set");

        MinecraftSide mcSide = null;

        switch (mcSideStr) {
            case "client":
                mcSide = MinecraftSide.CLIENT;
                break;
            case "server":
                mcSide = MinecraftSide.SERVER;
                break;
            default:
                crash("JVM property 'striderloader.mcSide' must be 'client' or 'server'");
        }

        uiEnabled = Boolean.parseBoolean(System.getProperty("striderloader.uiEnabled", "true"));

        String pid = ManagementFactory.getRuntimeMXBean()
                .getName()
                .split("@")[0];

        boolean hasStartOnFirstThread =
                "1".equals(System.getenv("JAVA_STARTED_ON_FIRST_THREAD_" + pid));

        if (uiEnabled && hasStartOnFirstThread) {
            uiEnabled = false;
            StriderLogger.warn("""
                UI will be disabled because -XstartOnFirstThread is enabled.
                This option is required by Minecraft on macOS,
                but prevents StriderLoader's startup UI from being handled correctly.""");
        }

        minecraftVersion = mcVersion;
        minecraftSide = mcSide;

        ObjectMapper mapper = new ObjectMapper();
        String resourcePath = "/config/" + minecraftVersion + ".json";

        try (InputStream input = this.getClass().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException(
                        "Unsupported Minecraft version: " + minecraftVersion
                );
            }

            if (minecraftSide == MinecraftSide.CLIENT) {
                gameConfig = mapper.readValue(input, GameConfig.class).client();
            } else {
                gameConfig = mapper.readValue(input, GameConfig.class).server();
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read game configuration for Minecraft version: " + minecraftVersion,
                    e
            );
        }

        Thread.currentThread().setContextClassLoader(classLoader);

        GameConfig.HookDescriptor readyHook = gameConfig.hooks().get("readyEvent");
        TransformationUtils.transformClass(
                classLoader,
                readyHook.clazz(),
                builder -> builder
                        .visit(
                                Advice.to(StriderHooks.ReadyHook.class)
                                        .on(
                                                ElementMatchers.named(readyHook.name())
                                                        .and(ElementMatchers.hasDescriptor(readyHook.descriptor()))
                                        )
                        )
        );

        GameConfig.HookDescriptor brandHook = gameConfig.hooks().get("brand");
        TransformationUtils.transformClass(
                classLoader,
                brandHook.clazz(),
                builder -> builder
                        .method(
                                ElementMatchers.named(brandHook.name())
                                        .and(ElementMatchers.hasDescriptor(brandHook.descriptor()))
                        )
                        .intercept(MethodDelegation.to(StriderHooks.BrandHook.class))
        );
    }

    public static void handleReadyEvent() {
        if (INSTANCE.uiEnabled) {
            INSTANCE.ui.close();
        }

        INSTANCE.addonManager.onReady();
    }

    @Override
    public Map<String, AddonContainer> getAddons() {
        return Map.copyOf(addonManager.getAddonMap());
    }

    @Override
    public void crash(String msg) {
        StriderLogger.error("✘ -- STRIDERLOADER CRASH -- ✘");
        StriderLogger.error("Message: {}", msg);

        if (uiEnabled) {
            String status = ui.getStatus();
            ui.setStatus("CRASH!");
            CrashDialog.showCrashDialog(null, msg, status);
        }

        Runtime.getRuntime().halt(1);
    }

    @Override
    public void crash(String msg, Throwable th) {
        StriderLogger.error("✘ -- STRIDERLOADER CRASH -- ✘");
        StriderLogger.error("Message: {}", th, msg);

        if (uiEnabled) {
            String status = ui.getStatus();
            ui.setStatus("CRASH!");
            CrashDialog.showCrashDialog(null, msg, status, th);
        }

        Runtime.getRuntime().halt(1);
    }

    @Override
    public void crash(Throwable th) {
        StriderLogger.error("✘ -- STRIDERLOADER CRASH -- ✘", th);

        if (uiEnabled) {
            String status = ui.getStatus();
            ui.setStatus("CRASH!");
            CrashDialog.showCrashDialog(null, status, th);
        }

        Runtime.getRuntime().halt(1);
    }

    @Override
    public String getLoaderVersion() {
        return LOADER_VERSION;
    }

    @Override
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    @Override
    public MinecraftSide getMinecraftSide() {
        return minecraftSide;
    }

    @Override
    public boolean isUiEnabled() {
        return uiEnabled;
    }

    @Override
    public StriderClassLoader getClassLoader() {
        return classLoader;
    }

    static void main(String[] args) {
        INSTANCE.launch(args);
    }
}
