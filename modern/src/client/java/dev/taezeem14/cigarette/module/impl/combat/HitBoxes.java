package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;

public class HitBoxes extends Module {

    private static HitBoxes instance;

    private final NumberSetting expand = new NumberSetting("Expand", 0.25, 0.0, 1.0, 0.05);

    public HitBoxes() {
        super("HitBoxes", "Expands entity target bounding boxes during combat raytrace", Category.COMBAT);
        addSettings(expand);
        instance = this;
    }

    @Override
    public String getSuffix() {
        return String.format("%.2f", expand.getValue());
    }

    public static float getExpansionSize() {
        if (instance == null || !instance.isEnabled()) {
            return 0.0F;
        }
        return instance.expand.getValueFloat();
    }
}
