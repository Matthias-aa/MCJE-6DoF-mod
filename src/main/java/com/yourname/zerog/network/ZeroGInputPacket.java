package com.yourname.zerog.network;

import com.yourname.zerog.capability.ZeroGCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Quaternionf;
import java.util.function.Supplier;

public record ZeroGInputPacket(float forward, float strafe, float up, 
                               boolean rollLeft, boolean rollRight, Quaternionf orientation) {
    public static void encode(ZeroGInputPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.forward);
        buf.writeFloat(msg.strafe);
        buf.writeFloat(msg.up);
        buf.writeBoolean(msg.rollLeft);
        buf.writeBoolean(msg.rollRight);
        buf.writeQuaternion(msg.orientation);
    }

    public static ZeroGInputPacket decode(FriendlyByteBuf buf) {
        return new ZeroGInputPacket(buf.readFloat(), buf.readFloat(), buf.readFloat(), 
                buf.readBoolean(), buf.readBoolean(), buf.readQuaternion());
    }

    public static void handle(ZeroGInputPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null) {
                sp.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(state -> {
                    state.inputForward = msg.forward;
                    state.inputStrafe = msg.strafe;
                    state.inputUp = msg.up;
                    state.inputRollLeft = msg.rollLeft;
                    state.inputRollRight = msg.rollRight;
                    state.orientation.set(msg.orientation); // 同步四元数到服务端
                    state.orientationInitialized = true;
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
