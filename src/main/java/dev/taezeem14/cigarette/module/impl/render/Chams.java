package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.module.impl.utility.AntiBot;
import dev.taezeem14.cigarette.module.impl.utility.Teams;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class Chams extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Static", "Static", "Rainbow", "Team");
    private final ColorSetting hiddenColor = new ColorSetting("Hidden Color", 0xFFFF5722);
    private final ColorSetting visibleColor = new ColorSetting("Visible Color", 0xFF3882F6);
    private final BooleanSetting textured = new BooleanSetting("Textured", true);

    public Chams() {
        super("Chams", "Renders entity player models visible through walls", Category.RENDER);
        addSettings(mode, hiddenColor, visibleColor, textured);
    }

    private static Chams getInstance() {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            return Cigarette.INSTANCE.getModuleManager().getModuleByClass(Chams.class);
        }
        return null;
    }

    public static boolean shouldChams(EntityLivingBase entity) {
        if (entity == null || entity == mc.thePlayer) return false;
        if (!(entity instanceof EntityPlayer)) return false;
        if (entity.getEntityId() == -1337 || AntiBot.isBot(entity)) return false;

        Chams chams = getInstance();
        return chams != null && chams.isEnabled();
    }

    public static int getChamsColor(EntityLivingBase entity) {
        Chams chams = getInstance();
        if (chams == null || entity == null) {
            return ColorUtil.getAccent();
        }

        String m = chams.mode.getMode();
        if ("rainbow".equalsIgnoreCase(m)) {
            return ColorUtil.getRainbow(4, entity.getEntityId() * 100);
        }
        if ("team".equalsIgnoreCase(m) && entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player.getTeam() != null) {
                String formatted = player.getTeam().formatString(player.getName());
                for (int i = 0; i < formatted.length() - 1; i++) {
                    if (formatted.charAt(i) == '\u00a7') {
                        char code = formatted.charAt(i + 1);
                        if ("0123456789abcdef".indexOf(Character.toLowerCase(code)) != -1) {
                            return 0xFF000000 | mc.fontRendererObj.getColorCode(code);
                        }
                    }
                }
            }
            return Teams.isTeammate(player) ? 0xFF10B981 : 0xFFEF4444;
        }
        return chams.visibleColor.getRGB();
    }

    public static boolean isTexturedMode() {
        Chams chams = getInstance();
        return chams == null || chams.isTextured();
    }

    public static void applyChamsTint(int color) {
        float a = ((color >> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GlStateManager.color(r, g, b, a > 0.0F ? a : 1.0F);
    }

    public static void preRenderCallback(EntityLivingBase entity) {
        if (!shouldChams(entity)) return;

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1.0F, -1100000.0F);

        if (!isTexturedMode()) {
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            applyChamsTint(getChamsColor(entity));
        }
    }

    public static void postRenderCallback(EntityLivingBase entity) {
        if (!shouldChams(entity)) return;

        if (!isTexturedMode()) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
        }

        GL11.glPolygonOffset(1.0F, 1100000.0F);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
    }

    public ColorSetting getHiddenColor() {
        return hiddenColor;
    }

    public ColorSetting getVisibleColor() {
        return visibleColor;
    }

    public boolean isTextured() {
        return textured.isEnabled();
    }
}
