package net.garb.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.garb.character.CharacterClassRegistry;
import net.garb.network.packet.C2SSelectCharacterPacket;
import net.garb.network.packet.S2CCharacterDataPacket;
import net.garb.network.packet.S2CClassDataPacket;
import net.garb.network.packet.S2CNameCheckResultPacket;
import net.garb.screen.CreateCharacterScreen;
import net.garb.screen.GarbScreen;

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
    }
}
