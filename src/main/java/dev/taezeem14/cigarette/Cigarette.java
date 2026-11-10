package dev.taezeem14.cigarette;

import dev.taezeem14.cigarette.config.ConfigManager;
import dev.taezeem14.cigarette.event.EventBus;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.ModuleManager;
import dev.taezeem14.cigarette.ui.clickgui.ClickGUI;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.ui.hud.NotificationManager;
import dev.taezeem14.cigarette.util.render.BlurUtil;

public class Cigarette {

    public static final String NAME = "Cigarette";
    public static final String VERSION = "1.2.0";
    public static final String AUTHOR = "taezeem14";

    public static Cigarette INSTANCE;

    private EventBus eventBus;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private NotificationManager notificationManager;
    private ClickGUI clickGUI;
    private volatile boolean initialized = false;
    private volatile boolean destructed = false;

    public Cigarette() {
        INSTANCE = this;
    }

    public void init() {
        System.out.println("[" + NAME + "] Initializing " + NAME + " v" + VERSION + " by " + AUTHOR);

        this.destructed = false;
        this.initialized = false;
        this.eventBus = new EventBus();
        this.notificationManager = new NotificationManager();
        this.moduleManager = new ModuleManager();
        this.configManager = new ConfigManager();

        FontUtil.init();
        this.clickGUI = new ClickGUI();
        this.configManager.init();
        this.eventBus.register(this);
        this.initialized = true;

        System.out.println("[" + NAME + "] Initialization complete.");
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.isPost() && configManager != null && !destructed) {
            configManager.pollExternalSync();
        }
    }

    public void shutdown() {
        if (destructed) {
            return;
        }
        System.out.println("[" + NAME + "] Shutting down...");
        if (this.configManager != null) {
            this.configManager.save("default");
        }
    }

    public void destruct() {
        this.destructed = true;
        if (this.eventBus != null) {
            this.eventBus.clear();
        }
        if (this.notificationManager != null) {
            this.notificationManager.clear();
        }
        BlurUtil.cleanup();
        this.clickGUI = null;
    }

    public static Cigarette getInstance() {
        return INSTANCE;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public ClickGUI getClickGUI() {
        if (clickGUI == null && !destructed) {
            clickGUI = new ClickGUI();
        }
        return clickGUI;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isDestructed() {
        return destructed;
    }
}
