package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Trajectories extends Module {

    private final ColorSetting color = new ColorSetting("Color", 0xFF3882F6);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 3.0, 0.1);
    private final BooleanSetting hitBox = new BooleanSetting("Landing Box", true);

    public Trajectories() {
        super("Trajectories", "Simulates and renders projectile flight paths", Category.RENDER);
        addSettings(color, lineWidth, hitBox);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) return;

        boolean isBow = stack.is(Items.BOW);
        boolean isPearl = stack.is(Items.ENDER_PEARL);
        boolean isSnowball = stack.is(Items.SNOWBALL);
        boolean isEgg = stack.is(Items.EGG);
        boolean isPotion = stack.is(Items.SPLASH_POTION);
        if (!isBow && !isPearl && !isSnowball && !isEgg && !isPotion) return;

        float pt = event.getPartialTicks();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();

        double posX = Mth.lerp(pt, mc.player.xo, mc.player.getX()) - Math.cos(Math.toRadians(yaw)) * 0.16;
        double posY = Mth.lerp(pt, mc.player.yo, mc.player.getY()) + mc.player.getEyeHeight() - 0.1;
        double posZ = Mth.lerp(pt, mc.player.zo, mc.player.getZ()) - Math.sin(Math.toRadians(yaw)) * 0.16;

        float velocity;
        float gravity;
        float drag = 0.99F;
        if (isBow) {
            float power = mc.player.isUsingItem() ? mc.player.getTicksUsingItem() / 20.0F : 1.0F;
            power = (power * power + power * 2.0F) / 3.0F;
            if (power < 0.1F) power = 1.0F;
            velocity = Math.min(1.0F, power) * 3.0F;
            gravity = 0.05F;
        } else if (isPotion) {
            velocity = 0.5F;
            gravity = 0.05F;
        } else {
            velocity = 1.5F;
            gravity = 0.03F;
        }

        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch - (isPotion ? 20.0F : 0.0F));
        double mx = -Math.sin(radYaw) * Math.cos(radPitch);
        double my = -Math.sin(radPitch);
        double mz = Math.cos(radYaw) * Math.cos(radPitch);
        double len = Math.sqrt(mx * mx + my * my + mz * mz);
        if (len < 1e-4) return;
        mx = (mx / len) * velocity;
        my = (my / len) * velocity;
        mz = (mz / len) * velocity;

        List<Vec3> points = new ArrayList<>();
        points.add(new Vec3(posX, posY, posZ));
        Vec3 landing = null;

        for (int i = 0; i < 150; i++) {
            Vec3 cur = new Vec3(posX, posY, posZ);
            Vec3 next = new Vec3(posX + mx, posY + my, posZ + mz);

            BlockHitResult blockHit = mc.level.clip(new ClipContext(cur, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (blockHit.getType() != HitResult.Type.MISS) {
                next = blockHit.getLocation();
                landing = next;
                points.add(landing);
                break;
            }

            AABB searchBox = new AABB(cur, next).inflate(1.0);
            EntityHitResult entHit = ProjectileUtil.getEntityHitResult(mc.level, mc.player, cur, next, searchBox,
                    e -> !e.isSpectator() && e.isPickable(), 0.3F);
            if (entHit != null) {
                landing = entHit.getLocation();
                points.add(landing);
                break;
            }

            posX += mx;
            posY += my;
            posZ += mz;
            points.add(new Vec3(posX, posY, posZ));
            mx *= drag;
            my *= drag;
            mz *= drag;
            my -= gravity;
        }

        int col = color.getRGB();
        float lw = lineWidth.getValueFloat();
        for (int i = 0; i < points.size() - 1; i++) {
            Gizmos.line(points.get(i), points.get(i + 1), col, lw);
        }
        if (hitBox.isEnabled() && landing != null) {
            AABB box = new AABB(landing.x - 0.25, landing.y - 0.25, landing.z - 0.25,
                    landing.x + 0.25, landing.y + 0.25, landing.z + 0.25);
            Gizmos.cuboid(box, GizmoStyle.strokeAndFill(col, 1.5F, ColorUtil.modifyAlpha(col, 70)));
        }
    }
}
