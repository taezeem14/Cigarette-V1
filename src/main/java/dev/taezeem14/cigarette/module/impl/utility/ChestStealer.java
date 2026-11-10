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
    private final BooleanSetting randomizeOrder = new BooleanSetting("Randomize Order", true);

    private final TimerUtil timer = new TimerUtil();
    private long currentDelay = 80;

    public ChestStealer() {
        super("ChestStealer", "Automatically loots chests with humanized randomized delays", Category.UTILITY);
        addSettings(delay, autoClose, nameCheck, randomizeOrder);
    }

    @Override
    public String getSuffix() {
        return String.format("%.0f-%.0f", delay.getValueMin(), delay.getValueMax());
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
            if (stack != null && hasInventorySpaceFor(stack)) {
                slotsWithItems.add(i);
            }
        }

        if (slotsWithItems.isEmpty()) {
            boolean chestCompletelyEmpty = true;
            for (int i = 0; i < chestSize; i++) {
                if (lowerChest.getStackInSlot(i) != null) {
                    chestCompletelyEmpty = false;
                    break;
                }
            }
            if (chestCompletelyEmpty && autoClose.isEnabled() && timer.hasTimeElapsed(currentDelay)) {
                mc.thePlayer.closeScreen();
            }
            return;
        }

        if (timer.hasTimeElapsed(currentDelay)) {
            if (randomizeOrder.isEnabled()) {
                Collections.shuffle(slotsWithItems);
            }
            int slotToLoot = slotsWithItems.get(0);
            InventoryUtil.clickSlot(container.windowId, slotToLoot, 0, 1); // Shift click

            currentDelay = (long) delay.getRandomValue();
            timer.reset();
        }
    }

    private boolean hasInventorySpaceFor(ItemStack candidate) {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return false;
        if (mc.thePlayer.inventory.getFirstEmptyStack() != -1) {
            return true;
        }
        if (candidate.isStackable()) {
            for (int i = 0; i < 36; i++) {
                ItemStack existing = mc.thePlayer.inventory.getStackInSlot(i);
                if (existing != null && existing.isItemEqual(candidate)
                        && ItemStack.areItemStackTagsEqual(existing, candidate)
                        && existing.stackSize < existing.getMaxStackSize()) {
                    return true;
                }
            }
        }
        return false;
    }
}
