package com.yourname.zerog;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID, value = {Dist.CLIENT}, bus = Mod.EventBusSubscriber.Bus.MOD)
/* loaded from: zerog-1.0.0.jar:com/yourname/zerog/ModKeyBindings.class */
public class ModKeyBindings {
    public static final String CATEGORY = "key.categories.zerog";
    public static final KeyMapping ROLL_LEFT = new KeyMapping("key.zerog.roll_left", InputConstants.Type.KEYSYM, 90, CATEGORY);
    public static final KeyMapping ROLL_RIGHT = new KeyMapping("key.zerog.roll_right", InputConstants.Type.KEYSYM, 88, CATEGORY);

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ROLL_LEFT);
        event.register(ROLL_RIGHT);
    }
}
