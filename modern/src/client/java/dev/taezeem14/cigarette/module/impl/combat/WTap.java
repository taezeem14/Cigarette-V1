package dev.taezeem14.cigarette.module.impl.combat;

import com.mojang.blaze3d.platform.InputConstants;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.AttackEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.DoubleSliderSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import dev.taezeem14.cigarette.util.math.RotationUtil;
import dev.taezeem14.cigarette.util.math.TimerUtil;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public class WTap extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Packet", "Packet", "Legit Key");
    private final DoubleSliderSetting delay = new DoubleSliderSetting("Delay", 30.0, 65.0, 10.0, 150.0, 1.0);
    private final NumberSetting chance = new NumberSetting("Chance", 100.0, 0.0, 100.0, 1.0);

    private final TimerUtil timer = new TimerUtil();
    private boolean awaitingReset = false;
    private int stage = 0;
    private long currentDelay = 50L;

    public WTap() {
        super("WTap", "Resets sprint on attack to maximize knockback dealt", Category.COMBAT);
        addSettings(mode, delay, chance);
    }

    @Override
    public String getSuffix() {
        return mode.getMode();
    }

    @Override
    public void onDisable() {
        if (awaitingReset && stage == 1 && mode.is("Legit Key") && mc.options != null) {
            InputConstants.Key fwd = KeyMappingHelper.getBoundKeyOf(mc.options.keyUp);
            KeyMapping.set(fwd, RotationUtil.isPhysicalKeyDown(mc.options.keyUp));
        }
        awaitingReset = false;
        stage = 0;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (mc.player == null || !mc.player.isSprinting()) return;
        if (!RandomUtil.chance(chance.getValue())) return;

        awaitingReset = true;
        stage = 0;
        currentDelay = (long) delay.getRandomValue();
        timer.reset();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || !awaitingReset || mc.player == null) return;

        if (stage == 0 && timer.hasTimeElapsed(currentDelay)) {
            if (mode.is("Packet") && mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                stage = 1;
                timer.reset();
            } else if (mode.is("Legit Key") && mc.options != null) {
                KeyMapping.set(KeyMappingHelper.getBoundKeyOf(mc.options.keyUp), false);
                stage = 1;
                timer.reset();
            }
        } else if (stage == 1 && timer.hasTimeElapsed(30L)) {
            if (mode.is("Packet") && mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            } else if (mode.is("Legit Key") && mc.options != null) {
                KeyMapping.set(KeyMappingHelper.getBoundKeyOf(mc.options.keyUp), RotationUtil.isPhysicalKeyDown(mc.options.keyUp));
            }
            awaitingReset = false;
            stage = 0;
        }
    }
}
