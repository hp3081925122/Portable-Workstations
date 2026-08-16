package com.portable_workstations.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class PortableWorkstationsClient implements ClientModInitializer {
    public static final KeyMapping OPEN_WHEEL = new KeyMapping(
            "key.portable_workstations.open_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.portable_workstations");

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(OPEN_WHEEL);
        PortableWorkstationsNetwork.registerClient();
        HudRenderCallback.EVENT.register((graphics, partialTick) ->
                PortableWorkstationsWheelOverlay.render(graphics, partialTick, graphics.guiWidth(), graphics.guiHeight()));
        ClientTickEvents.END_CLIENT_TICK.register(PortableWorkstationsClient::tick);
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            PortableWorkstationsWheelState.close();
            return;
        }

        boolean keyDown = OPEN_WHEEL.isDown();
        if (keyDown) {
            if (minecraft.screen == null && !PortableWorkstationsClientState.entries().isEmpty()) {
                PortableWorkstationsWheelState.open();
            }
        } else {
            PortableWorkstationsWheelState.close();
        }

    }
}
