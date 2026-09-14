package net.garb.compat;

import net.garb.data.GarbLoader;
import net.levelz.access.LevelManagerAccess;
import net.levelz.level.LevelManager;
import net.levelz.level.PlayerSkill;
import net.levelz.level.Skill;
import net.levelz.util.LevelHelper;
import net.levelz.util.PacketHelper;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Optional;

public class GarbLevelZCompat {

    public static Optional<Skill> findSkillByKey(String key) {
        return LevelManager.SKILLS.values().stream().filter(skill -> skill.getKey().equals(key)).findFirst();
    }

    public static void applySkillPresets(ServerPlayerEntity player, Map<String, Integer> skillPresets) {
        LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();

        for (Map.Entry<String, Integer> entry : skillPresets.entrySet()) {
            Optional<Skill> skillOpt = findSkillByKey(entry.getKey());
            if (skillOpt.isEmpty()) {
                GarbLoader.LOGGER.warn("Skill preset '{}' does not match any known LevelZ skill.", entry.getKey());
                continue;
            }

            Skill skill = skillOpt.get();
            int level = Math.min(entry.getValue(), skill.getMaxLevel());

            PlayerSkill playerSkill = levelManager.getPlayerSkills().get(skill.getId());
            if (playerSkill != null) {
                playerSkill.setLevel(level);
            }

            LevelHelper.updateSkill(player, skill);
        }

        PacketHelper.updateSkills(player);
        PacketHelper.updatePlayerSkills(player, null);
        PacketHelper.updateLevels(player);
    }
}