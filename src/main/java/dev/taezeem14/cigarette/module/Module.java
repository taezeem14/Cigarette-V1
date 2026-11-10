package dev.taezeem14.cigarette.module;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.setting.Setting;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Module {

    protected static final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private final String description;
    private final Category category;
    private int keyCode;
    private boolean state;
    private float animationProgress;
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category, int keyCode) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keyCode = keyCode;
        this.state = false;
        this.animationProgress = 0.0f;
    }

    public Module(String name, String description, Category category) {
        this(name, description, category, Keyboard.KEY_NONE);
    }

    public void addSettings(Setting<?>... settingsToAdd) {
        settings.addAll(Arrays.asList(settingsToAdd));
    }

    public void registerSetting(Setting<?> setting) {
        settings.add(setting);
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void toggle() {
        setState(!this.state);
    }

    public void setState(boolean state) {
        if (this.state == state) return;
        this.state = state;

        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
            if (this.state) {
                Cigarette.INSTANCE.getEventBus().register(this);
                onEnable();
            } else {
                Cigarette.INSTANCE.getEventBus().unregister(this);
                onDisable();
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public boolean isState() {
        return state;
    }

    public boolean isEnabled() {
        return state;
    }

    public float getAnimationProgress() {
        return animationProgress;
    }

    public void setAnimationProgress(float animationProgress) {
        this.animationProgress = animationProgress;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }
}
