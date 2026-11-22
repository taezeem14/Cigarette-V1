package dev.taezeem14.cigarette.forge;

import dev.taezeem14.cigarette.Cigarette;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Standard Forge 1.8.9 @Mod entry point.
 * Ensures Forge registers Cigarette V1 as an official client mod rather than logging "non-mod file",
 * and initializes Forge-side event bridges.
 */
@Mod(modid = "cigarette", name = "Cigarette V1", version = Cigarette.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
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
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(bridge);
        System.out.println("[Cigarette] Forge 1.8.9 client mod integration registered successfully.");
    }
}
