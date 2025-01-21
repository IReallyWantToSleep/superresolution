package io.homo.superresolution.gui.options.option;

import java.util.ArrayList;
import java.util.Objects;


public class EnumData {
    protected ArrayList<EnumInfo<?>> enums = new ArrayList<>();

    public EnumData addEnum(EnumInfo<?> e) {
        if (e == null) return this;
        this.enums.add(e);
        return this;
    }

    public EnumData removeEnum(EnumInfo<?> e) {
        this.enums.remove(e);
        return this;
    }

    public EnumInfo<?> getEnum(int index) {
        try {
            return this.enums.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public EnumInfo<?> getEnum(String key) {
        for (EnumInfo<?> e : this.enums) {
            if (Objects.equals(e.key, key)) return e;
        }
        return null;
    }

    public EnumInfo<?> getEnum(Object value) {
        for (EnumInfo<?> e : this.enums) {
            if (Objects.equals(e.value, value)) return e;
        }
        return null;
    }

    public int indexOf(EnumInfo<Object> e) {
        return this.enums.indexOf(e);
    }

    public static class EnumInfo<T> {
        public String key;
        public String displayName;
        public T value;

        public EnumInfo<T> setKey(String key) {
            this.key = key;
            return this;
        }

        public EnumInfo<T> setValue(T value) {
            this.value = value;
            return this;
        }

        public String getDisplayName() {
            return displayName == null ? this.key : this.displayName;
        }

        public EnumInfo<T> setDisplayName(String name) {
            this.displayName = name;
            return this;
        }
    }
}
