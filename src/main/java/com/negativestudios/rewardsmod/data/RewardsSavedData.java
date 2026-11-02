package com.negativestudios.rewardsmod.data;

import com.negativestudios.rewardsmod.network.RewardsNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RewardsSavedData extends SavedData {
    public static final ZoneId ZONA_HORARIA = ZoneId.of("America/Monterrey");
    private static final String DATA_ID = "rewardsmod_data";
    private static final int HOURS_COUNT = 10;

    private final ItemStack[] dailyRewards = emptyStacks(30);
    private final ItemStack[] streakRewards = emptyStacks(30);
    private final ItemStack[] hoursRewards = emptyStacks(HOURS_COUNT);
    private final int[] hourThresholds = new int[HOURS_COUNT];
    private final Map<UUID, PlayerProgress> players = new HashMap<>();
    private LocalDate dailyCycleStart = LocalDate.now(ZONA_HORARIA);

    public RewardsSavedData() {
        for (int i = 0; i < HOURS_COUNT; i++) hourThresholds[i] = (i + 1) * 10;
    }

    private static ItemStack[] emptyStacks(int count) {
        ItemStack[] stacks = new ItemStack[count];
        for (int i = 0; i < count; i++) stacks[i] = ItemStack.EMPTY;
        return stacks;
    }

    public static RewardsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                RewardsSavedData::load, RewardsSavedData::new, DATA_ID);
    }

    public static LocalDate today() {
        return LocalDate.now(ZONA_HORARIA);
    }

    public int dailyIndex(LocalDate date) {
        long days = ChronoUnit.DAYS.between(dailyCycleStart, date);
        return (int) Math.floorMod(days, 30);
    }

    public ItemStack getReward(RewardType type, int index) {
        ItemStack[] source = rewardsFor(type);
        if (index < 0 || index >= source.length) return ItemStack.EMPTY;
        return source[index].copy();
    }

    public void setReward(RewardType type, int index, ItemStack stack) {
        ItemStack[] target = rewardsFor(type);
        if (index < 0 || index >= target.length) return;
        target[index] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        setDirty();
    }

    public int getRewardCount(RewardType type) {
        return rewardsFor(type).length;
    }

    private ItemStack[] rewardsFor(RewardType type) {
        return switch (type) {
            case DAILY -> dailyRewards;
            case STREAK -> streakRewards;
            case HOURS -> hoursRewards;
        };
    }

    public int getHourThreshold(int index) {
        return hourThresholds[Math.max(0, Math.min(HOURS_COUNT - 1, index))];
    }

    public void adjustHourThreshold(int index, int amount) {
        if (index < 0 || index >= HOURS_COUNT) return;
        hourThresholds[index] = Math.max(1, Math.min(8760, hourThresholds[index] + amount));
        setDirty();
    }

    private PlayerProgress progress(UUID playerId) {
        return players.computeIfAbsent(playerId, ignored -> new PlayerProgress());
    }

    public void recordJoin(ServerPlayer player) {
        PlayerProgress p = progress(player.getUUID());
        LocalDate now = today();
        if (p.lastJoinDate == null) {
            p.streak = 1;
        } else if (!p.lastJoinDate.equals(now)) {
            long gap = ChronoUnit.DAYS.between(p.lastJoinDate, now);
            p.streak = gap == 1 ? (p.streak >= 30 ? 1 : p.streak + 1) : 1;
        }
        if (p.lastJoinDate == null || !p.lastJoinDate.equals(now)) {
            p.lastJoinDate = now;
            setDirty();
        }
    }

    public void tickPlayer(ServerPlayer player) {
        PlayerProgress p = progress(player.getUUID());
        p.playedTicks++;
        boolean changed = p.playedTicks % 20 == 0;

        for (int i = 0; i < HOURS_COUNT; i++) {
            long requiredTicks = (long) hourThresholds[i] * 60L * 60L * 20L;
            if (!p.hourNotified[i] && p.playedTicks >= requiredTicks) {
                p.hourNotified[i] = true;
                RewardsNetwork.sendHoursToast(player, i, hourThresholds[i]);
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    public boolean canClaimHours(UUID id, int index) {
        PlayerProgress p = progress(id);
        return index >= 0 && index < HOURS_COUNT
                && p.playedTicks >= (long) hourThresholds[index] * 72000L
                && !p.hourClaimed[index];
    }

    public boolean hasHoursClaimed(UUID id, int index) {
        return index >= 0 && index < HOURS_COUNT && progress(id).hourClaimed[index];
    }

    public void markHoursClaimed(UUID id, int index) {
        if (index < 0 || index >= HOURS_COUNT) return;
        progress(id).hourClaimed[index] = true;
        setDirty();
    }

    public long playedTicks(UUID id) {
        return progress(id).playedTicks;
    }

    public int streak(UUID id) {
        return progress(id).streak;
    }

    public LocalDate lastJoinDate(UUID id) {
        return progress(id).lastJoinDate;
    }

    public LocalDate dailyClaimDate(UUID id) {
        return progress(id).dailyClaimDate;
    }

    public void setDailyClaimDate(UUID id, LocalDate date) {
        progress(id).dailyClaimDate = date;
        setDirty();
    }

    public LocalDate streakClaimDate(UUID id) {
        return progress(id).streakClaimDate;
    }

    public void setStreakClaimDate(UUID id, LocalDate date) {
        progress(id).streakClaimDate = date;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("DailyCycleStart", dailyCycleStart.toString());
        saveRewards(tag, "DailyRewards", dailyRewards);
        saveRewards(tag, "StreakRewards", streakRewards);
        saveRewards(tag, "HoursRewards", hoursRewards);

        ListTag thresholds = new ListTag();
        for (int value : hourThresholds) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Value", value);
            thresholds.add(entry);
        }
        tag.put("HourThresholds", thresholds);

        CompoundTag playerTag = new CompoundTag();
        players.forEach((uuid, p) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("PlayedTicks", p.playedTicks);
            entry.putInt("Streak", p.streak);
            putDate(entry, "LastJoinDate", p.lastJoinDate);
            putDate(entry, "DailyClaimDate", p.dailyClaimDate);
            putDate(entry, "StreakClaimDate", p.streakClaimDate);
            for (int i = 0; i < HOURS_COUNT; i++) {
                entry.putBoolean("HourClaimed" + i, p.hourClaimed[i]);
                entry.putBoolean("HourNotified" + i, p.hourNotified[i]);
            }
            playerTag.put(uuid.toString(), entry);
        });
        tag.put("Players", playerTag);
        return tag;
    }

    private static void saveRewards(CompoundTag parent, String key, ItemStack[] rewards) {
        ListTag list = new ListTag();
        for (int i = 0; i < rewards.length; i++) {
            if (rewards[i].isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            entry.put("Stack", rewards[i].save(new CompoundTag()));
            list.add(entry);
        }
        parent.put(key, list);
    }

    private static void putDate(CompoundTag tag, String key, LocalDate date) {
        if (date != null) tag.putString(key, date.toString());
    }

    private static LocalDate readDate(CompoundTag tag, String key) {
        String value = tag.getString(key);
        if (value.isBlank()) return null;
        try { return LocalDate.parse(value); }
        catch (Exception ignored) { return null; }
    }

    public static RewardsSavedData load(CompoundTag tag) {
        RewardsSavedData data = new RewardsSavedData();
        LocalDate start = readDate(tag, "DailyCycleStart");
        if (start != null) data.dailyCycleStart = start;
        loadRewards(tag, "DailyRewards", data.dailyRewards);
        loadRewards(tag, "StreakRewards", data.streakRewards);
        loadRewards(tag, "HoursRewards", data.hoursRewards);

        ListTag thresholds = tag.getList("HourThresholds", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(HOURS_COUNT, thresholds.size()); i++) {
            data.hourThresholds[i] = Math.max(1, thresholds.getCompound(i).getInt("Value"));
        }

        CompoundTag savedPlayers = tag.getCompound("Players");
        for (String key : savedPlayers.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag entry = savedPlayers.getCompound(key);
                PlayerProgress p = new PlayerProgress();
                p.playedTicks = entry.getLong("PlayedTicks");
                p.streak = entry.getInt("Streak");
                p.lastJoinDate = readDate(entry, "LastJoinDate");
                p.dailyClaimDate = readDate(entry, "DailyClaimDate");
                p.streakClaimDate = readDate(entry, "StreakClaimDate");
                for (int i = 0; i < HOURS_COUNT; i++) {
                    p.hourClaimed[i] = entry.getBoolean("HourClaimed" + i);
                    p.hourNotified[i] = entry.getBoolean("HourNotified" + i);
                }
                data.players.put(uuid, p);
            } catch (IllegalArgumentException ignored) {
                // Se ignoran UUID dañados en el archivo del mundo.
            }
        }
        return data;
    }

    private static void loadRewards(CompoundTag parent, String key, ItemStack[] target) {
        ListTag list = parent.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < target.length) {
                target[slot] = ItemStack.of(entry.getCompound("Stack"));
            }
        }
    }

    private static class PlayerProgress {
        private long playedTicks;
        private int streak;
        private LocalDate lastJoinDate;
        private LocalDate dailyClaimDate;
        private LocalDate streakClaimDate;
        private final boolean[] hourClaimed = new boolean[HOURS_COUNT];
        private final boolean[] hourNotified = new boolean[HOURS_COUNT];
    }
}
