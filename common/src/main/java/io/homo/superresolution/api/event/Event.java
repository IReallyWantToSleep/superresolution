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

package io.homo.superresolution.api.event;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Event<T> {
    private final Class<T> listenerType;
    private final Function<T[], T> invokerFactory;
    private final List<T> listeners = new ArrayList<>();
    private T invoker;

    public Event(Class<T> listenerType, Function<T[], T> invokerFactory) {
        this.listenerType = listenerType;
        this.invokerFactory = invokerFactory;
        updateInvoker();
    }

    public void register(T listener) {
        listeners.add(listener);
        updateInvoker();
    }

    public T invoker() {
        return invoker;
    }

    @SuppressWarnings("unchecked")
    private void updateInvoker() {
        T[] listenersArray = listeners.toArray((T[]) Array.newInstance(listenerType, 0));
        invoker = invokerFactory.apply(listenersArray);
    }

    public boolean hasEvent() {
        return !listeners.isEmpty();
    }
}
