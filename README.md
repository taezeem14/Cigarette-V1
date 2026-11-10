# Cigarette V1

<p align="center">
  <b>Open-Source Hybrid Ghost &amp; Utility Client Modeled After Vape V4</b><br>
  Available natively for <b>Minecraft 1.8.9 (Forge)</b> and <b>Minecraft 26.3 (Fabric)</b>.
</p>

---

## Supported Minecraft Versions & Launchers

Cigarette V1 ships two native builds so you can play both **classic 1.8.9 PvP** and the **latest Minecraft releases** with the same 41-module suite, Vape V4 ClickGUI, and profile system.

| Edition | Minecraft Version | Mod Loader | Required Java | Server Compatibility | Asset Name |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Modern Edition** | **Minecraft `26.3`** (`~26.3`) | **Fabric Loader `0.20.1+`** & **Fabric API** | **Java 25+** | `26.3` natively, or `1.8`–`26.x` servers with **ViaFabricPlus** | `Cigarette-V1-*-mc26.3-fabric.jar` |
| **Legacy PvP Edition** | **Minecraft `1.8.9`** | **Minecraft Forge `11.15.1.2318`** | **Java 8** (`8u...`) | `1.8.9` natively, or `1.7`–`26.x` servers with **ViaForge `4.4.0+4+1.8.9`** | `Cigarette-V1-*-mc1.8.9-forge.jar` |

### Cracked / Offline Launchers (TLauncher, SKLauncher, Prism Offline)

**Yes — Cigarette V1 works on TLauncher, SKLauncher, Prism Launcher (Offline), and the Official Minecraft Launcher.**

- Cigarette V1 runs 100% client-side and performs **no** Mojang/Microsoft session verification, license checks, or telemetry phone-home.
- **Singleplayer, LAN, and Cracked/Offline Servers (`online-mode=false`)**: Works immediately with any offline username.
- **Premium Servers (`online-mode=true`, e.g., Hypixel)**: All premium servers validate Microsoft account tokens on the server side — an offline/cracked launcher account cannot join `online-mode=true` servers regardless of which client mod you install.

> [!IMPORTANT]
> **Never keep the `1.8.9 Forge` JAR and `26.3 Fabric` JAR in the same `mods/` folder at the same time.**
> Keep separate launcher profiles/game directories or move the unused JAR out before switching versions.

---

## Installation & Setup Tutorials

Download the matching release `.jar` from the **[GitHub Releases](../../releases)** page first:
- For **Latest Minecraft (`26.3`)**: download **`Cigarette-V1-*-mc26.3-fabric.jar`** + **[Fabric API for 26.3](https://modrinth.com/mod/fabric-api/versions?g=26.3)**.
- For **Classic PvP (`1.8.9`)**: download **`Cigarette-V1-*-mc1.8.9-forge.jar`**.

---

### Option 1: TLauncher / SKLauncher (Cracked & Offline Launchers)

#### Setup for Latest Minecraft (`26.3 Fabric`)
1. Open **TLauncher** (or **SKLauncher**) and click the version dropdown at the bottom.
2. Select **`Fabric 26.3`** (if not listed, open TLMods / Modpack Settings or click **Install Fabric** for `26.3` with Loader **`0.20.1+`**).
3. Make sure your launcher is configured to use **Java 25** (Eclipse Temurin 25 JDK/JRE):
   - In TLauncher: open **Gear Icon (Settings) → Java Path**, point it to Java 25 (`javaw.exe`), and click **Save**.
4. Open your Minecraft folder:
   - **Windows**: Press `Win + R`, type `%appdata%\.minecraft`, and press `Enter`.
   - **macOS**: `~/Library/Application Support/minecraft`
   - **Linux**: `~/.minecraft`
5. Open (or create) the **`mods`** folder inside `.minecraft`.
6. Place **both** files inside `.minecraft\mods\`:
   - `Cigarette-V1-1.0.0-mc26.3-fabric.jar`
   - `fabric-api-*.jar` (matching `26.3`)
7. Remove any old 1.8.9 mods from `mods/`, select `Fabric 26.3`, and click **Enter the Game**.

#### Setup for Classic PvP (`1.8.9 Forge`)
1. Open **TLauncher** (or **SKLauncher**) and select **`Forge 1.8.9`** (specifically **Forge `11.15.1.2318`** if selectable) in the version dropdown.
2. **Switch Java to Java 8**: Minecraft 1.8.9 Forge will **crash** on Java 17/21/25.
   - Install **Eclipse Temurin Java 8** if you don't already have it.
   - In TLauncher: open **Gear Icon (Settings) → Java Path** and select your Java 8 `javaw.exe` (for example `C:\Program Files\Eclipse Adoptium\jdk-8...\bin\javaw.exe`).
3. Open `%appdata%\.minecraft\mods\` (make sure no 26.3/Fabric jars are inside).
4. Drop **`Cigarette-V1-1.0.0-mc1.8.9-forge.jar`** into `.minecraft\mods\`.
5. Click **Enter the Game**.

---

### Option 2: Official Minecraft Launcher

#### For Minecraft `26.3` (Fabric)
1. Install **[Java 25 (Eclipse Temurin)](https://adoptium.net/)**.
2. Download and run the official **[Fabric Installer](https://fabricmc.net/use/installer/)**:
   - **Minecraft Version**: `26.3`
   - **Loader Version**: `0.20.1` or newer
   - Click **Install**.
3. Open `%appdata%\.minecraft\mods\` (Linux: `~/.minecraft/mods/`, macOS: `~/Library/Application Support/minecraft/mods/`).
4. Copy **`Cigarette-V1-1.0.0-mc26.3-fabric.jar`** and **[Fabric API for 26.3](https://modrinth.com/mod/fabric-api/versions?g=26.3)** into `mods/`.
5. Open the **Minecraft Launcher**, choose the **`fabric-loader-26.3`** installation, and click **Play**.

#### For Minecraft `1.8.9` (Forge)
1. Install **Java 8 (Eclipse Temurin 8)**.
2. Run Minecraft **`1.8.9`** once vanilla, then close it.
3. Download and run the **[Minecraft Forge `1.8.9-11.15.1.2318` Installer](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html)** (**Install client**).
4. Copy **`Cigarette-V1-1.0.0-mc1.8.9-forge.jar`** into `.minecraft\mods\`.
5. In the Official Launcher, edit your **`1.8.9-forge`** installation → **More Options → Java Executable** and verify it points to Java 8 `javaw.exe`. Click **Play**.

---

### Option 3: Prism Launcher / MultiMC (Recommended)

Prism Launcher automatically isolates `mods/` and Java versions per instance so you can keep **both** `1.8.9 Forge` and `26.3 Fabric` side by side without ever swapping files manually:

1. Click **Add Instance** → pick **`26.3` + Fabric (`0.20.1+`)** (or **`1.8.9` + Forge `11.15.1.2318`**).
2. Right-click the instance → **Edit → Mods**:
   - For `26.3`: click **Download mods** to add **Fabric API**, then click **Add file** and choose `Cigarette-V1-1.0.0-mc26.3-fabric.jar`.
   - For `1.8.9`: click **Add file** and choose `Cigarette-V1-1.0.0-mc1.8.9-forge.jar`.
3. Under **Settings → Java**, select **Java 25** (for `26.3`) or **Java 8** (for `1.8.9`).
4. Launch directly (supports both Microsoft accounts and Offline accounts).

---

### Joining Newer Servers From 1.8.9 (`ViaForge`)

Want classic 1.8.9 Forge smoothness while playing on `1.20` / `1.21` / `26.x` servers?
1. Add **[ViaForge `4.4.0+4+1.8.9`](https://modrinth.com/mod/viaforge)** into your `1.8.9 Forge` `mods/` folder alongside `Cigarette-V1-1.0.0-mc1.8.9-forge.jar`.
2. In the **Multiplayer** menu, pick the target server protocol version via the **ViaForge** button.
3. If fighting on `1.9+` cooldown servers with the **26.3 Fabric** build, keep **KillAura → `1.9+ Cooldown`** enabled for natural attack-gauge timing (or disable it to use `APS` sliders on 1.8 / spam servers).

---

## In-Game Controls & Usage

| Input / Action | Behavior |
| :--- | :--- |
| **`Right Shift`** (default) | Open / close the **ClickGUI** |
| **Left-Click** module row | Toggle module ON / OFF |
| **Right-Click** module row | Expand / collapse the scissor-clipped settings drawer |
| **Middle-Click** module row | Enter keybind listening mode (`ESC` / `Delete` / `Backspace` unbinds to `None`) |
| **Top Dock → Search** | Live filter across all 41 modules by name or category |
| **Top Dock → Theme** | Cycle built-in presets (`Electric Blue`, `Amber Gold`, `Emerald`, `Crimson`, `Amethyst`) |
| **Top Dock → Scale** | Drag slider between `0.75x` and `1.50x` |
| **Top Dock → Profiles** | Save, load, create, and delete named JSON configs |
| **Top Dock → HUD Edit** | Drag-and-snap **Watermark**, **ArrayList**, and **TargetHUD** (`Right-Click` or `ESC` returns) |

Profile JSON files are stored inside your game directory under:
- **1.8.9 Forge**: `.minecraft/cigarette/profiles/<name>.json`
- **26.3 Fabric**: `.minecraft/Cigarette/profiles/<name>.json`

---

## Complete 41-Module Suite

Cigarette V1 provides all **41 modules** across both `1.8.9 Forge` and `26.3 Fabric`:

| Category | Modules |
| :--- | :--- |
| **Combat** (`9`) | `AimAssist`, `AutoClicker`, `Reach`, `HitBoxes`, `Velocity`, `WTap`, `SilentAim`, `ThrowPot`, `RodAimbot` |
| **Blatant** (`10`) | `KillAura`, `Scaffold`, `Speed`, `Fly`, `Criticals`, `NoSlow`, `KeepSprint`, `Blink`, `Timer`, `AntiVoid` |
| **Render** (`10`) | `ClickGUI`, `HUD`, `TargetHUD`, `ESP`, `Chams`, `Nametags`, `Tracers`, `BedESP`, `Fullbright`, `Trajectories` |
| **Utility** (`9`) | `ChestStealer`, `InvManager`, `AutoTool`, `AutoArmor`, `FastPlace`, `Eagle`, `BridgeAssist`, `AntiBot`, `Teams` |
| **Client** (`3`) | `ArrayList`, `FriendManager`, `SelfDestruct` |

---

## Troubleshooting

| Symptom | Cause | Fix |
| :--- | :--- | :--- |
| Crash on launch with `UnsupportedClassVersionError` or Java exit code `1` | Wrong Java version for selected edition | Use **Java 8** for `1.8.9 Forge`; use **Java 25+** for `26.3 Fabric`. |
| Crash complaining about `ModResolutionException` / `Mixin` / Forge vs Fabric classes | Both JARs or mismatched-version mods are inside `.minecraft/mods/` | Keep only `1.8.9 Forge` mods when launching `1.8.9`, and only `26.3 Fabric` mods + `Fabric API` when launching `26.3`. |
| `Could not find required mod: fabric-api` | `Fabric API` missing in `26.3` instance | Download `Fabric API` for Minecraft `26.3` and drop it into `mods/`. |
| ClickGUI doesn't open on `Right Shift` | `ClickGUI` bound key changed or another mod conflicts | Rebind via `.minecraft/Cigarette/profiles/default.json` or check Controls for conflicting keybinds. |
| `"Failed to verify username"` / `"Invalid session"` on TLauncher | Joining a premium `online-mode=true` server with an offline account | Cracked launchers can only join singleplayer, LAN, or `online-mode=false` offline servers. |

---

## Architecture Overview

This repository houses both editions cleanly:

```text
Cigarette V1/
├── build.gradle                     # Root build: Minecraft 1.8.9 Forge (ForgeGradle 2.1, Java 8)
├── src/main/java/dev/taezeem14/cigarette/
│   ├── Cigarette.java               # Forge @Mod entry point & SelfDestruct cleanup
│   ├── agent/                       # Optional Java Instrumentation fallback for external loading
│   ├── mixin/impl/                  # SpongePowered 0.7.11 Mixins (MCP SRG mappings)
│   ├── module/impl/                 # 41 modules (1.8.9 Forge API + Sword AutoBlock)
│   └── ui/                          # Vape V4 ClickGUI + HUD Editor (GL11 / ShaderGroup blur)
│
└── modern/                          # Subproject: Minecraft 26.3 Fabric (Loom 1.19.4, Java 25)
    ├── build.gradle                 # Unobfuscated 26.3 Fabric Loom setup + SplitEnvironmentSourceSets
    └── src/
        ├── main/resources/fabric.mod.json
        └── client/
            ├── resources/cigarette.client.mixins.json
            └── java/dev/taezeem14/cigarette/
                ├── CigaretteModernClient.java   # ClientModInitializer + Fabric API hooks
                ├── mixin/                       # 11 Fabric Mixins targeting 26.3 Gui/Input/Entity/Net
                ├── module/impl/                 # 41 modules ported to 26.3 (1.9+ Cooldown, DataComponents)
                ├── ui/                          # Vape V4 ClickGUI + HUD Editor (GuiGraphicsExtractor, SDL keys, native blur)
                └── util/                        # 3D Gizmos, FrustumUtil (CameraRenderState), GCD rotations
```

### Technical Highlights

- **Event Bus Architecture**: Priority-ordered (`HIGHEST` → `LOWEST`) reflection/direct dispatch via `EventTarget`, supporting both cancellable game packets and pre/post motion phases.
- **Native Version Adaptation**:
  - `1.8.9`: SpongePowered Mixin `0.7.11` with MCP SRG `1.8.9` hooks (`PlayerControllerMP`, `NetworkManager`, `EntityPlayerSP`, `RenderManager`) plus optional JVM `Instrumentation` agent (`AgentMain`), GLSL `ShaderGroup` blur, and classic sword `AutoBlock`.
  - `26.3`: Fabric Loom `1.19.4` on Java 25 using Mojang's unobfuscated `26.3` classes (`Gui.screen()`, `GuiGraphicsExtractor`, `blurBeforeThisStratum()`, SDL `InputConstants.Type.KEYBOARD`, `DataComponents` gear inspection, `Gizmos` 3D overlays, and `LevelRenderEvents` / `HudElementRegistry`).
- **Bypass Math**: Mouse-sensitivity GCD quantization (`0.15f`-scaled cubic `f^3 * 8`), Box-Muller Gaussian click-interval jitter (`AutoClicker`), closest-point hitbox reach calculation, and movement yaw stranding correction (`SilentAim`, `KillAura`, `Scaffold`).

---

## Building from Source

### Prerequisites
- **For `1.8.9 Forge`**: **JDK 8** (e.g., Eclipse Temurin 8)
- **For `26.3 Fabric`**: **JDK 25** (e.g., Eclipse Temurin 25)

### Build Minecraft `1.8.9 Forge` (Repo Root)

```bash
# Linux / macOS
./gradlew setupCiWorkspace clean build --no-daemon

# Windows (PowerShell / CMD)
.\gradlew.bat setupCiWorkspace clean build --no-daemon
```
Output JAR: `build/libs/Cigarette-V1-1.0.0.jar`

### Build Minecraft `26.3 Fabric` (`modern/`)

```bash
cd modern

# Linux / macOS
./gradlew clean build --no-daemon

# Windows (PowerShell / CMD)
.\gradlew.bat clean build --no-daemon
```
Output JAR: `modern/build/libs/Cigarette-V1-1.0.0-mc26.3-fabric.jar`

---

## Legal Disclaimer & License

Cigarette V1 is an open-source research and software-engineering study demonstrating JVM bytecode instrumentation, Fabric/Forge mixin architectures, custom 2D/3D client rendering pipelines, and smooth rotational mathematics. Only use this client in singleplayer worlds, private testing servers, or servers where client modifications are explicitly permitted.

Licensed under the **[GNU General Public License v3.0](LICENSE)** — crafted by **[taezeem14](https://github.com/taezeem14)** (Muhammad Taezeem Tariq).
