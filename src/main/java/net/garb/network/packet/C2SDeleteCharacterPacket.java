package net.garb.network.packet;

import net.garb.GarbMain;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record C2SDeleteCharacterPacket(int slot) implements CustomPayload {

    public static final CustomPayload.Id<C2SDeleteCharacterPacket> ID = new CustomPayload.Id<>(GarbMain.identifierOf("delete_character"));

    public static final PacketCodec<PacketByteBuf, C2SDeleteCharacterPacket> CODEC = PacketCodec.of(C2SDeleteCharacterPacket::write, C2SDeleteCharacterPacket::read);

    private static void write(C2SDeleteCharacterPacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.slot());
    }

    private static C2SDeleteCharacterPacket read(PacketByteBuf buf) {
        return new C2SDeleteCharacterPacket(buf.readInt());
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
