<div align="center">

# Cigarette V1

**Open-Source Hybrid Ghost & Utility Client Modeled After Vape V4**

Natively engineered for **Minecraft `26.3` (Fabric)** and **Minecraft `1.8.9` (Forge)**

<p>
  <a href="https://github.com/taezeem14/Cigarette-V1/actions/workflows/build.yml">
    <img src="https://github.com/taezeem14/Cigarette-V1/actions/workflows/build.yml/badge.svg?style=for-the-badge" alt="CI Build" />
  </a>
  <a href="https://github.com/taezeem14/Cigarette-V1/releases/latest">
    <img src="https://img.shields.io/github/v/release/taezeem14/Cigarette-V1?style=for-the-badge&color=3882F6&label=Release" alt="Latest Release" />
  </a>
  <img src="https://img.shields.io/badge/Minecraft-26.3%20Fabric%20%7C%201.8.9%20Forge-8B5CF6?style=for-the-badge" alt="Minecraft Versions" />
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/taezeem14/Cigarette-V1?style=for-the-badge&color=10B981" alt="GPL-3.0 License" />
  </a>
</p>

<p>
  <a href="#key-highlights">Highlights</a> •
  <a href="#supported-versions--compatibility">Compatibility</a> •
  <a href="#installation--setup">Installation</a> •
  <a href="#in-game-controls">Controls</a> •
  <a href="#module-reference">Modules</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#building-from-source">Building</a>
</p>

</div>

---

## Overview

**Cigarette V1** is a dual-target, open-source Minecraft client built around a dark `#141416` **Vape V4** interface, deterministic combat/movement mathematics, and clean JSON profile management. Both the **Modern Fabric (`26.3`)** subproject and the **Legacy Forge (`1.8.9`)** root project ship all **41 modules** with zero proprietary DRM or telemetry.

### Key Highlights

- **Dual Native Runtimes** — Separate production builds tailored to **Minecraft `26.3` Fabric** (Java 25, unobfuscated engine) and **Minecraft `1.8.9` Forge** (Java 8, classic PvP).
- **Vape V4 Replica UI** — Draggable, z-ordered multi-panel ClickGUI (`Right Shift`), scissor-clipped setting drawers, top search & theme dock, GUI scale slider (`0.75x`–`1.50x`), interactive HUD snap editor, and 5 built-in presets (`Electric Blue`, `Ember Orange`, `Amethyst`, `Emerald`, `Rainbow`) plus custom RGB colors.
- **Ghost & Blatant Precision** — Mouse-sensitivity GCD quantization, Box-Muller Gaussian click-interval jitter (`AutoClicker`), closest-point hitbox reach calculation, movement-fix yaw correction (`SilentAim`, `KillAura`), and `1.9+ Cooldown` awareness.
- **Zero Lock-In** — Runs locally on the Official Launcher, Prism Launcher, MultiMC, Modrinth App, TLauncher, and SKLauncher with clean `SelfDestruct` state teardown.

---

## Supported Versions & Compatibility

| Edition | Minecraft Version | Mod Loader | Required Java | Multi-Version Server Support | Release Asset |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Modern Edition** | **`26.3`** (`~26.3`) | **Fabric Loader `0.20.1+`** & **Fabric API** | **Java 25+** | Join `1.8`–`26.x` servers via **ViaFabricPlus** | `Cigarette-V1-*-mc26.3-fabric.jar` |
| **Legacy PvP Edition** | **`1.8.9`** | **Forge `11.15.1.2318`** | **Java 8** | Join `1.7`–`26.x` servers via **ViaForge `4.4.0+4+1.8.9`** | `Cigarette-V1-*-mc1.8.9-forge.jar` |

### Official, Cracked & Offline Launchers

Cigarette V1 runs strictly client-side with **no** Mojang/Microsoft session verification, remote whitelist, or hardware lock.

- **Singleplayer, LAN & Offline Servers (`online-mode=false`)**: Works out of the box with **TLauncher**, **SKLauncher**, **Prism Launcher (Offline)**, **MultiMC**, and the **Official Launcher**.
- **Premium Servers (`online-mode=true`)**: Any server enforcing Mojang authentication (e.g., Hypixel) validates account session tokens server-side and rejects offline accounts regardless of installed mods.

> [!IMPORTANT]
> **Do not place the `1.8.9 Forge` JAR and the `26.3 Fabric` JAR in the same `mods/` directory.**
> Use separate instances (recommended) or swap out the unused JAR whenever switching between `1.8.9` and `26.3`.

---

## Installation & Setup

Download the build matching your Minecraft version from **[GitHub Releases](https://github.com/taezeem14/Cigarette-V1/releases/latest)**:
- **Latest Minecraft (`26.3`)**: Download **`Cigarette-V1-1.1.0-mc26.3-fabric.jar`** and **[Fabric API for `26.3`](https://modrinth.com/mod/fabric-api/versions?g=26.3)**.
- **Classic PvP (`1.8.9`)**: Download **`Cigarette-V1-1.1.0-mc1.8.9-forge.jar`**.

<details open>
<summary><b>Prism Launcher / MultiMC (Recommended — Clean Isolated Instances)</b></summary>

Prism Launcher keeps Java runtimes and `mods/` folders completely isolated per instance so you can keep **both** editions side by side without conflicts.

1. Click **Add Instance** and choose either:
   - **Minecraft `26.3`** with **Fabric (`0.20.1+`)**, or
   - **Minecraft `1.8.9`** with **Forge (`11.15.1.2318`)**.
2. Right-click the instance → **Edit → Mods**:
   - **For `26.3`**: Use **Download mods** to add **Fabric API**, then click **Add file** and select `Cigarette-V1-1.1.0-mc26.3-fabric.jar`.
   - **For `1.8.9`**: Click **Add file** and select `Cigarette-V1-1.1.0-mc1.8.9-forge.jar`.
3. Open **Settings → Java** inside the instance:
   - Set **Java 25+** for `26.3 Fabric`.
   - Set **Java 8** for `1.8.9 Forge`.
4. Click **Launch** (compatible with both Microsoft and Offline accounts).

</details>

<details>
<summary><b>TLauncher / SKLauncher (Cracked & Offline Launchers)</b></summary>

#### A. Latest Minecraft (`26.3 Fabric`)
1. Open the version selector at the bottom and pick **`Fabric 26.3`** (`Fabric Loader 0.20.1+`).
2. Open **Settings (Gear Icon) → Java Path** and ensure the launcher uses **Java 25** ([Eclipse Temurin 25](https://adoptium.net/)).
3. Open your `.minecraft/mods` directory:
   - **Windows**: `Win + R` → `%appdata%\.minecraft\mods`
   - **macOS**: `~/Library/Application Support/minecraft/mods`
   - **Linux**: `~/.minecraft/mods`
4. Copy **both** `Cigarette-V1-1.1.0-mc26.3-fabric.jar` and `fabric-api-*.jar` (`26.3`) into `mods/`.
5. Ensure no `1.8.9` mods remain in `mods/`, select `Fabric 26.3`, and click **Enter the Game**.

#### B. Classic PvP (`1.8.9 Forge`)
1. Select **`Forge 1.8.9`** (`11.15.1.2318`) in the version selector.
2. Open **Settings (Gear Icon) → Java Path** and point it to **Java 8** (`javaw.exe`).
   > Minecraft `1.8.9` Forge fails to start if launched under Java 17, 21, or 25.
3. Place `Cigarette-V1-1.1.0-mc1.8.9-forge.jar` into `.minecraft/mods/` (with no `26.3`/Fabric JARs present).
4. Click **Enter the Game**.

</details>

<details>
<summary><b>Official Minecraft Launcher</b></summary>

#### A. Minecraft `26.3` (Fabric)
1. Install **[Java 25 (Eclipse Temurin)](https://adoptium.net/)**.
2. Run the official **[Fabric Installer](https://fabricmc.net/use/installer/)**, select **Minecraft `26.3`** (`Loader 0.20.1+`), and click **Install**.
3. Drop `Cigarette-V1-1.1.0-mc26.3-fabric.jar` and **[Fabric API (`26.3`)](https://modrinth.com/mod/fabric-api/versions?g=26.3)** into `.minecraft/mods/`.
4. Launch the **`fabric-loader-26.3`** installation.

#### B. Minecraft `1.8.9` (Forge)
1. Install **Java 8 (Eclipse Temurin 8)**.
2. Run vanilla Minecraft **`1.8.9`** once, then close the game.
3. Run the **[Forge `1.8.9-11.15.1.2318` Installer](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html)** (**Install client**).
4. Drop `Cigarette-V1-1.1.0-mc1.8.9-forge.jar` into `.minecraft/mods/`.
5. In the Official Launcher, edit your `1.8.9-forge` profile → **More Options → Java Executable**, verify it points to Java 8 `javaw.exe`, and click **Play**.

</details>

<details>
<summary><b>ViaForge Setup (Join Modern 1.20 / 1.21 / 26.x Servers from 1.8.9 Forge)</b></summary>

1. Install **`1.8.9 Forge`** with `Cigarette-V1-1.1.0-mc1.8.9-forge.jar` in `mods/`.
2. Add **[ViaForge `4.4.0+4+1.8.9`](https://modrinth.com/mod/viaforge)** to the same `mods/` directory.
3. Choose your target server protocol version directly from the **Multiplayer** screen's **ViaForge** selector.

> [!TIP]
> On the native **`26.3 Fabric`** build, keep **KillAura → `1.9+ Cooldown`** enabled on modern combat servers for attack-gauge synchronization, or turn it off to use classic **APS** min/max sliders.

</details>

---

## In-Game Controls

| Control / UI Element | Action |
| :--- | :--- |
| **`Right Shift`** (default) | Open / close the ClickGUI |
| **Left-Click** module row | Toggle module ON / OFF |
| **Right-Click** module row | Expand / collapse module settings drawer |
| **Middle-Click** module row | Enter keybind capture (`ESC`, `Delete`, or `Backspace` resets bind to `None`) |
| **Top Dock → Search** | Real-time filter across all 41 modules |
| **Top Dock → Theme** | Cycle presets (`Electric Blue`, `Ember Orange`, `Amethyst`, `Emerald`, `Rainbow`) |
| **Top Dock → Scale** | Adjust GUI scale cleanly between `0.75x` and `1.50x` |
| **Top Dock → Profiles** | Create, save, load, and delete JSON configurations |
| **Top Dock → HUD Edit** | Drag-and-snap **Watermark**, **ArrayList**, and **TargetHUD** (`Right-Click` or `ESC` exits) |

**Profile Storage Locations**
- **1.8.9 Forge**: `.minecraft/cigarette/profiles/<name>.json`
- **26.3 Fabric**: `.minecraft/Cigarette/profiles/<name>.json`

---

## Module Reference

Cigarette V1 includes **41 modules** across 5 categories in both editions:

| Category | Count | Summary |
| :--- | :---: | :--- |
| **Combat** | 9 | GCD aim smoothing, Gaussian autoclicking, reach, hitboxes, velocity, W-Tap, silent aim, utilities |
| **Blatant** | 10 | KillAura (`AutoBlock` on `1.8.9` / `1.9+ Cooldown` on `26.3`), Scaffold, movement, packets, world safety |
| **Render** | 10 | Vape V4 ClickGUI, HUD overlays, 2D/3D ESP, Chams, Nametags, Tracers, BedESP, Fullbright, Trajectories |
| **Utility** | 9 | Inventory/chest automation, best-tool switching, bridge assist/eagle, antibot, team color matching |
| **Client** | 3 | Active module ArrayList HUD, friend list management, clean `SelfDestruct` teardown |

<details>
<summary><b>Expand Complete 41-Module List</b></summary>

- **Combat (9)**: `AimAssist`, `AutoClicker`, `Reach`, `HitBoxes`, `Velocity`, `WTap`, `SilentAim`, `ThrowPot`, `RodAimbot`
- **Blatant (10)**: `KillAura`, `Scaffold`, `Speed`, `Fly`, `Criticals`, `NoSlow`, `KeepSprint`, `Blink`, `Timer`, `AntiVoid`
- **Render (10)**: `ClickGUI`, `HUD`, `TargetHUD`, `ESP`, `Chams`, `Nametags`, `Tracers`, `BedESP`, `Fullbright`, `Trajectories`
- **Utility (9)**: `ChestStealer`, `InvManager`, `AutoTool`, `AutoArmor`, `FastPlace`, `Eagle`, `BridgeAssist`, `AntiBot`, `Teams`
- **Client (3)**: `ArrayList`, `FriendManager`, `SelfDestruct`

</details>

---

## Troubleshooting

| Symptom | Likely Cause | Resolution |
| :--- | :--- | :--- |
| Crash on startup with `UnsupportedClassVersionError` or Exit Code `1` | Java version mismatch | Use **Java 8** for `1.8.9 Forge`; use **Java 25+** for `26.3 Fabric`. |
| `ModResolutionException` / Mixin target failure | Incompatible JARs mixed inside `mods/` | Keep `1.8.9` JARs and `26.3` JARs in separate instances or directories. |
| Missing `fabric-api` dependency popup | `Fabric API` not installed for `26.3` | Place **[Fabric API for `26.3`](https://modrinth.com/mod/fabric-api/versions?g=26.3)** in `.minecraft/mods/`. |
| ClickGUI keybind unresponsive | Keybind changed or bound to another control | Inspect `ClickGUI` keybind in `.minecraft/Cigarette/profiles/default.json` or `cigarette/profiles/default.json`. |
| `"Invalid session"` / `"Failed to verify username"` | Joining an `online-mode=true` server from an offline launcher | Cracked launchers can only join singleplayer, LAN, or `online-mode=false` servers. |

---

## Architecture

The codebase cleanly separates version-specific platform adapters while preserving identical user-facing behavior and module structure:

```text
Cigarette-V1/
├── .github/workflows/                 # Dual CI matrix (Java 8 1.8.9 Forge + Java 25 26.3 Fabric) & Release automation
├── build.gradle                       # Root build: Minecraft 1.8.9 Forge (ForgeGradle 2.1, MCP stable_22, JDK 8)
├── src/main/java/dev/taezeem14/cigarette/
│   ├── Cigarette.java                 # Forge @Mod bootstrap, lifecycle hooks & BlurUtil teardown
│   ├── agent/                         # Optional JVM Instrumentation agent fallback (AgentMain)
│   ├── mixin/impl/                    # SpongePowered Mixin 0.7.11 hooks (MCP 1.8.9 SRG) + Access Transformer
│   ├── module/impl/                   # 41 modules (incl. 1.8.9 Sword AutoBlock)
│   └── ui/                            # LWJGL2 / OpenGL 1.1 ClickGUI, HUDEditor & GLSL ShaderGroup blur
│
└── modern/                            # Standalone Gradle subproject: Minecraft 26.3 Fabric (Loom 1.19.4, JDK 25)
    ├── build.gradle                   # Unobfuscated 26.3 Loom config + SplitEnvironmentSourceSets
    └── src/
        ├── main/resources/fabric.mod.json
        └── client/
            ├── resources/cigarette.client.mixins.json
            └── java/dev/taezeem14/cigarette/
                ├── CigaretteModernClient.java   # ClientModInitializer + Fabric API callbacks
                ├── mixin/                       # 11 bytecode-verified Mixins targeting 26.3 Gui/Input/Entity/Net
                ├── module/impl/                 # 41 modules ported to 26.3 (1.9+ Cooldown, DataComponents)
                ├── ui/                          # GuiGraphicsExtractor UI, native stratum blur & SDL input
                └── util/                        # Gizmos 3D renderer, CameraRenderState frustum projection, GCD math
```

### Engineering Design

- **Priority Event Pipeline**: Annotation-driven `@EventTarget` dispatch ordered from `HIGHEST` to `LOWEST` across game ticks, pre/post motion phases, movement vectors, attack hooks, incoming/outgoing Netty packets, and 2D/3D render passes.
- **Rotation & Input Quantization**: Yaw/pitch deltas are wrapped to `[-180, 180]` and snapped to the exact mouse-sensitivity GCD step (`0.15f` cubic factor `× 8`), paired with camera-relative movement vector correction (`MixinKeyboardInput` / `MixinEntityPlayerSP`).
- **26.3 Unobfuscated Adaptation**: Replaced legacy `LWJGL2` / `OpenGL 1.1` / `Minecraft.screen` patterns with Mojang's `26.3` API (`Gui.screen()`, `GuiGraphicsExtractor`, `blurBeforeThisStratum()`, SDL `InputConstants.Type.KEYBOARD`, `ItemTags.SWORDS`, `DataComponents`, and `LevelRenderEvents.BEFORE_GIZMOS`).

---

## Building from Source

### Prerequisites

- **Minecraft `1.8.9` (Forge)**: **JDK 8** (e.g., Eclipse Temurin 8)
- **Minecraft `26.3` (Fabric)**: **JDK 25** (e.g., Eclipse Temurin 25)

### Build Minecraft `1.8.9` Forge (Repository Root)

```bash
# Linux / macOS
./gradlew setupCiWorkspace clean build --no-daemon

# Windows (PowerShell / CMD)
.\gradlew.bat setupCiWorkspace clean build --no-daemon
```

> Output binary: `build/libs/Cigarette-V1-1.1.0.jar`

### Build Minecraft `26.3` Fabric (`modern/`)

```bash
cd modern

# Linux / macOS
./gradlew clean build --no-daemon

# Windows (PowerShell / CMD)
.\gradlew.bat clean build --no-daemon
```

> Output binary: `modern/build/libs/Cigarette-V1-1.1.0-mc26.3-fabric.jar`

---

## Disclaimer & License

> [!NOTE]
> Cigarette V1 is intended strictly for educational software-engineering study, JVM instrumentation research, singleplayer worlds, and private testing environments where client modifications are explicitly permitted.

Distributed under the **[GNU General Public License v3.0](LICENSE)** — Copyright © 2026 **[taezeem14](https://github.com/taezeem14)** (Muhammad Taezeem Tariq).
