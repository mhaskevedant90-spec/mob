# Mob Fusion (Fabric, Minecraft Java 1.21.1)

Build: install JDK 21, then `gradle build` (or open the folder in IntelliJ IDEA and run `genSources` / `runClient`).
The jar appears in `build/libs/`. Needs Fabric Loader + Fabric API.

## How it plays
- Craft: 4 cobblestone + 2 pistons -> 2 Mob Fusion Presses. 4 diamonds + 2 presses -> 2 Diamond Presses.
- Place two presses facing each other (max 8 blocks apart), put exactly 2 mobs between them.
- Put a button next to a press and push it. Pistons close, the hybrid appears.
- Mobs with 40+ HP (iron golem, warden...) need two Diamond presses. Ender Dragon and Wither are blocked.
- pig + bee = "Pee": pig with flapping wings that flies, throws poop and honey.
- Hybrids drop Ability Orbs; right-click one to use the ability (poop, honey, wings, arrow, fireball, web, explosion, sonic boom, smash, ender, egg, frost, wither, witch brew, rot, trident, spit, laser, shulker bullet).
- Fusion Ball (4 iron, 4 red dye, 1 ender pearl -> 2): throw at a hybrid to capture. Throw a full ball to release it as your ally.
  The ball always flies back into your inventory.

## Add more abilities
Edit `FusionAbility.java`: `BY_MOB` (mob -> ability) and `FLYERS` (mobs that give wings).

## Unique relics (only special pairs)
| Hybrid | Pair | Drops | What it does |
|---|---|---|---|
| Pee (pink bar) | pig + bee | Pee Saddle | Use: summon your flying Pee and ride it. Use again while riding: shoots poop / poison. Sneak + use: dismiss. |
| Zombeton (green bar) | zombie + skeleton | Zombie Gauntlet | Use: bone blast (5 bones in a fan). |
| Ghastlem (red bar) | ghast + iron golem | Ghast Cannon | Use: 3-fireball barrage. |
All other pairs drop only Ability Orbs. Any released ally hybrid can be ridden with an empty hand (flying ones fly).
Add more relics in `Relics.java`, `ModItems.java`, `RelicItem.java`.
