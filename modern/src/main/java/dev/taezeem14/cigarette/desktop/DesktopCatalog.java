package dev.taezeem14.cigarette.desktop;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Authoritative pure-Java metadata catalog for all 41 Cigarette V1 modules and built-in themes.
 */
public final class DesktopCatalog {

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

    public static final class ModuleDef {
        public final String name;
        public final String description;
        public final CategoryType category;
        public final boolean defaultEnabled;

        public ModuleDef(String name, String description, CategoryType category, boolean defaultEnabled) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.defaultEnabled = defaultEnabled;
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
        // Combat (9)
        list.add(new ModuleDef("AimAssist", "GCD-quantized horizontal and vertical target smoothing", CategoryType.COMBAT, false));
        list.add(new ModuleDef("AutoClicker", "Box-Muller Gaussian randomized CPS clicker", CategoryType.COMBAT, false));
        list.add(new ModuleDef("Reach", "Closest-point entity interaction range extender", CategoryType.COMBAT, false));
        list.add(new ModuleDef("HitBoxes", "Expands entity target bounding boxes cleanly", CategoryType.COMBAT, false));
        list.add(new ModuleDef("Velocity", "Scales or delays incoming knockback packets", CategoryType.COMBAT, false));
        list.add(new ModuleDef("WTap", "Automatic sprint reset timing for combo knockback", CategoryType.COMBAT, false));
        list.add(new ModuleDef("SilentAim", "Server-side aim rotations with movement correction", CategoryType.COMBAT, false));
        list.add(new ModuleDef("ThrowPot", "Rapid hotbar splash potion recovery thrower", CategoryType.COMBAT, false));
        list.add(new ModuleDef("RodAimbot", "Lead-compensated fishing rod trajectory assist", CategoryType.COMBAT, false));

        // Blatant (10)
        list.add(new ModuleDef("KillAura", "Automated target acquisition with cooldown or APS timing", CategoryType.BLATANT, false));
        list.add(new ModuleDef("Scaffold", "Rapid bridge block placement with rotation & slot picking", CategoryType.BLATANT, false));
        list.add(new ModuleDef("Speed", "Strafe, BHop, and LowHop movement acceleration", CategoryType.BLATANT, false));
        list.add(new ModuleDef("Fly", "Creative/Vanilla flight and boost modes", CategoryType.BLATANT, false));
        list.add(new ModuleDef("Criticals", "Packet, Mini-Jump, and NoGround critical strike triggers", CategoryType.BLATANT, false));
        list.add(new ModuleDef("NoSlow", "Removes item-use and bow movement slowdown", CategoryType.BLATANT, false));
        list.add(new ModuleDef("KeepSprint", "Maintains sprint momentum after hitting targets", CategoryType.BLATANT, false));
        list.add(new ModuleDef("Blink", "Buffers and bursts outgoing movement packets", CategoryType.BLATANT, false));
        list.add(new ModuleDef("Timer", "Scales client game tick simulation speed", CategoryType.BLATANT, false));
        list.add(new ModuleDef("AntiVoid", "Pulls back before fatal void falls", CategoryType.BLATANT, false));

        // Render (10)
        list.add(new ModuleDef("ClickGUI", "Opens the in-game Vape V4 interface (Right Shift)", CategoryType.RENDER, false));
        list.add(new ModuleDef("HUD", "Renders the Cigarette watermark and active module ArrayList", CategoryType.RENDER, true));
        list.add(new ModuleDef("TargetHUD", "Displays target health, armor, and distance card", CategoryType.RENDER, false));
        list.add(new ModuleDef("ESP", "2D/3D projected bounding boxes, health, and armor bars", CategoryType.RENDER, false));
        list.add(new ModuleDef("Chams", "Highlights player silhouettes cleanly through walls", CategoryType.RENDER, false));
        list.add(new ModuleDef("Nametags", "Projects enhanced distance, health, and equipment tags", CategoryType.RENDER, false));
        list.add(new ModuleDef("Tracers", "Draws color-coded lines toward nearby players", CategoryType.RENDER, false));
        list.add(new ModuleDef("BedESP", "Locates nearby beds for fast BedWars defense awareness", CategoryType.RENDER, false));
        list.add(new ModuleDef("Fullbright", "Permanent night-vision clarity without flickering", CategoryType.RENDER, false));
        list.add(new ModuleDef("Trajectories", "Predicts landing paths for arrows, pearls, and potions", CategoryType.RENDER, false));

        // Utility (9)
        list.add(new ModuleDef("ChestStealer", "Humanized chest looting with menu-title protection", CategoryType.UTILITY, false));
        list.add(new ModuleDef("InvManager", "Sorts weapons, blocks, tools, and drops clutter", CategoryType.UTILITY, false));
        list.add(new ModuleDef("AutoTool", "Selects optimal breaking tool per block hardness", CategoryType.UTILITY, false));
        list.add(new ModuleDef("AutoArmor", "Automatically equips highest-protection armor sets", CategoryType.UTILITY, false));
        list.add(new ModuleDef("FastPlace", "Reduces right-click block and projectile placement delay", CategoryType.UTILITY, false));
        list.add(new ModuleDef("Eagle", "Automatically sneaks over block edges while bridging", CategoryType.UTILITY, false));
        list.add(new ModuleDef("BridgeAssist", "Edge alignment and smooth pitch assist for bridging", CategoryType.UTILITY, false));
        list.add(new ModuleDef("AntiBot", "Filters server NPC/watchdog decoys from targeting", CategoryType.UTILITY, false));
        list.add(new ModuleDef("Teams", "Detects teammates via scoreboard teams and name color", CategoryType.UTILITY, false));

        // Client (3)
        list.add(new ModuleDef("ArrayList", "Configures HUD module list sorting and visual tags", CategoryType.CLIENT, true));
        list.add(new ModuleDef("FriendManager", "Protects added friends from combat modules", CategoryType.CLIENT, true));
        list.add(new ModuleDef("SelfDestruct", "Tears down modules, keybinds, and overlays cleanly", CategoryType.CLIENT, false));

        MODULES = Collections.unmodifiableList(list);
    }

    private DesktopCatalog() {
    }

    public static List<ModuleDef> getModules() {
        return MODULES;
    }

    public static ModuleDef findModule(String name) {
        for (ModuleDef m : MODULES) {
            if (m.name.equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
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
}
