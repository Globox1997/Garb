package net.garb.network.packet;

import net.garb.GarbMain;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record S2CPlayerCosmeticsPacket(UUID playerId, String name, String skinUsername) implements CustomPayload {

    public static final CustomPayload.Id<S2CPlayerCosmeticsPacket> ID = new CustomPayload.Id<>(GarbMain.identifierOf("player_cosmetics"));

    public static final PacketCodec<PacketByteBuf, S2CPlayerCosmeticsPacket> CODEC = PacketCodec.of(S2CPlayerCosmeticsPacket::write, S2CPlayerCosmeticsPacket::read);

    private static void write(S2CPlayerCosmeticsPacket packet, PacketByteBuf buf) {
        buf.writeUuid(packet.playerId());
        buf.writeString(packet.name() != null ? packet.name() : "");
        buf.writeString(packet.skinUsername() != null ? packet.skinUsername() : "");
    }

    private static S2CPlayerCosmeticsPacket read(PacketByteBuf buf) {
        return new S2CPlayerCosmeticsPacket(buf.readUuid(), buf.readString(), buf.readString());
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}