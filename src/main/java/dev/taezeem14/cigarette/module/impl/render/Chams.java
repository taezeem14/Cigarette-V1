package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
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

    public static boolean shouldChams(EntityLivingBase entity) {
        if (entity == mc.thePlayer) return false;
        if (!(entity instanceof EntityPlayer)) return false;

        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getModuleManager() != null) {
            Chams chams = Cigarette.INSTANCE.getModuleManager().getModuleByClass(Chams.class);
            return chams != null && chams.isEnabled();
        }
        return false;
    }

    public static void preRenderCallback(EntityLivingBase entity) {
        if (!shouldChams(entity)) return;

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1.0F, -1100000.0F);
    }

    public static void postRenderCallback(EntityLivingBase entity) {
        if (!shouldChams(entity)) return;

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
