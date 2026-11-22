package dev.taezeem14.cigarette.desktop;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative pure-JDK-8 metadata catalog for all 41 Cigarette V1 modules,
 * rich typed default settings, built-in themes, and edition-accurate keycode mapping.
 */
public final class DesktopCatalog {

    /** LWJGL 2 org.lwjgl.input.Keyboard.KEY_RSHIFT (Minecraft 1.8.9 Forge). */
    public static final int KEY_RSHIFT_189 = 54;

    /** Minecraft 26.3 com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT (SDL scancode 229). */
    public static final int KEY_RSHIFT_263 = 229;

    public enum CategoryType {
        COMBAT("Combat", FaIcon.Glyph.CROSSHAIRS),
        BLATANT("Blatant", FaIcon.Glyph.BOLT),
        RENDER("Render", FaIcon.Glyph.EYE),
        UTILITY("Utility", FaIcon.Glyph.WRENCH),
        CLIENT("Client", FaIcon.Glyph.SHIELD);

        private final String label;
        private final FaIcon.Glyph icon;

        CategoryType(String label, FaIcon.Glyph icon) {
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public FaIcon.Glyph getIcon() {
            return icon;
        }
    }

    public enum SettingType {
        BOOLEAN,
        NUMBER,
        RANGE,
        MODE
    }

    public static final class SettingSpec {
        public final String name;
        public final SettingType type;
        public final boolean defaultBool;
        public final double defaultMin;
        public final double defaultMax;
        public final double min;
        public final double max;
        public final double step;
        public final String defaultMode;
        public final String[] modes;

        private SettingSpec(String name, SettingType type, boolean defaultBool,
                            double defaultMin, double defaultMax, double min, double max, double step,
                            String defaultMode, String[] modes) {
            this.name = name;
            this.type = type;
            this.defaultBool = defaultBool;
            this.defaultMin = defaultMin;
            this.defaultMax = defaultMax;
            this.min = min;
            this.max = max;
            this.step = step;
            this.defaultMode = defaultMode;
            this.modes = modes;
        }

        public static SettingSpec bool(String name, boolean defaultValue) {
            return new SettingSpec(name, SettingType.BOOLEAN, defaultValue, 0, 0, 0, 1, 1, null, null);
        }

        public static SettingSpec number(String name, double defaultValue, double min, double max, double step) {
            return new SettingSpec(name, SettingType.NUMBER, false, defaultValue, defaultValue, min, max, step, null, null);
        }

        public static SettingSpec range(String name, double defaultMin, double defaultMax, double min, double max, double step) {
            return new SettingSpec(name, SettingType.RANGE, false, defaultMin, defaultMax, min, max, step, null, null);
        }

        public static SettingSpec mode(String name, String defaultMode, String... choices) {
            return new SettingSpec(name, SettingType.MODE, false, 0, 0, 0, 0, 0, defaultMode, choices);
        }

        public Object createDefaultJsonValue() {
            if (type == SettingType.BOOLEAN) {
                return Boolean.valueOf(defaultBool);
            }
            if (type == SettingType.NUMBER) {
                return Double.valueOf(defaultMin);
            }
            if (type == SettingType.RANGE) {
                List<Object> pair = new ArrayList<Object>(2);
                pair.add(Double.valueOf(defaultMin));
                pair.add(Double.valueOf(defaultMax));
                return pair;
            }
            return defaultMode != null ? defaultMode : "";
        }
    }

    public static final class ModuleDef {
        public final String name;
        public final String description;
        public final CategoryType category;
        public final boolean defaultEnabled;
        public final int defaultKeyCode189;
        public final int defaultKeyCode263;
        public final List<SettingSpec> settings189;
        public final List<SettingSpec> settings263;

        public ModuleDef(String name, String description, CategoryType category, boolean defaultEnabled,
                         int defaultKeyCode189, int defaultKeyCode263,
                         List<SettingSpec> settings189, List<SettingSpec> settings263) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.defaultEnabled = defaultEnabled;
            this.defaultKeyCode189 = defaultKeyCode189;
            this.defaultKeyCode263 = defaultKeyCode263;
            this.settings189 = settings189 != null
                    ? Collections.unmodifiableList(settings189)
                    : Collections.<SettingSpec>emptyList();
            this.settings263 = settings263 != null
                    ? Collections.unmodifiableList(settings263)
                    : this.settings189;
        }

        public int getDefaultKeyCode(boolean modern) {
            return modern ? defaultKeyCode263 : defaultKeyCode189;
        }

        public int getDefaultKeyCode(String edition) {
            return getDefaultKeyCode(isModernEdition(edition));
        }

        public List<SettingSpec> getSettings(boolean modern) {
            return modern ? settings263 : settings189;
        }

        public List<SettingSpec> getSettings(String edition) {
            return getSettings(isModernEdition(edition));
        }

        public Map<String, Object> createDefaultSettingsMap(boolean modern) {
            Map<String, Object> out = new LinkedHashMap<String, Object>();
            for (SettingSpec spec : getSettings(modern)) {
                out.put(spec.name, spec.createDefaultJsonValue());
            }
            return out;
        }
    }

    public static final String[] THEME_PRESETS = {
            "Electric Blue",
            "Ember Orange",
            "Amethyst",
            "Emerald",
            "Rainbow"
    };

    private static final List<ModuleDef> MODULES;

    static {
        List<ModuleDef> list = new ArrayList<ModuleDef>();

        // ─── Combat (9) ───────────────────────────────────────────────────────
        List<SettingSpec> aimSettings = specs(
                SettingSpec.number("Horizontal Speed", 3.5, 1.0, 10.0, 0.1),
                SettingSpec.number("Vertical Speed", 2.0, 1.0, 10.0, 0.1),
                SettingSpec.number("FOV", 90.0, 15.0, 360.0, 5.0),
                SettingSpec.number("Distance", 4.5, 3.0, 6.0, 0.1),
                SettingSpec.bool("Click Aim", true),
                SettingSpec.bool("Weapon Only", true),
                SettingSpec.bool("Aim Vertically", false),
                SettingSpec.bool("Multipoint Hitbox", true),
                SettingSpec.bool("Ignore Teams", true),
                SettingSpec.bool("Break Blocks Check", true)
        );
        list.add(mod("AimAssist", "GCD-quantized horizontal and vertical target smoothing", CategoryType.COMBAT, false, aimSettings));

        list.add(modSplit("AutoClicker", "Box-Muller Gaussian randomized CPS clicker", CategoryType.COMBAT, false,
                specs(
                        SettingSpec.range("Left CPS", 11.5, 14.5, 1.0, 20.0, 0.1),
                        SettingSpec.range("Right CPS", 14.0, 18.0, 1.0, 20.0, 0.1),
                        SettingSpec.bool("BlockHit", true),
                        SettingSpec.number("BlockHit Chance", 35.0, 0.0, 100.0, 5.0),
                        SettingSpec.number("Jitter Strength", 0.0, 0.0, 2.0, 0.1),
                        SettingSpec.bool("Weapon Only", true),
                        SettingSpec.bool("Break Blocks", true),
                        SettingSpec.bool("Inventory Fill", false)
                ),
                specs(
                        SettingSpec.range("Left CPS", 11.5, 14.5, 1.0, 20.0, 0.1),
                        SettingSpec.range("Right CPS", 14.0, 18.0, 1.0, 20.0, 0.1),
                        SettingSpec.bool("BlockHit", true),
                        SettingSpec.number("BlockHit Chance", 35.0, 0.0, 100.0, 5.0),
                        SettingSpec.number("Jitter Strength", 0.0, 0.0, 2.0, 0.1),
                        SettingSpec.bool("Weapon Only", true),
                        SettingSpec.bool("Break Blocks", true)
                )));

        list.add(modSplit("Reach", "Closest-point entity interaction range extender", CategoryType.COMBAT, false,
                specs(
                        SettingSpec.range("Range", 3.00, 3.35, 3.00, 6.00, 0.01),
                        SettingSpec.number("Chance", 100.0, 0.0, 100.0, 1.0),
                        SettingSpec.bool("Sprint Only", false),
                        SettingSpec.bool("Water Check", false),
                        SettingSpec.bool("Hit Through Walls", false)
                ),
                specs(
                        SettingSpec.range("Range", 3.00, 3.35, 3.00, 6.00, 0.01),
                        SettingSpec.number("Chance", 100.0, 0.0, 100.0, 1.0),
                        SettingSpec.bool("Sprint Only", false),
                        SettingSpec.bool("Water Check", false)
                )));

        list.add(modSplit("HitBoxes", "Expands entity target bounding boxes cleanly", CategoryType.COMBAT, false,
                specs(SettingSpec.number("Expand", 0.20, 0.00, 1.00, 0.01)),
                specs(SettingSpec.number("Expand", 0.25, 0.00, 1.00, 0.05))));

        list.add(mod("Velocity", "Scales or delays incoming knockback packets", CategoryType.COMBAT, false,
                specs(
                        SettingSpec.mode("Mode", "Simple", "Simple", "Jump Reset", "Delay", "Intave"),
                        SettingSpec.number("Horizontal", 0.0, 0.0, 100.0, 1.0),
                        SettingSpec.number("Vertical", 0.0, 0.0, 100.0, 1.0),
                        SettingSpec.number("Chance", 100.0, 0.0, 100.0, 1.0),
                        SettingSpec.bool("Only While Targeting", false),
                        SettingSpec.bool("Water Check", false)
                )));

        list.add(mod("WTap", "Automatic sprint reset timing for combo knockback", CategoryType.COMBAT, false,
                specs(
                        SettingSpec.mode("Mode", "Packet", "Packet", "Legit Key"),
                        SettingSpec.range("Delay", 30.0, 65.0, 10.0, 150.0, 1.0),
                        SettingSpec.number("Chance", 100.0, 0.0, 100.0, 1.0)
                )));

        list.add(mod("SilentAim", "Server-side aim rotations with movement correction", CategoryType.COMBAT, false,
                specs(
                        SettingSpec.number("FOV", 90.0, 15.0, 180.0, 5.0),
                        SettingSpec.number("Distance", 4.2, 3.0, 6.0, 0.1),
                        SettingSpec.number("Hit Chance", 100.0, 0.0, 100.0, 1.0),
                        SettingSpec.mode("Movement Fix", "Silent", "Silent", "Strict", "None")
                )));

        list.add(mod("ThrowPot", "Rapid hotbar splash potion recovery thrower", CategoryType.COMBAT, false,
                specs(
                        SettingSpec.number("Health Threshold", 13.0, 1.0, 20.0, 0.5),
                        SettingSpec.bool("Speed Pot", true),
                        SettingSpec.bool("Health Pot", true),
                        SettingSpec.number("Pitch", 89.0, 80.0, 90.0, 1.0)
                )));

        list.add(modSplit("RodAimbot", "Lead-compensated fishing rod trajectory assist", CategoryType.COMBAT, false,
                specs(
                        SettingSpec.number("Range", 12.0, 5.0, 20.0, 1.0),
                        SettingSpec.number("FOV", 120.0, 15.0, 360.0, 5.0)
                ),
                specs(
                        SettingSpec.number("FOV", 90.0, 15.0, 180.0, 5.0),
                        SettingSpec.number("Range", 12.0, 4.0, 20.0, 0.5),
                        SettingSpec.bool("Velocity Prediction", true)
                )));

        // ─── Blatant (10) ─────────────────────────────────────────────────────
        list.add(modSplit("KillAura", "Automated target acquisition with cooldown or APS timing", CategoryType.BLATANT, false,
                specs(
                        SettingSpec.mode("Target Mode", "Single", "Single", "Switch", "Multi"),
                        SettingSpec.mode("Sort Mode", "Distance", "Distance", "Health", "HurtTime", "FOV"),
                        SettingSpec.range("APS", 10.0, 14.0, 1.0, 20.0, 0.5),
                        SettingSpec.number("Attack Range", 4.2, 3.0, 6.0, 0.1),
                        SettingSpec.number("Swing Range", 4.5, 3.0, 6.0, 0.1),
                        SettingSpec.mode("AutoBlock Mode", "Fake", "None", "Vanilla", "Fake", "Interact"),
                        SettingSpec.mode("Rotations", "Smooth", "Smooth", "Linear", "Instant"),
                        SettingSpec.bool("KeepSprint", true),
                        SettingSpec.bool("Players", true),
                        SettingSpec.bool("Mobs", false),
                        SettingSpec.bool("Invisibles", true)
                ),
                specs(
                        SettingSpec.mode("Target Mode", "Single", "Single", "Switch", "Multi"),
                        SettingSpec.mode("Sort Mode", "Distance", "Distance", "Health", "HurtTime", "FOV"),
                        SettingSpec.bool("1.9+ Cooldown", true),
                        SettingSpec.range("APS", 10.0, 14.0, 1.0, 20.0, 0.5),
                        SettingSpec.number("Attack Range", 3.6, 2.5, 6.0, 0.1),
                        SettingSpec.number("Swing Range", 4.0, 2.5, 6.0, 0.1),
                        SettingSpec.mode("Rotations", "Smooth", "Smooth", "Linear", "Instant"),
                        SettingSpec.bool("KeepSprint", true),
                        SettingSpec.bool("Players", true),
                        SettingSpec.bool("Mobs", false),
                        SettingSpec.bool("Invisibles", true)
                )));

        list.add(mod("Scaffold", "Rapid bridge block placement with rotation & slot picking", CategoryType.BLATANT, false,
                specs(
                        SettingSpec.mode("Mode", "Normal", "Normal", "Telly", "GodBridge", "Eagle"),
                        SettingSpec.mode("Rotations", "Diagonal", "Diagonal", "Strict Raycast"),
                        SettingSpec.number("Pitch", 81.0, 75.0, 85.0, 0.5),
                        SettingSpec.bool("Tower", true),
                        SettingSpec.bool("SafeWalk", true),
                        SettingSpec.bool("Sprint", false),
                        SettingSpec.mode("Block Picker", "Switch", "Switch", "Spoof"),
                        SettingSpec.bool("Render Block Count", true)
                )));

        list.add(mod("Speed", "Strafe, BHop, and Vanilla movement acceleration", CategoryType.BLATANT, false,
                specs(
                        SettingSpec.mode("Mode", "Strafe", "Strafe", "BHop", "Vanilla"),
                        SettingSpec.number("Speed", 1.4, 0.5, 5.0, 0.1)
                )));

        list.add(mod("Fly", "Creative/Vanilla flight and boost modes", CategoryType.BLATANT, false,
                specs(
                        SettingSpec.mode("Mode", "Vanilla", "Vanilla", "Motion", "Glide", "Damage Boost"),
                        SettingSpec.number("Speed", 2.0, 0.5, 5.0, 0.1)
                )));

        list.add(modSplit("Criticals", "Packet, Mini-Jump, and NoGround critical strike triggers", CategoryType.BLATANT, false,
                specs(SettingSpec.mode("Mode", "Packet", "Packet", "MiniHop")),
                specs(SettingSpec.mode("Mode", "Packet", "Packet", "Mini-Jump", "NoGround"))));

        list.add(modSplit("NoSlow", "Removes item-use and bow movement slowdown", CategoryType.BLATANT, false,
                specs(
                        SettingSpec.mode("Mode", "Vanilla", "Vanilla", "Packet"),
                        SettingSpec.bool("Sword", true),
                        SettingSpec.bool("Bow", true),
                        SettingSpec.bool("Consumables", true)
                ),
                specs(
                        SettingSpec.mode("Mode", "Vanilla", "Vanilla", "Packet"),
                        SettingSpec.bool("Shield", true),
                        SettingSpec.bool("Bow / Crossbow", true),
                        SettingSpec.bool("Consumables", true)
                )));

        list.add(modSplit("KeepSprint", "Maintains sprint momentum after hitting targets", CategoryType.BLATANT, false,
                specs(SettingSpec.number("Slowdown Reduction", 100.0, 0.0, 100.0, 1.0)),
                specs(SettingSpec.number("Horizontal Slowdown %", 0.0, 0.0, 40.0, 5.0))));

        list.add(modSplit("Blink", "Buffers and bursts outgoing movement packets", CategoryType.BLATANT, false,
                specs(
                        SettingSpec.bool("Pulse", false),
                        SettingSpec.number("Pulse Delay", 200.0, 50.0, 1000.0, 50.0)
                ),
                specs(
                        SettingSpec.bool("Pulse", false),
                        SettingSpec.number("Pulse Delay (ticks)", 10.0, 2.0, 60.0, 1.0),
                        SettingSpec.bool("Render Origin Marker", true)
                )));

        list.add(mod("Timer", "Scales client game tick simulation speed", CategoryType.BLATANT, false,
                specs(SettingSpec.number("Speed", 1.2, 0.1, 5.0, 0.05))));

        list.add(modSplit("AntiVoid", "Pulls back before fatal void falls", CategoryType.BLATANT, false,
                specs(
                        SettingSpec.number("Fall Distance", 5.0, 3.0, 15.0, 0.5),
                        SettingSpec.mode("Mode", "Packet Bounce", "Packet Bounce", "Motion")
                ),
                specs(
                        SettingSpec.mode("Mode", "Setback", "Setback", "Packet"),
                        SettingSpec.number("Fall Distance", 4.0, 2.0, 10.0, 0.5)
                )));

        // ─── Render (10) ──────────────────────────────────────────────────────
        list.add(new ModuleDef(
                "ClickGUI",
                "Opens the in-game Vape V4 interface (Right Shift)",
                CategoryType.RENDER,
                false,
                KEY_RSHIFT_189,
                KEY_RSHIFT_263,
                specs(
                        SettingSpec.bool("Blur Backdrop", true),
                        SettingSpec.mode("Theme", "Electric Blue", THEME_PRESETS)
                ),
                specs(
                        SettingSpec.mode("Theme", "Electric Blue", THEME_PRESETS),
                        SettingSpec.bool("Use Custom Accent", false),
                        SettingSpec.bool("Background Blur", true)
                )
        ));

        list.add(mod("HUD", "Renders the Cigarette watermark and active module ArrayList", CategoryType.RENDER, true,
                specs(
                        SettingSpec.bool("Watermark", true),
                        SettingSpec.bool("ArrayList", true),
                        SettingSpec.bool("Side Bar", true),
                        SettingSpec.bool("Coordinates", true),
                        SettingSpec.bool("FPS Display", true),
                        SettingSpec.bool("CPS Display", true)
                )));

        list.add(mod("TargetHUD", "Displays target health, armor, and distance card", CategoryType.RENDER, false,
                specs(
                        SettingSpec.bool("Smooth Animation", true),
                        SettingSpec.bool("Show Armor", true),
                        SettingSpec.mode("Color Mode", "Health", "Health", "Theme", "Rainbow")
                )));

        list.add(modSplit("ESP", "2D/3D projected bounding boxes, health, and armor bars", CategoryType.RENDER, false,
                specs(
                        SettingSpec.mode("Mode", "Full", "Full", "Corner", "Outline"),
                        SettingSpec.bool("Health Bar", true),
                        SettingSpec.bool("Armor Bar", true),
                        SettingSpec.bool("Invisible Players", false),
                        SettingSpec.mode("Color Mode", "Static", "Static", "Team", "Rainbow", "Astolfo")
                ),
                specs(
                        SettingSpec.mode("Mode", "Full", "Full", "Corner", "Outline", "3D Box"),
                        SettingSpec.bool("Health Bar", true),
                        SettingSpec.bool("Armor Bar", true),
                        SettingSpec.bool("Invisible Players", false),
                        SettingSpec.mode("Color Mode", "Static", "Static", "Team", "Rainbow", "Astolfo")
                )));

        list.add(modSplit("Chams", "Highlights player silhouettes cleanly through walls", CategoryType.RENDER, false,
                specs(
                        SettingSpec.mode("Mode", "Static", "Static", "Rainbow", "Team"),
                        SettingSpec.bool("Textured", true)
                ),
                specs(
                        SettingSpec.mode("Mode", "Static", "Static", "Rainbow", "Team")
                )));

        list.add(modSplit("Nametags", "Projects enhanced distance, health, and equipment tags", CategoryType.RENDER, false,
                specs(
                        SettingSpec.bool("Distance", true),
                        SettingSpec.bool("Health", true),
                        SettingSpec.bool("Armor & Items", true),
                        SettingSpec.number("Scale", 1.8, 1.0, 3.5, 0.1)
                ),
                specs(
                        SettingSpec.bool("Distance", true),
                        SettingSpec.bool("Health", true),
                        SettingSpec.bool("Armor & Items", true),
                        SettingSpec.number("Scale", 1.0, 0.7, 1.8, 0.05)
                )));

        list.add(modSplit("Tracers", "Draws color-coded lines toward nearby players", CategoryType.RENDER, false,
                specs(
                        SettingSpec.mode("Color Mode", "Distance", "Distance", "Team", "Static", "Rainbow"),
                        SettingSpec.number("Line Width", 1.5, 0.5, 3.0, 0.1),
                        SettingSpec.bool("Invisible Players", false)
                ),
                specs(
                        SettingSpec.number("Line Width", 1.5, 0.5, 3.0, 0.1),
                        SettingSpec.bool("Invisible Players", false),
                        SettingSpec.bool("Distance Color Fade", true)
                )));

        list.add(mod("BedESP", "Locates nearby beds for fast BedWars defense awareness", CategoryType.RENDER, false,
                specs(
                        SettingSpec.number("Scan Radius", 32.0, 8.0, 48.0, 2.0),
                        SettingSpec.bool("Fill", true),
                        SettingSpec.bool("Outline", true),
                        SettingSpec.number("Line Width", 1.5, 0.5, 3.0, 0.1),
                        SettingSpec.bool("Defense Overlay", true)
                )));

        list.add(mod("Fullbright", "Permanent night-vision clarity without flickering", CategoryType.RENDER, false, specs()));

        list.add(mod("Trajectories", "Predicts landing paths for arrows, pearls, and potions", CategoryType.RENDER, false,
                specs(
                        SettingSpec.number("Line Width", 1.5, 0.5, 3.0, 0.1),
                        SettingSpec.bool("Landing Box", true)
                )));

        // ─── Utility (9) ──────────────────────────────────────────────────────
        list.add(mod("ChestStealer", "Humanized chest looting with menu-title protection", CategoryType.UTILITY, false,
                specs(
                        SettingSpec.range("Delay", 60.0, 100.0, 20.0, 300.0, 5.0),
                        SettingSpec.bool("Auto Close", true),
                        SettingSpec.bool("Name Check", true),
                        SettingSpec.bool("Randomize Order", true)
                )));

        list.add(mod("InvManager", "Sorts weapons, blocks, tools, and drops clutter", CategoryType.UTILITY, false,
                specs(
                        SettingSpec.range("Delay", 60.0, 100.0, 20.0, 250.0, 5.0),
                        SettingSpec.bool("Auto Armor", true),
                        SettingSpec.bool("Drop Trash", true),
                        SettingSpec.bool("Sort Hotbar", true),
                        SettingSpec.bool("Open Inv Only", true)
                )));

        list.add(mod("AutoTool", "Selects optimal breaking tool per block hardness", CategoryType.UTILITY, false,
                specs(SettingSpec.bool("Switch Back", true))));

        list.add(mod("AutoArmor", "Automatically equips highest-protection armor sets", CategoryType.UTILITY, false,
                specs(
                        SettingSpec.range("Delay", 60.0, 120.0, 20.0, 300.0, 5.0),
                        SettingSpec.bool("Open Inv Only", false)
                )));

        list.add(mod("FastPlace", "Reduces right-click block and projectile placement delay", CategoryType.UTILITY, false,
                specs(
                        SettingSpec.number("Delay", 0.0, 0.0, 4.0, 1.0),
                        SettingSpec.bool("Blocks Only", true),
                        SettingSpec.bool("Projectiles Only", false)
                )));

        list.add(modSplit("Eagle", "Automatically sneaks over block edges while bridging", CategoryType.UTILITY, false,
                specs(
                        SettingSpec.range("Delay", 15.0, 40.0, 0.0, 100.0, 1.0),
                        SettingSpec.bool("Blocks Only", true),
                        SettingSpec.bool("Pitch Check (>65\u00B0)", true)
                ),
                specs(
                        SettingSpec.range("Delay", 15.0, 40.0, 0.0, 100.0, 1.0),
                        SettingSpec.bool("Blocks Only", true),
                        SettingSpec.bool("Pitch Check (>65)", true)
                )));

        list.add(mod("BridgeAssist", "Edge alignment and smooth pitch assist for bridging", CategoryType.UTILITY, false,
                specs(
                        SettingSpec.mode("Yaw Snap", "45 Degrees", "45 Degrees", "90 Degrees", "None"),
                        SettingSpec.bool("Pitch Assist", true),
                        SettingSpec.number("Assisted Pitch", 78.0, 70.0, 85.0, 0.5)
                )));

        list.add(mod("AntiBot", "Filters server NPC/watchdog decoys from targeting", CategoryType.UTILITY, false,
                specs(
                        SettingSpec.bool("Tab Check", true),
                        SettingSpec.bool("Ping Check", false),
                        SettingSpec.number("Ticks Existed", 40.0, 10.0, 100.0, 5.0),
                        SettingSpec.bool("Invisible Check", true),
                        SettingSpec.bool("Duplicate UUID", true)
                )));

        list.add(mod("Teams", "Detects teammates via scoreboard teams and name color", CategoryType.UTILITY, false,
                specs(
                        SettingSpec.bool("Scoreboard Team", true),
                        SettingSpec.bool("Leather Armor Color", true)
                )));

        // ─── Client (3) ───────────────────────────────────────────────────────
        list.add(modSplit("ArrayList", "Configures HUD module list sorting and visual tags", CategoryType.CLIENT, true,
                specs(
                        SettingSpec.mode("Color Mode", "Theme", "Theme", "Rainbow", "Astolfo", "Static"),
                        SettingSpec.number("Rainbow Speed", 1.0, 0.5, 3.0, 0.1),
                        SettingSpec.bool("Background", true),
                        SettingSpec.bool("Suffix", true),
                        SettingSpec.mode("Font", "Custom", "Custom", "Minecraft"),
                        SettingSpec.bool("Animation", true)
                ),
                specs(
                        SettingSpec.mode("Color Mode", "Theme", "Theme", "Rainbow", "Astolfo", "Static"),
                        SettingSpec.number("Rainbow Speed", 1.0, 0.2, 5.0, 0.1),
                        SettingSpec.bool("Background", true),
                        SettingSpec.bool("Suffix", true),
                        SettingSpec.bool("Slide Animation", true)
                )));

        list.add(mod("FriendManager", "Protects added friends from combat modules", CategoryType.CLIENT, true, specs()));
        list.add(mod("SelfDestruct", "Tears down modules, keybinds, and overlays cleanly", CategoryType.CLIENT, false, specs()));

        MODULES = Collections.unmodifiableList(list);
    }

    private static List<SettingSpec> specs(SettingSpec... items) {
        return new ArrayList<SettingSpec>(Arrays.asList(items));
    }

    private static ModuleDef mod(String name, String desc, CategoryType cat, boolean defEn, List<SettingSpec> settings) {
        return new ModuleDef(name, desc, cat, defEn, 0, 0, settings, settings);
    }

    private static ModuleDef modSplit(String name, String desc, CategoryType cat, boolean defEn,
                                      List<SettingSpec> settings189, List<SettingSpec> settings263) {
        return new ModuleDef(name, desc, cat, defEn, 0, 0, settings189, settings263);
    }

    private DesktopCatalog() {
    }

    public static List<ModuleDef> getModules() {
        return MODULES;
    }

    public static ModuleDef findModule(String name) {
        if (name == null) return null;
        for (ModuleDef m : MODULES) {
            if (m.name.equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    public static boolean isModernEdition(String edition) {
        return edition != null && edition.contains("26.3");
    }

    public static int getDefaultClickGuiKey(boolean modern) {
        return modern ? KEY_RSHIFT_263 : KEY_RSHIFT_189;
    }

    public static boolean isBrokenClickGuiKeyCode(int keyCode, boolean modern) {
        if (keyCode <= 0) return true;
        // GLFW RSHIFT (344) or SDLK_RSHIFT (1073742053) are invalid in both 1.8.9 LWJGL2 and 26.3 InputConstants.KEY_*
        if (keyCode == 344 || keyCode == 1073742053) return true;
        // 26.3 RSHIFT (229) is not a valid LWJGL 2 key on 1.8.9
        if (!modern && keyCode == KEY_RSHIFT_263) return true;
        return false;
    }

    public static Color getThemeColor(String preset) {
        if ("Ember Orange".equalsIgnoreCase(preset)) return new Color(0xFF, 0x57, 0x22);
        if ("Amethyst".equalsIgnoreCase(preset)) return new Color(0x8B, 0x5C, 0xF6);
        if ("Emerald".equalsIgnoreCase(preset)) return new Color(0x10, 0xB9, 0x81);
        if ("Rainbow".equalsIgnoreCase(preset)) return new Color(0x38, 0xBD, 0xF8);
        return new Color(0x38, 0x82, 0xF6);
    }

    public static String nextThemePreset(String current) {
        List<String> presets = Arrays.asList(THEME_PRESETS);
        int idx = 0;
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).equalsIgnoreCase(current)) {
                idx = (i + 1) % presets.size();
                break;
            }
        }
        return presets.get(idx);
    }

    public static String detectEditionFromClasspath() {
        if (DesktopCatalog.class.getResource("/cigarette.client.mixins.json") != null
                || DesktopCatalog.class.getResource("/fabric.mod.json") != null) {
            return "26.3 Fabric";
        }
        return "1.8.9 Forge";
    }

    // ─── Edition-Accurate KeyCode Mapping & Formatting ────────────────────────

    private static final Map<Integer, String> NAMES_189 = new LinkedHashMap<Integer, String>();
    private static final Map<Integer, String> NAMES_263 = new LinkedHashMap<Integer, String>();

    static {
        // 1.8.9 LWJGL 2 Keyboard.KEY_*
        NAMES_189.put(Integer.valueOf(1), "ESC");
        NAMES_189.put(Integer.valueOf(2), "1");
        NAMES_189.put(Integer.valueOf(3), "2");
        NAMES_189.put(Integer.valueOf(4), "3");
        NAMES_189.put(Integer.valueOf(5), "4");
        NAMES_189.put(Integer.valueOf(6), "5");
        NAMES_189.put(Integer.valueOf(7), "6");
        NAMES_189.put(Integer.valueOf(8), "7");
        NAMES_189.put(Integer.valueOf(9), "8");
        NAMES_189.put(Integer.valueOf(10), "9");
        NAMES_189.put(Integer.valueOf(11), "0");
        NAMES_189.put(Integer.valueOf(12), "MINUS");
        NAMES_189.put(Integer.valueOf(13), "EQUALS");
        NAMES_189.put(Integer.valueOf(14), "BACK");
        NAMES_189.put(Integer.valueOf(15), "TAB");
        NAMES_189.put(Integer.valueOf(16), "Q");
        NAMES_189.put(Integer.valueOf(17), "W");
        NAMES_189.put(Integer.valueOf(18), "E");
        NAMES_189.put(Integer.valueOf(19), "R");
        NAMES_189.put(Integer.valueOf(20), "T");
        NAMES_189.put(Integer.valueOf(21), "Y");
        NAMES_189.put(Integer.valueOf(22), "U");
        NAMES_189.put(Integer.valueOf(23), "I");
        NAMES_189.put(Integer.valueOf(24), "O");
        NAMES_189.put(Integer.valueOf(25), "P");
        NAMES_189.put(Integer.valueOf(26), "[");
        NAMES_189.put(Integer.valueOf(27), "]");
        NAMES_189.put(Integer.valueOf(28), "ENTER");
        NAMES_189.put(Integer.valueOf(29), "LCTRL");
        NAMES_189.put(Integer.valueOf(30), "A");
        NAMES_189.put(Integer.valueOf(31), "S");
        NAMES_189.put(Integer.valueOf(32), "D");
        NAMES_189.put(Integer.valueOf(33), "F");
        NAMES_189.put(Integer.valueOf(34), "G");
        NAMES_189.put(Integer.valueOf(35), "H");
        NAMES_189.put(Integer.valueOf(36), "J");
        NAMES_189.put(Integer.valueOf(37), "K");
        NAMES_189.put(Integer.valueOf(38), "L");
        NAMES_189.put(Integer.valueOf(39), ";");
        NAMES_189.put(Integer.valueOf(40), "'");
        NAMES_189.put(Integer.valueOf(41), "`");
        NAMES_189.put(Integer.valueOf(42), "LSHIFT");
        NAMES_189.put(Integer.valueOf(43), "\\");
        NAMES_189.put(Integer.valueOf(44), "Z");
        NAMES_189.put(Integer.valueOf(45), "X");
        NAMES_189.put(Integer.valueOf(46), "C");
        NAMES_189.put(Integer.valueOf(47), "V");
        NAMES_189.put(Integer.valueOf(48), "B");
        NAMES_189.put(Integer.valueOf(49), "N");
        NAMES_189.put(Integer.valueOf(50), "M");
        NAMES_189.put(Integer.valueOf(51), ",");
        NAMES_189.put(Integer.valueOf(52), ".");
        NAMES_189.put(Integer.valueOf(53), "/");
        NAMES_189.put(Integer.valueOf(54), "RSHIFT");
        NAMES_189.put(Integer.valueOf(56), "LALT");
        NAMES_189.put(Integer.valueOf(57), "SPACE");
        NAMES_189.put(Integer.valueOf(58), "CAPS");
        for (int i = 1; i <= 10; i++) NAMES_189.put(Integer.valueOf(58 + i), "F" + i);
        NAMES_189.put(Integer.valueOf(87), "F11");
        NAMES_189.put(Integer.valueOf(88), "F12");
        NAMES_189.put(Integer.valueOf(82), "NUM0");
        NAMES_189.put(Integer.valueOf(79), "NUM1");
        NAMES_189.put(Integer.valueOf(80), "NUM2");
        NAMES_189.put(Integer.valueOf(81), "NUM3");
        NAMES_189.put(Integer.valueOf(75), "NUM4");
        NAMES_189.put(Integer.valueOf(76), "NUM5");
        NAMES_189.put(Integer.valueOf(77), "NUM6");
        NAMES_189.put(Integer.valueOf(71), "NUM7");
        NAMES_189.put(Integer.valueOf(72), "NUM8");
        NAMES_189.put(Integer.valueOf(73), "NUM9");
        NAMES_189.put(Integer.valueOf(157), "RCTRL");
        NAMES_189.put(Integer.valueOf(184), "RALT");
        NAMES_189.put(Integer.valueOf(199), "HOME");
        NAMES_189.put(Integer.valueOf(200), "UP");
        NAMES_189.put(Integer.valueOf(201), "PGUP");
        NAMES_189.put(Integer.valueOf(203), "LEFT");
        NAMES_189.put(Integer.valueOf(205), "RIGHT");
        NAMES_189.put(Integer.valueOf(207), "END");
        NAMES_189.put(Integer.valueOf(208), "DOWN");
        NAMES_189.put(Integer.valueOf(209), "PGDN");
        NAMES_189.put(Integer.valueOf(210), "INSERT");
        NAMES_189.put(Integer.valueOf(211), "DELETE");

        // 26.3 Fabric InputConstants.KEY_* (SDL scancodes)
        for (int i = 0; i < 26; i++) {
            NAMES_263.put(Integer.valueOf(4 + i), String.valueOf((char) ('A' + i)));
        }
        for (int i = 1; i <= 9; i++) {
            NAMES_263.put(Integer.valueOf(29 + i), String.valueOf(i));
        }
        NAMES_263.put(Integer.valueOf(39), "0");
        NAMES_263.put(Integer.valueOf(40), "ENTER");
        NAMES_263.put(Integer.valueOf(41), "ESC");
        NAMES_263.put(Integer.valueOf(42), "BACK");
        NAMES_263.put(Integer.valueOf(43), "TAB");
        NAMES_263.put(Integer.valueOf(44), "SPACE");
        NAMES_263.put(Integer.valueOf(45), "MINUS");
        NAMES_263.put(Integer.valueOf(46), "EQUALS");
        NAMES_263.put(Integer.valueOf(47), "[");
        NAMES_263.put(Integer.valueOf(48), "]");
        NAMES_263.put(Integer.valueOf(49), "\\");
        NAMES_263.put(Integer.valueOf(51), ";");
        NAMES_263.put(Integer.valueOf(52), "'");
        NAMES_263.put(Integer.valueOf(53), "`");
        NAMES_263.put(Integer.valueOf(54), ",");
        NAMES_263.put(Integer.valueOf(55), ".");
        NAMES_263.put(Integer.valueOf(56), "/");
        NAMES_263.put(Integer.valueOf(57), "CAPS");
        for (int i = 1; i <= 12; i++) NAMES_263.put(Integer.valueOf(57 + i), "F" + i);
        NAMES_263.put(Integer.valueOf(73), "INSERT");
        NAMES_263.put(Integer.valueOf(74), "HOME");
        NAMES_263.put(Integer.valueOf(75), "PGUP");
        NAMES_263.put(Integer.valueOf(76), "DELETE");
        NAMES_263.put(Integer.valueOf(77), "END");
        NAMES_263.put(Integer.valueOf(78), "PGDN");
        NAMES_263.put(Integer.valueOf(79), "RIGHT");
        NAMES_263.put(Integer.valueOf(80), "LEFT");
        NAMES_263.put(Integer.valueOf(81), "DOWN");
        NAMES_263.put(Integer.valueOf(82), "UP");
        for (int i = 1; i <= 9; i++) NAMES_263.put(Integer.valueOf(88 + i), "NUM" + i);
        NAMES_263.put(Integer.valueOf(98), "NUM0");
        NAMES_263.put(Integer.valueOf(224), "LCTRL");
        NAMES_263.put(Integer.valueOf(225), "LSHIFT");
        NAMES_263.put(Integer.valueOf(226), "LALT");
        NAMES_263.put(Integer.valueOf(228), "RCTRL");
        NAMES_263.put(Integer.valueOf(229), "RSHIFT");
        NAMES_263.put(Integer.valueOf(230), "RALT");
    }

    public static String formatKeyCode(int keyCode, boolean modern) {
        if (keyCode <= 0) return "NONE";
        Map<Integer, String> table = modern ? NAMES_263 : NAMES_189;
        String found = table.get(Integer.valueOf(keyCode));
        return found != null ? found : "KEY_" + keyCode;
    }

    public static int awtToMinecraftKeyCode(int vk, int location, boolean modern) {
        if (modern) {
            return awtTo263KeyCode(vk, location);
        }
        return awtTo189KeyCode(vk, location);
    }

    private static int awtTo263KeyCode(int vk, int location) {
        if (vk >= KeyEvent.VK_A && vk <= KeyEvent.VK_Z) {
            return 4 + (vk - KeyEvent.VK_A);
        }
        if (vk >= KeyEvent.VK_1 && vk <= KeyEvent.VK_9) {
            return 30 + (vk - KeyEvent.VK_1);
        }
        if (vk == KeyEvent.VK_0) return 39;
        if (vk >= KeyEvent.VK_F1 && vk <= KeyEvent.VK_F12) {
            return 58 + (vk - KeyEvent.VK_F1);
        }
        if (vk >= KeyEvent.VK_NUMPAD1 && vk <= KeyEvent.VK_NUMPAD9) {
            return 89 + (vk - KeyEvent.VK_NUMPAD1);
        }
        switch (vk) {
            case KeyEvent.VK_NUMPAD0: return 98;
            case KeyEvent.VK_SHIFT: return location == KeyEvent.KEY_LOCATION_RIGHT ? 229 : 225;
            case KeyEvent.VK_CONTROL: return location == KeyEvent.KEY_LOCATION_RIGHT ? 228 : 224;
            case KeyEvent.VK_ALT: return location == KeyEvent.KEY_LOCATION_RIGHT ? 230 : 226;
            case KeyEvent.VK_TAB: return 43;
            case KeyEvent.VK_SPACE: return 44;
            case KeyEvent.VK_ENTER: return 40;
            case KeyEvent.VK_CAPS_LOCK: return 57;
            case KeyEvent.VK_MINUS: return 45;
            case KeyEvent.VK_EQUALS: return 46;
            case KeyEvent.VK_OPEN_BRACKET: return 47;
            case KeyEvent.VK_CLOSE_BRACKET: return 48;
            case KeyEvent.VK_BACK_SLASH: return 49;
            case KeyEvent.VK_SEMICOLON: return 51;
            case KeyEvent.VK_QUOTE: return 52;
            case KeyEvent.VK_BACK_QUOTE: return 53;
            case KeyEvent.VK_COMMA: return 54;
            case KeyEvent.VK_PERIOD: return 55;
            case KeyEvent.VK_SLASH: return 56;
            case KeyEvent.VK_INSERT: return 73;
            case KeyEvent.VK_HOME: return 74;
            case KeyEvent.VK_PAGE_UP: return 75;
            case KeyEvent.VK_END: return 77;
            case KeyEvent.VK_PAGE_DOWN: return 78;
            case KeyEvent.VK_RIGHT: return 79;
            case KeyEvent.VK_LEFT: return 80;
            case KeyEvent.VK_DOWN: return 81;
            case KeyEvent.VK_UP: return 82;
            default: return 0;
        }
    }

    private static int awtTo189KeyCode(int vk, int location) {
        if (vk >= KeyEvent.VK_1 && vk <= KeyEvent.VK_9) {
            return 2 + (vk - KeyEvent.VK_1);
        }
        if (vk >= KeyEvent.VK_F1 && vk <= KeyEvent.VK_F10) {
            return 59 + (vk - KeyEvent.VK_F1);
        }
        switch (vk) {
            case KeyEvent.VK_0: return 11;
            case KeyEvent.VK_MINUS: return 12;
            case KeyEvent.VK_EQUALS: return 13;
            case KeyEvent.VK_TAB: return 15;
            case KeyEvent.VK_Q: return 16;
            case KeyEvent.VK_W: return 17;
            case KeyEvent.VK_E: return 18;
            case KeyEvent.VK_R: return 19;
            case KeyEvent.VK_T: return 20;
            case KeyEvent.VK_Y: return 21;
            case KeyEvent.VK_U: return 22;
            case KeyEvent.VK_I: return 23;
            case KeyEvent.VK_O: return 24;
            case KeyEvent.VK_P: return 25;
            case KeyEvent.VK_OPEN_BRACKET: return 26;
            case KeyEvent.VK_CLOSE_BRACKET: return 27;
            case KeyEvent.VK_ENTER: return 28;
            case KeyEvent.VK_A: return 30;
            case KeyEvent.VK_S: return 31;
            case KeyEvent.VK_D: return 32;
            case KeyEvent.VK_F: return 33;
            case KeyEvent.VK_G: return 34;
            case KeyEvent.VK_H: return 35;
            case KeyEvent.VK_J: return 36;
            case KeyEvent.VK_K: return 37;
            case KeyEvent.VK_L: return 38;
            case KeyEvent.VK_SEMICOLON: return 39;
            case KeyEvent.VK_QUOTE: return 40;
            case KeyEvent.VK_BACK_QUOTE: return 41;
            case KeyEvent.VK_BACK_SLASH: return 43;
            case KeyEvent.VK_Z: return 44;
            case KeyEvent.VK_X: return 45;
            case KeyEvent.VK_C: return 46;
            case KeyEvent.VK_V: return 47;
            case KeyEvent.VK_B: return 48;
            case KeyEvent.VK_N: return 49;
            case KeyEvent.VK_M: return 50;
            case KeyEvent.VK_COMMA: return 51;
            case KeyEvent.VK_PERIOD: return 52;
            case KeyEvent.VK_SLASH: return 53;
            case KeyEvent.VK_SHIFT: return location == KeyEvent.KEY_LOCATION_RIGHT ? 54 : 42;
            case KeyEvent.VK_CONTROL: return location == KeyEvent.KEY_LOCATION_RIGHT ? 157 : 29;
            case KeyEvent.VK_ALT: return location == KeyEvent.KEY_LOCATION_RIGHT ? 184 : 56;
            case KeyEvent.VK_SPACE: return 57;
            case KeyEvent.VK_CAPS_LOCK: return 58;
            case KeyEvent.VK_F11: return 87;
            case KeyEvent.VK_F12: return 88;
            case KeyEvent.VK_NUMPAD0: return 82;
            case KeyEvent.VK_NUMPAD1: return 79;
            case KeyEvent.VK_NUMPAD2: return 80;
            case KeyEvent.VK_NUMPAD3: return 81;
            case KeyEvent.VK_NUMPAD4: return 75;
            case KeyEvent.VK_NUMPAD5: return 76;
            case KeyEvent.VK_NUMPAD6: return 77;
            case KeyEvent.VK_NUMPAD7: return 71;
            case KeyEvent.VK_NUMPAD8: return 72;
            case KeyEvent.VK_NUMPAD9: return 73;
            case KeyEvent.VK_HOME: return 199;
            case KeyEvent.VK_UP: return 200;
            case KeyEvent.VK_PAGE_UP: return 201;
            case KeyEvent.VK_LEFT: return 203;
            case KeyEvent.VK_RIGHT: return 205;
            case KeyEvent.VK_END: return 207;
            case KeyEvent.VK_DOWN: return 208;
            case KeyEvent.VK_PAGE_DOWN: return 209;
            case KeyEvent.VK_INSERT: return 210;
            default: return 0;
        }
    }
}
