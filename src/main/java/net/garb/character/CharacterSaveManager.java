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
}