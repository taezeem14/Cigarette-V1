package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class ArmorHUD extends Module {

    private final BooleanSetting showDurability = new BooleanSetting("Show Durability %", true);
    private final BooleanSetting showHeld = new BooleanSetting("Show Held Item", true);

    public ArmorHUD() {
        super("ArmorHUD", "Displays equipped armor and held item durability above the hotbar", Category.RENDER);
        addSettings(showDurability, showHeld);
        setState(true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.gameSettings == null || mc.gameSettings.showDebugInfo) {
            return;
        }

        List<ItemStack> items = new ArrayList<>(5);
        for (int i = 3; i >= 0; i--) {
            ItemStack armor = mc.thePlayer.inventory.armorInventory[i];
            if (armor != null) {
                items.add(armor);
            }
        }
        if (showHeld.isEnabled() && mc.thePlayer.getHeldItem() != null) {
            items.add(mc.thePlayer.getHeldItem());
        }

        if (items.isEmpty()) {
            return;
        }

        FontUtil.ensureInitialized();
        ScaledResolution sr = event.getScaledResolution() != null ? event.getScaledResolution() : new ScaledResolution(mc);

        int baseX = sr.getScaledWidth() / 2 + 12;
        int baseY = sr.getScaledHeight() - 56;
        if (mc.thePlayer.isInsideOfMaterial(Material.water) && !mc.thePlayer.capabilities.isCreativeMode) {
            baseY -= 10;
        }

        GlStateManager.pushMatrix();
        try {
            int itemX = baseX;
            for (ItemStack stack : items) {
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableRescaleNormal();
                GlStateManager.enableDepth();
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, itemX, baseY);
                mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, stack, itemX, baseY, null);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableRescaleNormal();
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();

                if (showDurability.isEnabled() && stack.isItemStackDamageable() && stack.getMaxDamage() > 0) {
                    int remaining = Math.max(0, stack.getMaxDamage() - stack.getItemDamage());
                    int pct = Math.max(0, Math.min(100, Math.round((remaining * 100.0f) / stack.getMaxDamage())));
                    int durColor = ColorUtil.getHealthColor(remaining, stack.getMaxDamage());
                    String pctText = pct + "%";

                    GlStateManager.pushMatrix();
                    float scale = 0.75f;
                    GlStateManager.scale(scale, scale, 1.0f);
                    FontUtil.small.drawCenteredStringWithShadow(
                            pctText,
                            (itemX + 8.0f) / scale,
                            (baseY - 6.0f) / scale,
                            durColor
                    );
                    GlStateManager.popMatrix();
                }

                itemX += 18;
            }
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }
    }
}
