package dev.taezeem14.cigarette.module.impl.combat;

import dev.taezeem14.cigarette.module.Category;
import dev.taezeem14.cigarette.module.Module;
import dev.taezeem14.cigarette.setting.impl.NumberSetting;

public class HitBoxes extends Module {

    private static HitBoxes instance;

    private final NumberSetting expand = new NumberSetting("Expand", 0.20, 0.00, 1.00, 0.01);

    public HitBoxes() {
        super("HitBoxes", "Expands player collision bounding boxes", Category.COMBAT);
        addSettings(expand);
        instance = this;
    }

    public static float getExpansionSize() {
        if (instance == null || !instance.isEnabled()) {
            return 0.0F;
        }
        return instance.expand.getValueFloat();
    }

    public static HitBoxes getInstance() {
        return instance;
    }
}
