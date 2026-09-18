# Changelog - Enjoy Nights! 🌙

All notable changes, features, bug fixes, and vanilla mechanical overrides for **Enjoy Nights!** will be documented in this file.

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
