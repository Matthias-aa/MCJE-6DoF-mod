package com.yourname.zerog.server;

import com.yourname.zerog.capability.ZeroGCapability;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = ZeroGMod.MOD_ID)
public class ServerEventHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            player.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(state -> {
                if (!state.isZeroGEnabled) return;

                player.setNoGravity(true);

                // 根据同步过来的四元数计算局部轴
                Vector3f f = new Vector3f(0, 0, 1);
                Vector3f r = new Vector3f(1, 0, 0);
                Vector3f u = new Vector3f(0, 1, 0);
                state.orientation.transform(f);
                state.orientation.transform(r);
                state.orientation.transform(u);

                double speed = 0.12; // 提高基础速度
                Vec3 move = new Vec3(f.x, f.y, f.z).scale(state.inputForward * speed)
                        .add(new Vec3(r.x, r.y, r.z).scale(state.inputStrafe * speed))
                        .add(new Vec3(u.x, u.y, u.z).scale(state.inputUp * speed));

                player.setDeltaMovement(player.getDeltaMovement().add(move).scale(0.95));
                player.hurtMarked = true;
            });
        }
    }
}
