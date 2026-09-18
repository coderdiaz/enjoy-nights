# Enjoy nights! 🌙

Mod para **Minecraft 1.21.11** y **NeoForge 21.11.45** que transforma la noche en una experiencia desafiante, gratificante y atmosférica. Convierte las camas en estaciones de preparación y descanso temporal, recompensa la supervivencia nocturna con fases lunares y drops mejorados, y desata eventos periódicos de asedio con Lunas de Sangre e IA destructiva.

---

## 🌟 Características Detalladas

### 1. La Noche ya no se puede omitir (No Time Skip)
* Dormir en una cama ya **no adelanta el tiempo** a la mañana ni salta la noche.
* Tras acostarse durante **10 segundos (200 ticks)** de descanso reparador, el jugador se levantará automáticamente de la cama sin alterar el ciclo de luz.

### 2. Estación de Recuperación (Cama como Buff Station)
* Al completar los 10 segundos de descanso en la cama, el jugador recibe efectos de recuperación y resistencia durante **1 minuto y 20 segundos (80 segundos)**:
  * **Regeneración II** (recuperación acelerada de vida)
  * **Resistencia I** (reducción de daño recibido)
  * **Velocidad I** (estamina y agilidad para el combate)
  * **Absorción II** (corazones dorados adicionales)
  * **Saturación** (rellena barras de comida al instante)
* ¡Ideal para prepararte antes de salir a explorar o defender tu base de los peligros nocturnos!

### 3. Supresión Total de Pesadillas (Phantoms Eliminados)
* Los **Phantoms** ya no aparecen de manera natural por falta de sueño.
* El contador interno de insomnio (`Stats.TIME_SINCE_REST`) se reinicia periódicamente para que puedas disfrutar de largas noches de minería o combate sin molestias aéreas.

### 4. Drops Minerales Mejorados bajo Luna Llena (Full Moon)
* Durante las noches de **Luna Llena** (y Lunas de Sangre), los monstruos estándar (Zombies, Esqueletos, Creepers, Arañas, etc.) tienen probabilidad de soltar minerales al morir:
  * **Fragmentos de Hierro:** 35% Hierro en bruto (1-2) o 25% Pepitas de hierro (2-5).
  * **Gemas:** 20% Lapislázuli (1-4), 15% Esquirlas de amatista (1-2) u 8% Esmeraldas (1).
  * **Diamante:** Probabilidad del **1%** (`0.01`).

### 5. Fases Lunares Dinámicas
Aprovecha el ciclo astronómico nativo de 8 fases de Minecraft:
* **Luna Nueva (New Moon - Fase 4, cada 8 noches):**
  * La visibilidad en el exterior disminuye (niebla oscura densa y efecto de oscuridad ambiental bajo el cielo nocturno).
  * **Sigilo Incrementado:** Reduce en un **60%** la distancia a la cual los monstruos pueden detectarte (`newMoonStealthFactor = 0.40`), ideal para moverse sin ser visto.
* **Luna Llena (Full Moon - Fase 0, cada 8 noches):**
  * Los monstruos se vuelven más agresivos y tienen un 50% de probabilidad de aparecer con efectos de poción como **Fuerza** y **Velocidad**.
  * Rango de persecución aumentado (+16 bloques).
  * Al derrotar a estos monstruos potenciados, obtienes **3x más experiencia** (`fullMoonExperienceMultiplier = 3`).

### 6. Paranoia en la Oscuridad (Miedo al Confinamiento)
* Si un jugador permanece **más de 3 minutos (180s) quieto** en un área cerrada (bajo techo o sin luz solar) y con **luz de bloque en 0** durante la noche:
  * Entra en estado de paranoia y recibe **Debilidad (Weakness)** y pulsos de **Oscuridad (Darkness)**.
  * Escuchará **alucinaciones sonoras perturbadoras** (crujidos de cuevas, latidos de corazón de Warden, aleteos lejanos de phantom, siseos de creeper).
  * Aparece un susurro escalofriante en la barra de acción (*"Sientes una presencia escalofriante observándote en la oscuridad..."*).
  * Moverse, salir al exterior o iluminar el área con antorchas/faroles disipa la paranoia.

### 7. Noches de Asedio y Luna de Sangre (Blood Moon)
* Ocurre periódicamente cada **10 noches** por defecto (`bloodMoonIntervalDays = 10`, configurable de 1 a 365 días en el servidor).
* Al caer la noche de Luna de Sangre:
  * Suena una advertencia ominosa y se envía un anuncio en rojo a todos los jugadores: *¡La Luna de Sangre asciende... El asedio comienza!*
  * El cielo y la niebla se tiñen de un color **rojo sangre** atmosférico.
  * Hereda todos los drops minerales y 3x XP de la Luna Llena, con un 75% de probabilidad de que los monstruos tengan **Fuerza II**, **Resistencia** y **Velocidad**.
* **Generación de Mobs de Asedio Equilibrada (Anti-Lag):**
  * Spawns naturales con 35% de generar un compañero adicional.
  * Pequeñas oleadas de 1-2 zombies cada 30 segundos alrededor de jugadores activos.
  * **Tope Máximo de Seguridad (`bloodMoonMaxNearbyMobs = 16`):** Si ya hay 16 o más monstruos cerca del jugador, la generación extra se pausa automáticamente para prevenir lag o saturación.

### 8. IA de Asedio Avanzada para Zombies
* **Rastro de Sangre (Detección a través de paredes):** Durante la Luna de Sangre, los zombies perciben al jugador a través de paredes y puertas cerradas (`mustSee = false`) en un radio de hasta 32 bloques, por lo que **no perderán su objetivo** al entrar a una casa.
* **Filtro Anti-Montañas:** La IA distingue el terreno transitable de las barricadas reales. **Nunca romperá escalones o desniveles naturales de tierra**; los saltará normalmente para perseguirte.
* **Brecha Implacable de Barricadas:**
  * Si te encierras o colocas barricadas, el zombie se ancla frente al obstáculo (`navigation.stop()`) y golpea de forma continua sin cancelaciones accidentales por ventanas o movimientos internos.
  * Puede romper **bloques de madera** (puertas, trampillas, tablones, troncos, vallas) y **bloques de polvo/tierra** (tierra, arena, grava).
  * Emite los sonidos auténticos de Minecraft de golpeo (`1019`) y rotura completa de puerta (`1021`), destruyendo ambas mitades para que la entrada quede totalmente despejada y cargar hacia el jugador.

---

## ⚙️ Configuración del Servidor (`config/enjoy_nights-server.toml`)

Todas las mecánicas son 100% personalizables:

```toml
[server]
    [server.bed_and_sleep]
        # Evita que dormir salte la noche
        preventNightSkip = true
        # Duración del descanso en cama antes de levantarse automáticamente (segundos)
        bedSleepDurationSeconds = 10
        # Duración de los buffs de recuperación y estamina (segundos: 80s = 1m 20s)
        bedBuffDurationSeconds = 80

    [server.phantoms]
        # Deshabilita la aparición natural de Phantoms por insomnio
        preventPhantoms = true

    [server.lunar_phases]
        # Probabilidad de soltar diamantes en Luna Llena / Sangre (0.01 = 1%)
        fullMoonDiamondDropChance = 0.01
        # Habilita drops adicionales de hierro y gemas
        fullMoonExtraDropsEnabled = true
        # Factor de visibilidad en Luna Nueva (0.40 = 60% más sigilo)
        newMoonStealthFactor = 0.40
        # Mobs potenciados con Fuerza y Velocidad en Luna Llena
        fullMoonMobBuffsEnabled = true
        # Multiplicador de experiencia para mobs potenciados
        fullMoonExperienceMultiplier = 3

    [server.paranoia]
        # Segundos quieto en área oscura cerrada antes de entrar en paranoia (180s = 3 min)
        paranoiaDurationSeconds = 180
        # Habilita efectos de debilidad y alucinaciones sonoras
        paranoiaWeaknessEnabled = true

    [server.blood_moon]
        # Cada cuántas noches ocurre la Luna de Sangre (1 a 365 días)
        bloodMoonIntervalDays = 10
        # Habilita el evento de Luna de Sangre
        bloodMoonSiegeEnabled = true
        # Permite que los zombies rompan madera y tierra
        bloodMoonZombiesBreakBlocks = true
        # Generación de mobs de asedio incrementada y equilibrada
        bloodMoonExtraSpawnsEnabled = true
        bloodMoonExtraNaturalSpawnChance = 0.35
        bloodMoonSiegeWavesEnabled = true
        bloodMoonWaveIntervalSeconds = 30
        # Límite máximo de monstruos cercanos antes de pausar spawns extra (anti-lag)
        bloodMoonMaxNearbyMobs = 16
```

---

## 💬 Comandos Disponibles

* `/enjoynights info`: Muestra el día actual, fase lunar, estado de la noche y días restantes para la próxima Luna de Sangre.
* `/enjoynights bloodmoon start`: *(Nivel OP / Permiso 2)* Fuerza el inicio de una Luna de Sangre para pruebas o eventos.
* `/enjoynights bloodmoon stop`: *(Nivel OP / Permiso 2)* Detiene la Luna de Sangre forzada.

---

## 🛠️ Compilación y Desarrollo

```bash
# Compilar el mod
./gradlew build

# Ejecutar cliente en entorno de desarrollo
./gradlew runClient

# Ejecutar servidor en entorno de desarrollo
./gradlew runServer
```

El archivo JAR generado se encuentra en: `build/libs/enjoy_nights-1.0.0.jar`.
