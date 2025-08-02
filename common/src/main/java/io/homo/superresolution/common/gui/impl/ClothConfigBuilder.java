package io.homo.superresolution.common.gui.impl;

import com.google.common.collect.Maps;
import io.homo.superresolution.common.gui.screens.ClothStyleConfigScreen;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
    private boolean enableSearch = true;
    #if MC_VER > MC_1_20_6
    private ResourceLocation defaultBackground = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");
    #elif MC_VER == MC_1_20_6
    private ResourceLocation defaultBackground = Screen.MENU_BACKGROUND;
    #else
    private ResourceLocation defaultBackground = Screen.BACKGROUND_LOCATION;
    #endif
    private Consumer<Screen> afterInitConsumer = (screen) -> {
    };
    private String fallbackCategory = null;
    private boolean alwaysShowTabs = false;

    public ClothConfigBuilder setEnableSearch(boolean enableSearch) {
        this.enableSearch = enableSearch;
        return this;
    }

    public void setGlobalizedExpanded(boolean globalizedExpanded) {

    }

    public boolean isAlwaysShowTabs() {
        return this.alwaysShowTabs;
    }

    public ClothConfigBuilder setAlwaysShowTabs(boolean alwaysShowTabs) {
        this.alwaysShowTabs = alwaysShowTabs;
        return this;
    }

    public ClothConfigBuilder setTransparentBackground(boolean transparentBackground) {
        this.transparentBackground = transparentBackground;
        return this;
    }

    public boolean hasTransparentBackground() {
        return this.transparentBackground;
    }

    @Override
    public void setGlobalized(boolean b) {

    }

    public ClothConfigBuilder setFallbackCategory(ConfigCategory fallbackCategory) {
        this.fallbackCategory = Objects.requireNonNull(fallbackCategory).getCategoryKey().getString();
        return this;
    }

    public Screen getParentScreen() {
        return this.parent;
    }

    public ClothConfigBuilder setParentScreen(Screen parent) {
        this.parent = parent;
        return this;
    }

    public Component getTitle() {
        return this.title;
    }

    public ClothConfigBuilder setTitle(Component title) {
        this.title = title;
        return this;
    }

    public boolean isEditable() {
        return this.editable;
    }

    public ClothConfigBuilder setEditable(boolean editable) {
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

    public ClothConfigBuilder removeCategory(Component category) {
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

    public ClothConfigBuilder removeCategoryIfExists(Component category) {
        if (this.categoryMap.containsKey(category.getString()) && Objects.equals(this.fallbackCategory, category.getString())) {
            this.fallbackCategory = null;
        }

        this.categoryMap.remove(category.getString());
        return this;
    }

    public boolean hasCategory(Component category) {
        return this.categoryMap.containsKey(category.getString());
    }

    public ClothConfigBuilder setShouldTabsSmoothScroll(boolean shouldTabsSmoothScroll) {
        this.tabsSmoothScroll = shouldTabsSmoothScroll;
        return this;
    }

    public boolean isTabsSmoothScrolling() {
        return this.tabsSmoothScroll;
    }

    public ClothConfigBuilder setShouldListSmoothScroll(boolean shouldListSmoothScroll) {
        this.listSmoothScroll = shouldListSmoothScroll;
        return this;
    }

    public boolean isListSmoothScrolling() {
        return this.listSmoothScroll;
    }

    public ClothConfigBuilder setDoesConfirmSave(boolean confirmSave) {
        this.doesConfirmSave = confirmSave;
        return this;
    }

    public boolean doesConfirmSave() {
        return this.doesConfirmSave;
    }

    public ResourceLocation getDefaultBackgroundTexture() {
        return this.defaultBackground;
    }

    public ClothConfigBuilder setDefaultBackgroundTexture(ResourceLocation texture) {
        this.defaultBackground = texture;
        return this;
    }

    public Consumer<Screen> getAfterInitConsumer() {
        return this.afterInitConsumer;
    }

    public ClothConfigBuilder setAfterInitConsumer(Consumer<Screen> afterInitConsumer) {
        this.afterInitConsumer = afterInitConsumer;
        return this;
    }

    public Screen build() {
        throw new RuntimeException();
    }

    @SuppressWarnings("unchecked")
    public Screen build(Class<? extends ClothStyleConfigScreen> clazz) {
        if (!this.categoryMap.isEmpty() && this.fallbackCategory != null) {
            ClothStyleConfigScreen screen;
            try {
                Constructor<ClothStyleConfigScreen> constructor = (Constructor<ClothStyleConfigScreen>) clazz.getDeclaredConstructor(Screen.class, Component.class, Map.class, ResourceLocation.class);
                screen = constructor.newInstance(this.parent, this.title, this.categoryMap, this.defaultBackground);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            screen.setSavingRunnable(this.savingRunnable);
            screen.setEditable(this.editable);
            screen.setFallbackCategory(this.fallbackCategory == null ? null : Component.literal(this.fallbackCategory));
            screen.setTransparentBackground(this.transparentBackground);
            screen.setAlwaysShowTabs(this.alwaysShowTabs);
            screen.setConfirmSave(this.doesConfirmSave);
            screen.setEnableSearch(enableSearch);
            screen.setAfterInitConsumer(this.afterInitConsumer);
            return screen;
        } else {
            throw new NullPointerException("There cannot be no categories or fallback category!");
        }
    }

    public Runnable getSavingRunnable() {
        return this.savingRunnable;
    }

    public ClothConfigBuilder setSavingRunnable(Runnable runnable) {
        this.savingRunnable = runnable;
        return this;
    }
}
