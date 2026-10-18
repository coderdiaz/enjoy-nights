# Enjoy Nights! 🌙

**Transform your Minecraft nights from something you skip into an intense, rewarding, and atmospheric survival experience.**

Tired of players instantly skipping the night with a bed? Annoyed by annoying Phantoms? Want the moon phases to actually matter and fear the dark again? **Enjoy Nights!** reworks night mechanics with tactical bed resting, dynamic lunar phases, mineral drops, dark area paranoia, craftable base defenses, escalating Blood Moon sieges where monsters break down your doors, and a ritual-summoned Blood Moon boss!

---

## ✨ Features Overview

### 🛏️ Beds as Tactical Recovery Stations (No Night Skip!)
* **Sleeping no longer skips the night.** You can no longer fast-forward the daylight cycle.
* When you sleep in a bed, you rest for **10 seconds** before automatically standing up.
* Finishing your rest grants a **1 minute and 30 seconds (90s)** recovery buff:
  * **Regeneration II** (rapid healing)
  * **Resistance I** (damage reduction)
  * **Saturation** (fills hunger bars)
* **Fair-Play Anti-Abuse Cooldown (180s / 3 minutes):**
  * Once you rest in bed, you cannot sleep again until **3 minutes** have passed.
  * If you try to sleep before the cooldown expires, the bed prevents sleeping and an action bar message shows the remaining seconds.
* Use your bed as a pre-combat station before heading out into the dangerous night!

### 👻 Farewell, Phantoms! (Phantom Suppression)
* **Phantoms are completely disabled** from spawning naturally due to insomnia.
* The internal rest timer is automatically maintained so you can stay awake for hundreds of days without ever being harassed by flying nightmares.

### 💎 Enhanced Mineral Mob Drops
* Under a **Full Moon** (and Blood Moons), standard monsters (Zombies, Skeletons, Creepers, Spiders, etc.) drop additional mineral fragments:
  * **Raw Iron & Iron Nuggets** (35% & 25% chance)
  * **Lapis Lazuli, Amethyst Shards & Emeralds** (up to 20% chance)
  * **Diamonds** (1% chance / configurable)

### 🌓 Dynamic Lunar Phases
* **🌑 New Moon (Phase 4 - Every 8 Nights):**
  * Low outdoor visibility: dense dark fog when exposed outdoors under the open sky.
  * **Stealth Bonus:** Reduces the range at which mobs can spot you by **60%**! Perfect for stealthy supply runs.
* **🌕 Full Moon (Phase 0 - Every 8 Nights):**
  * Monsters become hyper-aggressive with extended follow range (+16 blocks).
  * 50% chance for monsters to spawn with **Strength** and **Speed** potion effects.
  * **Anti-Exploit Creeper Mechanics:** Creepers do not receive potion effects (preventing permanent lingering potion clouds upon explosion); instead, they have a **5% chance to spawn as Charged Creepers** with +15% movement speed!
  * **Rare Eerie Witch (3% chance):** A natural spawn may bring a lone **Witch** as an unsettling standalone threat — Full Moon has no sieges, so this is atmospheric, never a swarm.
  * Slaying these powered monsters rewards **3x Experience**!

### 🕯️ Enclosed Dark Area Paranoia
* Hiding in a hole or bunker isn't completely safe. If you stand still for **more than 3 minutes (180s)** in an enclosed, dark area (0 block light, under a roof) during the night:
  * Paranoia sets in: you receive **Weakness** and visual darkness pulses.
  * You will hear **disturbing auditory hallucinations** (cave rumbles, distant creeper hisses, phantom screeches, Warden heartbeats).
  * Moving around, placing torches, or going outside clears the paranoia.

### 🩸 Blood Moon Sieges & Destructive AI
* Every **10 nights** (configurable on the server from 1 to 365 days), the **Blood Moon** rises with an ominous announcement and a blood-red sky.
* **Universal Shader-Compatible Atmosphere:** Features a blood-red screen vignette overlay (100% visible even with shaders like Complementary Unbound or Iris), floating crimson ember particles, and dense crimson fog.
* All monsters gain **Strength II, Resistance, and Speed**.
* **Charged Creepers (25% chance):** Creepers have a 25% chance to spawn powered without dropping lingering potion clouds.
* **Flame Skeletons (15% chance):** Skeletons may spawn as fiery archers with Flame-enchanted bows; their bows and helmets never drop.
* **Torch & Light Immunity:** Outside protected Warding Totem areas, monsters ignore block light from torches and lanterns, so lit areas no longer stop spawns.
* **Escalating Mixed Sieges:** Siege waves spawn **3–5 mobs** per player, growing larger as the night deepens. A **core** of Zombies, poisonous Cave Spiders, and Skeletons is joined by **rare late-wave units** — Husks, Strays, Witches, Vindicators, and Breezes — past midnight, plus a ~1% chance for an elite **Wither Skeleton** (Wither on hit).
* **Poisonous Cave Spiders:** Blood Moon spawns can bring Cave Spider ambushers that inflict **Poison** on hit.
* **Intelligent Siege AI:**
  * **Blood Scent:** During the Blood Moon, zombies sense players through walls and doors (up to 32 blocks) without losing target when you enter a house.
  * **Breaching Barricades:** If you barricade yourself inside, zombies will actively break **wooden blocks** (doors, trapdoors, planks, logs, fences) and **dirt/dust blocks** (dirt, sand, gravel) with authentic hit and break sounds!
  * **Anti-Mountain Logic:** Zombies are smart enough to recognize mountain slopes and walkable terrain; they will never break hillside steps and will only destroy real obstacles blocking their path to you.
  * **Escalating Safety Cap:** The nearby-mob cap rises as midnight approaches, keeping sieges intense but performance-bounded.

### ⏳ Anticipatory Event Warnings
* **See it coming:** Chat warnings give you time to prepare instead of learning about events only when they start.
* **Blood Moon advance notice** a configurable number of days before the siege, plus a sunset chat warning on the Blood Moon day (the cinematic still plays at nightfall).
* **Full Moon sunset warning** hinting that monsters will be stronger that night.
* All warnings are chat-only and independently toggleable.

### 🛡️ Warding Totem (Base Sanctuary)
* Craftable **Warding Totem** that protects a **100-block radius**. Monsters attempting to spawn inside are relocated to the perimeter instead of being deleted.
* Activated by depositing **8 Amethyst Shards** via its GUI, binding it to your UUID. One active totem per player; safe deactivation refunds the shards. It is the only safe haven during a Blood Moon.

### ☠️ Blood Moon Zenith Boss (*El Heraldo Carmesí*) & Crimson Altar
* **Crimson Altar Ritual:** Summon the boss by crafting and placing the **Crimson Altar** on the surface during a Blood Moon night. The altar can only be placed in the **Overworld**, only drops when mined with a **diamond pickaxe or better**, stays **inert** (and recoverable) if placed outside a Blood Moon, and becomes **indestructible** once the ritual begins. It emits faint crimson particles at night while awaiting a Blood Moon.
* **Boss Arena:** The altar is not consumed on summon — it stays as an indestructible anchor marking a **56-block battle arena** around it. While the boss lives, regular siege mobs stop spawning in a wider **72-block radius** around the altar (only the boss's own reinforcements appear), so you face the boss without a pile-on. The boss **cannot be lured out** of the battle arena: run too far and it switches to another player still inside the radius. If everyone leaves, the boss returns to the altar and **slowly regenerates health** while it waits — fleeing only resets your progress. The altar is **consumed the instant the boss is defeated or eliminated** (with a triumphant burst), and normal mob spawning resumes immediately — so a well-geared team that kills it fast is not left in a dead zone.
* **Dynamic Scaling:** Boss health scales with nearby players (with diminishing returns for large groups) while keeping damage fair; an on-screen boss bar tracks the fight.
* **Elden Ring-style Moveset:** A true colossus — it shakes the ground with heavy footsteps and its strikes land as weighty overhead slams, not thin sword sweeps. Telegraphed cleave, charged lunge, ground shockwave, repulsion wave, and a committed **directed leap** that lands on a targeted player and unleashes a dispersal shockwave to scatter the group. New moves add real variety: a **Stomp** that punishes players hugging its legs, a dodgeable **Wither Skull volley** at range, and telegraphed **Soul Fire Pillars** that erupt under marked ground after a brief warning. Heavy moves have punish windows, a readable rhythm, and no longer chain their knockbacks. Melee hits have a chance (not a guarantee) to inflict Wither. Phase 2 "Furia del Eclipse" activates below 40% HP. Every new attack is individually configurable.
* **Reinforcements:** At 75%, 50%, and 25% health the boss summons minions. Most are fiery skeletons and armored zombies, but rarer illager specialists appear too — a **10% chance for a Vindicator** and a **5% chance for an Evoker** per minion. These are always exempt from the arena spawn suppression, so the boss's own backup always arrives.
* **Dawn Despawn:** If not defeated before the Blood Moon ends, the boss fades away **without dropping rewards**. Victory grants XP, Diamonds, Emeralds, Amethyst Shards, and a Totem of Undying.

### 🧹 Event Mob Cleanup (Performance)
* When a lunar event ends, event-spawned mobs are removed and empowered natural mobs are reverted to normal — regardless of location — preventing entity buildup. Lingering/legacy event mobs are also cleaned when their chunk reloads. The Blood Moon only affects players in the Overworld.

---

## ⚙️ Configuration

Fully configurable via `config/enjoy_nights-server.toml` and `config/enjoy_nights-client.toml`:

```toml
[server]
    [server.bed_and_sleep]
        preventNightSkip = true
        bedSleepDurationSeconds = 10
        bedBuffDurationSeconds = 90
        bedRestCooldownSeconds = 180

    [server.phantoms]
        preventPhantoms = true

    [server.lunar_phases]
        fullMoonDiamondDropChance = 0.01
        fullMoonExtraDropsEnabled = true
        newMoonStealthFactor = 0.40
        fullMoonMobBuffsEnabled = true
        fullMoonExperienceMultiplier = 3
        fullMoonChargedCreeperChance = 0.05
        bloodMoonChargedCreeperChance = 0.25
        fullMoonWarningEnabled = true
        fullMoonPreDuskWarningTicks = 200
        eventMobCleanupEnabled = true
        fullMoonWitchChance = 0.03

    [server.paranoia]
        paranoiaDurationSeconds = 180
        paranoiaWeaknessEnabled = true

    [server.warding_totem]
        wardingTotemRadius = 100
        wardingTotemAmethystCost = 8

    [server.blood_moon]
        bloodMoonIntervalDays = 10
        bloodMoonSiegeEnabled = true
        bloodMoonZombiesBreakBlocks = true
        bloodMoonExtraSpawnsEnabled = true
        bloodMoonExtraNaturalSpawnChance = 0.35
        bloodMoonSiegeWavesEnabled = true
        bloodMoonWaveIntervalSeconds = 30
        bloodMoonMaxNearbyMobs = 16
        bloodMoonDarknessPulseEnabled = true
        bloodMoonIgnoreTorches = true
        bloodMoonFlameSkeletonChance = 0.15
        bloodMoonWarningEnabled = true
        bloodMoonWarningDaysBefore = 3
        bloodMoonPreDuskWarningEnabled = true
        bloodMoonPreDuskWarningTicks = 300
        bloodMoonSiegeWaveMinMobs = 3
        bloodMoonSiegeWaveMaxMobs = 5
        bloodMoonSiegeEscalationEnabled = true
        bloodMoonSiegeMaxCapNight = 24
        bloodMoonSiegeEscalationWavesBeforeElite = 3
        bloodMoonWaveZombieWeight = 30
        bloodMoonWaveCaveSpiderWeight = 15
        bloodMoonWaveSkeletonWeight = 15
        bloodMoonWaveHuskWeight = 8
        bloodMoonWaveStrayWeight = 8
        bloodMoonWaveWitchWeight = 4
        bloodMoonWaveVindicatorWeight = 3
        bloodMoonWaveBreezeWeight = 2
        bloodMoonWitherSkeletonEliteChance = 0.01
        bloodMoonCaveSpiderChance = 0.20
        bloodMoonCaveSpiderPoisonSeconds = 5
        bloodMoonCaveSpiderPoisonAmplifier = 0

    [server.blood_moon_boss]
        bloodMoonBossEnabled = true
        bloodMoonBossRequireRitual = true
        bloodMoonBossBaseHealth = 200.0
        bloodMoonBossHealthPerPlayer = 60.0
        bloodMoonBossHealthDiminishAfterPlayers = 1
        bloodMoonBossHealthDiminishFactor = 0.6
        bloodMoonBossMaxMinionsPerPhase = 4
        bloodMoonBossBaseDamage = 11.0
        bloodMoonBossFreezeDurationSeconds = 600
        bloodMoonBossDespawnAtDawn = true
        bloodMoonBossLeapRadius = 6.0
        bloodMoonBossLeapKnockback = 1.6
        bloodMoonBossPhase2Enabled = true
        bloodMoonBossKnockbackMultiplier = 1.0
        bloodMoonBossMeleeBias = 0.5
        bloodMoonBossAbilityLockoutTicks = 65
        bloodMoonBossWitherOnHitChance = 0.25
        bloodMoonBossStompEnabled = true
        bloodMoonBossStompCooldownSeconds = 10
        bloodMoonBossSkullVolleyEnabled = true
        bloodMoonBossSkullVolleyCooldownSeconds = 11
        bloodMoonBossSoulPillarsEnabled = true
        bloodMoonBossSoulPillarsCooldownSeconds = 16
        bloodMoonBossArenaRadius = 56
        bloodMoonBossArenaSuppressRadius = 72
        bloodMoonBossArenaSuppressSieges = true
        bloodMoonBossArenaRegenPerSecond = 2.0

[client]
    bloodMoonFogTint = true
    bloodMoonScreenVignette = true
    bloodMoonVignetteIntensity = 0.70
    bloodMoonComplementarySpecific = true
    bloodMoonParticles = true
    paranoiaSoundsEnabled = true
    bloodMoonPhantomFootstepsEnabled = true
    bloodMoonShaderAtmosphereIntensity = 0.35
```

---

## 💬 In-Game Commands

* `/enjoynights info`: Displays current day, day/night state, current moon phase, days remaining until the next Blood Moon, and the predicted event for the coming night.
* `/enjoynights bloodmoon start`: *(Operator)* Manually triggers a Blood Moon for events or testing.
* `/enjoynights bloodmoon stop`: *(Operator)* Ends the forced Blood Moon.
* `/enjoynights purge`: *(Operator)* Force-deletes all event-spawned and empowered mobs across loaded dimensions for emergency cleanup.

---

## 💻 Compatibility & Requirements

* **Minecraft:** `1.21.11`
* **Mod Loader:** `NeoForge 21.11.45+`
* **Side:** Required on **Server**, recommended on **Client** for red fog, screen vignette, ember particles, and atmospheric sound effects.
* **Shaders:** Fully compatible with shaders including **Complementary Unbound**, **Iris**, **Sodium / Embeddium**, and Vanilla.
