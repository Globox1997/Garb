package net.garb.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.garb.character.CharacterSaveManager;
import net.garb.character.GarbPlayerManager;
import net.garb.init.EventInit;
import net.garb.network.packet.C2SSelectCharacterPacket;
import net.garb.network.packet.S2CCharacterDataPacket;

public class GarbServerPacket {

    public static void init() {
        PayloadTypeRegistry.configurationS2C().register(S2CCharacterDataPacket.ID, S2CCharacterDataPacket.CODEC);
        PayloadTypeRegistry.configurationC2S().register(C2SSelectCharacterPacket.ID, C2SSelectCharacterPacket.CODEC);

        ServerConfigurationNetworking.registerGlobalReceiver(C2SSelectCharacterPacket.ID, (packet, context) -> context.server().execute(() -> {
                    int slot = packet.slot();
                    if (slot < 0 || slot >= CharacterSaveManager.SLOT_COUNT) {
                        return;
                    }

                    GarbPlayerManager.saveSelection(context.networkHandler().getDebugProfile().getId(), slot);
                    context.networkHandler().completeTask(EventInit.CharacterSelectionTask.KEY);
                })
        );
    }
}
