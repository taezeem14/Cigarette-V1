package dev.taezeem14.cigarette.util.player;

import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

public final class InventoryUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static final int HELMET = 0;
    public static final int CHESTPLATE = 1;
    public static final int LEGGINGS = 2;
    public static final int BOOTS = 3;

    private InventoryUtil() {
    }

    public static float getWeaponScore(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemSword)) {
            return 0.0f;
        }

        ItemSword sword = (ItemSword) stack.getItem();
        float damage = sword.getDamageVsEntity() + 4.0f;
        int sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack);
        int fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack);

        return damage + (sharpness * 1.25f) + (fireAspect * 0.5f);
    }

    public static float getArmorScore(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) {
            return 0.0f;
        }

        ItemArmor armor = (ItemArmor) stack.getItem();
        float reduction = armor.damageReduceAmount;
        int protection = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
        int unbreaking = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack);

        return reduction + (protection * 1.25f) + (unbreaking * 0.1f);
    }

    public static int findBestWeapon() {
        if (mc.thePlayer == null) return -1;

        int bestSlot = -1;
        float bestScore = -1.0f;

        // Search hotbar and inventory (slots 9 to 44 in inventory container, or 0 to 8 for hotbar)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            float score = getWeaponScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public static int findBestArmor(int armorType) {
        if (mc.thePlayer == null) return -1;

        int bestSlot = -1;
        float bestScore = -1.0f;

        // Check equipped armor first (3 - armorType gives the index in armorInventory: 3=helm, 2=chest, 1=legs, 0=boots)
        int equippedIndex = 3 - armorType;
        ItemStack equipped = mc.thePlayer.inventory.armorInventory[equippedIndex];
        if (equipped != null && equipped.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) equipped.getItem();
            if (armor.armorType == armorType) {
                bestScore = getArmorScore(equipped);
            }
        }

        // Scan main inventory (slots 9 to 44)
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                if (armor.armorType == armorType) {
                    float score = getArmorScore(stack);
                    if (score > bestScore) {
                        bestScore = score;
                        bestSlot = i;
                    }
                }
            }
        }

        return bestSlot;
    }

    public static void clickSlot(int windowId, int slotId, int mouseButtonClicked, int mode) {
        if (mc.thePlayer == null || mc.playerController == null) return;
        mc.playerController.windowClick(windowId, slotId, mouseButtonClicked, mode, mc.thePlayer);
    }

    public static void dropItem(int slotId) {
        clickSlot(0, slotId, 1, 4);
    }

    public static void swap(int slot, int hotbarSlot) {
        clickSlot(0, slot, hotbarSlot, 2);
    }

    public static void shiftClick(int slotId) {
        clickSlot(0, slotId, 0, 1);
    }

    public static boolean isBestWeapon(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemSword)) return false;
        float score = getWeaponScore(stack);

        for (int i = 0; i < 45; i++) {
            ItemStack other = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (other != null && other != stack && other.getItem() instanceof ItemSword) {
                if (getWeaponScore(other) > score) {
                    return false;
                }
            }
        }
        return true;
    }
}
