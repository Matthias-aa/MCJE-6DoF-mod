package com.yourname.zerog.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import com.yourname.zerog.capability.ZeroGCapability;
import com.yourname.zerog.network.ModNetwork;
import com.yourname.zerog.network.ZeroGTogglePacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class ZeroGCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("zerog")
                .executes(ctx -> toggle(ctx.getSource()))
                .then(Commands.literal("on")
                    .executes(ctx -> setState(ctx.getSource(), true)))
                .then(Commands.literal("off")
                    .executes(ctx -> setState(ctx.getSource(), false)))
        );
    }

    private static int toggle(CommandSourceStack source) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) return 0;
        boolean current = player.level().isClientSide()
            ? ZeroGMod.CLIENT_STATE.isZeroGEnabled
            : player.getCapability(ZeroGCapability.ZERO_G_STATE).map(s -> s.isZeroGEnabled).orElse(false);
        return setState(source, !current);
    }

    private static int setState(CommandSourceStack source, boolean enable) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) return 0;

        if (player.level().isClientSide()) {
            // 客户端：立即更新本地状态并发包给服务端
            PlayerState state = ZeroGMod.CLIENT_STATE;
            state.isZeroGEnabled = enable;
            if (!enable) {
                state.reset();
            }
            ModNetwork.CHANNEL.sendToServer(new ZeroGTogglePacket(enable));
        } else {
            // 服务端命令直接操作 capability
            ServerPlayer sp = (ServerPlayer) player;
            sp.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(state -> {
                state.isZeroGEnabled = enable;
                if (!enable) {
                    state.velocity = Vec3.ZERO;
                    state.orientation = new Quaternionf();
                    state.orientationInitialized = false;
                }
            });
        }

        String status = enable ? "§a已开启" : "§c已关闭";
        source.sendSuccess(() -> Component.literal("§6[ZeroG] §f零重力模式 " + status), false);
        return 1;
    }
}
