package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.PacketEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.mapping.MappingResolver;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

public class Velocity extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Simple", "Simple", "Jump Reset", "Delay", "Intave");
    private final NumberSetting horizontal = new NumberSetting("Horizontal", 0.0, 0.0, 100.0, 1.0);
    private final NumberSetting vertical = new NumberSetting("Vertical", 0.0, 0.0, 100.0, 1.0);
    private final NumberSetting chance = new NumberSetting("Chance", 100.0, 0.0, 100.0, 1.0);
    private final BooleanSetting onlyTargeting = new BooleanSetting("Only While Targeting", false);
    private final BooleanSetting waterCheck = new BooleanSetting("Water Check", false);

    private boolean pendingVelocity = false;
    private boolean pendingIsExplosion = false;
    private int delayTicks = 0;
    private double delayedMotionX, delayedMotionY, delayedMotionZ;

    public Velocity() {
        super("Velocity", "Reduces or eliminates incoming knockback", Category.COMBAT);
        addSettings(mode, horizontal, vertical, chance, onlyTargeting, waterCheck);
    }

    @Override
    public String getSuffix() {
        if (mode.is("Simple")) {
            return horizontal.getValueInt() + "% " + vertical.getValueInt() + "%";
        }
        if (mode.is("Delay")) {
            return "Delay " + horizontal.getValueInt() + "% " + vertical.getValueInt() + "%";
        }
        return mode.getMode();
    }

    @Override
    public void onEnable() {
        resetPendingState();
    }

    @Override
    public void onDisable() {
        resetPendingState();
    }

    private void resetPendingState() {
        pendingVelocity = false;
        pendingIsExplosion = false;
        delayTicks = 0;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isIncoming() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (waterCheck.isEnabled() && mc.thePlayer.isInWater()) {
            return;
        }

        if (onlyTargeting.isEnabled() && (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null)) {
            return;
        }

        if (!RandomUtil.chance(chance.getValue())) {
            return;
        }

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() != mc.thePlayer.getEntityId()) {
                return;
            }

            if (mode.is("Simple")) {
                double h = horizontal.getValue() / 100.0;
                double v = vertical.getValue() / 100.0;

                if (h == 0.0 && v == 0.0) {
                    event.cancel();
                } else {
                    int motX = (int) (packet.getMotionX() * h);
                    int motY = (int) (packet.getMotionY() * v);
                    int motZ = (int) (packet.getMotionZ() * h);

                    MappingResolver.setFieldValue(packet, motX, S12PacketEntityVelocity.class, "motionX", "field_149415_b");
                    MappingResolver.setFieldValue(packet, motY, S12PacketEntityVelocity.class, "motionY", "field_149416_c");
                    MappingResolver.setFieldValue(packet, motZ, S12PacketEntityVelocity.class, "motionZ", "field_149414_d");
                }
            } else if (mode.is("Delay")) {
                event.cancel();
                double h = horizontal.getValue() / 100.0;
                double v = vertical.getValue() / 100.0;
                delayedMotionX = (packet.getMotionX() / 8000.0) * h;
                delayedMotionY = (packet.getMotionY() / 8000.0) * v;
                delayedMotionZ = (packet.getMotionZ() / 8000.0) * h;
                pendingIsExplosion = false;
                pendingVelocity = true;
                delayTicks = 2;
            }
        } else if (event.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
            double h = horizontal.getValue() / 100.0;
            double v = vertical.getValue() / 100.0;

            if (mode.is("Simple")) {
                if (h == 0.0 && v == 0.0) {
                    event.cancel();
                } else {
                    float motX = (float) (packet.func_149149_c() * h);
                    float motY = (float) (packet.func_149144_d() * v);
                    float motZ = (float) (packet.func_149147_e() * h);

                    MappingResolver.setFieldValue(packet, motX, S27PacketExplosion.class, "field_149152_f", "motionX");
                    MappingResolver.setFieldValue(packet, motY, S27PacketExplosion.class, "field_149153_g", "motionY");
                    MappingResolver.setFieldValue(packet, motZ, S27PacketExplosion.class, "field_149159_h", "motionZ");
                }
            } else if (mode.is("Delay")) {
                float expX = (float) (packet.func_149149_c() * h);
                float expY = (float) (packet.func_149144_d() * v);
                float expZ = (float) (packet.func_149147_e() * h);

                MappingResolver.setFieldValue(packet, 0.0F, S27PacketExplosion.class, "field_149152_f", "motionX");
                MappingResolver.setFieldValue(packet, 0.0F, S27PacketExplosion.class, "field_149153_g", "motionY");
                MappingResolver.setFieldValue(packet, 0.0F, S27PacketExplosion.class, "field_149159_h", "motionZ");

                if (h != 0.0 || v != 0.0) {
                    delayedMotionX = expX;
                    delayedMotionY = expY;
                    delayedMotionZ = expZ;
                    pendingIsExplosion = true;
                    pendingVelocity = true;
                    delayTicks = 2;
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) return;

        if (mode.is("Jump Reset") || mode.is("Intave")) {
            // Legit jump reset bypass
            if (mc.thePlayer.hurtTime == 9 && mc.thePlayer.onGround && mc.thePlayer.isSprinting()) {
                mc.thePlayer.jump();
            }
        }

        if (pendingVelocity && --delayTicks <= 0) {
            if (pendingIsExplosion) {
                mc.thePlayer.motionX += delayedMotionX;
                mc.thePlayer.motionY += delayedMotionY;
                mc.thePlayer.motionZ += delayedMotionZ;
            } else {
                mc.thePlayer.motionX = delayedMotionX;
                mc.thePlayer.motionY = delayedMotionY;
                mc.thePlayer.motionZ = delayedMotionZ;
            }
            resetPendingState();
        }
    }
}
