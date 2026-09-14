# Garb
Garb adds the mechanic of choosing classes.

### Installation
Garb is a mod built for the [Fabric Loader](https://fabricmc.net/). It requires [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) to be installed separately; all other dependencies are installed with the mod.

### License
#### Source Code
The source code in this repository is licensed under the MIT License.
See the LICENSE file for details.

#### Assets
All assets (including but not limited to images, audio, models, and other media)
are **All Rights Reserved** and may not be used, modified, or redistributed
without explicit permission from the copyright holder.

### Datapacks
Classes can be defined in a datapack.\
Folder path: ```data\garb\classes\YOURFILE.json```\
Example:

```json
{
  "id": "garb:warrior",
  "display_name": "Warrior",
  "icon": "garb:textures/class/warrior.png",
  "description": "A tough melee fighter with high health and armor.",
  "attributes": {
    "minecraft:generic.max_health": 30.0,
    "minecraft:generic.attack_damage": 4.0,
    "minecraft:generic.armor": 6.0,
    "minecraft:generic.armor_toughness": 2.0,
    "minecraft:generic.knockback_resistance": 0.25
  },
  "starting_items": [
    {
      "item": "minecraft:iron_sword",
      "count": 1
    },
    {
      "item": "minecraft:bread",
      "count": 8
    }
  ],
  "skills": {
    "constitution": 5,
    "melee": 2
  }
}
```

The skills part is a [LevelZ](https://github.com/Globox1997/LevelZ) integration. It applies skill presets.