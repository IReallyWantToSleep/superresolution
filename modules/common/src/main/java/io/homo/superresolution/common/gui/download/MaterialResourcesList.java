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
import java.util.concurrent.ConcurrentHashMap;

public class MaterialResourcesList extends MaterialContainerWidget<MaterialResourcesList> {
    private final ExtraResources extraResources;
    private final DirectoryEnsurer targetDirectory;
    private final Map<ExtraResource, MaterialResourcesListItem> itemMap = new LinkedHashMap<>();
    private final Map<ExtraResource, Thread> activeDownloads = new ConcurrentHashMap<>();
    private final ContainerWidget listContainer;
    private final boolean enableDownload;

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
            MaterialResourcesListItem item = new MaterialResourcesListItem(resource, targetDirectory, this, enableDownload);
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
        return !activeDownloads.isEmpty();
    }

    public void startDownload(ExtraResource resource, ExtraResource.ResourceSource source) {
        MaterialResourcesListItem item = itemMap.get(resource);
        if (item == null) {
            return;
        }
        if (source == null) {
            item.markError(ExtraResource.ErrorCode.UnknownError);
            return;
        }
        if (activeDownloads.containsKey(resource) || item.getState() == MaterialResourcesListItem.DownloadState.COMPLETED) {
            return;
        }
        item.markDownloading();
        Thread thread = new Thread(() -> {
            Thread self = Thread.currentThread();
            try {
                resource.get(
                        source,
                        targetDirectory,
                        (totalBytes, progress) -> {
                            if (item.getState() != MaterialResourcesListItem.DownloadState.DOWNLOADING) {
                                return;
                            }
                            long total = Math.max(0, totalBytes);
                            long downloaded = Math.max(0, (long) progress);
                            if (total > 0 && downloaded > total) {
                                downloaded = total;
                            }
                            item.updateProgress(downloaded, total);
                        },
                        (file) -> {
                            if (item.getState() != MaterialResourcesListItem.DownloadState.DOWNLOADING) {
                                return;
                            }
                            item.markCompleted();
                        },
                        (code) -> {
                            if (item.getState() != MaterialResourcesListItem.DownloadState.DOWNLOADING) {
                                return;
                            }
                            if (code == ExtraResource.ErrorCode.Cancelled) {
                                item.markCancelled();
                            } else {
                                item.markError(code);
                            }
                        }
                );
            } finally {
                activeDownloads.remove(resource, self);
            }
        }, "SR-ExtraResource-Getter-" + resource.getName());
        thread.setDaemon(true);
        activeDownloads.put(resource, thread);
        thread.start();
    }

    public void startDownload() {
        for (Map.Entry<ExtraResource, MaterialResourcesListItem> entry : itemMap.entrySet()) {
            if (entry.getValue().getState() != MaterialResourcesListItem.DownloadState.COMPLETED) {
                startDownload(entry.getKey(), entry.getValue().getSelectedSource());
            }
        }
    }

    public void cancelDownload(ExtraResource resource) {
        Thread thread = activeDownloads.get(resource);
        MaterialResourcesListItem item = itemMap.get(resource);
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        if (item != null && item.getState() == MaterialResourcesListItem.DownloadState.DOWNLOADING) {
            item.markCancelled();
        }
    }

    public void cancelDownload() {
        for (Thread thread : activeDownloads.values()) {
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }
        for (MaterialResourcesListItem item : itemMap.values()) {
            if (item.getState() == MaterialResourcesListItem.DownloadState.DOWNLOADING) {
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
