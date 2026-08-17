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
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

@StriderLoaderInternal
public final class StriderLoader implements IStriderLoader {

    public static final String LOADER_VERSION = "1.0.0";

    public static final StriderLoader INSTANCE = new StriderLoader();

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
                new Thread(() -> {
                    ui.setStatus("Launching game");

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}

                    ui.close();
                }).start();
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

        classLoader.addTransformer((className, bytecode) -> {
            GameConfig.HookDescriptor hook = gameConfig.hooks().get("readyEvent");

            if (!className.equals(hook.clazz())) {
                return bytecode;
            }

            ClassFileLocator locator = new ClassFileLocator.Compound(
                    new ClassFileLocator.Simple(
                            Map.of(className, bytecode)
                    ),
                    ClassFileLocator.ForClassLoader.of(classLoader)
            );

            TypePool typePool = TypePool.Default.of(locator);

            TypeDescription type = typePool
                    .describe(className)
                    .resolve();

            try (DynamicType.Unloaded<?> unloaded = new ByteBuddy()
                    .redefine(type, locator)
                    .visit(
                            Advice.to(StriderHooks.ReadyHook.class)
                                    .on(ElementMatchers.named(hook.name()).and(ElementMatchers.hasDescriptor(hook.descriptor())))
                    )
                    .make()) {

                return unloaded.getBytes();
            }
        });

        classLoader.addTransformer((className, bytecode) -> {
            GameConfig.HookDescriptor hook = gameConfig.hooks().get("brand");

            if (!className.equals(hook.clazz())) {
                return bytecode;
            }

            ClassFileLocator locator = new ClassFileLocator.Compound(
                    new ClassFileLocator.Simple(
                            Map.of(className, bytecode)
                    ),
                    ClassFileLocator.ForClassLoader.of(classLoader)
            );

            TypePool typePool = TypePool.Default.of(locator);

            TypeDescription type = typePool
                    .describe(className)
                    .resolve();

            try (DynamicType.Unloaded<?> unloaded = new ByteBuddy()
                    .redefine(type, locator)
                    .method(ElementMatchers.named(hook.name()).and(ElementMatchers.hasDescriptor(hook.descriptor())))
                    .intercept(MethodDelegation.to(StriderHooks.BrandHook.class))
                    .make()) {

                return unloaded.getBytes();
            }
        });
    }

    public void handleReadyEvent() {
        addonManager.onReady();
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
