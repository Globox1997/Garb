package net.garb.character;

import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CharacterClassRegistry {

    private static final Map<Identifier, CharacterClass> CLASSES = new ConcurrentHashMap<>();
    private static final Comparator<CharacterClass> ORDER = Comparator.comparingInt(CharacterClass::getOrder).thenComparing(characterClass -> characterClass.getId().toString());

    public static void clear() {
        CLASSES.clear();
    }

    public static void register(CharacterClass garbClass) {
        CLASSES.put(garbClass.getId(), garbClass);
    }

    public static Optional<CharacterClass> get(Identifier id) {
        return Optional.ofNullable(CLASSES.get(id));
    }

    public static List<CharacterClass> getAll() {
        return CLASSES.values().stream().sorted(ORDER).toList();
    }
}
