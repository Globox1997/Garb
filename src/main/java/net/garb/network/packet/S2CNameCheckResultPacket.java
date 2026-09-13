package net.garb.network.packet;

import net.garb.GarbMain;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record S2CNameCheckResultPacket(String name, boolean taken) implements CustomPayload {

    public static final CustomPayload.Id<S2CNameCheckResultPacket> ID = new CustomPayload.Id<>(GarbMain.identifierOf("name_check_result"));

    public static final PacketCodec<PacketByteBuf, S2CNameCheckResultPacket> CODEC = PacketCodec.of(S2CNameCheckResultPacket::write, S2CNameCheckResultPacket::read);

    private static void write(S2CNameCheckResultPacket packet, PacketByteBuf buf) {
        buf.writeString(packet.name() != null ? packet.name() : "");
        buf.writeBoolean(packet.taken());
    }

    private static S2CNameCheckResultPacket read(PacketByteBuf buf) {
        return new S2CNameCheckResultPacket(buf.readString(), buf.readBoolean());
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}