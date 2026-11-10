package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.MotionEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.List;

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

    @EventTarget
    public void onMotion(MotionEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        if (event.isPre()) {
            if (!timer.hasTimeElapsed(500L)) return;

            potSlot = findPotionSlot();
            if (potSlot != -1) {
                originalSlot = mc.thePlayer.inventory.currentItem;
                throwing = true;
                event.setPitch(pitch.getValueFloat());

                mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(potSlot));
                ItemStack potStack = mc.thePlayer.inventory.getStackInSlot(potSlot);
                mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(potStack));
            }
        } else if (event.isPost()) {
            if (throwing) {
                if (originalSlot != -1) {
                    mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(originalSlot));
                }
                throwing = false;
                timer.reset();
            }
        }
    }

    private int findPotionSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null || !(stack.getItem() instanceof ItemPotion)) continue;

            ItemPotion potionItem = (ItemPotion) stack.getItem();
            if (!ItemPotion.isSplash(stack.getMetadata())) continue;

            List<PotionEffect> effects = potionItem.getEffects(stack);
            if (effects == null) continue;

            for (PotionEffect effect : effects) {
                if (healthPot.isEnabled() && effect.getPotionID() == Potion.heal.id) {
                    if (mc.thePlayer.getHealth() <= healthThreshold.getValue()) {
                        return i;
                    }
                }
                if (speedPot.isEnabled() && effect.getPotionID() == Potion.moveSpeed.id) {
                    if (!mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }
}
