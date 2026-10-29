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

package io.homo.superresolution.api.registry.framegeneration;

import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public final class FrameGenerationDispatchResult {
    private final Status status;
    private final int actualGeneratedCount;
    private final @Nullable FrameGenerationProviderOutput providerOutput;
    private final List<VulkanTexture> generatedOutputs;
    private final @Nullable VulkanTexture realOutput;
    private final HistoryDisposition historyDisposition;
    private final @Nullable String failureReason;
    private FrameGenerationDispatchResult(
            Status status,
            int actualGeneratedCount,
            @Nullable
            FrameGenerationProviderOutput providerOutput,
            List<VulkanTexture> generatedOutputs,
            @Nullable
            VulkanTexture realOutput,
            HistoryDisposition historyDisposition,
            @Nullable
            String failureReason
    ) {
        this.status = status;
        this.actualGeneratedCount = actualGeneratedCount;
        this.providerOutput = providerOutput;
        this.generatedOutputs = generatedOutputs;
        this.realOutput = realOutput;
        this.historyDisposition = historyDisposition;
        this.failureReason = failureReason;
    }

    public static FrameGenerationDispatchResult success(
            int actualGeneratedCount,
            FrameGenerationProviderOutput output,
            HistoryDisposition historyDisposition
    ) {
        FrameGenerationProviderOutput providerOutput = Objects.requireNonNull(output, "output cannot be null");
        if (actualGeneratedCount < 0) {
            throw new IllegalArgumentException("actualGeneratedCount cannot be negative");
        }
        List<VulkanTexture> outputs = List.copyOf(providerOutput.generatedOutputs());
        if (outputs.size() != actualGeneratedCount) {
            throw new IllegalArgumentException(
                    "actualGeneratedCount must match the leased generated output count"
            );
        }
        Objects.requireNonNull(providerOutput.completion(), "outputLease completion cannot be null");
        Objects.requireNonNull(providerOutput.outputKey(), "outputLease outputKey cannot be null");
        return new FrameGenerationDispatchResult(
                Status.SUCCESS,
                actualGeneratedCount,
                providerOutput,
                outputs,
                providerOutput.realOutput(),
                Objects.requireNonNull(historyDisposition, "historyDisposition cannot be null"),
                null
        );
    }

    public static FrameGenerationDispatchResult failed(String failureReason) {
        String reason = Objects.requireNonNull(failureReason, "failureReason cannot be null");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("failureReason cannot be blank");
        }
        return new FrameGenerationDispatchResult(
                Status.FAILED,
                0,
                null,
                List.of(),
                null,
                HistoryDisposition.UNCHANGED,
                reason
        );
    }

    public Status status() {
        return status;
    }

    public boolean succeeded() {
        return status == Status.SUCCESS;
    }

    public int actualGeneratedCount() {
        return actualGeneratedCount;
    }

    public @Nullable FrameGenerationProviderOutput output() {
        return providerOutput;
    }

    public List<VulkanTexture> generatedOutputs() {
        return generatedOutputs;
    }

    public @Nullable VulkanTexture realOutput() {
        return realOutput;
    }

    public FrameGenerationDispatchCompletion completion() {
        return providerOutput == null
                ? FrameGenerationDispatchCompletion.completed()
                : providerOutput.completion();
    }

    public HistoryDisposition historyDisposition() {
        return historyDisposition;
    }

    public @Nullable String failureReason() {
        return failureReason;
    }

    public enum Status {
        SUCCESS,
        FAILED
    }

    public enum HistoryDisposition {
        UNCHANGED,
        SEEDED,
        RESET
    }
}
