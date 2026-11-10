package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public class ThrowPot extends Module {

    private final NumberSetting healthThreshold = new NumberSetting("Health Threshold", 13.0, 1.0, 20.0, 0.5);
    private final BooleanSetting speedPot = new BooleanSetting("Speed Pot", true);
    private final BooleanSetting healthPot = new BooleanSetting("Health Pot", true);
    private final NumberSetting pitch = new NumberSetting("Pitch", 89.0, 80.0, 90.0, 1.0);

    private final TimerUtil timer = new TimerUtil();
    private boolean throwing = false;
    private int originalSlot = -1;
    private int potSlot = -1;

    public ThrowPot() {
        super("ThrowPot", "Quickly throws splash potions downwards", Category.COMBAT);
        addSettings(healthThreshold, speedPot, healthPot, pitch);
    }

    @Override
    public String getSuffix() {
        return String.format("%.1f", healthThreshold.getValue());
    }

    @Override
    public void onDisable() {
        throwing = false;
        originalSlot = -1;
        potSlot = -1;
    }

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) {
            throwing = false;
            return;
        }

        if (event.isPre()) {
            if (!timer.hasTimeElapsed(500L)) {
                throwing = false;
                return;
            }
            potSlot = findPotionSlot();
            if (potSlot != -1) {
                originalSlot = mc.player.getInventory().getSelectedSlot();
                throwing = true;
                event.setPitch(pitch.getValueFloat());
            } else {
                throwing = false;
            }
        } else if (event.isPost()) {
            if (throwing && mc.getConnection() != null && mc.gameMode != null && potSlot >= 0 && potSlot < 9) {
                ItemStack potStack = mc.player.getInventory().getItem(potSlot);
                if (!potStack.isEmpty() && potStack.is(Items.SPLASH_POTION)) {
                    if (potSlot != originalSlot) {
                        mc.getConnection().send(new ServerboundSetCarriedItemPacket(potSlot));
                        mc.player.getInventory().setSelectedSlot(potSlot);
                    }
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    if (originalSlot != -1 && potSlot != originalSlot) {
                        mc.player.getInventory().setSelectedSlot(originalSlot);
                        mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
                    }
                }
                timer.reset();
            }
            throwing = false;
            originalSlot = -1;
            potSlot = -1;
        }
    }

    private int findPotionSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(Items.SPLASH_POTION)) continue;
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) continue;

            for (MobEffectInstance effect : contents.getAllEffects()) {
                if (healthPot.isEnabled() && effect.getEffect().equals(MobEffects.INSTANT_HEALTH)
                        && mc.player.getHealth() <= healthThreshold.getValue()) {
                    return i;
                }
                if (speedPot.isEnabled() && effect.getEffect().equals(MobEffects.SPEED)
                        && !mc.player.hasEffect(MobEffects.SPEED)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
