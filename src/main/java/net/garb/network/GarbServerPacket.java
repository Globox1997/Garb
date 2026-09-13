package net.garb.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.garb.character.CharacterData;
import net.garb.character.CharacterSaveManager;
import net.garb.character.GarbPlayerManager;
import net.garb.init.EventInit;
import net.garb.network.packet.C2SSelectCharacterPacket;
import net.garb.network.packet.S2CCharacterDataPacket;
import net.garb.network.packet.S2CClassDataPacket;

import java.util.UUID;

public class GarbServerPacket {

    public static void init() {
        PayloadTypeRegistry.configurationS2C().register(S2CClassDataPacket.ID, S2CClassDataPacket.CODEC);
        PayloadTypeRegistry.configurationS2C().register(S2CCharacterDataPacket.ID, S2CCharacterDataPacket.CODEC);
        PayloadTypeRegistry.configurationC2S().register(C2SSelectCharacterPacket.ID, C2SSelectCharacterPacket.CODEC);

        ServerConfigurationNetworking.registerGlobalReceiver(C2SSelectCharacterPacket.ID, (packet, context) -> context.server().execute(() -> {
                    int slot = packet.slot();
                    if (slot < 0 || slot >= CharacterSaveManager.SLOT_COUNT) {
                        return;
                    }

                    UUID uuid = context.networkHandler().getDebugProfile().getId();
                    CharacterData data = CharacterSaveManager.load(context.server(), uuid, slot);

                    if (!data.exists()) {
                        if (packet.characterName() != null && !packet.characterName().isEmpty()) {
                            data.setName(packet.characterName());
                        }
                        if (packet.skinUsername() != null && !packet.skinUsername().isEmpty()) {
                            data.setSkinUsername(packet.skinUsername());
                        }
                        if (packet.classId() != null && !packet.classId().isEmpty()) {
                            data.setClassId(packet.classId());
                        }

                        CharacterSaveManager.save(context.server(), uuid, slot, data);
                    }

                    GarbPlayerManager.saveSelection(uuid, slot);
                    context.networkHandler().completeTask(EventInit.CharacterSelectionTask.KEY);
                })
        );
    }
}
