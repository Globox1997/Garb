package net.garb.character;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;

public class CharacterData {

    private String name;
    private String skinUsername;
    private String classId;
    private boolean exists;
    private long lastPlayed;
    private float maxHealth;

    private NbtCompound playerNbt;

    public CharacterData() {
        this.exists = false;
        this.name = "";
        this.skinUsername = "";
        this.classId = "";
    }

    public void captureFromPlayer(ServerPlayerEntity player) {
        this.exists = true;
        this.lastPlayed = System.currentTimeMillis();
        this.maxHealth = player.getMaxHealth();

        this.playerNbt = new NbtCompound();
        player.writeNbt(this.playerNbt);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putBoolean("exists", exists);
        nbt.putString("name", name != null ? name : "");
        nbt.putString("skinUsername", skinUsername != null ? skinUsername : "");
        nbt.putString("classId", classId != null ? classId : "");
        nbt.putLong("lastPlayed", lastPlayed);
        nbt.putFloat("maxHealth", maxHealth);
        nbt.put("playerNbt", playerNbt != null ? playerNbt : new NbtCompound());

        return nbt;
    }


    public NbtCompound getPlayerNbt() {
        NbtCompound nbt = (this.playerNbt != null ? this.playerNbt.copy() : new NbtCompound());
        nbt.putBoolean("exists", exists);
        nbt.putString("name", name != null ? name : "");
        nbt.putString("skinUsername", skinUsername != null ? skinUsername : "");
        nbt.putString("classId", classId != null ? classId : "");
        nbt.putLong("lastPlayed", lastPlayed);
        return nbt;
    }

    public static CharacterData fromNbt(NbtCompound nbt) {
        CharacterData data = new CharacterData();
        data.exists = nbt.getBoolean("exists");
        data.name = nbt.contains("name") ? nbt.getString("name") : "";
        data.skinUsername = nbt.contains("skinUsername") ? nbt.getString("skinUsername") : "";
        data.classId = nbt.contains("classId") ? nbt.getString("classId") : "";
        data.lastPlayed = nbt.getLong("lastPlayed");
        data.maxHealth = nbt.getFloat("maxHealth");
        data.playerNbt = nbt.getCompound("playerNbt");
        return data;
    }

    public boolean exists() {
        return exists;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSkinUsername() {
        return skinUsername;
    }

    public void setSkinUsername(String skinUsername) {
        this.skinUsername = skinUsername;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public float getHealth() {
        if (playerNbt == null) return 20f;
        if (playerNbt.contains("Health")) return playerNbt.getFloat("Health");
        return 20f;
    }

    public float getMaxHealth() {
        if (maxHealth > 0) return maxHealth;
        if (playerNbt == null) return 20f;
        NbtList attributes = playerNbt.getList("attributes", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < attributes.size(); i++) {
            NbtCompound attribute = attributes.getCompound(i);
            if ("minecraft:generic.max_health".equals(attribute.getString("id"))) {
                return (float) attribute.getDouble("base");
            }
        }
        return 20f;
    }

    public int getFoodLevel() {
        if (playerNbt == null) return 20;
        if (playerNbt.contains("foodLevel")) return playerNbt.getInt("foodLevel");
        return 20;
    }

    public int getExperienceLevel() {
        if (playerNbt == null) return 0;
        if (playerNbt.contains("XpLevel")) return playerNbt.getInt("XpLevel");
        return 0;
    }

    public void setHealth(float health) {
        if (playerNbt != null) playerNbt.putFloat("Health", health);
    }
}