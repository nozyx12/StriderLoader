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

package dev.nozyx.strider.loader.api;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A class loader used by StriderLoader to load and transform game classes.
 *
 * <p>Classes outside the Minecraft and Mojang packages use the standard
 * parent-first class-loading strategy. Classes belonging to
 * {@code net.minecraft} or {@code com.mojang} are loaded using a child-first
 * strategy so they can be transformed before being defined by the JVM.</p>
 *
 * <p>Classes loaded by this class loader can be processed by registered
 * {@link ClassTransformer class transformers} before they are defined by
 * the JVM. Transformers are applied in the order in which they were
 * registered.</p>
 */
public final class StriderClassLoader extends URLClassLoader {

    private static final String MINECRAFT_PACKAGE = "net.minecraft.";
    private static final String MOJANG_PACKAGE = "com.mojang.";

    private final List<ClassTransformer> transformers = new ArrayList<>();

    /**
     * Creates a new {@code StriderClassLoader} using the current JVM
     * classpath as its initial classpath.
     */
    public StriderClassLoader() {
        this(getClasspathUrls(), getDefaultParent());
    }

    /**
     * Creates a new {@code StriderClassLoader} with the specified
     * classpath and parent class loader.
     *
     * @param urls the URLs from which classes and resources can be loaded
     * @param parent the parent class loader used for class loading
     */
    public StriderClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    private static boolean isChildFirstClass(String name) {
        return name.startsWith(MINECRAFT_PACKAGE)
                || name.startsWith(MOJANG_PACKAGE);
    }

    private static URL[] getClasspathUrls() {
        String classpath = System.getProperty("java.class.path");

        if (classpath == null || classpath.isBlank()) {
            return new URL[0];
        }

        String[] entries = classpath.split(File.pathSeparator);

        List<URL> urls = new ArrayList<>(entries.length);

        for (String entry : entries) {
            try {
                urls.add(Path.of(entry).toUri().toURL());
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to convert classpath entry to URL: " + entry,
                        e
                );
            }
        }

        return urls.toArray(URL[]::new);
    }

    /**
     * Loads a class using the configured class-loading strategy.
     *
     * @param name the fully qualified binary name of the class
     * @return the resulting {@link Class} object
     * @throws ClassNotFoundException if the class cannot be found
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    /**
     * Loads and optionally resolves a class.
     *
     * <p>Classes in {@code net.minecraft} and {@code com.mojang} are loaded
     * child-first. All other classes are loaded parent-first.</p>
     *
     * @param name the fully qualified binary name of the class
     * @param resolve whether the class should be resolved after loading
     * @return the resulting {@link Class} object
     * @throws ClassNotFoundException if the class cannot be found
     */
    @Override
    public Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {

        synchronized (getClassLoadingLock(name)) {

            Class<?> loaded = findLoadedClass(name);

            if (loaded == null) {
                if (isChildFirstClass(name)) {
                    loaded = loadChildFirst(name);
                } else {
                    loaded = loadParentFirst(name);
                }
            }

            if (resolve) {
                resolveClass(loaded);
            }

            return loaded;
        }
    }

    private Class<?> loadChildFirst(String name)
            throws ClassNotFoundException {

        try {
            return findClass(name);
        } catch (ClassNotFoundException ignored) {
            return loadParent(name);
        }
    }

    private Class<?> loadParentFirst(String name)
            throws ClassNotFoundException {

        try {
            return loadParent(name);
        } catch (ClassNotFoundException ignored) {
            return findClass(name);
        }
    }

    private Class<?> loadParent(String name)
            throws ClassNotFoundException {

        return getParent() != null
                ? getParent().loadClass(name)
                : findSystemClass(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytecode = findClassBytes(name);

        if (bytecode == null) {
            throw new ClassNotFoundException(name);
        }

        bytecode = transform(name, bytecode);

        return defineClass(
                name,
                bytecode,
                0,
                bytecode.length
        );
    }

    private byte[] findClassBytes(String name) {
        String resourceName = name.replace('.', '/') + ".class";

        URL resource = findResource(resourceName);

        if (resource == null) {
            return null;
        }

        try (var input = resource.openStream()) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read class: " + name,
                    e
            );
        }
    }

    private byte[] transform(String className, byte[] bytecode) {
        byte[] transformed = bytecode;

        for (ClassTransformer transformer : transformers) {
            transformed = transformer.transform(className, transformed);
        }

        return transformed;
    }

    /**
     * Adds a JAR file or directory to this class loader's classpath.
     *
     * @param path the JAR file or directory to add
     * @throws MalformedURLException if the path cannot be converted to a URL
     */
    public void addUrl(Path path) throws MalformedURLException {
        addURL(path.toUri().toURL());
    }

    /**
     * Registers a class transformer.
     *
     * @param transformer the transformer to register
     */
    public void addTransformer(ClassTransformer transformer) {
        transformers.add(transformer);
    }

    private static ClassLoader getDefaultParent() {
        return StriderClassLoader.class.getClassLoader();
    }
}
