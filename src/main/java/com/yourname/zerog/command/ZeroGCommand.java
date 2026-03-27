package com.yourname.zerog.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yourname.zerog.PlayerState;
import com.yourname.zerog.ZeroGMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ZeroGCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("zerog")
                        .executes(context -> {
                            PlayerState state = ZeroGMod.CLIENT_STATE;
                            state.isZeroGEnabled = !state.isZeroGEnabled;
                            if (!state.isZeroGEnabled) {
                                state.reset();
                            }
                            String status = state.isZeroGEnabled ? "§a已开启" : "§c已关闭";
                            context.getSource().sendSuccess(
                                    () -> Component.literal("§6[ZeroG] §f零重力模式 " + status),
                                    false
                            );
                            return 1;
                        })
                        .then(Commands.literal("on")
                                .executes(context -> {
                                    PlayerState state = ZeroGMod.CLIENT_STATE;
                                    state.isZeroGEnabled = true;
                                    state.orientationInitialized = false;
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("§6[ZeroG] §f零重力模式 §a已开启"),
                                            false
                                    );
                                    return 1;
                                })
                        )
                        .then(Commands.literal("off")
                                .executes(context -> {
                                    PlayerState state = ZeroGMod.CLIENT_STATE;
                                    state.isZeroGEnabled = false;
                                    state.reset();
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("§6[ZeroG] §f零重力模式 §c已关闭"),
                                            false
                                    );
                                    return 1;
                                })
                        )
        );
    }
}