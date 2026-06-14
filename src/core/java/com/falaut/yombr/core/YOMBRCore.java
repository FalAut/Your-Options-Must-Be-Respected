package com.falaut.yombr.core;

import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Applies modpack-provided default files before Minecraft reads options and
 * regular configuration files.
 *
 * <p>Default roots live under the live config directory, for example
 * {@code .minecraft/config/yosbr}
 * root aliases are hardcoded so the bootstrap path stays zero-config. Files
 * inside each root are mapped to the live game directory by
 * {@link #mapDefaultToTarget(Path, Path, Path, Path)}.</p>
 */
public final class YOMBRCore {
    public static final String OPTIONS_FILE_NAME = "options.txt";
    public static final String OPTIONS_MARKER_FILE_NAME = "options_applied.flag";
    public static final String CONFIG_PREFIX = "config";
    public static final String DEFAULT_ROOT_FOLDERS = "yosbr";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final String MARKER_CONTENTS = "applied";

    private YOMBRCore() {
    }

    /**
     * Runs the early default-file synchronization once per JVM.
     *
     * <p>NeoForge may construct language loaders more than once during mod
     * discovery, so this method is guarded by an atomic initialization flag.</p>
     */
    public static void apply() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        Path gameDir = FMLPaths.GAMEDIR.get().normalize();
        Path configDir = FMLPaths.CONFIGDIR.get().normalize();

        processDefaultRoot(gameDir, configDir);
    }

    /**
     * Maps a default file to its live destination.
     *
     * <p>If the source-relative path starts with {@code config/}, that prefix is
     * removed and the rest is resolved under the live config directory. All
     * other files are resolved directly under the live game directory.</p>
     *
     * @param gameDir the live Minecraft game directory
     * @param configDir the live Minecraft config directory
     * @param root the default root being scanned
     * @param sourceFile the source default file under {@code root}
     * @return the live target path for {@code sourceFile}
     */
    public static Path mapDefaultToTarget(Path gameDir, Path configDir, Path root, Path sourceFile) {
        Path relativePath = root.relativize(sourceFile);
        if (relativePath.getNameCount() > 1 && CONFIG_PREFIX.equals(relativePath.getName(0).toString())) {
            return configDir.resolve(relativePath.subpath(1, relativePath.getNameCount())).normalize();
        }
        return gameDir.resolve(relativePath).normalize();
    }

    private static void processDefaultRoot(Path gameDir, Path configDir) {
        if (!isSafeDefaultFolderName()) {
            YOMBRLanguageLoader.LOGGER.error("Skipping unsafe default folder name '{}'.", YOMBRCore.DEFAULT_ROOT_FOLDERS);
            return;
        }

        Path root = configDir.resolve(YOMBRCore.DEFAULT_ROOT_FOLDERS).normalize();
        if (!root.startsWith(configDir)) {
            YOMBRLanguageLoader.LOGGER.error("Skipping default folder outside the config directory: {}", YOMBRCore.DEFAULT_ROOT_FOLDERS);
            return;
        }

        if (!Files.isDirectory(root)) {
            YOMBRLanguageLoader.LOGGER.debug("Default root not found, skipping: {}", displayPath(gameDir, root));
            return;
        }

        try {
            Files.walkFileTree(root, new SyncVisitor(gameDir, configDir, root));
        } catch (IOException exception) {
            YOMBRLanguageLoader.LOGGER.error("Failed to process default root {}; continuing with remaining roots.",
                    displayPath(gameDir, root), exception);
        }
    }

    private static void syncDefaultFile(Path gameDir, Path sourceFile, Path targetFile) {
        if (isOptionsTarget(gameDir, targetFile)) {
            syncOptionsFile(gameDir, sourceFile, targetFile);
            return;
        }
        syncRegularFile(gameDir, sourceFile, targetFile);
    }

    private static void syncOptionsFile(Path gameDir, Path sourceFile, Path targetFile) {
        Path markerFile = gameDir.resolve(OPTIONS_MARKER_FILE_NAME);
        String targetDisplay = displayPath(gameDir, targetFile);

        try {
            if (Files.notExists(targetFile)) {
                copyAtomically(sourceFile, targetFile, false);
                writeStringAtomically(markerFile);
                YOMBRLanguageLoader.LOGGER.info("Copied default options to {}.", targetDisplay);
                return;
            }

            if (Files.exists(markerFile)) {
                YOMBRLanguageLoader.LOGGER.debug("Keeping existing {} because the marker is present.", targetDisplay);
                return;
            }

            copyAtomically(sourceFile, targetFile, true);
            writeStringAtomically(markerFile);
            YOMBRLanguageLoader.LOGGER.info("Replaced launcher-created {} and wrote the marker.", targetDisplay);
        } catch (IOException exception) {
            YOMBRLanguageLoader.LOGGER.error("Failed to apply default options from {} to {}.",
                    displayPath(gameDir, sourceFile), targetDisplay, exception);
        }
    }

    private static void syncRegularFile(Path gameDir, Path sourceFile, Path targetFile) {
        String targetDisplay = displayPath(gameDir, targetFile);

        try {
            if (Files.exists(targetFile)) {
                YOMBRLanguageLoader.LOGGER.debug("Keeping existing default target {}.", targetDisplay);
                return;
            }

            copyAtomically(sourceFile, targetFile, false);
            YOMBRLanguageLoader.LOGGER.info("Copied default target {}.", targetDisplay);
        } catch (IOException exception) {
            YOMBRLanguageLoader.LOGGER.error("Failed to copy default file from {} to {}.",
                    displayPath(gameDir, sourceFile), targetDisplay, exception);
        }
    }

    /**
     * Copies a file through a temporary sibling and then moves it into place.
     *
     * <p>The final move is attempted with {@link StandardCopyOption#ATOMIC_MOVE}
     * first. When the target already exists, the new file replaces it in a
     * single move so the live file is never left half-written.</p>
     */
    private static void copyAtomically(Path source, Path target, boolean replaceExisting) throws IOException {
        ensureParentDirectoryExists(target);

        Path parent = target.getParent();
        Path tempFile = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");

        try {
            Files.copy(source, tempFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            moveAtomically(tempFile, target, replaceExisting);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void writeStringAtomically(Path target) throws IOException {
        ensureParentDirectoryExists(target);

        Path parent = target.getParent();
        Path tempFile = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(tempFile, YOMBRCore.MARKER_CONTENTS);
            moveAtomically(tempFile, target, true);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void moveAtomically(Path source, Path target, boolean replaceExisting) throws IOException {
        try {
            if (replaceExisting) {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (AtomicMoveNotSupportedException exception) {
            YOMBRLanguageLoader.LOGGER.debug("Atomic move is not supported for {}; falling back to regular move.", target.getFileName());
            if (replaceExisting) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
        }
    }

    private static void ensureParentDirectoryExists(Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static boolean isOptionsTarget(Path gameDir, Path targetFile) {
        return gameDir.resolve(OPTIONS_FILE_NAME).normalize().equals(targetFile.normalize());
    }

    private static boolean isSafeDefaultFolderName() {
        Path folderPath = Path.of(YOMBRCore.DEFAULT_ROOT_FOLDERS);
        return folderPath.getRoot() == null && !folderPath.isAbsolute() && !folderPath.normalize().startsWith("..");
    }

    private static String displayPath(Path gameDir, Path path) {
        Path normalizedGameDir = gameDir.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        try {
            if (normalizedPath.startsWith(normalizedGameDir)) {
                Path relative = normalizedGameDir.relativize(normalizedPath);
                if (relative.getNameCount() == 0) {
                    return ".";
                }
                return relative.toString();
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to the file name below when paths are on different roots.
        }

        Path fileName = normalizedPath.getFileName();
        return fileName == null ? normalizedPath.toString() : fileName.toString();
    }

    private static final class SyncVisitor extends SimpleFileVisitor<Path> {
        private final Path gameDir;
        private final Path configDir;
        private final Path root;

        private SyncVisitor(Path gameDir, Path configDir, Path root) {
            this.gameDir = gameDir;
            this.configDir = configDir;
            this.root = root;
        }

        @Override
        public @NotNull FileVisitResult visitFile(@NotNull Path sourceFile, BasicFileAttributes attributes) {
            if (!attributes.isRegularFile()) {
                return FileVisitResult.CONTINUE;
            }

            try {
                Path targetFile = mapDefaultToTarget(gameDir, configDir, root, sourceFile);
                syncDefaultFile(gameDir, sourceFile, targetFile);
            } catch (RuntimeException exception) {
                YOMBRLanguageLoader.LOGGER.error("Failed to map default file {}; continuing.",
                        displayPath(gameDir, sourceFile), exception);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public @NotNull FileVisitResult visitFileFailed(@NotNull Path file, @NotNull IOException exception) {
            YOMBRLanguageLoader.LOGGER.error("Cannot access default file {}; continuing.", displayPath(gameDir, file), exception);
            return FileVisitResult.CONTINUE;
        }
    }
}