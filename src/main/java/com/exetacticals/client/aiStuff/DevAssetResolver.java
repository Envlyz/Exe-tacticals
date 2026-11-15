package com.exetacticals.client.aiStuff;

import com.exetacticals.GCR;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class DevAssetResolver {

    public static final String SOURCE_ROOT_PROPERTY = "exe_tacticals.hotreload.sourceRoot";

    private static final String[] SOURCE_ROOT_CANDIDATES = {
            "src/main/resources",
            "src/generated/resources"
    };

    private static List<Path> sourceRoots;
    private static boolean resolved;

    private DevAssetResolver() {
    }

    public static synchronized List<Path> sourceRoots() {
        if (resolved) {
            return sourceRoots;
        }
        resolved = true;
        List<Path> roots = new ArrayList<>();

        String explicit = System.getProperty(SOURCE_ROOT_PROPERTY);
        if (explicit != null && !explicit.isBlank()) {
            for (String part : explicit.split(File.pathSeparator)) {
                if (!part.isBlank()) {
                    addDirectory(roots, Paths.get(part));
                }
            }
        } else if (GCR.IS_DEVELOPMENT) {
            Path dir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
            for (int depth = 0; depth < 4 && dir != null; depth++, dir = dir.getParent()) {
                int before = roots.size();
                for (String candidate : SOURCE_ROOT_CANDIDATES) {
                    addDirectory(roots, dir.resolve(candidate));
                }
                if (roots.size() > before) {
                    break;
                }
            }
        }

        sourceRoots = List.copyOf(roots);
        if (!sourceRoots.isEmpty()) {
            GCR.LOGGER.info("[GCR HotReload] dev source roots: {}", sourceRoots);
        } else if (GCR.IS_DEVELOPMENT) {
            GCR.LOGGER.info("[GCR HotReload] no dev source root found, resources will be read from {}",
                    "the vanilla ResourceManager");
        }
        return sourceRoots;
    }

    private static void addDirectory(List<Path> roots, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized) && !roots.contains(normalized)) {
            roots.add(normalized);
        }
    }

    public static boolean enabled() {
        return !sourceRoots().isEmpty();
    }

    public static String toPackPath(ResourceLocation location) {
        return "assets/" + location.getNamespace() + "/" + location.getPath();
    }

    @Nullable
    public static Path resolveSourceFile(ResourceLocation location) {
        String packPath = toPackPath(location);
        for (Path root : sourceRoots()) {
            Path candidate = root.resolve(packPath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean hasSourceFile(ResourceLocation location) {
        return resolveSourceFile(location) != null;
    }

    public static boolean exists(ResourceLocation location, @Nullable ResourceManager fallback) {
        if (hasSourceFile(location)) {
            return true;
        }
        return fallback != null && fallback.getResource(location).isPresent();
    }

    @Nullable
    public static InputStream openOrNull(ResourceLocation location, @Nullable ResourceManager fallback) {
        Path source = resolveSourceFile(location);
        if (source != null) {
            try {
                return Files.newInputStream(source);
            } catch (IOException e) {
                GCR.LOGGER.warn("[GCR HotReload] failed to open dev source {}: {}", source, e.toString());
            }
        }
        if (fallback == null) {
            return null;
        }
        return fallback.getResource(location).map(resource -> {
            try {
                return resource.open();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).orElse(null);
    }
}
