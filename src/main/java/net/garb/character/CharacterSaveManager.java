package net.garb.character;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

public class CharacterSaveManager {

    public static final int SLOT_COUNT = 3;

    private static Path getSlotPath(MinecraftServer server, UUID playerUuid, int slot) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("characters").resolve(playerUuid.toString()).resolve("slot_" + slot + ".nbt");
    }

    public static CharacterData[] loadAll(MinecraftServer server, UUID playerUuid) {
        CharacterData[] slots = new CharacterData[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = load(server, playerUuid, i);
        }
        return slots;
    }

    public static CharacterData load(MinecraftServer server, UUID playerUuid, int slot) {
        Path path = getSlotPath(server, playerUuid, slot);
        if (!Files.exists(path)) {
            return new CharacterData();
        }

        try {
            NbtCompound nbt = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
            return CharacterData.fromNbt(nbt);
        } catch (IOException e) {
            e.printStackTrace();
            return new CharacterData();
        }
    }

    public static void save(MinecraftServer server, UUID playerUuid, int slot, CharacterData data) {
        Path path = getSlotPath(server, playerUuid, slot);
        try {
            Files.createDirectories(path.getParent());
            NbtIo.writeCompressed(data.toNbt(), path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void delete(MinecraftServer server, UUID playerUuid, int slot) {
        Path path = getSlotPath(server, playerUuid, slot);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isNameTaken(MinecraftServer server, String name) {
        if (name == null || name.isBlank()) {
            return false;
        }

        Path root = server.getSavePath(WorldSavePath.ROOT).resolve("characters");
        if (!Files.exists(root)) {
            return false;
        }

        try (Stream<Path> playerDirs = Files.list(root)) {
            for (Path playerDir : (Iterable<Path>) playerDirs::iterator) {
                if (!Files.isDirectory(playerDir)) {
                    continue;
                }
                for (int i = 0; i < SLOT_COUNT; i++) {
                    Path slotPath = playerDir.resolve("slot_" + i + ".nbt");
                    if (!Files.exists(slotPath)) {
                        continue;
                    }
                    try {
                        NbtCompound nbt = NbtIo.readCompressed(slotPath, NbtSizeTracker.ofUnlimitedBytes());
                        CharacterData data = CharacterData.fromNbt(nbt);
                        if (data.exists() && name.equalsIgnoreCase(data.getName())) {
                            return true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}