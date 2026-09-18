# Enjoy Nights! 🌙

**Transform your Minecraft nights from something you skip into an intense, rewarding, and atmospheric survival experience.**

Tired of players instantly skipping the night with a bed? Annoyed by annoying Phantoms? Want the moon phases to actually matter and fear the dark again? **Enjoy Nights!** reworks night mechanics with tactical bed resting, dynamic lunar phases, mineral drops, dark area paranoia, and periodic Blood Moon sieges where monsters can break down your doors!

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
  * Once you rest in bed, you cannot sleep again until **180 seconds (3 minutes)** have passed (`bedRestCooldownSeconds = 180`).
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
  * Atmospheric darkness: reduced view distance and dark fog when exposed outdoors.
  * **Stealth Bonus:** Reduces the range at which mobs can spot you by **60%**! Perfect for stealthy supply runs.
* **🌕 Full Moon (Phase 0 - Every 8 Nights):**
  * Monsters become hyper-aggressive with extended follow range (+16 blocks).
  * 50% chance for monsters to spawn with **Strength** and **Speed** potion effects.
  * **Anti-Exploit Creeper Mechanics:** Creepers do not receive potion effects (preventing permanent lingering potion clouds upon explosion); instead, they have a **5% chance to spawn as Charged Creepers** (`fullMoonChargedCreeperChance = 0.05`) with +15% movement speed!
  * Slaying these powered monsters rewards **3x Experience**!

### 🕯️ Enclosed Dark Area Paranoia
* Hiding in a hole or bunker isn't completely safe. If you stand still for **more than 3 minutes (180s)** in an enclosed, dark area (0 block light, under a roof) during the night:
  * Paranoia sets in: you receive **Weakness** and visual darkness pulses.
  * You will hear **disturbing auditory hallucinations** (cave rumbles, distant creeper hisses, phantom screeches, Warden heartbeats).
  * Moving around, placing torches, or going outside clears the paranoia.

### 🩸 Blood Moon Sieges & Destructive AI
* Every **10 nights** (configurable on the server from 1 to 365 days), the **Blood Moon** rises with an ominous announcement and a blood-red sky.
* **Universal Shader-Compatible Atmosphere:** Features a blood-red screen vignette overlay (100% visible even with shaders like Complementary Unbound or Iris), floating crimson ember particles (`CRIMSON_SPORE`), and dense crimson fog.
* All monsters gain **Strength II, Resistance, and Speed**.
* **Charged Creepers (25% chance):** Creepers have a 25% chance to spawn powered (`bloodMoonChargedCreeperChance = 0.25`) without dropping lingering potion clouds.
* **Intelligent Siege AI:**
  * **Blood Scent:** During the Blood Moon, zombies sense players through walls and doors (up to 32 blocks) without losing target when you enter a house.
  * **Breaching Barricades:** If you barricade yourself inside, zombies will actively break **wooden blocks** (doors, trapdoors, planks, logs, fences) and **dirt/dust blocks** (dirt, sand, gravel) with authentic hit and break sounds!
  * **Anti-Mountain Logic:** Zombies are smart enough to recognize mountain slopes and walkable terrain; they will never break hillside steps and will only destroy real obstacles blocking their path to you.
  * **Controlled Spawning:** Features extra companion spawns and periodic siege waves capped at 16 nearby mobs to prevent server lag or overwhelming swarms.

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

    [server.paranoia]
        paranoiaDurationSeconds = 180
        paranoiaWeaknessEnabled = true

    [server.blood_moon]
        bloodMoonIntervalDays = 10
        bloodMoonSiegeEnabled = true
        bloodMoonZombiesBreakBlocks = true
        bloodMoonExtraSpawnsEnabled = true
        bloodMoonExtraNaturalSpawnChance = 0.35
        bloodMoonSiegeWavesEnabled = true
        bloodMoonWaveIntervalSeconds = 30
        bloodMoonMaxNearbyMobs = 16

[client]
    bloodMoonFogTint = true
    bloodMoonScreenVignette = true
    bloodMoonVignetteIntensity = 0.70
    bloodMoonComplementarySpecific = true
    bloodMoonParticles = true
    paranoiaSoundsEnabled = true
```

---

## 💬 In-Game Commands

* `/enjoynights info`: Displays current day, day/night state, current moon phase, and days remaining until the next Blood Moon.
* `/enjoynights bloodmoon start`: *(Operator)* Manually triggers a Blood Moon for events or testing.
* `/enjoynights bloodmoon stop`: *(Operator)* Ends the forced Blood Moon.

---

## 💻 Compatibility & Requirements

* **Minecraft:** `1.21.11`
* **Mod Loader:** `NeoForge 21.11.45+`
* **Side:** Required on **Server**, recommended on **Client** for red fog, screen vignette, ember particles, and atmospheric sound effects.
* **Shaders:** Fully compatible with shaders including **Complementary Unbound**, **Iris**, **Sodium / Embeddium**, and Vanilla.
