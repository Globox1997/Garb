package net.garb;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.garb.init.RenderInit;
import net.garb.network.GarbClientPacket;

@Environment(EnvType.CLIENT)
public class GarbClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        GarbClientPacket.init();
        RenderInit.init();
    }
}
