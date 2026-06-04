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
import io.homo.superresolution.api.registry.ExtraResources;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.utils.DirectoryEnsurer;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;

import java.util.*;

public class MaterialResourcesList extends MaterialContainerWidget<MaterialResourcesList> {
    private final ExtraResources extraResources;
    private final DirectoryEnsurer targetDirectory;
    private final Map<ExtraResource, MaterialResourcesListItem> itemMap = new LinkedHashMap<>();
    private final ContainerWidget listContainer;
    private final boolean enableDownload;
    private volatile Thread downloadManagerThread;
    private volatile boolean downloading = false;

    private MaterialResourcesList(
            ExtraResources extraResources,
            DirectoryEnsurer targetDirectory,
            boolean enableDownload
    ) {
        this.extraResources = extraResources;
        this.targetDirectory = targetDirectory;
        this.enableDownload = enableDownload;
        getLayoutNode().setDebugName("MaterialDownloadList");

        listContainer = ContainerWidget.create();

        for (ExtraResource resource : extraResources.getResources()) {
            MaterialResourcesListItem item = new MaterialResourcesListItem(resource, targetDirectory, enableDownload);
            item.layout().setWidthPercent(100);
            itemMap.put(resource, item);
            listContainer.addChild(item);
        }

        addChild(listContainer);
    }

    public static MaterialResourcesList createDownload(ExtraResources extraResources, DirectoryEnsurer targetDirectory) {
        return new MaterialResourcesList(extraResources, targetDirectory, true);
    }

    public static MaterialResourcesList createFileChoose(ExtraResources extraResources, DirectoryEnsurer targetDirectory) {
        return new MaterialResourcesList(extraResources, targetDirectory, false);

    }

    public ExtraResources getExtraResources() {
        return extraResources;
    }

    public MaterialResourcesListItem getItem(ExtraResource resource) {
        return itemMap.get(resource);
    }

    public Collection<MaterialResourcesListItem> getItems() {
        return Collections.unmodifiableCollection(itemMap.values());
    }

    public boolean isDownloading() {
        return downloading;
    }

    public void startDownload() {
        if (downloading) {
            return;
        }
        downloading = true;
        extraResources.resetCancelState();

        for (MaterialResourcesListItem item : itemMap.values()) {
            if (item.getState() != MaterialResourcesListItem.DownloadState.COMPLETED) {
                item.resetToPending();
            }
        }

        downloadManagerThread = new Thread(() -> {
            List<ExtraResource> toDownload = new ArrayList<>();
            for (Map.Entry<ExtraResource, MaterialResourcesListItem> entry : itemMap.entrySet()) {
                if (entry.getValue().getState() != MaterialResourcesListItem.DownloadState.COMPLETED) {
                    toDownload.add(entry.getKey());
                }
            }

            if (toDownload.isEmpty()) {
                downloading = false;
                return;
            }

            extraResources.getAll(
                    toDownload,
                    ExtraResource.ResourceSource.Type.Remote,
                    targetDirectory,
                    (resource, totalBytesOrDownloaded, progressOrSize) -> {
                        MaterialResourcesListItem item = itemMap.get(resource);
                        if (item != null) {
                            long total = Math.max(0, totalBytesOrDownloaded);
                            long downloaded = Math.max(0, (long) progressOrSize);
                            if (total > 0 && downloaded > total) {
                                downloaded = total;
                            }
                            item.updateProgress(downloaded, total);
                        }
                    },
                    (resource, file) -> {
                        MaterialResourcesListItem item = itemMap.get(resource);
                        if (item != null) {
                            item.markCompleted();
                        }
                    },
                    (resource, code) -> {
                        MaterialResourcesListItem item = itemMap.get(resource);
                        if (item != null) {
                            if (code == ExtraResource.ErrorCode.Cancelled) {
                                item.markCancelled();
                            } else {
                                item.markError(code);
                            }
                        }
                    },
                    true
            );

            downloading = false;
        }, "SR-DownloadList-Manager");
        downloadManagerThread.setDaemon(true);
        downloadManagerThread.start();
    }

    public void cancelDownload() {
        extraResources.cancelAll();
        if (downloadManagerThread != null && downloadManagerThread.isAlive()) {
            downloadManagerThread.interrupt();
        }
        downloading = false;

        for (MaterialResourcesListItem item : itemMap.values()) {
            if (item.getState() == MaterialResourcesListItem.DownloadState.DOWNLOADING ||
                    item.getState() == MaterialResourcesListItem.DownloadState.PENDING) {
                item.markCancelled();
            }
        }
    }

    public void retryDownload() {
        cancelDownload();

        for (MaterialResourcesListItem item : itemMap.values()) {
            if (item.getState() != MaterialResourcesListItem.DownloadState.COMPLETED) {
                item.resetToPending();
            }
        }

        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            startDownload();
        }, "SR-DownloadList-Retry").start();
    }

    @Override
    protected void init() {
    }

    @Override
    public void layouting(RenderContext ctx) {
        super.layouting(ctx);
        layout().setFlexDirection(YogaFlexDirection.COLUMN);
        layout().setWidthPercent(100);
        listContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        listContainer.layout().setWidthPercent(100);
        listContainer.layout().setGap(YogaGutter.COLUMN, 2);
    }

    @Override
    protected Rectangle getViewRegion() {
        return getBounds();
    }

    @Override
    protected void renderSelf(RenderContext ctx, UIInputState inputState) {
    }
}
