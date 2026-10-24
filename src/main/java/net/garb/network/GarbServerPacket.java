package net.garb.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.garb.character.CharacterClassRegistry;
import net.garb.character.CharacterData;
import net.garb.character.CharacterSaveManager;
import net.garb.character.GarbActiveCharacterCache;
import net.garb.character.GarbPlayerManager;
import net.garb.init.EventInit;
import net.garb.network.packet.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class GarbServerPacket {

    private static final Set<ServerConfigurationNetworkHandler> COMPLETED_SELECTIONS = Collections.newSetFromMap(new WeakHashMap<>());

    public static void init() {
        PayloadTypeRegistry.configurationS2C().register(S2CClassDataPacket.ID, S2CClassDataPacket.CODEC);
        PayloadTypeRegistry.configurationS2C().register(S2CCharacterDataPacket.ID, S2CCharacterDataPacket.CODEC);
        PayloadTypeRegistry.configurationS2C().register(S2CNameCheckResultPacket.ID, S2CNameCheckResultPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(S2CPlayerCosmeticsPacket.ID, S2CPlayerCosmeticsPacket.CODEC);
        PayloadTypeRegistry.configurationC2S().register(C2SSelectCharacterPacket.ID, C2SSelectCharacterPacket.CODEC);
        PayloadTypeRegistry.configurationC2S().register(C2SCheckNamePacket.ID, C2SCheckNamePacket.CODEC);

        ServerConfigurationNetworking.registerGlobalReceiver(C2SCheckNamePacket.ID, (packet, context) -> context.server().execute(() -> {
            boolean taken = CharacterSaveManager.isNameTaken(context.server(), packet.name());
            ServerConfigurationNetworking.send(context.networkHandler(), new S2CNameCheckResultPacket(packet.name(), taken));
        }));

        ServerConfigurationNetworking.registerGlobalReceiver(C2SSelectCharacterPacket.ID, (packet, context) -> context.server().execute(() -> {
                    ServerConfigurationNetworkHandler handler = context.networkHandler();
                    if (COMPLETED_SELECTIONS.contains(handler)) {
                        return;
                    }

                    int slot = packet.slot();
                    if (slot < 0 || slot >= CharacterSaveManager.SLOT_COUNT) {
                        return;
                    }

                    UUID uuid = handler.getDebugProfile().getId();
                    CharacterData data = CharacterSaveManager.load(context.server(), uuid, slot);

                    if (!data.exists()) {
                        String errorKey = validateNewCharacter(context.server(), packet);
                        if (errorKey != null) {
                            ServerConfigurationNetworking.send(handler, S2CCharacterDataPacket.from(CharacterSaveManager.loadAll(context.server(), uuid), errorKey));
                            return;
                        }

                        data.setName(packet.characterName().trim());
                        if (packet.skinUsername() != null && !packet.skinUsername().isEmpty()) {
                            data.setSkinUsername(packet.skinUsername());
                        }
                        if (packet.classId() != null && !packet.classId().isEmpty()) {
                            data.setClassId(packet.classId());
                        }

                        CharacterSaveManager.save(context.server(), uuid, slot, data);
                    }

                    COMPLETED_SELECTIONS.add(handler);
                    GarbPlayerManager.saveSelection(uuid, slot);
                    handler.completeTask(EventInit.CharacterSelectionTask.KEY);
                })
        );
    }

    private static String validateNewCharacter(MinecraftServer server, C2SSelectCharacterPacket packet) {
        String name = packet.characterName() != null ? packet.characterName().trim() : "";
        if (name.isEmpty()) {
            return "screen.garb.error.name_required";
        }
        if (name.length() > CharacterSaveManager.NAME_MAX_LENGTH) {
            return "screen.garb.error.name_too_long";
        }
        if (CharacterSaveManager.isNameTaken(server, name)) {
            return "screen.garb.error.name_taken";
        }
        if (packet.skinUsername() != null && packet.skinUsername().length() > CharacterSaveManager.NAME_MAX_LENGTH) {
            return "screen.garb.error.invalid_skin";
        }
        if (packet.classId() != null && !packet.classId().isEmpty()) {
            Identifier classId = Identifier.tryParse(packet.classId());
            if (classId == null || CharacterClassRegistry.get(classId).isEmpty()) {
                return "screen.garb.error.invalid_class";
            }
        }
        return null;
    }

    public static void syncOnJoin(ServerPlayerEntity player, String name, String skinUsername) {
        S2CPlayerCosmeticsPacket selfPacket = new S2CPlayerCosmeticsPacket(player.getUuid(), name, skinUsername);

        for (ServerPlayerEntity other : player.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(other, selfPacket);

            if (other == player) {
                continue;
            }
            GarbActiveCharacterCache.get(other.getUuid()).ifPresent(otherActive ->
                    ServerPlayNetworking.send(player, new S2CPlayerCosmeticsPacket(other.getUuid(), otherActive.name(), otherActive.skinUsername()))
            );
        }
    }
}
