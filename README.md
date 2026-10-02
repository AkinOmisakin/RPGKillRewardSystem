# RPGKillRewardSystem

A configurable kill-reward plugin for [Paper](https://papermc.io/) servers. When a player kills a mob, every
reward set up for that mob is rolled: custom items, XP, console commands and messages. It is built for RPG
servers, where you want a Goblin King to drop a named crown and a zombie to drop coins.

<!--
  SCREENSHOT / GIF: add an image to docs/ (for example docs/drops.png or docs/demo.gif),
  then replace this comment with:   ![Reward drop](docs/drops.png)
-->

## Features

- **Per-mob reward tables**: each rule targets an entity type (`ZOMBIE`, `SKELETON`, ...) and holds any number of
  rewards with their own drop chance.
- **Custom items**: material, random amount (`"1-3"`), MiniMessage name and lore, and enchantments (levels above
  vanilla maximum are allowed).
- **XP, commands and messages**: a reward can give an item, XP, run console commands (with `%player%`, `%mob%`,
  `%world%`, `%x%`, `%y%`, `%z%`) and send a message, all at once.
- **Named mobs / bosses**: match mobs by custom name (works with name tags or other RPG plugins), so a "Goblin King"
  zombie can have different loot from a normal zombie.
- **Looting support**: each Looting level raises every reward's chance.
- **Direct or ground drops**: put items straight into the killer's inventory (overflow drops at their feet) or drop
  them where the mob died. Vanilla drops and XP can be wiped per rule.
- Bad config never crashes the plugin: invalid entries are skipped with a console warning.

## Requirements

- Paper **26.2** (or newer, with `api-version: 26.2`)
- Java **25**

## Installation

1. Download the jar from the Releases page, or build it yourself (see below).
2. Put it in your server's `plugins/` folder and start the server.
3. Edit `plugins/RPGKillRewardSystem/config.yml`, then run `/killreward reload`.

## Commands

Alias: `/kr`

| Command | Description |
| --- | --- |
| `/killreward reload` | Re-read `config.yml` without restarting the server. |
| `/killreward list` | Show every configured mob rule and how many rewards it has. |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `killreward.admin` | op | Use `/killreward reload` and `/killreward list`. |

Rewards are given to every player who lands the killing blow. No permission is needed to receive them.

## Configuration

```yaml
settings:
  give-directly: true     # true = into the killer's inventory, false = drop where the mob died
  looting-bonus: 0.25     # each Looting level multiplies chances by (1 + level * 0.25)
  send-messages: true

mobs:
  zombie:
    type: ZOMBIE
    rewards:
      rotten_coins:
        chance: 60.0                 # percent, 0-100
        item:
          material: GOLD_NUGGET
          amount: "1-3"
          name: "<gold>Zombie Coin"
          lore:
            - "<gray>Dropped by a zombie."
        exp: "2-5"
      cursed_blade:
        chance: 2.5
        item:
          material: IRON_SWORD
          name: "<dark_red><bold>Cursed Blade"
          enchantments:
            sharpness: 3
            unbreaking: 2
        message: "<dark_red>The zombie dropped a <bold>Cursed Blade</bold>!"

  # An RPG boss matched by custom name:
  goblin_king:
    type: ZOMBIE
    name: "Goblin King"              # only zombies whose custom name contains this
    clear-vanilla-drops: true
    rewards:
      broadcast:
        chance: 100.0
        commands:
          - "say %player% has slain the Goblin King in %world%!"
```

The default [`config.yml`](src/main/resources/config.yml) documents every field.

## Companion plugin

Pair it with [CustomShopGUI](https://github.com/AkinOmisakin/custom-shop-GUI): NPC merchants where players spend
the coins and materials this plugin drops. The two don't depend on each other.

## Building

```bash
./gradlew build
```

On Windows use `gradlew.bat build`. The jar is written to `build/libs/`.
To try it on a local test server, run `./gradlew runServer`.
