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
  * Low outdoor visibility: dense dark fog when exposed under the open sky.
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

### 9. Warding Totem (Base Sanctuary & Perimeter Repulsion)
* **Craftable Base Protection:** Placeable mystic totem (`enjoy_nights:warding_totem`) that creates a protective boundary in a **100-block radius** (`wardingTotemRadius = 100`).
* **Perimeter Repulsion Forcefield:** Instead of deleting mob spawns, monsters attempting to spawn within the 100-block protection radius are pushed outward and relocated to the perimeter exterior (102–106 blocks away on safe surface ground), preserving world mob density and concentrating sieges outside your base walls.
* **Amethyst Catalyst GUI:** Right-clicking the totem opens an interactive interface with an activation slot. Depositing **8 Amethyst Shards** (`wardingTotemAmethystCost = 8`) and clicking **[Activar]** binds the totem to your UUID.
* **Strict Anti-Abuse (1 Active Totem per Player):** Each player can only maintain 1 active totem at a time. Attempting to activate a second totem elsewhere on the server is rejected and reports the coordinates of your active base totem.
* **Safe Deactivation & Refund:** Deactivating the totem frees the player's binding and safely returns the 8 amethyst shards to your inventory.
* **Permanent Destruction (No Silk Touch Recovery):** Mining or breaking the totem block in the world permanently destroys it without dropping the totem item (even when mined with a Silk Touch pickaxe). Any stored amethysts are refunded on break.

### 10. Blood Moon Zenith Boss (*El Heraldo Carmesí*) & Midnight Time Lock
* **Midnight Arrival:** Exactly at midnight (tick `18000`) of the Blood Moon, the terrifying champion **El Heraldo Carmesí** descends upon the world (`bloodMoonBossEnabled = true`).
* **Frozen Time Mechanic:** As long as the boss is alive, world daylight progression is **strictly locked at midnight** (`bloodMoonBossFreezeTime = true`). The blood-red moon stays hung in the sky and dawn will never arrive until the boss is slain.
* **Dynamic Player Scaling:** Maximum health and attack damage scale dynamically based on the number of active players nearby ($150\text{ HP} + 75\text{ HP}$ per extra player; $10\text{ damage} + 1.5$ per extra player).
* **On-Screen Boss Bar:** Displays a vivid red boss bar (`ServerBossEvent`) at the top of the screen tracking health and engagement within 128 blocks.
* **Imposing Scale & Colossus Stride:** Towering at **1.7x scale** (`Attributes.SCALE`) with a 1.25 step height (`Attributes.STEP_HEIGHT`), navigating single-block elevations effortlessly.
* **Smart Multi-Target Aggro:** Commits to targets in 5-second windows to avoid stuttering pathfinding bugs, while tracking player threat to aggressively retarget snipers and archers.
* **Elden Ring-Style Moveset & Telegraphed Attacks:**
  * **Conical Cleave Attack:** Sweeping netherite axe swings deal damage in a 120° frontal cone to all players in melee reach.
  * **Devastating Leap Slam (Anti-Kiting):** When targets stay between 8 and 24 blocks away, the boss launches into the air and slams down, inflicting AoE damage and **disabling shields for 5 seconds**.
  * **Ground Fissure:** Slams the ground launching all nearby players into the air with vertical knockup.
  * **Repulsion Wave (Anti-Gank):** If 3 or more players crowd the boss within 3.5 blocks, emits a concussive shockwave blasting everyone back.
  * **Phase 2 - Furia del Eclipse (< 40% HP):** Emits soul-fire trails, gains Speed & Strength, attacks ignite targets, and ability cooldowns are halved.
  * **Midnight Roar:** Emits an echoing roar inflicting Darkness and Slowness on nearby players (`bloodMoonBossRoarCooldownSeconds = 30`).
  * **Reinforcement Waves:** Summons fiery skeletons and armored zombies at 75%, 50%, and 25% health.
* **Glorious Victory:** Vanquishing the boss broadcasts a global victory message, unfreezes the passage of time toward dawn, and awards 500 XP, Diamonds, Emeralds, Amethyst Shards, and a Totem of Undying.
* **100% Server Configurable:** All boss stats, scaling, moveset cooldowns, and mechanics can be fine-tuned in `enjoy_nights-server.toml`.

### 11. Blood Moon Torch Immunity & Flame Skeletons
* **Torch & Light Immunity:** Outside of protected Warding Totem areas, monsters during a Blood Moon ignore block light emitted by torches, lanterns, and glowstone (`bloodMoonIgnoreTorches = true`). The Warding Totem is the only safe haven where monsters cannot spawn.
* **Flame Skeletons (15% chance):** Skeletons spawning during Blood Moon have a 15% chance (`bloodMoonFlameSkeletonChance = 0.15`) to spawn as fiery archers equipped with helmets and Flame-enchanted bows, firing incendiary arrows that ignite players and blocks (their flame bows and helmets never drop upon death to prevent loot farming).

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

    [server.warding_totem]
        # Radius in blocks protected by an active Warding Totem from mob spawns (default: 100)
        wardingTotemRadius = 100
        # Number of amethyst shards required to activate a Warding Totem (default: 8)
        wardingTotemAmethystCost = 8

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
        # Whether players outdoors during Blood Moon experience sporadic brief darkness pulses
        bloodMoonDarknessPulseEnabled = true
        # Monsters ignore light from torches outside wards during Blood Moon
        bloodMoonIgnoreTorches = true
        # Chance for skeletons to spawn as fire-shooting Flame Skeletons (15%)
        bloodMoonFlameSkeletonChance = 0.15

    [server.blood_moon_boss]
        # Enables the midnight Blood Moon Boss
        bloodMoonBossEnabled = true
        # Name displayed on the Boss Bar
        bloodMoonBossName = "Heraldo de la Luna de Sangre"
        # Base maximum health for 1 player (default: 150.0 = 75 hearts)
        bloodMoonBossBaseHealth = 150.0
        # Extra health added per nearby player (default: 75.0)
        bloodMoonBossHealthPerPlayer = 75.0
        # Base attack damage (default: 10.0)
        bloodMoonBossBaseDamage = 10.0
        # Extra attack damage per nearby player (default: 1.5)
        bloodMoonBossDamagePerPlayer = 1.5
        # Movement speed attribute (default: 0.30)
        bloodMoonBossSpeed = 0.30
        # Knockback resistance (default: 0.85)
        bloodMoonBossKnockbackResistance = 0.85
        # Experience points dropped upon defeat (default: 500)
        bloodMoonBossExperience = 500
        # Freeze time at midnight until boss is defeated
        bloodMoonBossFreezeTime = true
        # Summon reinforcement minion waves at 75%, 50%, and 25% health
        bloodMoonBossSummonMinions = true
        # Cooldown in seconds between roar attacks causing darkness/slowness
        bloodMoonBossRoarCooldownSeconds = 30
        # Scale multiplier of the boss model and hitbox (default: 1.7)
        bloodMoonBossScale = 1.7
        # Enable frontal conical cleave attacks hitting multiple players
        bloodMoonBossCleaveEnabled = true
        # Angle in degrees for the frontal cleave cone (default: 120.0)
        bloodMoonBossCleaveAngle = 120.0
        # Reach in blocks for cleaving attacks (default: 4.5)
        bloodMoonBossCleaveRadius = 4.5
        # Enable devastating leap slam attack at distant archers/players
        bloodMoonBossLeapAttackEnabled = true
        # Cooldown in seconds between leap slam attacks (default: 14s)
        bloodMoonBossLeapCooldownSeconds = 14
        # Cooldown in seconds between ground shockwave attacks (default: 18s)
        bloodMoonBossShockwaveCooldownSeconds = 18
        # Enable Phase 2 "Furia del Eclipse" below 40% health
        bloodMoonBossPhase2Enabled = true
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
    # Play phantom footsteps rushing behind player when stopping or mining during Blood Moon
    bloodMoonPhantomFootstepsEnabled = true
    # Red atmospheric sky & cloud tint passed to shaders during Blood Moon (0.0 to 1.0, default: 0.35)
    bloodMoonShaderAtmosphereIntensity = 0.35
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
