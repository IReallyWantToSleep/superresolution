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

package io.homo.superresolution.common.upscale.anime4k;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import io.homo.superresolution.core.utils.FileReadHelper;

import java.util.List;

public class Anime4KModel {
    private static final Gson GSON = new GsonBuilder().create();

    @SerializedName("version")
    public int version;

    @SerializedName("workgroup_size")
    public int[] workgroupSize = {16, 16, 1};

    @SerializedName("output_texture")
    public String outputTexture;

    @SerializedName("intermediate_textures")
    public List<TextureDesc> intermediateTextures;

    @SerializedName("passes")
    public List<PassDesc> passes;

    public static Anime4KModel load(String resourcePath) {
        String json = String.join("\n", FileReadHelper.readText(resourcePath));
        return GSON.fromJson(json, Anime4KModel.class);
    }

    public static class TextureDesc {
        @SerializedName("name")
        public String name;

        @SerializedName("scale")
        public float[] scale = {1.0f, 1.0f};

        @SerializedName("components")
        public int components = 4;

        @SerializedName("format")
        public String format = "rgba32f";

        public float scaleX() {
            return scale[0];
        }

        public float scaleY() {
            return scale[1];
        }
    }

    public static class PassDesc {
        @SerializedName("file")
        public String file;

        @SerializedName("function")
        public String function;

        @SerializedName("samplers")
        public List<SamplerDesc> samplers;

        @SerializedName("image")
        public ImageDesc image;

        @SerializedName("output")
        public String output;

        @SerializedName("scale")
        public float[] scale = {1.0f, 1.0f};
    }

    public static class SamplerDesc {
        @SerializedName("binding")
        public int binding;

        @SerializedName("name")
        public String name;

        @SerializedName("source")
        public String source;

        @SerializedName("filter")
        public String filter = "NEAREST";
    }

    public static class ImageDesc {
        @SerializedName("binding")
        public int binding;

        @SerializedName("name")
        public String name;

        @SerializedName("source")
        public String source;

        @SerializedName("format")
        public String format = "rgba32f";
    }
}

