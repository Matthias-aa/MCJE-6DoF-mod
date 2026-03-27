package com.yourname.zerog;

import com.yourname.zerog.command.ZeroGCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("ZeroG Mod initialized!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ZeroGCommand.register(event.getDispatcher());
    }
}