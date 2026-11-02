package com.negativestudios.rewardsmod;

import com.negativestudios.rewardsmod.data.RewardsSavedData;
import com.negativestudios.rewardsmod.menu.RewardsMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RewardsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RewardsEvents {
    private RewardsEvents() {}

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RewardsSavedData.get(player.getServer()).recordJoin(player);
            RewardsMod.openMenu(player, RewardsMenu.View.HUB, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player
                && !player.getServer().isPaused()) {
            RewardsSavedData.get(player.getServer()).tickPlayer(player);
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("rewards")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            RewardsMod.openMenu(player, RewardsMenu.View.HUB, false);
                            return 1;
                        })
                        .then(net.minecraft.commands.Commands.literal("admin")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    RewardsMod.openMenu(player, RewardsMenu.View.ADMIN_HOME, true);
                                    return 1;
                                }))
        );
    }
}
