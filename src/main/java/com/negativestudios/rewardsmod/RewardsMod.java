package com.negativestudios.rewardsmod;

import com.negativestudios.rewardsmod.menu.ModMenus;
import com.negativestudios.rewardsmod.menu.RewardsMenu;
import com.negativestudios.rewardsmod.network.RewardsNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RewardsMod.MOD_ID)
public class RewardsMod {
    public static final String MOD_ID = "rewardsmod";

    public RewardsMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModMenus.register(modBus);
        RewardsNetwork.register();
    }

    public static void openMenu(ServerPlayer player, RewardsMenu.View view, boolean administrator) {
        NetworkHooks.openScreen(player, (id, inventory, user) ->
                new RewardsMenu(id, inventory, view, administrator),
                buffer -> {
                    buffer.writeVarInt(view.ordinal());
                    buffer.writeBoolean(administrator);
                });
    }
}
