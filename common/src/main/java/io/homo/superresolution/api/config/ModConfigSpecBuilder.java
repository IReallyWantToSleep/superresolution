package io.homo.superresolution.api.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.io.ParsingException;
import io.homo.superresolution.api.config.values.list.*;
import io.homo.superresolution.api.config.values.single.*;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ModConfigSpecBuilder {
    protected final Map<List<String>, ConfigValue<?>> values = new LinkedHashMap<>();
    protected final Map<List<String>, String> comments = new HashMap<>();
    protected boolean autoSave = false;
    protected boolean autoReload = false;
    protected final ConfigSpec spec = new ConfigSpec();
    private List<String> lastPath = List.of();
    private Path configPath;

    protected static List<String> splitPath(String path) {
        return Arrays.asList(path.split("/"));
    }

    public ModConfigSpecBuilder autoSave(boolean autoSave) {
        this.autoSave = autoSave;
        return this;
    }

    public ModConfigSpecBuilder autoReload(boolean autoReload) {
        this.autoReload = autoReload;
        return this;
    }

    public ModConfigSpecBuilder configPath(Path configPath) {
        this.configPath = configPath;
        return this;
    }

    public ModConfigSpecBuilder comment(String comment) {
        if (!lastPath.isEmpty()) {
            comments.put(new ArrayList<>(lastPath), comment);
        } else {
            throw new RuntimeException();
        }
        return this;
    }

    public ModConfigSpecBuilder comment(String path, String comment) {
        comments.put(new ArrayList<>(splitPath(path)), comment);
        return this;
    }


    public BooleanValue defineBoolean(String path, Supplier<Boolean> defaultSupplier) {
        return defineBoolean(path, defaultSupplier, null, v -> true);
    }

    public BooleanValue defineBoolean(List<String> path, Supplier<Boolean> defaultSupplier) {
        return defineBoolean(path, defaultSupplier, null, v -> true);
    }

    public BooleanValue defineBoolean(String path, Supplier<Boolean> defaultSupplier, String comment) {
        return defineBoolean(path, defaultSupplier, comment, v -> true);
    }

    public BooleanValue defineBoolean(List<String> path, Supplier<Boolean> defaultSupplier, String comment) {
        return defineBoolean(path, defaultSupplier, comment, v -> true);
    }

    public BooleanValue defineBoolean(String path, Supplier<Boolean> defaultSupplier, Predicate<Boolean> validator) {
        return defineBoolean(path, defaultSupplier, null, validator);
    }

    public BooleanValue defineBoolean(List<String> path, Supplier<Boolean> defaultSupplier, Predicate<Boolean> validator) {
        return defineBoolean(path, defaultSupplier, null, validator);
    }

    public BooleanValue defineBoolean(
            String path,
            Supplier<Boolean> defaultSupplier,
            String comment,
            Predicate<Boolean> validator
    ) {
        return defineBoolean(splitPath(path), defaultSupplier, comment, validator);
    }

    public BooleanValue defineBoolean(
            List<String> path,
            Supplier<Boolean> defaultSupplier,
            String comment,
            Predicate<Boolean> validator
    ) {
        this.lastPath = path;
        BooleanValue value = new BooleanValue(path, defaultSupplier, comment, validator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public <T extends Enum<T>> EnumValue<T> defineEnum(
            String path,
            Class<T> enumClass,
            Supplier<T> defaultSupplier
    ) {
        return defineEnum(path, enumClass, defaultSupplier, null);
    }

    public <T extends Enum<T>> EnumValue<T> defineEnum(
            List<String> path,
            Class<T> enumClass,
            Supplier<T> defaultSupplier
    ) {
        return defineEnum(path, enumClass, defaultSupplier, null);
    }

    public <T extends Enum<T>> EnumValue<T> defineEnum(
            String path,
            Class<T> enumClass,
            Supplier<T> defaultSupplier,
            String comment
    ) {
        return defineEnum(splitPath(path), enumClass, defaultSupplier, comment);
    }

    public <T extends Enum<T>> EnumValue<T> defineEnum(
            List<String> path,
            Class<T> enumClass,
            Supplier<T> defaultSupplier,
            String comment
    ) {
        this.lastPath = path;
        EnumValue<T> value = new EnumValue<>(path, defaultSupplier, enumClass, comment);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public FloatValue defineFloat(String path, Supplier<Float> defaultSupplier) {
        return defineFloat(path, defaultSupplier, null, v -> true);
    }

    public FloatValue defineFloat(List<String> path, Supplier<Float> defaultSupplier) {
        return defineFloat(path, defaultSupplier, null, v -> true);
    }

    public FloatValue defineFloat(String path, Supplier<Float> defaultSupplier, String comment) {
        return defineFloat(path, defaultSupplier, comment, v -> true);
    }

    public FloatValue defineFloat(List<String> path, Supplier<Float> defaultSupplier, String comment) {
        return defineFloat(path, defaultSupplier, comment, v -> true);
    }

    public FloatValue defineFloat(String path, Supplier<Float> defaultSupplier, Predicate<Float> validator) {
        return defineFloat(path, defaultSupplier, null, validator);
    }

    public FloatValue defineFloat(List<String> path, Supplier<Float> defaultSupplier, Predicate<Float> validator) {
        return defineFloat(path, defaultSupplier, null, validator);
    }

    public FloatValue defineFloat(
            String path,
            Supplier<Float> defaultSupplier,
            String comment,
            Predicate<Float> validator
    ) {
        return defineFloat(splitPath(path), defaultSupplier, comment, validator);
    }

    public FloatValue defineFloat(
            List<String> path,
            Supplier<Float> defaultSupplier,
            String comment,
            Predicate<Float> validator
    ) {
        this.lastPath = path;
        FloatValue value = new FloatValue(path, defaultSupplier, comment, validator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public IntValue defineInt(String path, Supplier<Integer> defaultSupplier) {
        return defineInt(path, defaultSupplier, null, v -> true);
    }

    public IntValue defineInt(List<String> path, Supplier<Integer> defaultSupplier) {
        return defineInt(path, defaultSupplier, null, v -> true);
    }

    public IntValue defineInt(String path, Supplier<Integer> defaultSupplier, String comment) {
        return defineInt(path, defaultSupplier, comment, v -> true);
    }

    public IntValue defineInt(List<String> path, Supplier<Integer> defaultSupplier, String comment) {
        return defineInt(path, defaultSupplier, comment, v -> true);
    }

    public IntValue defineInt(String path, Supplier<Integer> defaultSupplier, Predicate<Integer> validator) {
        return defineInt(path, defaultSupplier, null, validator);
    }

    public IntValue defineInt(List<String> path, Supplier<Integer> defaultSupplier, Predicate<Integer> validator) {
        return defineInt(path, defaultSupplier, null, validator);
    }

    public IntValue defineInt(
            String path,
            Supplier<Integer> defaultSupplier,
            String comment,
            Predicate<Integer> validator
    ) {
        return defineInt(splitPath(path), defaultSupplier, comment, validator);
    }

    public IntValue defineInt(
            List<String> path,
            Supplier<Integer> defaultSupplier,
            String comment,
            Predicate<Integer> validator
    ) {
        this.lastPath = path;
        IntValue value = new IntValue(path, defaultSupplier, comment, validator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public LongValue defineLong(String path, Supplier<Long> defaultSupplier) {
        return defineLong(path, defaultSupplier, null, v -> true);
    }

    public LongValue defineLong(List<String> path, Supplier<Long> defaultSupplier) {
        return defineLong(path, defaultSupplier, null, v -> true);
    }

    public LongValue defineLong(String path, Supplier<Long> defaultSupplier, String comment) {
        return defineLong(path, defaultSupplier, comment, v -> true);
    }

    public LongValue defineLong(List<String> path, Supplier<Long> defaultSupplier, String comment) {
        return defineLong(path, defaultSupplier, comment, v -> true);
    }

    public LongValue defineLong(String path, Supplier<Long> defaultSupplier, Predicate<Long> validator) {
        return defineLong(path, defaultSupplier, null, validator);
    }

    public LongValue defineLong(List<String> path, Supplier<Long> defaultSupplier, Predicate<Long> validator) {
        return defineLong(path, defaultSupplier, null, validator);
    }

    public LongValue defineLong(
            String path,
            Supplier<Long> defaultSupplier,
            String comment,
            Predicate<Long> validator
    ) {
        return defineLong(splitPath(path), defaultSupplier, comment, validator);
    }

    public LongValue defineLong(
            List<String> path,
            Supplier<Long> defaultSupplier,
            String comment,
            Predicate<Long> validator
    ) {
        this.lastPath = path;
        LongValue value = new LongValue(path, defaultSupplier, comment, validator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public StringValue defineString(String path, Supplier<String> defaultSupplier) {
        return defineString(path, defaultSupplier, null, v -> true);
    }

    public StringValue defineString(List<String> path, Supplier<String> defaultSupplier) {
        return defineString(path, defaultSupplier, null, v -> true);
    }

    public StringValue defineString(String path, Supplier<String> defaultSupplier, String comment) {
        return defineString(path, defaultSupplier, comment, v -> true);
    }

    public StringValue defineString(List<String> path, Supplier<String> defaultSupplier, String comment) {
        return defineString(path, defaultSupplier, comment, v -> true);
    }

    public StringValue defineString(String path, Supplier<String> defaultSupplier, Predicate<String> validator) {
        return defineString(path, defaultSupplier, null, validator);
    }

    public StringValue defineString(List<String> path, Supplier<String> defaultSupplier, Predicate<String> validator) {
        return defineString(path, defaultSupplier, null, validator);
    }

    public StringValue defineString(
            String path,
            Supplier<String> defaultSupplier,
            String comment,
            Predicate<String> validator
    ) {
        return defineString(splitPath(path), defaultSupplier, comment, validator);
    }

    public StringValue defineString(
            List<String> path,
            Supplier<String> defaultSupplier,
            String comment,
            Predicate<String> validator
    ) {
        this.lastPath = path;
        StringValue value = new StringValue(path, defaultSupplier, comment, validator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }

    public DoubleValue defineDouble(String path, Supplier<Double> defaultSupplier) {
        return defineDouble(path, defaultSupplier, null, v -> true);
    }

    public DoubleValue defineDouble(List<String> path, Supplier<Double> defaultSupplier) {
        return defineDouble(path, defaultSupplier, null, v -> true);
    }

    public DoubleValue defineDouble(String path, Supplier<Double> defaultSupplier, String comment) {
        return defineDouble(path, defaultSupplier, comment, v -> true);
    }

    public DoubleValue defineDouble(List<String> path, Supplier<Double> defaultSupplier, String comment) {
        return defineDouble(path, defaultSupplier, comment, v -> true);
    }

    public DoubleValue defineDouble(String path, Supplier<Double> defaultSupplier, Predicate<Double> validator) {
        return defineDouble(path, defaultSupplier, null, validator);
    }

    public DoubleValue defineDouble(List<String> path, Supplier<Double> defaultSupplier, Predicate<Double> validator) {
        return defineDouble(path, defaultSupplier, null, validator);
    }

    public DoubleValue defineDouble(
            String path,
            Supplier<Double> defaultSupplier,
            String comment,
            Predicate<Double> validator
    ) {
        return defineDouble(splitPath(path), defaultSupplier, comment, validator);
    }

    public DoubleValue defineDouble(
            List<String> path,
            Supplier<Double> defaultSupplier,
            String comment,
            Predicate<Double> validator
    ) {
        this.lastPath = path;
        DoubleValue value = new DoubleValue(path, defaultSupplier, comment, validator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }

    public <E> ListValue<E> defineList(
            String path,
            Supplier<List<E>> defaultSupplier
    ) {
        return defineList(path, defaultSupplier, null, obj -> (E) obj, v -> true);
    }

    public <E> ListValue<E> defineList(
            List<String> path,
            Supplier<List<E>> defaultSupplier
    ) {
        return defineList(path, defaultSupplier, null, obj -> (E) obj, v -> true);
    }

    public <E> ListValue<E> defineList(
            String path,
            Supplier<List<E>> defaultSupplier,
            String comment
    ) {
        return defineList(path, defaultSupplier, comment, obj -> (E) obj, v -> true);
    }

    public <E> ListValue<E> defineList(
            List<String> path,
            Supplier<List<E>> defaultSupplier,
            String comment
    ) {
        return defineList(path, defaultSupplier, comment, obj -> (E) obj, v -> true);
    }

    public <E> ListValue<E> defineList(
            String path,
            Supplier<List<E>> defaultSupplier,
            Function<Object, E> elementConverter,
            Predicate<E> elementValidator
    ) {
        return defineList(path, defaultSupplier, null, elementConverter, elementValidator);
    }

    public <E> ListValue<E> defineList(
            List<String> path,
            Supplier<List<E>> defaultSupplier,
            Function<Object, E> elementConverter,
            Predicate<E> elementValidator
    ) {
        return defineList(path, defaultSupplier, null, elementConverter, elementValidator);
    }

    public <E> ListValue<E> defineList(
            String path,
            Supplier<List<E>> defaultSupplier,
            String comment,
            Function<Object, E> elementConverter,
            Predicate<E> elementValidator
    ) {
        return defineList(splitPath(path), defaultSupplier, comment, elementConverter, elementValidator);
    }

    public <E> ListValue<E> defineList(
            List<String> path,
            Supplier<List<E>> defaultSupplier,
            String comment,
            Function<Object, E> elementConverter,
            Predicate<E> elementValidator
    ) {
        this.lastPath = path;

        ListValue<E> value = new ListValue<>(path, defaultSupplier, comment, elementConverter, elementValidator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public BooleanListValue defineBooleanList(String path, Supplier<List<Boolean>> defaultSupplier) {
        return defineBooleanList(path, defaultSupplier, null, v -> true);
    }

    public BooleanListValue defineBooleanList(List<String> path, Supplier<List<Boolean>> defaultSupplier) {
        return defineBooleanList(path, defaultSupplier, null, v -> true);
    }

    public BooleanListValue defineBooleanList(String path, Supplier<List<Boolean>> defaultSupplier, String comment) {
        return defineBooleanList(path, defaultSupplier, comment, v -> true);
    }

    public BooleanListValue defineBooleanList(List<String> path, Supplier<List<Boolean>> defaultSupplier, String comment) {
        return defineBooleanList(path, defaultSupplier, comment, v -> true);
    }

    public BooleanListValue defineBooleanList(String path, Supplier<List<Boolean>> defaultSupplier, Predicate<Boolean> elementValidator) {
        return defineBooleanList(path, defaultSupplier, null, elementValidator);
    }

    public BooleanListValue defineBooleanList(List<String> path, Supplier<List<Boolean>> defaultSupplier, Predicate<Boolean> elementValidator) {
        return defineBooleanList(path, defaultSupplier, null, elementValidator);
    }

    public BooleanListValue defineBooleanList(
            String path,
            Supplier<List<Boolean>> defaultSupplier,
            String comment,
            Predicate<Boolean> elementValidator
    ) {
        return defineBooleanList(splitPath(path), defaultSupplier, comment, elementValidator);
    }

    public BooleanListValue defineBooleanList(
            List<String> path,
            Supplier<List<Boolean>> defaultSupplier,
            String comment,
            Predicate<Boolean> elementValidator
    ) {
        this.lastPath = path;
        BooleanListValue value = new BooleanListValue(path, defaultSupplier, comment, elementValidator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public DoubleListValue defineDoubleList(String path, Supplier<List<Double>> defaultSupplier) {
        return defineDoubleList(path, defaultSupplier, null, v -> true);
    }

    public DoubleListValue defineDoubleList(List<String> path, Supplier<List<Double>> defaultSupplier) {
        return defineDoubleList(path, defaultSupplier, null, v -> true);
    }

    public DoubleListValue defineDoubleList(String path, Supplier<List<Double>> defaultSupplier, String comment) {
        return defineDoubleList(path, defaultSupplier, comment, v -> true);
    }

    public DoubleListValue defineDoubleList(List<String> path, Supplier<List<Double>> defaultSupplier, String comment) {
        return defineDoubleList(path, defaultSupplier, comment, v -> true);
    }

    public DoubleListValue defineDoubleList(String path, Supplier<List<Double>> defaultSupplier, Predicate<Double> elementValidator) {
        return defineDoubleList(path, defaultSupplier, null, elementValidator);
    }

    public DoubleListValue defineDoubleList(List<String> path, Supplier<List<Double>> defaultSupplier, Predicate<Double> elementValidator) {
        return defineDoubleList(path, defaultSupplier, null, elementValidator);
    }

    public DoubleListValue defineDoubleList(
            String path,
            Supplier<List<Double>> defaultSupplier,
            String comment,
            Predicate<Double> elementValidator
    ) {
        return defineDoubleList(splitPath(path), defaultSupplier, comment, elementValidator);
    }

    public DoubleListValue defineDoubleList(
            List<String> path,
            Supplier<List<Double>> defaultSupplier,
            String comment,
            Predicate<Double> elementValidator
    ) {
        this.lastPath = path;
        DoubleListValue value = new DoubleListValue(path, defaultSupplier, comment, elementValidator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public FloatListValue defineFloatList(String path, Supplier<List<Float>> defaultSupplier) {
        return defineFloatList(path, defaultSupplier, null, v -> true);
    }

    public FloatListValue defineFloatList(List<String> path, Supplier<List<Float>> defaultSupplier) {
        return defineFloatList(path, defaultSupplier, null, v -> true);
    }

    public FloatListValue defineFloatList(String path, Supplier<List<Float>> defaultSupplier, String comment) {
        return defineFloatList(path, defaultSupplier, comment, v -> true);
    }

    public FloatListValue defineFloatList(List<String> path, Supplier<List<Float>> defaultSupplier, String comment) {
        return defineFloatList(path, defaultSupplier, comment, v -> true);
    }

    public FloatListValue defineFloatList(String path, Supplier<List<Float>> defaultSupplier, Predicate<Float> elementValidator) {
        return defineFloatList(path, defaultSupplier, null, elementValidator);
    }

    public FloatListValue defineFloatList(List<String> path, Supplier<List<Float>> defaultSupplier, Predicate<Float> elementValidator) {
        return defineFloatList(path, defaultSupplier, null, elementValidator);
    }

    public FloatListValue defineFloatList(
            String path,
            Supplier<List<Float>> defaultSupplier,
            String comment,
            Predicate<Float> elementValidator
    ) {
        return defineFloatList(splitPath(path), defaultSupplier, comment, elementValidator);
    }

    public FloatListValue defineFloatList(
            List<String> path,
            Supplier<List<Float>> defaultSupplier,
            String comment,
            Predicate<Float> elementValidator
    ) {
        this.lastPath = path;
        FloatListValue value = new FloatListValue(path, defaultSupplier, comment, elementValidator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public IntListValue defineIntList(String path, Supplier<List<Integer>> defaultSupplier) {
        return defineIntList(path, defaultSupplier, null, v -> true);
    }

    public IntListValue defineIntList(List<String> path, Supplier<List<Integer>> defaultSupplier) {
        return defineIntList(path, defaultSupplier, null, v -> true);
    }

    public IntListValue defineIntList(String path, Supplier<List<Integer>> defaultSupplier, String comment) {
        return defineIntList(path, defaultSupplier, comment, v -> true);
    }

    public IntListValue defineIntList(List<String> path, Supplier<List<Integer>> defaultSupplier, String comment) {
        return defineIntList(path, defaultSupplier, comment, v -> true);
    }

    public IntListValue defineIntList(String path, Supplier<List<Integer>> defaultSupplier, Predicate<Integer> elementValidator) {
        return defineIntList(path, defaultSupplier, null, elementValidator);
    }

    public IntListValue defineIntList(List<String> path, Supplier<List<Integer>> defaultSupplier, Predicate<Integer> elementValidator) {
        return defineIntList(path, defaultSupplier, null, elementValidator);
    }

    public IntListValue defineIntList(
            String path,
            Supplier<List<Integer>> defaultSupplier,
            String comment,
            Predicate<Integer> elementValidator
    ) {
        return defineIntList(splitPath(path), defaultSupplier, comment, elementValidator);
    }


    public IntListValue defineIntList(
            List<String> path,
            Supplier<List<Integer>> defaultSupplier,
            String comment,
            Predicate<Integer> elementValidator
    ) {
        this.lastPath = path;
        IntListValue value = new IntListValue(path, defaultSupplier, comment, elementValidator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public LongListValue defineLongList(String path, Supplier<List<Long>> defaultSupplier) {
        return defineLongList(path, defaultSupplier, null, v -> true);
    }

    public LongListValue defineLongList(List<String> path, Supplier<List<Long>> defaultSupplier) {
        return defineLongList(path, defaultSupplier, null, v -> true);
    }

    public LongListValue defineLongList(String path, Supplier<List<Long>> defaultSupplier, String comment) {
        return defineLongList(path, defaultSupplier, comment, v -> true);
    }

    public LongListValue defineLongList(List<String> path, Supplier<List<Long>> defaultSupplier, String comment) {
        return defineLongList(path, defaultSupplier, comment, v -> true);
    }

    public LongListValue defineLongList(String path, Supplier<List<Long>> defaultSupplier, Predicate<Long> elementValidator) {
        return defineLongList(path, defaultSupplier, null, elementValidator);
    }

    public LongListValue defineLongList(List<String> path, Supplier<List<Long>> defaultSupplier, Predicate<Long> elementValidator) {
        return defineLongList(path, defaultSupplier, null, elementValidator);
    }

    public LongListValue defineLongList(
            String path,
            Supplier<List<Long>> defaultSupplier,
            String comment,
            Predicate<Long> elementValidator
    ) {
        return defineLongList(splitPath(path), defaultSupplier, comment, elementValidator);
    }

    public LongListValue defineLongList(
            List<String> path,
            Supplier<List<Long>> defaultSupplier,
            String comment,
            Predicate<Long> elementValidator
    ) {
        this.lastPath = path;
        LongListValue value = new LongListValue(path, defaultSupplier, comment, elementValidator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public StringListValue defineStringList(String path, Supplier<List<String>> defaultSupplier) {
        return defineStringList(path, defaultSupplier, null, v -> true);
    }

    public StringListValue defineStringList(List<String> path, Supplier<List<String>> defaultSupplier) {
        return defineStringList(path, defaultSupplier, null, v -> true);
    }

    public StringListValue defineStringList(String path, Supplier<List<String>> defaultSupplier, String comment) {
        return defineStringList(path, defaultSupplier, comment, v -> true);
    }

    public StringListValue defineStringList(List<String> path, Supplier<List<String>> defaultSupplier, String comment) {
        return defineStringList(path, defaultSupplier, comment, v -> true);
    }

    public StringListValue defineStringList(String path, Supplier<List<String>> defaultSupplier, Predicate<String> elementValidator) {
        return defineStringList(path, defaultSupplier, null, elementValidator);
    }

    public StringListValue defineStringList(List<String> path, Supplier<List<String>> defaultSupplier, Predicate<String> elementValidator) {
        return defineStringList(path, defaultSupplier, null, elementValidator);
    }

    public StringListValue defineStringList(
            String path,
            Supplier<List<String>> defaultSupplier,
            String comment,
            Predicate<String> elementValidator
    ) {
        return defineStringList(splitPath(path), defaultSupplier, comment, elementValidator);
    }

    public StringListValue defineStringList(
            List<String> path,
            Supplier<List<String>> defaultSupplier,
            String comment,
            Predicate<String> elementValidator
    ) {
        this.lastPath = path;
        StringListValue value = new StringListValue(path, defaultSupplier, comment, elementValidator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }

    public <T extends Enum<T>> EnumListValue<T> defineEnumList(
            String path,
            Class<T> enumClass,
            Supplier<List<T>> defaultSupplier
    ) {
        return defineEnumList(path, enumClass, defaultSupplier, null, v -> true);
    }

    public <T extends Enum<T>> EnumListValue<T> defineEnumList(
            List<String> path,
            Class<T> enumClass,
            Supplier<List<T>> defaultSupplier
    ) {
        return defineEnumList(path, enumClass, defaultSupplier, null, v -> true);
    }

    public <T extends Enum<T>> EnumListValue<T> defineEnumList(
            String path,
            Class<T> enumClass,
            Supplier<List<T>> defaultSupplier,
            String comment
    ) {
        return defineEnumList(path, enumClass, defaultSupplier, comment, v -> true);
    }

    public <T extends Enum<T>> EnumListValue<T> defineEnumList(
            List<String> path,
            Class<T> enumClass,
            Supplier<List<T>> defaultSupplier,
            String comment
    ) {
        return defineEnumList(path, enumClass, defaultSupplier, comment, v -> true);
    }

    public <T extends Enum<T>> EnumListValue<T> defineEnumList(
            String path,
            Class<T> enumClass,
            Supplier<List<T>> defaultSupplier,
            Predicate<T> elementValidator
    ) {
        return defineEnumList(path, enumClass, defaultSupplier, null, elementValidator);
    }

    public <T extends Enum<T>> EnumListValue<T> defineEnumList(
            List<String> path,
            Class<T> enumClass,
            Supplier<List<T>> defaultSupplier,
            Predicate<T> elementValidator
    ) {
        return defineEnumList(path, enumClass, defaultSupplier, null, elementValidator);
    }

    public <T extends Enum<T>> EnumListValue<T> defineEnumList(
            String path,
            Class<T> enumClass,
            Supplier<List<T>> defaultSupplier,
            String comment,
            Predicate<T> elementValidator
    ) {
        return defineEnumList(splitPath(path), enumClass, defaultSupplier, comment, elementValidator);
    }

    public <T extends Enum<T>> EnumListValue<T> defineEnumList(
            List<String> path,
            Class<T> enumClass,
            Supplier<List<T>> defaultSupplier,
            String comment,
            Predicate<T> elementValidator
    ) {
        this.lastPath = path;
        EnumListValue<T> value = new EnumListValue<>(path, enumClass, defaultSupplier, comment, elementValidator);
        values.put(path, value);
        comments.put(path, comment);
        return value;
    }


    public ModConfigSpec build() {
        if (configPath == null) {
            throw new IllegalStateException("Config path must be set before building");
        }

        ModConfigSpec spec = new ModConfigSpec(this, configPath);
        spec.configValues.putAll(values);
        spec.comments.putAll(comments);
        values.values().forEach((values) -> values.configSpec = spec);
        spec.fillSpec();
        try {
            spec.load();
        } catch (ParsingException e) {
            System.err.println("Failed to parse config file: " + e.getMessage());
            spec.initializeDefaultValues();
            spec.save();
        }

        return spec;
    }
}
