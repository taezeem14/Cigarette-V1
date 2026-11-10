package dev.taezeem14.cigarette.module;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.module.impl.blatant.*;
import dev.taezeem14.cigarette.module.impl.client.*;
import dev.taezeem14.cigarette.module.impl.combat.*;
import dev.taezeem14.cigarette.module.impl.render.*;
import dev.taezeem14.cigarette.module.impl.utility.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();

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

        // Blatant & Movement
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

        // Render & Visuals
        register(new ClickGUIModule());
        register(new HUD());
        register(new TargetHUD());
        register(new ESP());
        register(new Chams());
        register(new Nametags());
        register(new Tracers());
        register(new BedESP());
        register(new Fullbright());
        register(new Trajectories());

        // Utility & World
        register(new ChestStealer());
        register(new InvManager());
        register(new AutoTool());
        register(new AutoArmor());
        register(new FastPlace());
        register(new Eagle());
        register(new BridgeAssist());
        register(new AntiBot());
        register(new Teams());

        // Client
        register(new ArrayListModule());
        register(new FriendManager());
        register(new SelfDestruct());
    }

    private void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module getModuleByName(String name) {
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name)) return m;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModuleByClass(Class<T> clazz) {
        for (Module m : modules) {
            if (m.getClass() == clazz) return (T) m;
        }
        return null;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }

    public List<Module> getEnabledModules() {
        return modules.stream().filter(Module::isEnabled).collect(Collectors.toList());
    }

    public void onKeyPressed(int keyCode) {
        if (keyCode <= 0) return;
        if (CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().isDestructed()) {
            return;
        }
        for (Module m : modules) {
            if (m.getKeyCode() == keyCode) {
                m.toggle();
            }
        }
    }
}
