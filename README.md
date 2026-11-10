# 🧙 VoiceMage

![Version](https://img.shields.io/badge/version-2.14.1-4169E1)
![Minecraft](https://img.shields.io/badge/Minecraft-26.2-2E8B57?logo=minecraft)
![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4)
![Java](https://img.shields.io/badge/Java-25%2B-ED8B00)
![License](https://img.shields.io/badge/License-MIT-2E8B57)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-5865F2?logo=discord&logoColor=white)](https://discord.gg/cPfjZetypC)

**VoiceMage** is a Fabric mod for Minecraft 26.2 that turns your microphone into an arcane spellbook. Say *"fireball"* and a fireball flies. Say *"meteor"* and the sky burns. Say *"fly"* and take to the skies. 

Speech recognition runs **100% locally on your machine** via bundled offline Vosk models—no cloud, no accounts, and zero audio ever leaves your computer. The server manages all effects, mana checks, cooldowns, and world interactions.

---

## ✨ Features

### 🎙️ Voice Casting & Speech Engine
* **🗣️ 16 Built-in Spells:** Cast offensive blasts, defensive barriers, teleports, and creative flight with spoken incantations.
* **✨ Dynamic Custom Spells:** Create your own spells in-game! Input any spoken phrase, exact Minecraft particle ID (`minecraft:flame`, `witch`, `soul_fire_flame`, `sonic_boom`, etc.), mana cost, and power.
* **🔄 Hot-Reload Grammar:** The voice recognizer dynamically updates its vocabulary when custom spells are created or edited without restarting your microphone.
* **📴 Fully Offline Recognition:** Powered by [Vosk](https://github.com/alphacep/vosk-api). The speech model is packed directly into the mod JAR—no external downloads needed.
* **🎤 Microphone Selector:** Switch input devices seamlessly in-game with `/voicemic`. Non-standard 16 kHz audio devices are converted automatically.
* **⚡ Instant Mute:** Press **V** at any time to toggle microphone listening.

### 🖥️ In-Game Dark Operator GUIs
* **⚙️ Configuration Menu (`/voicemage settings`):** A dark obsidian GUI inspired by *Recolourful Containers DARK*. Easily tune `voicemage.json` parameters via dedicated tabs:
  * **⚡ Mana & Rules:** Pool size, regeneration rate, cast interval, explosion chant toggles, and flight duration.
  * **💥 Combat & World:** Explosion power, blast radius, crater depth, fireball power, meteor stats, and lightning damage.
  * **✨ Custom Spells Editor:** Add, test, and delete custom spells with instant particle ID validation and quick presets (`Flame`, `Witch`, `Soul`, `Sonic`, `Cloud`).
* **📖 Spell Grimoire (`/voicemage spells`):** A dark wizard spell tome displaying all 16 built-in spells + custom spells. Filter by category (`Offense`, `Defense`, `Utility`, `Custom`), search in real time, and **fine-tune live mana costs** directly with interactive `[-]`, `[+]`, and `[Save]` buttons.

### 🔮 Mana & Server Authority
* **💧 Mana HUD:** On-screen mana bar with real-time regeneration and voice feedback status.
* **🛡️ Server-Authoritative:** Server enforces range checks, line of sight, cooldowns, and permissions.
* **🔒 Operator Gated:** Settings, Grimoire tuning, and custom spells are locked to permission level 2 (Operators / Gamemasters).
* **🧹 Crash-Resistant Cleanup:** Temporary walls, ice cages, clones, and polymorphed mobs are safely cleaned up even after server restarts.

---

## 🪄 Spells

| Incantation | Mana | Effect | Category |
| :--- | ---: | :--- | :--- |
| 🔥 **Fireball** | 35 | Launches a blazing fireball in your crosshair direction. Detonates on impact. | Offense |
| 🕳️ **Pitfall** | 60 | Collapses a column of blocks ahead of you into a deep pit. | Utility |
| 💚 **Heal** | 30 | Restores 4 hearts of health. Costs nothing at full HP. | Defense |
| ⚡ **Lightning** | 45 | Calls down a devastating thunderbolt on your target (up to 64 blocks). | Offense |
| 🪨 **Earth Wall** | 25 | Raises a 5×4 temporary stone wall that blocks attacks and fades after 8s. | Defense |
| 🌀 **Blink** | 20 | Instantly teleports forward in the direction you look (up to 15 blocks). | Utility |
| ✨ **Magic Missile** | 25 | Fires rapid homing projectiles of pure arcane energy. | Offense |
| ☄️ **Meteor** | 70 | Summons a flaming celestial boulder from above that explodes on impact. | Offense |
| 💨 **Wind Blast** | 30 | Emits a powerful gale that damages and knocks back enemies. | Offense |
| ❄️ **Freeze** | 50 | Encased target enemies inside solid glacial ice for a duration. | Defense |
| 👥 **Clone** | 40 | Creates illusory decoys that mimic and protect you from hostile mobs. | Defense |
| 🐑 **Polymorph** | 55 | Transfigures targeted creatures into a harmless wandering sheep. | Utility |
| 🔬 **Shrink** | 40 | Shrinks targeted entity size by 65%, reducing reach and hitbox. | Utility |
| 💥 **Explosion** | 100 | The legendary Megumin-style tactical blast of cataclysmic destruction. | Offense |
| 🕊️ **Fly** | 30 | Grants creative flight for 10s with wind swirls, followed by a 5s cooldown. | Utility |
| 💀 **Get Doomed** | 50 | Halves target HP using dark warden magic. Stops at half a heart. | Offense |

---

### 💥 The Explosion Chant

**Explosion** drains a full mana bar and, by default, requires chanting:

> *O ancient flame, awaken from your endless slumber. Let the skies burn and turn my enemies to ash, leaving nothing behind. **EXPLOSION!***

> *Darkness blacker than black and darker than dark,*  
> *I beseech thee, combine with my deep crimson.*  
> *Return all creation to cinders,*  
> *and come from the abyss!*  
> ***EXPLOSION!***

Operators can toggle whether the chant is required via `/voicemage chant false` or inside `/voicemage settings`.

---

## 🎮 Commands & Permissions

### Server Commands

| Command | Description | Access |
| :--- | :--- | :--- |
| `/voicemage settings` | Opens the graphical **VoiceMage Configuration GUI** | **Op Only** |
| `/voicemage spells` | Opens the graphical **Dark Spell Grimoire** (with live cost tuning) | **Op Only** |
| `/spells` | Opens the Grimoire (or prints spell list if in console) | **Op Only** |
| `/voicemage` | Displays current mana cap, regeneration rate, and chant rules | Everyone |
| `/voicemage chant <true\|false>` | Toggles whether Explosion requires the full incantation | **Op Only** |
| `/voicemage regen <rate>` | Sets mana regeneration per second (0–100) | **Op Only** |
| `/voicemage <spell> <cost>` | Sets mana cost of a specific spell | **Op Only** |
| `/voicemage reload` | Reloads `config/voicemage.json` and syncs to all players | **Op Only** |
| `/voicemage settings add <phrase> <particle> [mana] [power]` | Registers a new custom spell | **Op Only** |
| `/voicemage settings remove <phrase>` | Deletes a custom spell | **Op Only** |

### Client Commands & Keybinds

| Command / Key | Action |
| :--- | :--- |
| `/voicemic` | Lists recording devices detected by Java and highlights active mic |
| `/voicemic <id>` | Switches microphone and restarts recognition |
| `/voicemic default` | Resets to system default microphone |
| **V** | Mute / unmute voice recognition |

---

## ⚙️ Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) (0.19.3+) for Minecraft 26.2 and the Fabric API.
2. Ensure you have **Java 25+** installed.
3. Place `voicemage-26.2-<version>.jar` into your `.minecraft/mods` folder (both client and server).
4. Launch the game. On first startup, the mod extracts the built-in speech model. When the HUD shows **Voice: listening**, you are ready to cast!

---

## 🧩 Configuration (`config/voicemage.json`)

You can edit all settings in-game via **`/voicemage settings`**, or directly in `config/voicemage.json`:

```json
{
  "maximumMana": 100.0,
  "manaRegenerationPerSecond": 5.0,
  "minimumCastIntervalTicks": 6,
  "explosionChantRequired": true,
  "pitfallEnabled": true,
  "flyManaCost": 30,
  "flyDurationTicks": 200,
  "flyCooldownTicks": 100,
  "getDoomedManaCost": 50,
  "customSpells": [
    {
      "phrase": "blaze wrath",
      "particleName": "minecraft:flame",
      "manaCost": 25.0,
      "damageOrEffect": 6.0
    }
  ]
}
```

<details>
<summary><b>📋 All Configuration Parameters</b></summary>

<br>

| Option | Default | Description |
| :--- | ---: | :--- |
| `maximumMana` | 100 | Size of the mana pool |
| `manaRegenerationPerSecond` | 5 | Mana regained per second |
| `minimumCastIntervalTicks` | 6 | Delay between casts (20 ticks = 1s) |
| `explosionChantRequired` | true | Require full chant for Explosion |
| `flyManaCost` / `flyDurationTicks` / `flyCooldownTicks` | 30 / 200 / 100 | Mana cost, duration (ticks), and cooldown (ticks) for Fly |
| `getDoomedManaCost` | 50 | Mana cost for Get Doomed |
| `fireballExplosionPower` | 4 | Fireball blast strength |
| `healAmount` | 8 | Health restored by Heal (2 = 1 heart) |
| `lightningDamage` / `lightningRange` | 16 / 64 | Lightning damage and reach |
| `earthWallWidth` / `earthWallHeight` / `earthWallLifetimeSeconds` | 5 / 4 / 8 | Wall dimensions and lifetime |
| `blinkRange` | 15 | Blink distance in blocks |
| `magicMissileDamage` / `magicMissileRange` | 10 / 32 | Missile damage and range |
| `meteorExplosionPower` / `meteorRange` | 8 / 48 | Meteor blast power and range |
| `windBlastRange` / `windBlastDamage` / `windBlastStrength` | 12 / 2 / 2.8 | Wind blast reach, damage, and knockback |
| `freezeRadius` / `freezeDurationTicks` | 4 / 60 | Glacial freeze area and duration |
| `cloneCount` / `cloneDurationTicks` | 3 / 120 | Number of decoy clones and lifetime |
| `polymorphDurationTicks` / `shrinkDurationTicks` | 100 / 120 | Polymorph and Shrink durations |
| `explosionPower` / `explosionRange` | 96 / 64 | Explosion strength and aim distance |
| `explosionCraterRadius` / `explosionCraterDepth` | 38 / 128 | Crater radius and depth |

</details>

---

## 🏗️ Building From Source

Requires **JDK 25**.

```bash
git clone https://github.com/Aradhy-arch/VoiceMage.git
cd VoiceMage
./gradlew build
```

Compiled JARs will be generated in `build/libs/`.

---

## 📜 Credits & License

* **VoiceMage** was originally created by **Cayce** and is licensed under the [MIT License](LICENSE).
* Rebuilt and maintained with ❤️ by **Aradhy**.
* Speech recognition powered by [Vosk](https://github.com/alphacep/vosk-api) (Apache 2.0).
