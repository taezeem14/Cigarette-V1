package dev.taezeem14.cigarette.module.impl.render;

import dev.taezeem14.cigarette.event.EventTarget;
import dev.taezeem14.cigarette.event.impl.Render3DEvent;
import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.BooleanSetting;
import dev.taezeem14.cigarette.setting.impl.ColorSetting;
import dev.taezeem14.cigarette.setting.impl.ModeSetting;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

public class Tracers extends Module {

    private final ModeSetting colorMode = new ModeSetting("Color Mode", "Distance", "Distance", "Static", "Rainbow");
    private final ColorSetting staticColor = new ColorSetting("Color", 0xFF3882F6);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", 1.5, 0.5, 3.0, 0.1);
    private final BooleanSetting invisibles = new BooleanSetting("Invisible Players", false);

    public Tracers() {
        super("Tracers", "Draws 3D tracer lines from crosshair to entities", Category.RENDER);
        addSettings(colorMode, staticColor, lineWidth, invisibles);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        float partialTicks = event.getPartialTicks();

        // Eye position
        Vec3 eyes = new Vec3(
                mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks,
                mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks + mc.thePlayer.getEyeHeight(),
                mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks
        );

        // Forward vector from look direction
        Vec3 look = mc.thePlayer.getLook(partialTicks);
        Vec3 start = eyes.addVector(look.xCoord, look.yCoord, look.zCoord);

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead) continue;
            if (player.isInvisible() && !invisibles.isEnabled()) continue;

            Vec3 target = new Vec3(
                    player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks,
                    player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks + player.height / 2.0D,
                    player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
            );

            int color = getColor(player);
            RenderUtil.draw3DLine(start, target, color, lineWidth.getValueFloat());
        }
    }

    private int getColor(EntityPlayer player) {
        String mode = colorMode.getMode();
        switch (mode.toLowerCase()) {
            case "distance":
                float dist = mc.thePlayer.getDistanceToEntity(player);
                if (dist <= 15.0f) {
                    return 0xFFFF3333; // Red
                } else if (dist <= 35.0f) {
                    return 0xFFFFCC00; // Orange / Yellow
                } else {
                    return 0xFF33FF33; // Green
                }
            case "rainbow":
                return ColorUtil.getRainbow(4, player.getEntityId() * 100);
            case "static":
            default:
                return staticColor.getRGB();
        }
    }
}
