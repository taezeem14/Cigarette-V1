package dev.taezeem14.cigarette.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.taezeem14.cigarette.CigaretteModernClient;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File baseDir;
    private final File profilesDir;
    private Profile currentProfile;
    private long lastBridgeModified = 0L;
    private long lastBridgePollMs = 0L;

    public ConfigManager() {
        File mcDir = Minecraft.getInstance().gameDirectory;
        this.baseDir = new File(mcDir, CigaretteModernClient.NAME);
        this.profilesDir = new File(baseDir, "profiles");
        if (!profilesDir.exists()) {
            profilesDir.mkdirs();
        }
    }

    public void init() {
        File defaultFile = new File(profilesDir, "default.json");
        if (defaultFile.exists()) {
            load("default");
        } else {
            currentProfile = new Profile("default");
            save("default");
        }
    }

    public void save(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) return;
        Profile profile = new Profile(profileName);
        this.currentProfile = profile;
        File target = new File(profilesDir, profileName + ".json");
        try (FileWriter writer = new FileWriter(target)) {
            GSON.toJson(profile.toJson(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean load(String profileName) {
        File target = new File(profilesDir, profileName + ".json");
        if (!target.exists()) return false;
        try (FileReader reader = new FileReader(target)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            Profile profile = new Profile(profileName);
            profile.fromJson(obj);
            this.currentProfile = profile;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String exportJson(String profileName) {
        return GSON.toJson(new Profile(profileName).toJson());
    }

    public boolean importJson(String profileName, String rawJson) {
        try {
            JsonObject obj = JsonParser.parseString(rawJson).getAsJsonObject();
            Profile profile = new Profile(profileName);
            profile.fromJson(obj);
            save(profileName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void delete(String profileName) {
        if ("default".equalsIgnoreCase(profileName)) return;
        File target = new File(profilesDir, profileName + ".json");
        if (target.exists()) target.delete();
    }

    public List<String> getProfileNames() {
        List<String> names = new ArrayList<>();
        File[] files = profilesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                names.add(f.getName().replace(".json", ""));
            }
        }
        if (names.isEmpty()) names.add("default");
        return names;
    }

    public Profile getCurrentProfile() {
        return currentProfile;
    }

    public void pollExternalSync() {
        CigaretteModernClient inst = CigaretteModernClient.getInstance();
        if (inst == null || inst.isDestructed()) {
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

        try (FileReader reader = new FileReader(bridge)) {
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            String profileName = obj.has("profile") ? obj.get("profile").getAsString() : "default";
            String action = obj.has("action") ? obj.get("action").getAsString() : "reload";

            if (!"none".equalsIgnoreCase(action)) {
                if (load(profileName) && inst.getNotificationManager() != null) {
                    inst.getNotificationManager().push(
                            "Desktop Companion",
                            "Synced profile '" + profileName + "'",
                            dev.taezeem14.cigarette.ui.hud.NotificationManager.NotificationType.INFO,
                            2000L
                    );
                }
            }
            if ("open_clickgui".equalsIgnoreCase(action) && inst.getModuleManager() != null) {
                dev.taezeem14.cigarette.module.impl.render.ClickGUIModule guiMod =
                        inst.getModuleManager().getModuleByClass(
                                dev.taezeem14.cigarette.module.impl.render.ClickGUIModule.class);
                if (guiMod != null && Minecraft.getInstance().player != null) {
                    guiMod.setState(true);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
