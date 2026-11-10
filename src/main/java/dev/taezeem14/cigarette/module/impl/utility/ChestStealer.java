package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import dev.taezeem14.cigarette.util.player.InventoryUtil;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChestStealer extends Module {

    private final DoubleSliderSetting delay = new DoubleSliderSetting("Delay", 60.0, 100.0, 20.0, 300.0, 5.0);
    private final BooleanSetting autoClose = new BooleanSetting("Auto Close", true);
    private final BooleanSetting nameCheck = new BooleanSetting("Name Check", true);

    private final TimerUtil timer = new TimerUtil();
    private long currentDelay = 80;

    public ChestStealer() {
        super("ChestStealer", "Automatically loots chests with humanized randomized delays", Category.UTILITY);
        addSettings(delay, autoClose, nameCheck);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null || !(mc.currentScreen instanceof GuiChest)) {
            return;
        }

        GuiChest guiChest = (GuiChest) mc.currentScreen;
        ContainerChest container = (ContainerChest) guiChest.inventorySlots;
        IInventory lowerChest = container.getLowerChestInventory();

        if (nameCheck.isEnabled()) {
            String title = lowerChest.getDisplayName().getUnformattedText().toLowerCase();
            if (title.contains("menu") || title.contains("select") || title.contains("shop")
                    || title.contains("kit") || title.contains("warp") || title.contains("play")) {
                return;
            }
        }

        List<Integer> slotsWithItems = new ArrayList<>();
        int chestSize = lowerChest.getSizeInventory();

        for (int i = 0; i < chestSize; i++) {
            ItemStack stack = lowerChest.getStackInSlot(i);
            if (stack != null) {
                slotsWithItems.add(i);
            }
        }

        if (slotsWithItems.isEmpty()) {
            if (autoClose.isEnabled() && timer.hasTimeElapsed(currentDelay)) {
                mc.thePlayer.closeScreen();
            }
            return;
        }

        if (timer.hasTimeElapsed(currentDelay)) {
            int slotToLoot = slotsWithItems.get(0);
            InventoryUtil.clickSlot(container.windowId, slotToLoot, 0, 1); // Shift click

            currentDelay = (long) delay.getRandomValue();
            timer.reset();
        }
    }
}
