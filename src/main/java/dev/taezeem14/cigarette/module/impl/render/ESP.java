package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render2DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.FrustumUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.util.vector.Vector4f;

public class ESP extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Full", "Full", "Corner", "Outline");
    private final BooleanSetting targetPlayers = new BooleanSetting("Players", true);
    private final BooleanSetting targetMobs = new BooleanSetting("Mobs", true);
    private final BooleanSetting targetAnimals = new BooleanSetting("Animals", true);
    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", true);
    private final BooleanSetting armorBar = new BooleanSetting("Armor Bar", true);
    private final BooleanSetting invisibles = new BooleanSetting("Invisible Players", false);
    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Static", "Static", "Team", "Rainbow", "Astolfo");
    private final ColorSetting staticColor = new ColorSetting("Color", 0xFF3882F6);

    public ESP() {
        super("ESP", "Renders 2D projected bounding boxes and status bars on entities", Category.RENDER);
        addSettings(mode, targetPlayers, targetMobs, targetAnimals, healthBar, armorBar, invisibles, colorMode, staticColor);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer || entity.isDead) continue;
            if (entity.getEntityId() == -1337 || entity instanceof EntityArmorStand) continue;

            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.getHealth() <= 0.0f) continue;
            if (living.isInvisible() && !invisibles.isEnabled()) continue;
            if (!shouldRenderEntity(living)) continue;

            Vector4f bounds = FrustumUtil.getEntityPositionsOn2D(living, event.getPartialTicks());
            if (bounds == null) continue;

            float x = bounds.x;
            float y = bounds.y;
            float w = bounds.z - bounds.x;
            float h = bounds.w - bounds.y;

            if (w <= 0.0f || h <= 0.0f) continue;

            int boxColor = getEntityColor(living);

            renderBox(x, y, w, h, boxColor);

            if (healthBar.isEnabled()) {
                renderHealthBar(living, x, y, h);
            }

            if (armorBar.isEnabled()) {
                renderArmorBar(living, x, y, w, h);
            }
        }
    }

    private boolean shouldRenderEntity(EntityLivingBase living) {
        if (living instanceof EntityPlayer) {
            return targetPlayers.isEnabled() && !AntiBot.isBot(living);
        }
        if (living instanceof IMob || living instanceof IBossDisplayData) {
            return targetMobs.isEnabled();
        }
        if (living instanceof EntityAnimal || living instanceof EntityAmbientCreature
                || living instanceof EntityWaterMob || living instanceof EntityVillager) {
            return targetAnimals.isEnabled();
        }
        if (living instanceof EntityGolem) {
            return targetMobs.isEnabled() || targetAnimals.isEnabled();
        }
        return targetMobs.isEnabled() || targetAnimals.isEnabled();
    }

    private void renderBox(float x, float y, float w, float h, int color) {
        String boxMode = mode.getMode();
        switch (boxMode.toLowerCase()) {
            case "corner":
                renderCornerBox(x, y, w, h, color);
                break;
            case "outline":
                RenderUtil.drawOutline(x, y, w, h, 1.0f, color);
                break;
            case "full":
            default:
                RenderUtil.drawOutline(x - 0.5f, y - 0.5f, w + 1.0f, h + 1.0f, 1.0f, 0x99000000);
                RenderUtil.drawOutline(x, y, w, h, 1.0f, color);
                RenderUtil.drawOutline(x + 0.5f, y + 0.5f, w - 1.0f, h - 1.0f, 1.0f, 0x99000000);
                break;
        }
    }

    private void renderCornerBox(float x, float y, float w, float h, int color) {
        float lineW = Math.max(2.0f, w / 4.0f);
        float lineH = Math.max(2.0f, h / 4.0f);

        drawCornerLines(x - 0.5f, y - 0.5f, w + 1.0f, h + 1.0f, lineW, lineH, 0x99000000);
        drawCornerLines(x, y, w, h, lineW, lineH, color);
    }

    private void drawCornerLines(float x, float y, float w, float h, float lw, float lh, int color) {
        // Top-Left
        RenderUtil.drawRect(x, y, lw, 1.0f, color);
        RenderUtil.drawRect(x, y, 1.0f, lh, color);

        // Top-Right
        RenderUtil.drawRect(x + w - lw, y, lw, 1.0f, color);
        RenderUtil.drawRect(x + w - 1.0f, y, 1.0f, lh, color);

        // Bottom-Left
        RenderUtil.drawRect(x, y + h - 1.0f, lw, 1.0f, color);
        RenderUtil.drawRect(x, y + h - lh, 1.0f, lh, color);

        // Bottom-Right
        RenderUtil.drawRect(x + w - lw, y + h - 1.0f, lw, 1.0f, color);
        RenderUtil.drawRect(x + w - 1.0f, y + h - lh, 1.0f, lh, color);
    }

    private void renderHealthBar(EntityLivingBase entity, float x, float y, float h) {
        float barWidth = 2.0f;
        float barX = x - barWidth - 3.0f;

        float health = entity.getHealth();
        float maxHealth = Math.max(1.0f, entity.getMaxHealth());
        float ratio = Math.max(0.0f, Math.min(1.0f, health / maxHealth));

        float barHeight = h * ratio;

        RenderUtil.drawRect(barX - 0.5f, y - 0.5f, barWidth + 1.0f, h + 1.0f, 0xAA000000);

        int hpColor = ColorUtil.getHealthColor(health, maxHealth);
        RenderUtil.drawRect(barX, y + h - barHeight, barWidth, barHeight, hpColor);
    }

    private void renderArmorBar(EntityLivingBase entity, float x, float y, float w, float h) {
        float barHeight = 2.0f;
        float barY = y + h + 3.0f;

        int totalArmor = entity.getTotalArmorValue();
        float ratio = Math.max(0.0f, Math.min(1.0f, (float) totalArmor / 20.0f));

        if (ratio > 0.0f) {
            float barW = w * ratio;
            RenderUtil.drawRect(x - 0.5f, barY - 0.5f, w + 1.0f, barHeight + 1.0f, 0xAA000000);
            RenderUtil.drawRect(x, barY, barW, barHeight, ColorUtil.getAccent());
        }
    }

    public int getEntityColor(EntityLivingBase entity) {
        String cm = colorMode.getMode();
        switch (cm.toLowerCase()) {
            case "team":
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    if (player.getTeam() != null) {
                        String formatted = player.getTeam().formatString(player.getName());
                        for (int i = 0; i < formatted.length() - 1; i++) {
                            if (formatted.charAt(i) == '\u00a7') {
                                char code = formatted.charAt(i + 1);
                                int colIndex = "0123456789abcdef".indexOf(Character.toLowerCase(code));
                                if (colIndex != -1) {
                                    return 0xFF000000 | mc.fontRendererObj.getColorCode(code);
                                }
                            }
                        }
                    }
                    return Teams.isTeammate(player) ? 0xFF10B981 : 0xFFFFFFFF;
                }
                return (entity instanceof IMob || entity instanceof IBossDisplayData) ? 0xFFEF4444 : 0xFF10B981;
            case "rainbow":
                return ColorUtil.getRainbow(4, entity.getEntityId() * 100);
            case "astolfo":
                return ColorUtil.getAstolfo(4, entity.getEntityId() * 100);
            case "static":
            default:
                return staticColor.getRGB();
        }
    }
}
