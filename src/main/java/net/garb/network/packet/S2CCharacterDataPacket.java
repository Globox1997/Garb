package net.garb.network.packet;

import net.garb.GarbMain;
import net.garb.character.CharacterData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;

public record S2CCharacterDataPacket(List<SlotInfo> slots, String errorKey) implements CustomPayload {

    public static final CustomPayload.Id<S2CCharacterDataPacket> ID = new CustomPayload.Id<>(GarbMain.identifierOf("character_data"));

    public static final PacketCodec<PacketByteBuf, S2CCharacterDataPacket> CODEC = PacketCodec.of(S2CCharacterDataPacket::write, S2CCharacterDataPacket::read);

    public record SlotInfo(boolean exists, String name, String skinUsername, long lastPlayed, float health, float maxHealth, int foodLevel, int xpLevel) {
    }


    private static void write(S2CCharacterDataPacket packet, PacketByteBuf buf) {
        buf.writeString(packet.errorKey() != null ? packet.errorKey() : "");
        buf.writeInt(packet.slots.size());
        for (SlotInfo slot : packet.slots) {
            buf.writeBoolean(slot.exists());
            buf.writeString(slot.name() != null ? slot.name() : "");
            buf.writeString(slot.skinUsername() != null ? slot.skinUsername() : "");
            buf.writeLong(slot.lastPlayed());
            buf.writeFloat(slot.health());
            buf.writeFloat(slot.maxHealth());
            buf.writeInt(slot.foodLevel());
            buf.writeInt(slot.xpLevel());
        }
    }

    private static S2CCharacterDataPacket read(PacketByteBuf buf) {
        String errorKey = buf.readString();
        int count = buf.readInt();
        List<SlotInfo> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            boolean exists = buf.readBoolean();
            String name = buf.readString();
            String skinUsername = buf.readString();
            long lastPlayed = buf.readLong();
            float health = buf.readFloat();
            float maxHealth = buf.readFloat();
            int foodLevel = buf.readInt();
            int xpLevel = buf.readInt();
            slots.add(new SlotInfo(exists, name, skinUsername, lastPlayed, health, maxHealth, foodLevel, xpLevel));
        }
        return new S2CCharacterDataPacket(slots, errorKey);
    }

    public static S2CCharacterDataPacket from(CharacterData[] dataSlots) {
        return from(dataSlots, "");
    }

    public static S2CCharacterDataPacket from(CharacterData[] dataSlots, String errorKey) {
        List<SlotInfo> slots = new ArrayList<>();
        for (CharacterData data : dataSlots) {
            slots.add(new SlotInfo(data.exists(), data.getName(), data.getSkinUsername(), data.getLastPlayed(), data.getHealth(), data.getMaxHealth(), data.getFoodLevel(), data.getExperienceLevel()));
        }
        return new S2CCharacterDataPacket(slots, errorKey);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}