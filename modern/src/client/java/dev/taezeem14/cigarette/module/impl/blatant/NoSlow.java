package dev.taezeem14.cigarette.module.impl.blatant;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class NoSlow extends Module {

    private static NoSlow instance;

    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Vanilla", "Packet");
    private final BooleanSetting shield = new BooleanSetting("Shield", true);
    private final BooleanSetting bow = new BooleanSetting("Bow / Crossbow", true);
    private final BooleanSetting consumables = new BooleanSetting("Consumables", true);

    public NoSlow() {
        super("NoSlow", "Prevents movement slowdown while using items or blocking", Category.BLATANT);
        addSettings(mode, shield, bow, consumables);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    public static boolean shouldCancelSlowdown() {
        if (instance == null || !instance.isEnabled() || mc.player == null) {
            return false;
        }
        ItemStack active = mc.player.getUseItem();
        if (active.isEmpty()) return false;

        if (active.is(Items.SHIELD) && instance.shield.isEnabled()) return true;
        if ((active.is(Items.BOW) || active.is(Items.CROSSBOW)) && instance.bow.isEnabled()) return true;
        return (active.has(DataComponents.FOOD) || active.has(DataComponents.CONSUMABLE)) && instance.consumables.isEnabled();
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.getConnection() == null || !mc.player.isUsingItem() || !shouldCancelSlowdown()) {
            return;
        }
        if (mode.is("Packet")) {
            if (event.isPre()) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                        BlockPos.ZERO,
                        Direction.DOWN
                ));
            } else if (event.isPost()) {
                InteractionHand hand = mc.player.getUsedItemHand();
                mc.getConnection().send(new ServerboundUseItemPacket(hand, 0, mc.player.getYRot(), mc.player.getXRot()));
            }
        }
    }
}
