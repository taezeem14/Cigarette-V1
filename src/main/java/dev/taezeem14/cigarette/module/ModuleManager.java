package dev.taezeem14.cigarette.module;

import dev.taezeem14.cigarette.event.impl.KeyEvent;
import dev.taezeem14.cigarette.module.impl.blatant.*;
import dev.taezeem14.cigarette.module.impl.client.*;
import dev.taezeem14.cigarette.module.impl.combat.*;
import dev.taezeem14.cigarette.module.impl.render.*;
import dev.taezeem14.cigarette.module.impl.utility.*;
import org.lwjgl.input.Keyboard;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ModuleManager {

    private final List<Module> modules = new CopyOnWriteArrayList<>();
    private final Map<Class<? extends Module>, Module> moduleClassMap = new ConcurrentHashMap<>();
    private final Map<String, Module> moduleNameMap = new ConcurrentHashMap<>();

    public ModuleManager() {
        // Combat
        register(new AimAssist());
        register(new AutoClicker());
        register(new Reach());
        register(new HitBoxes());
        register(new Velocity());
        register(new WTap());
        register(new SilentAim());
        register(new ThrowPot());
        register(new RodAimbot());

        // Blatant
        register(new KillAura());
        register(new Scaffold());
        register(new Speed());
        register(new Fly());
        register(new Criticals());
        register(new NoSlow());
        register(new KeepSprint());
        register(new Blink());
        register(new Timer());
        register(new AntiVoid());

        // Utility & World
        register(new Eagle());
        register(new FastPlace());
        register(new ChestStealer());
        register(new InvManager());
        register(new AutoTool());
        register(new AutoArmor());
        register(new BridgeAssist());
        register(new AntiBot());
        register(new Teams());

        // Client
        register(new SelfDestruct());
        register(new FriendManager());
        register(new ArrayListModule());

        // Render
        register(new ClickGUIModule());
        register(new HUD());
        register(new ESP());
        register(new Chams());
        register(new Nametags());
        register(new Tracers());
        register(new BedESP());
        register(new TargetHUD());
        register(new Fullbright());
        register(new Trajectories());
    }

    public void register(Module module) {
        if (module == null) return;
        modules.add(module);
        moduleClassMap.put(module.getClass(), module);
        moduleNameMap.put(module.getName().toLowerCase(), module);
    }

    public void registerModules(Module... modulesToRegister) {
        for (Module module : modulesToRegister) {
            register(module);
        }
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }

    public Module getModuleByName(String name) {
        if (name == null) return null;
        return moduleNameMap.get(name.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModuleByClass(Class<T> clazz) {
        return (T) moduleClassMap.get(clazz);
    }

    public void onKeyPressed(int keyCode) {
        if (keyCode <= Keyboard.KEY_NONE || keyCode >= 256) return;

        for (Module module : modules) {
            if (module.getKeyCode() == keyCode) {
                module.toggle();
            }
        }
    }
}
