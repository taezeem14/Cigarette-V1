package dev.taezeem14.cigarette.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.ModuleManager;
import dev.taezeem14.cigarette.module.impl.client.SelfDestruct;
import dev.taezeem14.cigarette.module.impl.render.ClickGUIModule;
import dev.taezeem14.cigarette.module.impl.render.HUD;
import dev.taezeem14.cigarette.module.impl.render.TargetHUD;
import dev.taezeem14.cigarette.setting.Setting;
import dev.taezeem14.cigarette.setting.impl.*;
import dev.taezeem14.cigarette.ui.clickgui.CategoryFrame;
import dev.taezeem14.cigarette.ui.clickgui.ClickGUI;
import dev.taezeem14.cigarette.util.render.ColorUtil;

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
        CigaretteModernClient inst = CigaretteModernClient.getInstance();
        ModuleManager mm = inst != null ? inst.getModuleManager() : null;
        if (mm != null) {
            for (Module module : mm.getModules()) {
                JsonObject moduleData = new JsonObject();
                boolean persisted = !(module instanceof ClickGUIModule || module instanceof SelfDestruct) && module.isEnabled();
                moduleData.addProperty("enabled", persisted);
                moduleData.addProperty("keyCode", module.getKeyCode());
                moduleData.addProperty("hidden", module.isHidden());

                JsonObject settingsJson = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof BooleanSetting bs) {
                        settingsJson.addProperty(setting.getName(), bs.isEnabled());
                    } else if (setting instanceof NumberSetting ns) {
                        settingsJson.addProperty(setting.getName(), ns.getValue());
                    } else if (setting instanceof DoubleSliderSetting ds) {
                        JsonArray arr = new JsonArray();
                        arr.add(new JsonPrimitive(ds.getValueMin()));
                        arr.add(new JsonPrimitive(ds.getValueMax()));
                        settingsJson.add(setting.getName(), arr);
                    } else if (setting instanceof ModeSetting ms) {
                        settingsJson.addProperty(setting.getName(), ms.getMode());
                    } else if (setting instanceof ColorSetting cs) {
                        settingsJson.addProperty(setting.getName(), cs.getValue());
                        settingsJson.addProperty(setting.getName() + "_rainbow", cs.isRainbow());
                        settingsJson.addProperty(setting.getName() + "_speed", cs.getRainbowSpeed());
                    } else if (setting instanceof KeybindSetting ks) {
                        settingsJson.addProperty(setting.getName(), ks.getKeyCode());
                    }
                }
                moduleData.add("settings", settingsJson);
                modulesJson.add(module.getName(), moduleData);
            }
        }
        root.add("modules", modulesJson);

        JsonObject uiJson = new JsonObject();
        uiJson.addProperty("themePreset", ColorUtil.getThemePreset());
        uiJson.addProperty("accentColor", ColorUtil.getAccent());

        ClickGUI gui = inst != null ? inst.getClickGUI() : null;
        if (gui != null) {
            uiJson.addProperty("guiScale", gui.getGuiScale());
            JsonObject framesJson = new JsonObject();
            for (CategoryFrame frame : gui.getFrames()) {
                JsonObject fj = new JsonObject();
                fj.addProperty("x", frame.getX());
                fj.addProperty("y", frame.getY());
                fj.addProperty("collapsed", frame.isCollapsed());
                fj.addProperty("userMoved", frame.hasUserMoved());
                framesJson.add(frame.getCategory().name(), fj);
            }
            uiJson.add("frames", framesJson);
        }

        if (mm != null) {
            HUD hud = mm.getModuleByClass(HUD.class);
            TargetHUD th = mm.getModuleByClass(TargetHUD.class);
            JsonObject hudJson = new JsonObject();
            if (hud != null) {
                hudJson.addProperty("watermarkX", hud.getWatermarkX());
                hudJson.addProperty("watermarkY", hud.getWatermarkY());
                hudJson.addProperty("arrayListX", hud.getRawArrayListX());
                hudJson.addProperty("arrayListY", hud.getArrayListY());
            }
            if (th != null) {
                hudJson.addProperty("targetHudX", th.getX());
                hudJson.addProperty("targetHudY", th.getY());
            }
            uiJson.add("hud", hudJson);
        }
        root.add("ui", uiJson);
        return root;
    }

    public void fromJson(JsonObject root) {
        if (root == null) return;
        CigaretteModernClient inst = CigaretteModernClient.getInstance();
        ModuleManager mm = inst != null ? inst.getModuleManager() : null;

        if (root.has("modules") && root.get("modules").isJsonObject() && mm != null) {
            JsonObject modulesJson = root.getAsJsonObject("modules");
            for (Module module : mm.getModules()) {
                if (!modulesJson.has(module.getName()) || !modulesJson.get(module.getName()).isJsonObject()) continue;
                JsonObject md = modulesJson.getAsJsonObject(module.getName());

                if (md.has("settings") && md.get("settings").isJsonObject()) {
                    JsonObject sj = md.getAsJsonObject("settings");
                    for (Setting<?> s : module.getSettings()) {
                        if (!sj.has(s.getName())) continue;
                        JsonElement el = sj.get(s.getName());
                        if (s instanceof BooleanSetting bs && el.isJsonPrimitive()) bs.setValue(el.getAsBoolean());
                        else if (s instanceof NumberSetting ns && el.isJsonPrimitive()) ns.setValue(el.getAsDouble());
                        else if (s instanceof DoubleSliderSetting ds && el.isJsonArray() && el.getAsJsonArray().size() >= 2) {
                            ds.setValue(new double[]{el.getAsJsonArray().get(0).getAsDouble(), el.getAsJsonArray().get(1).getAsDouble()});
                        } else if (s instanceof ModeSetting ms && el.isJsonPrimitive()) ms.setMode(el.getAsString());
                        else if (s instanceof ColorSetting cs && el.isJsonPrimitive()) {
                            cs.setValue(el.getAsInt());
                            if (sj.has(s.getName() + "_rainbow")) cs.setRainbow(sj.get(s.getName() + "_rainbow").getAsBoolean());
                            if (sj.has(s.getName() + "_speed")) cs.setRainbowSpeed(sj.get(s.getName() + "_speed").getAsFloat());
                        } else if (s instanceof KeybindSetting ks && el.isJsonPrimitive()) ks.setValue(el.getAsInt());
                    }
                }
                if (md.has("hidden") && md.get("hidden").isJsonPrimitive()) module.setHidden(md.get("hidden").getAsBoolean());
                if (md.has("keyCode") && md.get("keyCode").isJsonPrimitive()) module.setKeyCode(md.get("keyCode").getAsInt());
                if (md.has("enabled") && md.get("enabled").isJsonPrimitive()) {
                    boolean en = md.get("enabled").getAsBoolean();
                    if (module instanceof SelfDestruct) module.setState(false);
                    else if (!(module instanceof ClickGUIModule)) module.setState(en);
                }
            }
        }

        if (root.has("ui") && root.get("ui").isJsonObject()) {
            JsonObject ui = root.getAsJsonObject("ui");
            if (ui.has("themePreset") && ui.get("themePreset").isJsonPrimitive()) {
                String preset = ui.get("themePreset").getAsString();
                ColorUtil.setThemePreset(preset);
                if (mm != null) {
                    ClickGUIModule gm = mm.getModuleByClass(ClickGUIModule.class);
                    if (gm != null) gm.setThemePreset(preset);
                }
            }
            if (ui.has("accentColor") && ui.get("accentColor").isJsonPrimitive() && !"Rainbow".equalsIgnoreCase(ColorUtil.getThemePreset())) {
                ColorUtil.setAccent(ui.get("accentColor").getAsInt());
            }
            if (ui.has("hud") && ui.get("hud").isJsonObject() && mm != null) {
                JsonObject hj = ui.getAsJsonObject("hud");
                HUD hud = mm.getModuleByClass(HUD.class);
                if (hud != null) {
                    if (hj.has("watermarkX")) hud.setWatermarkX(hj.get("watermarkX").getAsFloat());
                    if (hj.has("watermarkY")) hud.setWatermarkY(hj.get("watermarkY").getAsFloat());
                    if (hj.has("arrayListX")) hud.setArrayListX(hj.get("arrayListX").getAsFloat());
                    if (hj.has("arrayListY")) hud.setArrayListY(hj.get("arrayListY").getAsFloat());
                }
                TargetHUD th = mm.getModuleByClass(TargetHUD.class);
                if (th != null) {
                    if (hj.has("targetHudX")) th.setX(hj.get("targetHudX").getAsFloat());
                    if (hj.has("targetHudY")) th.setY(hj.get("targetHudY").getAsFloat());
                }
            }
            ClickGUI gui = inst != null ? inst.getClickGUI() : null;
            if (gui != null) {
                if (ui.has("guiScale")) gui.setGuiScale(ui.get("guiScale").getAsFloat());
                if (ui.has("frames") && ui.get("frames").isJsonObject()) {
                    JsonObject fjMap = ui.getAsJsonObject("frames");
                    for (CategoryFrame frame : gui.getFrames()) {
                        String key = frame.getCategory().name();
                        if (!fjMap.has(key) || !fjMap.get(key).isJsonObject()) continue;
                        JsonObject fj = fjMap.getAsJsonObject(key);
                        if (fj.has("x")) frame.setX(fj.get("x").getAsFloat());
                        if (fj.has("y")) frame.setY(fj.get("y").getAsFloat());
                        if (fj.has("collapsed")) frame.setCollapsed(fj.get("collapsed").getAsBoolean());
                        if (fj.has("userMoved")) frame.setUserMoved(fj.get("userMoved").getAsBoolean());
                    }
                }
            }
        }
    }
}
