package dev.taezeem14.cigarette.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
            load("default");
        } else {
            save("default");
        }
    }

    public boolean save(String profileName) {
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
        if (profileName == null || profileName.trim().isEmpty()) {
            profileName = "default";
        }
        File file = new File(profilesDir, profileName + ".json");
        if (!file.exists()) {
            return false;
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
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
}
