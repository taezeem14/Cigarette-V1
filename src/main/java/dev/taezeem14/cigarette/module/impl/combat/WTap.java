package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.AttackEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import org.lwjgl.input.Keyboard;

public class WTap extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "Legit Key");
    private final DoubleSliderSetting delay = new DoubleSliderSetting("Delay", 30.0, 65.0, 10.0, 150.0, 1.0);
    private final NumberSetting chance = new NumberSetting("Chance", 100.0, 0.0, 100.0, 1.0);

    private final TimerUtil timer = new TimerUtil();
    private boolean awaitingReset = false;
    private int stage = 0;
    private long currentDelay = 50;

    public WTap() {
        super("WTap", "Resets sprint on attack to maximize knockback dealt", Category.COMBAT);
        addSettings(mode, delay, chance);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (mc.thePlayer == null || !mc.thePlayer.isSprinting()) return;

        if (!RandomUtil.chance(chance.getValue())) {
            return;
        }

        awaitingReset = true;
        stage = 0;
        currentDelay = (long) delay.getRandomValue();
        timer.reset();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || !awaitingReset || mc.thePlayer == null) return;

        if (stage == 0 && timer.hasTimeElapsed(currentDelay)) {
            if (mode.is("Packet")) {
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                stage = 1;
                timer.reset();
            } else if (mode.is("Legit Key")) {
                int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();
                KeyBinding.setKeyBindState(forwardKey, false);
                stage = 1;
                timer.reset();
            }
        } else if (stage == 1 && timer.hasTimeElapsed(30L)) {
            if (mode.is("Packet")) {
                mc.getNetHandler().addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
            } else if (mode.is("Legit Key")) {
                int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();
                KeyBinding.setKeyBindState(forwardKey, Keyboard.isKeyDown(forwardKey));
            }
            awaitingReset = false;
            stage = 0;
        }
    }
}
