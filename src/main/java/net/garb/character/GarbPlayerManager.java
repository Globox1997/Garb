package net.garb.character;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GarbPlayerManager {

    private static final Map<UUID, Integer> SLOTS = new ConcurrentHashMap<>();

    public static void saveSelection(UUID uuid, int slot) {
        SLOTS.put(uuid, slot);
    }

    public static int getSelection(UUID uuid) {
        return SLOTS.getOrDefault(uuid, -1);
    }

    public static int getAndRemoveSelection(UUID uuid) {
        Integer removed = SLOTS.remove(uuid);
        return removed != null ? removed : -1;
    }

    public static boolean hasSelection(UUID uuid) {
        return SLOTS.containsKey(uuid);
    }
}