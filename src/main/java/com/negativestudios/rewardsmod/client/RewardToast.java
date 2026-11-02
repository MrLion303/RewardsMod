package com.negativestudios.rewardsmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RewardToast implements Toast {
    private final int rewardNumber;
    private final int hours;

    private RewardToast(int rewardNumber, int hours) {
        this.rewardNumber = rewardNumber;
        this.hours = hours;
    }

    public static void show(int rewardNumber, int hours) {
        Minecraft.getInstance().getToasts().addToast(new RewardToast(rewardNumber, hours));
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent component, long elapsed) {
        graphics.fill(0, 0, 160, 32, 0xEE242424);
        graphics.renderOutline(0, 0, 160, 32, 0xFFB99A42);
        graphics.renderItem(new ItemStack(Items.CHEST), 8, 8);
        graphics.drawString(Minecraft.getInstance().font, Component.literal("¡Recompensa desbloqueada!"),
                30, 7, 0xFFFFD86B, false);
        graphics.drawString(Minecraft.getInstance().font,
                Component.literal("Nivel " + rewardNumber + " · " + hours + " horas jugadas"),
                30, 18, 0xFFFFFFFF, false);
        return elapsed >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}
