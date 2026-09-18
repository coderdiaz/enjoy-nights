# Enjoy nights! 🌙

A mod for **Minecraft** and **NeoForge** that transforms the night from something to skip into a challenging, rewarding, and atmospheric survival experience. Beds become temporary tactical preparation stations, nighttime survival is rewarded with lunar phase perks and mineral drops, and periodic Blood Moon sieges bring destructive zombie AI to breach your fortifications.

---

## 🌟 Detailed Features

### 1. Nights Cannot Be Skipped (No Time Skip)
* Sleeping in a bed **no longer advances the time** to morning or skips the night.
* After lying down for **10 seconds (200 ticks)** of rest, the player automatically wakes up and stands up without altering the daylight cycle.

### 2. Recovery Station (Bed as a Buff Station)
* Completing the 10-second rest in bed grants a tactical recovery buff lasting **1 minute and 30 seconds (90 seconds)**:
  * **Regeneration II** (accelerated health recovery)
  * **Resistance I** (reduced incoming damage)
  * **Saturation** (replenishes hunger bars)
* **Fair-Play Anti-Abuse Cooldown (180s / 3 minutes):**
  * Once you rest in bed, you cannot sleep again until **180 seconds (3 minutes)** have passed (`bedRestCooldownSeconds = 180`).
  * Attempting to sleep during cooldown displays an action bar alert indicating the remaining seconds before you can rest again.
* Ideal for preparing yourself before venturing out into the dangerous night or defending your base.

### 3. Total Nightmare Suppression (Phantoms Removed)
* **Phantoms never spawn naturally** due to insomnia.
* The internal insomnia counter (`Stats.TIME_SINCE_REST`) is automatically reset periodically so players can enjoy long mining or building sessions through the night without annoying aerial harassment.

### 4. Enhanced Mineral Mob Drops under Full Moon
* During **Full Moon** (and Blood Moon) nights, standard monsters (Zombies, Skeletons, Creepers, Spiders, etc.) have a chance to drop additional mineral fragments upon death:
  * **Iron Fragments:** 35% Raw Iron (1-2) or 25% Iron Nuggets (2-5).
  * **Gems:** 20% Lapis Lazuli (1-4), 15% Amethyst Shards (1-2), or 8% Emeralds (1).
  * **Diamond:** Configurable **1% chance** (`0.01`).

### 5. Dynamic Lunar Phases
Integrates seamlessly with Minecraft's native 8-phase astronomical cycle:
* **New Moon (Phase 4 - Every 8 Nights):**
  * Low outdoor visibility: dense dark fog and ambient darkness effects when exposed under the open sky.
  * **Increased Stealth:** Reduces the range at which monsters can detect the player by **60%** (`newMoonStealthFactor = 0.40`), perfect for sneaking around unnoticed.
* **Full Moon (Phase 0 - Every 8 Nights):**
  * Monsters become more aggressive, spawning with an extended follow range (+16 blocks).
  * 50% chance for monsters to spawn with **Strength** and **Speed** potion effects.
  * **Anti-Exploit Creeper Mechanics:** Creepers never receive potion effects (preventing permanent lingering potion clouds upon explosion); instead, they have a **5% chance to spawn as Charged Creepers** (`fullMoonChargedCreeperChance = 0.05`) and receive a direct +15% movement speed attribute buff.
  * Defeating powered monsters rewards **3x Experience** (`fullMoonExperienceMultiplier = 3`).

### 6. Enclosed Dark Area Paranoia
* If a player stays **still for more than 3 minutes (180s)** in an enclosed area (under a ceiling / no sky exposure) with **block light at 0** during the night:
  * Paranoia sets in, afflicting the player with **Weakness** and visual **Darkness** pulses.
  * Causes **disturbing auditory hallucinations** (creepy cave noises, Warden heartbeats, faint phantom screeches, distant creeper hisses).
  * Displays an unsettling whisper in the action bar (*"You feel an eerie presence lurking in the dark..."*).
  * Moving around, stepping outdoors, or placing torches/lanterns clears the paranoia.

### 7. Blood Moon Sieges (Every 10 Nights by Default)
* Occurs periodically every **10 nights** by default (`bloodMoonIntervalDays = 10`, fully configurable on the server from 1 to 365 days).
* When night falls on a Blood Moon night:
  * An ominous warning sounds and a broadcast is sent to all players: *"The Blood Moon rises... The siege begins!"*
  * **Universal Shader-Compatible Atmosphere:** Renders a blood-red screen vignette tint overlay (100% visible even with shaders like Complementary Unbound or Iris), floating crimson ember particles (`CRIMSON_SPORE`), and dense crimson fog.
  * Inherits all mineral drops and 3x XP from the Full Moon, with a 75% chance for monsters to gain **Strength II**, **Resistance**, and **Speed**.
  * **Charged Creepers (25% chance):** Creepers have a 25% chance to spawn powered (`bloodMoonChargedCreeperChance = 0.25`) without dropping lingering potion clouds.
* **Balanced Siege Mob Spawning (Anti-Lag):**
  * Natural monster spawns have a 35% chance to spawn an additional companion mob.
  * Small periodic waves of 1-2 siege zombies spawn every 30 seconds around active players.
  * **Strict Safety Cap (`bloodMoonMaxNearbyMobs = 16`):** If there are already 16 or more monsters within 32 blocks of a player, extra spawns are paused to prevent server lag or overwhelming swarms.

### 8. Advanced Zombie Siege AI
* **Blood Scent (Tracking through walls):** During the Blood Moon, zombies sense players through walls and closed doors (`mustSee = false`) within a 32-block radius, meaning they **never lose their target** when you enter a house.
* **Anti-Mountain Terrain Logic:** The AI distinguishes walkable hillside slopes from real barricades. **Zombies will never attack natural dirt steps**; they simply jump over them to pursue you.
* **Relentless Barricade Breaching:**
  * When barricaded or behind doors, the zombie locks in place (`navigation.stop()`) and pounds the obstacle continuously without mid-way cancellations from door windows or internal player movement.
  * Capable of destroying **wooden blocks** (doors, trapdoors, planks, logs, fences) and **dirt/dust blocks** (dirt, sand, gravel).
  * Plays authentic Minecraft wood hit sounds (`1019`) and full door break sounds (`1021`), completely clearing both halves of doors so the zombie can immediately charge in.

---

### ⚙️ Configuration

### Server Configuration (`config/enjoy_nights-server.toml`)

All mechanics are 100% customizable:

```toml
[server]
    [server.bed_and_sleep]
        # Prevent sleeping from skipping the night to daytime
        preventNightSkip = true
        # Duration of bed rest in seconds before waking up automatically
        bedSleepDurationSeconds = 10
        # Duration of recovery buffs in seconds (default: 90s = 1m 30s)
        bedBuffDurationSeconds = 90
        # Cooldown in seconds before a player can rest in bed again (default: 180s = 3 minutes)
        bedRestCooldownSeconds = 180

    [server.phantoms]
        # Disables natural Phantom spawning caused by insomnia
        preventPhantoms = true

    [server.lunar_phases]
        # Diamond drop chance under Full Moon / Blood Moon (0.01 = 1%)
        fullMoonDiamondDropChance = 0.01
        # Enables extra mineral fragments (iron, gems) from mobs
        fullMoonExtraDropsEnabled = true
        # Visibility factor on New Moon (0.40 = 60% more stealth)
        newMoonStealthFactor = 0.40
        # Chance for monsters to spawn with Strength and Speed on Full Moon
        fullMoonMobBuffsEnabled = true
        # Experience multiplier for powered monsters
        fullMoonExperienceMultiplier = 3
        # Chance (0.0 to 1.0) for creepers to spawn as Charged Creepers on Full Moon (default: 0.05 = 5%)
        fullMoonChargedCreeperChance = 0.05
        # Chance (0.0 to 1.0) for creepers to spawn as Charged Creepers on Blood Moon (default: 0.25 = 25%)
        bloodMoonChargedCreeperChance = 0.25

    [server.paranoia]
        # Seconds standing still in a dark enclosed space before paranoia triggers (180s = 3 min)
        paranoiaDurationSeconds = 180
        # Enables weakness effects and spooky auditory hallucinations
        paranoiaWeaknessEnabled = true

    [server.blood_moon]
        # Number of nights between each Blood Moon siege (1 to 365 days)
        bloodMoonIntervalDays = 10
        # Enables the Blood Moon event
        bloodMoonSiegeEnabled = true
        # Allows zombies to break wood and dirt blocks
        bloodMoonZombiesBreakBlocks = true
        # Enables balanced additional siege mob spawns
        bloodMoonExtraSpawnsEnabled = true
        bloodMoonExtraNaturalSpawnChance = 0.35
        bloodMoonSiegeWavesEnabled = true
        bloodMoonWaveIntervalSeconds = 30
        # Maximum nearby hostile mobs before pausing extra spawns (anti-lag cap)
        bloodMoonMaxNearbyMobs = 16
```

### Client Configuration (`config/enjoy_nights-client.toml`)

```toml
[client]
    # Tint the vanilla atmosphere and fog blood-red during a Blood Moon night
    bloodMoonFogTint = true
    # Display a smooth, circular dark-crimson vignette overlay on screen during Blood Moon
    bloodMoonScreenVignette = true
    # Intensity of the blood-red vignette on screen edges during Blood Moon (0.1 to 1.0, default: 0.70)
    bloodMoonVignetteIntensity = 0.70
    # Automatically apply an optimized softer vignette balance when Complementary Unbound shader is detected
    bloodMoonComplementarySpecific = true
    # Spawn ambient crimson ember spores floating around the player during Blood Moon nights
    bloodMoonParticles = true
    # Play disturbing hallucinations and ambient sounds when paranoia is triggered
    paranoiaSoundsEnabled = true
```

---

## 💬 In-Game Commands

* `/enjoynights info`: Shows current day, lunar phase, night status, and days remaining until the next Blood Moon.
* `/enjoynights bloodmoon start`: *(Operator / Permission level 2)* Forces a Blood Moon siege to start for testing or server events.
* `/enjoynights bloodmoon stop`: *(Operator / Permission level 2)* Stops the forced Blood Moon.

---

## 🛠️ Build and Development

```bash
# Build the mod JAR
./gradlew build

# Run the client in development environment
./gradlew runClient

# Run the dedicated server in development environment
./gradlew runServer
```

The generated JAR file is located at: `build/libs`.
