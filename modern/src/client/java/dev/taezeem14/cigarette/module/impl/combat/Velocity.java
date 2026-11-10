package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.PacketEvent;
import dev.taezeem14.cigarette.event.impl.TickEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.math.RandomUtil;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
    private double delayedX, delayedY, delayedZ;

    public Velocity() {
        super("Velocity", "Reduces or eliminates incoming knockback", Category.COMBAT);
        addSettings(mode, horizontal, vertical, chance, onlyTargeting, waterCheck);
    }

    @Override
    public String getSuffix() {
        if (mode.is("Simple")) return horizontal.getValueInt() + "% " + vertical.getValueInt() + "%";
        if (mode.is("Delay")) return "Delay " + horizontal.getValueInt() + "% " + vertical.getValueInt() + "%";
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
        if (!event.isIncoming() || mc.player == null || mc.level == null) return;
        if (waterCheck.isEnabled() && mc.player.isInWater()) return;
        if (onlyTargeting.isEnabled() && (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.ENTITY)) return;
        if (!RandomUtil.chance(chance.getValue())) return;

        double h = horizontal.getValue() / 100.0;
        double v = vertical.getValue() / 100.0;

        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet) {
            if (packet.id() != mc.player.getId()) return;
            Vec3 motion = packet.movement();
            if (mode.is("Simple")) {
                event.cancel();
                if (h != 0.0 || v != 0.0) {
                    mc.execute(() -> {
                        if (mc.player != null) {
                            mc.player.setDeltaMovement(motion.x * h, motion.y * v, motion.z * h);
                        }
                    });
                }
            } else if (mode.is("Delay")) {
                event.cancel();
                delayedX = motion.x * h;
                delayedY = motion.y * v;
                delayedZ = motion.z * h;
                pendingIsExplosion = false;
                pendingVelocity = true;
                delayTicks = 2;
            }
        } else if (event.getPacket() instanceof ClientboundExplodePacket explode && explode.playerKnockback().isPresent()) {
            Vec3 kb = explode.playerKnockback().get();
            if (mode.is("Simple")) {
                event.cancel();
                if (h != 0.0 || v != 0.0) {
                    mc.execute(() -> {
                        if (mc.player != null) {
                            mc.player.push(kb.x * h, kb.y * v, kb.z * h);
                        }
                    });
                }
            } else if (mode.is("Delay")) {
                event.cancel();
                if (h != 0.0 || v != 0.0) {
                    delayedX = kb.x * h;
                    delayedY = kb.y * v;
                    delayedZ = kb.z * h;
                    pendingIsExplosion = true;
                    pendingVelocity = true;
                    delayTicks = 2;
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!event.isPre() || mc.player == null || mc.level == null) return;

        if (mode.is("Jump Reset") || mode.is("Intave")) {
            if (mc.player.hurtTime == 9 && mc.player.onGround() && mc.player.isSprinting()) {
                mc.player.jumpFromGround();
            }
        }

        if (mode.is("Delay") && pendingVelocity) {
            delayTicks--;
            if (delayTicks <= 0) {
                if (pendingIsExplosion) {
                    mc.player.push(delayedX, delayedY, delayedZ);
                } else {
                    mc.player.setDeltaMovement(delayedX, delayedY, delayedZ);
                }
                resetPendingState();
            }
        }
    }
}
