package com.portable_workstations.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.List;

public final class PortableWorkstationsWheelOverlay {
    private static final int OVERLAY_COLOR = 0x88000000;
    private static final int EMPTY_INNER_COLOR = 0xDD0A1628;
    private static final int EMPTY_OUTER_COLOR = 0xCC172035;
    private static final int EMPTY_BORDER_COLOR = 0x3394A3B8;
    private static final int ITEM_INNER_COLOR = 0xDD0B1A30;
    private static final int ITEM_OUTER_COLOR = 0xCC1A2E4A;
    private static final int ITEM_BORDER_COLOR = 0x6638BDF8;
    private static final int HOVER_INNER_COLOR = 0xAA00B4D4;
    private static final int HOVER_OUTER_COLOR = 0x6600BFFF;
    private static final int HOVER_BORDER_COLOR = 0xFF22D3EE;
    private static final int HOVER_GLOW_COLOR = 0x3300BFFF;
    private static final int CENTER_COLOR = 0xFF0C1824;
    private static final int CENTER_INNER_RING_COLOR = 0xFF1E3A52;
    private static final int CENTER_OUTER_RING_COLOR = 0x8838BDF8;
    private static final int OUTER_RING_COLOR = 0x5538BDF8;
    private static final int PRIMARY_TEXT_COLOR = 0xFFE2E8F0;
    private static final int SECONDARY_TEXT_COLOR = 0xFF94A3B8;
    private static final int CONTROL_BACKGROUND_COLOR = 0xCC0A1628;
    private static final int CONTROL_BORDER_COLOR = 0x8838BDF8;

    private static final double SIZE_SCALE = 0.70D;
    private static final double INNER_RADIUS = PortableWorkstationsWheelState.INNER_RADIUS;
    private static final double OUTER_RADIUS = 108.0D * SIZE_SCALE;
    private static final double GLOW_RADIUS = 115.0D * SIZE_SCALE;
    private static final double ICON_RADIUS = 62.0D * SIZE_SCALE;

    private PortableWorkstationsWheelOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        if (!PortableWorkstationsWheelState.isOpen()) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.screen != null) {
            PortableWorkstationsWheelState.close();
            return;
        }

        List<PortableWorkstationsClientState.ClientEntry> entries = PortableWorkstationsClientState.entries();
        if (entries.isEmpty()) {
            PortableWorkstationsWheelState.close();
            return;
        }

        PortableWorkstationsWheelState.updateHoveredIndex(screenWidth, screenHeight);
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int hovered = PortableWorkstationsWheelState.hoveredIndex();
        Font font = minecraft.font;

        graphics.fill(0, 0, screenWidth, screenHeight, OVERLAY_COLOR);
        drawRingOutline(graphics, centerX, centerY, OUTER_RADIUS, OUTER_RADIUS + 2.0D, OUTER_RING_COLOR, 64);

        for (int index = 0; index < entries.size(); index++) {
            drawSector(graphics, centerX, centerY, index, entries.size(), index == hovered, entries.get(index).stack(), font);
        }

        drawCenter(graphics, centerX, centerY);
        if (hovered >= 0 && hovered < entries.size()) {
            drawSelectedName(graphics, centerX, centerY, entries.get(hovered).stack().getHoverName(), font);
        }
        drawControls(graphics, centerX, centerY, screenWidth, screenHeight, font);
    }

    private static void drawSector(GuiGraphics graphics, int centerX, int centerY, int index, int count, boolean hovered, ItemStack stack, Font font) {
        double sectorAngle = 360.0D / count;
        double middleAngle = -90.0D + index * sectorAngle;
        double gapAngle = count == 1 ? 0.0D : Math.min(1.2D, sectorAngle * 0.08D);
        double startAngle = middleAngle - sectorAngle / 2.0D + gapAngle / 2.0D;
        double endAngle = middleAngle + sectorAngle / 2.0D - gapAngle / 2.0D;
        if (hovered) {
            drawAnnularSector(graphics, centerX, centerY, OUTER_RADIUS, GLOW_RADIUS, startAngle, endAngle,
                    HOVER_GLOW_COLOR, HOVER_GLOW_COLOR, 0, 20);
        }

        int innerColor = hovered ? HOVER_INNER_COLOR : (stack.isEmpty() ? EMPTY_INNER_COLOR : ITEM_INNER_COLOR);
        int outerColor = hovered ? HOVER_OUTER_COLOR : (stack.isEmpty() ? EMPTY_OUTER_COLOR : ITEM_OUTER_COLOR);
        int borderColor = hovered ? HOVER_BORDER_COLOR : (stack.isEmpty() ? EMPTY_BORDER_COLOR : ITEM_BORDER_COLOR);
        drawAnnularSector(graphics, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, startAngle, endAngle,
                innerColor, outerColor, borderColor, 24);

        double middleRadians = Math.toRadians(middleAngle);
        int iconCenterX = centerX + (int) (ICON_RADIUS * Math.cos(middleRadians));
        int iconCenterY = centerY + (int) (ICON_RADIUS * Math.sin(middleRadians));
        int iconX = iconCenterX - 8;
        int iconY = iconCenterY - 8;
        if (stack.isEmpty()) {
            graphics.fill(iconX + 6, iconY + 6, iconX + 10, iconY + 10, 0x5564748B);
        } else {
            graphics.renderItem(stack, iconX, iconY);
            graphics.renderItemDecorations(font, stack, iconX, iconY);
        }
    }

    private static void drawSelectedName(GuiGraphics graphics, int centerX, int centerY, Component name, Font font) {
        int width = font.width(name) + 16;
        int height = font.lineHeight + 8;
        int x = centerX - width / 2;
        int y = centerY - (int) OUTER_RADIUS - height - 10;
        graphics.fill(x, y, x + width, y + height, 0xCC0A1628);
        drawBorder(graphics, x, y, width, height, 0x8838BDF8);
        graphics.drawCenteredString(font, name, centerX, y + 4, PRIMARY_TEXT_COLOR);
    }

    private static void drawControls(GuiGraphics graphics, int centerX, int centerY, int screenWidth, int screenHeight, Font font) {
        Component controls = Component.translatable("overlay.portable_workstations.controls");
        int width = font.width(controls) + 16;
        int height = font.lineHeight + 8;
        int x = Math.max(4, Math.min(screenWidth - width - 4, centerX - width / 2));
        int y = centerY + (int) GLOW_RADIUS + 16;
        if (y + height > screenHeight - 26) {
            y = screenHeight - height - 26;
        }
        graphics.fill(x, y, x + width, y + height, CONTROL_BACKGROUND_COLOR);
        drawBorder(graphics, x, y, width, height, CONTROL_BORDER_COLOR);
        graphics.drawCenteredString(font, controls, centerX, y + 4, SECONDARY_TEXT_COLOR);
    }

    private static void drawCenter(GuiGraphics graphics, int centerX, int centerY) {
        drawFilledCircle(graphics, centerX, centerY, INNER_RADIUS - 2.0D, CENTER_COLOR, 40);
        drawCircleOutline(graphics, centerX, centerY, INNER_RADIUS - 2.0D, CENTER_INNER_RING_COLOR, 40);
        drawCircleOutline(graphics, centerX, centerY, INNER_RADIUS - 0.5D, CENTER_OUTER_RING_COLOR, 40);
        int arm = (int) INNER_RADIUS - 6;
        graphics.fill(centerX - arm, centerY, centerX + arm, centerY + 1, 0x3338BDF8);
        graphics.fill(centerX, centerY - arm, centerX + 1, centerY + arm, 0x3338BDF8);
        graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, CENTER_OUTER_RING_COLOR);
    }

    private static void drawAnnularSector(GuiGraphics graphics, int centerX, int centerY, double innerRadius, double outerRadius,
                                          double startAngle, double endAngle, int innerColor, int outerColor, int borderColor, int segments) {
        Matrix4f matrix = graphics.pose().last().pose();
        float innerAlpha = ((innerColor >>> 24) & 255) / 255.0F;
        float innerRed = ((innerColor >>> 16) & 255) / 255.0F;
        float innerGreen = ((innerColor >>> 8) & 255) / 255.0F;
        float innerBlue = (innerColor & 255) / 255.0F;
        float outerAlpha = ((outerColor >>> 24) & 255) / 255.0F;
        float outerRed = ((outerColor >>> 16) & 255) / 255.0F;
        float outerGreen = ((outerColor >>> 8) & 255) / 255.0F;
        float outerBlue = (outerColor & 255) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        double startRadians = Math.toRadians(startAngle);
        double endRadians = Math.toRadians(endAngle);
        for (int index = 0; index <= segments; index++) {
            double angle = startRadians + (endRadians - startRadians) * index / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            builder.addVertex(matrix, centerX + (float) outerRadius * cos, centerY + (float) outerRadius * sin, 0.0F)
                    .setColor(outerRed, outerGreen, outerBlue, outerAlpha);
            builder.addVertex(matrix, centerX + (float) innerRadius * cos, centerY + (float) innerRadius * sin, 0.0F)
                    .setColor(innerRed, innerGreen, innerBlue, innerAlpha);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());

        if (borderColor != 0) {
            float alpha = ((borderColor >>> 24) & 255) / 255.0F;
            float red = ((borderColor >>> 16) & 255) / 255.0F;
            float green = ((borderColor >>> 8) & 255) / 255.0F;
            float blue = (borderColor & 255) / 255.0F;
            builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            for (int index = 0; index <= segments; index++) {
                double angle = startRadians + (endRadians - startRadians) * index / segments;
                builder.addVertex(matrix, centerX + (float) outerRadius * (float) Math.cos(angle), centerY + (float) outerRadius * (float) Math.sin(angle), 0.0F)
                        .setColor(red, green, blue, alpha);
            }
            for (int index = segments; index >= 0; index--) {
                double angle = startRadians + (endRadians - startRadians) * index / segments;
                builder.addVertex(matrix, centerX + (float) innerRadius * (float) Math.cos(angle), centerY + (float) innerRadius * (float) Math.sin(angle), 0.0F)
                        .setColor(red, green, blue, alpha);
            }
            BufferUploader.drawWithShader(builder.buildOrThrow());
        }
        RenderSystem.disableBlend();
    }

    private static void drawRingOutline(GuiGraphics graphics, int centerX, int centerY, double innerRadius, double outerRadius, int color, int segments) {
        Matrix4f matrix = graphics.pose().last().pose();
        float alpha = ((color >>> 24) & 255) / 255.0F;
        float red = ((color >>> 16) & 255) / 255.0F;
        float green = ((color >>> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int index = 0; index <= segments; index++) {
            double angle = Math.toRadians(index * 360.0D / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            builder.addVertex(matrix, centerX + (float) outerRadius * cos, centerY + (float) outerRadius * sin, 0.0F)
                    .setColor(red, green, blue, alpha);
            builder.addVertex(matrix, centerX + (float) innerRadius * cos, centerY + (float) innerRadius * sin, 0.0F)
                    .setColor(red, green, blue, alpha);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void drawFilledCircle(GuiGraphics graphics, int centerX, int centerY, double radius, int color, int segments) {
        Matrix4f matrix = graphics.pose().last().pose();
        float alpha = ((color >>> 24) & 255) / 255.0F;
        float red = ((color >>> 16) & 255) / 255.0F;
        float green = ((color >>> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(matrix, centerX, centerY, 0.0F).setColor(red, green, blue, alpha);
        for (int index = 0; index <= segments; index++) {
            double angle = Math.toRadians(index * 360.0D / segments);
            builder.addVertex(matrix, centerX + (float) (radius * Math.cos(angle)), centerY + (float) (radius * Math.sin(angle)), 0.0F)
                    .setColor(red, green, blue, alpha);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void drawCircleOutline(GuiGraphics graphics, int centerX, int centerY, double radius, int color, int segments) {
        Matrix4f matrix = graphics.pose().last().pose();
        float alpha = ((color >>> 24) & 255) / 255.0F;
        float red = ((color >>> 16) & 255) / 255.0F;
        float green = ((color >>> 8) & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int index = 0; index <= segments; index++) {
            double angle = Math.toRadians(index * 360.0D / segments);
            builder.addVertex(matrix, centerX + (float) (radius * Math.cos(angle)), centerY + (float) (radius * Math.sin(angle)), 0.0F)
                    .setColor(red, green, blue, alpha);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
