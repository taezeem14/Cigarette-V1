package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.FrustumUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Nametags extends Module {

    private static final DecimalFormat HP_FMT = new DecimalFormat("0.#");

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", true);
    private final BooleanSetting showDistance = new BooleanSetting("Distance", true);
    private final BooleanSetting showHealth = new BooleanSetting("Health", true);
    private final BooleanSetting showItems = new BooleanSetting("Armor & Items", true);
    private final NumberSetting scaleFactor = new NumberSetting("Scale", 1.0, 0.7, 1.8, 0.05);

    public Nametags() {
        super("Nametags", "Renders clean HUD-projected nametags with health and equipment", Category.RENDER);
        addSettings(players, mobs, showDistance, showHealth, showItems, scaleFactor);
    }

    public static boolean shouldCancelVanillaNametag(Entity entity) {
        if (!(entity instanceof LivingEntity living) || living == mc.player) return false;
        if (living instanceof ArmorStand || living.getId() == -1337) return false;
        if (CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getModuleManager() != null) {
            Nametags n = CigaretteModernClient.getInstance().getModuleManager().getModuleByClass(Nametags.class);
            if (n == null || !n.isEnabled()) return false;
            if (living instanceof Player player) {
                return n.players.isEnabled() && !AntiBot.isBot(player);
            }
            return n.mobs.isEnabled();
        }
        return false;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.level == null || mc.player == null) return;
        GuiGraphicsExtractor g = event.getGraphics();
        float pt = event.getPartialTicks();
        int screenW = event.getScreenWidth();
        int screenH = event.getScreenHeight();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == mc.player || !living.isAlive()) continue;
            if (living instanceof ArmorStand || living.getId() == -1337) continue;

            if (living instanceof Player player) {
                if (!players.isEnabled() || AntiBot.isBot(player)) continue;
            } else if (!mobs.isEnabled()) {
                continue;
            }

            double ix = living.xo + (living.getX() - living.xo) * pt;
            double iy = living.yo + (living.getY() - living.yo) * pt + living.getBbHeight() + 0.55;
            double iz = living.zo + (living.getZ() - living.zo) * pt;

            float[] pos = FrustumUtil.projectToScreen(ix, iy, iz, screenW, screenH);
            if (pos == null) continue;

            float dist = mc.player.distanceTo(living);
            StringBuilder sb = new StringBuilder();
            if (showDistance.isEnabled()) sb.append("\u00a77[").append((int) dist).append("m] ");
            sb.append(living.getName().getString());
            if (showHealth.isEnabled()) {
                float hp = living.getHealth() + living.getAbsorptionAmount();
                sb.append(" \u00a7f[").append(HP_FMT.format(hp)).append("]");
            }
            String text = sb.toString();

            float s = scaleFactor.getValueFloat();
            float tagW = RenderUtil.getStringWidth(text) + 10.0F;
            float tagH = 14.0F;
            float tagX = -tagW / 2.0F;
            float tagY = -tagH / 2.0F;

            g.pose().pushMatrix();
            g.pose().translate(pos[0], pos[1]);
            g.pose().scale(s, s);

            RenderUtil.drawRoundedRect(g, tagX, tagY, tagW, tagH, 2.5F, 0xB8141418);
            RenderUtil.drawRoundedOutline(g, tagX, tagY, tagW, tagH, 2.5F, 1.0F, ColorUtil.BORDER);

            if (showHealth.isEnabled()) {
                float maxHp = Math.max(1.0F, living.getMaxHealth());
                int hpCol = ColorUtil.getHealthColor(living.getHealth(), maxHp);
                float ratio = Math.max(0.0F, Math.min(1.0F, living.getHealth() / maxHp));
                RenderUtil.drawRect(g, tagX + 1.0F, tagY + tagH - 1.5F, (tagW - 2.0F) * ratio, 1.5F, hpCol);
            }
            RenderUtil.drawText(g, text, tagX + 5.0F, tagY + (tagH - RenderUtil.getFontHeight()) / 2.0F, ColorUtil.TEXT_PRIMARY, true);

            if (showItems.isEnabled()) {
                List<ItemStack> gear = new ArrayList<>();
                ItemStack main = living.getMainHandItem();
                if (!main.isEmpty()) gear.add(main);
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ItemStack armor = living.getItemBySlot(slot);
                    if (!armor.isEmpty()) gear.add(armor);
                }
                if (!gear.isEmpty()) {
                    float ixStart = -(gear.size() * 17.0F) / 2.0F;
                    float iyIcon = tagY - 18.0F;
                    for (ItemStack st : gear) {
                        g.item(st, Math.round(ixStart), Math.round(iyIcon));
                        ixStart += 17.0F;
                    }
                }
            }

            g.pose().popMatrix();
        }
    }
}
