package io.homo.superresolution.common.gui.screens;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.gui.ConfigScreenBuilder;
import io.homo.superresolution.common.gui.Rectangle;
import io.homo.superresolution.common.gui.ScissorsHandler;
import io.homo.superresolution.common.gui.widgets.ClothListWidget;
import io.homo.superresolution.core.utils.ColorUtil;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.EmptyEntry;
import me.shedaniel.clothconfig2.gui.widget.SearchFieldEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class ClothStyleConfigScreen extends AbstractConfigScreen {
    #if MC_VER < MC_1_21_6
    protected static final PanoramaRenderer panorama = new PanoramaRenderer(TitleScreen.CUBE_MAP);
    #else
    protected final PanoramaRenderer panorama = Minecraft.getInstance().gameRenderer.getPanorama();
    #endif
    protected final LinkedHashMap<Component, List<AbstractConfigEntry<?>>> categorizedEntries = Maps.newLinkedHashMap();
    public ClothListWidget listWidget;
    protected Button cancelButton, exitButton, saveButton;
    protected SearchFieldEntry searchFieldEntry;
    protected boolean enableSearch;
    protected double lastScroll = -1145.1145;

    @SuppressWarnings({"deprecation"})
    public ClothStyleConfigScreen(Screen parent, Component title, Map<String, ConfigCategory> categoryMap, ResourceLocation backgroundLocation) {
        super(parent, title, backgroundLocation);
        categoryMap.forEach((categoryName, category) -> {
            List<AbstractConfigEntry<?>> entries = Lists.newArrayList();
            for (Object object : category.getEntries()) {
                AbstractConfigListEntry<?> entry;
                if (object instanceof Tuple<?, ?>) {
                    entry = (AbstractConfigListEntry<?>) ((Tuple<?, ?>) object).getB();
                } else {
                    entry = (AbstractConfigListEntry<?>) object;
                }
                entry.setScreen(this);
                entries.add(entry);
            }
            categorizedEntries.put(category.getCategoryKey(), entries);
        });
    }

    public void setEnableSearch(boolean enableSearch) {
        this.enableSearch = enableSearch;
    }

    @Override
    public Map<Component, List<AbstractConfigEntry<?>>> getCategorizedEntries() {
        return this.categorizedEntries;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected void init() {
        super.init();
        this.addWidget(listWidget = new ClothListWidget(this, minecraft, width, height, 30, height - 32, getBackgroundLocation()));
        if (enableSearch) {
            this.listWidget.children().add((AbstractConfigEntry) new EmptyEntry(5));
            this.listWidget.children().add((AbstractConfigEntry) (searchFieldEntry = new SearchFieldEntry(this, listWidget)));
            this.listWidget.children().add((AbstractConfigEntry) new EmptyEntry(5));
        }
        this.categorizedEntries.forEach((category, entries) -> {
            if (!listWidget.children().isEmpty())
                this.listWidget.children().add((AbstractConfigEntry) new EmptyEntry(5));
            this.listWidget.children().add((AbstractConfigEntry) new EmptyEntry(4));
            this.listWidget.children().add((AbstractConfigEntry) new CategoryTextEntry(category, category.copy().withStyle(ChatFormatting.BOLD)));
            this.listWidget.children().add((AbstractConfigEntry) new EmptyEntry(4));
            this.listWidget.children().addAll((List) entries);
        });
        int buttonWidths = Math.min(200, (width - 50 - 12) / 4);
        addWidget(cancelButton = Button.builder(
                isEdited() ? Component.translatable("text.cloth-config.cancel_discard") : Component.translatable("gui.cancel"),
                widget -> quit()
        ).bounds(0, height - 26, buttonWidths, 20).build());
        addWidget(exitButton = new Button(
                0, height - 26, buttonWidths, 20,
                Component.empty(),
                button -> saveAll(true),
                Supplier::get
        ) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                boolean hasErrors = false;
                label:
                for (List<AbstractConfigEntry<?>> entries : categorizedEntries.values()) {
                    for (AbstractConfigEntry<?> entry : entries) {
                        if (entry.getConfigError().isPresent()) {
                            hasErrors = true;
                            break label;
                        }
                    }
                }
                active = isEdited() && !hasErrors;
                setMessage(hasErrors ? Component.translatable("text.cloth-config.error_cannot_save") : Component.translatable("text.cloth-config.save_and_done"));
                super.renderWidget(graphics, mouseX, mouseY, delta);
            }
        });
        addWidget(saveButton = new Button(
                0, height - 26, buttonWidths, 20,
                Component.translatable("superresolution.screen.button.label.apply"),
                button -> {
                    saveAll(false);
                    double scroll = listWidget.getScroll();
                    Minecraft.getInstance().setScreen(ConfigScreenBuilder.create().buildConfigScreen(parent));
                    if (Minecraft.getInstance().screen instanceof ClothStyleConfigScreen) {
                        ((ClothStyleConfigScreen) Minecraft.getInstance().screen).lastScroll = scroll;
                    }
                }, Supplier::get
        ) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
                boolean hasErrors = false;
                label:
                for (List<AbstractConfigEntry<?>> entries : categorizedEntries.values()) {
                    for (AbstractConfigEntry<?> entry : entries) {
                        if (entry.getConfigError().isPresent()) {
                            hasErrors = true;
                            break label;
                        }
                    }
                }
                active = isEdited() && !hasErrors;
                setMessage(hasErrors ? Component.translatable("text.cloth-config.error_cannot_save") : Component.translatable("superresolution.screen.button.label.apply"));
                super.renderWidget(graphics, mouseX, mouseY, delta);
            }
        });
        Optional.ofNullable(this.afterInitConsumer).ifPresent(consumer -> consumer.accept(this));
    }

    @Override
    public boolean matchesSearch(Iterator<String> tags) {
        if (searchFieldEntry == null) return true;
        return searchFieldEntry.matchesSearch(tags);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (Minecraft.getInstance().level == null) {
            #if MC_VER >= MC_1_20_6
            #if MC_VER < MC_1_21_6
               panorama.render(graphics, width, height, 1.0f, delta);
            #else
            panorama.render(graphics, width, height, true);
            #endif
            #else
            panorama.render(Minecraft.getInstance().getDeltaFrameTime(), 1.0f);
            #endif
        }
        graphics.fill(
                0, 0,
                width, listWidget.top,
                ColorUtil.color(60, 0, 0, 0)
        );

        graphics.fill(
                0, listWidget.bottom,
                width, listWidget.height,
                ColorUtil.color(60, 0, 0, 0)
        );

        if (lastScroll != -1145.1145) {
            listWidget.scrollTo(lastScroll, false);
            lastScroll = -1145.1145;
        }
        listWidget.width = width;
        listWidget.render(graphics, mouseX, mouseY, delta);
        saveButton.setX((width / 2) - (saveButton.getWidth() / 2));
        cancelButton.setX(saveButton.getX() - 3 - saveButton.getWidth());
        exitButton.setX(saveButton.getX() + 3 + saveButton.getWidth());
        cancelButton.render(graphics, mouseX, mouseY, delta);
        saveButton.render(graphics, mouseX, mouseY, delta);
        exitButton.render(graphics, mouseX, mouseY, delta);
        #if MC_VER < MC_1_21_4
        ScissorsHandler.scissor(new Rectangle(listWidget.left, listWidget.top, listWidget.width, listWidget.bottom - listWidget.top));
        #else
        graphics.enableScissor(listWidget.left, listWidget.top, listWidget.width + listWidget.left, listWidget.bottom);
        #endif
        for (AbstractConfigEntry<?> child : listWidget.children())
            child.lateRender(graphics, mouseX, mouseY, delta);
        #if MC_VER < MC_1_21_4
        ScissorsHandler.removeLastScissor();
        #else
        graphics.disableScissor();
        #endif
        graphics.drawString(font, title.getVisualOrderText(), (int) ((width) / 2f - font.width(title) / 2f), 12, -1);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    #if MC_VER > MC_1_20_1
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    #else
    public void renderBackground(@NotNull GuiGraphics guiGraphics)
    #endif {
    }

    public boolean isPauseScreen() {
        return SuperResolutionConfig.isPauseGameOnGui();
    }

    protected static class CategoryTextEntry extends AbstractConfigListEntry<Object> {
        private final Component category;
        private final Component text;

        public CategoryTextEntry(Component category, Component text) {
            super(Component.literal(UUID.randomUUID().toString()), false);
            this.category = category;
            this.text = text;
        }

        @Override
        public int getItemHeight() {
            List<FormattedCharSequence> strings = Minecraft.getInstance().font.split(text, getParent().getItemWidth());
            if (strings.isEmpty())
                return 0;
            return 4 + strings.size() * 10;
        }

        @Nullable
        public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
            return null;
        }

        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public Optional<Object> getDefaultValue() {
            return Optional.empty();
        }

        @Override
        public boolean isMouseInside(int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight) {
            return false;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
            super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
            int yy = y + 2;
            List<FormattedCharSequence> texts = Minecraft.getInstance().font.split(this.text, getParent().getItemWidth());
            for (FormattedCharSequence text : texts) {
                graphics.drawString(Minecraft.getInstance().font, text, x - 4 + entryWidth / 2 - Minecraft.getInstance().font.width(text) / 2, yy, -1);
                yy += 10;
            }
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.emptyList();
        }

        public Component getCategory() {
            return category;
        }
    }
}
