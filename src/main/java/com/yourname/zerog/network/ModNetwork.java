package com.yourname.zerog.network;

import com.yourname.zerog.ZeroGMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ZeroGMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ZeroGTogglePacket.class, ZeroGTogglePacket::encode, ZeroGTogglePacket::decode, ZeroGTogglePacket::handle);
        CHANNEL.registerMessage(id++, ZeroGInputPacket.class, ZeroGInputPacket::encode, ZeroGInputPacket::decode, ZeroGInputPacket::handle);
    }
}