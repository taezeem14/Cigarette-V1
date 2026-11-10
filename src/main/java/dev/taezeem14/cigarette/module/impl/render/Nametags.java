package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Nametags extends Module {

    private static final DecimalFormat HP_FORMAT = new DecimalFormat("0.#");

    private final BooleanSetting showDistance = new BooleanSetting("Distance", true);
    private final BooleanSetting showHealth = new BooleanSetting("Health", true);
    private final BooleanSetting showItems = new BooleanSetting("Armor & Items", true);
    private final NumberSetting scaleFactor = new NumberSetting("Scale", 1.8, 1.0, 3.5, 0.1);

    public Nametags() {
        super("Nametags", "Distance-scaled custom 2D nametags with gear and health", Category.RENDER);
        addSettings(showDistance, showHealth, showItems, scaleFactor);
    }

    public static boolean shouldCancelVanillaNametag(Entity entity) {
        if (!(entity instanceof EntityPlayer) || entity == mc.thePlayer) return false;
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            Nametags nametags = Cigarette.INSTANCE.getModuleManager().getModuleByClass(Nametags.class);
            return nametags != null && nametags.isEnabled();
        }
        return false;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        FontUtil.ensureInitialized();
        RenderManager rm = mc.getRenderManager();
        float partialTicks = event.getPartialTicks();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead) continue;
            if (player.getEntityId() == -1337 || AntiBot.isBot(player)) continue;

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - rm.viewerPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - rm.viewerPosY + player.height + 0.55;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - rm.viewerPosZ;

            float distance = mc.thePlayer.getDistanceToEntity(player);
            float scale = Math.max(distance / 50.0f, 1.0f) * (scaleFactor.getValueFloat() * 0.015f);

            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(x, y, z);
                GL11.glNormal3f(0.0f, 1.0f, 0.0f);
                GlStateManager.rotate(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
                GlStateManager.rotate(rm.playerViewX, 1.0f, 0.0f, 0.0f);
                GlStateManager.scale(-scale, -scale, scale);

                GlStateManager.disableLighting();
                GlStateManager.depthMask(false);
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

                StringBuilder text = new StringBuilder();
                if (showDistance.isEnabled()) {
                    text.append("\u00a77[").append((int) distance).append("m] ");
                }
                text.append(player.getName());
                if (showHealth.isEnabled()) {
                    float hp = player.getHealth() + player.getAbsorptionAmount();
                    text.append(" \u00a7f[").append(HP_FORMAT.format(hp)).append("]");
                }

                String tagStr = text.toString();
                int width = FontUtil.regular.getStringWidth(tagStr);
                float tagW = width + 10.0f;
                float tagH = 14.0f;
                float tagX = -tagW / 2.0f;
                float tagY = -tagH / 2.0f;

                // Background & Border
                RenderUtil.drawRoundedRect(tagX, tagY, tagW, tagH, 2.5f, 0xA8141418);
                RenderUtil.drawRoundedOutline(tagX, tagY, tagW, tagH, 2.5f, 1.0f, ColorUtil.BORDER);

                // Health color bar under tag
                if (showHealth.isEnabled()) {
                    float maxHp = Math.max(1.0f, player.getMaxHealth());
                    int hpCol = ColorUtil.getHealthColor(player.getHealth(), maxHp);
                    float hpRatio = Math.max(0.0f, Math.min(1.0f, player.getHealth() / maxHp));
                    RenderUtil.drawRect(tagX + 1.0f, tagY + tagH - 1.5f, (tagW - 2.0f) * hpRatio, 1.5f, hpCol);
                }

                // Ensure depth stays disabled before rendering tag text & gear
                GlStateManager.disableDepth();
                GlStateManager.depthMask(false);
                FontUtil.regular.drawStringWithShadow(tagStr, tagX + 5.0f, tagY + (tagH - FontUtil.regular.getHeight()) / 2.0f - 0.5f, ColorUtil.TEXT_PRIMARY);

                if (showItems.isEnabled()) {
                    renderEquipment(player, tagY);
                }
            } finally {
                GlStateManager.enableDepth();
                GlStateManager.depthMask(true);
                GlStateManager.enableLighting();
                GlStateManager.disableBlend();
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                GlStateManager.popMatrix();
            }
        }
    }

    private void renderEquipment(EntityPlayer player, float tagY) {
        List<ItemStack> items = new ArrayList<>();
        if (player.getCurrentEquippedItem() != null) {
            items.add(player.getCurrentEquippedItem());
        }
        for (int i = 3; i >= 0; i--) {
            ItemStack stack = player.inventory.armorInventory[i];
            if (stack != null) {
                items.add(stack);
            }
        }

        if (items.isEmpty()) return;

        float totalW = items.size() * 18.0f;
        float startX = -totalW / 2.0f;
        float itemY = tagY - 20.0f;

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);

        for (ItemStack stack : items) {
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) startX, (int) itemY);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, (int) startX, (int) itemY, null);
            startX += 18.0f;
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
    }
}
