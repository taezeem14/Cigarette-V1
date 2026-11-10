package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class NoSlow extends Module {

    private static NoSlow instance;

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Packet");
    private final BooleanSetting sword = new BooleanSetting("Sword", true);
    private final BooleanSetting bow = new BooleanSetting("Bow", true);
    private final BooleanSetting consumables = new BooleanSetting("Consumables", true);

    public NoSlow() {
        super("NoSlow", "Prevents movement slowdown while using items or blocking", Category.BLATANT);
        addSettings(mode, sword, bow, consumables);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    public static boolean shouldCancelSlowdown() {
        if (instance == null || !instance.isEnabled() || mc.thePlayer == null) {
            return false;
        }

        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return false;

        if (held.getItem() instanceof ItemSword && instance.sword.isEnabled()) {
            return true;
        }
        if (held.getItem() instanceof ItemBow && instance.bow.isEnabled()) {
            return true;
        }
        if ((held.getItem() instanceof ItemFood || held.getItem() instanceof ItemPotion) && instance.consumables.isEnabled()) {
            return true;
        }

        return false;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || !mc.thePlayer.isUsingItem() || !shouldCancelSlowdown()) {
            return;
        }

        if (mode.is("Packet")) {
            if (event.isPre()) {
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        EnumFacing.DOWN
                ));
            } else if (event.isPost()) {
                mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            }
        }
    }

    public static NoSlow getInstance() {
        return instance;
    }
}
