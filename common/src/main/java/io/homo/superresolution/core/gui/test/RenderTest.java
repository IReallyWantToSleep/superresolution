/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.core.gui.test;

import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVG;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGPaintWrapper;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.render.RenderLayer;
import io.homo.superresolution.core.utils.Color;

public class RenderTest {

    public static void drawTestPath(RenderContext context) {
        context.beginPath();
        context.strokeWidth(2f);
        context.strokeColor(Color.rgb(255, 255, 255));
        context.rect(10, 10, 100, 50);
        context.endPath(false);

        context.rect(120, 10, 100, 50, Color.rgb(255, 0, 0), true);

        context.beginPath();
        context.strokeWidth(3f);
        context.strokeColor(Color.rgb(0, 255, 0));
        context.arc(250, 35, 25, 0, (float) (Math.PI * 1.5));
        context.endPath(false);

        context.arc(350, 35, 25, Color.rgb(0, 0, 255), true);

        context.line(10, 100, 100, 150, 2f, Color.rgb(255, 255, 0));

        context.roundedRect(120, 100, 100, 50, 10, Color.rgb(0, 255, 255), false);
        context.strokeWidth(2f);

        context.roundedRectComplex(10, 180, 100, 50, 5, 15, 20, 30, Color.rgb(255, 0, 255), true);

        context.beginPath();
        context.paint(context.linearGradient(120, 180, 220, 230, Color.rgb(255, 0, 0), Color.rgb(0, 0, 255)));
        context.rect(120, 180, 100, 50);
        context.endPath(true);

        context.beginPath();
        context.paint(context.radialGradient(290, 205, 30, Color.rgb(255, 255, 0), Color.rgb(0, 255, 0)));
        context.arc(290, 205, 30);
        context.endPath(true);

        context.beginPath();
        context.paint(context.radialGradient(390, 205, 15, 35, Color.rgb(0, 255, 255), Color.rgb(255, 0, 255)));
        context.arc(390, 205, 35);
        context.endPath(true);

        context.beginPath();
        context.strokeWidth(2f);
        context.strokeColor(Color.rgb(255, 255, 255));
        context.line(10, 270, 50, 300);
        context.line(50, 300, 90, 270);
        context.line(90, 270, 10, 270);
        context.endPath(false);

        context.beginPath();
        context.fillColor(Color.hex("#FF6B6B"));
        context.strokeColor(Color.rgb(255, 255, 255));
        context.strokeWidth(3f);
        context.roundedRect(120, 270, 100, 50, 8);
        context.endPath(true);
        context.beginPath();
        context.strokeColor(Color.rgb(255, 255, 255));
        context.roundedRect(120, 270, 100, 50, 8);
        context.endPath(false);

        context.beginPath();
        context.paint(context.linearGradient(240, 280, 340, 320, Color.hex("#FFD93D"), Color.hex("#6BCB77")));
        context.rect(240, 270, 100, 50);
        context.arc(340, 295, 20);
        context.endPath(true);
    }

    public static void drawTestText(RenderContext context) {
        float startY = 20;
        float boxW = 180;
        float boxH = 40;

        drawTextWithBox(context, "Left-Top", 810, startY, boxW, boxH,
                TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP));

        drawTextWithBox(context, "Center-Middle", 810, startY + 50, boxW, boxH,
                TextAlign.of(TextAlignType.ALIGN_CENTER, TextAlignType.ALIGN_MIDDLE));

        drawTextWithBox(context, "Right-Bottom", 810, startY + 100, boxW, boxH,
                TextAlign.of(TextAlignType.ALIGN_RIGHT, TextAlignType.ALIGN_BOTTOM));

        float multiX = 20;
        float multiY = 20;
        float multiW = 600;
        float multiH = 600;

        context.line(multiX, multiY, multiX + multiW, multiY, 1f, Color.rgb(255, 0, 0)); // Top
        context.line(multiX, multiY + multiH, multiX + multiW, multiY + multiH, 1f, Color.rgb(255, 0, 0)); // Bottom
        context.line(multiX, multiY, multiX, multiY + multiH, 1f, Color.rgb(255, 0, 0)); // Left
        context.line(multiX + multiW, multiY, multiX + multiW, multiY + multiH, 1f, Color.rgb(255, 0, 100)); // Right

        String longText = """
                Attention Is All You Need
                
                The dominant sequence transduction models are based on complex recurrent or convolutional neural networks that include an encoder and a decoder. The best performing models also connect the encoder and decoder through an attention mechanism. We propose a new simple network architecture, the Transformer, based solely on attention mechanisms, dispensing with recurrence and convolutions entirely. Experiments on two machine translation tasks show these models to be superior in quality while being more parallelizable and requiring significantly less time to train. Our model achieves 28.4 BLEU on the WMT 2014 English-to-German translation task, improving over the existing best results, including ensembles, by over 2 BLEU. On the WMT 2014 English-to-French translation task, our model establishes a new single-model state-of-the-art BLEU score of 41.8 after training for 3.5 days on eight GPUs, a small fraction of the training costs of the best models from the literature. We show that the Transformer generalizes well to other tasks by applying it successfully to English constituency parsing both with large and limited training data.
                In this work, we presented the Transformer, the first sequence transduction model based entirely on attention, replacing the recurrent layers most commonly used in encoder-decoder architectures with multi-headed self-attention.
                
                For translation tasks, the Transformer can be trained significantly faster than architectures based on recurrent or convolutional layers. On both WMT 2014 English-to-German and WMT 2014 English-to-French translation tasks, we achieve a new state of the art. In the former task our best model outperforms even all previously reported ensembles.
                
                We are excited about the future of attention-based models and plan to apply them to other tasks. We plan to extend the Transformer to problems involving input and output modalities other than text and to investigate local, restricted attention mechanisms to efficiently handle large inputs and outputs such as images, audio and video. Making generation less sequential is another research goals of ours.
                
                From:https://arxiv.org/html/1706.03762v7
                """;

        context.drawAlignedText(
                context.font(),
                13,
                longText,
                multiX, multiY, multiW, 16,
                Color.hex("#FFD93D"),
                TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                true
        );
    }

    private static void drawTextWithBox(RenderContext context, String text, float x, float y, float w, float h, TextAlign align) {
        context.beginPath();
        context.strokeWidth(1f);
        context.strokeColor(Color.rgb(150, 150, 150));
        context.rect(x, y, w, h);
        context.endPath(false);

        float cx = x + w / 2;
        float cy = y + h / 2;
        context.line(cx - 5, cy, cx + 5, cy, 0.5f, Color.rgb(255, 0, 0));
        context.line(cx, cy - 5, cx, cy + 5, 0.5f, Color.rgb(255, 0, 0));

        context.line(x - 2, y, x + 2, y, 1f, Color.rgb(0, 255, 0));
        context.line(x, y - 2, x, y + 2, 1f, Color.rgb(0, 255, 0));

        context.drawAlignedText(
                context.font(),
                14,
                text,
                x, y, w, h,
                Color.rgb(255, 255, 255),
                align,
                false
        );
    }

    public static void drawTestScissor(RenderContext context) {
        float startX = 50;
        float startY = 50;

        context.drawAlignedText(context.font(), 16, "Scissor", startX, startY - 25, 200, 20, Color.rgb(0xFFFFFF), TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_BOTTOM), false);

        context.strokeColor(Color.rgb(255, 0, 0));
        context.strokeWidth(1f);
        context.beginPath();
        context.rect(startX, startY, 100, 100);
        context.endPath(false);

        context.save();
        context.scissor(startX, startY, 100, 100);

        context.beginPath();
        context.fillColor(Color.rgb(0, 255, 0));
        context.rect(startX - 20, startY - 20, 140, 140);
        context.endPath(true);

        context.resetScissor();
        context.restore();

        startX += 200;
        context.drawAlignedText(context.font(), 16, "Defer Layer Scissor", startX, startY - 25, 200, 20, Color.rgb(0xFFFFFF), TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_BOTTOM), false);

        context.strokeColor(Color.rgb(255, 0, 0));
        context.strokeWidth(1f);
        context.beginPath();
        context.rect(startX, startY, 100, 100);
        context.endPath(false);

        float finalStartX = startX;
        float finalStartY = startY;

        context.deferToLayer(RenderLayer.Content, 100, (ctx) -> {
            ctx.save();
            ctx.scissor(finalStartX, finalStartY, 100, 100);

            ctx.beginPath();
            ctx.fillColor(Color.rgb(0, 0, 255));
            ctx.rect(finalStartX - 20, finalStartY - 20, 140, 140);
            ctx.endPath(true);

            ctx.resetScissor();
            ctx.restore();
        });
    }

    public static void drawTestAlpha(RenderContext context) {
        float x = 50;
        float y = 250;
        float w = 100;
        float h = 50;
        float gap = 20;

        context.drawAlignedText(context.font(), 16, "不透明栈测试", x, y - 25, 300, 20, Color.rgb(0xFFFFFF), TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_BOTTOM), false);

        drawAlphaBox(context, x, y, w, h, "1.0");

        x += w + gap;
        context.pushAlpha(0.5f);
        drawAlphaBox(context, x, y, w, h, "0.5");

        x += w + gap;
        context.pushAlpha(0.5f);
        drawAlphaBox(context, x, y, w, h, "0.25");

        x += w + gap;
        context.popAlpha();
        drawAlphaBox(context, x, y, w, h, "Pop -> 0.5");

        x += w + gap;
        context.popAlpha();
        drawAlphaBox(context, x, y, w, h, "Pop -> 1.0");
    }

    private static void drawAlphaBox(RenderContext context, float x, float y, float w, float h, String text) {
        context.beginPath();
        context.fillColor(Color.rgb(255, 255, 255));
        context.rect(x, y, w, h);
        context.endPath(true);

        context.drawAlignedText(context.font(), 12, text, x, y + h + 5, w, 20, Color.rgb(255, 255, 255), TextAlign.of(TextAlignType.ALIGN_CENTER, TextAlignType.ALIGN_TOP), false);
    }
}
