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

package io.homo.superresolution.common.upscale.algo.dlss;

import io.homo.superresolution.api.registry.ExtraResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NgxDlssLatestProvider implements ExtraResource.SrcProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(NgxDlssLatestProvider.class);
    private static final NgxDlssLatestProvider DLSS_INSTANCE = new NgxDlssLatestProvider(NgxModel.DLSS);
    private static final NgxDlssLatestProvider DLSSD_INSTANCE = new NgxDlssLatestProvider(NgxModel.DLSSD);

    private static final String BASE_URL = "https://ngx.download.nvidia.com/";
    private static final String CONFIG_URL = BASE_URL + "dev-models/org/nvidia/team/ngx/models/config/versions/2/files/nvngx_server_config.txt";
    private static final int MAX_PAGES = 100;

    private static final Pattern INI_SECTION = Pattern.compile("^\\s*\\[(.+?)]");
    private static final Pattern INI_APP_ENTRY = Pattern.compile("^\\s*app_([0-9A-Fa-f]+)\\s*=\\s*([0-9]+(?:\\.[0-9]+)*)");
    private static final Pattern XML_KEY = Pattern.compile("<Key>([^<]+)</Key>");
    private static final Pattern XML_TRUNCATED = Pattern.compile("<IsTruncated>(true|false)</IsTruncated>");

    private final NgxModel model;
    private volatile String cachedUrl;

    private NgxDlssLatestProvider(NgxModel model) {
        this.model = model;
    }

    public static NgxDlssLatestProvider getInstance() {
        return DLSS_INSTANCE;
    }

    public static NgxDlssLatestProvider getInstance(NgxModel model) {
        return switch (model) {
            case DLSS -> DLSS_INSTANCE;
            case DLSSD -> DLSSD_INSTANCE;
        };
    }

    private static int compareSemver(String a, String b) {
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int length = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < length; i++) {
            int partA = i < partsA.length ? parseIntOrZero(partsA[i]) : 0;
            int partB = i < partsB.length ? parseIntOrZero(partsB[i]) : 0;
            if (partA != partB) {
                return Integer.compare(partA, partB);
            }
        }
        return 0;
    }

    private static int parseIntOrZero(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String unescapeXml(String value) {
        return value.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }

    @Override
    public String get() throws Exception {
        String url = cachedUrl;
        if (url != null) {
            return url;
        }
        synchronized (this) {
            if (cachedUrl == null) {
                cachedUrl = resolve();
            }
            return cachedUrl;
        }
    }

    private String resolve() throws IOException {
        Map<String, String> apps = fetchAppVersions();
        if (apps.isEmpty()) {
            throw new IOException("No entries found in [" + model.configSection + "] section of NGX server config");
        }
        List<String> candidates = new ArrayList<>(apps.keySet());
        candidates.sort((a, b) -> compareSemver(apps.get(b), apps.get(a)));
        Map<String, DlssObject> objects = fetchObjects();
        for (String appId : candidates) {
            DlssObject object = objects.get(appId);
            if (object != null) {
                String url = BASE_URL + object.key;
                LOGGER.info("Resolved latest {} object from NGX: app_{} = {}, {}", model.configSection, appId, apps.get(appId), url);
                return url;
            }
        }
        throw new IOException("No " + model.configSection + " object found on NGX server for any known app id");
    }

    private Map<String, String> fetchAppVersions() throws IOException {
        String config = httpGet(CONFIG_URL);
        Map<String, String> apps = new HashMap<>();
        String section = null;
        for (String line : config.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#")) {
                continue;
            }
            Matcher sectionMatcher = INI_SECTION.matcher(line);
            if (sectionMatcher.find()) {
                section = sectionMatcher.group(1).trim();
                continue;
            }
            if (!model.configSection.equalsIgnoreCase(section)) {
                continue;
            }
            Matcher entryMatcher = INI_APP_ENTRY.matcher(line);
            if (entryMatcher.find()) {
                apps.put(entryMatcher.group(1).toUpperCase(), entryMatcher.group(2));
            }
        }
        return apps;
    }

    private Map<String, DlssObject> fetchObjects() throws IOException {
        Map<String, DlssObject> objects = new HashMap<>();
        String marker = null;
        for (int page = 0; page < MAX_PAGES; page++) {
            String url = marker == null
                    ? BASE_URL
                    : BASE_URL + "?marker=" + URLEncoder.encode(marker, StandardCharsets.UTF_8);
            String xml = httpGet(url);
            String lastKey = null;
            Matcher keyMatcher = XML_KEY.matcher(xml);
            while (keyMatcher.find()) {
                String key = unescapeXml(keyMatcher.group(1));
                lastKey = key;
                Matcher objectMatcher = model.objectKeyPattern.matcher(key);
                if (objectMatcher.find()) {
                    long version = Long.parseLong(objectMatcher.group(1));
                    String appId = objectMatcher.group(2).toUpperCase();
                    DlssObject existing = objects.get(appId);
                    if (existing == null || version > existing.version) {
                        objects.put(appId, new DlssObject(version, key));
                    }
                }
            }
            Matcher truncatedMatcher = XML_TRUNCATED.matcher(xml);
            boolean truncated = truncatedMatcher.find() && Boolean.parseBoolean(truncatedMatcher.group(1));
            if (!truncated || lastKey == null || lastKey.equals(marker)) {
                break;
            }
            marker = lastKey;
        }
        return objects;
    }

    private String httpGet(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP " + responseCode + " for " + url);
            }
            try (InputStream in = connection.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    public enum NgxModel {
        DLSS("dlss"),
        DLSSD("dlssd");

        private final String configSection;
        private final Pattern objectKeyPattern;

        NgxModel(String name) {
            this.configSection = name;
            this.objectKeyPattern = Pattern.compile(
                    "org/nvidia/team/ngx/models/" + name + "/versions/(\\d+)/files/160_([0-9A-Fa-f]+)\\.bin$");
        }
    }

    private static class DlssObject {
        final long version;
        final String key;

        DlssObject(long version, String key) {
            this.version = version;
            this.key = key;
        }
    }
}
