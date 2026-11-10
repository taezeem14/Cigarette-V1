package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.AttackEvent;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;

public class TargetHUD extends Module {

    private static final DecimalFormat HP_FORMAT = new DecimalFormat("0.#");

    private final BooleanSetting smoothAnimation = new BooleanSetting("Smooth Animation", true);
    private final BooleanSetting showArmor = new BooleanSetting("Show Armor", true);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Health", "Health", "Theme", "Rainbow");

    private float x = 200.0f;
    private float y = 200.0f;
    private final float width = 150.0f;
    private final float height = 45.0f;

    private EntityLivingBase currentTarget;
    private long lastTargetTime = 0L;
    private float animatedHealth = 20.0f;

    public TargetHUD() {
        super("TargetHUD", "Displays targeted player's avatar, health, and status", Category.RENDER);
        addSettings(smoothAnimation, showArmor, colorMode);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.getTarget() instanceof EntityLivingBase) {
            currentTarget = (EntityLivingBase) event.getTarget();
            lastTargetTime = System.currentTimeMillis();
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        EntityLivingBase target = getActiveTarget();
        if (target == null) return;

        float maxHp = target.getMaxHealth();
        float hp = Math.max(0.0f, Math.min(maxHp, target.getHealth()));

        if (smoothAnimation.isEnabled()) {
            animatedHealth += (hp - animatedHealth) * 0.2f;
        } else {
            animatedHealth = hp;
        }

        // Background & Shadow
        RenderUtil.drawDropShadow(x, y, width, height, 4.0f, 0x66000000);
        RenderUtil.drawRoundedRect(x, y, width, height, 4.0f, ColorUtil.BACKGROUND);
        RenderUtil.drawRoundedOutline(x, y, width, height, 4.0f, 1.0f, 0xFF25252C);

        // Player Avatar
        float avatarSize = 32.0f;
        float avatarX = x + 6.0f;
        float avatarY = y + (height - avatarSize) / 2.0f;

        if (target instanceof EntityPlayer) {
            renderPlayerHead((EntityPlayer) target, avatarX, avatarY, avatarSize);
        } else {
            RenderUtil.drawRoundedRect(avatarX, avatarY, avatarSize, avatarSize, 3.0f, 0xFF222228);
        }

        // Target Name
        float textX = avatarX + avatarSize + 8.0f;
        float nameY = y + 6.0f;
        FontUtil.bold.drawString(target.getName(), textX, nameY, ColorUtil.TEXT_PRIMARY);

        // Health text & percentage
        float hpPercent = (hp / maxHp) * 100.0f;
        String hpStr = HP_FORMAT.format(hp) + " HP §7(" + (int) hpPercent + "%)";
        FontUtil.small.drawString(hpStr, textX, nameY + 11.0f, ColorUtil.TEXT_MUTED);

        // Health Bar
        float barX = textX;
        float barY = nameY + 22.0f;
        float barW = width - (textX - x) - 8.0f;
        float barH = 5.0f;

        RenderUtil.drawRoundedRect(barX, barY, barW, barH, 2.0f, 0xFF222228);

        float fillRatio = Math.max(0.0f, Math.min(1.0f, animatedHealth / maxHp));
        float fillW = barW * fillRatio;

        if (fillW > 1.0f) {
            int barColor = getBarColor(target);
            RenderUtil.drawRoundedRect(barX, barY, fillW, barH, 2.0f, barColor);
        }

        // Render Armor
        if (showArmor.isEnabled() && target instanceof EntityPlayer) {
            renderTargetArmor((EntityPlayer) target, x + width - 58.0f, y + 4.0f);
        }
    }

    private void renderPlayerHead(EntityPlayer player, float x, float y, float size) {
        if (player instanceof AbstractClientPlayer) {
            mc.getTextureManager().bindTexture(((AbstractClientPlayer) player).getLocationSkin());
        } else {
            mc.getTextureManager().bindTexture(net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(player.getUniqueID()));
        }
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableBlend();

        // 8x8 base head texture in 64x64 skin
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 8.0f, 8.0f, 8, 8, (int) size, (int) size, 64.0f, 64.0f);
        // Hat layer
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 40.0f, 8.0f, 8, 8, (int) size, (int) size, 64.0f, 64.0f);

        RenderUtil.drawRoundedOutline(x, y, size, size, 3.0f, 1.0f, 0xFF333338);
    }

    private void renderTargetArmor(EntityPlayer player, float x, float y) {
        RenderHelper.enableGUIStandardItemLighting();
        float itemX = x;
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = player.inventory.armorInventory[i];
            if (stack != null) {
                GL11.glPushMatrix();
                GL11.glScalef(0.7f, 0.7f, 0.7f);
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) (itemX / 0.7f), (int) (y / 0.7f));
                GL11.glPopMatrix();
                itemX += 13.0f;
            }
        }
        RenderHelper.disableStandardItemLighting();
    }

    private int getBarColor(EntityLivingBase target) {
        String mode = colorMode.getMode();
        switch (mode.toLowerCase()) {
            case "theme":
                return ColorUtil.ACCENT;
            case "rainbow":
                return ColorUtil.getRainbow(4, target.getEntityId() * 100);
            case "health":
            default:
                return ColorUtil.getHealthColor(target.getHealth(), target.getMaxHealth());
        }
    }

    private EntityLivingBase getActiveTarget() {
        if (currentTarget != null && !currentTarget.isDead && (System.currentTimeMillis() - lastTargetTime < 4000L)) {
            return currentTarget;
        }

        if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
            currentTarget = (EntityLivingBase) mc.objectMouseOver.entityHit;
            lastTargetTime = System.currentTimeMillis();
            return currentTarget;
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
}
