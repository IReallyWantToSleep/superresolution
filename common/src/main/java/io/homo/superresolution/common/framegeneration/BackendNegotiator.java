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

package io.homo.superresolution.common.framegeneration;

import io.homo.superresolution.api.registry.BackendGroup;
import io.homo.superresolution.api.registry.FrameGenerationDescription;
import io.homo.superresolution.api.registry.FrameGenerationProvider;
import io.homo.superresolution.api.registry.FrameGenerationRegistry;
import io.homo.superresolution.api.registry.LowLatencyBinding;
import io.homo.superresolution.api.registry.LowLatencyDescription;
import io.homo.superresolution.api.registry.LowLatencyGroups;
import io.homo.superresolution.api.registry.LowLatencyRegistry;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Resolves the {@code (frame generation backend, low latency backend)} pair for the current
 * configuration. Users pick <em>algorithm groups</em> in the UI; the negotiator picks a
 * concrete backend inside each group, honoring binding constraints between them.
 * <p>
 * Both sides negotiate strictly within the selected group, or across all groups when the
 * frame generation side is set to auto. Priority within a group breaks ties (higher wins).
 * The result is stateless — {@link FrameGeneration} / {@link io.homo.superresolution.common.lowlatency.LowLatency}
 * cache and invalidate it themselves.
 */
public final class BackendNegotiator {
    /** Sentinel group id meaning "any FG group". */
    public static final String AUTO_FG_GROUP = "superresolution:auto";
    /** Group id meaning "no low latency". */
    public static final String NONE_LL_GROUP = LowLatencyGroups.NONE.getId();

    public record Resolution(
            @Nullable String fgBackendId,
            @Nullable String lowLatencyBackendId
    ) {
        public static final Resolution EMPTY = new Resolution(null, null);
    }

    private BackendNegotiator() {
    }

    /**
     * @param fgGroupId            {@link #AUTO_FG_GROUP} or a specific FG group id, or
     *                             {@code null} / empty for "no frame generation"
     * @param lowLatencyGroupId    {@link #NONE_LL_GROUP} for none, otherwise a specific LL
     *                             group id
     * @param fgProviderLookup     maps FG backend id to its instantiated provider (typically
     *                             {@code FrameGeneration}'s internal provider map)
     */
    public static Resolution resolve(
            @Nullable String fgGroupId,
            @Nullable String lowLatencyGroupId,
            Function<String, FrameGenerationProvider> fgProviderLookup
    ) {
        return resolve(fgGroupId, lowLatencyGroupId, null, fgProviderLookup);
    }

    public static Resolution resolve(
            @Nullable String fgGroupId,
            @Nullable String lowLatencyGroupId,
            @Nullable String preferredFgBackendId,
            Function<String, FrameGenerationProvider> fgProviderLookup
    ) {
        List<LowLatencyDescription> lowLatencyCandidates = collectLowLatencyCandidates(lowLatencyGroupId);

        boolean fgEnabled = fgGroupId != null && !fgGroupId.isEmpty();
        if (fgEnabled) {
            for (FrameGenerationDescription fg : collectFrameGenerationCandidates(
                    fgGroupId,
                    preferredFgBackendId,
                    fgProviderLookup
            )) {
                LowLatencyDescription paired = pickLowLatency(fg.getLowLatencyBinding(), lowLatencyCandidates);
                if (paired == null && fg.getLowLatencyBinding().getKind() == LowLatencyBinding.Kind.REQUIRES) {
                    continue;
                }
                return new Resolution(fg.getId(), paired == null ? null : paired.getId());
            }
        }
        // FG could not be satisfied — LL picks independently.
        LowLatencyDescription ll = lowLatencyCandidates.isEmpty() ? null : lowLatencyCandidates.get(0);
        return new Resolution(null, ll == null ? null : ll.getId());
    }

    private static List<LowLatencyDescription> collectLowLatencyCandidates(@Nullable String groupId) {
        List<LowLatencyDescription> out = new ArrayList<>();
        if (groupId == null || groupId.isEmpty() || NONE_LL_GROUP.equals(groupId)) {
            return out;
        }
        for (LowLatencyDescription description : LowLatencyRegistry.getDescriptions().values()) {
            BackendGroup group = description.getGroup();
            if (group == null || !groupId.equals(group.getId())) {
                continue;
            }
            if (!LowLatencyRegistry.isSupported(description)
                    || !description.isAvailable()
                    || !description.dependenciesSatisfied()) {
                continue;
            }
            out.add(description);
        }
        out.sort(Comparator.comparingInt(LowLatencyDescription::getPriority).reversed());
        return out;
    }

    private static List<FrameGenerationDescription> collectFrameGenerationCandidates(
            String fgGroupId,
            @Nullable String preferredFgBackendId,
            Function<String, FrameGenerationProvider> providerLookup
    ) {
        boolean auto = AUTO_FG_GROUP.equals(fgGroupId);
        boolean preferredAuto = preferredFgBackendId == null
                || preferredFgBackendId.isBlank()
                || AUTO_FG_GROUP.equals(preferredFgBackendId);
        List<FrameGenerationDescription> out = new ArrayList<>();
        for (FrameGenerationDescription description : FrameGenerationRegistry.getDescriptions().values()) {
            if (description.isAutomatic()) {
                continue;
            }
            if (!preferredAuto && !description.getId().equals(preferredFgBackendId)) {
                continue;
            }
            BackendGroup group = description.getGroup();
            if (!auto) {
                if (group == null || !fgGroupId.equals(group.getId())) {
                    continue;
                }
            }
            if (!FrameGenerationRegistry.isSupported(description)) {
                continue;
            }
            FrameGenerationProvider provider = providerLookup.apply(description.getId());
            if (provider == null || !provider.isAvailable() || !provider.dependenciesSatisfied()) {
                continue;
            }
            out.add(description);
        }
        out.sort(Comparator.comparingInt(FrameGenerationDescription::getPriority).reversed());
        return out;
    }

    private static @Nullable LowLatencyDescription pickLowLatency(
            LowLatencyBinding binding,
            List<LowLatencyDescription> candidates
    ) {
        switch (binding.getKind()) {
            case REQUIRES -> {
                String required = binding.getBackendId();
                for (LowLatencyDescription candidate : candidates) {
                    if (candidate.getId().equals(required)) {
                        return candidate;
                    }
                }
                return null;
            }
            case EXCLUDES -> {
                String excluded = binding.getBackendId();
                for (LowLatencyDescription candidate : candidates) {
                    if (!candidate.getId().equals(excluded)) {
                        return candidate;
                    }
                }
                return null;
            }
            case NONE -> {
                return candidates.isEmpty() ? null : candidates.get(0);
            }
        }
        return null;
    }
}
