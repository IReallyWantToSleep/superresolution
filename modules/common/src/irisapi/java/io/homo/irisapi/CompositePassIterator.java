package io.homo.irisapi;

import com.google.common.collect.UnmodifiableIterator;
import io.homo.irisapi.handlers.IrisRenderingPipelineHandler;

import java.util.Iterator;

public final class CompositePassIterator extends UnmodifiableIterator<Object> {
    private final Iterator<?> passes;
    private final ICompositeRendererAccessor renderer;
    private NamedCompositePass current;
    private IrisCompositePassType type;

    public CompositePassIterator(Iterator<?> passes, ICompositeRendererAccessor renderer) {
        this.passes = passes;
        this.renderer = renderer;
    }

    @Override
    public boolean hasNext() {
        if (current != null) {
            IrisRenderingPipelineHandler.onCompositePassDispatchAfter(renderer, current, type);
            IrisRenderingPipelineHandler.onCompositePassEnd(renderer, current, type);
            current = null;
        }
        return passes.hasNext();
    }

    @Override
    public Object next() {
        current = (NamedCompositePass) passes.next();
        type = IrisReflectionUtils.getCompositePassType(current);
        IrisRenderingPipelineHandler.onCompositePassStart(renderer, current, type);
        IrisRenderingPipelineHandler.onCompositePassDispatchBefore(renderer, current, type);
        return current;
    }
}
