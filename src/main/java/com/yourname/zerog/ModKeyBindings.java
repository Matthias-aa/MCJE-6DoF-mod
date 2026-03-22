package com.yourname.zerog;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static KeyMapping ROLL_LEFT;
    public static KeyMapping ROLL_RIGHT;

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener(ModKeyBindings::onKeyRegister);
    }

    private static void onKeyRegister(RegisterKeyMappingsEvent event) {
        ROLL_LEFT = new KeyMapping("Roll Left (Q)", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, "Zero G Mod");
        ROLL_RIGHT = new KeyMapping("Roll Right (E)", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, "Zero G Mod");

        event.register(ROLL_LEFT);
        event.register(ROLL_RIGHT);
    }
}