package com.negativestudios.rewardsmod.menu;

import com.negativestudios.rewardsmod.RewardsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {}

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RewardsMod.MOD_ID);

    public static final RegistryObject<MenuType<RewardsMenu>> REWARDS =
            MENUS.register("rewards", () -> IForgeMenuType.create(RewardsMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
