package dev.taezeem14.cigarette.util.player;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.TntBlock;

public final class InventoryUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    public static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private InventoryUtil() {
    }

    public static boolean isSword(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ItemTags.SWORDS);
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.has(DataComponents.WEAPON);
    }

    public static boolean isValidScaffoldBlock(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return false;
        }
        Block block = ((BlockItem) stack.getItem()).getBlock();
        return !(block instanceof FallingBlock) && !(block instanceof TntBlock)
                && !block.defaultBlockState().isAir() && !block.defaultBlockState().canBeReplaced();
    }

    public static float getWeaponScore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isWeapon(stack)) {
            return 0.0F;
        }
        float damage = 1.0F;
        ItemAttributeModifiers attrs = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (attrs != null) {
            for (ItemAttributeModifiers.Entry entry : attrs.modifiers()) {
                if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                    damage += (float) entry.modifier().amount();
                }
            }
        }
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(Enchantments.SHARPNESS)) {
                damage += entry.getIntValue() * 1.25F;
            } else if (entry.getKey().is(Enchantments.FIRE_ASPECT)) {
                damage += entry.getIntValue() * 0.5F;
            }
        }
        return damage;
    }

    public static EquipmentSlot getArmorSlot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Equippable eq = stack.get(DataComponents.EQUIPPABLE);
        if (eq == null) return null;
        EquipmentSlot slot = eq.slot();
        if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
            return slot;
        }
        return null;
    }

    public static float getArmorScore(ItemStack stack) {
        if (stack == null || stack.isEmpty() || getArmorSlot(stack) == null) {
            return 0.0F;
        }
        float score = 0.0F;
        ItemAttributeModifiers attrs = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (attrs != null) {
            for (ItemAttributeModifiers.Entry entry : attrs.modifiers()) {
                if (entry.attribute().equals(Attributes.ARMOR)) {
                    score += (float) entry.modifier().amount();
                } else if (entry.attribute().equals(Attributes.ARMOR_TOUGHNESS)) {
                    score += (float) entry.modifier().amount() * 0.25F;
                }
            }
        }
        ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        for (var entry : enchants.entrySet()) {
            if (entry.getKey().is(Enchantments.PROTECTION)) {
                score += entry.getIntValue() * 1.25F;
            } else if (entry.getKey().is(Enchantments.UNBREAKING)) {
                score += entry.getIntValue() * 0.1F;
            }
        }
        return score;
    }

    public static int findBestArmorSlot(EquipmentSlot slot) {
        if (mc.player == null) return -1;
        ItemStack equipped = mc.player.getItemBySlot(slot);
        float bestScore = getArmorScore(equipped);
        int bestMenuSlot = -1;

        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.inventoryMenu.slots.get(i).getItem();
            if (!stack.isEmpty() && getArmorSlot(stack) == slot) {
                float score = getArmorScore(stack);
                if (score > bestScore) {
                    bestScore = score;
                    bestMenuSlot = i;
                }
            }
        }
        return bestMenuSlot;
    }

    public static boolean isBestWeapon(ItemStack stack) {
        if (!isSword(stack) || mc.player == null) return false;
        float score = getWeaponScore(stack);
        int selfSlot = findMenuSlotOf(stack);
        for (int i = 0; i < 45; i++) {
            ItemStack other = mc.player.inventoryMenu.slots.get(i).getItem();
            if (!other.isEmpty() && other != stack && isSword(other)) {
                float otherScore = getWeaponScore(other);
                if (otherScore > score || (otherScore == score && i < selfSlot)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isBestArmor(ItemStack stack) {
        EquipmentSlot slot = getArmorSlot(stack);
        if (slot == null || mc.player == null) return false;
        float score = getArmorScore(stack);
        ItemStack equipped = mc.player.getItemBySlot(slot);
        if (!equipped.isEmpty() && equipped != stack && getArmorScore(equipped) >= score) {
            return false;
        }
        int selfSlot = findMenuSlotOf(stack);
        for (int i = 9; i < 45; i++) {
            ItemStack other = mc.player.inventoryMenu.slots.get(i).getItem();
            if (!other.isEmpty() && other != stack && getArmorSlot(other) == slot) {
                float otherScore = getArmorScore(other);
                if (otherScore > score || (otherScore == score && i < selfSlot)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int findMenuSlotOf(ItemStack target) {
        if (mc.player == null) return Integer.MAX_VALUE;
        for (int i = 0; i < 45; i++) {
            if (mc.player.inventoryMenu.slots.get(i).getItem() == target) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    public static void clickSlot(int containerId, int slotId, int button, ContainerInput input) {
        if (mc.player == null || mc.gameMode == null) return;
        mc.gameMode.handleContainerInput(containerId, slotId, button, input, mc.player);
    }

    public static void shiftClick(int slotId) {
        if (mc.player == null) return;
        clickSlot(mc.player.inventoryMenu.containerId, slotId, 0, ContainerInput.QUICK_MOVE);
    }

    public static void swap(int slotId, int hotbarIdx) {
        if (mc.player == null) return;
        clickSlot(mc.player.inventoryMenu.containerId, slotId, hotbarIdx, ContainerInput.SWAP);
    }

    public static void dropItem(int slotId) {
        if (mc.player == null) return;
        clickSlot(mc.player.inventoryMenu.containerId, slotId, 1, ContainerInput.THROW);
    }
}
