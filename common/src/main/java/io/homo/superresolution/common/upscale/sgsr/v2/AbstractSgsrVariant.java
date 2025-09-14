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

package io.homo.superresolution.common.upscale.sgsr.v2;

import io.homo.superresolution.core.impl.Destroyable;
import io.homo.superresolution.core.impl.Resizable;
import io.homo.superresolution.core.graphics.impl.framebuffer.IFrameBuffer;
import io.homo.superresolution.common.upscale.DispatchResource;

public abstract class AbstractSgsrVariant implements Resizable, Destroyable {
    protected IFrameBuffer output;

    public void setOutput(IFrameBuffer output) {
        this.output = output;
    }

    public abstract void dispatch(DispatchResource resource, Sgsr2 sgsr);

    public abstract void init(Sgsr2 sgsr);
}
