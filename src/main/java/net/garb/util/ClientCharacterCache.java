package net.garb.util;

import net.minecraft.client.util.SkinTextures;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientCharacterCache {

    private static final Map<UUID, String> NAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, SkinTextures> SKINS = new ConcurrentHashMap<>();

    public static void setName(UUID uuid, String name) {
        if (name != null && !name.isEmpty()) {
            NAMES.put(uuid, name);
        } else {
            NAMES.remove(uuid);
        }
    }

    public static Optional<String> getName(UUID uuid) {
        return Optional.ofNullable(NAMES.get(uuid));
    }

    public static void setSkin(UUID uuid, SkinTextures textures) {
        SKINS.put(uuid, textures);
    }

    public static Optional<SkinTextures> getSkin(UUID uuid) {
        return Optional.ofNullable(SKINS.get(uuid));
    }

    public static void clear(UUID uuid) {
        NAMES.remove(uuid);
        SKINS.remove(uuid);
    }

    public static void clearAll() {
        NAMES.clear();
        SKINS.clear();
    }
}