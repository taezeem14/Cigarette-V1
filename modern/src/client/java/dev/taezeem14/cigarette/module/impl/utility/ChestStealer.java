package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import dev.taezeem14.cigarette.util.player.InventoryUtil;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

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
        if (!event.isPre() || mc.player == null || !(mc.gui.screen() instanceof ContainerScreen chestScreen)) {
            return;
        }

        ChestMenu menu = chestScreen.getMenu();
        Container container = menu.getContainer();
        if (nameCheck.isEnabled()) {
            String title = chestScreen.getTitle().getString().toLowerCase();
            if (title.contains("menu") || title.contains("select") || title.contains("shop")
                    || title.contains("kit") || title.contains("warp") || title.contains("play")) {
                return;
            }
        }

        List<Integer> slotsToLoot = new ArrayList<>();
        int size = container.getContainerSize();
        boolean anyItemsLeft = false;

        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                anyItemsLeft = true;
                if (hasSpaceFor(stack)) {
                    slotsToLoot.add(i);
                }
            }
        }

        if (slotsToLoot.isEmpty()) {
            if (!anyItemsLeft && autoClose.isEnabled() && timer.hasTimeElapsed(currentDelay)) {
                mc.player.closeContainer();
            }
            return;
        }

        if (timer.hasTimeElapsed(currentDelay)) {
            if (randomizeOrder.isEnabled()) {
                Collections.shuffle(slotsToLoot);
            }
            InventoryUtil.clickSlot(menu.containerId, slotsToLoot.get(0), 0, ContainerInput.QUICK_MOVE);
            currentDelay = (long) delay.getRandomValue();
            timer.reset();
        }
    }

    private boolean hasSpaceFor(ItemStack candidate) {
        if (mc.player.getInventory().getFreeSlot() != -1) return true;
        return candidate.isStackable() && mc.player.getInventory().getSlotWithRemainingSpace(candidate) != -1;
    }
}
