package net.garb;

import net.fabricmc.api.ModInitializer;
import net.garb.init.EventInit;
import net.garb.init.LoaderInit;
import net.garb.network.GarbServerPacket;
import net.minecraft.util.Identifier;

public class GarbMain implements ModInitializer {

    @Override
    public void onInitialize() {
        GarbServerPacket.init();
        EventInit.init();
        LoaderInit.init();
    }

    public static Identifier identifierOf(String name) {
        return Identifier.of("garb", name);
    }
}
