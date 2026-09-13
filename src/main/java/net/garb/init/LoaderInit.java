package net.garb.init;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.garb.character.CharacterClassRegistry;
import net.garb.character.CharacterData;
import net.garb.character.CharacterSaveManager;
import net.garb.character.GarbPlayerManager;
import net.garb.data.GarbLoader;
import net.garb.util.ClassApplier;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class LoaderInit {

    public static void init() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new GarbLoader());

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    int slot = GarbPlayerManager.getSelection(player.getUuid());
                    if (slot == -1) {
                        continue;
                    }
                    CharacterData data = CharacterSaveManager.load(server, player.getUuid(), slot);
                    if (data.getClassId().isEmpty()) {
                        continue;
                    }
                    CharacterClassRegistry.get(Identifier.of(data.getClassId())).ifPresent(garbClass -> ClassApplier.applyOnLogin(player, garbClass));
                }
                GarbLoader.LOGGER.info("Reloaded and reapplied classes");
            }
        });
    }

}
