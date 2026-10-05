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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.homo.superresolution.thirdparty.jcpp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public class ResourceFileSystem implements VirtualFileSystem {

    private final ClassLoader loader;
    private final Charset charset;

    public ResourceFileSystem(@Nonnull ClassLoader loader, @Nonnull Charset charset) {
        this.loader = loader;
        this.charset = charset;
    }

    @Override
    public VirtualFile getFile(String path) {
        return new ResourceFile(loader, path);
    }

    @Override
    public VirtualFile getFile(String dir, String name) {
        return getFile(dir + "/" + name);
    }

    private class ResourceFile implements VirtualFile {

        private final ClassLoader loader;
        private final String path;

        public ResourceFile(ClassLoader loader, String path) {
            this.loader = loader;
            this.path = path;
        }

        @Override
        public boolean isFile() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getName() {
            return path.substring(path.lastIndexOf('/') + 1);
        }

        @Override
        public ResourceFile getParentFile() {
            int idx = path.lastIndexOf('/');
            if (idx < 1)
                return null;
            return new ResourceFile(loader, path.substring(0, idx));
        }

        @Override
        public ResourceFile getChildFile(String name) {
            return new ResourceFile(loader, path + "/" + name);
        }

        @Override
        public Source getSource() throws IOException {
            InputStream stream = loader.getResourceAsStream(path);
            return new InputLexerSource(stream, charset);
        }
    }
}
