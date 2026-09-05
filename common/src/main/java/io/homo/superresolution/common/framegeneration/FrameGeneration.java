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
import io.homo.superresolution.api.registry.AsyncFrameGenerationDispatchRequest;
import io.homo.superresolution.api.registry.AsyncFrameGenerationDispatchResult;
import io.homo.superresolution.api.registry.FrameGenerationDescription;
import io.homo.superresolution.api.registry.FrameGenerationExecutionModel;
import io.homo.superresolution.api.registry.FrameGenerationProvider;
import io.homo.superresolution.api.registry.FrameGenerationRegistry;
import io.homo.superresolution.api.registry.ProviderInputSnapshot;
import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.config.enums.InteropSyncMode;
import io.homo.superresolution.common.framegeneration.constants.FGConstants;
import io.homo.superresolution.common.framegeneration.constants.FGConstantsFeature;
import io.homo.superresolution.common.lowlatency.LowLatency;
import io.homo.superresolution.common.presentation.capture.FrameResources;
import io.homo.superresolution.common.presentation.vulkan.VulkanPresentationFeature;
import io.homo.superresolution.common.workmode.SRWorkModeManager;
import io.homo.superresolution.common.workmode.SRWorkModeState;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.vulkan.VulkanAsyncDispatchCapabilities;
import io.homo.superresolution.core.streamline.Streamline;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Frontend for frame generation. Owns the backend-agnostic policy (mode gating,
 * shader-environment checks, per-frame constants) and dispatches the actual work to a
 * concrete {@link FrameGenerationProvider} picked by {@link BackendNegotiator} from the
 * algorithm group ({@code frame_generation/provider}) and optional concrete backend
 * preference ({@code frame_generation/backend}).
 * <p>
 * The FG group and concrete backend preference are latched at startup — together with the
 * LL group configuration they decide whether Streamline is initialized (see
 * {@code VulkanPresentationFeature.shouldInitializeStreamline}) — so changing either takes
 * effect after a restart. Runtime availability and low-latency bindings are still
 * re-negotiated as needed.
 */
public final class FrameGeneration {
    private static final Map<String, FrameGenerationProvider> providers = new LinkedHashMap<>();
    private static BackendNegotiator.Resolution loggedResolution;
    private static @Nullable Boolean startupStreamlineRequested;
    private static @Nullable Boolean loggedRestartStreamlineRequest;
    private static @Nullable String startupPreferredFgBackendId;
    private static @Nullable String loggedAsyncCapabilityFailure;
    private static boolean initialized;

    static {
        FrameGenerationDescriptions.register();
    }

    private FrameGeneration() {
    }

    public static synchronized void initialize() {
        if (initialized || !VulkanPresentationFeature.isRequested()) {
            return;
        }
        FGConstantsFeature.initialize();
        FGConstantsFeature.register();
        startupStreamlineRequested = VulkanPresentationFeature.shouldInitializeStreamline();
        startupPreferredFgBackendId = SuperResolutionConfig.getFrameGenerationBackend();
        for (FrameGenerationDescription description : FrameGenerationRegistry.getDescriptions().values()) {
            if (description.isAutomatic() || !FrameGenerationRegistry.isSupported(description)) {
                continue;
            }
            FrameGenerationProvider provider = description.createProvider();
            if (provider != null) {
                if (provider.executionModel() != description.getExecutionModel()) {
                    throw new IllegalStateException(
                            "Frame generation provider '" + description.getId()
                                    + "' declares " + provider.executionModel()
                                    + " at runtime but its description declares "
                                    + description.getExecutionModel()
                    );
                }
                providers.put(description.getId(), provider);
                provider.initialize();
            }
        }
        initialized = true;
    }

    public static void shutdown() {
        List<FrameGenerationProvider> providersToShutdown;
        List<ApplicationManagedShutdown> applicationManagedShutdowns;
        synchronized (FrameGeneration.class) {
            if (!initialized) {
                return;
            }
            providersToShutdown = List.copyOf(providers.values());
            applicationManagedShutdowns = new ArrayList<>();
            for (Map.Entry<String, FrameGenerationProvider> entry : providers.entrySet()) {
                if (entry.getValue().executionModel()
                        == FrameGenerationExecutionModel.APPLICATION_MANAGED_ASYNC) {
                    applicationManagedShutdowns.add(
                            new ApplicationManagedShutdown(entry.getKey(), entry.getValue())
                    );
                }
            }
            initialized = false;
        }

        for (ApplicationManagedShutdown shutdown : applicationManagedShutdowns) {
            VulkanPresentationFeature.shutdownApplicationManagedProvider(
                    shutdown.providerId(),
                    shutdown.provider()::shutdownOnFrameGenerationThread
            );
        }
        for (FrameGenerationProvider provider : providersToShutdown) {
            provider.shutdown();
        }

        synchronized (FrameGeneration.class) {
            providers.clear();
            loggedResolution = null;
            startupStreamlineRequested = null;
            loggedRestartStreamlineRequest = null;
            startupPreferredFgBackendId = null;
            loggedAsyncCapabilityFailure = null;
            FGConstantsFeature.shutdown();
        }
    }

    public static synchronized FramePresentPlan prepareFrame(
            FrameResources frameResources,
            int colorWidth,
            int colorHeight,
            int colorFormat,
            int backBufferCount,
            long commandBuffer
    ) {
        if (!initialized || !frameGenerationConfigured()) {
            disableFrameGeneration();
            return FramePresentPlan.none();
        }
        FrameGenerationProvider provider = activeProvider();
        FrameGenerationMode mode = displayedModeWith(provider);
        if (provider == null
                || provider.executionModel() != FrameGenerationExecutionModel.EXTERNAL_INTERPOSER
                || !mode.isEnabled()
                || frameResources == null
                || commandBuffer == 0L
                || !frameResources.hasHudlessColor()
                || !frameResources.hasDepth()
                || !frameResources.hasMotionVector()) {
            disableFrameGeneration();
            return FramePresentPlan.none();
        }

        FGConstants constants = FGConstantsFeature.getConstants(frameResources.logicalFrameIndex());
        if (constants == null) {
            disableFrameGeneration();
            return FramePresentPlan.none();
        }

        return provider.prepareFrame(
                frameResources,
                constants,
                mode,
                colorWidth,
                colorHeight,
                colorFormat,
                backBufferCount,
                commandBuffer
        );
    }

    /**
     * Captures immutable render-side input for the one negotiated application-managed
     * provider. Returning {@code null} marks the frame as real-only; this method never
     * dispatches provider work.
     */
    public static synchronized @Nullable ProviderInputSnapshot captureProviderInputSnapshotForFrame(
            FrameResources frameResources
    ) {
        String providerId = activeApplicationManagedProviderId();
        return providerId.isEmpty()
                ? null
                : captureProviderInputSnapshotForFrame(providerId, frameResources);
    }

    /**
     * Captures inputs only when the active provider still matches the scheduler
     * lifecycle that requested the snapshot.
     */
    public static synchronized @Nullable ProviderInputSnapshot captureProviderInputSnapshotForFrame(
            String expectedProviderId,
            FrameResources frameResources
    ) {
        Objects.requireNonNull(expectedProviderId, "expectedProviderId cannot be null");
        if (expectedProviderId.isBlank()) {
            throw new IllegalArgumentException("expectedProviderId cannot be blank");
        }
        if (!initialized || !frameGenerationConfigured()) {
            return null;
        }
        ProviderSelection selection = activeProviderSelection();
        FrameGenerationMode mode =
                displayedModeWith(selection == null ? null : selection.provider());
        if (selection == null
                || !selection.id().equals(expectedProviderId)
                || selection.executionModel() != FrameGenerationExecutionModel.APPLICATION_MANAGED_ASYNC
                || !mode.isEnabled()
                || frameResources == null
                || !frameResources.hasHudlessColor()
                || !frameResources.hasDepth()
                || !frameResources.hasMotionVector()) {
            return null;
        }

        FGConstants constants = FGConstantsFeature.getConstants(frameResources.logicalFrameIndex());
        if (constants == null) {
            return null;
        }
        return selection.provider().captureInputSnapshot(
                selection.id(),
                frameResources,
                constants,
                mode
        );
    }

    /**
     * Returns whether an existing scheduler may keep owning application-managed
     * presentation. A temporarily unavailable provider is compatible and produces
     * Real-only jobs; selecting another provider or an external interposer requires
     * a drain/restart instead of sharing one scheduler lifecycle.
     */
    public static synchronized boolean isApplicationManagedSchedulerCompatible(
            String schedulerProviderId
    ) {
        Objects.requireNonNull(schedulerProviderId, "schedulerProviderId cannot be null");
        if (schedulerProviderId.isBlank()) {
            throw new IllegalArgumentException("schedulerProviderId cannot be blank");
        }
        ProviderSelection selection = activeProviderSelection();
        return selection == null
                || (selection.executionModel() == FrameGenerationExecutionModel.APPLICATION_MANAGED_ASYNC
                && selection.id().equals(schedulerProviderId));
    }

    /**
     * Dispatches the provider captured by {@link ProviderInputSnapshot#providerId()}.
     * Provider code runs outside the facade monitor on the scheduler's FG thread.
     */
    public static AsyncFrameGenerationDispatchResult dispatchAsync(
            AsyncFrameGenerationDispatchRequest request
    ) {
        Objects.requireNonNull(request, "request cannot be null");
        ProviderSelection selection;
        synchronized (FrameGeneration.class) {
            selection = activeProviderSelection();
            if (!initialized || selection == null) {
                return AsyncFrameGenerationDispatchResult.failed(
                        "No active frame-generation provider"
                );
            }
            if (selection.executionModel() != FrameGenerationExecutionModel.APPLICATION_MANAGED_ASYNC) {
                return AsyncFrameGenerationDispatchResult.failed(
                        "Active provider does not use application-managed async dispatch"
                );
            }
            if (!selection.id().equals(request.providerInputSnapshot().providerId())) {
                return AsyncFrameGenerationDispatchResult.failed(
                        "Provider changed after the input snapshot was captured"
                );
            }
            if (request.frameResources().logicalFrameIndex()
                    != request.providerInputSnapshot().logicalFrameIndex()) {
                return AsyncFrameGenerationDispatchResult.failed(
                        "Provider input snapshot does not belong to the queued frame"
                );
            }
        }
        return selection.provider().dispatchAsync(request);
    }

    public static synchronized void finishPresent(
            FrameResources frameResources,
            FramePresentPlan plan
    ) {
        FrameGenerationProvider provider = activeProvider();
        if (provider != null
                && provider.executionModel() == FrameGenerationExecutionModel.EXTERNAL_INTERPOSER) {
            provider.finishPresent(frameResources, plan != null && plan.frameGenerationActive());
        }
    }

    public static synchronized void disableFrameGeneration() {
        for (FrameGenerationProvider provider : providers.values()) {
            provider.disable();
        }
    }

    public static void invalidateHistory() {
        FGConstantsFeature.invalidateHistory();
    }

    /**
     * Number of interpolated frames the presentation layer must present itself for the
     * next frame. Zero when disabled or when the active backend presents them.
     */
    public static synchronized int plannedGeneratedFrameCount() {
        if (!initialized || !frameGenerationConfigured()) {
            return 0;
        }
        FrameGenerationProvider provider = activeProvider();
        FrameGenerationMode mode = displayedModeWith(provider);
        if (!mode.isEnabled()) {
            return 0;
        }
        return provider == null ? 0 : provider.presentationManagedGeneratedFrameCount(mode);
    }

    public static synchronized boolean isSupported() {
        return presentationDependenciesSatisfied() && isSupportedWith(activeProvider());
    }

    public static boolean isFrameGenerationEnabled() {
        return displayedMode().isEnabled();
    }

    public static synchronized void setFrameGenerationMode(FrameGenerationMode mode) {
        FrameGenerationProvider provider = activeProvider();
        FrameGenerationMode selected = mode;
        if (selected == null || !isHardwareModeSupportedWith(provider, selected)) {
            selected = FrameGenerationMode.OFF;
        }
        SuperResolutionConfig.setFrameGenerationMode(selected);
        if (!displayedModeWith(provider).isEnabled()) {
            disableFrameGeneration();
        }
    }

    /**
     * Resolves the negotiated provider once and derives the mode from it. Every
     * predicate below used to call {@link #activeProvider()} itself, so one
     * {@code displayedMode()} used to run {@link BackendNegotiator#resolve} three times.
     */
    public static synchronized FrameGenerationMode displayedMode() {
        return frameGenerationConfigured()
                ? displayedModeWith(activeProvider())
                : FrameGenerationMode.OFF;
    }

    public static synchronized FrameGenerationMode[] availableModes() {
        FrameGenerationProvider provider = activeProvider();
        if (!isSupportedWith(provider)) {
            return new FrameGenerationMode[]{FrameGenerationMode.OFF};
        }
        return availableModesForMaximum(supportedGeneratedFrameCountWith(provider));
    }

    static FrameGenerationMode[] availableModesForMaximum(int maximumGeneratedFrames) {
        List<FrameGenerationMode> modes = new ArrayList<>();
        modes.add(FrameGenerationMode.OFF);
        for (FrameGenerationMode mode : FrameGenerationMode.values()) {
            if (mode != FrameGenerationMode.OFF
                    && mode.generatedFrameCount() <= maximumGeneratedFrames) {
                modes.add(mode);
            }
        }
        // if there are exactly three modes (OFF,AUTO,2X),remove AUTO
        if (modes.size() == 3) {
            modes.remove(FrameGenerationMode.AUTO);
        }
        return modes.toArray(FrameGenerationMode[]::new);
    }

    /**
     * The configured entry, falling back to the automatic one when the id is unknown.
     * The returned description may be an algorithm group representative (automatic with a
     * group) rather than a concrete backend.
     */
    public static FrameGenerationDescription mode() {
        FrameGenerationDescription description =
                FrameGenerationRegistry.getDescriptionById(SuperResolutionConfig.getFrameGenerationProvider());
        return description != null
                ? description
                : FrameGenerationRegistry.getDescriptionById(FrameGenerationDescriptions.AUTO_ID);
    }

    /**
     * Id of the FG backend actually in use, as resolved by the negotiator for the
     * currently configured FG and LL groups. Empty when nothing is usable.
     */
    public static synchronized String activeId() {
        String id = activeResolution().fgBackendId();
        return id == null ? "" : id;
    }

    /**
     * Id of the LL backend the negotiator picked to pair with the current FG choice.
     * Empty when no LL backend is active. Callers on the LL side (mainly {@link LowLatency})
     * use this to switch providers when the FG side's binding constraints flip.
     */
    public static synchronized String activeLowLatencyBackendId() {
        String id = activeResolution().lowLatencyBackendId();
        return id == null ? "" : id;
    }

    /**
     * Execution model of the single negotiated provider, or {@code null} when no provider
     * is active. A scheduler must only start for
     * {@link FrameGenerationExecutionModel#APPLICATION_MANAGED_ASYNC}.
     */
    public static synchronized @Nullable FrameGenerationExecutionModel activeExecutionModel() {
        ProviderSelection selection = activeProviderSelection();
        return selection == null ? null : selection.executionModel();
    }

    /**
     * Id of the single application-managed provider selected for the current scheduler
     * lifecycle, or an empty string when the active provider retains external ownership.
     */
    public static synchronized String activeApplicationManagedProviderId() {
        ProviderSelection selection = activeProviderSelection();
        return selection != null
                && selection.executionModel() == FrameGenerationExecutionModel.APPLICATION_MANAGED_ASYNC
                ? selection.id()
                : "";
    }

    /**
     * Resolves the startup-visible provider declaration before provider initialization.
     * Vulkan device creation uses this immutable declaration to decide whether it must
     * request a second queue. Runtime negotiation still performs the full provider
     * availability and dependency checks after initialization.
     */
    public static synchronized StartupProviderSelection startupProviderSelection() {
        FrameGenerationDescription description = resolveStartupProviderDescription();
        return description == null
                ? StartupProviderSelection.NONE
                : new StartupProviderSelection(
                        description.getId(),
                        description.getExecutionModel()
                );
    }

    /**
     * Runs the negotiator with the current configuration. Cheap enough to invoke per frame
     * (a few map iterations); callers that hit it repeatedly per frame should cache locally.
     */
    private static synchronized BackendNegotiator.Resolution activeResolution() {
        String fgGroupId = configuredFgGroupId();
        BackendNegotiator.Resolution resolution = fgGroupId == null || fgGroupId.isEmpty()
                ? BackendNegotiator.Resolution.EMPTY
                : BackendNegotiator.resolve(
                        fgGroupId,
                        LowLatency.configuredGroupId(),
                        startupPreferredFgBackendId != null
                                ? startupPreferredFgBackendId
                                : SuperResolutionConfig.getFrameGenerationBackend(),
                        FrameGeneration::providerForNegotiation
                );
        logResolution(resolution);
        logRestartRequirement();
        return resolution;
    }

    private static void logResolution(BackendNegotiator.Resolution resolution) {
        if (!resolution.equals(loggedResolution)) {
            SuperResolution.LOGGER.info(
                    "Frame generation backend negotiation: fg={}, lowLatency={}",
                    resolution.fgBackendId(),
                    resolution.lowLatencyBackendId()
            );
            loggedResolution = resolution;
        }
    }

    private static void logRestartRequirement() {
        if (startupStreamlineRequested == null) {
            return;
        }
        boolean streamlineRequested = VulkanPresentationFeature.shouldInitializeStreamline();
        if (streamlineRequested == startupStreamlineRequested
                || Boolean.valueOf(streamlineRequested).equals(loggedRestartStreamlineRequest)
                || (!streamlineRequested && !Streamline.isInterposerLoaded())) {
            return;
        }
        SuperResolution.LOGGER.warn(
                "Streamline backend selection changed after startup; restart the game to {} the Streamline interposer.",
                streamlineRequested ? "load" : "unload"
        );
        loggedRestartStreamlineRequest = streamlineRequested;
    }

    private static synchronized @Nullable FrameGenerationProvider activeProvider() {
        ProviderSelection selection = activeProviderSelection();
        return selection == null ? null : selection.provider();
    }

    private static @Nullable FrameGenerationProvider providerForNegotiation(String id) {
        FrameGenerationProvider provider = providers.get(id);
        if (provider == null
                || provider.executionModel() != FrameGenerationExecutionModel.APPLICATION_MANAGED_ASYNC) {
            return provider;
        }
        String unavailableReason = asyncDispatchUnavailableReason(id);
        if (unavailableReason.isEmpty()) {
            return provider;
        }
        String logKey = id + ": " + unavailableReason;
        if (!logKey.equals(loggedAsyncCapabilityFailure)) {
            SuperResolution.LOGGER.warn(
                    "Frame generation provider '{}' is unavailable: {}",
                    id,
                    unavailableReason
            );
            loggedAsyncCapabilityFailure = logKey;
        }
        return null;
    }

    private static String asyncDispatchUnavailableReason(String providerId) {
        if (RenderSystems.vulkan() == null || RenderSystems.vulkan().device() == null) {
            return "Vulkan device is unavailable";
        }
        VulkanAsyncDispatchCapabilities capabilities =
                RenderSystems.vulkan().device().asyncDispatchCapabilities();
        if (capabilities.available() && providerId.equals(capabilities.providerId())) {
            return "";
        }
        if (capabilities.available()) {
            return "Vulkan async foundation was created for provider '"
                    + capabilities.providerId() + "', not '" + providerId + "'";
        }
        return capabilities.unavailableReason();
    }

    private static @Nullable ProviderSelection activeProviderSelection() {
        String id = activeId();
        if (id.isEmpty()) {
            return null;
        }
        FrameGenerationProvider provider = providers.get(id);
        return provider == null
                ? null
                : new ProviderSelection(id, provider, provider.executionModel());
    }

    private static @Nullable FrameGenerationDescription resolveStartupProviderDescription() {
        String fgGroupId = configuredFgGroupId();
        if (fgGroupId == null || fgGroupId.isEmpty()) {
            return null;
        }
        String preferredBackendId = SuperResolutionConfig.getFrameGenerationBackend();
        boolean autoGroup = BackendNegotiator.AUTO_FG_GROUP.equals(fgGroupId);
        boolean preferredAuto = preferredBackendId == null
                || preferredBackendId.isBlank()
                || BackendNegotiator.AUTO_FG_GROUP.equals(preferredBackendId);
        List<FrameGenerationDescription> candidates = new ArrayList<>();
        for (FrameGenerationDescription description
                : FrameGenerationRegistry.getDescriptions().values()) {
            if (description.isAutomatic()) {
                continue;
            }
            if (!preferredAuto && !description.getId().equals(preferredBackendId)) {
                continue;
            }
            BackendGroup group = description.getGroup();
            if (!autoGroup && (group == null || !fgGroupId.equals(group.getId()))) {
                continue;
            }
            if (!FrameGenerationRegistry.isSupported(description)) {
                continue;
            }
            candidates.add(description);
        }
        candidates.sort(Comparator.comparingInt(FrameGenerationDescription::getPriority).reversed());
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Derives the FG group id passed to the negotiator from the configured entry. Automatic
     * with no group means "any group"; automatic with a group means "this group only";
     * a concrete backend selection restricts to that backend's group.
     */
    private static @Nullable String configuredFgGroupId() {
        FrameGenerationDescription description = mode();
        if (description == null) {
            return null;
        }
        BackendGroup group = description.getGroup();
        if (description.isAutomatic()) {
            return group != null ? group.getId() : BackendNegotiator.AUTO_FG_GROUP;
        }
        return group != null ? group.getId() : null;
    }

    /**
     * Cheap gates that make provider negotiation pointless. Checking these before
     * {@link #activeProvider()} keeps a disabled or incompatible setup at zero
     * {@link BackendNegotiator#resolve} calls per frame, which is what the predicates
     * used to achieve by short-circuiting before they reached the negotiator.
     */
    private static boolean frameGenerationConfigured() {
        FrameGenerationMode configured = SuperResolutionConfig.getFrameGenerationMode();
        return configured != null
                && configured != FrameGenerationMode.OFF
                && presentationDependenciesSatisfied()
                && isShaderEnvironmentCompatible();
    }

    private static boolean presentationDependenciesSatisfied() {
        return SuperResolutionConfig.isEnableVulkanPresentation()
                && SuperResolutionConfig.getInteropSyncMode() == InteropSyncMode.LowLatency;
    }

    private static boolean isSupportedWith(@Nullable FrameGenerationProvider provider) {
        return dependenciesSatisfiedWith(provider) && provider != null && provider.isAvailable();
    }

    private static int supportedGeneratedFrameCountWith(@Nullable FrameGenerationProvider provider) {
        return provider == null ? 0 : provider.supportedGeneratedFrameCount();
    }

    static synchronized boolean dependenciesSatisfied() {
        return dependenciesSatisfiedWith(activeProvider());
    }

    private static boolean dependenciesSatisfiedWith(@Nullable FrameGenerationProvider provider) {
        return presentationDependenciesSatisfied()
                && provider != null
                && provider.dependenciesSatisfied();
    }

    // FG only under shader_compat + loaded pack; vanilla/hack breaks UI presentation
    static boolean isShaderEnvironmentCompatible() {
        if (!SRWorkModeManager.isCurrentMode(SRWorkModeManager.SHADER_COMPAT)) {
            return false;
        }
        SRWorkModeState state = SRWorkModeManager.getCurrentState();
        return state.shaderPackInUse() && !state.shaderPackLoading();
    }

    private static FrameGenerationMode displayedModeWith(@Nullable FrameGenerationProvider provider) {
        FrameGenerationMode mode = SuperResolutionConfig.getFrameGenerationMode();
        return isModeSupportedWith(provider, mode) ? mode : FrameGenerationMode.OFF;
    }

    private static boolean isModeSupportedWith(
            @Nullable FrameGenerationProvider provider,
            FrameGenerationMode mode
    ) {
        return isHardwareModeSupportedWith(provider, mode)
                && (mode == FrameGenerationMode.OFF || isShaderEnvironmentCompatible());
    }

    private static boolean isHardwareModeSupportedWith(
            @Nullable FrameGenerationProvider provider,
            FrameGenerationMode mode
    ) {
        if (mode == null || mode == FrameGenerationMode.OFF) {
            return mode == FrameGenerationMode.OFF;
        }
        return isSupportedWith(provider)
                && mode.generatedFrameCount() <= supportedGeneratedFrameCountWith(provider);
    }

    private record ApplicationManagedShutdown(
            String providerId,
            FrameGenerationProvider provider
    ) {
    }

    private record ProviderSelection(
            String id,
            FrameGenerationProvider provider,
            FrameGenerationExecutionModel executionModel
    ) {
    }

    public record StartupProviderSelection(
            String providerId,
            FrameGenerationExecutionModel executionModel
    ) {
        private static final StartupProviderSelection NONE =
                new StartupProviderSelection("", FrameGenerationExecutionModel.EXTERNAL_INTERPOSER);

        public StartupProviderSelection {
            providerId = Objects.requireNonNull(providerId, "providerId cannot be null");
            executionModel = Objects.requireNonNull(
                    executionModel,
                    "executionModel cannot be null"
            );
        }

        public boolean applicationManagedAsync() {
            return executionModel == FrameGenerationExecutionModel.APPLICATION_MANAGED_ASYNC;
        }
    }
}
