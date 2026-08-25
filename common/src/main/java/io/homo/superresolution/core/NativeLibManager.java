/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core;

import io.homo.superresolution.api.platform.OperatingSystem;
import io.homo.superresolution.api.platform.OperatingSystemType;
import io.homo.superresolution.api.platform.SystemArchitecture;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.framegeneration.FrameGenerationDescriptions;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.core.utils.MessageBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class NativeLibManager {
    public static final String BASE_PATH = "lib";
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution/NativeLib");

    #if USE_DEBUG_LIB == 1
    public static final boolean USE_DEBUG_LIB = true;
    #else
    public static final boolean USE_DEBUG_LIB = false;
    #endif
    private static final List<NativeLib> libs = new ArrayList<>();
    public static NativeLib LIB_SUPER_RESOLUTION = null;
    public static NativeLib LIB_SUPER_RESOLUTION_D3D12 = null;
    /**
     * @deprecated use {@link #LIB_SUPER_RESOLUTION_D3D12}.
     */
    @Deprecated
    public static NativeLib LIB_SUPER_RESOLUTION_D3D12_INTEROP = null;
    public static NativeLib LIB_SUPER_RESOLUTION_FSR = null;
    public static NativeLib LIB_SUPER_RESOLUTION_FSR4 = null;
    public static NativeLib LIB_SUPER_RESOLUTION_XESS = null;
    public static NativeLib LIB_SUPER_RESOLUTION_NGX = null;
    public static NativeLib LIB_SUPER_RESOLUTION_STREAMLINE = null;
    public static NativeLib LIB_NGX_DLSSG_SNIPPET = null;
    private static boolean nativeApiAvailable;
    private static boolean librariesExtracted;
    private static boolean librariesLoaded;

    static {
        OperatingSystem operatingSystem = new OperatingSystem();
        if (operatingSystem.type == OperatingSystemType.WINDOWS && operatingSystem.arch == SystemArchitecture.X86_64) {
            boolean shouldExtract = VulkanPresentationFeature.isRequested() && SuperResolutionConfig.CURRENT_OS_TYPE == OperatingSystemType.WINDOWS;
            boolean shouldLoad = FrameGenerationDescriptions.mayUseStreamline(
                    SuperResolutionConfig.getFrameGenerationProvider());
            LIB_SUPER_RESOLUTION = new NativeLib(
                    "SuperResolution",
                    true,
                    true
            );
            LIB_SUPER_RESOLUTION_D3D12 = new NativeLib(
                    "SuperResolutionD3D12",
                    true,
                    false
            );
            LIB_SUPER_RESOLUTION_D3D12_INTEROP = LIB_SUPER_RESOLUTION_D3D12;
            LIB_SUPER_RESOLUTION_FSR = new NativeLib(
                    "SuperResolutionFSR",
                    false,
                    false
            );
            LIB_SUPER_RESOLUTION_FSR4 = new NativeLib(
                    "SuperResolutionFSR4",
                    false,
                    false
            );
            LIB_SUPER_RESOLUTION_XESS = new NativeLib(
                    "SuperResolutionXeSS",
                    false,
                    false
            );
            LIB_SUPER_RESOLUTION_NGX = new NativeLib(
                    "SuperResolutionNGX",
                    false,
                    false
            );
            LIB_SUPER_RESOLUTION_STREAMLINE = new NativeLib(
                    "SuperResolutionStreamline",
                    shouldLoad,
                    shouldExtract
            );

            libs.add(LIB_SUPER_RESOLUTION);
            libs.add(LIB_SUPER_RESOLUTION_D3D12);
            libs.add(LIB_SUPER_RESOLUTION_FSR);
            libs.add(LIB_SUPER_RESOLUTION_FSR4);
            libs.add(LIB_SUPER_RESOLUTION_XESS);
            libs.add(LIB_SUPER_RESOLUTION_NGX);
            libs.add(LIB_SUPER_RESOLUTION_STREAMLINE);
        } else if (operatingSystem.type == OperatingSystemType.ANDROID && operatingSystem.arch == SystemArchitecture.AARCH64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true, true);
            libs.add(LIB_SUPER_RESOLUTION);

        } else if (operatingSystem.type == OperatingSystemType.LINUX && operatingSystem.arch == SystemArchitecture.X86_64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true, true);
            LIB_SUPER_RESOLUTION_FSR = new NativeLib("SuperResolutionFSR", false, false);
            LIB_SUPER_RESOLUTION_NGX = new NativeLib("SuperResolutionNGX", true, false);
            LIB_NGX_DLSSG_SNIPPET = new NativeLib("libnvidia-ngx-dlssg", false, false, true);
            libs.add(LIB_SUPER_RESOLUTION);
            libs.add(LIB_SUPER_RESOLUTION_FSR);
            libs.add(LIB_SUPER_RESOLUTION_NGX);
            libs.add(LIB_NGX_DLSSG_SNIPPET);

        } else if (operatingSystem.type == OperatingSystemType.MACOS && operatingSystem.arch == SystemArchitecture.AARCH64) {
            LIB_SUPER_RESOLUTION = new NativeLib("SuperResolution", true, true);
            libs.add(LIB_SUPER_RESOLUTION);
        }
    }

    public static boolean nativeApiAvailable() {
        return nativeApiAvailable;
    }

    public static boolean d3d12Available() {
        return LIB_SUPER_RESOLUTION_D3D12 != null
                && LIB_SUPER_RESOLUTION_D3D12.available;
    }

    /**
     * @deprecated use {@link #d3d12Available()}.
     */
    @Deprecated
    public static boolean d3d12InteropAvailable() {
        return d3d12Available();
    }

    public static void createLibraryDir(Path path) {
        File dir = path.toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.error("Unable to create directory: {}", dir);
        }
    }

    public static synchronized void extract(Path path) {
        if (librariesExtracted) {
            return;
        }
        LOGGER.info("Extracting native dependencies");
        createLibraryDir(path);
        List<String> requiredFailures = new ArrayList<>();
        List<String> optionalFailures = new ArrayList<>();

        for (NativeLib lib : libs) {
            try {
                if (!extractLibrary(path, lib)) {
                    if (lib.required) {
                        requiredFailures.add(lib.fileName);
                        LOGGER.error("Failed to extract required dependency {}", lib.fileName);
                    } else {
                        optionalFailures.add(lib.fileName);
                        LOGGER.warn("Failed to extract optional dependency {}; skipping it", lib.fileName);
                    }
                }
            } catch (Exception e) {
                if (lib.required) {
                    requiredFailures.add("%s: %s".formatted(lib.fileName, e.getMessage()));
                    LOGGER.error("Failed to extract required dependency {}: {}", lib.fileName, e.getMessage());
                    LOGGER.error("Native dependency extraction failure details", e);
                } else {
                    optionalFailures.add(lib.fileName);
                    LOGGER.warn("Failed to extract optional dependency {}; skipping it: {}", lib.fileName, e.getMessage());
                }
            }
        }

        if (!requiredFailures.isEmpty()) {
            String errorMsg = String.join(", ", requiredFailures);
            LOGGER.error("Required dependency extraction failed: {}", errorMsg);
            MessageBox.createError(
                    "SuperResolution在提取必要依赖库时失败，失败的库：%s".formatted(errorMsg),
                    "Error"
            );
            throw new RuntimeException("Required dependency extraction failed: " + errorMsg);
        }

        if (!optionalFailures.isEmpty()) {
            LOGGER.info("Skipped optional dependencies: {}", String.join(", ", optionalFailures));
        }

        LOGGER.info("Native dependencies extracted to {}", path);
        librariesExtracted = true;
    }

    public static synchronized void load(Path path) {
        if (librariesLoaded) {
            nativeApiAvailable = true;
            return;
        }
        createLibraryDir(path);
        for (NativeLib lib : libs) {
            if (lib.extractedPath == null) {
                if (lib.required) {
                    LOGGER.error("Required dependency {} was not extracted and cannot be loaded", lib.fileName);
                    throw new RuntimeException("Required dependency " + lib.fileName + " was not extracted");
                } else {
                    LOGGER.warn("Optional dependency {} was not extracted; skipping load", lib.fileName);
                    continue;
                }
            }

            File f = lib.getTargetPath(path).toFile();
            if (lib.loadAtStartup) {
                try {
                    LOGGER.info("Loading native dependency: {}", f.getAbsolutePath());
                    System.load(f.getAbsolutePath());
                    lib.available = true;
                } catch (Throwable e) {
                    if (lib.required) {
                        LOGGER.error("Failed to load required dependency {}: {}", lib.fileName, e.getMessage());
                        throw new RuntimeException("Failed to load required dependency: " + lib.fileName, e);
                    } else {
                        LOGGER.warn("Failed to load optional dependency {}; skipping it: {}", lib.fileName, e.getMessage());
                        lib.available = false;
                    }
                }
            }
        }
        librariesLoaded = true;
        nativeApiAvailable = true;
    }

    private static boolean _writeFile(
            InputStream in,
            Path filePath,
            String embeddedChecksum,
            String existingChecksum
    ) throws IOException {
        if (in == null) {
            return false;
        }

        Path parent = filePath.getParent();
        Files.createDirectories(parent);
        Path temporaryFile = Files.createTempFile(parent, filePath.getFileName().toString() + ".", ".tmp");
        try {
            Files.copy(in, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            try {
                replaceFile(temporaryFile, filePath);
            } catch (IOException replacementFailure) {
                String currentChecksum = getExistingChecksum(filePath);
                if (embeddedChecksum.equals(currentChecksum)) {
                    LOGGER.info("Native dependency {} was written by another Minecraft instance with a matching checksum; reusing it", filePath);
                    return true;
                }
                throw createReplacementException(
                        filePath,
                        embeddedChecksum,
                        existingChecksum,
                        currentChecksum,
                        replacementFailure
                );
            }
            return true;
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static void replaceFile(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String calculateChecksum(InputStream in) throws IOException {
        MessageDigest digest = createSha256Digest();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = in.read(buffer)) != -1) {
            digest.update(buffer, 0, length);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String getExistingChecksum(Path filePath) {
        if (!Files.exists(filePath)) {
            return "<不存在>";
        }
        if (!Files.isRegularFile(filePath)) {
            return "<不是普通文件>";
        }
        try (InputStream in = Files.newInputStream(filePath)) {
            return calculateChecksum(in);
        } catch (IOException e) {
            return "<无法读取: %s>".formatted(e.getMessage());
        }
    }

    private static MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    private static IOException createReplacementException(
            Path filePath,
            String embeddedChecksum,
            String previousChecksum,
            String currentChecksum,
            IOException cause
    ) {
        StringBuilder message = new StringBuilder()
                .append("无法替换 checksum 不一致的依赖库 ")
                .append(filePath.toAbsolutePath())
                .append("；内置 SHA-256: ")
                .append(embeddedChecksum)
                .append("；替换前 SHA-256: ")
                .append(previousChecksum)
                .append("；当前 SHA-256: ")
                .append(currentChecksum);
        if (new OperatingSystem().type == OperatingSystemType.WINDOWS) {
            message.append("。目标 DLL 可能正被另一个 Minecraft 实例加载并锁定，请关闭使用旧 DLL 的实例后重试");
        }
        message.append("。原始错误: ").append(cause);
        return new IOException(message.toString(), cause);
    }

    private static boolean extractLibrary(Path path, NativeLib library) throws IOException {
        Path sourcePath = Paths.get(BASE_PATH, library.fileName);
        Path targetPath = library.getTargetPath(path);
        String resourceName = sourcePath.toString().replace("\\", "/");
        ClassLoader classLoader = NativeLibManager.class.getClassLoader();

        try (InputStream in = classLoader.getResourceAsStream(resourceName)) {
            if (in == null) {
                if (library.required) {
                    LOGGER.error("Failed to extract required dependency {}: resource not found", sourcePath);
                } else {
                    LOGGER.warn("Failed to extract optional dependency {}: resource not found", sourcePath);
                }
                return false;
            }

            String embeddedChecksum = calculateChecksum(in);
            String existingChecksum = getExistingChecksum(targetPath);
            if (embeddedChecksum.equals(existingChecksum)) {
                library.extractedPath = targetPath;
                LOGGER.info("{} already exists with a matching checksum; skipping extraction", library.fileName);
                return true;
            }

            if (Files.exists(targetPath)) {
                LOGGER.warn(
                        "{} checksum 不一致，将尝试替换；内置 SHA-256: {}；现有 SHA-256: {}；路径: {}",
                        library.fileName,
                        embeddedChecksum,
                        existingChecksum,
                        targetPath.toAbsolutePath()
                );
            }

            try (InputStream copyInput = classLoader.getResourceAsStream(resourceName)) {
                if (_writeFile(copyInput, targetPath, embeddedChecksum, existingChecksum)) {
                    library.extractedPath = targetPath;
                    LOGGER.info("Extracted {}", library.fileName);
                    return true;
                }
            }

            if (library.required) {
                throw new IOException("Failed to extract required dependency " + library.fileName);
            } else {
                LOGGER.warn("Failed to extract optional dependency {}", library.fileName);
                return false;
            }
        } catch (IOException e) {
            if (library.required) {
                LOGGER.error("Failed to extract required dependency {}; details: {}", library.fileName, e.toString());
                throw e;
            } else {
                LOGGER.warn("Failed to extract optional dependency {}; details: {}", library.fileName, e.toString());
                return false;
            }
        }
    }

    public static class NativeLib {
        public final String baseName;
        public final String fileName;
        public final boolean loadAtStartup;
        public final boolean required;
        public final Path preExtractPath;
        public Path extractedPath;
        public boolean available;
        public boolean nameIsPath;
        public Path targetPath;

        public NativeLib(String baseName, boolean loadAtStartup, boolean required) {
            this(baseName, loadAtStartup, required, false);
        }

        public NativeLib(String baseName, boolean loadAtStartup, boolean required, boolean nameIsPath) {
            this(baseName, loadAtStartup, required, nameIsPath, null);
        }

        public NativeLib(String baseName, boolean loadAtStartup, boolean required, boolean nameIsPath, Path targetPath) {
            this.baseName = baseName;
            this.loadAtStartup = loadAtStartup;
            this.required = required;
            this.fileName = buildFullFileName(baseName, nameIsPath);
            this.preExtractPath = Paths.get(BASE_PATH, this.fileName);
            this.nameIsPath = nameIsPath;
            this.targetPath = targetPath;
        }

        private static String buildFullFileName(String baseName, boolean nameIsPath) {
            OperatingSystem operatingSystem = new OperatingSystem();
            StringBuilder sb = new StringBuilder();
            if (!nameIsPath) {
                sb.append("lib");
                sb.append(baseName);

                if (operatingSystem.type == OperatingSystemType.WINDOWS) {
                    sb.append("+win64");
                } else if (operatingSystem.type == OperatingSystemType.LINUX) {
                    sb.append("+linux64");
                } else if (operatingSystem.type == OperatingSystemType.MACOS) {
                    sb.append("+macarm64");
                } else if (operatingSystem.type == OperatingSystemType.ANDROID) {
                    sb.append("+android");
                }

                if (USE_DEBUG_LIB) {
                    sb.append("+debug");
                } else {
                    sb.append("+release");
                }
            } else {
                sb.append(baseName);
            }

            if (operatingSystem.type == OperatingSystemType.WINDOWS) {
                sb.append(".dll");
            } else if (operatingSystem.type == OperatingSystemType.LINUX || operatingSystem.type == OperatingSystemType.ANDROID) {
                sb.append(".so");
            } else if (operatingSystem.type == OperatingSystemType.MACOS) {
                sb.append(".dylib");
            }

            return sb.toString();
        }

        public Path getTargetPath(Path root) {
            if (targetPath != null) {
                this.extractedPath = targetPath.resolve(fileName);
            } else {
                this.extractedPath = root.resolve(fileName);
            }
            return this.extractedPath;
        }
    }
}
