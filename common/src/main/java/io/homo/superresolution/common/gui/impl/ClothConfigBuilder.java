package io.homo.superresolution.common.gui.impl;

import com.google.common.collect.Maps;
import io.homo.superresolution.common.gui.screens.ClothStyleConfigScreen;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ClothConfigBuilder implements ConfigBuilder {
    private final Map<String, ConfigCategory> categoryMap = Maps.newLinkedHashMap();
    private Runnable savingRunnable;
    private Screen parent;
    private Component title = Component.translatable("text.cloth-config.config");
    private boolean editable = true;
    private boolean tabsSmoothScroll = true;
    private boolean listSmoothScroll = true;
    private boolean doesConfirmSave = true;
    private boolean transparentBackground = true;
    #if MC_VER > MC_1_20_1
    private ResourceLocation defaultBackground = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");
    #else
    private ResourceLocation defaultBackground = Screen.BACKGROUND_LOCATION;
    #endif
    private Consumer<Screen> afterInitConsumer = (screen) -> {
    };
    private String fallbackCategory = null;
    private boolean alwaysShowTabs = false;

    public void setGlobalizedExpanded(boolean globalizedExpanded) {

    }

    public boolean isAlwaysShowTabs() {
        return this.alwaysShowTabs;
    }

    public ConfigBuilder setAlwaysShowTabs(boolean alwaysShowTabs) {
        this.alwaysShowTabs = alwaysShowTabs;
        return this;
    }

    public ConfigBuilder setTransparentBackground(boolean transparentBackground) {
        this.transparentBackground = transparentBackground;
        return this;
    }

    public boolean hasTransparentBackground() {
        return this.transparentBackground;
    }

    public ConfigBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer) {
        this.afterInitConsumer = afterInitConsumer;
        return this;
    }

    @Override
    public void setGlobalized(boolean b) {

    }

    public ConfigBuilder setFallbackCategory(ConfigCategory fallbackCategory) {
        this.fallbackCategory = ((ConfigCategory) Objects.requireNonNull(fallbackCategory)).getCategoryKey().getString();
        return this;
    }

    public Screen getParentScreen() {
        return this.parent;
    }

    public ConfigBuilder setParentScreen(Screen parent) {
        this.parent = parent;
        return this;
    }

    public Component getTitle() {
        return this.title;
    }

    public ConfigBuilder setTitle(Component title) {
        this.title = title;
        return this;
    }

    public boolean isEditable() {
        return this.editable;
    }

    public ConfigBuilder setEditable(boolean editable) {
        this.editable = editable;
        return this;
    }

    public ConfigCategory getOrCreateCategory(Component categoryKey) {
        if (this.categoryMap.containsKey(categoryKey.getString())) {
            return this.categoryMap.get(categoryKey.getString());
        } else {
            if (this.fallbackCategory == null) {
                this.fallbackCategory = categoryKey.getString();
            }

            return this.categoryMap.computeIfAbsent(categoryKey.getString(), (key) -> new ClothConfigCategoryImpl(this, categoryKey));
        }
    }

    public ConfigBuilder removeCategory(Component category) {
        if (this.categoryMap.containsKey(category.getString()) && Objects.equals(this.fallbackCategory, category.getString())) {
            this.fallbackCategory = null;
        }

        if (!this.categoryMap.containsKey(category.getString())) {
            throw new NullPointerException("Category doesn't exist!");
        } else {
            this.categoryMap.remove(category.getString());
            return this;
        }
    }

    public ConfigBuilder removeCategoryIfExists(Component category) {
        if (this.categoryMap.containsKey(category.getString()) && Objects.equals(this.fallbackCategory, category.getString())) {
            this.fallbackCategory = null;
        }

        this.categoryMap.remove(category.getString());
        return this;
    }

    public boolean hasCategory(Component category) {
        return this.categoryMap.containsKey(category.getString());
    }

    public ConfigBuilder setShouldTabsSmoothScroll(boolean shouldTabsSmoothScroll) {
        this.tabsSmoothScroll = shouldTabsSmoothScroll;
        return this;
    }

    public boolean isTabsSmoothScrolling() {
        return this.tabsSmoothScroll;
    }

    public ConfigBuilder setShouldListSmoothScroll(boolean shouldListSmoothScroll) {
        this.listSmoothScroll = shouldListSmoothScroll;
        return this;
    }

    public boolean isListSmoothScrolling() {
        return this.listSmoothScroll;
    }

    public ConfigBuilder setDoesConfirmSave(boolean confirmSave) {
        this.doesConfirmSave = confirmSave;
        return this;
    }

    public boolean doesConfirmSave() {
        return this.doesConfirmSave;
    }

    public ResourceLocation getDefaultBackgroundTexture() {
        return this.defaultBackground;
    }

    public ConfigBuilder setDefaultBackgroundTexture(ResourceLocation texture) {
        this.defaultBackground = texture;
        return this;
    }

    public ConfigBuilder setSavingRunnable(Runnable runnable) {
        this.savingRunnable = runnable;
        return this;
    }

    public Consumer<Screen> getAfterInitConsumer() {
        return this.afterInitConsumer;
    }

    public Screen build() {
        if (!this.categoryMap.isEmpty() && this.fallbackCategory != null) {
            ClothStyleConfigScreen screen = new ClothStyleConfigScreen(this.parent, this.title, this.categoryMap, this.defaultBackground);
            screen.setSavingRunnable(this.savingRunnable);
            screen.setEditable(this.editable);
            screen.setFallbackCategory(this.fallbackCategory == null ? null : Component.literal(this.fallbackCategory));
            screen.setTransparentBackground(this.transparentBackground);
            screen.setAlwaysShowTabs(this.alwaysShowTabs);
            screen.setConfirmSave(this.doesConfirmSave);
            screen.setAfterInitConsumer(this.afterInitConsumer);

            return screen;
        } else {
            throw new NullPointerException("There cannot be no categories or fallback category!");
        }
    }

    public Runnable getSavingRunnable() {
        return this.savingRunnable;
    }
}
