package com.negativestudios.rewardsmod;

import com.negativestudios.rewardsmod.menu.ModMenus;
import com.negativestudios.rewardsmod.menu.RewardsMenu;
import com.negativestudios.rewardsmod.network.RewardsNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
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
        String title = switch (view) {
            case HUB -> "Recompensas";
            case DAILY -> "Recompensas diarias";
            case STREAK -> "Recompensas por racha";
            case HOURS -> "Recompensas por horas jugadas";
            case ADMIN_HOME -> "Administración de recompensas";
            case EDIT_DAILY -> "Editar recompensas diarias";
            case EDIT_STREAK -> "Editar recompensas por racha";
            case EDIT_HOURS -> "Editar recompensas por horas";
        };
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((id, inventory, user) ->
                        new RewardsMenu(id, inventory, view, administrator), Component.literal(title)),
                buffer -> {
                    buffer.writeVarInt(view.ordinal());
                    buffer.writeBoolean(administrator);
                });
    }
}
