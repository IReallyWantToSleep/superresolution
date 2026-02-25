/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaConfig;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleLength;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleSizeLength;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class NodeToString {

    private static void appendFloatIfNotDefault(
            JsonObject json,
            String key,
            float num,
            float defaultNum) {
        if (num != defaultNum && !YogaConstants.isUndefined(num)) {
            json.addProperty(key, num);
        }
    }

    private static void appendYogaValueIfNotDefault(
            JsonObject json,
            String key,
            YogaValue value,
            YogaValue defaultValue) {
        if (!value.equals(defaultValue)) {
            if (value.unit == YogaUnit.AUTO) {
                json.addProperty(key, "auto");
            } else if (value.unit == YogaUnit.UNDEFINED) {
                json.addProperty(key, "undefined");
            } else {
                var valueObj = new JsonObject();
                valueObj.addProperty("value", value.value);
                valueObj.addProperty("unit", value.unit == YogaUnit.POINT ? "px" : "pct");
                json.add(key, valueObj);
            }
        }
    }

    private static void appendEnumValueIfNotDefault(
            JsonObject json,
            String key,
            String value,
            String defaultValue) {
        if (!value.equals(defaultValue)) {
            json.addProperty(key, value);
        }
    }

    private static void appendBoolIfNotDefault(
            JsonObject json,
            String key,
            boolean value,
            boolean defaultValue) {
        if (value != defaultValue) {
            json.addProperty(key, value);
        }
    }

    private static void appendEdges(
            JsonObject json,
            String key,
            YogaNode node,
            YogaNode defaultNode,
            EdgeValueGetter valueGetter) {

        appendYogaValueIfNotDefault(
                json.getAsJsonObject("style"),
                key + "-left",
                valueGetter.getValue(node, YogaEdge.LEFT),
                valueGetter.getValue(defaultNode, YogaEdge.LEFT));

        appendYogaValueIfNotDefault(
                json.getAsJsonObject("style"),
                key + "-right",
                valueGetter.getValue(node, YogaEdge.RIGHT),
                valueGetter.getValue(defaultNode, YogaEdge.RIGHT));

        appendYogaValueIfNotDefault(
                json.getAsJsonObject("style"),
                key + "-top",
                valueGetter.getValue(node, YogaEdge.TOP),
                valueGetter.getValue(defaultNode, YogaEdge.TOP));

        appendYogaValueIfNotDefault(
                json.getAsJsonObject("style"),
                key + "-bottom",
                valueGetter.getValue(node, YogaEdge.BOTTOM),
                valueGetter.getValue(defaultNode, YogaEdge.BOTTOM));

        appendYogaValueIfNotDefault(
                json.getAsJsonObject("style"),
                key + "-all",
                valueGetter.getValue(node, YogaEdge.ALL),
                valueGetter.getValue(defaultNode, YogaEdge.ALL));

        appendYogaValueIfNotDefault(
                json.getAsJsonObject("style"),
                key + "-start",
                valueGetter.getValue(node, YogaEdge.START),
                valueGetter.getValue(defaultNode, YogaEdge.START));

        appendYogaValueIfNotDefault(
                json.getAsJsonObject("style"),
                key + "-end",
                valueGetter.getValue(node, YogaEdge.END),
                valueGetter.getValue(defaultNode, YogaEdge.END));

        appendYogaValueIfNotDefault(
                json.getAsJsonObject("style"),
                key + "-vertical",
                valueGetter.getValue(node, YogaEdge.VERTICAL),
                valueGetter.getValue(defaultNode, YogaEdge.VERTICAL));

        appendYogaValueIfNotDefault(
                json.getAsJsonObject("style"),
                key + "-horizontal",
                valueGetter.getValue(node, YogaEdge.HORIZONTAL),
                valueGetter.getValue(defaultNode, YogaEdge.HORIZONTAL));
    }

    private static YogaValue borderFloatToYogaValue(YogaNode node, YogaEdge edge) {
        float val = node.getBorder(edge);
        var unit = YogaConstants.isUndefined(val) ? YogaUnit.UNDEFINED : YogaUnit.POINT;
        return new YogaValue(val, unit);
    }

    private static void serializeMeasureFuncResults(
            JsonObject json,
            List<SerializedMeasureFunc> measureFuncs) {

        var measureFuncsArray = new JsonArray();
        for (var measureFunc : measureFuncs) {
            var measureFuncObj = new JsonObject();
            measureFuncObj.addProperty("width", measureFunc.inputWidth());
            measureFuncObj.addProperty("width-mode", measureFunc.widthMode().toString());
            measureFuncObj.addProperty("height", measureFunc.inputHeight());
            measureFuncObj.addProperty("height-mode", measureFunc.heightMode().toString());
            measureFuncObj.addProperty("output-width", measureFunc.outputWidth());
            measureFuncObj.addProperty("output-height", measureFunc.outputHeight());
            measureFuncObj.addProperty("duration-ns", measureFunc.durationNs());

            measureFuncsArray.add(measureFuncObj);
        }

        json.add("measure-funcs", measureFuncsArray);
    }

    private static void serializeTreeImpl(
            JsonObject json,
            Map<YogaNode, List<SerializedMeasureFunc>> nodesToMeasureFuncs,
            YogaNode node,
            Set<PrintOptions> options) {

        if (options.contains(PrintOptions.LAYOUT)) {
            var layoutObj = new JsonObject();
            layoutObj.add("width", toJson(node.getStyle().getDimension(YogaDimension.WIDTH)));
            layoutObj.add("height", toJson(node.getStyle().getDimension(YogaDimension.HEIGHT)));
            layoutObj.add("top", toJson(node.getStyle().getPosition(YogaEdge.TOP)));
            layoutObj.add("left", toJson(node.getStyle().getPosition(YogaEdge.LEFT)));

            layoutObj.add("layout-width", new JsonPrimitive(node.getLayoutWidth()));
            layoutObj.add("layout-height", new JsonPrimitive(node.getLayoutWidth()));
            layoutObj.add("layout-top", new JsonPrimitive(node.getLayoutX()));
            layoutObj.add("layout-left", new JsonPrimitive(node.getLayoutY()));
            json.add("layout", layoutObj);
        }

        // Create default node for comparison
        var defaultNode = new YogaNode();

        if (options.contains(PrintOptions.STYLE)) {
            if (!json.has("style")) {
                json.add("style", new JsonObject());
            }

            appendEnumValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "flex-direction",
                    node.getFlexDirection().toString(),
                    defaultNode.getFlexDirection().toString());

            appendEnumValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "justify-content",
                    node.getJustifyContent().toString(),
                    defaultNode.getJustifyContent().toString());

            appendEnumValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "align-items",
                    node.getAlignItems().toString(),
                    defaultNode.getAlignItems().toString());

            appendEnumValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "align-content",
                    node.getAlignContent().toString(),
                    defaultNode.getAlignContent().toString());

            appendEnumValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "align-self",
                    node.getAlignSelf().toString(),
                    defaultNode.getAlignSelf().toString());

            appendEnumValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "flex-wrap",
                    node.getStyle().getFlexWrap().toString(),
                    defaultNode.getStyle().getFlexWrap().toString());

            appendEnumValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "overflow",
                    node.getOverflow().toString(),
                    defaultNode.getOverflow().toString());

            appendEnumValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "display",
                    node.getDisplay().toString(),
                    defaultNode.getDisplay().toString());

            appendEnumValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "position-type",
                    node.getPositionType().toString(),
                    defaultNode.getPositionType().toString());

            appendFloatIfNotDefault(
                    json.getAsJsonObject("style"),
                    "flex-grow",
                    node.getFlexGrow(),
                    defaultNode.getFlexGrow());

            appendFloatIfNotDefault(
                    json.getAsJsonObject("style"),
                    "flex-shrink",
                    node.getFlexShrink(),
                    defaultNode.getFlexShrink());

            appendFloatIfNotDefault(
                    json.getAsJsonObject("style"),
                    "flex",
                    node.getFlex(),
                    defaultNode.getFlex());

            appendYogaValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "flex-basis",
                    node.getFlexBasis(),
                    defaultNode.getFlexBasis());

            // Lambda for margin getter
            appendEdges(json, "margin", node, defaultNode, YogaNode::getMargin);

            // Lambda for padding getter
            appendEdges(json, "padding", node, defaultNode, YogaNode::getPadding);

            // Lambda for border getter
            appendEdges(json, "border", node, defaultNode, NodeToString::borderFloatToYogaValue);

            // Lambda for position getter
            EdgeValueGetter positionGetter = (YogaNode n, YogaEdge e) -> n.getStyle().getPosition(e).asYogaValue();
            appendEdges(json, "position", node, defaultNode, positionGetter);

            appendYogaValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "gap",
                    node.getGap(YogaGutter.ALL),
                    defaultNode.getGap(YogaGutter.ALL));

            appendYogaValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "column-gap",
                    node.getGap(YogaGutter.COLUMN),
                    defaultNode.getGap(YogaGutter.COLUMN));

            appendYogaValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "row-gap",
                    node.getGap(YogaGutter.ROW),
                    defaultNode.getGap(YogaGutter.ROW));

            appendYogaValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "width",
                    node.getStyle().getDimension(YogaDimension.WIDTH).asYogaValue(),
                    defaultNode.getStyle().getDimension(YogaDimension.WIDTH).asYogaValue());

            appendYogaValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "height",
                    node.getStyle().getDimension(YogaDimension.HEIGHT).asYogaValue(),
                    defaultNode.getStyle().getDimension(YogaDimension.HEIGHT).asYogaValue());

            appendYogaValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "max-width",
                    node.getMaxWidth(),
                    defaultNode.getMaxWidth());

            appendYogaValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "max-height",
                    node.getMaxHeight(),
                    defaultNode.getMaxHeight());

            appendYogaValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "min-width",
                    node.getMinWidth(),
                    defaultNode.getMinWidth());

            appendYogaValueIfNotDefault(
                    json.getAsJsonObject("style"),
                    "min-height",
                    node.getMinHeight(),
                    defaultNode.getMinHeight());
        }

        if (options.contains(PrintOptions.CONFIG)) {
            var configObj = new JsonObject();
            var yogaConfig = node.getConfig();
            var defaultConfig = YogaConfig.getDefault();

            appendBoolIfNotDefault(
                    configObj,
                    "use-web-defaults",
                    yogaConfig.useWebDefaults(),
                    defaultConfig.useWebDefaults());

            appendFloatIfNotDefault(
                    configObj,
                    "point-scale-factor",
                    yogaConfig.getPointScaleFactor(),
                    defaultConfig.getPointScaleFactor());

            var errata = yogaConfig.getErrata();
            var defaultErrata = defaultConfig.getErrata();

            if (errata.equals(YogaErrata.NONE) || errata.equals(YogaErrata.ALL) || errata.equals(YogaErrata.CLASSIC)) {
                appendEnumValueIfNotDefault(
                        configObj,
                        "errata",
                        errataToString(errata),
                        errataToString(defaultErrata));
            }

            if (yogaConfig.isExperimentalFeatureEnabled(YogaExperimentalFeature.WEB_FLEX_BASIS) !=
                    defaultConfig.isExperimentalFeatureEnabled(YogaExperimentalFeature.WEB_FLEX_BASIS)) {
                var featuresArray = new JsonArray();
                featuresArray.add(YogaExperimentalFeature.WEB_FLEX_BASIS.toString());
                configObj.add("experimental-features", featuresArray);
            }

            if (node.getDebugName() != null) {
                json.addProperty("name", node.getDebugName());
            }

            json.add("config", configObj);
        }

        if (options.contains(PrintOptions.NODE)) {
            var nodeObj = new JsonObject();

            appendBoolIfNotDefault(
                    nodeObj,
                    "always-forms-containing-block",
                    node.alwaysFormsContainingBlock(),
                    defaultNode.alwaysFormsContainingBlock());

            if (node.isMeasureDefined()) {
                var measureFuncs = nodesToMeasureFuncs.get(node);
                if (measureFuncs == null) {
                    nodeObj.add("measure-funcs", new JsonArray());
                } else {
                    serializeMeasureFuncResults(nodeObj, measureFuncs);
                }
            }

            json.add("node", nodeObj);
        }

        int childCount = node.getChildCount();
        if (options.contains(PrintOptions.CHILDREN) && childCount > 0) {
            var childrenArray = new JsonArray();

            for (var i = 0; i < childCount; i++) {
                var childObj = new JsonObject();
                serializeTreeImpl(
                        childObj,
                        nodesToMeasureFuncs,
                        node.getChild(i),
                        options);
                childrenArray.add(childObj);
            }

            json.add("children", childrenArray);
        }
    }

    private static String errataToString(Set<YogaErrata> errata) {
        if (errata.equals(YogaErrata.ALL)) {
            return "all";
        } else if (errata.equals(YogaErrata.CLASSIC)) {
            return "classic";
        } else if (errata.equals(YogaErrata.NONE)) {
            return "none";
        } else {
            return errata.toString();
        }
    }

    private static JsonElement toJson(StyleLength length) {
        return new JsonPrimitive(length.asYogaValue().toString());
    }

    private static JsonElement toJson(StyleSizeLength length) {
        return new JsonPrimitive(length.toString());
    }

    public static void serializeTree(
            JsonObject json,
            Map<YogaNode, List<SerializedMeasureFunc>> nodesToMeasureFuncs,
            YogaNode node,
            Set<PrintOptions> options) {

        var treeObj = new JsonObject();
        json.add("tree", treeObj);
        serializeTreeImpl(treeObj, nodesToMeasureFuncs, node, options);
    }

    public static void serializeLayoutInputs(
            JsonObject json,
            float availableWidth,
            float availableHeight,
            YogaDirection ownerDirection) {

        var layoutInputsObj = new JsonObject();
        layoutInputsObj.addProperty("available-width", availableWidth);
        layoutInputsObj.addProperty("available-height", availableHeight);
        layoutInputsObj.addProperty("owner-direction", ownerDirection.toString());

        json.add("layout-inputs", layoutInputsObj);
    }

    @FunctionalInterface
    private interface EdgeValueGetter {
        YogaValue getValue(YogaNode node, YogaEdge edge);
    }
}
