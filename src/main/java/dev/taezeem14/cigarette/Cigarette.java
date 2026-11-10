package dev.taezeem14.cigarette;

import dev.taezeem14.cigarette.config.ConfigManager;
import dev.taezeem14.cigarette.event.EventBus;
import dev.taezeem14.cigarette.module.ModuleManager;
import dev.taezeem14.cigarette.ui.font.FontUtil;

public class Cigarette {

    public static final String NAME = "Cigarette";
    public static final String VERSION = "1.0.0";
    public static final String AUTHOR = "taezeem14";

    public static Cigarette INSTANCE;

    private EventBus eventBus;
    private ModuleManager moduleManager;
    private ConfigManager configManager;

    public Cigarette() {
        INSTANCE = this;
    }

    public void init() {
        System.out.println("[" + NAME + "] Initializing " + NAME + " v" + VERSION + " by " + AUTHOR);

        this.eventBus = new EventBus();
        this.moduleManager = new ModuleManager();
        this.configManager = new ConfigManager();

        FontUtil.init();
        this.configManager.init();

        System.out.println("[" + NAME + "] Initialization complete.");
    }

    public void shutdown() {
        System.out.println("[" + NAME + "] Shutting down...");
        if (this.configManager != null) {
            this.configManager.save("default");
        }
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
}
