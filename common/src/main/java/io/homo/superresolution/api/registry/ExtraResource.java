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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

public class ExtraResource {
    protected final String name;
    protected final List<ResourceSource> sources;

    public ExtraResource(String name, List<ResourceSource> sources) {
        this.name = name;
        this.sources = Collections.unmodifiableList(sources);
    }

    public String getName() {
        return name;
    }

    public List<ResourceSource> getSources() {
        return sources;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    private boolean isCancelled(ErrorListener errorListener) {
        if (Thread.currentThread().isInterrupted()) {
            errorListener.onError(ErrorCode.Cancelled);
            return true;
        }
        return false;
    }

    private void deletePartialFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException ignored) {
            }
        }
    }

    public boolean check(DirectoryEnsurer targetDirectory) {
        Path targetPath = targetDirectory.getPath().resolve(name);
        File targetFile = targetPath.toFile();
        return targetFile.exists() && targetFile.canWrite() && targetFile.canRead();
    }

    public boolean get(
            ResourceSource.Type type,
            DirectoryEnsurer targetDirectory,
            ProgressListener progressListener,
            FinishListener finishListener,
            ErrorListener errorListener
    ) {
        ResourceSource source = sources.stream().filter((src) -> src.type == type).findFirst().orElse(null);
        if (source == null) {
            throw new IllegalArgumentException("No such source type: " + type);
        }
        return get(source, targetDirectory, progressListener, finishListener, errorListener);
    }

    public boolean get(
            ResourceSource source,
            DirectoryEnsurer targetDirectory,
            ProgressListener progressListener,
            FinishListener finishListener,
            ErrorListener errorListener
    ) {
        if (source.type == ResourceSource.Type.Local) {
            return getLocal(
                    source,
                    targetDirectory,
                    progressListener,
                    finishListener,
                    errorListener
            );
        } else if (source.type == ResourceSource.Type.Remote) {
            return getRemote(
                    source,
                    targetDirectory,
                    progressListener,
                    finishListener,
                    errorListener
            );
        } else {
            errorListener.onError(ErrorCode.UnknownError);
            throw new IllegalArgumentException();
        }
    }

    protected boolean getRemote(
            ResourceSource source,
            DirectoryEnsurer targetDirectory,
            ProgressListener progressListener,
            FinishListener finishListener,
            ErrorListener errorListener
    ) {
        if (source.type != ResourceSource.Type.Remote) {
            return false;
        }
        if (isCancelled(errorListener)) {
            return false;
        }
        Path targetPath = targetDirectory.getPath().resolve(name);
        File targetFile = targetPath.toFile();
        File parentDir = targetFile.getParentFile();
        if (targetFile.exists() && !targetFile.canWrite()) {
            errorListener.onError(ErrorCode.PermissionDenied);
            return false;
        }
        if (parentDir != null) {
            try {
                Files.createDirectories(parentDir.toPath());
            } catch (IOException e) {
                errorListener.onError(ErrorCode.PermissionDenied);
                return false;
            }
            if (!parentDir.canWrite()) {
                errorListener.onError(ErrorCode.PermissionDenied);
                return false;
            }
        }
        HttpURLConnection connection = null;
        try {
            URI uri = new URI(source.src);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(5 * 60 * 1000); //5min
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    errorListener.onError(ErrorCode.FileNotFound);
                } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    errorListener.onError(ErrorCode.NetworkError);
                } else {
                    errorListener.onError(ErrorCode.NetworkError);
                }
                return false;
            }
            long size = Math.max(0, connection.getContentLengthLong());
            progressListener.onProgress(size, 0f);

            if (isCancelled(errorListener)) {
                deletePartialFile(targetFile);
                return false;
            }

            if (!targetFile.exists()) {
                Files.createFile(targetPath);
            }

            try (
                    InputStream inputStream = connection.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(targetFile)
            ) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long downloaded = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (isCancelled(errorListener)) {
                        deletePartialFile(targetFile);
                        return false;
                    }
                    outputStream.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    progressListener.onProgress(size, downloaded);
                }
            } catch (FileNotFoundException e) {
                errorListener.onError(ErrorCode.PermissionDenied);
                deletePartialFile(targetFile);
                return false;
            } catch (IOException e) {
                if (Thread.currentThread().isInterrupted()) {
                    errorListener.onError(ErrorCode.Cancelled);
                } else {
                    errorListener.onError(ErrorCode.NetworkError);
                }
                deletePartialFile(targetFile);
                e.printStackTrace();
                return false;
            }
            finishListener.onFinish(targetFile);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (Thread.currentThread().isInterrupted()) {
                errorListener.onError(ErrorCode.Cancelled);
            } else {
                errorListener.onError(ErrorCode.UnknownError);
            }
            deletePartialFile(targetFile);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected boolean getLocal(
            ResourceSource source,
            DirectoryEnsurer targetDirectory,
            ProgressListener progressListener,
            FinishListener finishListener,
            ErrorListener errorListener
    ) {
        if (source.type != ResourceSource.Type.Local) {
            errorListener.onError(ErrorCode.UnknownError);
            return false;
        }
        if (isCancelled(errorListener)) {
            return false;
        }
        try (
                InputStream in = ExtraResource.class.getClassLoader()
                        .getResourceAsStream(source.src.replace("\\", "/"))
        ) {
            if (in != null) {
                long totalBytes = Math.max(0, in.available());
                progressListener.onProgress(totalBytes, 0f);
                Path targetPath = targetDirectory.getPath().resolve(name);
                File targetFile = targetPath.toFile();
                File parentDir = targetFile.getParentFile();
                if (parentDir != null) {
                    Files.createDirectories(parentDir.toPath());
                    if (!parentDir.canWrite()) {
                        errorListener.onError(ErrorCode.PermissionDenied);
                        return false;
                    }
                }
                Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                progressListener.onProgress(totalBytes, totalBytes);
                finishListener.onFinish(targetFile);
                return true;
            } else {
                errorListener.onError(ErrorCode.FileNotFound);
                throw new FileNotFoundException(source.src.replace("\\", "/"));
            }
        } catch (FileNotFoundException ex) {
            errorListener.onError(ErrorCode.PermissionDenied);
            ex.printStackTrace();
            return false;
        } catch (Exception ex) {
            if (Thread.currentThread().isInterrupted()) {
                errorListener.onError(ErrorCode.Cancelled);
            } else {
                errorListener.onError(ErrorCode.UnknownError);
            }
            ex.printStackTrace();
            return false;
        }
    }

    public enum ErrorCode {
        NetworkError,
        FileNotFound,
        PermissionDenied,
        Cancelled,
        UnknownError
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(long totalBytes, float progress);
    }

    @FunctionalInterface
    public interface FinishListener {
        void onFinish(File file);
    }

    @FunctionalInterface
    public interface ErrorListener {
        void onError(ErrorCode code);
    }

    public static class ResourceSource {
        protected final String src;
        protected final Type type;
        protected final String sourceName;

        public ResourceSource(String src, Type type, String sourceName) {
            this.src = src;
            this.type = type;
            this.sourceName = sourceName;
        }

        public String getSrc() {
            return src;
        }

        public Type getType() {
            return type;
        }

        public String getSourceName() {
            return sourceName;
        }

        public enum Type {
            Local,
            Remote
        }
    }

    public static class Builder {
        private final String name;
        private final List<ResourceSource> sources = new java.util.ArrayList<>();

        public Builder(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name is blank");
            }
            this.name = name;
        }

        public Builder addSource(ResourceSource source) {
            if (source == null) {
                throw new IllegalArgumentException("source is null");
            }
            sources.add(source);
            return this;
        }

        public Builder addLocal(String src, String sourceName) {
            return addSource(new ResourceSource(src, ResourceSource.Type.Local, sourceName));
        }

        public Builder addRemote(String src, String sourceName) {
            return addSource(new ResourceSource(src, ResourceSource.Type.Remote, sourceName));
        }

        public ExtraResource build() {
            return new ExtraResource(name, sources);
        }
    }
}
