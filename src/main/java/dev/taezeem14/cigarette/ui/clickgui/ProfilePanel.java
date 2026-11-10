package dev.taezeem14.cigarette.ui.clickgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.config.ConfigManager;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.ui.hud.NotificationManager;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import org.lwjgl.input.Keyboard;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class ProfilePanel {

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final float HEADER_HEIGHT = 26.0f;

    private float x;
    private float y;
    private float width = 370.0f;
    private float height = 252.0f;

    private boolean dragging = false;
    private float dragX = 0.0f;
    private float dragY = 0.0f;

    private float scrollOffset = 0.0f;
    private float targetScrollOffset = 0.0f;

    private String newProfileName = "";
    private boolean typing = false;
    private String statusMessage = "Manage, export, or import JSON profiles";
    private long statusTimer = 0L;

    public ProfilePanel(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void render(float mouseX, float mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks, 1.0f, 10000.0f, 10000.0f);
    }

    public void render(float mouseX, float mouseY, float partialTicks, float guiScale, float viewportW, float viewportH) {
        if (dragging) {
            this.x = AnimationUtil.clamp(mouseX - dragX, 4.0f, Math.max(4.0f, viewportW - width - 4.0f));
            this.y = AnimationUtil.clamp(mouseY - dragY, 40.0f, Math.max(40.0f, viewportH - height - 4.0f));
        } else if (viewportW > 0.0f && viewportH > 0.0f) {
            this.x = AnimationUtil.clamp(this.x, 4.0f, Math.max(4.0f, viewportW - width - 4.0f));
            this.y = AnimationUtil.clamp(this.y, 40.0f, Math.max(40.0f, viewportH - height - 4.0f));
        }

        int accent = ColorUtil.getAccent();

        // Main panel shadow & background
        RenderUtil.drawDropShadow(x, y, width, height, 6.0f, 0x66000000);
        RenderUtil.drawRoundedRect(x, y, width, height, 5.0f, ColorUtil.BACKGROUND);
        RenderUtil.drawRoundedOutline(x, y, width, height, 5.0f, 1.0f, ColorUtil.BORDER);

        // Draggable Header
        RenderUtil.drawRoundedRectTop(x, y, width, HEADER_HEIGHT, 5.0f, ColorUtil.HEADER);
        RenderUtil.drawRect(x + 1.0f, y + HEADER_HEIGHT - 1.0f, width - 2.0f, 1.0f, ColorUtil.modifyAlpha(accent, 175));
        FontUtil.bold.drawString("Profiles & Cloud Configs", x + 10.0f, y + (HEADER_HEIGHT - FontUtil.bold.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

        ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
        String activeName = (cm != null && cm.getActiveProfile() != null) ? cm.getActiveProfile().getName() : "default";
        String activeLabel = "Active: " + activeName;
        FontUtil.small.drawString(activeLabel, x + width - FontUtil.small.getStringWidth(activeLabel) - 10.0f,
                y + (HEADER_HEIGHT - FontUtil.small.getHeight()) / 2.0f, accent);

        // Input + action buttons row
        float inputY = y + HEADER_HEIGHT + 8.0f;
        float inputW = 148.0f;
        float inputH = 18.0f;
        float inputX = x + 10.0f;

        RenderUtil.drawRoundedRect(inputX, inputY, inputW, inputH, 3.0f, 0xFF19191E);
        RenderUtil.drawRoundedOutline(inputX, inputY, inputW, inputH, 3.0f, 1.0f, typing ? accent : ColorUtil.BORDER);

        String displayText = newProfileName.isEmpty() && !typing
                ? "Enter profile name..."
                : newProfileName + (typing && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        int textCol = newProfileName.isEmpty() && !typing ? 0xFF666670 : ColorUtil.TEXT_PRIMARY;
        FontUtil.small.drawString(displayText, inputX + 6.0f, inputY + (inputH - FontUtil.small.getHeight()) / 2.0f, textCol);

        // Create button
        float createBtnX = inputX + inputW + 5.0f;
        float createBtnW = 50.0f;
        boolean createHovered = mouseX >= createBtnX && mouseX <= createBtnX + createBtnW && mouseY >= inputY && mouseY <= inputY + inputH;
        RenderUtil.drawRoundedRect(createBtnX, inputY, createBtnW, inputH, 3.0f, createHovered ? accent : 0xFF23232B);
        FontUtil.small.drawCenteredString("Create", createBtnX + createBtnW / 2.0f, inputY + (inputH - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

        // Save Active button
        float saveBtnX = createBtnX + createBtnW + 5.0f;
        float saveBtnW = 64.0f;
        boolean saveHovered = mouseX >= saveBtnX && mouseX <= saveBtnX + saveBtnW && mouseY >= inputY && mouseY <= inputY + inputH;
        RenderUtil.drawRoundedRect(saveBtnX, inputY, saveBtnW, inputH, 3.0f, saveHovered ? 0xFF10B981 : 0xFF1A2923);
        FontUtil.small.drawCenteredString("Save Active", saveBtnX + saveBtnW / 2.0f, inputY + (inputH - FontUtil.small.getHeight()) / 2.0f, saveHovered ? 0xFFFFFFFF : 0xFF6EE7B7);

        // Import from Clipboard button
        float importBtnX = saveBtnX + saveBtnW + 5.0f;
        float importBtnW = 73.0f;
        boolean importHovered = mouseX >= importBtnX && mouseX <= importBtnX + importBtnW && mouseY >= inputY && mouseY <= inputY + inputH;
        RenderUtil.drawRoundedRect(importBtnX, inputY, importBtnW, inputH, 3.0f, importHovered ? 0xFF0284C7 : 0xFF1B2430);
        FontUtil.small.drawCenteredString("Import Clip", importBtnX + importBtnW / 2.0f, inputY + (inputH - FontUtil.small.getHeight()) / 2.0f, importHovered ? 0xFFFFFFFF : 0xFF7DD3FC);

        // Scrollable Profile List Box
        float listY = inputY + inputH + 8.0f;
        float listW = width - 20.0f;
        float listH = 148.0f;
        float listX = x + 10.0f;

        RenderUtil.drawRoundedRect(listX, listY, listW, listH, 3.0f, 0xFF16161A);
        RenderUtil.drawRoundedOutline(listX, listY, listW, listH, 3.0f, 1.0f, ColorUtil.BORDER);

        if (cm != null) {
            List<String> profiles = cm.listProfiles();
            float totalH = profiles.size() * 23.0f + 6.0f;
            float maxScroll = Math.max(0.0f, totalH - listH);
            targetScrollOffset = AnimationUtil.clamp(targetScrollOffset, 0.0f, maxScroll);
            scrollOffset = AnimationUtil.lerp(scrollOffset, targetScrollOffset, 18.0f);

            boolean hasScroll = maxScroll > 0.5f;
            float rowW = hasScroll ? listW - 12.0f : listW - 8.0f;

            RenderUtil.pushScissor(listX + 1.0f, listY + 1.0f, listW - 2.0f, listH - 2.0f, guiScale);
            try {
                float itemY = listY + 4.0f - scrollOffset;
                for (String profile : profiles) {
                    boolean isCurrent = profile.equalsIgnoreCase(activeName);
                    boolean itemHovered = mouseX >= listX + 4.0f && mouseX <= listX + 4.0f + rowW
                            && mouseY >= Math.max(listY, itemY) && mouseY <= Math.min(listY + listH, itemY + 20.0f);

                    RenderUtil.drawRoundedRect(listX + 4.0f, itemY, rowW, 20.0f, 2.5f,
                            itemHovered ? 0xFF212129 : (isCurrent ? 0xFF1A1B24 : 0xFF18181E));

                    int nameCol = isCurrent ? accent : ColorUtil.TEXT_PRIMARY;
                    FontUtil.regular.drawString(profile + (isCurrent ? " \u00a77(Active)" : ""),
                            listX + 8.0f, itemY + (20.0f - FontUtil.regular.getHeight()) / 2.0f, nameCol);

                    float delBtnW = 32.0f;
                    float expBtnW = 38.0f;
                    float loadBtnW = 38.0f;
                    float rightEdge = listX + 4.0f + rowW - 4.0f;

                    float delBtnX = rightEdge - delBtnW;
                    float expBtnX = delBtnX - expBtnW - 4.0f;
                    float loadBtnX = expBtnX - loadBtnW - 4.0f;

                    boolean loadHov = mouseX >= loadBtnX && mouseX <= loadBtnX + loadBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f;
                    RenderUtil.drawRoundedRect(loadBtnX, itemY + 2.0f, loadBtnW, 16.0f, 2.0f, loadHov ? accent : 0xFF25252E);
                    FontUtil.small.drawCenteredString("Load", loadBtnX + loadBtnW / 2.0f,
                            itemY + 2.0f + (16.0f - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

                    boolean expHov = mouseX >= expBtnX && mouseX <= expBtnX + expBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f;
                    RenderUtil.drawRoundedRect(expBtnX, itemY + 2.0f, expBtnW, 16.0f, 2.0f, expHov ? 0xFF0288D1 : 0xFF1C2833);
                    FontUtil.small.drawCenteredString("Copy", expBtnX + expBtnW / 2.0f,
                            itemY + 2.0f + (16.0f - FontUtil.small.getHeight()) / 2.0f, 0xFF4FC3F7);

                    if (!profile.equalsIgnoreCase("default")) {
                        boolean delHov = mouseX >= delBtnX && mouseX <= delBtnX + delBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f;
                        RenderUtil.drawRoundedRect(delBtnX, itemY + 2.0f, delBtnW, 16.0f, 2.0f, delHov ? 0xFFC62828 : 0xFF2C1E1E);
                        FontUtil.small.drawCenteredString("Del", delBtnX + delBtnW / 2.0f,
                                itemY + 2.0f + (16.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFEF9A9A);
                    }

                    itemY += 23.0f;
                }

                if (hasScroll) {
                    float trackX = listX + listW - 4.5f;
                    float trackY = listY + 4.0f;
                    float trackH = listH - 8.0f;
                    RenderUtil.drawRoundedRect(trackX, trackY, 2.0f, trackH, 1.0f, 0xFF22222A);
                    float thumbH = Math.max(16.0f, trackH * (listH / totalH));
                    float thumbY = trackY + (trackH - thumbH) * (scrollOffset / maxScroll);
                    RenderUtil.drawRoundedRect(trackX, thumbY, 2.0f, thumbH, 1.0f, ColorUtil.modifyAlpha(accent, 190));
                }
            } finally {
                RenderUtil.popScissor();
            }
        }

        // Status bar
        float statusY = listY + listH + 7.0f;
        FontUtil.small.drawString(statusMessage, x + 12.0f, statusY,
                (System.currentTimeMillis() - statusTimer < 3000L) ? accent : ColorUtil.TEXT_MUTED);
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT) {
            dragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
            typing = false;
            return;
        }

        float inputY = y + HEADER_HEIGHT + 8.0f;
        float inputW = 148.0f;
        float inputH = 18.0f;
        float inputX = x + 10.0f;

        if (mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + inputH) {
            typing = true;
            return;
        } else {
            typing = false;
        }

        ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
        if (cm == null) return;

        // Create button
        float createBtnX = inputX + inputW + 5.0f;
        float createBtnW = 50.0f;
        if (mouseX >= createBtnX && mouseX <= createBtnX + createBtnW && mouseY >= inputY && mouseY <= inputY + inputH) {
            String sanitized = sanitizeName(newProfileName);
            if (!sanitized.isEmpty()) {
                cm.save(sanitized);
                setStatus("Created and saved profile '" + sanitized + "'");
                notifyUser("Profile Created", "Saved profile '" + sanitized + "'", NotificationManager.NotificationType.SUCCESS);
                newProfileName = "";
            }
            return;
        }

        // Save Active button
        float saveBtnX = createBtnX + createBtnW + 5.0f;
        float saveBtnW = 64.0f;
        if (mouseX >= saveBtnX && mouseX <= saveBtnX + saveBtnW && mouseY >= inputY && mouseY <= inputY + inputH) {
            String active = cm.getActiveProfile() != null ? cm.getActiveProfile().getName() : "default";
            cm.save(active);
            setStatus("Saved active profile '" + active + "'");
            notifyUser("Profile Saved", "Updated '" + active + "'", NotificationManager.NotificationType.SUCCESS);
            return;
        }

        // Import from Clipboard button
        float importBtnX = saveBtnX + saveBtnW + 5.0f;
        float importBtnW = 73.0f;
        if (mouseX >= importBtnX && mouseX <= importBtnX + importBtnW && mouseY >= inputY && mouseY <= inputY + inputH) {
            importFromClipboard(cm);
            return;
        }

        // List interactions
        float listY = inputY + inputH + 8.0f;
        float listW = width - 20.0f;
        float listH = 148.0f;
        float listX = x + 10.0f;
        if (mouseX < listX || mouseX > listX + listW || mouseY < listY || mouseY > listY + listH) {
            return;
        }

        List<String> profiles = cm.listProfiles();
        float totalH = profiles.size() * 23.0f + 6.0f;
        boolean hasScroll = totalH > listH + 0.5f;
        float rowW = hasScroll ? listW - 12.0f : listW - 8.0f;
        float itemY = listY + 4.0f - scrollOffset;

        for (String profile : profiles) {
            float delBtnW = 32.0f;
            float expBtnW = 38.0f;
            float loadBtnW = 38.0f;
            float rightEdge = listX + 4.0f + rowW - 4.0f;

            float delBtnX = rightEdge - delBtnW;
            float expBtnX = delBtnX - expBtnW - 4.0f;
            float loadBtnX = expBtnX - loadBtnW - 4.0f;

            if (mouseX >= loadBtnX && mouseX <= loadBtnX + loadBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f) {
                cm.load(profile);
                setStatus("Loaded profile '" + profile + "'");
                notifyUser("Profile Loaded", "Switched to '" + profile + "'", NotificationManager.NotificationType.INFO);
                return;
            }

            if (mouseX >= expBtnX && mouseX <= expBtnX + expBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f) {
                try {
                    File f = new File(cm.getProfilesDir(), profile + ".json");
                    if (f.exists()) {
                        String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
                        setStatus("Copied '" + profile + "' JSON to clipboard!");
                        notifyUser("Profile Exported", "Copied '" + profile + "' JSON to clipboard", NotificationManager.NotificationType.SUCCESS);
                    }
                } catch (Exception e) {
                    setStatus("Error copying config to clipboard");
                    notifyUser("Export Failed", "Could not copy profile JSON", NotificationManager.NotificationType.ERROR);
                }
                return;
            }

            if (!profile.equalsIgnoreCase("default")) {
                if (mouseX >= delBtnX && mouseX <= delBtnX + delBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f) {
                    cm.delete(profile);
                    setStatus("Deleted profile '" + profile + "'");
                    notifyUser("Profile Deleted", "Removed '" + profile + "'", NotificationManager.NotificationType.WARNING);
                    return;
                }
            }

            itemY += 23.0f;
        }
    }

    public void mouseReleased(float mouseX, float mouseY, int mouseButton) {
        this.dragging = false;
    }

    public boolean handleScroll(float mouseX, float mouseY, int dWheel) {
        if (dWheel == 0) return false;
        float inputY = y + HEADER_HEIGHT + 8.0f;
        float listY = inputY + 18.0f + 8.0f;
        float listW = width - 20.0f;
        float listH = 148.0f;
        float listX = x + 10.0f;

        if (mouseX < listX || mouseX > listX + listW || mouseY < listY || mouseY > listY + listH) {
            return false;
        }

        ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
        if (cm == null) return false;
        float totalH = cm.listProfiles().size() * 23.0f + 6.0f;
        float maxScroll = Math.max(0.0f, totalH - listH);
        if (maxScroll <= 0.0f) return false;

        targetScrollOffset = AnimationUtil.clamp(targetScrollOffset + (dWheel > 0 ? -20.0f : 20.0f), 0.0f, maxScroll);
        return true;
    }

    private void importFromClipboard(ConfigManager cm) {
        try {
            Object raw = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (!(raw instanceof String)) {
                setStatus("Clipboard does not contain text JSON");
                notifyUser("Import Failed", "Clipboard has no text", NotificationManager.NotificationType.ERROR);
                return;
            }
            String jsonText = ((String) raw).trim();
            if (jsonText.isEmpty()) {
                setStatus("Clipboard is empty");
                notifyUser("Import Failed", "Clipboard is empty", NotificationManager.NotificationType.ERROR);
                return;
            }

            JsonObject parsed = new JsonParser().parse(jsonText).getAsJsonObject();
            if (!parsed.has("modules") || !parsed.get("modules").isJsonObject()) {
                setStatus("Invalid profile JSON (missing 'modules')");
                notifyUser("Import Failed", "Invalid profile JSON", NotificationManager.NotificationType.ERROR);
                return;
            }

            String targetName = sanitizeName(newProfileName);
            if (targetName.isEmpty() && parsed.has("profile_name")) {
                targetName = sanitizeName(parsed.get("profile_name").getAsString());
            }
            if (targetName.isEmpty()) {
                targetName = "imported_" + (System.currentTimeMillis() % 10000L);
            }

            parsed.addProperty("profile_name", targetName);
            File dir = cm.getProfilesDir();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File outFile = new File(dir, targetName + ".json");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)) {
                PRETTY_GSON.toJson(parsed, writer);
            }

            if (cm.load(targetName)) {
                newProfileName = "";
                setStatus("Imported and loaded '" + targetName + "'!");
                notifyUser("Profile Imported", "Loaded '" + targetName + "' from clipboard", NotificationManager.NotificationType.SUCCESS);
            } else {
                setStatus("Saved '" + targetName + "' but failed to apply");
                notifyUser("Import Warning", "Saved but failed to load '" + targetName + "'", NotificationManager.NotificationType.WARNING);
            }
        } catch (Exception e) {
            setStatus("Invalid JSON in clipboard");
            notifyUser("Import Failed", "Clipboard does not contain valid JSON", NotificationManager.NotificationType.ERROR);
        }
    }

    private String sanitizeName(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private void notifyUser(String title, String message, NotificationManager.NotificationType type) {
        if (Cigarette.INSTANCE != null && Cigarette.INSTANCE.getNotificationManager() != null) {
            Cigarette.INSTANCE.getNotificationManager().push(title, message, type, 2500L);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (typing) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (!newProfileName.isEmpty()) {
                    newProfileName = newProfileName.substring(0, newProfileName.length() - 1);
                }
            } else if (keyCode == Keyboard.KEY_RETURN) {
                String sanitized = sanitizeName(newProfileName);
                if (!sanitized.isEmpty() && Cigarette.INSTANCE != null && Cigarette.INSTANCE.getConfigManager() != null) {
                    Cigarette.INSTANCE.getConfigManager().save(sanitized);
                    setStatus("Created profile '" + sanitized + "'");
                    notifyUser("Profile Created", "Saved '" + sanitized + "'", NotificationManager.NotificationType.SUCCESS);
                    newProfileName = "";
                    typing = false;
                }
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                typing = false;
            } else if (Character.isLetterOrDigit(typedChar) || typedChar == '_' || typedChar == '-') {
                if (newProfileName.length() < 20) {
                    newProfileName += typedChar;
                }
            }
        }
    }

    private void setStatus(String msg) {
        this.statusMessage = msg;
        this.statusTimer = System.currentTimeMillis();
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
