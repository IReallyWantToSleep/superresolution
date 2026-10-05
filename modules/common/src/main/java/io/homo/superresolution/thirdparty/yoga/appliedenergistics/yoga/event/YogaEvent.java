/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.event;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaMeasureMode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaNode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.config.YogaConfig;

import java.util.concurrent.atomic.AtomicReference;

public class YogaEvent {

    private static final AtomicReference<SubscriberNode> subscribers = new AtomicReference<>();

    public static void reset() {
        SubscriberNode head = pushSubscriber(null);
        while (head != null) {
            SubscriberNode current = head;
            head = head.next;
            // In Java there's no need to explicitly delete, GC will handle it
        }
    }

    public static void subscribe(Subscriber subscriber) {
        pushSubscriber(new SubscriberNode(subscriber));
    }

    public static void publish(YogaEventType eventType, YogaNode node) {
        publish(node, eventType);
    }

    public static void publish(YogaNode node, YogaEventType eventType) {
        // Create an empty event data for this type
        var emptyData = switch (eventType) {
            case NODE_ALLOCATION -> new NodeAllocationData(null);
            case NODE_DEALLOCATION -> new NodeDeallocationData(null);
            case NODE_LAYOUT -> new NodeLayoutData(null);
            case LAYOUT_PASS_END -> new LayoutPassEndData(null);
            case MEASURE_CALLBACK_END -> new MeasureCallbackEndData(0, null, 0, null, 0, 0, null);
            default -> null;
        };

        publish(node, eventType, emptyData);
    }

    public static void publish(YogaNode node, YogaEventType eventType, TypedData eventData) {
        for (SubscriberNode subscriber = subscribers.get();
             subscriber != null;
             subscriber = subscriber.next) {
            subscriber.subscriber.onEvent(node, eventType, eventData);
        }
    }

    private static SubscriberNode pushSubscriber(SubscriberNode newHead) {
        SubscriberNode oldHead;
        do {
            oldHead = subscribers.get();
            if (newHead != null) {
                newHead.next = oldHead;
            }
        } while (!subscribers.compareAndSet(oldHead, newHead));
        return oldHead;
    }

    @FunctionalInterface
    public interface Subscriber {
        void onEvent(YogaNode node, YogaEventType type, TypedData data);
    }

    public interface TypedData {
    }

    private static final class SubscriberNode {
        final Subscriber subscriber;
        SubscriberNode next;

        SubscriberNode(Subscriber subscriber) {
            this.subscriber = subscriber;
        }
    }

    public static final class NodeAllocationData implements TypedData {
        public final YogaConfig config;

        public NodeAllocationData(YogaConfig config) {
            this.config = config;
        }
    }

    public static final class NodeDeallocationData implements TypedData {
        public final YogaConfig config;

        public NodeDeallocationData(YogaConfig config) {
            this.config = config;
        }
    }

    public static final class LayoutPassEndData implements TypedData {
        public final LayoutData layoutData;

        public LayoutPassEndData(LayoutData layoutData) {
            this.layoutData = layoutData;
        }
    }

    public static final class MeasureCallbackEndData implements TypedData {
        public final float width;
        public final YogaMeasureMode widthMeasureMode;
        public final float height;
        public final YogaMeasureMode heightMeasureMode;
        public final float measuredWidth;
        public final float measuredHeight;
        public final LayoutPassReason reason;

        public MeasureCallbackEndData(
                float width,
                YogaMeasureMode widthMeasureMode,
                float height,
                YogaMeasureMode heightMeasureMode,
                float measuredWidth,
                float measuredHeight,
                LayoutPassReason reason) {
            this.width = width;
            this.widthMeasureMode = widthMeasureMode;
            this.height = height;
            this.heightMeasureMode = heightMeasureMode;
            this.measuredWidth = measuredWidth;
            this.measuredHeight = measuredHeight;
            this.reason = reason;
        }
    }

    public static final class NodeLayoutData implements TypedData {
        public final LayoutType layoutType;

        public NodeLayoutData(LayoutType layoutType) {
            this.layoutType = layoutType;
        }
    }
}
