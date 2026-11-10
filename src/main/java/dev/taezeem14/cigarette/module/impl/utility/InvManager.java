package dev.taezeem14.cigarette.module.impl.utility;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import dev.taezeem14.cigarette.util.player.InventoryUtil;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

public class InvManager extends Module {

    private final DoubleSliderSetting delay = new DoubleSliderSetting("Delay", 60.0, 100.0, 20.0, 250.0, 5.0);
    private final BooleanSetting autoArmor = new BooleanSetting("Auto Armor", true);
    private final BooleanSetting dropTrash = new BooleanSetting("Drop Trash", true);
    private final BooleanSetting sortHotbar = new BooleanSetting("Sort Hotbar", true);
    private final BooleanSetting openInvOnly = new BooleanSetting("Open Inv Only", true);

    private final TimerUtil timer = new TimerUtil();
    private long currentDelay = 80;

    public InvManager() {
        super("InvManager", "Automatically cleans, sorts and equips inventory items", Category.UTILITY);
        addSettings(delay, autoArmor, dropTrash, sortHotbar, openInvOnly);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null) return;

        if (openInvOnly.isEnabled() && !(mc.currentScreen instanceof GuiInventory)) {
            return;
        }

        if (!timer.hasTimeElapsed(currentDelay)) {
            return;
        }

        // 1. Auto Armor
        if (autoArmor.isEnabled()) {
            for (int type = 0; type < 4; type++) {
                int bestSlot = InventoryUtil.findBestArmor(type);
                if (bestSlot != -1) {
                    // Equip best armor
                    InventoryUtil.shiftClick(bestSlot);
                    currentDelay = (long) delay.getRandomValue();
                    timer.reset();
                    return;
                }
            }
        }

        // 2. Sort Hotbar (Slot 0 = Sword, Slot 1 = Blocks, Slot 2 = Gapples/Pots)
        if (sortHotbar.isEnabled()) {
            int bestSwordSlot = -1;
            float bestSwordDamage = -1.0F;

            for (int i = 9; i < 45; i++) {
                ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (stack != null && stack.getItem() instanceof ItemSword) {
                    float score = InventoryUtil.getWeaponScore(stack);
                    if (score > bestSwordDamage) {
                        bestSwordDamage = score;
                        bestSwordSlot = i;
                    }
                }
            }

            if (bestSwordSlot != -1 && bestSwordSlot != 36) { // 36 is hotbar slot 0
                InventoryUtil.swap(bestSwordSlot, 0);
                currentDelay = (long) delay.getRandomValue();
                timer.reset();
                return;
            }
        }

        // 3. Drop Trash
        if (dropTrash.isEnabled()) {
            for (int i = 9; i < 45; i++) {
                ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
                if (stack != null && isTrash(stack)) {
                    InventoryUtil.dropItem(i);
                    currentDelay = (long) delay.getRandomValue();
                    timer.reset();
                    return;
                }
            }
        }
    }

    private boolean isTrash(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();

        // Check if worse sword
        if (item instanceof ItemSword) {
            return !InventoryUtil.isBestWeapon(stack);
        }

        // Check if worse armor
        if (item instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) item;
            int bestSlot = InventoryUtil.findBestArmor(armor.armorType);
            if (bestSlot != -1) {
                ItemStack best = mc.thePlayer.inventoryContainer.getSlot(bestSlot).getStack();
                if (best != null && best != stack && InventoryUtil.getArmorScore(best) > InventoryUtil.getArmorScore(stack)) {
                    return true;
                }
            }
        }

        // Junk item IDs (feathers, sticks, seeds, rotten flesh, glass bottles)
        String unlocalized = item.getUnlocalizedName().toLowerCase();
        return unlocalized.contains("feather") || unlocalized.contains("stick") || unlocalized.contains("seeds")
                || unlocalized.contains("rotten") || unlocalized.contains("string") || unlocalized.contains("flower")
                || unlocalized.contains("deadbush") || unlocalized.contains("reeds");
    }
}
