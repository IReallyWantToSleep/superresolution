/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.graphics.renderdoc;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;

public class RenderDoc {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-RenderDoc");
    public static RenderdocLibrary.RenderdocApi renderdoc;

    private RenderDoc() {
    }

    public static void init() {
        var apiPointer = new PointerByReference();
        RenderdocLibrary.RenderdocApi apiInstance = null;
        if (OSType.isCurrentOS(OSType.WINDOWS) || OSType.isCurrentOS(OSType.LINUX)) {
            try {
                LOGGER.info(Thread.currentThread().getName());
                String projectDir = Platform.currentPlatform.getGameFolder().getParent().getParent().toAbsolutePath().toString();
                String libPath = "";
                if (OSType.isCurrentOS(OSType.WINDOWS)) {
                    libPath = Path.of(projectDir, "renderdoc", "renderdoc.dll").toAbsolutePath().toString();
                } else {
                    libPath = Path.of(projectDir, "renderdoc", "librenderdoc.so").toAbsolutePath().toString();
                }
                LOGGER.info("RenderDoc库路径 {}", libPath);
                System.load(libPath);
                RenderdocLibrary renderdocLibrary;
                renderdocLibrary = Native.load("renderdoc", RenderdocLibrary.class);
                int initResult = renderdocLibrary.RENDERDOC_GetAPI(10500, apiPointer);
                if (initResult != 1) {
                    LOGGER.error("无法初始化RenderDoc");
                } else {
                    apiInstance = new RenderdocLibrary.RenderdocApi(apiPointer.getValue());
                    var major = new IntByReference();
                    var minor = new IntByReference();
                    var patch = new IntByReference();
                    apiInstance.GetAPIVersion.call(major, minor, patch);
                    LOGGER.info("RenderDoc版本 {}.{}.{}", major.getValue(), minor.getValue(), patch.getValue());
                }
            } catch (UnsatisfiedLinkError ignored) {
                LOGGER.error("无法加载RenderDoc库");
            }
        }
        renderdoc = apiInstance;
    }

    public static Capture getCapture(int index) {
        if (renderdoc == null) return null;

        var length = new IntByReference();
        if (renderdoc.GetCapture.call(index, null, length, null).intValue() != 1) {
            return null;
        }

        var filename = new byte[length.getValue()];
        var timestamp = new LongByReference();

        renderdoc.GetCapture.call(index, filename, length, timestamp);
        return new Capture(new String(filename, 0, filename.length - 1), Instant.ofEpochSecond(timestamp.getValue()));
    }

    public static int getNumCaptures() {
        if (renderdoc == null) return -1;
        return renderdoc.GetNumCaptures.call().intValue();
    }

    public record Capture(String path, Instant timestamp) {
    }
}
