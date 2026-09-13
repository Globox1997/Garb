package net.garb.character;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GarbActiveCharacterCache {

    public record ActiveCharacter(String name, String skinUsername) {
    }

    private static final Map<UUID, ActiveCharacter> ACTIVE = new ConcurrentHashMap<>();

    public static void set(UUID uuid, String name, String skinUsername) {
        ACTIVE.put(uuid, new ActiveCharacter(name != null ? name : "", skinUsername != null ? skinUsername : ""));
    }

    public static Optional<ActiveCharacter> get(UUID uuid) {
        return Optional.ofNullable(ACTIVE.get(uuid));
    }

    public static void clear(UUID uuid) {
        ACTIVE.remove(uuid);
    }
}