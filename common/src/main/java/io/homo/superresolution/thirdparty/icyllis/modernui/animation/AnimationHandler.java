/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.thirdparty.icyllis.modernui.animation;

import io.homo.superresolution.thirdparty.icyllis.modernui.annotation.NonNull;
import io.homo.superresolution.thirdparty.icyllis.modernui.core.Core;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;

@ApiStatus.Internal
public class AnimationHandler {

    private static volatile AnimationHandler instance;
    private static final Object instanceLock = new Object();

    private final ArrayList<FrameCallback> animationCallbacks = new ArrayList<>();
    private final Object2LongOpenHashMap<FrameCallback> delayedStartTime = new Object2LongOpenHashMap<>();

    private boolean isListDirty = false;

    private AnimationHandler() {
    }

    @NonNull
    public static AnimationHandler getInstance() {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new AnimationHandler();
                }
            }
        }
        return instance;
    }

    public static int getAnimationCount() {
        if (instance == null) {
            return 0;
        }
        synchronized (instance.animationCallbacks) {
            return instance.getCallbackSize();
        }
    }

    public void addFrameCallback(@NonNull FrameCallback callback, long delay) {
        synchronized (animationCallbacks) {
            boolean newlyAdded;
            if (!animationCallbacks.contains(callback)) {
                animationCallbacks.add(callback);
                newlyAdded = true;
            } else {
                newlyAdded = false;
            }

            if (delay > 0) {
                delayedStartTime.put(callback, Core.timeMillis() + delay);
            } else if (!newlyAdded) {
                delayedStartTime.removeLong(callback);
            }
        }
    }

    public void removeCallback(@NonNull FrameCallback callback) {
        synchronized (animationCallbacks) {
            int id = animationCallbacks.indexOf(callback);
            if (id >= 0) {
                // 标记为已移除
                animationCallbacks.set(id, null);
                delayedStartTime.removeLong(callback);
                isListDirty = true;
            }
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void doAnimationFrame(long frameTime) {
        long currentTime = Core.timeMillis();

        final ArrayList<FrameCallback> callbacks;
        synchronized (animationCallbacks) {
            callbacks = new ArrayList<>(animationCallbacks);
        }

        for (int i = 0; i < callbacks.size(); i++) {
            final FrameCallback callback = callbacks.get(i);
            if (callback == null) {
                continue;
            }
            if (isCallbackDue(callback, currentTime)) {
                callback.doAnimationFrame(frameTime);
            }
        }

        synchronized (animationCallbacks) {
            //cleanUpList();
        }
    }


    private boolean isCallbackDue(@NonNull FrameCallback callback, long currentTime) {
        synchronized (animationCallbacks) {
            long startTime = delayedStartTime.getLong(callback);
            if (startTime == 0) {
                return true;
            }
            if (currentTime >= startTime) {
                delayedStartTime.removeLong(callback);
                return true;
            }
            return false;
        }
    }

    void autoCancelBasedOn(@NonNull ObjectAnimator animator) {
        synchronized (animationCallbacks) {
            for (int i = animationCallbacks.size() - 1; i >= 0; i--) {
                FrameCallback cb = animationCallbacks.get(i);
                if (cb == null) {
                    continue;
                }
                if (animator.shouldAutoCancel(cb)) {
                    ((Animator) cb).cancel();
                }
            }
        }
    }

    private void cleanUpList() {
        if (isListDirty) {
            for (int i = animationCallbacks.size() - 1; i >= 0; i--) {
                if (animationCallbacks.get(i) == null) {
                    animationCallbacks.remove(i);
                }
            }
            isListDirty = false;
        }
    }

    private int getCallbackSize() {
        int count = 0;
        for (int i = animationCallbacks.size() - 1; i >= 0; i--) {
            if (animationCallbacks.get(i) != null) {
                count++;
            }
        }
        return count;
    }

    public interface FrameCallback {
        boolean doAnimationFrame(long frameTime);
    }
}