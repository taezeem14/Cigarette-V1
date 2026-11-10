package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.AttackEvent;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.blatant.KillAura;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.text.DecimalFormat;

public class TargetHUD extends Module {

    private static final DecimalFormat HP_FMT = new DecimalFormat("0.#");
    private static final DecimalFormat DIST_FMT = new DecimalFormat("0.0");

    private final BooleanSetting smoothAnimation = new BooleanSetting("Smooth Animation", true);
    private final BooleanSetting showArmor = new BooleanSetting("Show Armor", true);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Health", "Health", "Theme", "Rainbow");

    private float x = 200.0F;
    private float y = 200.0F;
    private final float width = 166.0F;
    private final float height = 52.0F;

    private LivingEntity currentTarget;
    private LivingEntity lastRenderedTarget;
    private long lastTargetTime = 0L;
    private float animatedHealth = 20.0F;
    private float damageTrailHealth = 20.0F;
    private float openAnimation = 0.0F;

    public TargetHUD() {
        super("TargetHUD", "Displays targeted entity avatar, health, armor, and status", Category.RENDER);
        addSettings(smoothAnimation, showArmor, colorMode);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.getTarget() instanceof LivingEntity living && !AntiBot.isBot(living)) {
            currentTarget = living;
            lastTargetTime = System.currentTimeMillis();
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.level == null || mc.player == null) return;

        LivingEntity target = getActiveTarget();
        if (target != null) {
            if (lastRenderedTarget != target) {
                float initHp = Math.max(0.0F, Math.min(target.getMaxHealth(), target.getHealth()));
                animatedHealth = initHp;
                damageTrailHealth = initHp;
                lastRenderedTarget = target;
            }
            openAnimation = smoothAnimation.isEnabled() ? AnimationUtil.lerp(openAnimation, 1.0F, 15.0F) : 1.0F;
        } else {
            openAnimation = smoothAnimation.isEnabled() ? AnimationUtil.lerp(openAnimation, 0.0F, 15.0F) : 0.0F;
        }

        if (openAnimation <= 0.01F || lastRenderedTarget == null) {
            if (target == null) lastRenderedTarget = null;
            return;
        }

        LivingEntity renderTarget = lastRenderedTarget;
        float maxHp = Math.max(1.0F, renderTarget.getMaxHealth());
        float hp = Math.max(0.0F, Math.min(maxHp, renderTarget.getHealth()));
        if (smoothAnimation.isEnabled()) {
            animatedHealth = AnimationUtil.lerp(animatedHealth, hp, 14.0F);
            damageTrailHealth = hp > damageTrailHealth
                    ? AnimationUtil.lerp(damageTrailHealth, hp, 14.0F)
                    : AnimationUtil.lerp(damageTrailHealth, hp, 3.2F);
        } else {
            animatedHealth = hp;
            damageTrailHealth = hp;
        }

        float scale = smoothAnimation.isEnabled() ? (0.85F + 0.15F * AnimationUtil.easeOutCubic(openAnimation)) : 1.0F;
        float cx = x + width / 2.0F;
        float cy = y + height / 2.0F;
        GuiGraphicsExtractor g = event.getGraphics();

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(scale, scale);
        g.pose().translate(-cx, -cy);

        RenderUtil.drawDropShadow(g, x, y, width, height, 5.0F, 0x66000000);
        RenderUtil.drawRoundedRect(g, x, y, width, height, 5.0F, 0xEE141418);
        RenderUtil.drawRoundedOutline(g, x, y, width, height, 5.0F, 1.0F, ColorUtil.BORDER);

        float avatarSize = 36.0F;
        float avatarX = x + 7.0F;
        float avatarY = y + (height - avatarSize) / 2.0F;
        if (renderTarget instanceof AbstractClientPlayer clientPlayer) {
            PlayerFaceExtractor.extractRenderState(g, clientPlayer.getSkin(), Math.round(avatarX), Math.round(avatarY), Math.round(avatarSize));
            if (renderTarget.hurtTime > 0) {
                int hurtAlpha = Math.min(130, renderTarget.hurtTime * 15);
                RenderUtil.drawRoundedRect(g, avatarX, avatarY, avatarSize, avatarSize, 2.0F, hurtAlpha << 24 | 0xFF2222);
            }
        } else {
            RenderUtil.drawRoundedRect(g, avatarX, avatarY, avatarSize, avatarSize, 3.0F, 0xFF222228);
        }

        float textX = avatarX + avatarSize + 8.0F;
        float rightPad = x + width - 7.0F;
        float nameY = y + 7.0F;

        float myHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float targetEff = renderTarget.getHealth() + renderTarget.getAbsorptionAmount();
        String badge = myHp > targetEff + 0.5F ? "Winning" : (targetEff > myHp + 0.5F ? "Losing" : "Tied");
        int badgeCol = myHp > targetEff + 0.5F ? 0xFF10B981 : (targetEff > myHp + 0.5F ? 0xFFEF4444 : 0xFFF59E0B);

        float badgeW = RenderUtil.getSmallStringWidth(badge) + 8.0F;
        float badgeH = 11.0F;
        float badgeX = rightPad - badgeW;
        float badgeY = nameY - 1.0F;
        RenderUtil.drawRoundedRect(g, badgeX, badgeY, badgeW, badgeH, 2.5F, ColorUtil.modifyAlpha(badgeCol, 45));
        RenderUtil.drawRoundedOutline(g, badgeX, badgeY, badgeW, badgeH, 2.5F, 1.0F, ColorUtil.modifyAlpha(badgeCol, 170));
        RenderUtil.drawCenteredSmallText(g, badge, badgeX + badgeW / 2.0F, badgeY + (badgeH - RenderUtil.getSmallFontHeight()) / 2.0F + 0.5F, badgeCol, false);

        String name = renderTarget.getName().getString();
        float maxNameW = badgeX - textX - 4.0F;
        while (RenderUtil.getStringWidth(name) > maxNameW && name.length() > 3) {
            name = name.substring(0, name.length() - 2) + "...";
        }
        RenderUtil.drawText(g, name, textX, nameY, ColorUtil.TEXT_PRIMARY, true);

        float hpPct = Math.min(100.0F, Math.max(0.0F, (hp / maxHp) * 100.0F));
        String stats = HP_FMT.format(hp) + " HP (" + Math.round(hpPct) + "%) \u2022 " + DIST_FMT.format(mc.player.distanceTo(renderTarget)) + "m";
        RenderUtil.drawSmallText(g, stats, textX, y + 19.0F, ColorUtil.TEXT_MUTED, false);

        if (showArmor.isEnabled() && renderTarget instanceof Player playerTarget) {
            float itemX = textX - 1.0F;
            float itemY = y + 26.0F;
            EquipmentSlot[] gear = {EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (EquipmentSlot slot : gear) {
                ItemStack stack = playerTarget.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    g.pose().pushMatrix();
                    g.pose().translate(itemX, itemY);
                    g.pose().scale(0.68F, 0.68F);
                    g.item(stack, 0, 0);
                    g.pose().popMatrix();
                    itemX += 12.0F;
                }
            }
        }

        float barX = textX;
        float barY = y + height - 10.0F;
        float barW = rightPad - barX;
        float barH = 4.5F;
        RenderUtil.drawRoundedRect(g, barX, barY, barW, barH, 2.0F, 0xFF202028);

        float trailFill = Math.max(0.0F, Math.min(1.0F, damageTrailHealth / maxHp)) * barW;
        if (trailFill > 0.5F) {
            RenderUtil.drawRoundedRect(g, barX, barY, trailFill, barH, 2.0F, 0xFFFFA726);
        }
        float hpFill = Math.max(0.0F, Math.min(1.0F, animatedHealth / maxHp)) * barW;
        if (hpFill > 0.5F) {
            int barCol = resolveHealthBarColor(hp, maxHp);
            RenderUtil.drawRoundedRect(g, barX, barY, hpFill, barH, 2.0F, barCol);
        }

        g.pose().popMatrix();
    }

    private int resolveHealthBarColor(float hp, float maxHp) {
        if (colorMode.is("Rainbow")) return ColorUtil.getRainbow(4, 0);
        if (colorMode.is("Theme")) return ColorUtil.getAccent();
        return ColorUtil.getHealthColor(hp, maxHp);
    }

    private LivingEntity getActiveTarget() {
        KillAura ka = CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getModuleManager() != null
                ? CigaretteModernClient.getInstance().getModuleManager().getModuleByClass(KillAura.class) : null;
        if (ka != null && ka.isEnabled() && ka.getCurrentTarget() != null) {
            currentTarget = ka.getCurrentTarget();
            lastTargetTime = System.currentTimeMillis();
            return currentTarget;
        }
        if (currentTarget != null && currentTarget.isAlive() && (System.currentTimeMillis() - lastTargetTime < 4000L)) {
            return currentTarget;
        }
        if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity hit
                && hit.isAlive() && !AntiBot.isBot(hit)) {
            currentTarget = hit;
            lastTargetTime = System.currentTimeMillis();
            return currentTarget;
        }
        return null;
    }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
}
