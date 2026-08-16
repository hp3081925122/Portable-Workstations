package com.portable_workstations.mixin.client;

import com.portable_workstations.client.PortableWorkstationsClientState;
import com.portable_workstations.client.PortableWorkstationsWheelState;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class PortableWorkstationsMouseMixin {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void portableWorkstations$handleWheelClick(long window, int button, int action, int modifiers, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getInstance();
        if (window != minecraft.getWindow().getWindow()
                || !PortableWorkstationsWheelState.isOpen()
                || action != GLFW.GLFW_PRESS
                || (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            return;
        }

        // 在原版处理鼠标事件前拦截轮盘点击，避免被普通攻击或使用逻辑消费。
        callbackInfo.cancel();
        PortableWorkstationsWheelState.updateHoveredIndex(
                minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        int hovered = PortableWorkstationsWheelState.hoveredIndex();
        if (hovered < 0 || hovered >= PortableWorkstationsClientState.entries().size()) {
            return;
        }

        // 使用事件时刻的扇区索引发送动作，确保显示图标和服务端打开的工作台一致。
        PortableWorkstationsClientState.ClientEntry entry = PortableWorkstationsClientState.entries().get(hovered);
        boolean retrieve = button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        PortableWorkstationsNetwork.requestAction(entry.type(), retrieve);
        if (!retrieve) {
            PortableWorkstationsWheelState.close();
        }
    }
}
