/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
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

package io.homo.superresolution.common.gui.screens;

import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.common.gui.options.OptionCategory;
import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferAttachmentType;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.framebuffer.IBindableFrameBuffer;
import io.homo.superresolution.core.graphics.impl.texture.*;
import io.homo.superresolution.core.graphics.opengl.Gl;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.gui.*;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGDrawContext;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL46;

import java.util.List;

public class MaterialStyleConfigScreen extends NanoVGScreen<MaterialStyleConfigScreen> {
    protected List<OptionCategory> categories;
    protected OptionCategory currentCategory;
    protected MaterialScheme materialScheme = MaterialScheme.defaultDark;
    protected IBindableFrameBuffer uiFrameBuffer;

    protected ContainerWidget mainContainer;
    protected ContainerWidget actionContainer;
    protected ContainerWidget headerContainer;

    protected MaterialButton saveButton;
    protected MaterialButton saveAndExitButton;
    protected MaterialButton exitButton;
    protected MaterialButton resetButton;
    protected LayoutConstants constants = new LayoutConstants();

    private static class LayoutConstants {
        public float ACTIONS_HEIGHT = 64f;
        public float HEADER_HEIGHT = 64f;
        public float PADDING = 72f;
        public float TOP_PADDING = 24f;
        public float BOTTOM_PADDING = 24f;
        public float OPTIONS_SCROLLBAR_WIDTH = 8f;
        public float OPTIONS_HORIZONTAL_PADDING = 6f;
        public float HEADER_BUTTON_GAP = 8f;
    }

    public Screen getParent() {
        return parent;
    }

    public MaterialStyleConfigScreen setParent(Screen parent) {
        this.parent = parent;
        return this;
    }

    protected Screen parent;

    public MaterialStyleConfigScreen(
            Text title,
            List<OptionCategory> categories
    ) {
        super(Component.literal(title.toString()));
        this.categories = categories;
        currentCategory = categories.get(0);
        //initWidgets();
    }

    @Override
    protected void buildWidgets() {
    }

    /*
        protected void initWidgets() {
            mainContainer = new ContainerWidget();
            mainContainer.layout(new AbsoluteLayout());
            rebuildWidget();
            updateLayout();
            addWidget(mainContainer);
        }

        protected void rebuildWidget() {
            removeExistingContainers();
            createHeaderContainer();
            createActionContainer();
            createOptionsContainer();
            mainContainer.addChild(headerContainer);
            mainContainer.addChild(actionContainer);
            mainContainer.addChild(optionsContainer);
        }

        private void removeExistingContainers() {
            if (optionsContainer != null) {
                mainContainer.removeChild(optionsContainer);
                mainContainer.getLayout().removeElement(optionsContainer);
            }
            if (actionContainer != null) {
                mainContainer.removeChild(actionContainer);
                mainContainer.getLayout().removeElement(actionContainer);
            }
            if (headerContainer != null) {
                mainContainer.removeChild(headerContainer);
                mainContainer.getLayout().removeElement(headerContainer);
            }
        }

        private void createHeaderContainer() {
            headerContainer = new ContainerWidget();
        }

        private void createActionContainer() {
            actionContainer = new ContainerWidget();
            LinearLayout linearLayout = new LinearLayout();
            actionContainer.layout(linearLayout);
            linearLayout.setHorizontalGap(4);

            saveButton = createButton("保存", MaterialSymbols.iconSave(), MaterialButtonVariant.Filled);
            saveAndExitButton = createButton("保存并退出", MaterialSymbols.iconDoneAll(), MaterialButtonVariant.Filled);
            exitButton = createButton("退出", MaterialSymbols.iconExitToApp(), MaterialButtonVariant.Filled);
            resetButton = createButton("重置所有选项", MaterialSymbols.iconResetSettings(), MaterialButtonVariant.Elevated);

            actionContainer.addChild(saveButton);
            actionContainer.addChild(saveAndExitButton);
            actionContainer.addChild(exitButton);
            actionContainer.addChild(resetButton);

            linearLayout.setElementPosition(saveButton, 0, LinearLayout.HorizontalAlignment.CENTER, LinearLayout.VerticalAlignment.CENTER);
            linearLayout.setElementPosition(saveAndExitButton, 1, LinearLayout.HorizontalAlignment.CENTER, LinearLayout.VerticalAlignment.CENTER);
            linearLayout.setElementPosition(exitButton, 2, LinearLayout.HorizontalAlignment.CENTER, LinearLayout.VerticalAlignment.CENTER);
            linearLayout.setElementPosition(resetButton, 3, LinearLayout.HorizontalAlignment.CENTER, LinearLayout.VerticalAlignment.CENTER);
        }

        private MaterialButton createButton(String text, MaterialSymbol icon, MaterialButtonVariant variant) {
            MaterialButton button = MaterialButton.create(MaterialButtonSize.Small);
            button.style().variant(variant);
            button.text(text);
            if (icon != null) {
                button.icon(icon);
            }
            return button;
        }

        private void createOptionsContainer() {
            optionsContainer = new MaterialScrollableContainerWidget();
            optionsContainer.scheme(materialScheme);
            optionsContainer.setHorizontalScrollEnabled(false);
            optionsContainer.setVerticalScrollEnabled(true);
            optionsContainer.setTopPadding(constants.TOP_PADDING);
            optionsContainer.setBottomPadding(constants.BOTTOM_PADDING);
            AbsoluteLayout absoluteLayout = new AbsoluteLayout();
            optionsContainer.layout(absoluteLayout);

            currentCategory.getEntries().forEach(entry -> {
                optionsContainer.addChild(entry.getContainer());
                entry.getContainer().scheme(materialScheme);
            });
        }

        protected void updateLayout() {
            AbsoluteLayout absoluteLayout = (AbsoluteLayout) mainContainer.getLayout();

            Vector2f screenSize = getScreenDimensions();
            mainContainer.setBounds(new Rectangle(0, 0, screenSize.x, screenSize.y));

            Rectangle headerRegion = calculateHeaderRegion(screenSize);
            Rectangle actionsRegion = calculateActionsRegion(screenSize);
            Rectangle optionsRegion = calculateOptionsRegion(screenSize, headerRegion, actionsRegion);

            headerContainer.setBounds(
                    headerRegion.x,
                    headerRegion.y,
                    headerRegion.width,
                    headerRegion.height
            );
            actionContainer.setBounds(
                    actionsRegion.x,
                    actionsRegion.y,
                    actionsRegion.width,
                    actionsRegion.height
            );
            optionsContainer.setRightPadding(constants.PADDING);
            optionsContainer.setBounds(
                    optionsRegion.x,
                    optionsRegion.y,
                    optionsRegion.width + constants.PADDING,
                    optionsRegion.height
            );
            absoluteLayout.setPosition(headerContainer, headerRegion.getPosition());
            absoluteLayout.setPosition(actionContainer, actionsRegion.getPosition());
            absoluteLayout.setPosition(optionsContainer, optionsRegion.getPosition());

            ((LinearLayout) actionContainer.getLayout()).setLayoutBounds(
                    new Rectangle(0, 0, actionsRegion.width, actionsRegion.height)
            );
            updateOptionsEntriesLayout(optionsRegion);
            optionsContainer.setViewRegion(new Vector2f(
                    optionsRegion.width + constants.PADDING,
                    optionsRegion.height + constants.OPTIONS_SCROLLBAR_WIDTH
            ));
        }

        private Vector2f getScreenDimensions() {
            float width = MinecraftWindow.getWindowWidth() / NanoVG.context.globalScale();
            float height = MinecraftWindow.getWindowHeight() / NanoVG.context.globalScale();
            return new Vector2f(width, height);
        }

        private Rectangle calculateHeaderRegion(Vector2f screenSize) {
            return new Rectangle(
                    constants.PADDING,
                    0,
                    screenSize.x - constants.PADDING,
                    constants.HEADER_HEIGHT
            );
        }

        private Rectangle calculateActionsRegion(Vector2f screenSize) {
            return new Rectangle(
                    0,
                    screenSize.y - constants.ACTIONS_HEIGHT,
                    screenSize.x,
                    constants.ACTIONS_HEIGHT
            );
        }

        private Rectangle calculateOptionsRegion(Vector2f screenSize, Rectangle headerRegion, Rectangle actionsRegion) {
            return new Rectangle(
                    constants.PADDING,
                    headerRegion.y + headerRegion.height,
                    screenSize.x - 2 * constants.PADDING,
                    screenSize.y - headerRegion.height - actionsRegion.height - constants.BOTTOM_PADDING
            );
        }

        private void updateOptionsEntriesLayout(Rectangle optionsRegion) {
            AbsoluteLayout absoluteLayout = (AbsoluteLayout) optionsContainer.getWrappedLayout();
            float currentY = 0;
            float entryWidth = optionsRegion.width - constants.OPTIONS_SCROLLBAR_WIDTH - constants.OPTIONS_HORIZONTAL_PADDING;

            for (AbstractOptionEntry<?, ?, ?> entry : currentCategory.getEntries()) {
                entry.getContainer().setBounds(0, 0, entryWidth, entry.getEntryHeight());
                if (entry.getContainer().getLayout() instanceof LinearLayout layout) {
                    layout.setLayoutBounds(new Rectangle(0, 0, entryWidth, entry.getEntryHeight()));
                }
                absoluteLayout.setPosition(entry.getContainer(), new Vector2f(0, currentY));
                currentY += entry.getEntryHeight();
            }
        }
    */
    @Override
    public void drawBefore(IUIDrawContext drawContext, UIInputState inputState) {
        super.drawBefore(drawContext, inputState);
        Vector2f screenSize = MinecraftWindow.getWindowSize();
        drawContext.beginBatch();

        drawContext.rect(0, 0, screenSize.x, screenSize.y, materialScheme.background(), true);

        drawHeaderBackground(drawContext, screenSize);

        drawActionsBackground(drawContext, screenSize);

        drawContext.endBatch(0);
        //updateLayout();
    }

    private void drawHeaderBackground(IUIDrawContext drawContext, Vector2f screenSize) {
        drawContext.beginPath();
        drawContext.fillColor(materialScheme.surfaceContainer());
        drawContext.roundedRectComplex(
                0,
                0,
                screenSize.x,
                constants.HEADER_HEIGHT,
                24, 24, 0, 0
        );
        drawContext.endPath();
    }

    private void drawActionsBackground(IUIDrawContext drawContext, Vector2f screenSize) {
        drawContext.beginPath();
        drawContext.fillColor(materialScheme.surfaceContainer());
        drawContext.roundedRectComplex(
                0,
                screenSize.y - constants.ACTIONS_HEIGHT,
                screenSize.x,
                constants.ACTIONS_HEIGHT,
                0, 0, 24, 24
        );
        drawContext.endPath();
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        //updateLayout();
        initUIFrameBuffer();
    }

    protected void initUIFrameBuffer() {
        if (uiFrameBuffer != null) {
            uiFrameBuffer.getTexture(FrameBufferAttachmentType.Color).destroy();
            uiFrameBuffer.getTexture(FrameBufferAttachmentType.AnyDepth).destroy();
            uiFrameBuffer.destroy();
        }
        uiFrameBuffer = GlFrameBuffer.create(
                RenderSystems.current().device().createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .usages(TextureUsages.create().sampler().storage().attachmentColor())
                                .filterMode(TextureFilterMode.Linear)
                                .wrapMode(TextureWrapMode.ClampToEdge)
                                .format(TextureFormat.R11G11B10F)
                                .size(MinecraftWindow.getWindowWidth(), MinecraftWindow.getWindowHeight())
                                .build()
                ),
                RenderSystems.current().device().createTexture(
                        TextureDescription.create()
                                .type(TextureType.Texture2D)
                                .usages(TextureUsages.create().sampler().storage().attachmentColor())
                                .filterMode(TextureFilterMode.Linear)
                                .wrapMode(TextureWrapMode.ClampToEdge)
                                .format(TextureFormat.DEPTH24_STENCIL8)
                                .size(MinecraftWindow.getWindowWidth(), MinecraftWindow.getWindowHeight())
                                .build()
                )
        );
    }

    @Override
    protected void init() {
        //updateLayout();
        initUIFrameBuffer();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX_, int mouseY_, float partialTick) {
        float mouseX = (float) (Minecraft.getInstance().getWindow().getGuiScale() * mouseX_);
        float mouseY = (float) (Minecraft.getInstance().getWindow().getGuiScale() * mouseY_);
        drawBefore(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
        uiFrameBuffer.bind(FrameBufferBindPoint.Write);
        nvg.begin(true);
        nvg.resetGlobalTransform();
        nvg.resetTransform();
        nvg.globalAlpha(1.0f);
        NanoVGDrawContext drawContext = new NanoVGDrawContext(nvg);
        draw(drawContext, new UIInputState(
                new Vector2f(mouseX, mouseY),
                partialTick
        ));
        drawContext.drawAll();
        nvg.end();
        uiFrameBuffer.unbind(FrameBufferBindPoint.Write);

        RenderHandlerManager.getOriginRenderTarget().bind(FrameBufferBindPoint.All);
        Gl.DSA.blitFramebuffer(
                (int) uiFrameBuffer.handle(),
                (int) RenderHandlerManager.getOriginRenderTarget().handle(),
                0, 0, RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight(),
                0, 0, RenderHandlerManager.getScreenWidth(), RenderHandlerManager.getScreenHeight(),
                GL46.GL_COLOR_BUFFER_BIT,
                GL46.GL_LINEAR
        );
        drawAfter(guiGraphics, (int) mouseX, (int) mouseY, partialTick);
    }
}