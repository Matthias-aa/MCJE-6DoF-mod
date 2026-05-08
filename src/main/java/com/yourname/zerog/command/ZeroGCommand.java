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

        boolean current;
        if (player.level().isClientSide()) {
            current = ZeroGMod.CLIENT_STATE.isZeroGEnabled;
        } else {
            current = player.getCapability(ZeroGCapability.ZERO_G_STATE)
                    .map(s -> s.isZeroGEnabled)
                    .orElse(false);
        }
        return setState(source, !current);
    }

    private static int setState(CommandSourceStack source, boolean enable) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) return 0;

        if (player.level().isClientSide()) {
            PlayerState state = ZeroGMod.CLIENT_STATE;
            state.isZeroGEnabled = enable;
            if (!enable) state.reset();
            ModNetwork.CHANNEL.sendToServer(new ZeroGTogglePacket(enable));
        } else {
            ServerPlayer sp = (ServerPlayer) player;
            sp.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(s -> {
                s.isZeroGEnabled = enable;
                if (!enable) {
                    s.velocity = Vec3.ZERO;
                    s.orientation = new Quaternionf();
                    s.orientationInitialized = false;
                }
            });
        }

        String status = enable ? "§a已开启" : "§c已关闭";
        source.sendSuccess(() -> Component.literal("§6[ZeroG] §f零重力模式 " + status), false);
        return 1;
    }
}            current = ZeroGMod.CLIENT_STATE.isZeroGEnabled;
        } else {
            current = player.getCapability(ZeroGCapability.ZERO_G_STATE)
                    .map(s -> s.isZeroGEnabled)
                    .orElse(false);
        }
        return setState(source, !current);
    }

    private static int setState(CommandSourceStack source, boolean enable) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) return 0;

        if (player.level().isClientSide()) {
            PlayerState state = ZeroGMod.CLIENT_STATE;
            state.isZeroGEnabled = enable;
            if (!enable) state.reset();
            // 关键：通知服务端
            ModNetwork.CHANNEL.sendToServer(new ZeroGTogglePacket(enable));
        } else {
            ServerPlayer sp = (ServerPlayer) player;
            sp.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(s -> {
                s.isZeroGEnabled = enable;
                if (!enable) {
                    s.velocity = Vec3.ZERO;
                    s.orientation = new Quaternionf();
                    s.orientationInitialized = false;
                }
            });
        }

        String status = enable ? "§a已开启" : "§c已关闭";
        source.sendSuccess(() -> Component.literal("§6[ZeroG] §f零重力模式 " + status), false);
        return 1;
    }
}            current = player.getCapability(ZeroGCapability.ZERO_G_STATE)
                    .map(s -> s.isZeroGEnabled).orElse(false);
        }
        return setState(source, !current);
    }

    private static int setState(CommandSourceStack source, boolean enable) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) return 0;

        if (player.level().isClientSide()) {
            // 本地立即生效
            PlayerState state = ZeroGMod.CLIENT_STATE;
            state.isZeroGEnabled = enable;
            if (!enable) state.reset();

            // ★ 关键：通知服务端
            ModNetwork.CHANNEL.sendToServer(new ZeroGTogglePacket(enable));
        } else {
            // 服务端直接修改 capability
            ServerPlayer sp = (ServerPlayer) player;
            sp.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(s -> {
                s.isZeroGEnabled = enable;
                if (!enable) {
                    s.velocity = Vec3.ZERO;
                    s.orientation = new Quaternionf();
                    s.orientationInitialized = false;
                }
            });
        }

        String status = enable ? "§a已开启" : "§c已关闭";
        source.sendSuccess(() -> Component.literal("§6[ZeroG] §f零重力模式 " + status), false);
        return 1;
    }
}            current = player.getCapability(ZeroGCapability.ZERO_G_STATE)
                    .map(s -> s.isZeroGEnabled).orElse(false);
        }
        return setState(source, !current);
    }

    private static int setState(CommandSourceStack source, boolean enable) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) return 0;

        if (player.level().isClientSide()) {
            // 本地状态立即生效
            PlayerState state = ZeroGMod.CLIENT_STATE;
            state.isZeroGEnabled = enable;
            if (!enable) state.reset();

            // 通知服务端（单人内建服务器 / 远程服务器均由此包触发）
            ModNetwork.CHANNEL.sendToServer(new ZeroGTogglePacket(enable));
        } else {
            // 服务端直接执行
            ServerPlayer sp = (ServerPlayer) player;
            sp.getCapability(ZeroGCapability.ZERO_G_STATE).ifPresent(s -> {
                s.isZeroGEnabled = enable;
                if (!enable) {
                    s.velocity = Vec3.ZERO;
                    s.orientation = new Quaternionf();
                    s.orientationInitialized = false;
                }
            });
        }

        String status = enable ? "§a已开启" : "§c已关闭";
        source.sendSuccess(() -> Component.literal("§6[ZeroG] §f零重力模式 " + status), false);
        return 1;
    }
}
    private static int setState(CommandSourceStack source, boolean enable) {
        Entity entity = source.getEntity();
        if (!(entity instanceof Player player)) return 0;

        if (player.level().isClientSide()) {
            // 客户端立即更新本地状态，同时通知服务端
            PlayerState state = ZeroGMod.CLIENT_STATE;
            state.isZeroGEnabled = enable;
            if (!enable) state.reset();
            ModNetwork.CHANNEL.sendToServer(new ZeroGTogglePacket(enable));
        } else {
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
