package com.yourname.zerog.network;

import com.yourname.zerog.ZeroGMod;
import com.yourname.zerog.capability.ZeroGCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ZeroGTogglePacket(boolean enable) {
    public static void encode(ZeroGTogglePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enable);
    }

    public static ZeroGTogglePacket decode(FriendlyByteBuf buf) {
        return new ZeroGTogglePacket(buf.readBoolean());
    }

    public static void handle(ZeroGTogglePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null) {
                // 服务端处理客户端发来的请求
                sp.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(state -> {
                    state.isZeroGEnabled = msg.enable;
                    if (!msg.enable) {
                        state.velocity = net.minecraft.world.phys.Vec3.ZERO;
                        state.orientation = new org.joml.Quaternionf();
                        state.orientationInitialized = false;
                    }
                });
            } else {
                // 客户端收到服务端同步过来的状态
                ZeroGMod.CLIENT_STATE.isZeroGEnabled = msg.enable;
                if (!msg.enable) ZeroGMod.CLIENT_STATE.reset();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
