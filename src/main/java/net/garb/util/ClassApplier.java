package net.garb.util;

import net.fabricmc.loader.api.FabricLoader;
import net.garb.character.CharacterClass;
import net.garb.compat.GarbLevelZCompat;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ClassApplier {

    public static void applyToNewCharacter(ServerPlayerEntity player, CharacterClass characterClass) {
        applyAttributes(player, characterClass);
        giveStartingItems(player, characterClass);
        applySkillPresets(player, characterClass);
    }

    public static void applyOnLogin(ServerPlayerEntity player, CharacterClass characterClass) {
        applyAttributes(player, characterClass);
    }

    private static void applyAttributes(ServerPlayerEntity player, CharacterClass characterClass) {
        for (var entry : characterClass.getAttributes().entrySet()) {
            Identifier attrId = Identifier.of(entry.getKey());
            var attribute = Registries.ATTRIBUTE.get(attrId);
            if (attribute == null) {
                continue;
            }
            EntityAttributeInstance instance = player.getAttributeInstance(Registries.ATTRIBUTE.getEntry(attrId).orElseThrow());
            if (instance != null) {
                instance.setBaseValue(entry.getValue());
            }
        }
    }

    private static void giveStartingItems(ServerPlayerEntity player, CharacterClass characterClass) {
        for (CharacterClass.StartItem startItem : characterClass.getStartingItems()) {
            Item item = Registries.ITEM.get(Identifier.of(startItem.item()));
            player.getInventory().insertStack(new ItemStack(item, startItem.count()));
        }
    }

    private static void applySkillPresets(ServerPlayerEntity player, CharacterClass characterClass) {
        if (characterClass.getSkillPresets().isEmpty()) {
            return;
        }
        if (!FabricLoader.getInstance().isModLoaded("levelz")) {
            return;
        }
        GarbLevelZCompat.applySkillPresets(player, characterClass.getSkillPresets());
    }

}
