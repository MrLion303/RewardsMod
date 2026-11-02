package com.negativestudios.rewardsmod.network;

import com.negativestudios.rewardsmod.RewardsMod;
import com.negativestudios.rewardsmod.client.RewardToast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class RewardsNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RewardsMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int packetId = 0;

    private RewardsNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(packetId++, HoursToastPacket.class,
                HoursToastPacket::encode, HoursToastPacket::decode, HoursToastPacket::handle);
    }

    public static void sendHoursToast(ServerPlayer player, int index, int hours) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HoursToastPacket(index, hours));
    }

    public record HoursToastPacket(int index, int hours) {
        public static void encode(HoursToastPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.index);
            buffer.writeVarInt(packet.hours);
        }
        public static HoursToastPacket decode(FriendlyByteBuf buffer) {
            return new HoursToastPacket(buffer.readVarInt(), buffer.readVarInt());
        }
        public static void handle(HoursToastPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> RewardToast.show(packet.index + 1, packet.hours)));
            context.setPacketHandled(true);
        }
    }
}
