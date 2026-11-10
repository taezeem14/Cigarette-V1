package dev.taezeem14.cigarette.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.ModuleManager;
import dev.taezeem14.cigarette.module.impl.client.SelfDestruct;
import dev.taezeem14.cigarette.module.impl.render.ClickGUIModule;
import dev.taezeem14.cigarette.module.impl.render.HUD;
import dev.taezeem14.cigarette.module.impl.render.TargetHUD;
import dev.taezeem14.cigarette.setting.Setting;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.setting.impl.KeybindSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.ui.clickgui.CategoryFrame;
import dev.taezeem14.cigarette.ui.clickgui.ClickGUI;
import dev.taezeem14.cigarette.util.mapping.MappingResolver;
import dev.taezeem14.cigarette.util.render.ColorUtil;

import java.util.List;

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
        ModuleManager moduleManager = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getModuleManager() : null;
        if (moduleManager != null) {
            for (Module module : moduleManager.getModules()) {
                JsonObject moduleData = new JsonObject();
                boolean persistedState = !(module instanceof ClickGUIModule || module instanceof SelfDestruct) && module.isEnabled();
                moduleData.addProperty("enabled", persistedState);
                moduleData.addProperty("keyCode", module.getKeyCode());
                moduleData.addProperty("hidden", module.isHidden());

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

        JsonObject uiJson = new JsonObject();
        uiJson.addProperty("themePreset", ColorUtil.getThemePreset());
        uiJson.addProperty("accentColor", ColorUtil.getAccent());

        ClickGUI clickGUI = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getClickGUI() : null;
        if (clickGUI != null) {
            Float guiScale = MappingResolver.getFieldValue(clickGUI, ClickGUI.class, "guiScale");
            uiJson.addProperty("guiScale", guiScale != null ? guiScale : 1.0f);

            List<CategoryFrame> frames = MappingResolver.getFieldValue(clickGUI, ClickGUI.class, "frames");
            if (frames != null) {
                JsonObject framesJson = new JsonObject();
                for (CategoryFrame frame : frames) {
                    JsonObject frameJson = new JsonObject();
                    frameJson.addProperty("x", frame.getX());
                    frameJson.addProperty("y", frame.getY());
                    Boolean collapsed = MappingResolver.getFieldValue(frame, CategoryFrame.class, "collapsed");
                    frameJson.addProperty("collapsed", Boolean.TRUE.equals(collapsed));
                    frameJson.addProperty("userMoved", frame.hasUserMoved());
                    framesJson.add(frame.getCategory().name(), frameJson);
                }
                uiJson.add("frames", framesJson);
            }
        }

        if (moduleManager != null) {
            HUD hud = moduleManager.getModuleByClass(HUD.class);
            TargetHUD targetHud = moduleManager.getModuleByClass(TargetHUD.class);
            if (hud != null || targetHud != null) {
                JsonObject hudJson = new JsonObject();
                if (hud != null) {
                    hudJson.addProperty("watermarkX", hud.getWatermarkX());
                    hudJson.addProperty("watermarkY", hud.getWatermarkY());
                    Float rawArrayListX = MappingResolver.getFieldValue(hud, HUD.class, "arrayListX");
                    hudJson.addProperty("arrayListX", rawArrayListX != null ? rawArrayListX : hud.getArrayListX());
                    hudJson.addProperty("arrayListY", hud.getArrayListY());
                }
                if (targetHud != null) {
                    hudJson.addProperty("targetHudX", targetHud.getX());
                    hudJson.addProperty("targetHudY", targetHud.getY());
                }
                uiJson.add("hud", hudJson);
            }
        }

        root.add("ui", uiJson);
        return root;
    }

    public void fromJson(JsonObject root) {
        if (root == null) return;

        ModuleManager moduleManager = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getModuleManager() : null;

        if (root.has("modules") && root.get("modules").isJsonObject() && moduleManager != null) {
            JsonObject modulesJson = root.getAsJsonObject("modules");

            for (Module module : moduleManager.getModules()) {
                if (!modulesJson.has(module.getName()) || !modulesJson.get(module.getName()).isJsonObject()) {
                    continue;
                }

                JsonObject moduleData = modulesJson.getAsJsonObject(module.getName());

                // 1. Load ALL module settings first
                if (moduleData.has("settings") && moduleData.get("settings").isJsonObject()) {
                    JsonObject settingsJson = moduleData.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        if (!settingsJson.has(setting.getName())) continue;

                        JsonElement elem = settingsJson.get(setting.getName());
                        if (setting instanceof BooleanSetting && elem.isJsonPrimitive()) {
                            ((BooleanSetting) setting).setValue(elem.getAsBoolean());
                        } else if (setting instanceof NumberSetting && elem.isJsonPrimitive()) {
                            ((NumberSetting) setting).setValue(elem.getAsDouble());
                        } else if (setting instanceof DoubleSliderSetting && elem.isJsonArray()) {
                            JsonArray array = elem.getAsJsonArray();
                            if (array.size() >= 2) {
                                ((DoubleSliderSetting) setting).setValue(new double[]{
                                        array.get(0).getAsDouble(),
                                        array.get(1).getAsDouble()
                                });
                            }
                        } else if (setting instanceof ModeSetting && elem.isJsonPrimitive()) {
                            ((ModeSetting) setting).setMode(elem.getAsString());
                        } else if (setting instanceof ColorSetting && elem.isJsonPrimitive()) {
                            ColorSetting cs = (ColorSetting) setting;
                            cs.setValue(elem.getAsInt());
                            if (settingsJson.has(setting.getName() + "_rainbow") && settingsJson.get(setting.getName() + "_rainbow").isJsonPrimitive()) {
                                cs.setRainbow(settingsJson.get(setting.getName() + "_rainbow").getAsBoolean());
                            }
                            if (settingsJson.has(setting.getName() + "_speed") && settingsJson.get(setting.getName() + "_speed").isJsonPrimitive()) {
                                cs.setRainbowSpeed(settingsJson.get(setting.getName() + "_speed").getAsFloat());
                            }
                        } else if (setting instanceof KeybindSetting && elem.isJsonPrimitive()) {
                            ((KeybindSetting) setting).setValue(elem.getAsInt());
                        }
                    }
                }

                // 2. Restore visibility and keybind
                if (moduleData.has("hidden") && moduleData.get("hidden").isJsonPrimitive()) {
                    module.setHidden(moduleData.get("hidden").getAsBoolean());
                }
                if (moduleData.has("keyCode") && moduleData.get("keyCode").isJsonPrimitive()) {
                    module.setKeyCode(moduleData.get("keyCode").getAsInt());
                }

                // 3. Apply module state last; never auto-enable ClickGUI or SelfDestruct
                if (moduleData.has("enabled") && moduleData.get("enabled").isJsonPrimitive()) {
                    boolean enabled = moduleData.get("enabled").getAsBoolean();
                    if (module instanceof SelfDestruct) {
                        if (module.isEnabled()) {
                            module.setState(false);
                        }
                    } else if (!(module instanceof ClickGUIModule)) {
                        module.setState(enabled);
                    }
                }
            }
        }

        if (root.has("ui") && root.get("ui").isJsonObject()) {
            JsonObject uiJson = root.getAsJsonObject("ui");

            String preset = (uiJson.has("themePreset") && uiJson.get("themePreset").isJsonPrimitive())
                    ? uiJson.get("themePreset").getAsString()
                    : null;
            Integer accent = (uiJson.has("accentColor") && uiJson.get("accentColor").isJsonPrimitive())
                    ? uiJson.get("accentColor").getAsInt()
                    : null;

            if (preset != null) {
                if ("Custom".equalsIgnoreCase(preset)) {
                    ColorUtil.setThemePreset("Electric Blue");
                    if (accent != null) {
                        ColorUtil.setAccent(accent);
                    }
                } else {
                    ColorUtil.setThemePreset(preset);
                    if (moduleManager != null) {
                        ClickGUIModule guiModule = moduleManager.getModuleByClass(ClickGUIModule.class);
                        if (guiModule != null) {
                            guiModule.setThemePreset(preset);
                        }
                    }
                    if (accent != null && !"Rainbow".equalsIgnoreCase(preset)) {
                        ColorUtil.setAccent(accent);
                    }
                }
            } else if (accent != null) {
                ColorUtil.setAccent(accent);
            }

            if (uiJson.has("hud") && uiJson.get("hud").isJsonObject() && moduleManager != null) {
                JsonObject hudJson = uiJson.getAsJsonObject("hud");
                HUD hud = moduleManager.getModuleByClass(HUD.class);
                if (hud != null) {
                    if (hudJson.has("watermarkX") && hudJson.get("watermarkX").isJsonPrimitive()) {
                        hud.setWatermarkX(hudJson.get("watermarkX").getAsFloat());
                    }
                    if (hudJson.has("watermarkY") && hudJson.get("watermarkY").isJsonPrimitive()) {
                        hud.setWatermarkY(hudJson.get("watermarkY").getAsFloat());
                    }
                    if (hudJson.has("arrayListX") && hudJson.get("arrayListX").isJsonPrimitive()) {
                        hud.setArrayListX(hudJson.get("arrayListX").getAsFloat());
                    }
                    if (hudJson.has("arrayListY") && hudJson.get("arrayListY").isJsonPrimitive()) {
                        hud.setArrayListY(hudJson.get("arrayListY").getAsFloat());
                    }
                }

                TargetHUD targetHud = moduleManager.getModuleByClass(TargetHUD.class);
                if (targetHud != null) {
                    if (hudJson.has("targetHudX") && hudJson.get("targetHudX").isJsonPrimitive()) {
                        targetHud.setX(hudJson.get("targetHudX").getAsFloat());
                    }
                    if (hudJson.has("targetHudY") && hudJson.get("targetHudY").isJsonPrimitive()) {
                        targetHud.setY(hudJson.get("targetHudY").getAsFloat());
                    }
                }
            }

            ClickGUI clickGUI = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getClickGUI() : null;
            if (clickGUI != null) {
                if (uiJson.has("guiScale") && uiJson.get("guiScale").isJsonPrimitive()) {
                    float scale = Math.max(0.75f, Math.min(1.5f, uiJson.get("guiScale").getAsFloat()));
                    MappingResolver.setFieldValue(clickGUI, scale, ClickGUI.class, "guiScale");
                }

                if (uiJson.has("frames") && uiJson.get("frames").isJsonObject()) {
                    JsonObject framesJson = uiJson.getAsJsonObject("frames");
                    List<CategoryFrame> frames = MappingResolver.getFieldValue(clickGUI, ClickGUI.class, "frames");
                    if (frames != null) {
                        for (CategoryFrame frame : frames) {
                            String key = frame.getCategory().name();
                            if (!framesJson.has(key) || !framesJson.get(key).isJsonObject()) {
                                continue;
                            }
                            JsonObject frameJson = framesJson.getAsJsonObject(key);
                            if (frameJson.has("x") && frameJson.get("x").isJsonPrimitive()) {
                                frame.setX(frameJson.get("x").getAsFloat());
                            }
                            if (frameJson.has("y") && frameJson.get("y").isJsonPrimitive()) {
                                frame.setY(frameJson.get("y").getAsFloat());
                            }
                            if (frameJson.has("collapsed") && frameJson.get("collapsed").isJsonPrimitive()) {
                                boolean collapsed = frameJson.get("collapsed").getAsBoolean();
                                MappingResolver.setFieldValue(frame, collapsed, CategoryFrame.class, "collapsed");
                                MappingResolver.setFieldValue(frame, collapsed ? 0.0f : 1.0f, CategoryFrame.class, "collapseAnimation");
                            }
                            if (frameJson.has("userMoved") && frameJson.get("userMoved").isJsonPrimitive()) {
                                MappingResolver.setFieldValue(frame, frameJson.get("userMoved").getAsBoolean(), CategoryFrame.class, "userMoved");
                            }
                        }
                    }
                }
            }
        }
    }
}
