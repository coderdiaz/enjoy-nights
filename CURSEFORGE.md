# Enjoy Nights! 🌙

**Transform your Minecraft nights from something you skip into an intense, rewarding, and atmospheric survival experience.**

Tired of players instantly skipping the night with a bed? Annoyed by annoying Phantoms? Want the moon phases to actually matter and fear the dark again? **Enjoy Nights!** reworks night mechanics with tactical bed resting, dynamic lunar phases, mineral drops, dark area paranoia, and periodic Blood Moon sieges where monsters can break down your doors!

---

## ✨ Features Overview

### 🛏️ Beds as Tactical Recovery Stations (No Night Skip!)
* **Sleeping no longer skips the night.** You can no longer fast-forward the daylight cycle.
* When you sleep in a bed, you rest for **10 seconds** before automatically standing up.
* Finishing your rest grants a **1 minute and 20 seconds (80s)** stamina and recovery buff:
  * **Regeneration II** (rapid healing)
  * **Resistance I** (damage reduction)
  * **Speed I** (stamina & agility)
  * **Absorption II** (extra golden hearts)
  * **Saturation** (fills hunger bars)
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
  * Slaying these powered monsters rewards **3x Experience**!

### 🕯️ Enclosed Dark Area Paranoia
* Hiding in a hole or bunker isn't completely safe. If you stand still for **more than 3 minutes (180s)** in an enclosed, dark area (0 block light, under a roof) during the night:
  * Paranoia sets in: you receive **Weakness** and visual darkness pulses.
  * You will hear **disturbing auditory hallucinations** (cave rumbles, distant creeper hisses, phantom screeches, Warden heartbeats).
  * Moving around, placing torches, or going outside clears the paranoia.

### 🩸 Blood Moon Sieges & Destructive AI
* Every **10 nights** (configurable on the server from 1 to 365 days), the **Blood Moon** rises with an ominous announcement and a blood-red sky.
* All monsters gain **Strength II, Resistance, and Speed**.
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
        bedBuffDurationSeconds = 80

    [server.phantoms]
        preventPhantoms = true

    [server.lunar_phases]
        fullMoonDiamondDropChance = 0.01
        fullMoonExtraDropsEnabled = true
        newMoonStealthFactor = 0.40
        fullMoonMobBuffsEnabled = true
        fullMoonExperienceMultiplier = 3

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
* **Side:** Required on **Server**, recommended on **Client** for red fog and atmospheric sound effects.
* Compatible with performance mods like Iris and Sodium / Embeddium.

---

<details>
<summary><b>🇪🇸 Descripción en Español (Haz clic para expandir)</b></summary>

### ¡Disfruta de las noches en Minecraft!
**Enjoy Nights!** convierte las noches de Minecraft en un verdadero reto de supervivencia:
- **Las camas ya no saltan la noche:** Ahora sirven como estaciones de recuperación. Al descansar 10 segundos recibes buffs de Regeneración II, Resistencia, Velocidad, Absorción y Saturación por 1 minuto y 20 segundos.
- **Sin Phantoms:** Adiós a las molestas pesadillas aéreas; el insomnio queda deshabilitado.
- **Fases Lunares:** Luna Nueva te otorga 60% más sigilo entre las sombras; Luna Llena trae mobs potenciados con Fuerza/Velocidad, 3x más experiencia y probabilidad de soltar hierro, gemas y diamantes (1%).
- **Paranoia en la Oscuridad:** Si pasas más de 3 minutos escondido y quieto en un lugar cerrado sin luz, sufrirás alucinaciones sonoras y debilidad.
- **Lunas de Sangre y Asedio (Cada 10 días):** Noche de asedio con niebla roja, mayor spawn controlado y zombies con IA destructiva capaz de derribar puertas, madera y tierra para alcanzarte, rastreándote a través de muros mediante "olfato de sangre".

Totalmente configurable en el servidor con comandos como `/enjoynights info` y `/enjoynights bloodmoon start`.
</details>
