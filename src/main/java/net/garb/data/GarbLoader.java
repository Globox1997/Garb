package net.garb.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.garb.GarbMain;
import net.garb.character.CharacterClass;
import net.garb.character.CharacterClassRegistry;
import net.garb.compat.GarbLevelZCompat;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class GarbLoader implements SimpleSynchronousResourceReloadListener {

    public static final Logger LOGGER = LogManager.getLogger("Garb");

    @Override
    public Identifier getFabricId() {
        return GarbMain.identifierOf("garb_loader");
    }

    @Override
    public void reload(ResourceManager resourceManager) {
        CharacterClassRegistry.clear();

        resourceManager.findResources("classes", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (InputStream stream = resource.getInputStream()) {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

                CharacterClass garbClass = parse(json);
                CharacterClassRegistry.register(garbClass);
            } catch (Exception e) {
                LOGGER.error("Failed to load class file {}: {}", id, e.toString());
            }
        });
        LOGGER.info("Loaded {} classes total", CharacterClassRegistry.getAll().size());
    }

    private CharacterClass parse(JsonObject json) {
        String classId = json.get("id").getAsString();
        Identifier id = Identifier.of(classId);
        String displayName = json.get("display_name").getAsString();
        Identifier icon = Identifier.of(json.get("icon").getAsString());

        String description = json.has("description") ? json.get("description").getAsString() : "";

        Map<String, Double> attributes = new HashMap<>();
        if (json.has("attributes")) {
            for (Map.Entry<String, JsonElement> attr : json.getAsJsonObject("attributes").entrySet()) {

                Optional<RegistryEntry.Reference<EntityAttribute>> entityAttribute = Registries.ATTRIBUTE.getEntry(Identifier.of(attr.getKey()));
                if (entityAttribute.isEmpty()) {
                    LOGGER.warn("Attribute {} is not a usable attribute in class {}.", attr.getKey(), classId);
                    continue;
                }
                attributes.put(attr.getKey(), attr.getValue().getAsDouble());
            }
        }

        List<CharacterClass.StartItem> startingItems = new ArrayList<>();
        if (json.has("starting_items")) {
            for (JsonElement el : json.getAsJsonArray("starting_items")) {
                JsonObject obj = el.getAsJsonObject();
                String item = obj.get("item").getAsString();
                if (Registries.ITEM.get(Identifier.of(item)).getDefaultStack().isEmpty()) {
                    LOGGER.warn("Item {} is an invalid item id in class {}.", item, classId);
                    continue;
                }
                int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                startingItems.add(new CharacterClass.StartItem(item, count));
            }
        }

        Map<String, Integer> skillPresets = new HashMap<>();
        if (json.has("skills")) {
            boolean levelZLoaded = FabricLoader.getInstance().isModLoaded("levelz");

            if (levelZLoaded) {
                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("skills").entrySet()) {
                    skillPresets.put(entry.getKey(), entry.getValue().getAsInt());
                }
            }
        }

        return new CharacterClass(id, displayName, icon, description, attributes, startingItems, skillPresets);
    }
}
