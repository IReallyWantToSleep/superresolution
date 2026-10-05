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

package io.homo.superresolution.common.presentation.vulkan;

import io.homo.superresolution.api.registry.framegeneration.FrameGenerationDispatchCompletion;
import io.homo.superresolution.core.graphics.vulkan.VulkanCommandBuffer;

import java.util.function.LongConsumer;

/** Internal FG-to-presentation dependencies, independent of provider output reuse. */
final class GpuReadyFences {
    private final FrameGenerationDispatchCompletion[] completions;
    private final long[] handoffSemaphores;
    private final boolean[] consumed;
    private final LongConsumer recycler;
    private final LongConsumer disposer;

    GpuReadyFences(
            FrameGenerationDispatchCompletion[] completions,
            long[] handoffSemaphores,
            LongConsumer recycler,
            LongConsumer disposer
    ) {
        this.completions = completions.clone();
        this.handoffSemaphores = handoffSemaphores.clone();
        this.consumed = new boolean[handoffSemaphores.length];
        this.recycler = recycler;
        this.disposer = disposer;
    }

    static GpuReadyFences none() {
        return new GpuReadyFences(new FrameGenerationDispatchCompletion[0], new long[0],
                semaphore -> {
                }, semaphore -> {
        });
    }

    static FrameGenerationDispatchCompletion completion(VulkanCommandBuffer commandBuffer) {
        return new SubmissionCompletion(commandBuffer, commandBuffer.submissionGeneration());
    }

    int size() {
        return handoffSemaphores.length;
    }

    FrameGenerationDispatchCompletion completionFor(int index) {
        return completions[index];
    }

    long handoffSemaphoreFor(int index) {
        return handoffSemaphores[index];
    }

    void markConsumed(int index) {
        consumed[index] = true;
    }

    boolean isConsumed(int index) {
        return consumed[index];
    }

    boolean isComplete() {
        for (FrameGenerationDispatchCompletion completion : completions) {
            if (!completion.isComplete()) {
                return false;
            }
        }
        return true;
    }

    void awaitAll() {
        for (FrameGenerationDispatchCompletion completion : completions) {
            completion.awaitCompletion();
        }
    }

    // Called on the FG thread after every presentation submission has retired.
    void release() {
        for (int index = 0; index < handoffSemaphores.length; index++) {
            if (consumed[index]) {
                recycler.accept(handoffSemaphores[index]);
            } else {
                // A signalled binary semaphore cannot be returned to an unsignalled pool.
                disposer.accept(handoffSemaphores[index]);
            }
        }
    }

    private record SubmissionCompletion(
            VulkanCommandBuffer commandBuffer,

            long generation
    ) implements FrameGenerationDispatchCompletion {
        @Override
        public boolean isComplete() {
            return commandBuffer.isSubmissionComplete(generation);
        }

        @Override
        public void awaitCompletion() {
            commandBuffer.waitForSubmission(generation);
        }
    }
}
