package com.yourname.zerog;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyBindings {

    public static final String CATEGORY = "key.categories.zerog";

    /**
     * 向左滚转（左肩向下）— Q 键
     */
    public static final KeyMapping ROLL_LEFT = new KeyMapping(
            "key.zerog.roll_left",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,  // 用 Z 键避免与原版 Q（丢弃物品）冲突
            CATEGORY
    );

    /**
     * 向右滚转（右肩向下）— E 键
     */
    public static final KeyMapping ROLL_RIGHT = new KeyMapping(
            "key.zerog.roll_right",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,  // 用 X 键避免与原版 E（背包）冲突
            CATEGORY
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ROLL_LEFT);
        event.register(ROLL_RIGHT);
    }
}