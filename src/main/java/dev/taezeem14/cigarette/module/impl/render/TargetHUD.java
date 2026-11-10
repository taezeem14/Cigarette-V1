package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.AttackEvent;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.blatant.KillAura;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;

public class TargetHUD extends Module {

    private static final DecimalFormat HP_FORMAT = new DecimalFormat("0.#");
    private static final DecimalFormat DIST_FORMAT = new DecimalFormat("0.0");

    private final BooleanSetting smoothAnimation = new BooleanSetting("Smooth Animation", true);
    private final BooleanSetting showArmor = new BooleanSetting("Show Armor", true);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Health", "Health", "Theme", "Rainbow");

    private float x = 200.0f;
    private float y = 200.0f;
    private final float width = 166.0f;
    private final float height = 52.0f;

    private EntityLivingBase currentTarget;
    private EntityLivingBase lastRenderedTarget;
    private long lastTargetTime = 0L;
    private float animatedHealth = 20.0f;
    private float damageTrailHealth = 20.0f;
    private float openAnimation = 0.0f;

    public TargetHUD() {
        super("TargetHUD", "Displays targeted player's avatar, health, and status", Category.RENDER);
        addSettings(smoothAnimation, showArmor, colorMode);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.getTarget() instanceof EntityLivingBase) {
            EntityLivingBase candidate = (EntityLivingBase) event.getTarget();
            if (!AntiBot.isBot(candidate) && candidate.getEntityId() != -1337) {
                currentTarget = candidate;
                lastTargetTime = System.currentTimeMillis();
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        FontUtil.ensureInitialized();
        EntityLivingBase target = getActiveTarget();

        if (target != null) {
            if (lastRenderedTarget != target) {
                float initialHp = Math.max(0.0f, Math.min(target.getMaxHealth(), target.getHealth()));
                animatedHealth = initialHp;
                damageTrailHealth = initialHp;
                lastRenderedTarget = target;
            }
            openAnimation = smoothAnimation.isEnabled() ? AnimationUtil.lerp(openAnimation, 1.0f, 15.0f) : 1.0f;
        } else {
            openAnimation = smoothAnimation.isEnabled() ? AnimationUtil.lerp(openAnimation, 0.0f, 15.0f) : 0.0f;
        }

        if (openAnimation <= 0.01f || lastRenderedTarget == null) {
            if (target == null) {
                lastRenderedTarget = null;
            }
            return;
        }

        EntityLivingBase renderTarget = lastRenderedTarget;
        float maxHp = Math.max(1.0f, renderTarget.getMaxHealth());
        float hp = Math.max(0.0f, Math.min(maxHp, renderTarget.getHealth()));

        if (smoothAnimation.isEnabled()) {
            animatedHealth = AnimationUtil.lerp(animatedHealth, hp, 14.0f);
            if (hp > damageTrailHealth) {
                damageTrailHealth = AnimationUtil.lerp(damageTrailHealth, hp, 14.0f);
            } else {
                damageTrailHealth = AnimationUtil.lerp(damageTrailHealth, hp, 3.2f);
            }
        } else {
            animatedHealth = hp;
            damageTrailHealth = hp;
        }

        float scale = smoothAnimation.isEnabled() ? (0.85f + 0.15f * AnimationUtil.easeOutCubic(openAnimation)) : 1.0f;
        float cx = x + width / 2.0f;
        float cy = y + height / 2.0f;

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(cx, cy, 0.0f);
            GlStateManager.scale(scale, scale, 1.0f);
            GlStateManager.translate(-cx, -cy, 0.0f);

            // Background & Shadow
            RenderUtil.drawDropShadow(x, y, width, height, 5.0f, 0x66000000);
            RenderUtil.drawRoundedRect(x, y, width, height, 5.0f, 0xEE141418);
            RenderUtil.drawRoundedOutline(x, y, width, height, 5.0f, 1.0f, ColorUtil.BORDER);

            // Player Avatar with hurt-time red flash
            float avatarSize = 36.0f;
            float avatarX = x + 7.0f;
            float avatarY = y + (height - avatarSize) / 2.0f;

            if (renderTarget instanceof EntityPlayer) {
                renderPlayerHead((EntityPlayer) renderTarget, avatarX, avatarY, avatarSize);
            } else {
                RenderUtil.drawRoundedRect(avatarX, avatarY, avatarSize, avatarSize, 3.0f, 0xFF222228);
            }

            float textX = avatarX + avatarSize + 8.0f;
            float rightPad = x + width - 7.0f;
            float nameY = y + 7.0f;

            // Win / Lose badge comparing effective health
            float myEffectiveHp = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
            float targetEffectiveHp = renderTarget.getHealth() + renderTarget.getAbsorptionAmount();
            String badgeText;
            int badgeColor;
            if (myEffectiveHp > targetEffectiveHp + 0.5f) {
                badgeText = "Winning";
                badgeColor = 0xFF10B981;
            } else if (targetEffectiveHp > myEffectiveHp + 0.5f) {
                badgeText = "Losing";
                badgeColor = 0xFFEF4444;
            } else {
                badgeText = "Tied";
                badgeColor = 0xFFF59E0B;
            }

            float badgeW = FontUtil.small.getStringWidth(badgeText) + 8.0f;
            float badgeH = 11.0f;
            float badgeX = rightPad - badgeW;
            float badgeY = nameY - 1.0f;
            RenderUtil.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 2.5f, ColorUtil.modifyAlpha(badgeColor, 45));
            RenderUtil.drawRoundedOutline(badgeX, badgeY, badgeW, badgeH, 2.5f, 1.0f, ColorUtil.modifyAlpha(badgeColor, 170));
            FontUtil.small.drawCenteredString(badgeText, badgeX + badgeW / 2.0f, badgeY + (badgeH - FontUtil.small.getHeight()) / 2.0f - 0.5f, badgeColor);

            // Target Name (clamped before badge)
            String displayName = trimName(renderTarget.getName(), badgeX - textX - 4.0f);
            FontUtil.bold.drawString(displayName, textX, nameY, ColorUtil.TEXT_PRIMARY);

            // Health, absorption, and distance indicator
            float hpPercent = Math.max(0.0f, Math.min(100.0f, (hp / maxHp) * 100.0f));
            float dist = mc.thePlayer.getDistanceToEntity(renderTarget);
            String statsStr = HP_FORMAT.format(hp) + " HP \u00a77(" + (int) hpPercent + "%) \u2022 " + DIST_FORMAT.format(dist) + "m";
            FontUtil.small.drawString(statsStr, textX, nameY + 11.5f, ColorUtil.TEXT_MUTED);

            // Armor icons (cleanly placed above health bar on right if enabled)
            if (showArmor.isEnabled() && renderTarget instanceof EntityPlayer) {
                renderTargetArmor((EntityPlayer) renderTarget, textX, y + height - 22.5f);
            }

            // Health Bar with Delayed Damage-Trail Bar
            float barX = textX;
            float barY = y + height - 10.0f;
            float barW = rightPad - barX;
            float barH = 4.5f;

            RenderUtil.drawRoundedRect(barX, barY, barW, barH, 2.0f, 0xFF202028);

            float trailRatio = Math.max(0.0f, Math.min(1.0f, damageTrailHealth / maxHp));
            float trailW = barW * trailRatio;
            if (trailW > 1.0f) {
                RenderUtil.drawRoundedRect(barX, barY, trailW, barH, 2.0f, 0xFFFFA726);
            }

            float fillRatio = Math.max(0.0f, Math.min(1.0f, animatedHealth / maxHp));
            float fillW = barW * fillRatio;
            if (fillW > 1.0f) {
                int barColor = getBarColor(renderTarget);
                RenderUtil.drawRoundedRect(barX, barY, fillW, barH, 2.0f, barColor);
            }
        } finally {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }
    }

    private String trimName(String name, float maxWidth) {
        if (name == null) return "";
        if (FontUtil.bold.getStringWidth(name) <= maxWidth) return name;
        String trimmed = name;
        while (trimmed.length() > 1 && FontUtil.bold.getStringWidth(trimmed + "..") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "..";
    }

    private void renderPlayerHead(EntityPlayer player, float hx, float hy, float size) {
        if (player instanceof AbstractClientPlayer) {
            mc.getTextureManager().bindTexture(((AbstractClientPlayer) player).getLocationSkin());
        } else {
            mc.getTextureManager().bindTexture(DefaultPlayerSkin.getDefaultSkin(player.getUniqueID()));
        }

        float hurtFactor = player.hurtTime > 0 ? Math.min(1.0f, player.hurtTime / 10.0f) : 0.0f;
        float shrink = hurtFactor * 1.5f;
        float drawX = hx + shrink;
        float drawY = hy + shrink;
        float drawSize = size - shrink * 2.0f;

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f - 0.55f * hurtFactor, 1.0f - 0.55f * hurtFactor, 1.0f);

        Gui.drawScaledCustomSizeModalRect((int) drawX, (int) drawY, 8.0f, 8.0f, 8, 8, (int) drawSize, (int) drawSize, 64.0f, 64.0f);
        Gui.drawScaledCustomSizeModalRect((int) drawX, (int) drawY, 40.0f, 8.0f, 8, 8, (int) drawSize, (int) drawSize, 64.0f, 64.0f);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        if (hurtFactor > 0.05f) {
            RenderUtil.drawRoundedRect(drawX, drawY, drawSize, drawSize, 2.5f, ColorUtil.modifyAlpha(0xFFFF2424, (int) (75 * hurtFactor)));
        }
        RenderUtil.drawRoundedOutline(hx, hy, size, size, 3.0f, 1.0f, hurtFactor > 0.05f ? 0xFFFF4444 : 0xFF33333C);
    }

    private void renderTargetArmor(EntityPlayer player, float ax, float ay) {
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        float itemX = ax;
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = player.inventory.armorInventory[i];
            if (stack != null) {
                GlStateManager.pushMatrix();
                GL11.glScalef(0.65f, 0.65f, 0.65f);
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) (itemX / 0.65f), (int) (ay / 0.65f));
                GlStateManager.popMatrix();
                itemX += 11.5f;
            }
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private int getBarColor(EntityLivingBase target) {
        String mode = colorMode.getMode();
        switch (mode.toLowerCase()) {
            case "theme":
                return ColorUtil.getAccent();
            case "rainbow":
                return ColorUtil.getRainbow(4, target.getEntityId() * 100);
            case "health":
            default:
                return ColorUtil.getHealthColor(target.getHealth(), target.getMaxHealth());
        }
    }

    private EntityLivingBase getActiveTarget() {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            KillAura killAura = Cigarette.INSTANCE.getModuleManager().getModuleByClass(KillAura.class);
            if (killAura != null && killAura.isEnabled() && killAura.getCurrentTarget() != null && !killAura.getCurrentTarget().isDead) {
                currentTarget = killAura.getCurrentTarget();
                lastTargetTime = System.currentTimeMillis();
                return currentTarget;
            }
        }

        if (currentTarget != null && !currentTarget.isDead && (System.currentTimeMillis() - lastTargetTime < 4000L)) {
            return currentTarget;
        }

        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            EntityLivingBase hit = (EntityLivingBase) mc.objectMouseOver.entityHit;
            if (!hit.isDead && !AntiBot.isBot(hit) && hit.getEntityId() != -1337) {
                currentTarget = hit;
                lastTargetTime = System.currentTimeMillis();
                return currentTarget;
            }
        }

        return null;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
