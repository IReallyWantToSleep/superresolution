package io.homo.superresolution.common.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.homo.superresolution.core.utils.Color;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SponsorService {
    private static final String ENDPOINT = "https://api.187j3x1-114514.org/sr/sponsors/list";
    private static final String READ_TOKEN = "b78e84af90423d46e4d547eba871c2e9e89140b4ada692ad63dc6085193e53d1";

    private SponsorService() {
    }

    public static CompletableFuture<Result> fetchAsync() {
        return CompletableFuture.supplyAsync(SponsorService::fetch);
    }

    private static Result fetch() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(ENDPOINT).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + READ_TOKEN);
            connection.setConnectTimeout(30_000);
            connection.setReadTimeout(30_000);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                return Result.failure();
            }
            try (InputStream stream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonElement data = root.get("data");
                if (data == null || !data.isJsonObject()) {
                    return Result.failure();
                }
                JsonElement sponsors = data.getAsJsonObject().get("sponsors");
                if (sponsors == null || !sponsors.isJsonArray()) {
                    return Result.failure();
                }
                List<Sponsor> result = new ArrayList<>();
                for (JsonElement element : sponsors.getAsJsonArray()) {
                    Sponsor sponsor = parseSponsor(element);
                    if (sponsor != null) {
                        result.add(sponsor);
                    }
                }
                return Result.success(result);
            }
        } catch (Exception ignored) {
            return Result.failure();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static Sponsor parseSponsor(JsonElement element) {
        try {
            if (!element.isJsonObject()) {
                return null;
            }
            JsonObject object = element.getAsJsonObject();
            String name = string(object, "name");
            Color[] nameColors = colors(object, "nameColor", "name_color", "labelColor", "label_color");
            Color[] backgroundColors = colors(object, "backgroundColor", "background_color");
            if (name == null || name.isBlank() || nameColors == null || backgroundColors == null) {
                return null;
            }
            return new Sponsor(name, nameColors[0], nameColors[1], backgroundColors[0], backgroundColors[1]);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : null;
    }

    private static Color[] colors(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement value = object.get(key);
            Color[] colors = parseColors(value);
            if (colors != null) {
                return colors;
            }
        }
        return null;
    }

    private static Color[] parseColors(JsonElement value) {
        if (value == null) {
            return null;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            Color color = parseColor(value.getAsString());
            return color == null ? null : new Color[]{color, color};
        }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            Color first = parseColor(string(object, "startColor"));
            Color second = parseColor(string(object, "endColor"));
            return first == null || second == null ? null : new Color[]{first, second};
        }
        if (!value.isJsonArray()) {
            return null;
        }
        JsonArray array = value.getAsJsonArray();
        if (array.size() == 0 || array.size() > 2) {
            return null;
        }
        Color first = parseColor(array.get(0).getAsString());
        Color second = array.size() == 2 ? parseColor(array.get(1).getAsString()) : first;
        return first == null || second == null ? null : new Color[]{first, second};
    }

    private static Color parseColor(String value) {
        if (value == null || !value.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
            return null;
        }
        try {
            return Color.hex(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public record Sponsor(String name, Color nameStart, Color nameEnd, Color backgroundStart, Color backgroundEnd) {
    }

    public record Result(boolean success, List<Sponsor> sponsors) {
        static Result success(List<Sponsor> sponsors) {
            return new Result(true, Collections.unmodifiableList(new ArrayList<>(sponsors)));
        }

        static Result failure() {
            return new Result(false, List.of());
        }
    }
}
