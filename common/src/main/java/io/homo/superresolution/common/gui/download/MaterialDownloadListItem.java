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
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.progress.MaterialLinearProgressIndicator;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import org.joml.Vector2f;

public class MaterialDownloadListItem extends MaterialContainerWidget<MaterialDownloadListItem> {

    private final ExtraResource resource;
    private final IconWidget iconWidget;
    private final MaterialLabel nameLabel;
    private final MaterialLabel infoLabel;
    private final MaterialLinearProgressIndicator progressBar;
    private final ContainerWidget contentContainer = ContainerWidget.create();
    private final ContainerWidget textContainer = ContainerWidget.create();
    private final ContainerWidget iconContainer = ContainerWidget.create();
    private final ContainerWidget progressContainer = ContainerWidget.create();
    private volatile DownloadState state = DownloadState.PENDING;
    private volatile long downloadedBytes = 0;
    private volatile long totalBytes = 0;
    private volatile ExtraResource.ErrorCode errorCode;

    public MaterialDownloadListItem(ExtraResource resource) {
        this.resource = resource;
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

        infoLabel = MaterialLabel.create()
                .text(() -> getInfoText())
                .fontSize(12)
                .color(scheme -> getInfoColor(scheme));
        infoLabel.style().sizeToContent(true);
        textContainer.addChild(infoLabel);

        contentContainer.addChild(textContainer);

        progressBar = new MaterialLinearProgressIndicator();
        progressContainer.addChild(progressBar);
        contentContainer.addChild(progressContainer);
        addChild(contentContainer);
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
        if (totalBytes > 0) {
            progressBar.setProgress((float) downloadedBytes / totalBytes);
        } else {
            progressBar.setProgress(0f);
        }
    }

    public void markCompleted() {
        this.state = DownloadState.COMPLETED;
        progressBar.setProgress(1f);
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
        progressBar.setProgress(0f);
    }

    private String getInfoText() {
        switch (state) {
            case PENDING:
                return "等待中...";
            case DOWNLOADING:
                if (totalBytes > 0) {
                    float progressPercent = (float) downloadedBytes / totalBytes * 100;
                    return String.format("%.1f", progressPercent) + "%" + " | " + formatBytes(downloadedBytes) + " / " + formatBytes(totalBytes);
                }
                if (downloadedBytes > 0) {
                    return formatBytes(downloadedBytes);
                }
                return "下载中...";
            case COMPLETED:
                return "已完成";
            case ERROR:
                if (errorCode != null) {
                    return switch (errorCode) {
                        case NetworkError -> "网络错误";
                        case FileNotFound -> "文件未找到";
                        case PermissionDenied -> "权限不足";
                        case Cancelled -> "已取消";
                        default -> "未知错误";
                    };
                }
                return "错误";
            case CANCELLED:
                return "已取消";
            default:
                return "";
        }
    }

    private Color getInfoColor(MaterialScheme scheme) {
        return switch (state) {
            case COMPLETED -> scheme.primary();
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
        progressBar.layout().setWidthPercent(100);
        progressBar.layout().setHeight(4);
    }

    @Override
    protected Rectangle getViewRegion() {
        return getBounds();
    }

    @Override
    protected void renderSelf(RenderContext ctx, UIInputState inputState) {
        ((YogaNode) nameLabel.layout()).markDirtyAndPropagate();
        ((YogaNode) infoLabel.layout()).markDirtyAndPropagate();
    }

    public enum DownloadState {
        PENDING,
        DOWNLOADING,
        COMPLETED,
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
