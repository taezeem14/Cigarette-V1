package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.CigaretteModernClient;
import dev.taezeem14.cigarette.config.ConfigManager;
import dev.taezeem14.cigarette.ui.hud.NotificationManager;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public class ProfilePanel {

    private float x;
    private float y;
    private final float width = 228.0F;
    private final float height = 224.0F;

    private String selectedProfile = "default";
    private String statusText = "";
    private long statusTime = 0L;
    private float scrollOffset = 0.0F;

    public ProfilePanel(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void render(GuiGraphicsExtractor g, float mouseX, float mouseY, float partialTicks, float guiScale, float viewportW, float viewportH) {
        this.x = AnimationUtil.clamp(this.x, 8.0F, Math.max(8.0F, viewportW - width - 8.0F));
        this.y = AnimationUtil.clamp(this.y, 40.0F, Math.max(40.0F, viewportH - height - 24.0F));

        int accent = ColorUtil.getAccent();
        RenderUtil.drawDropShadow(g, x, y, width, height, 6.0F, 0x70000000);
        RenderUtil.drawRoundedRect(g, x, y, width, height, 5.0F, ColorUtil.BACKGROUND);
        RenderUtil.drawRoundedOutline(g, x, y, width, height, 5.0F, 1.0F, ColorUtil.BORDER);

        float headerH = 26.0F;
        RenderUtil.drawRoundedRectTop(g, x, y, width, headerH, 5.0F, ColorUtil.HEADER);
        RenderUtil.drawRect(g, x + 1.0F, y + headerH - 1.0F, width - 2.0F, 1.0F, ColorUtil.modifyAlpha(accent, 180));
        RenderUtil.drawText(g, "Profiles & Cloud Config", x + 10.0F, y + (headerH - RenderUtil.getFontHeight()) / 2.0F, ColorUtil.TEXT_PRIMARY, false);

        ConfigManager cm = CigaretteModernClient.getInstance() != null ? CigaretteModernClient.getInstance().getConfigManager() : null;
        if (cm == null) return;
        List<String> profiles = cm.getProfileNames();

        float listX = x + 8.0F;
        float listY = y + headerH + 6.0F;
        float listW = width - 16.0F;
        float listH = 122.0F;
        RenderUtil.drawRoundedRect(g, listX, listY, listW, listH, 3.0F, 0xFF121216);
        RenderUtil.drawRoundedOutline(g, listX, listY, listW, listH, 3.0F, 1.0F, ColorUtil.BORDER);

        RenderUtil.pushScissor(g, listX, listY, listW, listH, guiScale);
        try {
            float itemY = listY + 4.0F - scrollOffset;
            for (String profile : profiles) {
                boolean selected = profile.equalsIgnoreCase(selectedProfile);
                boolean hovered = mouseX >= listX + 4.0F && mouseX <= listX + listW - 4.0F
                        && mouseY >= itemY && mouseY <= itemY + 18.0F
                        && mouseY >= listY && mouseY <= listY + listH;
                int bg = selected ? ColorUtil.modifyAlpha(accent, 42) : (hovered ? 0xFF222229 : 0xFF18181D);
                RenderUtil.drawRoundedRect(g, listX + 4.0F, itemY, listW - 8.0F, 17.0F, 2.5F, bg);
                if (selected) {
                    RenderUtil.drawRoundedRect(g, listX + 6.0F, itemY + 4.0F, 2.0F, 9.0F, 1.0F, accent);
                }
                RenderUtil.drawText(g, profile, listX + (selected ? 13.0F : 9.0F),
                        itemY + (17.0F - RenderUtil.getFontHeight()) / 2.0F,
                        selected ? ColorUtil.TEXT_PRIMARY : ColorUtil.TEXT_MUTED, false);
                itemY += 20.0F;
            }
        } finally {
            RenderUtil.popScissor(g);
        }

        float btnW = (width - 24.0F) / 3.0F;
        float btnH = 18.0F;
        float btnY1 = y + height - 56.0F;
        drawActionButton(g, "Save", x + 8.0F, btnY1, btnW, btnH, mouseX, mouseY, accent);
        drawActionButton(g, "Load", x + 12.0F + btnW, btnY1, btnW, btnH, mouseX, mouseY, 0xFF1F1F26);
        drawActionButton(g, "Delete", x + 16.0F + btnW * 2.0F, btnY1, btnW, btnH, mouseX, mouseY, 0xFF2B1C1E);

        float halfW = (width - 20.0F) / 2.0F;
        float btnY2 = y + height - 34.0F;
        drawActionButton(g, "Copy JSON", x + 8.0F, btnY2, halfW, btnH, mouseX, mouseY, 0xFF1F1F26);
        drawActionButton(g, "Import Clip", x + 12.0F + halfW, btnY2, halfW, btnH, mouseX, mouseY, 0xFF1F1F26);

        if (!statusText.isEmpty() && System.currentTimeMillis() - statusTime < 2500L) {
            RenderUtil.drawCenteredSmallText(g, statusText, x + width / 2.0F, y + height - 12.0F, accent, false);
        }
    }

    private void drawActionButton(GuiGraphicsExtractor g, String label, float bx, float by, float bw, float bh, float mx, float my, int baseBg) {
        boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        int col = hov ? ColorUtil.interpolateColor(baseBg, 0xFFFFFFFF, 0.14F) : baseBg;
        RenderUtil.drawRoundedRect(g, bx, by, bw, bh, 3.0F, col);
        RenderUtil.drawRoundedOutline(g, bx, by, bw, bh, 3.0F, 1.0F, ColorUtil.BORDER);
        RenderUtil.drawCenteredSmallText(g, label, bx + bw / 2.0F, by + (bh - RenderUtil.getSmallFontHeight()) / 2.0F, ColorUtil.TEXT_PRIMARY, false);
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        ConfigManager cm = CigaretteModernClient.getInstance() != null ? CigaretteModernClient.getInstance().getConfigManager() : null;
        if (cm == null) return;

        List<String> profiles = cm.getProfileNames();
        float listX = x + 8.0F;
        float listY = y + 32.0F;
        float listW = width - 16.0F;
        float listH = 122.0F;
        float itemY = listY + 4.0F - scrollOffset;
        for (String p : profiles) {
            if (mouseX >= listX + 4.0F && mouseX <= listX + listW - 4.0F
                    && mouseY >= itemY && mouseY <= itemY + 18.0F
                    && mouseY >= listY && mouseY <= listY + listH) {
                selectedProfile = p;
                return;
            }
            itemY += 20.0F;
        }

        float btnW = (width - 24.0F) / 3.0F;
        float btnH = 18.0F;
        float btnY1 = y + height - 56.0F;
        if (mouseY >= btnY1 && mouseY <= btnY1 + btnH) {
            if (mouseX >= x + 8.0F && mouseX <= x + 8.0F + btnW) {
                cm.save(selectedProfile);
                setStatus("Saved '" + selectedProfile + "'", NotificationManager.NotificationType.SUCCESS);
                return;
            }
            if (mouseX >= x + 12.0F + btnW && mouseX <= x + 12.0F + btnW * 2.0F) {
                boolean ok = cm.load(selectedProfile);
                setStatus(ok ? "Loaded '" + selectedProfile + "'" : "Load failed", ok ? NotificationManager.NotificationType.SUCCESS : NotificationManager.NotificationType.ERROR);
                return;
            }
            if (mouseX >= x + 16.0F + btnW * 2.0F && mouseX <= x + 16.0F + btnW * 3.0F) {
                cm.delete(selectedProfile);
                selectedProfile = "default";
                setStatus("Deleted profile", NotificationManager.NotificationType.WARNING);
                return;
            }
        }

        float halfW = (width - 20.0F) / 2.0F;
        float btnY2 = y + height - 34.0F;
        if (mouseY >= btnY2 && mouseY <= btnY2 + btnH) {
            if (mouseX >= x + 8.0F && mouseX <= x + 8.0F + halfW) {
                String json = cm.exportJson(selectedProfile);
                Minecraft.getInstance().keyboardHandler.setClipboard(json);
                setStatus("Copied JSON to clipboard", NotificationManager.NotificationType.INFO);
            } else if (mouseX >= x + 12.0F + halfW && mouseX <= x + 12.0F + halfW * 2.0F) {
                String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
                boolean ok = cm.importJson("imported", clip);
                if (ok) selectedProfile = "imported";
                setStatus(ok ? "Imported clipboard JSON" : "Invalid clipboard JSON",
                        ok ? NotificationManager.NotificationType.SUCCESS : NotificationManager.NotificationType.ERROR);
            }
        }
    }

    public void handleScroll(float mouseX, float mouseY, double scrollDeltaY) {
        ConfigManager cm = CigaretteModernClient.getInstance() != null ? CigaretteModernClient.getInstance().getConfigManager() : null;
        if (cm == null) return;
        if (mouseX >= x && mouseX <= x + width && mouseY >= y + 32.0F && mouseY <= y + 154.0F) {
            float maxScroll = Math.max(0.0F, cm.getProfileNames().size() * 20.0F - 114.0F);
            scrollOffset = AnimationUtil.clamp(scrollOffset + (scrollDeltaY > 0 ? -16.0F : 16.0F), 0.0F, maxScroll);
        }
    }

    private void setStatus(String msg, NotificationManager.NotificationType type) {
        statusText = msg;
        statusTime = System.currentTimeMillis();
        if (CigaretteModernClient.getInstance() != null && CigaretteModernClient.getInstance().getNotificationManager() != null) {
            CigaretteModernClient.getInstance().getNotificationManager().push("Profile", msg, type, 2000L);
        }
    }

    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
}
