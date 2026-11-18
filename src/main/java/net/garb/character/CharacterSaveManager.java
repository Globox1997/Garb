package net.garb.character;

import com.mojang.authlib.GameProfile;
import net.garb.data.GarbLoader;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class CharacterSaveManager {

    public static final int SLOT_COUNT = 3;
    public static final int NAME_MAX_LENGTH = 32;
    private static final List<String> VANILLA_PLAYER_SUFFIXES = List.of(".dat", ".dat_old");

    private record VanillaPlayer(NbtCompound nbt, long lastPlayed) {
    }

    private static Path getPlayerPath(MinecraftServer server, UUID playerUuid) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("characters").resolve(playerUuid.toString());
    }

    private static Path getSlotPath(MinecraftServer server, UUID playerUuid, int slot) {
        return getPlayerPath(server, playerUuid).resolve("slot_" + slot + ".nbt");
    }

    public static void importVanillaPlayer(MinecraftServer server, GameProfile profile) {
        UUID uuid = profile.getId();
        if (Files.exists(getPlayerPath(server, uuid))) {
            return;
        }
        VanillaPlayer vanillaPlayer = loadVanillaPlayer(server, profile);
        if (vanillaPlayer == null) {
            return;
        }
        String name = profile.getName();
        for (int i = 2; isNameTaken(server, name); i++) {
            name = profile.getName() + "_" + i;
        }
        save(server, uuid, 0, CharacterData.fromVanillaPlayer(vanillaPlayer.nbt(), name, vanillaPlayer.lastPlayed()));
        GarbLoader.LOGGER.info("Imported existing player data of {} as character {}", profile.getName(), name);
    }

    private static VanillaPlayer loadVanillaPlayer(MinecraftServer server, GameProfile profile) {
        NbtCompound hostData = server.getSaveProperties().getPlayerData();
        if (server.isHost(profile) && hostData != null) {
            return new VanillaPlayer(hostData.copy(), getLastModified(server.getSavePath(WorldSavePath.LEVEL_DAT)));
        }
        Path playerDataDir = server.getSavePath(WorldSavePath.PLAYERDATA);
        for (String suffix : VANILLA_PLAYER_SUFFIXES) {
            Path path = playerDataDir.resolve(profile.getId().toString() + suffix);
            if (!Files.exists(path)) {
                continue;
            }
            try {
                NbtCompound nbt = NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
                NbtCompound fixed = DataFixTypes.PLAYER.update(server.getDataFixer(), nbt, NbtHelper.getDataVersion(nbt, -1));
                return new VanillaPlayer(fixed, getLastModified(path));
            } catch (IOException e) {
                GarbLoader.LOGGER.warn("Could not read player data {}: {}", path, e.getMessage());
            }
        }
        return null;
    }

    private static long getLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return System.currentTimeMillis();
        }
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