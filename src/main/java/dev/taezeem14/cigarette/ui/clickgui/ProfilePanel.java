package dev.taezeem14.cigarette.ui.clickgui;

import dev.taezeem14.cigarette.Cigarette;
import dev.taezeem14.cigarette.config.ConfigManager;
import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import org.lwjgl.input.Keyboard;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class ProfilePanel {

    private float x;
    private float y;
    private float width = 320.0f;
    private float height = 240.0f;

    private String newProfileName = "";
    private boolean typing = false;
    private String statusMessage = "Manage and switch client profiles";
    private long statusTimer = 0L;

    public ProfilePanel(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void render(float mouseX, float mouseY, float partialTicks) {
        // Main panel shadow & background
        RenderUtil.drawDropShadow(x, y, width, height, 6.0f, 0x66000000);
        RenderUtil.drawRoundedRect(x, y, width, height, 5.0f, ColorUtil.BACKGROUND);
        RenderUtil.drawRoundedOutline(x, y, width, height, 5.0f, 1.0f, 0xFF25252C);

        // Header
        float headerH = 26.0f;
        RenderUtil.drawRoundedRect(x, y, width, headerH, 5.0f, ColorUtil.HEADER);
        RenderUtil.drawRect(x, y + headerH - 1.0f, width, 1.0f, 0xFF2A2A32);
        FontUtil.bold.drawString("Profiles & Cloud Configs", x + 10.0f, y + (headerH - FontUtil.bold.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

        // Active profile indicator
        ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
        String activeName = (cm != null && cm.getActiveProfile() != null) ? cm.getActiveProfile().getName() : "default";
        String activeLabel = "Active: §9" + activeName;
        FontUtil.small.drawString(activeLabel, x + width - FontUtil.small.getStringWidth(activeLabel) - 10.0f, y + (headerH - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_MUTED);

        // New profile input row
        float inputY = y + headerH + 8.0f;
        float inputW = 160.0f;
        float inputH = 18.0f;
        float inputX = x + 10.0f;

        RenderUtil.drawRoundedRect(inputX, inputY, inputW, inputH, 3.0f, 0xFF19191D);
        RenderUtil.drawRoundedOutline(inputX, inputY, inputW, inputH, 3.0f, 1.0f, typing ? ColorUtil.ACCENT : 0xFF2A2A32);

        String displayText = newProfileName.isEmpty() && !typing ? "Enter profile name..." : newProfileName + (typing && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
        int textCol = newProfileName.isEmpty() && !typing ? 0xFF666670 : ColorUtil.TEXT_PRIMARY;
        FontUtil.small.drawString(displayText, inputX + 6.0f, inputY + (inputH - FontUtil.small.getHeight()) / 2.0f, textCol);

        // "Create" button
        float createBtnX = inputX + inputW + 6.0f;
        float createBtnW = 55.0f;
        boolean createHovered = mouseX >= createBtnX && mouseX <= createBtnX + createBtnW && mouseY >= inputY && mouseY <= inputY + inputH;
        RenderUtil.drawRoundedRect(createBtnX, inputY, createBtnW, inputH, 3.0f, createHovered ? ColorUtil.ACCENT : 0xFF25252C);
        FontUtil.small.drawCenteredString("Create", createBtnX + createBtnW / 2.0f, inputY + (inputH - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

        // "Save Current" button
        float saveBtnX = createBtnX + createBtnW + 6.0f;
        float saveBtnW = 65.0f;
        boolean saveHovered = mouseX >= saveBtnX && mouseX <= saveBtnX + saveBtnW && mouseY >= inputY && mouseY <= inputY + inputH;
        RenderUtil.drawRoundedRect(saveBtnX, inputY, saveBtnW, inputH, 3.0f, saveHovered ? 0xFF2E7D32 : 0xFF202A22);
        FontUtil.small.drawCenteredString("Save Active", saveBtnX + saveBtnW / 2.0f, inputY + (inputH - FontUtil.small.getHeight()) / 2.0f, 0xFF81C784);

        // Profile List Box
        float listY = inputY + inputH + 8.0f;
        float listW = width - 20.0f;
        float listH = 140.0f;
        float listX = x + 10.0f;

        RenderUtil.drawRoundedRect(listX, listY, listW, listH, 3.0f, 0xFF161619);
        RenderUtil.drawRoundedOutline(listX, listY, listW, listH, 3.0f, 1.0f, 0xFF222228);

        if (cm != null) {
            List<String> profiles = cm.listProfiles();
            float itemY = listY + 4.0f;
            for (String profile : profiles) {
                if (itemY + 20.0f > listY + listH) break;

                boolean isCurrent = profile.equalsIgnoreCase(activeName);
                boolean itemHovered = mouseX >= listX + 4.0f && mouseX <= listX + listW - 4.0f && mouseY >= itemY && mouseY <= itemY + 20.0f;

                RenderUtil.drawRoundedRect(listX + 4.0f, itemY, listW - 8.0f, 20.0f, 2.5f, itemHovered ? 0xFF202026 : 0xFF19191E);

                // Profile Name
                int nameCol = isCurrent ? ColorUtil.ACCENT : ColorUtil.TEXT_PRIMARY;
                FontUtil.regular.drawString(profile + (isCurrent ? " §7(Active)" : ""), listX + 8.0f, itemY + (20.0f - FontUtil.regular.getHeight()) / 2.0f, nameCol);

                // Load button
                float loadBtnW = 38.0f;
                float loadBtnX = listX + listW - 8.0f - loadBtnW - 40.0f - 40.0f;
                boolean loadHov = mouseX >= loadBtnX && mouseX <= loadBtnX + loadBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f;
                RenderUtil.drawRoundedRect(loadBtnX, itemY + 2.0f, loadBtnW, 16.0f, 2.0f, loadHov ? ColorUtil.ACCENT : 0xFF25252E);
                FontUtil.small.drawCenteredString("Load", loadBtnX + loadBtnW / 2.0f, itemY + 2.0f + (16.0f - FontUtil.small.getHeight()) / 2.0f, ColorUtil.TEXT_PRIMARY);

                // Export button
                float expBtnW = 38.0f;
                float expBtnX = loadBtnX + loadBtnW + 4.0f;
                boolean expHov = mouseX >= expBtnX && mouseX <= expBtnX + expBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f;
                RenderUtil.drawRoundedRect(expBtnX, itemY + 2.0f, expBtnW, 16.0f, 2.0f, expHov ? 0xFF0288D1 : 0xFF1C2833);
                FontUtil.small.drawCenteredString("Copy", expBtnX + expBtnW / 2.0f, itemY + 2.0f + (16.0f - FontUtil.small.getHeight()) / 2.0f, 0xFF4FC3F7);

                // Delete button
                if (!profile.equalsIgnoreCase("default")) {
                    float delBtnW = 32.0f;
                    float delBtnX = expBtnX + expBtnW + 4.0f;
                    boolean delHov = mouseX >= delBtnX && mouseX <= delBtnX + delBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f;
                    RenderUtil.drawRoundedRect(delBtnX, itemY + 2.0f, delBtnW, 16.0f, 2.0f, delHov ? 0xFFC62828 : 0xFF2C1E1E);
                    FontUtil.small.drawCenteredString("Del", delBtnX + delBtnW / 2.0f, itemY + 2.0f + (16.0f - FontUtil.small.getHeight()) / 2.0f, 0xFFEF9A9A);
                }

                itemY += 23.0f;
            }
        }

        // Status bar
        float statusY = listY + listH + 4.0f;
        FontUtil.small.drawString(statusMessage, x + 12.0f, statusY, (System.currentTimeMillis() - statusTimer < 3000L) ? ColorUtil.EMBER_ORANGE : ColorUtil.TEXT_MUTED);
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        float headerH = 26.0f;
        float inputY = y + headerH + 8.0f;
        float inputW = 160.0f;
        float inputH = 18.0f;
        float inputX = x + 10.0f;

        // Input field click
        if (mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + inputH) {
            typing = true;
            return;
        } else {
            typing = false;
        }

        ConfigManager cm = Cigarette.INSTANCE != null ? Cigarette.INSTANCE.getConfigManager() : null;
        if (cm == null) return;

        // Create button click
        float createBtnX = inputX + inputW + 6.0f;
        float createBtnW = 55.0f;
        if (mouseX >= createBtnX && mouseX <= createBtnX + createBtnW && mouseY >= inputY && mouseY <= inputY + inputH) {
            if (!newProfileName.trim().isEmpty()) {
                cm.save(newProfileName.trim());
                setStatus("Created and saved profile '" + newProfileName.trim() + "'");
                newProfileName = "";
            }
            return;
        }

        // Save active button click
        float saveBtnX = createBtnX + createBtnW + 6.0f;
        float saveBtnW = 65.0f;
        if (mouseX >= saveBtnX && mouseX <= saveBtnX + saveBtnW && mouseY >= inputY && mouseY <= inputY + inputH) {
            String active = cm.getActiveProfile() != null ? cm.getActiveProfile().getName() : "default";
            cm.save(active);
            setStatus("Saved active profile '" + active + "'");
            return;
        }

        // List interactions
        float listY = inputY + inputH + 8.0f;
        float listW = width - 20.0f;
        float listH = 140.0f;
        float listX = x + 10.0f;

        List<String> profiles = cm.listProfiles();
        float itemY = listY + 4.0f;
        for (String profile : profiles) {
            if (itemY + 20.0f > listY + listH) break;

            float loadBtnW = 38.0f;
            float loadBtnX = listX + listW - 8.0f - loadBtnW - 40.0f - 40.0f;
            if (mouseX >= loadBtnX && mouseX <= loadBtnX + loadBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f) {
                cm.load(profile);
                setStatus("Loaded profile '" + profile + "'");
                return;
            }

            float expBtnW = 38.0f;
            float expBtnX = loadBtnX + loadBtnW + 4.0f;
            if (mouseX >= expBtnX && mouseX <= expBtnX + expBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f) {
                try {
                    File f = new File(cm.getProfilesDir(), profile + ".json");
                    if (f.exists()) {
                        String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
                        setStatus("Copied '" + profile + "' JSON to clipboard!");
                    }
                } catch (Exception e) {
                    setStatus("Error copying config to clipboard");
                }
                return;
            }

            if (!profile.equalsIgnoreCase("default")) {
                float delBtnW = 32.0f;
                float delBtnX = expBtnX + expBtnW + 4.0f;
                if (mouseX >= delBtnX && mouseX <= delBtnX + delBtnW && mouseY >= itemY + 2.0f && mouseY <= itemY + 18.0f) {
                    cm.delete(profile);
                    setStatus("Deleted profile '" + profile + "'");
                    return;
                }
            }

            itemY += 23.0f;
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (typing) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (!newProfileName.isEmpty()) {
                    newProfileName = newProfileName.substring(0, newProfileName.length() - 1);
                }
            } else if (keyCode == Keyboard.KEY_RETURN) {
                if (!newProfileName.trim().isEmpty() && Cigarette.INSTANCE != null) {
                    Cigarette.INSTANCE.getConfigManager().save(newProfileName.trim());
                    setStatus("Created profile '" + newProfileName.trim() + "'");
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
