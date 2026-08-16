package com.portable_workstations.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.portable_workstations.Portable_workstations;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Portable_workstations.MODID, value = Dist.CLIENT)
public final class PortableWorkstationsClient {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Portable_workstations.location("portable_workstations"));
    public static final KeyMapping OPEN_WHEEL = new KeyMapping(
            "key.portable_workstations.open_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    private PortableWorkstationsClient() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_WHEEL);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Portable_workstations.location("portable_workstations_wheel"),
                (graphics, deltaTracker) -> PortableWorkstationsWheelOverlay.render(graphics));
    }

    @EventBusSubscriber(modid = Portable_workstations.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void keyInput(InputEvent.Key event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.level == null || !OPEN_WHEEL.matches(event.getKeyEvent())) {
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
