package net.garb.character;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public class CharacterClass {

    private final Identifier id;
    private final String displayName;
    private final Identifier icon;
    private final String description;
    private final Map<String, Double> attributes;
    private final List<StartItem> startingItems;
    private final Map<String, Integer> skillPresets;

    public CharacterClass(Identifier id, String displayName, Identifier icon, String description, Map<String, Double> attributes, List<StartItem> startingItems, Map<String, Integer> skillPresets) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.attributes = attributes;
        this.startingItems = startingItems;
        this.skillPresets = skillPresets;
    }

    public record StartItem(String item, int count) {
    }

    public Identifier getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Identifier getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Double> getAttributes() {
        return attributes;
    }

    public List<StartItem> getStartingItems() {
        return startingItems;
    }

    public Map<String, Integer> getSkillPresets() {
        return skillPresets;
    }
}
