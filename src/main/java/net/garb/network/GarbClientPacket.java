package net.garb.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.garb.network.packet.C2SSelectCharacterPacket;
import net.garb.network.packet.S2CCharacterDataPacket;
import net.garb.screen.GarbScreen;

@Environment(EnvType.CLIENT)
public class GarbClientPacket {

    public static void init() {
        ClientConfigurationNetworking.registerGlobalReceiver(S2CCharacterDataPacket.ID, (packet, context) -> {
            context.client().execute(() -> {
                context.client().setScreen(new GarbScreen(context.networkHandler(), packet.slots(), (selectedId) -> {

                    ClientConfigurationNetworking.send(new C2SSelectCharacterPacket(selectedId));
                }));
            });
        });
    }
}
