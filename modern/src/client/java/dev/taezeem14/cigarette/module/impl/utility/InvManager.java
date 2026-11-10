package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import dev.taezeem14.cigarette.util.player.InventoryUtil;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InvManager extends Module {

    private final DoubleSliderSetting delay = new DoubleSliderSetting("Delay", 60.0, 100.0, 20.0, 250.0, 5.0);
    private final BooleanSetting autoArmor = new BooleanSetting("Auto Armor", true);
    private final BooleanSetting dropTrash = new BooleanSetting("Drop Trash", true);
    private final BooleanSetting sortHotbar = new BooleanSetting("Sort Hotbar", true);
    private final BooleanSetting openInvOnly = new BooleanSetting("Open Inv Only", true);

    private final TimerUtil timer = new TimerUtil();
    private long currentDelay = 80;

    public InvManager() {
        super("InvManager", "Automatically cleans, equips armor, and sorts hotbar slots", Category.UTILITY);
        addSettings(delay, autoArmor, dropTrash, sortHotbar, openInvOnly);
    }

    @Override
    public String getSuffix() {
        return String.format("%.0f-%.0f", delay.getValueMin(), delay.getValueMax());
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.player == null) return;
        if (openInvOnly.isEnabled() && !(mc.gui.screen() instanceof InventoryScreen)) return;
        if (!timer.hasTimeElapsed(currentDelay)) return;

        if (autoArmor.isEnabled()) {
            for (EquipmentSlot slot : InventoryUtil.ARMOR_SLOTS) {
                int bestSlot = InventoryUtil.findBestArmorSlot(slot);
                if (bestSlot != -1) {
                    InventoryUtil.shiftClick(bestSlot);
                    currentDelay = (long) delay.getRandomValue();
                    timer.reset();
                    return;
                }
            }
        }

        if (sortHotbar.isEnabled()) {
            int bestSwordSlot = -1;
            float bestSwordDamage = -1.0F;
            for (int i = 9; i < 45; i++) {
                ItemStack stack = mc.player.inventoryMenu.slots.get(i).getItem();
                if (InventoryUtil.isSword(stack)) {
                    float score = InventoryUtil.getWeaponScore(stack);
                    if (score > bestSwordDamage) {
                        bestSwordDamage = score;
                        bestSwordSlot = i;
                    }
                }
            }
            if (bestSwordSlot != -1 && bestSwordSlot != 36) {
                InventoryUtil.swap(bestSwordSlot, 0);
                currentDelay = (long) delay.getRandomValue();
                timer.reset();
                return;
            }

            ItemStack slot1 = mc.player.inventoryMenu.slots.get(37).getItem();
            if (!InventoryUtil.isValidScaffoldBlock(slot1)) {
                int bestBlockSlot = -1;
                int bestCount = 0;
                for (int i = 9; i < 45; i++) {
                    ItemStack stack = mc.player.inventoryMenu.slots.get(i).getItem();
                    if (InventoryUtil.isValidScaffoldBlock(stack) && stack.getCount() > bestCount) {
                        bestCount = stack.getCount();
                        bestBlockSlot = i;
                    }
                }
                if (bestBlockSlot != -1 && bestBlockSlot != 37) {
                    InventoryUtil.swap(bestBlockSlot, 1);
                    currentDelay = (long) delay.getRandomValue();
                    timer.reset();
                    return;
                }
            }

            ItemStack slot2 = mc.player.inventoryMenu.slots.get(38).getItem();
            if (!slot2.has(DataComponents.FOOD)) {
                int foodSlot = -1;
                for (int i = 9; i < 45; i++) {
                    ItemStack stack = mc.player.inventoryMenu.slots.get(i).getItem();
                    if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                        foodSlot = i;
                        break;
                    }
                }
                if (foodSlot != -1 && foodSlot != 38) {
                    InventoryUtil.swap(foodSlot, 2);
                    currentDelay = (long) delay.getRandomValue();
                    timer.reset();
                    return;
                }
            }
        }

        if (dropTrash.isEnabled()) {
            for (int i = 9; i < 45; i++) {
                ItemStack stack = mc.player.inventoryMenu.slots.get(i).getItem();
                if (!stack.isEmpty() && isTrash(stack)) {
                    InventoryUtil.dropItem(i);
                    currentDelay = (long) delay.getRandomValue();
                    timer.reset();
                    return;
                }
            }
        }
    }

    private boolean isTrash(ItemStack stack) {
        if (InventoryUtil.isSword(stack)) {
            return !InventoryUtil.isBestWeapon(stack);
        }
        if (InventoryUtil.getArmorSlot(stack) != null) {
            return !InventoryUtil.isBestArmor(stack);
        }
        return stack.is(Items.FEATHER) || stack.is(Items.STICK) || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.ROTTEN_FLESH) || stack.is(Items.STRING) || stack.is(Items.POISONOUS_POTATO);
    }
}
