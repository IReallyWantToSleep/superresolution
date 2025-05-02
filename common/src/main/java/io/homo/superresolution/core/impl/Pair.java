package io.homo.superresolution.core.impl;

public class Pair<F, S> {
    public final F first;
    public final S second;

    protected Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair(first, second);
    }

    public S right() {
        return second;
    }

    public F left() {
        return first;
    }
}
