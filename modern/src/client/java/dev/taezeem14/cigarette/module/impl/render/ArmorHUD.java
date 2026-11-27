package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ArmorHUD extends Module {

    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private final BooleanSetting showHeld = new BooleanSetting("Held Item", true);
    private final BooleanSetting showPercent = new BooleanSetting("Durability %", true);
    private final BooleanSetting showBackground = new BooleanSetting("Background", true);

    public ArmorHUD() {
        super("ArmorHUD", "Displays equipped armor and held item durability above the hotbar", Category.RENDER);
        addSettings(showHeld, showPercent, showBackground);
        setState(true);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (mc.getDebugOverlay() != null && mc.getDebugOverlay().showDebugScreen()) return;

        List<ItemStack> items = new ArrayList<>(5);
        for (EquipmentSlot slot : ARMOR_ORDER) {
            ItemStack armor = mc.player.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                items.add(armor);
            }
        }
        if (showHeld.isEnabled()) {
            ItemStack held = mc.player.getMainHandItem();
            if (!held.isEmpty()) {
                items.add(held);
            }
        }

        if (items.isEmpty()) return;

        GuiGraphicsExtractor g = event.getGraphics();
        float slotW = 22.0F;
        float gap = 2.0F;
        float pad = 3.0F;
        float totalW = items.size() * slotW + (items.size() - 1) * gap + pad * 2.0F;
        float totalH = showPercent.isEnabled() ? 28.0F : 22.0F;
        float x = event.getScreenWidth() / 2.0F - totalW / 2.0F;
        float baseOffset = (mc.player.isCreative() || mc.player.isSpectator()) ? 40.0F : 58.0F;
        float y = event.getScreenHeight() - baseOffset - (totalH - 22.0F);

        if (showBackground.isEnabled()) {
            RenderUtil.drawDropShadow(g, x, y, totalW, totalH, 4.0F, 0x50000000);
            RenderUtil.drawRoundedRect(g, x, y, totalW, totalH, 4.0F, 0xCC141418);
            RenderUtil.drawRoundedOutline(g, x, y, totalW, totalH, 4.0F, 1.0F, ColorUtil.BORDER);
        }

        float curX = x + pad;
        float iconY = showPercent.isEnabled() ? (y + 8.5F) : (y + 2.0F);

        for (ItemStack stack : items) {
            int maxDmg = stack.getMaxDamage();
            if (maxDmg > 0) {
                int rem = Math.max(0, maxDmg - stack.getDamageValue());
                float ratio = Math.max(0.0F, Math.min(1.0F, (float) rem / (float) maxDmg));
                int col = ColorUtil.getHealthColor(rem, maxDmg);
                if (showPercent.isEnabled()) {
                    String pct = Math.round(ratio * 100.0F) + "%";
                    RenderUtil.drawCenteredSmallText(g, pct, curX + slotW / 2.0F, y + 1.5F, col, true);
                }
                g.item(stack, Math.round(curX + (slotW - 16.0F) / 2.0F), Math.round(iconY));
                RenderUtil.drawRect(g, curX + 2.0F, y + totalH - 3.0F, slotW - 4.0F, 1.5F, 0xAA000000);
                RenderUtil.drawRect(g, curX + 2.0F, y + totalH - 3.0F, (slotW - 4.0F) * ratio, 1.5F, col);
            } else {
                g.item(stack, Math.round(curX + (slotW - 16.0F) / 2.0F), Math.round(iconY));
                if (stack.getCount() > 1) {
                    String cnt = String.valueOf(stack.getCount());
                    RenderUtil.drawSmallText(g, cnt, curX + slotW - RenderUtil.getSmallStringWidth(cnt) - 2.0F, iconY + 9.0F, ColorUtil.TEXT_PRIMARY, true);
                }
            }
            curX += slotW + gap;
        }
    }
}
