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

package io.homo.superresolution.common.gui.download;

import io.homo.superresolution.api.registry.ExtraResource;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.progress.MaterialLinearProgressIndicator;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.utils.DirectoryEnsurer;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import org.joml.Vector2f;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MaterialResourcesListItem extends MaterialContainerWidget<MaterialResourcesListItem> {
    private final ExtraResource resource;
    private final DirectoryEnsurer targetDirectory;
    private final AbstractWidget<?> iconWidget;
    private final MaterialLabel nameLabel;
    private final MaterialLabel infoLabel;
    private final MaterialLabel filePathLabel;
    private final MaterialLinearProgressIndicator progressBar;
    private final MaterialButton selectFileButton;
    private final ContainerWidget contentContainer = ContainerWidget.create();
    private final ContainerWidget textContainer = ContainerWidget.create();
    private final ContainerWidget iconContainer = ContainerWidget.create();
    private final ContainerWidget progressContainer = ContainerWidget.create();
    private final ContainerWidget rightButtonContainer = ContainerWidget.create();
    private final boolean enableDownload;
    private volatile DownloadState state = DownloadState.PENDING;
    private volatile long downloadedBytes = 0;
    private volatile long totalBytes = 0;
    private volatile ExtraResource.ErrorCode errorCode;
    private volatile String selectedPath = null;

    public MaterialResourcesListItem(
            ExtraResource resource,
            DirectoryEnsurer targetDirectory,
            boolean enableDownload
    ) {
        this.enableDownload = enableDownload;
        this.resource = resource;
        this.targetDirectory = targetDirectory;
        getLayoutNode().setDebugName("MaterialDownloadListItem");

        iconWidget = new IconWidget();
        iconWidget.setElementSize(24, 24);
        iconContainer.addChild(iconWidget);
        addChild(iconContainer);

        nameLabel = MaterialLabel.create()
                .text(resource.getName())
                .fontSize(14)
                .color(MaterialScheme::onSurface);
        nameLabel.style().sizeToContent(true);
        textContainer.addChild(nameLabel);

        if (enableDownload) {
            infoLabel = MaterialLabel.create()
                    .text(() -> getInfoText())
                    .fontSize(12)
                    .color(scheme -> getInfoColor(scheme));
            infoLabel.style().sizeToContent(true);
            textContainer.addChild(infoLabel);
            filePathLabel = null;
        } else {
            filePathLabel = MaterialLabel.create()
                    .text(() -> {
                        String noFile = Text.translatable("superresolution.screen.download.no_file_selected").getString();
                        if (selectedPath != null) {
                            String display = selectedPath.length() > 71 ? selectedPath.substring(0, 71) + "..." : selectedPath;
                            return display;
                        }
                        return noFile;
                    })
                    .fontSize(12)
                    .color(scheme -> scheme.onSurfaceVariant());
            filePathLabel.style().sizeToContent(false);
            textContainer.addChild(filePathLabel);
            infoLabel = null;
        }

        contentContainer.addChild(textContainer);

        if (enableDownload) {
            progressBar = new MaterialLinearProgressIndicator();
            progressContainer.addChild(progressBar);
            contentContainer.addChild(progressContainer);
            selectFileButton = null;
        } else {
            progressBar = null;
            selectFileButton = MaterialButton.create(MaterialButtonSize.ExtraSmall)
                    .variant(MaterialButtonVariant.Outlined)
                    .text(Text.translatable("superresolution.screen.download.button.select_file").getString())
                    .icon(MaterialSymbols.iconFileOpen());
            selectFileButton.onClick(event -> onSelectFileButtonClicked());
            rightButtonContainer.addChild(selectFileButton);
        }

        addChild(contentContainer);
        if (!enableDownload) {
            addChild(rightButtonContainer);
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    public ExtraResource getResource() {
        return resource;
    }

    public DownloadState getState() {
        return state;
    }

    public void updateProgress(long downloadedBytes, long totalBytes) {
        this.downloadedBytes = downloadedBytes;
        this.totalBytes = totalBytes;
        this.state = DownloadState.DOWNLOADING;
        if (enableDownload && progressBar != null) {
            if (totalBytes > 0) {
                progressBar.setProgress((float) downloadedBytes / totalBytes);
            } else {
                progressBar.setProgress(0f);
            }
        }
    }

    public void markCompleted() {
        this.state = DownloadState.COMPLETED;
        if (enableDownload && progressBar != null) {
            progressBar.setProgress(1f);
        }
    }

    public void markError(ExtraResource.ErrorCode code) {
        if (code == ExtraResource.ErrorCode.Cancelled) {
            markCancelled();
            return;
        }
        this.state = DownloadState.ERROR;
        this.errorCode = code;
    }

    public void markCancelled() {
        this.state = DownloadState.CANCELLED;
    }

    public void resetToPending() {
        this.state = DownloadState.PENDING;
        this.downloadedBytes = 0;
        this.totalBytes = 0;
        this.errorCode = null;
        if (enableDownload && progressBar != null) {
            progressBar.setProgress(0f);
        }
    }

    private void onSelectFileButtonClicked() {
        String selected = TinyFileDialogs.tinyfd_openFileDialog(Text.translatable("superresolution.screen.download.dialog.select_file_title").getString(), null, null, null, false);
        if (selected != null && !selected.isEmpty()) {
            copyFileToTarget(selected);
        }
    }

    private void copyFileToTarget(String sourcePath) {
        try {
            Path source = Path.of(sourcePath);
            Path target = targetDirectory.getPath().resolve(resource.getName());

            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            this.selectedPath = sourcePath;
            this.state = DownloadState.SELECTED;
        } catch (Exception e) {
            SuperResolution.LOGGER.error("选择资源文件失败", e);
            this.state = DownloadState.ERROR;
            this.errorCode = ExtraResource.ErrorCode.PermissionDenied;
        }
    }

    private String getInfoText() {
        switch (state) {
            case PENDING:
                return Text.translatable("superresolution.screen.download.state.pending").getString();
            case DOWNLOADING:
                if (totalBytes > 0) {
                    float progressPercent = (float) downloadedBytes / totalBytes * 100;
                    return String.format("%.1f", progressPercent) + "%" + " | " + formatBytes(downloadedBytes) + " / " + formatBytes(totalBytes);
                }
                if (downloadedBytes > 0) {
                    return formatBytes(downloadedBytes);
                }
                return Text.translatable("superresolution.screen.download.state.downloading").getString();
            case COMPLETED:
                return Text.translatable("superresolution.screen.download.state.completed").getString();
            case SELECTED:
                return Text.translatable("superresolution.screen.download.state.selected").getString();
            case ERROR:
                if (errorCode != null) {
                    return switch (errorCode) {
                        case NetworkError ->
                                Text.translatable("superresolution.screen.download.error.network").getString();
                        case FileNotFound ->
                                Text.translatable("superresolution.screen.download.error.file_not_found").getString();
                        case PermissionDenied ->
                                Text.translatable("superresolution.screen.download.error.permission_denied").getString();
                        case Cancelled ->
                                Text.translatable("superresolution.screen.download.state.cancelled").getString();
                        default -> Text.translatable("superresolution.screen.download.error.unknown").getString();
                    };
                }
                return Text.translatable("superresolution.screen.download.state.error").getString();
            case CANCELLED:
                return Text.translatable("superresolution.screen.download.state.cancelled").getString();
            default:
                return "";
        }
    }

    private Color getInfoColor(MaterialScheme scheme) {
        return switch (state) {
            case COMPLETED -> scheme.primary();
            case SELECTED -> scheme.primary();
            case ERROR -> scheme.error();
            case CANCELLED -> scheme.onSurfaceVariant();
            default -> scheme.onSurfaceVariant();
        };
    }

    private MaterialSymbol getCurrentIcon() {
        return switch (state) {
            case PENDING -> MaterialSymbols.iconCloudDownload();
            case DOWNLOADING -> MaterialSymbols.iconDownloading();
            case COMPLETED -> MaterialSymbols.iconDownloadDone();
            case SELECTED -> MaterialSymbols.iconFileOpen();
            case ERROR -> MaterialSymbols.iconError();
            case CANCELLED -> MaterialSymbols.iconFileDownloadOff();
        };
    }

    @Override
    protected void init() {
    }

    @Override
    public void layouting(RenderContext ctx) {
        super.layouting(ctx);

        layout().setFlexDirection(YogaFlexDirection.ROW);
        layout().setWidthPercent(100);
        layout().setAlignItems(YogaAlign.CENTER);
        layout().setPadding(YogaEdge.HORIZONTAL, 0);

        iconContainer.layout().setWidth(24);
        iconContainer.layout().setHeight(40);
        iconContainer.layout().setMargin(YogaEdge.RIGHT, 8);
        iconContainer.layout().setAlignItems(YogaAlign.CENTER);
        iconContainer.layout().setJustifyContent(YogaJustify.CENTER);
        iconContainer.layout().setFlexShrink(0);

        if (enableDownload) {
            progressContainer.layout().setWidthPercent(100);

            contentContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
            contentContainer.layout().setFlexGrow(1f);
            contentContainer.layout().setPadding(YogaEdge.LEFT, 8);
            contentContainer.layout().setGap(YogaGutter.COLUMN, 4);

            textContainer.layout().setFlexDirection(YogaFlexDirection.ROW);
            textContainer.layout().setWidthPercent(100);
            textContainer.layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
            textContainer.layout().setAlignItems(YogaAlign.CENTER);
            textContainer.layout().setMargin(YogaEdge.BOTTOM, 4);

            if (progressBar != null) {
                progressBar.layout().setWidthPercent(100);
                progressBar.layout().setHeight(4);
            }
        } else {
            contentContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
            contentContainer.layout().setFlexGrow(1f);
            contentContainer.layout().setMargin(YogaEdge.RIGHT, 8);

            textContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
            textContainer.layout().setAlignItems(YogaAlign.FLEX_START);
            textContainer.layout().setJustifyContent(YogaJustify.CENTER);

            rightButtonContainer.layout().setFlexShrink(0);
            rightButtonContainer.layout().setAlignItems(YogaAlign.CENTER);
            rightButtonContainer.layout().setJustifyContent(YogaJustify.CENTER);
        }
    }

    @Override
    protected Rectangle getViewRegion() {
        return getBounds();
    }

    @Override
    protected void renderSelf(RenderContext ctx, UIInputState inputState) {
        ((YogaNode) nameLabel.layout()).markDirtyAndPropagate();
        if (enableDownload && infoLabel != null) {
            ((YogaNode) infoLabel.layout()).markDirtyAndPropagate();
        } else if (!enableDownload && filePathLabel != null) {
            ((YogaNode) filePathLabel.layout()).markDirtyAndPropagate();
        }
    }

    public enum DownloadState {
        PENDING,
        DOWNLOADING,
        COMPLETED,
        SELECTED,
        ERROR,
        CANCELLED
    }

    private class IconWidget extends MaterialWidget<IconWidget> {
        IconWidget() {
            getLayoutNode().setDebugName("DownloadItemIcon");
        }

        @Override
        protected void init() {
        }

        @Override
        protected boolean isInteractive() {
            return false;
        }

        @Override
        public void render(RenderContext ctx, UIInputState inputState) {
            MaterialSymbol icon = getCurrentIcon();
            Color iconColor = switch (state) {
                case COMPLETED -> scheme().primary();
                case ERROR -> scheme().error();
                default -> scheme().onSurfaceVariant();
            };
            Rectangle bounds = getBounds();
            icon.render(ctx, iconColor, 24,
                    new Vector2f(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2));
        }
    }
}
