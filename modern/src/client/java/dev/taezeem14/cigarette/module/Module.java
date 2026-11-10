package dev.taezeem14.cigarette.module;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.setting.Setting;
import dev.taezeem14.cigarette.ui.hud.NotificationManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Module {

    protected static final Minecraft mc = Minecraft.getInstance();

    private final String name;
    private final String description;
    private final Category category;

    private int keyCode;
    private boolean enabled;
    private boolean hidden;
    private final List<Setting<?>> settings = new ArrayList<>();

    private float animationProgress = 0.0F;

    public Module(String name, String description, Category category, int keyCode) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keyCode = keyCode;
    }

    public Module(String name, String description, Category category) {
        this(name, description, category, 0);
    }

    protected void addSettings(Setting<?>... settings) {
        this.settings.addAll(Arrays.asList(settings));
    }

    public void toggle() {
        setState(!enabled);
    }

    public void setState(boolean state) {
        if (this.enabled == state) return;
        this.enabled = state;

        CigaretteModernClient inst = CigaretteModernClient.getInstance();
        boolean canNotify = inst != null && inst.isInitialized() && !inst.isDestructed()
                && inst.getNotificationManager() != null
                && !name.equalsIgnoreCase("ClickGUI")
                && !name.equalsIgnoreCase("SelfDestruct");

        if (state) {
            if (inst != null && !inst.isDestructed() && inst.getEventBus() != null) {
                inst.getEventBus().register(this);
            }
            onEnable();
            if (canNotify) {
                inst.getNotificationManager().push(name, "Module enabled", NotificationManager.NotificationType.SUCCESS, 1800L);
            }
        } else {
            if (inst != null && inst.getEventBus() != null) {
                inst.getEventBus().unregister(this);
            }
            onDisable();
            if (canNotify) {
                inst.getNotificationManager().push(name, "Module disabled", NotificationManager.NotificationType.INFO, 1600L);
            }
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public String getSuffix() {
        return null;
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

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public float getAnimationProgress() {
        return animationProgress;
    }

    public void setAnimationProgress(float animationProgress) {
        this.animationProgress = animationProgress;
    }
}
