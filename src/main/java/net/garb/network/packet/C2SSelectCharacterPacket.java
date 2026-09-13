package net.garb.network.packet;

import net.garb.GarbMain;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record C2SSelectCharacterPacket(int slot, String characterName, String skinUsername, String classId) implements CustomPayload {

    public static final CustomPayload.Id<C2SSelectCharacterPacket> ID = new CustomPayload.Id<>(GarbMain.identifierOf("select_character"));

    public static final PacketCodec<PacketByteBuf, C2SSelectCharacterPacket> CODEC = PacketCodec.of(C2SSelectCharacterPacket::write, C2SSelectCharacterPacket::read);

    private static void write(C2SSelectCharacterPacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.slot());
        buf.writeString(packet.characterName() != null ? packet.characterName() : "");
        buf.writeString(packet.skinUsername() != null ? packet.skinUsername() : "");
        buf.writeString(packet.classId() != null ? packet.classId() : "");
    }

    private static C2SSelectCharacterPacket read(PacketByteBuf buf) {
        return new C2SSelectCharacterPacket(buf.readInt(), buf.readString(), buf.readString(), buf.readString());
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}