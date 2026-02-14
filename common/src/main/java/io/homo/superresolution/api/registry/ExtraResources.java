/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.api.registry;

import io.homo.superresolution.core.utils.DirectoryEnsurer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtraResources {
    private final List<ExtraResource> resources;
    private final List<Thread> activeThreads = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean cancelled = false;

    public ExtraResources(List<ExtraResource> resources) {
        this.resources = resources;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ExtraResource> getResources() {
        return resources;
    }

    public void cancelAll() {
        cancelled = true;
        synchronized (activeThreads) {
            for (Thread thread : activeThreads) {
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            }
            activeThreads.clear();
        }
    }

    public void resetCancelState() {
        cancelled = false;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public List<ExtraResource> checkAll(DirectoryEnsurer directory) {
        List<ExtraResource> errors = new ArrayList<>();
        for (ExtraResource resource : resources) {
            if (!resource.check(directory)) {
                errors.add(resource);
            }
        }
        return errors;
    }

    public List<ExtraResource> getAll(
            ExtraResource.ResourceSource.Type preferType,
            DirectoryEnsurer directory,
            ResourcesProgressListener progressListener,
            ResourcesFinishListener finishListener,
            ResourcesErrorListener errorListener,
            boolean async
    ) {
        return getAll(resources, preferType, directory, progressListener, finishListener, errorListener, async);
    }

    public List<ExtraResource> getAll(
            List<ExtraResource> resourcesToDownload,
            ExtraResource.ResourceSource.Type preferType,
            DirectoryEnsurer directory,
            ResourcesProgressListener progressListener,
            ResourcesFinishListener finishListener,
            ResourcesErrorListener errorListener,
            boolean async
    ) {
        cancelled = false;
        List<ExtraResource> errors = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        for (ExtraResource resource : resourcesToDownload) {
            if (cancelled) {
                errorListener.onError(resource, ExtraResource.ErrorCode.Cancelled);
                errors.add(resource);
                continue;
            }
            if (resource.getSources().isEmpty()) {
                errorListener.onError(resource, ExtraResource.ErrorCode.UnknownError);
                errors.add(resource);
                continue;
            }
            ExtraResource.ResourceSource selectedSource = null;
            for (ExtraResource.ResourceSource source : resource.getSources()) {
                if (source.getType() == preferType) {
                    selectedSource = source;
                    break;
                }
            }
            if (selectedSource == null) {
                selectedSource = resource.getSources().getFirst();
            }
            ExtraResource.ProgressListener resourceProgressListener = (totalBytes, progress) -> {
                progressListener.onProgress(
                        resource,
                        totalBytes,
                        progress
                );
            };
            ExtraResource.FinishListener resourceFinishListener = (file) -> {
                finishListener.onFinish(
                        resource,
                        file
                );
            };
            ExtraResource.ErrorListener resourceErrorListener = (code) -> {
                errorListener.onError(
                        resource,
                        code
                );
            };
            if (async) {
                final ExtraResource.ResourceSource finalSelectedSource = selectedSource;
                Thread thread = new Thread(() -> {
                    if (!resource.get(
                            finalSelectedSource,
                            directory,
                            resourceProgressListener,
                            resourceFinishListener,
                            resourceErrorListener
                    )) {
                        synchronized (errors) {
                            errors.add(resource);
                        }
                    }
                });
                thread.setName("SR-ExtraResource-Getter-" + resource.getName());
                activeThreads.add(thread);
                thread.start();
                threads.add(thread);
            } else {
                if (!resource.get(
                        selectedSource,
                        directory,
                        resourceProgressListener,
                        resourceFinishListener,
                        resourceErrorListener
                )) {
                    errors.add(resource);
                }
            }

        }
        if (async) {
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            activeThreads.removeAll(threads);
        }
        return errors;
    }

    @FunctionalInterface
    public interface ResourcesProgressListener {
        void onProgress(ExtraResource resource, long totalBytes, float progress);
    }

    @FunctionalInterface
    public interface ResourcesFinishListener {
        void onFinish(ExtraResource resource, File file);
    }

    @FunctionalInterface
    public interface ResourcesErrorListener {
        void onError(ExtraResource resource, ExtraResource.ErrorCode code);
    }

    public static class Builder {
        private final List<ExtraResource> resources = new ArrayList<>();

        public Builder add(ExtraResource resource) {
            if (resource == null) {
                throw new IllegalArgumentException("resource is null");
            }
            resources.add(resource);
            return this;
        }

        public Builder addAll(List<ExtraResource> resources) {
            if (resources == null) {
                throw new IllegalArgumentException("resources is null");
            }
            this.resources.addAll(resources);
            return this;
        }

        public ExtraResources build() {
            return new ExtraResources(resources);
        }
    }

}
