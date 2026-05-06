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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ZeroGMod.MOD_ID)
public class ZeroGMod {

    public static final String MOD_ID = "zerog";
    public static final Logger LOGGER = LogManager.getLogger();

    /** 客户端本地视觉状态（与之前完全兼容） */
    public static final PlayerState CLIENT_STATE = new PlayerState();

    public ZeroGMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);

        // 注册网络包
        ModNetwork.register();
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("ZeroG Mod initialized!");
    }

    /** 将 Capability 附加到每个玩家 */
    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(MOD_ID, "zerog_state"), new ZeroGStateProvider());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ZeroGCommand.register(event.getDispatcher());
    }
}
