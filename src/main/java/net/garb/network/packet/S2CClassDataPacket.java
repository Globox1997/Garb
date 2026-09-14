package net.garb.network.packet;

import net.garb.GarbMain;
import net.garb.character.CharacterClass;
import net.garb.character.CharacterClassRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record S2CClassDataPacket(List<CharacterClass> classes) implements CustomPayload {

    public static final CustomPayload.Id<S2CClassDataPacket> ID = new CustomPayload.Id<>(GarbMain.identifierOf("class_data"));

    public static final PacketCodec<PacketByteBuf, S2CClassDataPacket> CODEC = PacketCodec.of(S2CClassDataPacket::write, S2CClassDataPacket::read);

    private static void write(S2CClassDataPacket packet, PacketByteBuf buf) {
        buf.writeInt(packet.classes.size());
        for (CharacterClass characterClass : packet.classes) {
            buf.writeString(characterClass.getId().toString());
            buf.writeString(characterClass.getDisplayName());
            buf.writeString(characterClass.getIcon().toString());
            buf.writeString(characterClass.getDescription());

            buf.writeInt(characterClass.getAttributes().size());
            for (Map.Entry<String, Double> attr : characterClass.getAttributes().entrySet()) {
                buf.writeString(attr.getKey());
                buf.writeDouble(attr.getValue());
            }
            buf.writeInt(characterClass.getStartingItems().size());
            for (CharacterClass.StartItem item : characterClass.getStartingItems()) {
                buf.writeString(item.item());
                buf.writeInt(item.count());
            }
            buf.writeInt(characterClass.getSkillPresets().size());
            for (Map.Entry<String, Integer> skill : characterClass.getSkillPresets().entrySet()) {
                buf.writeString(skill.getKey());
                buf.writeInt(skill.getValue());
            }
        }
    }

    private static S2CClassDataPacket read(PacketByteBuf buf) {
        int count = buf.readInt();
        List<CharacterClass> classes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Identifier id = Identifier.of(buf.readString());
            String displayName = buf.readString();
            Identifier icon = Identifier.of(buf.readString());
            String description = buf.readString();

            int attrCount = buf.readInt();
            Map<String, Double> attributes = new HashMap<>();
            for (int j = 0; j < attrCount; j++) {
                attributes.put(buf.readString(), buf.readDouble());
            }
            int itemCount = buf.readInt();
            List<CharacterClass.StartItem> items = new ArrayList<>();
            for (int j = 0; j < itemCount; j++) {
                items.add(new CharacterClass.StartItem(buf.readString(), buf.readInt()));
            }
            int skillCount = buf.readInt();
            Map<String, Integer> skillPresets = new HashMap<>();
            for (int k = 0; k < skillCount; k++) {
                skillPresets.put(buf.readString(), buf.readInt());
            }

            classes.add(new CharacterClass(id, displayName, icon, description, attributes, items, skillPresets));
        }
        return new S2CClassDataPacket(classes);
    }

    public static S2CClassDataPacket fromRegistry() {
        return new S2CClassDataPacket(new ArrayList<>(CharacterClassRegistry.getAll()));
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}