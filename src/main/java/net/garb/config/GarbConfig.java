package net.garb.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "garb")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class GarbConfig implements ConfigData {

    public boolean defaultClasses = true;
}