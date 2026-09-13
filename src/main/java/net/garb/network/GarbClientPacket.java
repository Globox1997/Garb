package net.garb.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.garb.character.CharacterClassRegistry;
import net.garb.network.packet.*;
import net.garb.screen.CreateCharacterScreen;
import net.garb.screen.GarbScreen;
import net.garb.util.ClientCharacterCache;
import net.garb.util.SkinLoader;
import net.minecraft.client.util.SkinTextures;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class GarbClientPacket {

    public static void init() {
        ClientConfigurationNetworking.registerGlobalReceiver(S2CCharacterDataPacket.ID, (packet, context) -> {
            context.client().execute(() -> {
                Consumer<C2SSelectCharacterPacket> onSelect = ClientConfigurationNetworking::send;
                context.client().setScreen(new GarbScreen(packet.slots(), onSelect));
            });
        });
        ClientConfigurationNetworking.registerGlobalReceiver(S2CClassDataPacket.ID, (packet, context) -> {
            context.client().execute(() -> {
                CharacterClassRegistry.clear();
                packet.classes().forEach(CharacterClassRegistry::register);
            });
        });
        ClientConfigurationNetworking.registerGlobalReceiver(S2CNameCheckResultPacket.ID, (packet, context) -> {
            context.client().execute(() -> {
                if (context.client().currentScreen instanceof CreateCharacterScreen screen) {
                    screen.onNameCheckResult(packet.name(), packet.taken());
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(S2CPlayerCosmeticsPacket.ID, (packet, context) -> {
            context.client().execute(() -> {
                ClientCharacterCache.setName(packet.playerId(), packet.name());

                if (packet.skinUsername() != null && !packet.skinUsername().isEmpty()) {
                    SkinLoader.loadForUsername(packet.skinUsername(), result -> context.client().execute(() -> {
                        if (result.success()) {
                            SkinTextures textures = new SkinTextures(
                                    result.textureId(), null, null, null,
                                    result.slim() ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE,
                                    true
                            );
                            ClientCharacterCache.setSkin(packet.playerId(), textures);
                        }
                    }));
                }
            });
        });
    }
}
