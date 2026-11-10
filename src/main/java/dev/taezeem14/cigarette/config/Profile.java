package dev.taezeem14.cigarette.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonPrimitive;
import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.ModuleManager;
import dev.taezeem14.cigarette.setting.Setting;
import dev.taezeem14.cigarette.setting.impl.*;

public class Profile {

    private final String name;

    public Profile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("profile_name", name);

        JsonObject modulesJson = new JsonObject();
        ModuleManager moduleManager = Cigarette.INSTANCE.getModuleManager();
        if (moduleManager != null) {
            for (Module module : moduleManager.getModules()) {
                JsonObject moduleData = new JsonObject();
                moduleData.addProperty("enabled", module.isEnabled());
                moduleData.addProperty("keyCode", module.getKeyCode());

                JsonObject settingsJson = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting) {
                        settingsJson.addProperty(setting.getName(), ((BooleanSetting) setting).isEnabled());
                    } else if (setting instanceof NumberSetting) {
                        settingsJson.addProperty(setting.getName(), ((NumberSetting) setting).getValue());
                    } else if (setting instanceof DoubleSliderSetting) {
                        DoubleSliderSetting ds = (DoubleSliderSetting) setting;
                        JsonArray bounds = new JsonArray();
                        bounds.add(new JsonPrimitive(ds.getValueMin()));
                        bounds.add(new JsonPrimitive(ds.getValueMax()));
                        settingsJson.add(setting.getName(), bounds);
                    } else if (setting instanceof ModeSetting) {
                        settingsJson.addProperty(setting.getName(), ((ModeSetting) setting).getMode());
                    } else if (setting instanceof ColorSetting) {
                        ColorSetting cs = (ColorSetting) setting;
                        settingsJson.addProperty(setting.getName(), cs.getValue());
                        settingsJson.addProperty(setting.getName() + "_rainbow", cs.isRainbow());
                        settingsJson.addProperty(setting.getName() + "_speed", cs.getRainbowSpeed());
                    } else if (setting instanceof KeybindSetting) {
                        settingsJson.addProperty(setting.getName(), ((KeybindSetting) setting).getKeyCode());
                    }
                }
                moduleData.add("settings", settingsJson);
                modulesJson.add(module.getName(), moduleData);
            }
        }

        root.add("modules", modulesJson);
        return root;
    }

    public void fromJson(JsonObject root) {
        if (root == null || !root.has("modules")) return;

        JsonObject modulesJson = root.getAsJsonObject("modules");
        ModuleManager moduleManager = Cigarette.INSTANCE.getModuleManager();
        if (moduleManager == null) return;

        for (Module module : moduleManager.getModules()) {
            if (!modulesJson.has(module.getName())) continue;

            JsonObject moduleData = modulesJson.getAsJsonObject(module.getName());
            if (moduleData.has("enabled")) {
                module.setState(moduleData.get("enabled").getAsBoolean());
            }
            if (moduleData.has("keyCode")) {
                module.setKeyCode(moduleData.get("keyCode").getAsInt());
            }

            if (moduleData.has("settings")) {
                JsonObject settingsJson = moduleData.getAsJsonObject("settings");
                for (Setting<?> setting : module.getSettings()) {
                    if (!settingsJson.has(setting.getName())) continue;

                    JsonElement elem = settingsJson.get(setting.getName());
                    if (setting instanceof BooleanSetting) {
                        ((BooleanSetting) setting).setValue(elem.getAsBoolean());
                    } else if (setting instanceof NumberSetting) {
                        ((NumberSetting) setting).setValue(elem.getAsDouble());
                    } else if (setting instanceof DoubleSliderSetting) {
                        if (elem.isJsonArray()) {
                            JsonArray array = elem.getAsJsonArray();
                            if (array.size() >= 2) {
                                ((DoubleSliderSetting) setting).setValue(new double[]{
                                        array.get(0).getAsDouble(),
                                        array.get(1).getAsDouble()
                                });
                            }
                        }
                    } else if (setting instanceof ModeSetting) {
                        ((ModeSetting) setting).setMode(elem.getAsString());
                    } else if (setting instanceof ColorSetting) {
                        ColorSetting cs = (ColorSetting) setting;
                        cs.setValue(elem.getAsInt());
                        if (settingsJson.has(setting.getName() + "_rainbow")) {
                            cs.setRainbow(settingsJson.get(setting.getName() + "_rainbow").getAsBoolean());
                        }
                        if (settingsJson.has(setting.getName() + "_speed")) {
                            cs.setRainbowSpeed(settingsJson.get(setting.getName() + "_speed").getAsFloat());
                        }
                    } else if (setting instanceof KeybindSetting) {
                        ((KeybindSetting) setting).setValue(elem.getAsInt());
                    }
                }
            }
        }
    }
}
