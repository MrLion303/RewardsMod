package com.negativestudios.rewardsmod.menu;

import com.negativestudios.rewardsmod.RewardsMod;
import com.negativestudios.rewardsmod.data.RewardType;
import com.negativestudios.rewardsmod.data.RewardsSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalDate;

public class RewardsMenu extends AbstractContainerMenu {
    public enum View { HUB, DAILY, STREAK, HOURS, ADMIN_HOME, EDIT_DAILY, EDIT_STREAK, EDIT_HOURS }

    private final SimpleContainer menuInventory = new SimpleContainer(54);
    private final View view;
    private final boolean administrator;
    private final DataSlot selectedHour = DataSlot.standalone();

    public RewardsMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory,
                View.values()[Math.max(0, Math.min(View.values().length - 1, buffer.readVarInt()))],
                buffer.readBoolean());
    }

    public RewardsMenu(int id, Inventory inventory, View view, boolean administrator) {
        super(ModMenus.REWARDS.get(), id);
        this.view = view;
        this.administrator = administrator;
        selectedHour.set(0);
        menuInventory.startOpen(inventory.player);

        for (int i = 0; i < 54; i++) {
            final int slotIndex = i;
            addSlot(new Slot(menuInventory, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18) {
                @Override public boolean mayPickup(Player player) {
                    return isEditor() && slotIndex < rewardCount();
                }
                @Override public boolean mayPlace(ItemStack stack) {
                    return isEditor() && slotIndex < rewardCount();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, 140 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 198));
        }
        addDataSlot(selectedHour);

        if (!inventory.player.level().isClientSide) refreshContents(inventory.player);
    }

    public View view() { return view; }
    public boolean isEditor() {
        return view == View.EDIT_DAILY || view == View.EDIT_STREAK || view == View.EDIT_HOURS;
    }
    private boolean isRewardView() {
        return view == View.DAILY || view == View.STREAK || view == View.HOURS;
    }
    private int rewardCount() {
        return switch (view) {
            case DAILY, STREAK, EDIT_DAILY, EDIT_STREAK -> 30;
            case HOURS, EDIT_HOURS -> 10;
            default -> 0;
        };
    }
    private RewardType rewardType() {
        return switch (view) {
            case DAILY, EDIT_DAILY -> RewardType.DAILY;
            case STREAK, EDIT_STREAK -> RewardType.STREAK;
            case HOURS, EDIT_HOURS -> RewardType.HOURS;
            default -> RewardType.DAILY;
        };
    }

    private void refreshContents(Player player) {
        menuInventory.clearContent();
        for (int i = 45; i < 54; i++) menuInventory.setItem(i, button(Items.GRAY_STAINED_GLASS_PANE, " "));
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        RewardsSavedData data = RewardsSavedData.get(serverPlayer.getServer());

        switch (view) {
            case HUB -> {
                menuInventory.setItem(20, button(Items.CHEST, "Recompensas diarias"));
                menuInventory.setItem(22, button(Items.FIRE_CHARGE, "Recompensas por racha"));
                menuInventory.setItem(24, button(Items.CLOCK, "Recompensas por horas jugadas"));
                menuInventory.setItem(49, button(Items.BOOK, "Consulta tus recompensas"));
                if (serverPlayer.hasPermissions(2)) menuInventory.setItem(53, button(Items.COMPARATOR, "Administración"));
            }
            case DAILY -> {
                LocalDate today = RewardsSavedData.today();
                int current = data.dailyIndex(today);
                boolean claimed = today.equals(data.dailyClaimDate(player.getUUID()));
                for (int i = 0; i < 30; i++) {
                    String status = i == current ? (claimed ? "RECLAMADA" : "DISPONIBLE") : "BLOQUEADA";
                    menuInventory.setItem(i, rewardDisplay(data.getReward(RewardType.DAILY, i), i + 1, status,
                            i == current ? "Recompensa correspondiente a hoy" : "Día de rotación: " + (i + 1)));
                }
                menuInventory.setItem(45, button(Items.ARROW, "Volver"));
                menuInventory.setItem(49, button(Items.CLOCK, "Día actual: " + (current + 1) + " de 30"));
                if (serverPlayer.hasPermissions(2)) menuInventory.setItem(53, button(Items.COMPARATOR, "Administración"));
            }
            case STREAK -> {
                LocalDate today = RewardsSavedData.today();
                int streak = data.streak(player.getUUID());
                boolean joinedToday = today.equals(data.lastJoinDate(player.getUUID()));
                boolean claimed = today.equals(data.streakClaimDate(player.getUUID()));
                for (int i = 0; i < 30; i++) {
                    boolean current = i == streak - 1 && joinedToday;
                    String status = current ? (claimed ? "RECLAMADA" : "DISPONIBLE")
                            : (i < streak ? "COMPLETADA" : "BLOQUEADA");
                    menuInventory.setItem(i, rewardDisplay(data.getReward(RewardType.STREAK, i), i + 1, status,
                            current ? "Recompensa de tu racha actual" : "Mantén tu racha diaria"));
                }
                menuInventory.setItem(45, button(Items.ARROW, "Volver"));
                menuInventory.setItem(49, button(Items.FLINT_AND_STEEL, "Racha actual: " + streak + " días"));
                if (serverPlayer.hasPermissions(2)) menuInventory.setItem(53, button(Items.COMPARATOR, "Administración"));
            }
            case HOURS -> {
                long ticks = data.playedTicks(player.getUUID());
                long hours = ticks / 72000L;
                for (int i = 0; i < 10; i++) {
                    int required = data.getHourThreshold(i);
                    String status = data.hasHoursClaimed(player.getUUID(), i) ? "RECLAMADA"
                            : (ticks >= (long) required * 72000L ? "DISPONIBLE" : "BLOQUEADA");
                    menuInventory.setItem(i, rewardDisplay(data.getReward(RewardType.HOURS, i), i + 1, status,
                            "Requiere " + required + " horas · Tienes " + hours + " horas"));
                }
                menuInventory.setItem(45, button(Items.ARROW, "Volver"));
                menuInventory.setItem(49, button(Items.CLOCK, "Tiempo jugado: " + hours + " horas"));
                if (serverPlayer.hasPermissions(2)) menuInventory.setItem(53, button(Items.COMPARATOR, "Administración"));
            }
            case ADMIN_HOME -> {
                menuInventory.setItem(20, button(Items.CHEST, "Editar recompensas diarias"));
                menuInventory.setItem(22, button(Items.FIRE_CHARGE, "Editar recompensas por racha"));
                menuInventory.setItem(24, button(Items.CLOCK, "Editar recompensas por horas"));
                menuInventory.setItem(49, button(Items.ARROW, "Volver a recompensas"));
            }
            case EDIT_DAILY, EDIT_STREAK, EDIT_HOURS -> {
                for (int i = 0; i < rewardCount(); i++) {
                    ItemStack configured = data.getReward(rewardType(), i);
                    if (!configured.isEmpty()) addLore(configured, "Espacio " + (i + 1) + " · clic para editar");
                    menuInventory.setItem(i, configured);
                }
                menuInventory.setItem(45, button(Items.ARROW, "Volver a administración"));
                if (view == View.EDIT_HOURS) {
                    int selected = Math.max(0, Math.min(9, selectedHour.get()));
                    menuInventory.setItem(49, button(Items.CLOCK, "Nivel " + (selected + 1) + " · "
                            + data.getHourThreshold(selected) + " horas"));
                    menuInventory.setItem(51, button(Items.RED_DYE, "Restar 1 hora"));
                    menuInventory.setItem(52, button(Items.LIME_DYE, "Sumar 1 hora"));
                    menuInventory.setItem(53, button(Items.EMERALD, "Sumar 5 horas"));
                } else {
                    menuInventory.setItem(49, button(Items.PAPER,
                            rewardCount() + " espacios · coloca objetos desde tu inventario"));
                }
            }
        }
        menuInventory.setChanged();
    }

    private static ItemStack button(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(Component.literal(name).withStyle(ChatFormatting.YELLOW));
        return stack;
    }

    private static ItemStack rewardDisplay(ItemStack reward, int number, String status, String detail) {
        ItemStack display = reward.isEmpty() ? new ItemStack(Items.GRAY_STAINED_GLASS_PANE) : reward.copy();
        Component original = display.getHoverName();
        ChatFormatting color = switch (status) {
            case "DISPONIBLE" -> ChatFormatting.GREEN;
            case "RECLAMADA", "COMPLETADA" -> ChatFormatting.GRAY;
            default -> ChatFormatting.RED;
        };
        display.setHoverName(Component.literal("Día/Nivel " + number + " · " + status).withStyle(color));
        addLore(display, original.getString());
        addLore(display, detail);
        if (status.equals("DISPONIBLE")) addLore(display, "Haz clic para reclamar");
        else if (status.equals("BLOQUEADA")) addLore(display, "Todavía no está disponible");
        return display;
    }

    private static void addLore(ItemStack stack, String line) {
        net.minecraft.nbt.CompoundTag root = stack.getOrCreateTag();
        net.minecraft.nbt.CompoundTag display = root.getCompound("display");
        net.minecraft.nbt.ListTag lore = display.getList("Lore", net.minecraft.nbt.Tag.TAG_STRING);
        lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal(line))));
        display.put("Lore", lore);
        root.put("display", display);
        stack.setTag(root);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean serverSide = !player.level().isClientSide;
        if (serverSide && player instanceof ServerPlayer serverPlayer && slotId >= 0 && slotId < 54) {
            if (view == View.HUB) {
                if (slotId == 20) { RewardsMod.openMenu(serverPlayer, View.DAILY, false); return; }
                if (slotId == 22) { RewardsMod.openMenu(serverPlayer, View.STREAK, false); return; }
                if (slotId == 24) { RewardsMod.openMenu(serverPlayer, View.HOURS, false); return; }
                if (slotId == 53 && serverPlayer.hasPermissions(2)) {
                    RewardsMod.openMenu(serverPlayer, View.ADMIN_HOME, true); return;
                }
            }
            if (view == View.ADMIN_HOME) {
                if (!serverPlayer.hasPermissions(2)) return;
                if (slotId == 20) { RewardsMod.openMenu(serverPlayer, View.EDIT_DAILY, true); return; }
                if (slotId == 22) { RewardsMod.openMenu(serverPlayer, View.EDIT_STREAK, true); return; }
                if (slotId == 24) { RewardsMod.openMenu(serverPlayer, View.EDIT_HOURS, true); return; }
                if (slotId == 49) { RewardsMod.openMenu(serverPlayer, View.HUB, false); return; }
            }
            if (isRewardView()) {
                if (slotId == 45) { RewardsMod.openMenu(serverPlayer, View.HUB, false); return; }
                if (slotId == 53 && serverPlayer.hasPermissions(2)) {
                    RewardsMod.openMenu(serverPlayer, View.ADMIN_HOME, true); return;
                }
                if (slotId < rewardCount()) { attemptClaim(serverPlayer, slotId); return; }
            }
            if (isEditor()) {
                if (!serverPlayer.hasPermissions(2)) return;
                if (slotId == 45) { RewardsMod.openMenu(serverPlayer, View.ADMIN_HOME, true); return; }
                if (view == View.EDIT_HOURS && slotId >= 0 && slotId < 10) {
                    if (clickType == ClickType.PICKUP && button == 0 && getCarried().isEmpty()) {
                        selectedHour.set(slotId);
                        refreshContents(serverPlayer);
                        broadcastChanges();
                        return;
                    }
                    if (clickType == ClickType.PICKUP && button == 1 && getCarried().isEmpty()) {
                        menuInventory.setItem(slotId, ItemStack.EMPTY);
                        saveEditedRewards(serverPlayer);
                        refreshContents(serverPlayer);
                        broadcastChanges();
                        return;
                    }
                }
                if (view == View.EDIT_HOURS && slotId == 51) {
                    RewardsSavedData.get(serverPlayer.getServer()).adjustHourThreshold(selectedHour.get(), -1);
                    refreshContents(serverPlayer); broadcastChanges(); return;
                }
                if (view == View.EDIT_HOURS && slotId == 52) {
                    RewardsSavedData.get(serverPlayer.getServer()).adjustHourThreshold(selectedHour.get(), 1);
                    refreshContents(serverPlayer); broadcastChanges(); return;
                }
                if (view == View.EDIT_HOURS && slotId == 53) {
                    RewardsSavedData.get(serverPlayer.getServer()).adjustHourThreshold(selectedHour.get(), 5);
                    refreshContents(serverPlayer); broadcastChanges(); return;
                }
            }
        }
        super.clicked(slotId, button, clickType, player);
        if (serverSide && player instanceof ServerPlayer serverPlayer && isEditor()
                && serverPlayer.hasPermissions(2)) saveEditedRewards(serverPlayer);
    }

    private void saveEditedRewards(ServerPlayer player) {
        RewardsSavedData data = RewardsSavedData.get(player.getServer());
        for (int i = 0; i < rewardCount(); i++) data.setReward(rewardType(), i, menuInventory.getItem(i));
    }

    private void attemptClaim(ServerPlayer player, int index) {
        RewardsSavedData data = RewardsSavedData.get(player.getServer());
        LocalDate today = RewardsSavedData.today();
        ItemStack reward;
        switch (view) {
            case DAILY -> {
                if (today.equals(data.dailyClaimDate(player.getUUID()))) {
                    player.sendSystemMessage(Component.literal("Ya reclamaste la recompensa diaria de hoy.")
                            .withStyle(ChatFormatting.YELLOW)); return;
                }
                if (index != data.dailyIndex(today)) {
                    player.sendSystemMessage(Component.literal("Esta recompensa todavía no está disponible.")
                            .withStyle(ChatFormatting.RED)); return;
                }
                reward = data.getReward(RewardType.DAILY, index);
                if (reward.isEmpty()) {
                    player.sendSystemMessage(Component.literal("Esta recompensa aún no ha sido configurada.")
                            .withStyle(ChatFormatting.RED)); return;
                }
                data.setDailyClaimDate(player.getUUID(), today);
                give(player, reward);
                player.sendSystemMessage(Component.literal("¡Recompensa diaria reclamada!").withStyle(ChatFormatting.GREEN));
            }
            case STREAK -> {
                if (!today.equals(data.lastJoinDate(player.getUUID())) || index != data.streak(player.getUUID()) - 1) {
                    player.sendSystemMessage(Component.literal("Debes mantener tu racha para desbloquear esta recompensa.")
                            .withStyle(ChatFormatting.RED)); return;
                }
                if (today.equals(data.streakClaimDate(player.getUUID()))) {
                    player.sendSystemMessage(Component.literal("Ya reclamaste la recompensa de tu racha de hoy.")
                            .withStyle(ChatFormatting.YELLOW)); return;
                }
                reward = data.getReward(RewardType.STREAK, index);
                if (reward.isEmpty()) {
                    player.sendSystemMessage(Component.literal("Esta recompensa aún no ha sido configurada.")
                            .withStyle(ChatFormatting.RED)); return;
                }
                data.setStreakClaimDate(player.getUUID(), today);
                give(player, reward);
                player.sendSystemMessage(Component.literal("¡Recompensa de racha reclamada!").withStyle(ChatFormatting.GREEN));
            }
            case HOURS -> {
                if (!data.canClaimHours(player.getUUID(), index)) {
                    player.sendSystemMessage(Component.literal("Todavía no has alcanzado las horas necesarias.")
                            .withStyle(ChatFormatting.RED)); return;
                }
                reward = data.getReward(RewardType.HOURS, index);
                if (reward.isEmpty()) {
                    player.sendSystemMessage(Component.literal("Esta recompensa aún no ha sido configurada.")
                            .withStyle(ChatFormatting.RED)); return;
                }
                data.markHoursClaimed(player.getUUID(), index);
                give(player, reward);
                player.sendSystemMessage(Component.literal("¡Recompensa por horas jugadas reclamada!")
                        .withStyle(ChatFormatting.GREEN));
            }
            default -> { return; }
        }
        RewardsMod.openMenu(player, view, false);
    }

    private static void give(ServerPlayer player, ItemStack reward) {
        ItemStack copy = reward.copy();
        if (!player.getInventory().add(copy)) player.drop(copy, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();

        if (index < 54) {
            if (!moveItemStackTo(source, 54, slots.size(), true)) return ItemStack.EMPTY;
        } else if (isEditor()) {
            if (!moveItemStackTo(source, 0, rewardCount(), false)) return ItemStack.EMPTY;
        } else if (index < 81) {
            if (!moveItemStackTo(source, 81, slots.size(), false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(source, 54, 81, false)) return ItemStack.EMPTY;
        }

        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (source.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source);
        if (player instanceof ServerPlayer serverPlayer && isEditor() && !player.level().isClientSide) {
            saveEditedRewards(serverPlayer);
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
