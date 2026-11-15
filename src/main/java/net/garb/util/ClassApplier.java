package net.garb.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.garb.GarbMain;
import net.garb.character.CharacterClass;
import net.garb.compat.GarbLevelZCompat;
import net.garb.data.GarbLoader;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class ClassApplier {

    public static final Identifier CLASS_MODIFIER_ID = GarbMain.identifierOf("class");
    private static final double EPSILON = 0.0001;

    public static void applyToNewCharacter(ServerPlayerEntity player, CharacterClass characterClass) {
        applyAttributes(player, characterClass);
        giveStartingItems(player, characterClass);
        applySkillPresets(player, characterClass);
        player.setHealth(player.getMaxHealth());
    }

    public static void applyOnLogin(ServerPlayerEntity player, CharacterClass characterClass) {
        applyAttributes(player, characterClass);
    }

    public static double getBonus(EntityAttribute attribute, double classValue) {
        return classValue - attribute.getDefaultValue();
    }

    private static void applyAttributes(ServerPlayerEntity player, CharacterClass characterClass) {
        for (var entry : characterClass.getAttributes().entrySet()) {
            Optional<RegistryEntry.Reference<EntityAttribute>> attributeEntry = Registries.ATTRIBUTE.getEntry(Identifier.of(entry.getKey()));
            if (attributeEntry.isEmpty()) {
                continue;
            }
            EntityAttributeInstance instance = player.getAttributeInstance(attributeEntry.get());
            if (instance == null) {
                continue;
            }
            EntityAttribute attribute = attributeEntry.get().value();
            double classValue = entry.getValue();
            double defaultValue = attribute.getDefaultValue();

            if (Math.abs(classValue - defaultValue) > EPSILON && Math.abs(instance.getBaseValue() - classValue) < EPSILON) {
                instance.setBaseValue(defaultValue);
            }

            instance.removeModifier(CLASS_MODIFIER_ID);
            double bonus = getBonus(attribute, classValue);
            if (Math.abs(bonus) > EPSILON) {
                instance.addPersistentModifier(new EntityAttributeModifier(CLASS_MODIFIER_ID, bonus, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    private static void giveStartingItems(ServerPlayerEntity player, CharacterClass characterClass) {
        ItemStringReader itemStringReader = new ItemStringReader(player.getRegistryManager());
        for (CharacterClass.StartItem startItem : characterClass.getStartingItems()) {
            try {
                ItemStringReader.ItemResult result = itemStringReader.consume(new StringReader(startItem.item()));
                player.getInventory().insertStack(new ItemStack(result.item(), startItem.count(), result.components()));
            } catch (CommandSyntaxException e) {
                GarbLoader.LOGGER.warn("Could not give starting item {} of class {}: {}", startItem.item(), characterClass.getId(), e.getMessage());
            }
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
