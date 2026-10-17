# Changelog - Enjoy Nights! 🌙

All notable changes, features, bug fixes, and vanilla mechanical overrides for **Enjoy Nights!** will be documented in this file.

---

## [1.2.0] - 2026-09-20

### 🚀 New Features & Enhancements
* **Anticipatory Event Warnings (Chat):** Players can now foresee incoming lunar events instead of only learning about them at the moment they begin.
  * **Blood Moon Advance Warning:** A chat warning is broadcast a configurable number of days before a Blood Moon siege (`bloodMoonWarningDaysBefore = 3`), fired once on that day so players have time to fortify.
  * **Blood Moon Pre-Nightfall Warning:** On the Blood Moon day itself, a chat warning appears in the sunset window a configurable number of ticks before night falls (`bloodMoonPreDuskWarningTicks = 300` = 15s). The existing cinematic title still plays when night actually begins — this warning does not replace it.
  * **Full Moon Pre-Nightfall Warning:** On a Full Moon day, a lighter chat warning appears in the sunset window before night falls (`fullMoonPreDuskWarningTicks = 200` = 10s), hinting that monsters will be stronger that night.
  * All anticipatory warnings are **chat-only** (no sounds, no cinematic) and each is independently toggleable (`bloodMoonWarningEnabled`, `bloodMoonPreDuskWarningEnabled`, `fullMoonWarningEnabled`). Full Moon warnings function even when the Blood Moon siege mechanic is disabled.
  * **`/enjoynights info`** now reports the predicted event for the coming night (Blood Moon, Full Moon, New Moon, or normal night), readable during the day. Backed by new null-safe lunar-prediction helpers that latch per day and use a tick window so a laggy or skipped tick cannot miss the sunset moment.
* **Event Mob Lifecycle Cleanup (Performance):** Mobs created or empowered by lunar events are now cleaned up deterministically, preventing entity accumulation that degraded server performance over time.
  * **Deterministic end-of-event sweep:** When a Full Moon or Blood Moon ends, event-spawned mobs (siege companions, flame skeletons, boss minions) are removed and naturally-spawned empowered mobs are reverted to normal — regardless of location, so mobs in caves or under cover no longer linger because they never saw the sun.
  * **Empowered natural mobs are normalized, not deleted:** Strength/Speed/Resistance effects, the follow-range bonus, charged-creeper state, and the creeper speed bonus are stripped, leaving a legitimate ordinary monster in the world.
  * **Chunk-reload cleanup for stale & legacy mobs:** When a chunk reloads outside an active event, any lingering event-tagged mobs (including leftovers from older mod versions and mobs that were in unloaded chunks when the event ended) are cleaned up on load.
  * Empowerment now uses removable attribute modifiers, so reversal is exact and idempotent. Toggle with `eventMobCleanupEnabled = true`. The `/enjoynights purge` admin command still force-deletes all event mobs for emergency cleanup.
* **Blood Moon Siege Overhaul & Event Identity:** The Blood Moon is now a distinctly harder, escalating siege, clearly separated from the Full Moon's "empowered individuals" night.
  * **Escalating mixed waves:** Siege waves grew from 1–2 zombies to a configurable **3–5 mobs** per player (`bloodMoonSiegeWaveMinMobs`/`bloodMoonSiegeWaveMaxMobs`), scaling up as the night deepens (`bloodMoonSiegeEscalationEnabled`). A weighted **core** (Zombies, Cave Spiders, Skeletons) is joined by **rare late-wave units** — Husks, Strays, Witches, Vindicators, and Breezes — that only appear past midnight and after `bloodMoonSiegeEscalationWavesBeforeElite` waves. All weights are configurable.
  * **Poisonous Cave Spiders:** Blood Moon natural spawns can bring a Cave Spider ambusher (`bloodMoonCaveSpiderChance = 0.20`) that inflicts configurable **Poison** on hit (`bloodMoonCaveSpiderPoisonSeconds`, `bloodMoonCaveSpiderPoisonAmplifier`).
  * **Elite Wither Skeleton (~1%):** Late-wave skeleton slots have a small chance (`bloodMoonWitherSkeletonEliteChance = 0.01`) to become an elite Wither Skeleton that inflicts **Wither** on hit and gains Resistance — distinct from the Crimson boss.
  * **Escalating anti-lag cap:** The nearby-mob ceiling rises from `bloodMoonMaxNearbyMobs` toward `bloodMoonSiegeMaxCapNight = 24` as midnight approaches, keeping sieges intense yet performance-bounded.
  * **Rare Full Moon Witch:** Full Moon nights (no sieges) may spawn a lone, eerie **Witch** (`fullMoonWitchChance = 0.03`) as a standalone atmospheric threat — never a swarm.
  * All new siege mobs are tagged as event spawns, so they are covered by the event-mob lifecycle cleanup.
* **Warding Totem & Base Sanctuary System:**
  * Added craftable **Warding Totem** (`enjoy_nights:warding_totem`) to safeguard player bases and structures.
  * **Perimeter Repulsion Forcefield:** Instead of cancelling mob spawns, monsters attempting to spawn within the 100-block protection radius (`wardingTotemRadius = 100`) are repelled and relocated to the perimeter exterior (102–106 blocks away on surface ground), concentrating the siege outside the base without reducing world mob density.
  * **Amethyst Catalyst GUI:** Dedicated interface where players deposit 8 Amethyst Shards (`wardingTotemAmethystCost = 8`) and click **[Activar]** to bind the totem to their UUID.
  * **Anti-Abuse Rule (1 Active Totem per Player):** Players can only operate 1 active totem at a time. Attempting to activate a second totem anywhere on the server is strictly rejected with coordinates to their existing active ward.
  * **Safe Deactivation & Refund:** Deactivating the totem via its GUI frees the player's binding and safely returns the 8 amethyst shards to their inventory.
  * **Permanent Destruction (No Silk Touch Recovery):** Mining or breaking the totem block in the world permanently destroys it without dropping the totem item (even when using a Silk Touch pickaxe). Any stored amethysts are refunded on break.
* **Blood Moon Torch & Light Immunity:**
  * Monsters outside protected wards now ignore light levels emitted by torches and lanterns during Blood Moon (`bloodMoonIgnoreTorches = true`). Outdoor areas will crawl with monsters regardless of artificial lighting.
  * The Warding Totem remains the only inviolable sanctuary where monsters cannot spawn.
* **Blood Moon Zenith Boss (*El Heraldo Carmesí*) & Crimson Altar Ritual:**
  * **Crimson Altar Ritual Summoning (`enjoy_nights:crimson_altar`):** Players now summon the boss deliberately by crafting and placing the Crimson Altar during a Blood Moon night on the surface (`bloodMoonBossRequireRitual = true`).
  * **Surface Requirement (No Caves):** Placing or activating the altar underground or without open sky is strictly rejected, returning the altar to the player with a warning message.
  * **1000-Block Dynamic Player Scaling:** Power scales dynamically based on the count of active players within a **1000-block radius** (`bloodMoonBossScalingRadius = 1000.0`), broadcasting the adjusted difficulty in chat.
  * **10-Minute Freeze & Dawn Despawn:**
    * When summoned, time freezes at midnight for a maximum of **10 minutes** (`bloodMoonBossFreezeDurationSeconds = 600`).
    * Once the 10 minutes conclude, time resumes natural flow toward morning.
    * If players fail to defeat the boss before dawn arrives, the boss triggers a pulse of darkness and blindness, dissolving into the morning shadows without dropping loot (`bloodMoonBossDespawnAtDawn = true`).
  * **On-Screen Boss Bar:** Vivid red boss bar (`ServerBossEvent`) visible at the top of the screen tracking health percentage and player engagement within 128 blocks.
  * **Imposing Scale & Colossus Stride:** Towering at 1.7x scale (`Attributes.SCALE`) with 1.25 step height (`Attributes.STEP_HEIGHT`).
  * **Smart Aggro & Target Commitment:** Commits to targets in 5-second windows to eliminate pathfinder stutter while tracking damage threat to aggressively pursue snipers and archers.
  * **Pacing, Speed & Combat Balance Calibration:**
    * Base movement speed calibrated to 0.20 (deliberate, menacing colossus stride instead of hyperactive rushing).
    * **Damage & Survivability Overhaul (No One-Shots):** Removed player count damage scaling (`damagePerPlayer = 0.0`) and calibrated total attack damage to 14.0. Players in Diamond/Netherite armor can survive 4 to 5 hits.
    * **Shortened Wither:** Reduced Wither effect duration from 10s to 3s (60 ticks) per strike.
    * **Health & Poise Scaling for Groups:** Base health set to 200 HP (+100 HP per extra player in 1000 blocks). Knockback resistance set to 0.90 to prevent group stunlocking.
    * **Minion Scaling:** Reinforced minion squads now scale dynamically with group size (2 to 6 minions) at 75%, 50%, and 25% health.
    * Alternating melee combo: standard single-target strike followed by sweeping cleave finisher, eliminating repetitive cleave spam.
    * Added **Extended Punish Windows (Recovery Frames):** After executing heavy abilities (Leap, Shockwave, or Charged Lunge), the boss pauses in recovery for ~1.75s (`bloodMoonBossRecoveryTicks = 35`), giving players tactical opportunities to counterattack safely.
  * **Elden Ring Moveset & Combat Abilities:**
    * **Telegraphed Charged Lunge & Sweeping Dash:** Winds up with 1.5s visual/sound telegraph, dynamic target tracking during charge, and dashes forward in a 140° wide sweep inflicting balanced damage, shield breaks (in Phase 2), and knockback (`bloodMoonBossChargedAttackEnabled = true`).
    * **Conical Cleave:** 140° frontal sweep damaging multiple players within melee range.
    * **Devastating Leap Slam:** Launches toward distant archers, dealing AoE explosion damage and disabling shields for 5 seconds.
    * **Ground Fissure:** Slams the ground launching nearby players into the air.
    * **Repulsion Wave:** Discharges a concussive shockwave when crowded by 3 or more players.
    * **Phase 2 "Furia del Eclipse":** Activates below 40% health with soul-fire aura, speed buffs, fire-infused attacks, and halved ability cooldowns (without damage multipliers).
    * **Midnight Roar & Minion Waves:** Emits Warden roars causing Darkness and Slowness, and summons minion reinforcements at 75%, 50%, and 25% health.
  * **Triumphant Victory & Rewards:** Defeating the boss broadcasts a global victory announcement, unfreezes the passage of time toward dawn, and awards 500 XP, Diamonds, Emeralds, Amethyst Shards, and a Totem of Undying.
  * **100% Server Configurable:** Scale, speed, charged attack, recovery ticks, cleave angle/radius, leap attack, shockwave, phase 2, health, damage, freeze duration, scaling radius, and dawn despawn are fully customizable in `enjoy_nights-server.toml`.
* **Server Performance & Night Mob Cleanup Fix:**
  * **Eliminated Server Entity Saturation:** Fixed lingering mobs from full and blood moons by replacing `EntitySpawnReason.EVENT` with `NATURAL` and `REINFORCEMENT` spawn types.
  * **Daytime Despawn Routine (`MobDespawnEvent`):** Mobs with night tags (`enjoy_nights:extra_spawn`, `enjoy_nights:fire_skeleton`) are forcibly despawned at dawn even when wearing helmets.
  * **Chunk Load Purge (`EntityJoinLevelEvent`):** Mobs from previous versions loading from disk during the daytime are automatically discarded.
  * **Admin Purge Command:** Added `/enjoynights purge` (permission level 2) to sweep and remove all lingering night mobs across all loaded dimensions.
* **Blood Moon Flame Skeletons:**
  * Skeletons spawning during Blood Moon have a 15% chance (`bloodMoonFlameSkeletonChance = 0.15`) to spawn as fiery archers equipped with helmets and Flame-enchanted bows, firing incendiary arrows that ignite targets and combustible blocks. Their flame bows and helmets are guaranteed never to drop on death to prevent loot farming.

---

## [1.1.1] - 2026-09-19

### 🐛 Bug Fixes
* **Fixed Darkness Effect Loop & Sky Flickering:**
  * Removed unintended `MobEffects.DARKNESS` application on New Moon nights (the darkness pulse effect is exclusive to Blood Moon horror mechanics).
  * Fixed active darkness effect removal in `BloodMoonEvents` running continuously on every server tick during non-Blood Moon periods. The removal now executes strictly when a Blood Moon event concludes, resolving the rapid effect add/remove loop that caused severe visual sky and lighting flickering across New Moon and other nights.
  * Added null-safety guards across all `NightAndMoonHelper` lunar calculation methods.

---

## [1.1.0] - 2026-09-18

### 🚀 Features & Balance
* **Creeper Potion Cloud Fix & Charged Creepers:**
  * Creepers no longer receive potion effects (eliminating the lingering `AreaEffectCloud` explosion exploit where players could obtain permanent Strength/Speed).
  * Added chance for Creepers to spawn as **Charged Creepers** under lunar events: **5%** on Full Moon (`fullMoonChargedCreeperChance = 0.05`) and **25%** on Blood Moon (`bloodMoonChargedCreeperChance = 0.25`) to prevent excessive terrain destruction.
  * Normal boosted Creepers receive agility improvements directly via attribute modifier (+15% movement speed) without potion particles.
* **Bed Recovery Station Rebalance & Fair Play Cooldown:**
  * Streamlined recovery buffs: removed Speed I and Absorption II; now grants **Regeneration II**, **Resistance I**, and instant **Saturation**.
  * Increased buff duration from 80s to **90 seconds (1800 ticks)**.
  * Added a **180-second (3 minutes) cooldown** between bed rests (`bedRestCooldownSeconds = 180`).
  * If a player attempts to sleep before 180s have passed, the bed prevents sleeping and displays an action bar message showing the remaining cooldown time.
* **Atmospheric Blood Moon Terror (Universal & Shader-Compatible):**
  * **Pulsing Blood Vignette Overhaul:** Replaced screen-darkening blend with a dedicated 512x512 smooth radial alpha texture rendered via `RenderPipelines.GUI_TEXTURED`. The center crosshair view remains 100% clear and unobstructed, while the outer perimeter pulses with a vivid, deep crimson heartbeat wave that is clearly visible against pitch-black skies even when using high-contrast shaders like **Complementary Unbound**.
  * **Continuous Basalt Deltas Ambient Loop:** Blood Moon nights now immerse the player with the low, oppressive volcanic rumble of the Nether Basalt Deltas (`SoundEvents.AMBIENT_BASALT_DELTAS_LOOP`), complete with smooth 2-second fade-in and fade-out transitions.
  * **Ambient Nether Spore Atmosphere:** Ethereal floating cobalt motes (`WARPED_SPORE` as in the Warped Forest), blood-red motes (`CRIMSON_SPORE`), subtle dark falling ash (`ASH`), and ground mist wisps (`SMOKE`).
  * **Paced Psychological Horror & Phantom Creeper Fuse:** Sporadic horror sound cues now trigger at spaced-out, suspenseful intervals of **45 to 80 seconds** (preventing sound repetition/fatigue). Added faint **Creeper primed fuse hisses** (`SoundEvents.CREEPER_PRIMED`) directly behind or near the player to induce real paranoia.
  * **Sporadic Darkness Pulses (Warden Jump-Flicker):** Outdoors players experience a sudden, brief 1.75s darkness pulse (`bloodMoonDarknessPulseEnabled`) every 2 to 3 minutes accompanied by a heavy Warden heartbeat thump, providing a sudden jolt without causing continuous blindness.
  * **Shader-Native Blood-Red Sky & Natural Lighting:** Dynamically overrides `EnvironmentAttributes.SKY_COLOR` on the client during Blood Moon, enabling shaders like **Complementary Unbound** to render spectacular crimson clouds and sky while keeping terrain lighting completely natural and clear (no pitch-black terrain, retaining authentic clean vanilla nighttime visibility).
  * **Rushing Phantom Footsteps:** When players pause walking, mine blocks, or gather items, 2 to 3 rapid footsteps (`bloodMoonPhantomFootstepsEnabled`) rush right behind their back (spaced by ~0.3s and closing in by 0.5 blocks per step), perfectly matching the material of the floor (wood, grass, stone).
  * **Cinematic Nightfall Announcement:** Starting the Blood Moon triggers a dramatic Souls-like on-screen title banner (*"THE BLOOD MOON RISES — The siege has begun. Seek shelter."*) accompanied by a deep rumble and tolling bell.

---

## [1.0.0] - 2026-09-18

### 🚀 Features
* **Beds as Tactical Recovery Stations (No Night Skip):**
  * Sleeping in beds no longer skips time to morning.
  * Players rest for exactly 10 seconds before being automatically woken up.
  * Completing the rest awards an 80-second (1m 20s) tactical buff: Regeneration II, Resistance I, Speed I, Absorption II, and instant Saturation.
* **Phantom Suppression:**
  * Complete cancellation of natural Phantom spawns caused by insomnia.
  * Automatic periodic reset of `Stats.TIME_SINCE_REST` to keep players insomnia-free.
* **Dynamic Lunar Phases:**
  * **New Moon (Phase 4):** Grants players a +60% stealth bonus against mob detection (`newMoonStealthFactor = 0.40`) and adds atmospheric dark fog when outdoors.
  * **Full Moon (Phase 0):** Spawns 50% of monsters with Strength and Speed buffs with extended follow range (+16 blocks), rewarding 3x Experience upon defeat.
* **Enhanced Mineral Mob Drops:**
  * Under Full Moon and Blood Moon nights, standard monsters (Zombies, Skeletons, Creepers, Spiders) have a chance to drop Raw Iron, Iron Nuggets, Lapis Lazuli, Amethyst Shards, Emeralds, and a 1% chance for Diamonds.
* **Enclosed Dark Area Paranoia:**
  * Players who remain stationary for over 3 minutes (180s) in closed, unlit areas (block light 0, overhead roof) during the night receive Weakness, darkness pulses, and unsettling auditory hallucinations (cave noises, Warden heartbeats, faint creeper hisses, phantom screeches).
* **Periodic Blood Moon Sieges:**
  * Configurable server-wide siege event occurring every 10 nights by default (`bloodMoonIntervalDays = 10`).
  * Features ominous broadcast alerts, blood-red sky and fog rendering, and enhanced mob attributes (Strength II, Resistance, Speed).
* **Balanced Blood Moon Spawning:**
  * Natural monster spawns have a 35% chance to spawn an extra companion mob.
  * Controlled periodic siege waves (1-2 zombies every 30s) around active survival players.
  * Strict safety cap (`bloodMoonMaxNearbyMobs = 16`) within 32 blocks to prevent entity swarming and server lag.
* **In-Game Management Commands:**
  * `/enjoynights info`: Inspect current in-game day, lunar phase, night status, and days until next Blood Moon.
  * `/enjoynights bloodmoon start` & `/enjoynights bloodmoon stop`: Operator commands to trigger or cancel Blood Moon sieges.

---

### 🐛 Fixes & AI Improvements
* **Intelligent Barrier Detection (Anti-Mountain Fix):**
  * Fixed an issue where zombies attacked natural hillside/mountain dirt steps instead of pathfinding up slopes.
  * Added logic verifying that jumpable 1-block terrain steps are ignored so mobs maintain full pursuit speed up hills.
* **Continuous Breach & Door Break Progress Fix:**
  * Fixed a critical issue where zombie door breaking would cancel mid-way (progress resetting to 0%) due to raycast line-of-sight checks through door windows.
  * Fixed concurrent pathfinding conflicts by claiming `Goal.Flag.MOVE` and halting navigation (`navigation.stop()`) while actively breaching.
  * Added authentic vanilla wood attack (`1019`) and door break (`1021`) sound effects, ensuring both halves of doors are properly destroyed so entrances are completely clear.
* **Blood Scent Target Persistence:**
  * Implemented `BloodMoonSiegeTargetGoal` (`mustSee = false`), preventing zombies from dropping target when players enter houses, close doors, or break direct line of sight.
  * Added automatic fallback target acquisition within 24 blocks through solid walls.
  * Added post-breach acceleration: zombies immediately charge towards the player (`1.25x` speed) the moment a door or barricade is broken.

---

### ⚠️ Breaking Changes & Vanilla Overrides
* **Vanilla Daylight Skipping Disabled:**
  * Beds can no longer be used by players to skip the night or thunderstorm cycles. All players will wake up after 10 seconds into the ongoing night.
* **Phantom Mechanics Overridden:**
  * The vanilla Phantom insomnia mechanic is completely bypassed. If datapacks or other mods rely on natural Phantom spawns for Phantom Membrane farming, membranes must now be obtained through alternative means (cats, structures, or custom drops).
* **Zombie Block Destructibility:**
  * Zombies during Blood Moon can permanently break player-placed wooden blocks (doors, trapdoors, planks, fences, logs) and dirt/dust blocks (dirt, sand, gravel).

---

### ⚙️ Server Configuration Additions (`enjoy_nights-server.toml`)
* `bloodMoonIntervalDays`: Configurable interval in days between Blood Moon sieges (Range: 1–365, default: 10).
* `bloodMoonExtraSpawnsEnabled`: Toggle extra natural companion spawns during Blood Moon.
* `bloodMoonExtraNaturalSpawnChance`: Companion spawn probability (default: 0.35).
* `bloodMoonSiegeWavesEnabled`: Toggle periodic siege waves around players.
* `bloodMoonWaveIntervalSeconds`: Interval in seconds between siege waves (default: 30s).
* `bloodMoonMaxNearbyMobs`: Maximum nearby monster safety cap (default: 16).
