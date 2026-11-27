package dev.taezeem14.cigarette.forge;

import dev.taezeem14.cigarette.Cigarette;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Standard Forge 1.8.x @Mod entry point.
 * Ensures Forge registers Cigarette V1 across 1.8–1.8.9 and initializes Forge-side event bridges.
 */
@Mod(modid = "cigarette", name = "Cigarette V1", version = Cigarette.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8,1.8.9]")
public class CigaretteMod {

    @Mod.Instance("cigarette")
    public static CigaretteMod INSTANCE;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Cigarette.INSTANCE == null) {
            new Cigarette().init();
        }
        ForgeEventBridge bridge = new ForgeEventBridge();
        MinecraftForge.EVENT_BUS.register(bridge);
        FMLCommonHandler.instance().bus().register(bridge);
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getEventBus() != null) {
            Cigarette.INSTANCE.getEventBus().register(bridge);
        }
        System.out.println("[Cigarette] Forge 1.8.x client mod integration registered successfully.");
    }
}
