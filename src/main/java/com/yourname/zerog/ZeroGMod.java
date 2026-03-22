package com.yourname.zerog;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// 这里的 "zerog" 必须和下面 MOD_ID 一致
@Mod(ZeroGMod.MOD_ID)
public class ZeroGMod {
    public static final String MOD_ID = "zerog";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 这是一个简单的临时存储，代替复杂的 Capability/Attachment 系统
    // 注意：这只是为了新手教学方便，正式Mod最好用 Attachment
    public static PlayerState CLIENT_STATE = new PlayerState();

    public ZeroGMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册按键
        ModKeyBindings.register(modEventBus);

        // 注册我们自己写的事件
        MinecraftForge.EVENT_BUS.register(this);
    }
}