package com.portable_workstations.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.portable_workstations.Portable_workstations;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Portable_workstations.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PortableWorkstationsClient {
    public static final KeyMapping OPEN_WHEEL = new KeyMapping(
            "key.portable_workstations.open_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.portable_workstations");

    private PortableWorkstationsClient() {
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_WHEEL);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "portable_workstations_wheel", (gui, graphics, partialTick, screenWidth, screenHeight) ->
                PortableWorkstationsWheelOverlay.render(gui, graphics, partialTick, screenWidth, screenHeight));
    }

    @Mod.EventBusSubscriber(modid = Portable_workstations.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void keyInput(InputEvent.Key event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || !OPEN_WHEEL.matches(event.getKey(), event.getScanCode())) {
                return;
            }

            if (event.getAction() == GLFW.GLFW_PRESS) {
                if (minecraft.screen == null && !PortableWorkstationsClientState.entries().isEmpty()) {
                    PortableWorkstationsWheelState.open();
                }
                while (OPEN_WHEEL.consumeClick()) {
                }
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                PortableWorkstationsWheelState.close();
            }
        }

        @SubscribeEvent
        public static void mouseButton(InputEvent.MouseButton.Pre event) {
            if (!PortableWorkstationsWheelState.isOpen() || event.getAction() != GLFW.GLFW_PRESS) {
                return;
            }

            event.setCanceled(true);
            if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                return;
            }

            int hovered = PortableWorkstationsWheelState.hoveredIndex();
            if (hovered < 0 || hovered >= PortableWorkstationsClientState.entries().size()) {
                return;
            }

            PortableWorkstationsClientState.ClientEntry entry = PortableWorkstationsClientState.entries().get(hovered);
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                PortableWorkstationsNetwork.requestAction(entry.type(), false);
                PortableWorkstationsWheelState.close();
            } else {
                PortableWorkstationsNetwork.requestAction(entry.type(), true);
            }
        }
    }
}
