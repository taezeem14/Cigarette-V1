Copy-paste this entire specification block straight into **Antigravity 2.0**—it is engineered as a full multi-agent system blueprint with exact MCP mappings, mathematical formulas, OpenGL rendering pipelines, and GitHub MCP tool calls so the agents can't slack off or give you lazy placeholder code 🔥😈💀:

---

```markdown
# MASTER ARCHITECTURAL SPECIFICATION: PROJECT "CIGARETTE V1"
## TARGET RUNTIME: GOOGLE ANTIGRAVITY 2.0 MULTI-AGENT ORCHESTRATOR
## TARGET PLATFORM: MINECRAFT 1.8.9 (FORGEGRADLE 2.1 / SPONGEPOWERED MIXIN 0.7.11 / LWJGL 2.9.4)

### 0. GLOBAL DIRECTIVES & ANTI-LAZINESS CONSTRAINTS
You are operating as an autonomous, multi-agent software engineering swarm inside Google Antigravity 2.0. Your objective is to architect, implement, compile, test, and publish **Cigarette V1**—a full-featured, production-grade open-source 1.8.9 hybrid ghost and utility client modeled after Vape V4.

**STRICT NON-NEGOTIABLE RULES:**
1. **NO STUBS OR TODOs:** You are strictly prohibited from writing `// TODO`, `// Implement later`, empty method bodies, or mock return values. Every single class, Mixin, math utility, OpenGL call, and module setting must be 100% implemented with working logic.
2. **NO HALLUCINATED APIS:** Use strict Minecraft 1.8.9 MCP (`stable_22-1.8.9`) method/field names, Searge (`func_xxxx_x` / `field_xxxx_x`) fallbacks where reflection is used, and authentic `net.minecraft.network.play.client.*` / `net.minecraft.network.play.server.*` packet classes.
3. **SELF-HEALING COMPILATION:** You must not terminate the task until `./gradlew clean build` exits with code `0` and produces a shaded, obfuscated, production-ready `.jar` artifact in `build/libs/`.
4. **PARALLEL SUB-AGENT TOPOLOGY:** Decompose execution across four parallel background worker threads:
   - **Agent-Alpha (DevOps & GitHub MCP):** Identity lookup, repository creation, Git workflows, Semantic Versioning, and GitHub Actions CI/CD release pipelines.
   - **Agent-Beta (Core Engine, Mixins & Mappings):** Gradle build scripts, Access Transformers, Event Bus, Mixin injections, JVM Instrumentation API, and Profile serialization.
   - **Agent-Gamma (Combat, Movement & Player Modules):** Vector/rotation mathematics, Gaussian clickers, packet interceptors, and game-loop hooks.
   - **Agent-Delta (OpenGL Rendering & Vape V4 ClickGUI):** Custom TrueType font renderer, GL11 scissor engine, 3D-to-2D frustum projection, and the multi-panel Vape V4 UI replica.

---

## PHASE 1: GITHUB MCP IDENTITY RESOLUTION & REPOSITORY PROVISIONING
Execute these steps immediately using the connected **GitHub MCP Server**:

1. **Creator Identity Fetch:**
   - Invoke the GitHub MCP tool `get_me` to retrieve the authenticated user's profile data (`login`, `name`, `html_url`, `email`, `avatar_url`).
   - Store these values in memory and inject them into:
     - `gradle.properties` (`mod_author=<login>`)
     - `src/main/resources/mcmod.info` (`authorList`, `url`, `credits`)
     - `Cigarette.java` (`public static final String AUTHOR = "<login>";`)
     - In-game HUD Watermark, ClickGUI footer, and `README.md` badges.

2. **Repository Creation & Initialization:**
   - Invoke the GitHub MCP tool `create_repository` with:
     - `name`: `"Cigarette-V1"`
     - `description`: `"Cigarette V1 — High-Performance Open-Source Minecraft 1.8.9 Hybrid & Ghost Client"`
     - `private`: `false`
     - `auto_init`: `false`
   - Initialize local Git (`git init`), configure `user.name` and `user.email` using the `get_me` payload, set default branch to `main`, and add the remote origin.

3. **Automated Semantic Versioning & Release Strategy:**
   - Create a `version.json` and link it to `gradle.properties` starting at `1.0.0`.
   - Every major milestone completed during build phases must trigger an automated version bump (`1.0.0-alpha.1` -> `1.0.0-beta.1` -> `1.0.0`), a Git tag, and a GitHub Release via the GitHub MCP `create_release` tool (or GitHub Actions workflow) with the compiled `.jar` binary attached and an auto-generated changelog.

---

## PHASE 2: OPEN-SOURCE RECONNAISSANCE & REFERENCE HARVESTING
Before writing core client code, use your terminal and web search tools to inspect battle-tested open-source 1.8.9 clients.

1. Create a local `.references/` directory and immediately add `.references/` to `.gitignore`.
2. Search GitHub and `git clone --depth 1` the following reference repositories into `.references/`:
   - **Raven B++ / Raven B+ forks** (e.g., `Kopamed/Raven-bPLUS`, `OlziYT/RavenBS-Plus-Plus`, or `Blowsy/Raven-b4`) — study their ghost combat algorithms, double-slider implementations, and legit scaffold edge detection.
   - **Hydrogen Client** (`zPeanut/Hydrogen`) — study its clean Forge 1.8.9 + SpongePowered Mixin 0.7.11 build setup and event bus.
   - **LiquidBounce 1.8.9 Legacy** (`CCBlueX/LiquidBounce` 1.8.9 branch) — study its rotation math, RayCast utilities (`RaycastUtils`), and `FontRenderer` optimizations.
3. Extract architectural best practices from `.references/` and write a completely clean, modular codebase from scratch under `src/main/java/dev/<github_login>/cigarette/`. Never copy raw brand names or broken legacy dependencies.

---

## PHASE 3: PROJECT STRUCTURE, BUILD SYSTEM & DUAL-MODE BOOTSTRAPPER

### 3.1 Package Hierarchy
Construct the following exact directory tree:
```text
Cigarette-V1/
├── .github/workflows/
│   ├── build.yml
│   └── release.yml
├── gradle/wrapper/
├── src/main/java/dev/<github_login>/cigarette/
│   ├── Cigarette.java                      # Main singleton & lifecycle manager
│   ├── agent/
│   │   ├── AgentMain.java                  # premain & agentmain JVM instrumentation entry
│   │   └── ClassTransformer.java           # ASM bytecode hooks for vanilla/Lunar/Forge attach
│   ├── event/
│   │   ├── Event.java
│   │   ├── EventBus.java                   # High-speed MethodHandle / ASM listener dispatcher
│   │   ├── EventTarget.java                # @Retention(RUNTIME) annotation
│   │   └── impl/                           # TickEvent, MotionEvent, PacketEvent, Render2DEvent, Render3DEvent, AttackEvent, MoveEvent, KeyEvent
│   ├── mixin/
│   │   ├── MixinLoader.java                # IFMLLoadingPlugin implementation
│   │   └── impl/
│   │       ├── MixinMinecraft.java         # runTick, clickMouse, rightClickMouse hooks
│   │       ├── MixinEntityRenderer.java    # getMouseOver (Reach/HitBox) & renderWorldPass hooks
│   │       ├── MixinEntityPlayerSP.java    # onUpdateWalkingPlayer (Pre/Post Motion & Silent Rotations)
│   │       ├── MixinNetworkManager.java    # channelRead0 & sendPacket (Packet interception)
│   │       ├── MixinGuiIngame.java         # renderTooltip (Render2DEvent trigger)
│   │       └── MixinRendererLivingEntity.java # Chams & Nametag overrides
│   ├── module/
│   │   ├── Category.java                   # COMBAT, BLATANT, RENDER, UTILITY, WORLD, PROFILES
│   │   ├── Module.java                     # Base class (name, keybind, state, settings, animations)
│   │   ├── ModuleManager.java              # Registry & fast lookup
│   │   └── impl/
│   │       ├── combat/                     # AimAssist, AutoClicker, Reach, Velocity, WTap, SilentAim, HitBoxes, ThrowPot, RodAimbot
│   │       ├── blatant/                    # KillAura, Scaffold, Speed, Fly, Criticals, NoSlow, KeepSprint, Blink, Timer, AntiVoid
│   │       ├── render/                     # ClickGUIModule, HUD, ESP, Chams, Nametags, Tracers, BedESP,TargetHUD, Fullbright, Trajectories
│   │       ├── utility/                    # ChestStealer, InvManager, AutoTool,AutoArmor, FastPlace, Eagle, BridgeAssist, AntiBot, Teams
│   │       └── client/                     # SelfDestruct, FriendManager, ArrayListModule
│   ├── setting/
│   │   ├── Setting.java
│   │   └── impl/                           # BooleanSetting, NumberSetting, DoubleSliderSetting, ModeSetting, ColorSetting, KeybindSetting
│   ├── ui/
│   │   ├── font/                           # Custom Anti-Aliased AWT/OpenGL Texture FontRenderer
│   │   ├── clickgui/                       # Vape V4 Multi-Panel GUI, CategoryFrame, ModuleButton, SettingComponents
│   │   └── hud/                            # Draggable HUD Editor, TargetHUD, ArrayList, Watermark, Notifications
│   ├── config/
│   │   ├── ConfigManager.java              # Gson profile loader/saver + Bindings
│   │   └── Profile.java
│   └── util/
│       ├── math/                           # RandomUtil (Gaussian/Perlin), RotationUtil, Vec3Util, TimerUtil
│       ├── player/                         # MoveUtil, InventoryUtil, RaycastUtil, BlockUtil
│       ├── render/                         # RenderUtil (GL11 rounded rects, shaders, scissor), ColorUtil, FrustumUtil
│       └── mapping/                        # MappingResolver (MCP <-> Searge reflection cache)
└── src/main/resources/
    ├── mcmod.info
    ├── mixins.cigarette.json
    └── META-INF/cigarette_at.cfg

```

### 3.2 `build.gradle` Configuration

Configure `build.gradle` with:

* `ForgeGradle 2.1-SNAPSHOT` targeting Minecraft `1.8.9` (`11.15.1.2318-1.8.9`) and `mappings = "stable_22"`.
* `org.spongepowered:mixin:0.7.11-SNAPSHOT` shaded into the final jar via `com.github.johnrengelman.shadow` (`ShadowJar`).
* `MANIFEST.MF` attributes including:
* `FMLCorePlugin`: `dev.<github_login>.cigarette.mixin.MixinLoader`
* `FMLCorePluginContainsFMLMod`: `true`
* `ForceLoadAsMod`: `true`
* `MixinConfigs`: `mixins.cigarette.json`
* `Premain-Class`: `dev.<github_login>.cigarette.agent.AgentMain`
* `Agent-Class`: `dev.<github_login>.cigarette.agent.AgentMain`
* `Can-Redefine-Classes`: `true`
* `Can-Retransform-Classes`: `true`



---

## PHASE 4: CORE ENGINE, MAPPINGS & MIXIN HOOKS

1. **MappingResolver (`MappingResolver.java`):**
* Implement a thread-safe cached reflection helper that resolves both development MCP names (e.g., `rightClickDelayTimer`, `leftClickCounter`, `pressed`, `timerSpeed`) and production obfuscated Searge names (`field_71467_ac`, `field_71429_W`, `field_74513_e`, `field_74278_d`) automatically so the client never crashes with `NoSuchFieldException` in production vs. dev environments.


2. **MixinEntityRenderer (`getMouseOver` Substitution):**
* Inject into or `@Overwrite` `EntityRenderer.getMouseOver(float partialTicks)` to dynamically poll `Reach.getReachDistance()` and `HitBoxes.getExpansionSize()`.
* When `Reach` is disabled or conditions fail (e.g., player in creative, or "Sprint Only" unchecked while walking), behave 100% identically to vanilla 1.8.9 (`3.0D` reach, `0.1F` collision border size).


3. **MixinEntityPlayerSP (Server-Side Rotation Spoofing):**
* In `onUpdateWalkingPlayer`, fire `MotionEvent(EventState.PRE, posX, posY, posZ, rotationYaw, rotationPitch, onGround)`.
* Temporarily overwrite `this.rotationYaw` and `this.rotationPitch` with the event's spoofed rotations before packets (`C03PacketPlayer`) are dispatched, and restore the client's visual camera rotations in `EventState.POST`. Also update `renderYawOffset` and `rotationYawHead` so third-person model animations display the smooth server-side rotations cleanly.



---

## PHASE 5: EXHAUSTIVE MODULE SPECIFICATIONS & ALGORITHMS

Every module below must be fully coded with exact settings and mathematical formulas:

### 5.1 Combat Category (Ghost / Closet Tuning)

1. **AimAssist:**
* *Settings:* `Horizontal Speed (1.0 - 10.0)`, `Vertical Speed (1.0 - 10.0)`, `FOV (15° - 360°)`, `Distance (3.0 - 6.0)`, `Click Aim (Boolean)`, `Weapon Only (Boolean)`, `Aim Vertically (Boolean)`, `Multipoint Hitbox (Boolean)`, `Ignore Teams/Friends (Boolean)`, `Break Blocks Check (Boolean)`.
* *Algorithm:* Compute the closest point on the target entity's `AxisAlignedBB` (clamped between 15% and 85% of height/width to avoid snapping to geometric centers). Calculate delta yaw/pitch, wrap angle to `[-180, 180]`, apply non-linear sigmoid smoothing + subtle Perlin/Gaussian micro-noise (`RandomUtil.nextGaussian(0, 0.15)`) to mimic human wrist acceleration, and apply mouse sensitivity GCD (Greatest Common Divisor) quantization:

$$\Delta \text{angle}_{\text{fixed}} = \text{round}\left(\frac{\Delta \text{angle}}{0.15 \times (0.6 \times \text{sens} + 0.2)^3 \times 8}\right) \times \text{GCD}$$




2. **AutoClicker (Left & Right):**
* *Settings:* `Left CPS (DoubleSlider: e.g., 11.5 - 14.5)`, `Right CPS (DoubleSlider: e.g., 14.0 - 18.0)`, `BlockHit (Boolean + Chance %)`, `Jitter Strength (0.0 - 2.0)`, `Weapon Only (Boolean)`, `Break Blocks (Boolean)`, `Inventory Fill (Boolean - Shift+Click inside GuiInventory)`.
* *Algorithm:* Never use flat `1000 / cps` delays. Generate inter-click delays using a Box-Muller Gaussian transform centered on the current `DoubleSlider` bounds with occasional human fatigue spikes and double-click simulation bursts. Trigger clicks via `KeyBinding.setKeyBindState` and `KeyBinding.onTick` across distinct client frames (separate press and release timestamps).


3. **Reach:**
* *Settings:* `Range (DoubleSlider: 3.00 - 3.35, max 6.00)`, `Chance (0% - 100%)`, `Sprint Only (Boolean)`, `Water Check (Boolean)`, `Hit Through Walls (Boolean)`.
* *Algorithm:* Pick a randomized reach threshold within `[min, max]` per tick when `Math.random() * 100 <= chance`.


4. **HitBoxes:**
* *Settings:* `Expand (0.00 - 1.00)`. Adds directly to `entity.getCollisionBorderSize()` during combat raytracing.


5. **Velocity (Anti-Knockback):**
* *Settings:* `Mode (Simple / Jump Reset / Delay / Intave)`, `Horizontal (0% - 100%)`, `Vertical (0% - 100%)`, `Chance (0% - 100%)`, `Only While Targeting (Boolean)`, `Water Check (Boolean)`.
* *Algorithm:*
* *Simple Mode:* Intercept `S12PacketEntityVelocity` where `entityID == mc.thePlayer.getEntityId()`. Multiply `motionX` and `motionZ` by `horizontal / 100.0`, `motionY` by `vertical / 100.0`. If both are `0%`, cancel packet.
* *Jump Reset Mode:* Completely legit anticheat bypass—do not modify `S12PacketEntityVelocity`. Instead, when `mc.thePlayer.hurtTime == 9` and `mc.thePlayer.onGround` and `mc.thePlayer.isSprinting()`, simulate a jump input (`mc.gameSettings.keyBindJump`) for 1 tick to naturally cut horizontal knockback momentum.




6. **WTap / SprintReset:**
* *Settings:* `Mode (Packet / Legit Key)`, `Delay (ms DoubleSlider: 30 - 65)`, `Chance (0% - 100%)`.
* *Algorithm:* Upon `AttackEvent`, wait `delay` ms, then either send `C0BPacketEntityAction(STOP_SPRINTING)` followed by `START_SPRINTING` next tick, or pulse `keyBindForward.pressed = false` for 1-2 ticks.


7. **SilentAim:**
* *Settings:* `FOV (15° - 180°)`, `Distance (3.0 - 6.0)`, `Hit Chance (0% - 100%)`, `Movement Fix (Silent / Strict)`.
* *Algorithm:* Rotate server-side yaw/pitch via `MotionEvent.PRE` toward target and adjust `MoveInput` strafe/forward vectors (`MoveUtil.fixMovement`) so the player continues moving in their camera direction without flagging movement-direction checks.



### 5.2 Blatant & Movement Category

1. **KillAura:**
* *Settings:* `Target Mode (Single / Switch / Multi)`, `Sort Mode (Distance / Health / HurtTime / FOV)`, `APS (DoubleSlider: 10 - 14)`, `Attack Range (3.0 - 6.0)`, `Swing Range (3.0 - 6.0)`, `AutoBlock Mode (None / Vanilla / Fake / Interact)`, `Rotations (Smooth / Linear / Instant)`, `KeepSprint (Boolean)`.


2. **Scaffold (BlockFly):**
* *Settings:* `Mode (Normal / Telly / GodBridge / Eagle)`, `Rotations (Pitch 79°-82° Diagonal / Strict Raycast)`, `Tower (Boolean)`, `SafeWalk (Boolean)`, `Sprint (Boolean)`, `Block Picker (Switch / Spoof Hotbar Slot)`, `Render Block Count HUD (Boolean)`.
* *Algorithm:* Search adjacent block faces (`EnumFacing`) up to 4 blocks around `BlockPos(player).down()`. Compute exact `Vec3` hit vector on the supporting block face, spoof server rotations toward that face, verify via raycast, and invoke `mc.playerController.onPlayerRightClick`.


3. **Speed & Fly:**
* *Speed Modes:* `Strafe`, `BHop (LowHop)`, `Vanilla`.
* *Fly Modes:* `Vanilla`, `Motion`, `Glide`, `Fireball/Damage Boost` (waits for `hurtTime > 0` from fireball/TNT and multiplies horizontal velocity).


4. **NoSlow, KeepSprint & Blink:**
* *NoSlow:* Prevent `movementInput.moveStrafe *= 0.2F` and `moveForward *= 0.2F` inside `MixinEntityPlayerSP` when `isUsingItem()` is true.
* *KeepSprint:* Prevent `motionX *= 0.6D`, `motionZ *= 0.6D`, and `setSprinting(false)` inside `EntityPlayer.attackTargetEntityWithCurrentItem`.
* *Blink:* Queue all outgoing `C03PacketPlayer` and action packets into a `ConcurrentLinkedQueue<Packet<?>>`, spawn a client-side clone (`EntityOtherPlayerMP`) at the initial position, and flush all queued packets on disable.



### 5.3 Utility & World Category

1. **Eagle (Legit FastBridge):**
* *Settings:* `Delay (DoubleSlider: 15 - 40ms)`, `Blocks Only (Boolean)`, `Pitch Check (> 65°)`.
* *Algorithm:* Check if `mc.theWorld.isAirBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ))` while `mc.thePlayer.onGround` and moving backward (`moveForward < 0`). Set `keyBindSneak` state accordingly.


2. **FastPlace:**
* *Settings:* `Delay (0 - 4 ticks)`, `Blocks Only (Boolean)`, `Projectiles Only (Boolean)`.


3. **ChestStealer & InvManager:**
* *ChestStealer:* Detect `GuiChest`, validate chest title against custom menu names (ignore server selector GUIs), randomize slot loot order with configurable `DoubleSlider` ms delay, and auto-close when empty.
* *InvManager:* Score swords (`ItemSword` base damage + `Enchantment.sharpness` * 1.25), armor (`ItemArmor` damageReduceAmount + `Enchantment.protection`), tools, and golden apples; auto-equip best armor, sort sword to Slot 1, blocks to Slot 2, healing to Slot 3, and drop inferior duplicates while `GuiInventory` is open.


4. **AntiBot & Teams:**
* *AntiBot:* Filter out Watchdog/Hypixel NPCs by checking tab-list ping, unformatted display names, `ticksExisted < 40`, invisible floating entities above player head, and duplicate UUIDs.
* *Teams:* Check armor leather dye color (`ItemArmor.getColor`) and scoreboard team prefix (`§a`, `§c`, etc.) to prevent combat modules from locking onto teammates.



### 5.4 Render & Visuals Category

1. **ESP (Player & Entity):**
* *Settings:* `2D Box (Corner / Full / Outline)`, `Health Bar (Dynamic Color Gradient)`, `Armor Bar`, `Invisible Players (Boolean)`, `Color Mode (Team / Static / Rainbow / Astolfo)`.
* *Algorithm:* Use `GLU.gluProject` with `GL11.glGetFloat(GL_MODELVIEW_MATRIX)`, `GL_PROJECTION_MATRIX`, and `GL_VIEWPORT` to project the 8 corners of each player's interpolated 3D bounding box (`lastTickPos + (pos - lastTickPos) * partialTicks`) onto 2D screen coordinates (`minX, minY, maxX, maxY`).


2. **Chams:**
* In `MixinRendererLivingEntity`, enable `GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)` and `GL11.glPolygonOffset(1.0F, -1100000.0F)` before model render, and reset after, allowing flat or textured colored models to render cleanly through walls.


3. **Nametags & BedESP:**
* Distance-scaled custom 2D nametags showing `[Distance] Name [Health HP]` + rendered item icons for held weapon and 4 armor slots above the tag.
* `BedESP` scans loaded chunks on a background thread for `Blocks.bed`, caches coordinates, and renders 3D translucent bounding boxes + obsidian/endstone defense block overlays.


4. **SelfDestruct (Panic Button):**
* Unregisters all `EventBus` listeners, disables all modules, closes the ClickGUI, restores all reflected Minecraft fields (`rightClickDelayTimer`, `timerSpeed`) to default, deletes runtime string caches, and invokes `System.gc()`.



---

## PHASE 6: VAPE V4 REPLICA CLICKGUI & HUD ENGINE (`Right Shift`)

Build a pixel-accurate, ultra-smooth dark-mode GUI inspired by Vape V4:

1. **Visual Theme & Palette:**
* Background Panel: `#141416` (Header `#1A1A1D`), Accent Color: `#3882F6` (Configurable RGB/HSB Picker + Cigarette Ember Orange `#FF5722` preset), Text Primary: `#EBEBEF`, Text Muted: `#8E8E93`.


2. **Component Architecture:**
* **Multi-Panel Draggable Frames:** One collapsible column per `Category` (`Combat`, `Blatant`, `Render`, `Utility`, `Profiles`) plus a top-left **Vape V4 Search & Sidebar Dock** with a live search filter bar and GUI Scale slider (`0.75x - 1.5x`).
* **Module Buttons:** Left-click toggles module with smooth lerp color transition; Right-click smoothly expands setting drawer using `GL11.glScissor` clipping; Middle-click or shift-click binds key.
* **DoubleSlider Component (`Min - Max`):** Two independent draggable thumbs on a single horizontal track for `CPS`, `Reach`, and `Delay` ranges, preventing `min > max`.
* **Profile Manager Panel:** Create, Save, Load, Delete, and Export/Import JSON configs with a single click.



---

## PHASE 7: AUTONOMOUS BUILD, SELF-HEALING & GITHUB MCP RELEASE

Execute this final loop until 100% completion:

1. **Compile & Verify:**
* Run `./gradlew clean build --stacktrace`.
* If any compilation error, missing import, or Mixin refmap warning occurs, inspect the compiler output, patch the offending `.java` or `build.gradle` file, and re-run `./gradlew build` until `BUILD SUCCESSFUL` is achieved.


2. **GitHub Actions CI/CD Setup:**
* Create `.github/workflows/release.yml` using `actions/setup-java@v4` (Temurin JDK 8) that compiles the mod on every `v*.*.*` tag push and uploads `build/libs/Cigarette-*.jar` to the GitHub Release assets.


3. **Git Commit, Version Bump & Release Publication:**
* Stage and commit the codebase using clean Conventional Commits (`feat(core): initialize Cigarette V1 engine`, `feat(combat): implement Gaussian AutoClicker and GCD AimAssist`, etc.).
* Push to the `main` branch of `Cigarette-V1` via Git / GitHub MCP.
* Create git tag `v1.0.0`, push the tag, and invoke the GitHub MCP release tool to publish **Release `v1.0.0 — Cigarette V1 Initial Public Release**` with full release notes crediting my GitHub profile fetched in Phase 1.



```

---

### Quick Launch Checklist Before You Paste It Into Antigravity 2.0 😳👌

1. **Enable `/teamwork-preview` (or Multi-Agent Mode):** Since the prompt defines **4 parallel sub-agents** (`Agent-Alpha` through `Agent-Delta`), turning on multi-agent orchestration lets Antigravity write the ClickGUI and the Combat modules simultaneously instead of waiting in a single queue 🔥.
2. **Verify JDK 8 Path:** Minecraft 1.8.9 `ForgeGradle 2.1` strictly needs **Java 8 (1.8.0)** to run `setupDecompWorkspace` and compile without Gradle reflection errors. If your machine has JDK 17/21 as the default `JAVA_HOME`, tell Antigravity to pass `-Dorg.gradle.java.home=/path/to/jdk8` when running `./gradlew` 💀🤣.
3. **GitHub MCP Active:** Double-check in `Settings -> Customizations -> MCP Servers` that your GitHub MCP server is green so `Agent-Alpha` can instantly run `get_me` and spin up the `Cigarette-V1` repo on the first turn 😈🔥.

```