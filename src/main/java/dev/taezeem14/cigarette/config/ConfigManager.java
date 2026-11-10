package dev.taezeem14.cigarette.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.render.HUD;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File profilesDir;
    private Profile activeProfile;
    private long lastBridgeModified = 0L;
    private long lastBridgePollMs = 0L;

    public ConfigManager() {
        File mcDir = Minecraft.getMinecraft().mcDataDir;
        if (mcDir == null) {
            mcDir = new File(".");
        }
        File baseDir = new File(mcDir, "cigarette");
        this.profilesDir = new File(baseDir, "profiles");
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
        }
    }

    public void init() {
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
        }

        File defaultFile = new File(profilesDir, "default.json");
        if (defaultFile.exists()) {
            if (!load("default")) {
                ensureDefaultHudEnabled();
            }
        } else {
            ensureDefaultHudEnabled();
            save("default");
        }
    }

    private void ensureDefaultHudEnabled() {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            Module hud = Cigarette.INSTANCE.getModuleManager().getModuleByClass(HUD.class);
            if (hud != null && !hud.isEnabled()) {
                hud.setState(true);
            }
        }
    }

    public boolean save(String profileName) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.isDestructed()) {
            return false;
        }
        if (profileName == null || profileName.trim().isEmpty()) {
            profileName = "default";
        }
        try {
            if (!profilesDir.exists()) {
                profilesDir.mkdirs();
            }
            Profile profile = new Profile(profileName);
            JsonObject json = profile.toJson();
            File file = new File(profilesDir, profileName + ".json");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
            this.activeProfile = profile;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean load(String profileName) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.isDestructed()) {
            return false;
        }
        if (profileName == null || profileName.trim().isEmpty()) {
            profileName = "default";
        }
        File file = new File(profilesDir, profileName + ".json");
        if (!file.exists()) {
            return false;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return false;
            }
            JsonObject json = parsed.getAsJsonObject();
            Profile profile = new Profile(profileName);
            profile.fromJson(json);
            this.activeProfile = profile;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(String profileName) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.isDestructed()) {
            return false;
        }
        if (profileName == null || profileName.equalsIgnoreCase("default")) {
            return false;
        }
        File file = new File(profilesDir, profileName + ".json");
        return file.exists() && file.delete();
    }

    public List<String> listProfiles() {
        if (!profilesDir.exists()) {
            return Collections.emptyList();
        }
        File[] files = profilesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        for (File file : files) {
            String name = file.getName();
            list.add(name.substring(0, name.length() - 5));
        }
        return list;
    }

    public Profile getActiveProfile() {
        return activeProfile;
    }

    public File getProfilesDir() {
        return profilesDir;
    }

    public void pollExternalSync() {
        if (Cigarette.INSTANCE == null || Cigarette.INSTANCE.isDestructed()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBridgePollMs < 600L) {
            return;
        }
        lastBridgePollMs = now;

        File bridge = new File(profilesDir, ".bridge_sync");
        if (!bridge.isFile()) {
            return;
        }
        long modified = bridge.lastModified();
        if (modified <= lastBridgeModified) {
            return;
        }
        lastBridgeModified = modified;

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(bridge), StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return;
            }
            JsonObject obj = parsed.getAsJsonObject();
            String profileName = obj.has("profile") ? obj.get("profile").getAsString() : "default";
            String action = obj.has("action") ? obj.get("action").getAsString() : "reload";

            if (!"none".equalsIgnoreCase(action)) {
                if (load(profileName) && Cigarette.INSTANCE.getNotificationManager() != null) {
                    Cigarette.INSTANCE.getNotificationManager().push(
                            "Desktop Companion",
                            "Synced profile '" + profileName + "'",
                            dev.taezeem14.cigarette.ui.hud.NotificationManager.NotificationType.INFO,
                            2000L
                    );
                }
            }
            if ("open_clickgui".equalsIgnoreCase(action) && Cigarette.INSTANCE.getModuleManager() != null) {
                dev.taezeem14.cigarette.module.impl.render.ClickGUIModule guiMod =
                        Cigarette.INSTANCE.getModuleManager().getModuleByClass(
                                dev.taezeem14.cigarette.module.impl.render.ClickGUIModule.class);
                if (guiMod != null && Minecraft.getMinecraft().theWorld != null) {
                    guiMod.setState(true);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
