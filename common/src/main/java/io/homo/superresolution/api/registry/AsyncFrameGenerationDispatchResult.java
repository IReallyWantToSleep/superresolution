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

package io.homo.superresolution.api.registry;

import io.homo.superresolution.core.graphics.vulkan.VulkanTexture;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Atomic result of one application-managed dispatch.
 * <p>
 * A successful result owns one complete {@link ProviderOutputLease}. A failed
 * result never exposes partial generated outputs; the scheduler must publish a
 * real-only batch through its normal ordering path.
 */
public final class AsyncFrameGenerationDispatchResult {
    public enum Status {
        SUCCESS,
        FAILED
    }

    public enum HistoryDisposition {
        UNCHANGED,
        SEEDED,
        RESET
    }

    private final Status status;
    private final int actualGeneratedCount;
    private final @Nullable ProviderOutputLease outputLease;
    private final List<VulkanTexture> generatedOutputs;
    private final @Nullable VulkanTexture realOutput;
    private final HistoryDisposition historyDisposition;
    private final @Nullable String failureReason;

    private AsyncFrameGenerationDispatchResult(
            Status status,
            int actualGeneratedCount,
            @Nullable ProviderOutputLease outputLease,
            List<VulkanTexture> generatedOutputs,
            @Nullable VulkanTexture realOutput,
            HistoryDisposition historyDisposition,
            @Nullable String failureReason
    ) {
        this.status = status;
        this.actualGeneratedCount = actualGeneratedCount;
        this.outputLease = outputLease;
        this.generatedOutputs = generatedOutputs;
        this.realOutput = realOutput;
        this.historyDisposition = historyDisposition;
        this.failureReason = failureReason;
    }

    public static AsyncFrameGenerationDispatchResult success(
            int actualGeneratedCount,
            ProviderOutputLease outputLease,
            HistoryDisposition historyDisposition
    ) {
        ProviderOutputLease lease = Objects.requireNonNull(outputLease, "outputLease cannot be null");
        if (actualGeneratedCount < 0) {
            throw new IllegalArgumentException("actualGeneratedCount cannot be negative");
        }
        List<VulkanTexture> outputs = List.copyOf(lease.generatedOutputs());
        if (outputs.size() != actualGeneratedCount) {
            throw new IllegalArgumentException(
                    "actualGeneratedCount must match the leased generated output count"
            );
        }
        Objects.requireNonNull(lease.completion(), "outputLease completion cannot be null");
        Objects.requireNonNull(lease.outputKey(), "outputLease outputKey cannot be null");
        return new AsyncFrameGenerationDispatchResult(
                Status.SUCCESS,
                actualGeneratedCount,
                lease,
                outputs,
                lease.realOutput(),
                Objects.requireNonNull(historyDisposition, "historyDisposition cannot be null"),
                null
        );
    }

    public static AsyncFrameGenerationDispatchResult failed(String failureReason) {
        String reason = Objects.requireNonNull(failureReason, "failureReason cannot be null");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("failureReason cannot be blank");
        }
        return new AsyncFrameGenerationDispatchResult(
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

    public @Nullable ProviderOutputLease outputLease() {
        return outputLease;
    }

    public List<VulkanTexture> generatedOutputs() {
        return generatedOutputs;
    }

    public @Nullable VulkanTexture realOutput() {
        return realOutput;
    }

    public FrameGenerationDispatchCompletion completion() {
        return outputLease == null
                ? FrameGenerationDispatchCompletion.completed()
                : outputLease.completion();
    }

    public HistoryDisposition historyDisposition() {
        return historyDisposition;
    }

    public @Nullable String failureReason() {
        return failureReason;
    }
}
