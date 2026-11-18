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
  "order": 1,
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
    },
    {
      "item": "minecraft:potion[potion_contents={potion:'minecraft:strength'}]",
      "count": 1
    }
  ],
  "skills": {
    "constitution": 5,
    "melee": 2
  }
}
```

The skills part is a [LevelZ](https://github.com/Globox1997/LevelZ) integration. It applies skill presets.

#### Class order
The optional `order` field sets the position of the class on the create character screen.\
Classes with a lower order are shown first, classes without an order use `0` and classes with the same order are sorted by their id.

#### Starting items with components
The `item` field of a starting item accepts a plain item id or an item with components, written like in the `/give` command.\
The components are added to the item when the character gets created and the item name is shown on the create character screen.\
Items which can't be parsed are skipped and logged.

```json
"starting_items": [
  {
    "item": "minecraft:potion[potion_contents={potion:'minecraft:strength'}]",
    "count": 1
  },
  {
    "item": "minecraft:iron_sword[enchantments={levels:{'minecraft:sharpness':2}},custom_name='\"Old Blade\"']",
    "count": 1
  },
  {
    "item": "minecraft:leather_chestplate[dyed_color={rgb:16711680}]",
    "count": 1
  }
]
```