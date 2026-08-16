package com.portable_workstations.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;

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

    public static void render(GuiGraphicsExtractor graphics) {
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
        graphics.submitGuiElementRenderState(new WheelRenderState(centerX, centerY, entries.size(), hovered));
        for (int index = 0; index < entries.size(); index++) {
            drawItem(graphics, centerX, centerY, index, entries.size(), entries.get(index).stack(), font);
        }
        if (hovered >= 0 && hovered < entries.size()) {
            drawSelectedName(graphics, centerX, centerY, entries.get(hovered).stack().getHoverName(), font);
        }
        drawControls(graphics, centerX, centerY, screenWidth, screenHeight, font);
    }

    private static void drawItem(GuiGraphicsExtractor graphics, int centerX, int centerY, int index, int count, ItemStack stack, Font font) {
        double sectorAngle = 360.0D / count;
        double middleAngle = -90.0D + index * sectorAngle;
        double middleRadians = Math.toRadians(middleAngle);
        int iconCenterX = centerX + (int) (ICON_RADIUS * Math.cos(middleRadians));
        int iconCenterY = centerY + (int) (ICON_RADIUS * Math.sin(middleRadians));
        int iconX = iconCenterX - 8;
        int iconY = iconCenterY - 8;
        if (stack.isEmpty()) {
            graphics.fill(iconX + 6, iconY + 6, iconX + 10, iconY + 10, 0x5564748B);
        } else {
            graphics.item(stack, iconX, iconY);
            graphics.itemDecorations(font, stack, iconX, iconY);
        }
    }

    private static void drawSelectedName(GuiGraphicsExtractor graphics, int centerX, int centerY, Component name, Font font) {
        int width = font.width(name) + 16;
        int height = font.lineHeight + 8;
        int x = centerX - width / 2;
        int y = centerY - (int) OUTER_RADIUS - height - 10;
        graphics.fill(x, y, x + width, y + height, 0xCC0A1628);
        drawBorder(graphics, x, y, width, height, 0x8838BDF8);
        graphics.centeredText(font, name, centerX, y + 4, PRIMARY_TEXT_COLOR);
    }

    private static void drawControls(GuiGraphicsExtractor graphics, int centerX, int centerY, int screenWidth, int screenHeight, Font font) {
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
        graphics.centeredText(font, controls, centerX, y + 4, SECONDARY_TEXT_COLOR);
    }

    private static final class WheelRenderState implements GuiElementRenderState {
        private final int centerX;
        private final int centerY;
        private final int count;
        private final int hovered;
        private final ScreenRectangle bounds;
        private final Matrix3x2f pose = new Matrix3x2f();

        private WheelRenderState(int centerX, int centerY, int count, int hovered) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.count = count;
            this.hovered = hovered;
            int radius = (int) Math.ceil(GLOW_RADIUS + 2.0D);
            this.bounds = new ScreenRectangle(centerX - radius, centerY - radius, radius * 2, radius * 2);
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            addAnnularSector(consumer, OUTER_RADIUS, OUTER_RADIUS + 2.0D, 0.0D, 360.0D,
                    OUTER_RING_COLOR, OUTER_RING_COLOR, 128);
            double sectorAngle = 360.0D / count;
            for (int index = 0; index < count; index++) {
                double middleAngle = -90.0D + index * sectorAngle;
                double gapAngle = count == 1 ? 0.0D : Math.min(1.2D, sectorAngle * 0.08D);
                double startAngle = middleAngle - sectorAngle / 2.0D + gapAngle / 2.0D;
                double endAngle = middleAngle + sectorAngle / 2.0D - gapAngle / 2.0D;
                if (index == hovered) {
                    addAnnularSector(consumer, OUTER_RADIUS, GLOW_RADIUS, startAngle, endAngle,
                            HOVER_GLOW_COLOR, HOVER_GLOW_COLOR, 40);
                }
                boolean hasItem = !PortableWorkstationsClientState.entries().get(index).stack().isEmpty();
                int innerColor = index == hovered ? HOVER_INNER_COLOR : (hasItem ? ITEM_INNER_COLOR : EMPTY_INNER_COLOR);
                int outerColor = index == hovered ? HOVER_OUTER_COLOR : (hasItem ? ITEM_OUTER_COLOR : EMPTY_OUTER_COLOR);
                int borderColor = index == hovered ? HOVER_BORDER_COLOR : (hasItem ? ITEM_BORDER_COLOR : EMPTY_BORDER_COLOR);
                addAnnularSector(consumer, INNER_RADIUS, OUTER_RADIUS, startAngle, endAngle, innerColor, outerColor, 48);
                addAnnularSector(consumer, OUTER_RADIUS - 1.0D, OUTER_RADIUS + 1.0D, startAngle, endAngle,
                        borderColor, borderColor, 48);
                addAnnularSector(consumer, INNER_RADIUS - 1.0D, INNER_RADIUS + 1.0D, startAngle, endAngle,
                        borderColor, borderColor, 48);
            }
            addAnnularSector(consumer, 0.0D, INNER_RADIUS - 2.0D, 0.0D, 360.0D, CENTER_COLOR, CENTER_COLOR, 80);
            addAnnularSector(consumer, INNER_RADIUS - 3.0D, INNER_RADIUS - 1.0D, 0.0D, 360.0D,
                    CENTER_INNER_RING_COLOR, CENTER_INNER_RING_COLOR, 80);
            addAnnularSector(consumer, INNER_RADIUS - 1.5D, INNER_RADIUS + 0.5D, 0.0D, 360.0D,
                    CENTER_OUTER_RING_COLOR, CENTER_OUTER_RING_COLOR, 80);
            int arm = (int) INNER_RADIUS - 6;
            addRectangle(consumer, centerX - arm, centerY, centerX + arm, centerY + 1, 0x3338BDF8);
            addRectangle(consumer, centerX, centerY - arm, centerX + 1, centerY + arm, 0x3338BDF8);
            addRectangle(consumer, centerX - 1, centerY - 1, centerX + 2, centerY + 2, CENTER_OUTER_RING_COLOR);
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }

        @Override
        public ScreenRectangle scissorArea() {
            return null;
        }

        @Override
        public ScreenRectangle bounds() {
            return bounds;
        }

        private void addAnnularSector(VertexConsumer consumer, double innerRadius, double outerRadius,
                                      double startAngle, double endAngle, int innerColor, int outerColor, int segments) {
            double startRadians = Math.toRadians(startAngle);
            double angleStep = Math.toRadians(endAngle - startAngle) / segments;
            for (int index = 0; index < segments; index++) {
                double angle0 = startRadians + angleStep * index;
                double angle1 = angle0 + angleStep;
                // 按 26.1.2 GUI 管线的正面顺序提交顶点，避免扇区被背面剔除。
                addVertex(consumer, innerRadius, angle0, innerColor);
                addVertex(consumer, innerRadius, angle1, innerColor);
                addVertex(consumer, outerRadius, angle1, outerColor);
                addVertex(consumer, outerRadius, angle0, outerColor);
            }
        }

        private void addVertex(VertexConsumer consumer, double radius, double angle, int color) {
            consumer.addVertexWith2DPose(pose,
                    (float) (centerX + radius * Math.cos(angle)),
                    (float) (centerY + radius * Math.sin(angle)))
                    .setColor(color);
        }

        private void addRectangle(VertexConsumer consumer, int x0, int y0, int x1, int y1, int color) {
            consumer.addVertexWith2DPose(pose, x0, y0).setColor(color);
            consumer.addVertexWith2DPose(pose, x0, y1).setColor(color);
            consumer.addVertexWith2DPose(pose, x1, y1).setColor(color);
            consumer.addVertexWith2DPose(pose, x1, y0).setColor(color);
        }
    }

    private static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
