/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.irisapi;

import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

public class IrisCompositePassRenderingEvent extends Event {
    protected @Nullable CompositeRenderer compositeRenderer;
    protected IrisCompositeRenderingPhase phase;
    protected String passName;
    protected IrisCompositePassType passType;

    public NamedCompositePass getCompositePass() {
        return compositePass;
    }

    protected NamedCompositePass compositePass;

    public @Nullable CompositeRenderer getCompositeRenderer() {
        return compositeRenderer;
    }

    public IrisCompositeRenderingPhase getPhase() {
        return phase;
    }


    public String getPassName() {
        return passName;
    }

    public IrisCompositePassType getPassType() {
        return passType;
    }

    public static class PassBegin extends IrisCompositePassRenderingEvent {
        public PassBegin(
                @Nullable CompositeRenderer compositeRenderer,
                IrisCompositeRenderingPhase phase,
                String passName,
                IrisCompositePassType passType,
                NamedCompositePass compositePass
        ) {
            this.compositeRenderer = compositeRenderer;
            this.phase = phase;
            this.passName = passName;
            this.passType = passType;
            this.compositePass = compositePass;
        }
    }

    public static class BeforePassRender extends IrisCompositePassRenderingEvent {
        public BeforePassRender(
                @Nullable CompositeRenderer compositeRenderer,
                IrisCompositeRenderingPhase phase,
                String passName,
                IrisCompositePassType passType,
                NamedCompositePass compositePass
        ) {
            this.compositeRenderer = compositeRenderer;
            this.phase = phase;
            this.passName = passName;
            this.passType = passType;
            this.compositePass = compositePass;
        }
    }

    public static class AfterPassRender extends IrisCompositePassRenderingEvent {
        public AfterPassRender(
                @Nullable CompositeRenderer compositeRenderer,
                IrisCompositeRenderingPhase phase,
                String passName,
                IrisCompositePassType passType,
                NamedCompositePass compositePass
        ) {
            this.compositeRenderer = compositeRenderer;
            this.phase = phase;
            this.passName = passName;
            this.passType = passType;
            this.compositePass = compositePass;
        }
    }

    public static class PassEnd extends IrisCompositePassRenderingEvent {
        public PassEnd(
                @Nullable CompositeRenderer compositeRenderer,
                IrisCompositeRenderingPhase phase,
                String passName,
                IrisCompositePassType passType,
                NamedCompositePass compositePass
        ) {
            this.compositeRenderer = compositeRenderer;
            this.phase = phase;
            this.passName = passName;
            this.passType = passType;
            this.compositePass = compositePass;
        }
    }
}
