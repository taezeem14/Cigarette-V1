package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
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

        RenderManager rm = mc.getRenderManager();
        float partialTicks = event.getPartialTicks();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead) continue;

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - rm.viewerPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - rm.viewerPosY + player.height + 0.55;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - rm.viewerPosZ;

            float distance = mc.thePlayer.getDistanceToEntity(player);
            float scale = Math.max(distance / 50.0f, 1.0f) * (scaleFactor.getValueFloat() * 0.015f);

            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            GL11.glNormal3f(0.0f, 1.0f, 0.0f);
            GL11.glRotatef(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
            GL11.glRotatef(rm.playerViewX, 1.0f, 0.0f, 0.0f);
            GL11.glScalef(-scale, -scale, scale);

            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            // Construct text: [dist] Name [health HP]
            StringBuilder text = new StringBuilder();
            if (showDistance.isEnabled()) {
                text.append("§7[").append((int) distance).append("m] ");
            }
            text.append(player.getName());
            if (showHealth.isEnabled()) {
                float hp = player.getHealth() + player.getAbsorptionAmount();
                text.append(" §f[").append(HP_FORMAT.format(hp)).append("]");
            }

            String tagStr = text.toString();
            int width = FontUtil.regular.getStringWidth(tagStr);
            float tagW = width + 10.0f;
            float tagH = 14.0f;
            float tagX = -tagW / 2.0f;
            float tagY = -tagH / 2.0f;

            // Background & Border
            RenderUtil.drawRoundedRect(tagX, tagY, tagW, tagH, 2.5f, 0x88141416);
            RenderUtil.drawRoundedOutline(tagX, tagY, tagW, tagH, 2.5f, 1.0f, 0xFF25252C);

            // Health color line under tag
            if (showHealth.isEnabled()) {
                int hpCol = ColorUtil.getHealthColor(player.getHealth(), player.getMaxHealth());
                float hpRatio = Math.max(0.0f, Math.min(1.0f, player.getHealth() / player.getMaxHealth()));
                RenderUtil.drawRect(tagX, tagY + tagH - 1.5f, tagW * hpRatio, 1.5f, hpCol);
            }

            // Draw String
            FontUtil.regular.drawString(tagStr, tagX + 5.0f, tagY + (tagH - FontUtil.regular.getHeight()) / 2.0f - 0.5f, ColorUtil.TEXT_PRIMARY);

            // Render Equipment Icons
            if (showItems.isEnabled()) {
                renderEquipment(player, tagY);
            }

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GL11.glPopMatrix();
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

        for (ItemStack stack : items) {
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) startX, (int) itemY);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, (int) startX, (int) itemY, null);
            startX += 18.0f;
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
    }
}
