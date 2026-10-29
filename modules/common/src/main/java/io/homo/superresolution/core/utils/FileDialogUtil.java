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

package io.homo.superresolution.core.utils;

#if MC_VER >= MC_26_3
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDL_DialogFileCallbackI;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;
#else
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
#endif

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

#if MC_VER >= MC_26_3
import static org.lwjgl.sdl.SDLDialog.*;
import static org.lwjgl.sdl.SDLProperties.*;
#endif

public final class FileDialogUtil {
    private FileDialogUtil() {
    }

    /**
     * Opens a file selection dialog.
     *
     * <p>SDL 3 uses an asynchronous callback, while tinyfiledialogs blocks until
     * the native dialog closes. Both implementations expose the same future-based
     * API to the callers.</p>
     *
     * @param dialog      whether to open an open or save dialog
     * @param title       the title of the dialog window
     * @param origin      the path that the window should start at
     * @param filterLabel a label describing the allowed file extensions
     * @param filters     file extension filters, each formatted as {@code "*.extension"}
     * @return a future completed with the selected path, or empty when cancelled
     */
    public static CompletableFuture<Optional<Path>> fileSelectDialog(
            DialogType dialog,
            String title,
            Path origin,
            String filterLabel,
            String... filters
    ) {
#if MC_VER >= MC_26_3
        CompletableFuture<Optional<Path>> future = new CompletableFuture<>();
        String[] effectiveFilters = filters == null ? new String[0] : filters;

        /*
         * SDL requires the filter buffer and its strings to remain valid until
         * the callback runs, so these allocations cannot use MemoryStack.
         */
        SDL_DialogFileFilter.Buffer filterBuffer =
                effectiveFilters.length == 0
                        ? null
                        : SDL_DialogFileFilter.calloc(effectiveFilters.length);

        if (filterBuffer != null) {
            for (int i = 0; i < effectiveFilters.length; i++) {
                String filter = effectiveFilters[i];
                filterBuffer.get(i)
                        .name(MemoryUtil.memUTF8(filterLabel == null ? filter : filterLabel))
                        .pattern(MemoryUtil.memUTF8(toSDLFilter(filter)));
            }
        }

        int properties = SDL_CreateProperties();
        if (properties == 0) {
            freeFilters(filterBuffer);
            future.completeExceptionally(
                    new IllegalStateException("Failed to create SDL file dialog properties")
            );
            return future;
        }

        SDL_SetStringProperty(
                properties,
                SDL_PROP_FILE_DIALOG_TITLE_STRING,
                title
        );

        if (origin != null) {
            SDL_SetStringProperty(
                    properties,
                    SDL_PROP_FILE_DIALOG_LOCATION_STRING,
                    origin.toAbsolutePath().toString()
            );
        }

        if (filterBuffer != null) {
            SDL_SetPointerProperty(
                    properties,
                    SDL_PROP_FILE_DIALOG_FILTERS_POINTER,
                    filterBuffer.address()
            );
            SDL_SetNumberProperty(
                    properties,
                    SDL_PROP_FILE_DIALOG_NFILTERS_NUMBER,
                    filterBuffer.capacity()
            );
        }

        SDL_DialogFileCallbackI callback = (userdata, fileList, selectedFilter) -> {
            try {
                if (fileList == MemoryUtil.NULL) {
                    future.completeExceptionally(
                            new IllegalStateException("SDL file dialog failed")
                    );
                    return;
                }

                PointerBuffer files = MemoryUtil.memPointerBuffer(fileList, 1);
                long firstFile = files.get(0);
                if (firstFile == MemoryUtil.NULL) {
                    future.complete(Optional.empty());
                } else {
                    future.complete(Optional.of(Path.of(MemoryUtil.memUTF8(firstFile))));
                }
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            } finally {
                SDL_DestroyProperties(properties);
                freeFilters(filterBuffer);
            }
        };

        int type = switch (dialog) {
            case OPEN -> SDL_FILEDIALOG_OPENFILE;
            case SAVE -> SDL_FILEDIALOG_SAVEFILE;
        };

        SDL_ShowFileDialogWithProperties(
                type,
                callback,
                MemoryUtil.NULL,
                properties
        );
        return future;
#else
        String[] effectiveFilters = filters == null ? new String[0] : filters;
        String selected;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filterBuffer = null;
            if (effectiveFilters.length > 0) {
                filterBuffer = stack.mallocPointer(effectiveFilters.length);
                for (String filter : effectiveFilters) {
                    filterBuffer.put(stack.UTF8(filter));
                }
                filterBuffer.flip();
            }

            String defaultPath = origin == null ? null : origin.toString();
            selected = switch (dialog) {
                case OPEN -> TinyFileDialogs.tinyfd_openFileDialog(
                        title,
                        defaultPath,
                        filterBuffer,
                        filterLabel,
                        false
                );
                case SAVE -> TinyFileDialogs.tinyfd_saveFileDialog(
                        title,
                        defaultPath,
                        filterBuffer,
                        filterLabel
                );
            };
        }

        if (selected == null || selected.isBlank()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        try {
            return CompletableFuture.completedFuture(Optional.of(Path.of(selected)));
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
#endif
    }

#if MC_VER >= MC_26_3
    private static String toSDLFilter(String filter) {
        /*
         * tinyfiledialogs uses patterns such as "*.zip"; SDL expects "zip".
         */
        if (filter.equals("*") || filter.equals("*.*")) {
            return "*";
        }
        if (filter.startsWith("*.")) {
            return filter.substring(2);
        }
        return filter;
    }

    private static void freeFilters(SDL_DialogFileFilter.Buffer filters) {
        if (filters == null) {
            return;
        }

        for (int i = 0; i < filters.capacity(); i++) {
            SDL_DialogFileFilter filter = filters.get(i);
            MemoryUtil.memFree(filter.name());
            MemoryUtil.memFree(filter.pattern());
        }
        filters.free();
    }
#endif

    public enum DialogType {
        SAVE,
        OPEN
    }
}
