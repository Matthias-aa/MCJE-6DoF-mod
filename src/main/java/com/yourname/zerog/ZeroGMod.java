package com.yourname.zerog;

import com.yourname.zerog.capability.ZeroGStateProvider;
import com.yourname.zerog.command.ZeroGCommand;
import com.yourname.zerog.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ZeroGMod.MOD_ID)
public class ZeroGMod {
    public static final String MOD_ID = "zerog";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final PlayerState CLIENT_STATE = new PlayerState();

    public ZeroGMod() {
        // 修复弃用警告：使用 ModLoadingContext
        ModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
        ModNetwork.register();
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("ZeroG Mod initialized!");
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            // 使用推荐的 ResourceLocation 构造方式
            event.addCapability(new ResourceLocation(MOD_ID, "zerog_state"), new ZeroGStateProvider());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ZeroGCommand.register(event.getDispatcher());
    }
}