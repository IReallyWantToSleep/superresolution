package io.homo.superresolution.common.gui.impl;

import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ClothConfigCategoryImpl implements ConfigCategory {
    private final ConfigBuilder builder;
    private final List<Object> data;
    private final Component categoryKey;
    private @Nullable ResourceLocation background;
    private @Nullable Supplier<Optional<FormattedText[]>> description = Optional::empty;

    public ClothConfigCategoryImpl(ConfigBuilder builder, Component categoryKey) {
        this.builder = builder;
        this.data = Lists.newArrayList();
        this.categoryKey = categoryKey;
    }

    public Component getCategoryKey() {
        return this.categoryKey;
    }

    public List<Object> getEntries() {
        return this.data;
    }

    public ConfigCategory addEntry(AbstractConfigListEntry entry) {
        this.data.add(entry);
        return this;
    }

    public ConfigCategory setCategoryBackground(ResourceLocation identifier) {
        this.background = identifier;
        return this;
    }

    public void removeCategory() {
        this.builder.removeCategory(this.categoryKey);
    }

    public @Nullable ResourceLocation getBackground() {
        return this.background;
    }

    public void setBackground(@Nullable ResourceLocation background) {
        this.background = background;
    }

    public @Nullable Supplier<Optional<FormattedText[]>> getDescription() {
        return this.description;
    }

    public void setDescription(@Nullable Supplier<Optional<FormattedText[]>> description) {
        this.description = description;
    }
}
