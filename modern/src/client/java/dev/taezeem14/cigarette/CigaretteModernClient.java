package dev.taezeem14.cigarette;

import dev.taezeem14.cigarette.config.ConfigManager;
import dev.taezeem14.cigarette.event.EventBus;
import dev.taezeem14.cigarette.event.EventState;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.ModuleManager;
import dev.taezeem14.cigarette.ui.clickgui.ClickGUI;
import dev.taezeem14.cigarette.ui.hud.NotificationManager;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.FrustumUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class CigaretteModernClient implements ClientModInitializer {

    public static final String NAME = "Cigarette";
    public static final String VERSION = "1.1.0";
    public static final String AUTHOR = "taezeem14";

    private static CigaretteModernClient instance;

    private EventBus eventBus;
    private NotificationManager notificationManager;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private ClickGUI clickGUI;
    private volatile boolean initialized = false;
    private volatile boolean destructed = false;

    @Override
    public void onInitializeClient() {
        instance = this;
        this.destructed = false;
        this.initialized = false;
        this.eventBus = new EventBus();
        this.notificationManager = new NotificationManager();
        this.moduleManager = new ModuleManager();
        this.configManager = new ConfigManager();
        this.clickGUI = new ClickGUI();
        this.configManager.init();
        this.initialized = true;

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!destructed && eventBus != null) {
                eventBus.post(new TickEvent(EventState.PRE));
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!destructed && eventBus != null) {
                eventBus.post(new TickEvent(EventState.POST));
            }
        });

        LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
            if (destructed || eventBus == null) return;
            AnimationUtil.updateDeltaTime();
            var camState = context.levelState().cameraRenderState;
            FrustumUtil.updateCamera(camState);
            float pt = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            eventBus.post(new Render3DEvent(camState, pt));
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("cigarette", "hud"), (graphics, deltaTracker) -> {
            if (destructed || eventBus == null) return;
            AnimationUtil.updateDeltaTime();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            float pt = deltaTracker.getGameTimeDeltaPartialTick(false);
            eventBus.post(new Render2DEvent(graphics, pt, graphics.guiWidth(), graphics.guiHeight()));
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
    }

    public void shutdown() {
        if (destructed) return;
        if (configManager != null) {
            configManager.save("default");
        }
    }

    public void destruct() {
        this.destructed = true;
        if (eventBus != null) eventBus.clear();
        if (notificationManager != null) notificationManager.clear();
        this.clickGUI = null;
    }

    public static CigaretteModernClient getInstance() { return instance; }
    public EventBus getEventBus() { return eventBus; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public ClickGUI getClickGUI() {
        if (clickGUI == null && !destructed) clickGUI = new ClickGUI();
        return clickGUI;
    }
    public boolean isInitialized() { return initialized; }
    public boolean isDestructed() { return destructed; }
}
