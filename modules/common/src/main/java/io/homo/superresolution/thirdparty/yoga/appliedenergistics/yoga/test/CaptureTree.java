/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaMeasureMode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaSize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public class CaptureTree {

    private static final ThreadLocal<Map<YogaNode, List<SerializedMeasureFunc>>> currentSerializedMeasureFuncMap =
            ThreadLocal.withInitial(HashMap::new);

    private static void captureTree(String serializedTree, Path path) {
        try {
            Files.writeString(path, serializedTree);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to file: " + path, e);
        }
    }

    /**
     * Capturing a tree often means that we capturing multiple serial layouts over
     * the course of the capture. Because of this, we need to make sure that we do
     * a full layout pass with no caching. If we do not do this there is a chance
     * we do not capture measure functions that were called and cached in previous
     * layouts. Missing these captures would lead to inaccurate benchmarking where
     * we do not have cached state.
     */
    private static void dirtyTree(YogaNode node) {
        if (node.isMeasureDefined()) {
            node.markDirtyAndPropagate();
        }

        for (var i = 0; i < node.getChildCount(); i++) {
            dirtyTree(node.getChild(i));
        }
    }

    public static void calculateLayoutWithCapture(
            YogaNode node,
            float availableWidth,
            float availableHeight,
            YogaDirection ownerDirection,
            Path path) {

        dirtyTree(node);
        node.calculateLayout(availableWidth, availableHeight, ownerDirection);

        var gson = new GsonBuilder().setPrettyPrinting().create();
        var jsonObject = new JsonObject();

        NodeToString.serializeLayoutInputs(jsonObject, availableWidth, availableHeight, ownerDirection);
        NodeToString.serializeTree(
                jsonObject,
                currentSerializedMeasureFuncMap.get(),
                node,
                EnumSet.of(PrintOptions.STYLE, PrintOptions.CHILDREN, PrintOptions.CONFIG, PrintOptions.NODE, PrintOptions.LAYOUT)
        );

        // Clear the measure function map after serialization
        currentSerializedMeasureFuncMap.get().clear();
        captureTree(gson.toJson(jsonObject), path);
    }

    public static void captureMeasureFunc(
            YogaNode node,
            float width,
            YogaMeasureMode widthMode,
            float height,
            YogaMeasureMode heightMode,
            YogaSize output,
            Duration duration) {

        var map = currentSerializedMeasureFuncMap.get();
        var measureFuncs = map.computeIfAbsent(node, k -> new ArrayList<>());

        measureFuncs.add(new SerializedMeasureFunc(
                width,
                widthMode,
                height,
                heightMode,
                output.width(),
                output.height(),
                duration.toNanos()
        ));
    }
}
