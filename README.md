<div align="center">

# Cigarette V1

**High-Performance Open-Source Hybrid Ghost, Utility & Multi-Version Minecraft Client**

Engineered natively for **Minecraft `1.8`–`1.8.9` (Forge)** and **Minecraft `1.21+` / `26.x` (Fabric)** — playable across **every Minecraft server `1.8+` through `26.x`**

<p>
  <a href="https://github.com/taezeem14/Cigarette-V1/actions/workflows/build.yml">
    <img src="https://github.com/taezeem14/Cigarette-V1/actions/workflows/build.yml/badge.svg?style=for-the-badge" alt="CI Build" />
  </a>
  <a href="https://github.com/taezeem14/Cigarette-V1/releases/latest">
    <img src="https://img.shields.io/github/v/release/taezeem14/Cigarette-V1?style=for-the-badge&color=3882F6&label=Release" alt="Latest Release" />
  </a>
  <img src="https://img.shields.io/badge/Minecraft-1.8%2B%20Forge%20%7C%201.21%E2%80%9326.x%20Fabric-8B5CF6?style=for-the-badge" alt="Minecraft Versions" />
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/taezeem14/Cigarette-V1?style=for-the-badge&color=10B981" alt="GPL-3.0 License" />
  </a>
</p>

<p>
  <a href="#key-highlights">Highlights</a> •
  <a href="#supported-versions--compatibility">Compatibility</a> •
  <a href="#standalone-launcher--companion-gui">Launcher GUI</a> •
  <a href="#installation--setup">Installation</a> •
  <a href="#in-game-controls">Controls</a> •
  <a href="#module-reference">Modules (47)</a> •
  <a href="#building-from-source">Building</a>
</p>

</div>

---

## Overview

**Cigarette V1** is a dual-target, open-source Minecraft client featuring a custom **`#141418` Cigarette V1 dual-card ClickGUI**, an **executable Desktop Launcher & Multi-Version Companion App**, deterministic combat/movement mathematics, and live JSON profile synchronization. Both the **Legacy Forge (`1.8`–`1.8.9`)** root project and the **Modern Fabric (`1.21+` / `26.x`)** subproject ship all **47 real modules across 6 categories** with zero DRM, telemetry, or account locks.

### Key Highlights

- **Signature Cigarette V1 In-Game UI** — Auto-fitting dual-card ClickGUI (`Right Shift`) with glowing Cigarette V1 ember branding, cross-category live search bar, category enabled/total counters, laptop/trackpad-friendly left-click keybind capture, hover-description status bar, HUD snap editor, and **7 accent themes** (`Electric Blue`, `Ember Orange`, `Amethyst`, `Emerald`, `Crimson Rose`, `Cyber Cyan`, `Rainbow`).
- **Standalone Launcher & Companion Desktop GUI** — Double-click either `.jar` (`java -jar ...`) to launch **Cigarette V1 Desktop**: custom anti-aliased Java2D controls, interactive sliders/switches/keybinds for all 47 modules, 1-click **TLauncher / Prism / Official Launcher** starter, 1-click **ViaForge / ViaFabricPlus** multi-version protocol installer, and **Minimize to System Tray** background mode.
- **47 Working Modules (Singleplayer & Multiplayer Ready)** — Every combat, movement, render, utility, and world module works out of the box against players, hostile mobs, and passive animals in Singleplayer, LAN, OptiFine (`HD U M5`), and Multiplayer servers.
- **Playable Across Every Server `1.8+` – `26.x`** — Includes built-in `1.9+ Cooldown` weapon-recharge pacing in `KillAura` and `AutoClicker` plus 1-click **ViaForge / ViaFabricPlus** installation so a single setup connects to `1.8`, `1.12.2`, `1.16.5`, `1.20.x`, `1.21.x`, and `26.x` servers seamlessly.
- **Fault-Tolerant Mixin + Forge Event Fallback** — Non-fatal Mixin configuration paired with an automatic `ForgeEventBridge` tick/render/input fallback ensures Cigarette V1 never crashes custom Forge or OptiFine setups.

---

## Supported Versions & Compatibility

| Edition | Native Loader Range | Required Java | Multi-Version Server Protocol (`1.8` – `26.x`) | Release Asset |
| :--- | :--- | :--- | :--- | :--- |
| **Classic & Multi-Protocol Edition** | **Forge `1.8` – `1.8.9`** (incl. OptiFine) | **Java 8** | Join **`1.7` – `1.21.x` – `26.x`** servers via **ViaForge** (1-click install in Launcher GUI) | `Cigarette-V1-1.4.0-mc1.8.9-forge.jar` |
| **Modern Native Edition** | **Fabric `1.21+` – `26.x`** (`Fabric API`) | **Java 21 / 25+** | Join **`1.8` – `26.x`** servers via **ViaFabricPlus** (1-click install in Launcher GUI) | `Cigarette-V1-1.4.0-mc26.3-fabric.jar` |

### Official, Cracked & Offline Launchers

Cigarette V1 runs strictly client-side with **no** Mojang/Microsoft session verification, remote whitelist, or hardware lock:

- **Singleplayer, LAN & Offline Servers (`online-mode=false`)**: Works out of the box with **TLauncher**, **SKLauncher**, **Prism Launcher (Offline)**, **MultiMC**, and the **Official Launcher**.
- **Premium Servers (`online-mode=true`)**: Servers enforcing Mojang session authentication validate accounts server-side independently of client mods.

> [!IMPORTANT]
> **Keep `1.8.9 Forge` JARs and `Fabric` JARs in separate `mods/` folders** (or swap out the unused JAR when switching loaders).

---

## Standalone Launcher & Companion GUI

Every Cigarette V1 `.jar` is both a **Minecraft Mod** and a **Standalone Desktop Application**. Double-click the `.jar` or run:

```bash
java -jar Cigarette-V1-1.4.0-mc1.8.9-forge.jar
```

- **Dashboard & Quick Actions** — Live Minecraft JVM detection, 1-click **Install Mod to `.minecraft/mods/`**, **Open ClickGUI In-Game**, **Launch Minecraft Launcher** (auto-detects TLauncher, Prism, MultiMC, Modrinth, and Official Launcher), and **Hide to System Tray**.
- **Full 47-Module Configurator** — Configure module toggles, number sliders, min/max range sliders, mode selectors, and keybinds outside or during gameplay, with sub-second `.bridge_sync` live reload into running Minecraft.
- **Multi-Version `1.8+` – `26.x` Hub** — Scans installed launcher versions and downloads **ViaForge** or **ViaFabricPlus** directly from Modrinth into your `mods/` folder in one click.

---

## Installation & Setup

Download the latest release from **[GitHub Releases (`v1.4.0`)](https://github.com/taezeem14/Cigarette-V1/releases/latest)**:
- **Minecraft `1.8`–`1.8.9` (Forge / ForgeOptiFine)**: Download **`Cigarette-V1-1.4.0-mc1.8.9-forge.jar`**.
- **Minecraft `26.3` / Modern Fabric**: Download **`Cigarette-V1-1.4.0-mc26.3-fabric.jar`** and **[Fabric API](https://modrinth.com/mod/fabric-api)**.

<details open>
<summary><b>Quickest Setup (Using Built-In Cigarette V1 Launcher GUI)</b></summary>

1. Double-click **`Cigarette-V1-1.4.0-mc1.8.9-forge.jar`** (or run `java -jar Cigarette-V1-1.4.0-mc1.8.9-forge.jar`).
2. Select your Minecraft / TLauncher directory (auto-detected, e.g., `D:\Minecraft-launcher` or `%appdata%\.minecraft`) and click **Install to Mods**.
3. *(Optional — Play on `1.9`–`1.21.x`–`26.x` servers)* Open the **Launcher & Versions** tab and click **Install ViaForge (1.8+ -> 26.x)**.
4. Click **Launch Minecraft**, enter any Singleplayer world or Multiplayer server, and press **`Right Shift`**!

</details>

<details>
<summary><b>TLauncher / SKLauncher Manual Setup</b></summary>

#### A. Classic & Multi-Version (`Forge 1.8.9` / `ForgeOptiFine 1.8.9`)
1. Select **`Forge 1.8.9`** or **`ForgeOptiFine 1.8.9`** in the launcher version list.
2. Point **Settings → Java Path** to **Java 8** (`javaw.exe`).
3. Drop `Cigarette-V1-1.4.0-mc1.8.9-forge.jar` into your launcher's `mods/` folder (remove any older `Cigarette-V1-*.jar`).
4. Click **Enter the Game**.

#### B. Modern Native (`Fabric 26.3`)
1. Select **`Fabric 26.3`** (`Fabric Loader 0.20.1+`) and **Java 25** in launcher settings.
2. Place `Cigarette-V1-1.4.0-mc26.3-fabric.jar` and `fabric-api-*.jar` into `mods/`.
3. Click **Enter the Game**.

</details>

<details>
<summary><b>Prism Launcher / Official Launcher Setup</b></summary>

1. Create an isolated instance for **`1.8.9` + Forge (`11.15.1.2318`, Java 8)** or **`26.3` + Fabric (`0.20.1+`, Java 25)**.
2. Add the matching `Cigarette-V1-1.4.0-*.jar` to the instance's `mods/` folder (plus `Fabric API` on Fabric).
3. Launch the instance and press **`Right Shift`** in-game.

</details>

---

## In-Game Controls

| Control / UI Element | Action |
| :--- | :--- |
| **`Right Shift`** or **`Insert`** | Open / close the Cigarette V1 ClickGUI |
| **Left-Click** module row | Toggle module ON / OFF |
| **Click `⋮` icon** or **Right-Click** row | Expand / collapse module settings, **Keybind** row, and **Show in ArrayList** toggle |
| **Click `Keybind` row** (or Middle-Click) | Capture any keyboard key (`ESC`, `Delete`, or `Backspace` clears to `None`) |
| **Header Search Pill (`Search all...`)** | Instant cross-category filter across all 47 modules by name or description |
| **Gear Icon (`⚙`) → Settings** | Switch between 7 themes, adjust GUI scale (`0.70x`–`1.25x`), toggle Gaussian blur, or toggle **`1.9+ Server Cooldown`** |
| **Sidebar → `HUD Editor`** | Drag-and-snap **Watermark**, **ArrayList**, **TargetHUD**, and **Keystrokes** (`ESC` or Right-Click returns) |

**Profile Storage Locations**
- **Forge (`1.8.9`)**: `<minecraft-dir>/cigarette/profiles/<name>.json`
- **Fabric (`26.3`)**: `<minecraft-dir>/Cigarette/profiles/<name>.json`

---

## Module Reference

Cigarette V1 ships **47 modules** across **6 categories**:

| Category | Count | Summary |
| :--- | :---: | :--- |
| **Combat** | 10 | GCD aim smoothing, Gaussian autoclicker, auto-sprint, reach, hitboxes, velocity, W-Tap, silent aim, utilities |
| **Blatant** | 10 | KillAura (`AutoBlock` + `1.9+ Cooldown`), Scaffold, Speed, Fly, Criticals, NoSlow, KeepSprint, Blink, Timer, AntiVoid |
| **Render** | 12 | Cigarette V1 ClickGUI, HUD (`FPS`/`CPS`/`BPS`), TargetHUD, Keystrokes, ArmorHUD, ESP, Chams, Nametags, Tracers, BedESP, Fullbright, Trajectories |
| **Utility** | 5 | ChestStealer, InvManager, AutoArmor, AntiBot, Teams |
| **World** | 7 | FastPlace, Eagle, BridgeAssist, AutoTool, NoFall, SpeedMine, Step |
| **Client** | 3 | Active module ArrayList controller, FriendManager protection, clean `SelfDestruct` teardown |

<details open>
<summary><b>Expand Complete 47-Module List</b></summary>

- **Combat (10)**: `AimAssist`, `AutoClicker`, `Sprint`, `Reach`, `HitBoxes`, `Velocity`, `WTap`, `SilentAim`, `ThrowPot`, `RodAimbot`
- **Blatant (10)**: `KillAura`, `Scaffold`, `Speed`, `Fly`, `Criticals`, `NoSlow`, `KeepSprint`, `Blink`, `Timer`, `AntiVoid`
- **Render (12)**: `ClickGUI`, `HUD`, `TargetHUD`, `Keystrokes`, `ArmorHUD`, `ESP`, `Chams`, `Nametags`, `Tracers`, `BedESP`, `Fullbright`, `Trajectories`
- **Utility (5)**: `ChestStealer`, `InvManager`, `AutoArmor`, `AntiBot`, `Teams`
- **World (7)**: `FastPlace`, `Eagle`, `BridgeAssist`, `AutoTool`, `NoFall`, `SpeedMine`, `Step`
- **Client (3)**: `ArrayList`, `FriendManager`, `SelfDestruct`

</details>

---

## Troubleshooting

| Symptom | Likely Cause | Resolution |
| :--- | :--- | :--- |
| Crash on startup with `UnsupportedClassVersionError` | Java version mismatch | Use **Java 8** for `1.8.9 Forge`; use **Java 25+** for `26.3 Fabric`. |
| `ModResolutionException` / Mixin conflict | Both `1.8.9` and `26.3` JARs placed in the same `mods/` folder | Keep only the JAR matching your active loader in `mods/`. |
| Modules only target players or `AimAssist` pauses near grass | Older `v1.3.0` config cached in `default.json` | Update to **`v1.4.0`** (auto-enables mob targeting & non-blocking raytrace checks) or create a fresh profile in **Profiles**. |
| `LWJGLException: Pixel format not accelerated` (1.8.9 on Intel HD Windows 10) | Legacy Intel HD OpenGL driver rejecting newer Java 8 builds | Use Eclipse Adoptium JDK `8u512` (or set Windows 7/8 compatibility on `javaw.exe`). |

---

## Architecture

```text
Cigarette-V1/
├── .github/workflows/                 # Dual CI matrix (Java 8 1.8.9 Forge + Java 25 26.3 Fabric) & Release automation
├── build.gradle                       # Root build: Minecraft 1.8.9 Forge + Standalone Desktop Manifest
├── src/main/java/dev/taezeem14/cigarette/
│   ├── Cigarette.java                 # Forge @Mod bootstrap, lifecycle hooks & BlurUtil teardown
│   ├── desktop/                       # Standalone Cigarette V1 Launcher, Module Configurator & ViaForge Installer
│   ├── forge/ForgeEventBridge.java    # Fault-tolerant Forge tick/render/input bridge
│   ├── mixin/impl/                    # SpongePowered Mixin 0.7.11 hooks (MCP 1.8.9 SRG)
│   ├── module/impl/                   # 47 modules across Combat, Blatant, Render, Utility, World & Client
│   └── ui/                            # Cigarette V1 dual-card ClickGUI, HUDEditor & GLSL ShaderGroup blur
│
└── modern/                            # Standalone Gradle subproject: Minecraft 26.3 Fabric (Loom 1.19.4, JDK 25)
    ├── build.gradle                   # Unobfuscated 26.3 Loom config + Java 8 Desktop Launcher main-class
    └── src/
        ├── main/java/.../desktop/     # Shared Standalone Cigarette V1 Launcher & ViaFabricPlus Installer
        └── client/java/.../           # 47 modules + GuiGraphicsExtractor Cigarette V1 ClickGUI & HUD
```

---

## Building from Source

### Build Minecraft `1.8.9` Forge + Desktop Launcher (Repository Root — JDK 8)

```bash
# Linux / macOS
./gradlew setupCiWorkspace clean build --no-daemon

# Windows (PowerShell / CMD)
.\gradlew.bat setupCiWorkspace clean build --no-daemon
```

> Output binary: `build/libs/Cigarette-V1-1.4.0.jar`

### Build Minecraft `26.3` Fabric + Desktop Launcher (`modern/` — JDK 25)

```bash
cd modern

# Linux / macOS
./gradlew clean build --no-daemon

# Windows (PowerShell / CMD)
.\gradlew.bat clean build --no-daemon
```

> Output binary: `modern/build/libs/Cigarette-V1-1.4.0-mc26.3-fabric.jar`

---

## Disclaimer & License

> [!NOTE]
> Cigarette V1 is intended strictly for educational software-engineering study, JVM instrumentation research, singleplayer worlds, and private testing environments where client modifications are explicitly permitted.

Distributed under the **[GNU General Public License v3.0](LICENSE)** — Copyright © 2026 **[taezeem14](https://github.com/taezeem14)** (Muhammad Taezeem Tariq).
