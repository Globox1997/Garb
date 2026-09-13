package net.garb.network.packet;

import net.garb.GarbMain;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record C2SCheckNamePacket(String name) implements CustomPayload {

    public static final CustomPayload.Id<C2SCheckNamePacket> ID = new CustomPayload.Id<>(GarbMain.identifierOf("check_name"));

    public static final PacketCodec<PacketByteBuf, C2SCheckNamePacket> CODEC = PacketCodec.of(C2SCheckNamePacket::write, C2SCheckNamePacket::read);

    private static void write(C2SCheckNamePacket packet, PacketByteBuf buf) {
        buf.writeString(packet.name() != null ? packet.name() : "");
    }

    private static C2SCheckNamePacket read(PacketByteBuf buf) {
        return new C2SCheckNamePacket(buf.readString());
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}