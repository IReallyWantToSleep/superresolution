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

package io.homo.superresolution.common.perf;

/**
 * Stable event identifiers used by the CPU frame-pacing trace.
 *
 * <p>The enum name is written to the CSV {@code event_id} column. The
 * human-readable event name remains in the separate {@code event} column so
 * viewers can use the enum value as their schema key without losing the
 * original operation name for custom performance regions.</p>
 */
public enum FramePacingEvent {
    TRACE_START("trace_start"),
    TRACE_STOP("trace_stop"),

    FRAME("Frame"),
    REFLEX_SLEEP("Reflex Sleep"),
    LEVEL_RENDER("Level Render"),
    MAIN_RENDER("Main Render"),
    UPSCALE("Upscale"),
    GUI("GUI"),
    GL_INPUT_CONVERT("GL Input Convert"),
    GL_INTEROP_FLIP("GL Interop Flip"),
    GL_CAPTURE_FLIP("GL Capture Flip"),

    REAL_FRAME_RENDERING("real_frame_rendering"),
    FRAME_GENERATION_INPUT_SNAPSHOT("frame_generation_input_snapshot"),
    FRAME_GENERATION_ENQUEUE_WAIT("frame_generation_enqueue_wait"),
    PRESENTATION_ENQUEUE("presentation_enqueue"),
    FRAME_GENERATION_DISPATCH("frame_generation_dispatch"),
    FRAME_GENERATION_QUEUE_WAIT("frame_generation_queue_wait"),
    FRAME_GENERATION_PROVIDER_DISPATCH("frame_generation_provider_dispatch"),

    PRESENT_BATCH("present_batch"),
    PRESENT_TARGET_ACQUIRE("present_target_acquire"),
    PRESENT_BLIT_SUBMIT("present_blit_submit"),
    PRESENT_PACING_WAIT("present_pacing_wait"),
    PRESENT_CALL("present_call"),
    PRESENT_IMAGE_FIRST_PIXEL_VISIBLE("present_image_first_pixel_visible"),
    PRESENT_IMAGE_FIRST_PIXEL_OUT("present_image_first_pixel_out"),
    PRESENT_BATCH_RELEASE("present_batch_release"),

    EXTERNAL_FRAME_GENERATION_PREPARE("external_frame_generation_prepare"),
    EXTERNAL_PRESENT_BLIT_SUBMIT("external_present_blit_submit"),
    EXTERNAL_PRESENT("external_present"),

    CUSTOM("");

    private final String eventName;

    FramePacingEvent(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Returns the legacy/readable event name associated with this identifier.
     */
    public String eventName() {
        return eventName;
    }

    /**
     * Resolves an existing trace event name to its stable identifier.
     */
    public static FramePacingEvent fromEventName(String eventName) {
        if (eventName == null || eventName.isEmpty()) {
            return CUSTOM;
        }
        for (FramePacingEvent event : values()) {
            if (!event.eventName.isEmpty() && event.eventName.equals(eventName)) {
                return event;
            }
        }
        return CUSTOM;
    }
}
