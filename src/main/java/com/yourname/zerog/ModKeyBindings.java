package com.yourname.zerog;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String CATEGORY = "key.category.zerog";

    public static KeyMapping ROLL_LEFT;
    public static KeyMapping ROLL_RIGHT;

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener(ModKeyBindings::onKeyRegister);
    }

    private static void onKeyRegister(RegisterKeyMappingsEvent event) {
        ROLL_LEFT = new KeyMapping("key.zerog.roll_left", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, CATEGORY);
        ROLL_RIGHT = new KeyMapping("key.zerog.roll_right", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, CATEGORY);

        event.register(ROLL_LEFT);
        event.register(ROLL_RIGHT);
    }
}
