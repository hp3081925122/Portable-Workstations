package com.portable_workstations.client;

import net.minecraft.client.Minecraft;

public final class PortableWorkstationsWheelState {
    public static final int SLOT_NONE = -1;
    public static final double INNER_RADIUS = 24.0D * 0.70D;

    private static boolean open;
    private static int hoveredIndex = SLOT_NONE;

    private PortableWorkstationsWheelState() {
    }

    public static boolean isOpen() {
        return open;
    }

    public static void open() {
        if (open) {
            return;
        }
        open = true;
        hoveredIndex = SLOT_NONE;
        Minecraft.getInstance().mouseHandler.releaseMouse();
    }

    public static void close() {
        if (!open) {
            return;
        }
        open = false;
        hoveredIndex = SLOT_NONE;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            minecraft.mouseHandler.grabMouse();
        }
    }

    public static int hoveredIndex() {
        return hoveredIndex;
    }

    public static void updateHoveredIndex(int screenWidth, int screenHeight) {
        int count = PortableWorkstationsClientState.entries().size();
        if (count == 0) {
            hoveredIndex = SLOT_NONE;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        double mouseX = minecraft.mouseHandler.xpos() * screenWidth / (double) minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * screenHeight / (double) minecraft.getWindow().getScreenHeight();
        double dx = mouseX - screenWidth / 2.0D;
        double dy = mouseY - screenHeight / 2.0D;
        if (dx * dx + dy * dy <= INNER_RADIUS * INNER_RADIUS) {
            hoveredIndex = SLOT_NONE;
            return;
        }

        double sectorAngle = 360.0D / count;
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90.0D + sectorAngle / 2.0D;
        angle %= 360.0D;
        if (angle < 0.0D) {
            angle += 360.0D;
        }
        hoveredIndex = Math.min(count - 1, (int) (angle / sectorAngle));
    }
}
