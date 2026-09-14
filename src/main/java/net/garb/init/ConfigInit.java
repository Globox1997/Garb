package net.garb.init;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.garb.config.GarbConfig;

public class ConfigInit {

    public static GarbConfig CONFIG = new GarbConfig();

    public static void init() {
        AutoConfig.register(GarbConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(GarbConfig.class).getConfig();
    }

}