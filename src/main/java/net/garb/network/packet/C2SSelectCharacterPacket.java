package net.garb.network.packet;

import net.garb.GarbMain;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record C2SSelectCharacterPacket(int slot) implements CustomPayload {

    public static final CustomPayload.Id<C2SSelectCharacterPacket> ID = new CustomPayload.Id<>(GarbMain.identifierOf("select_character"));

    public static final PacketCodec<PacketByteBuf, C2SSelectCharacterPacket> CODEC = PacketCodec.of(C2SSelectCharacterPacket::write, C2SSelectCharacterPacket::read);

    private static void write(C2SSelectCharacterPacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.slot());
    }

    private static C2SSelectCharacterPacket read(PacketByteBuf buf) {
        return new C2SSelectCharacterPacket(buf.readInt());
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}