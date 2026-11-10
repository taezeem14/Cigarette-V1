package dev.taezeem14.cigarette.ui.hud;

import dev.taezeem14.cigarette.ui.font.FontUtil;
import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages animated bottom-right toast notifications with progress bars and theme integration.
 */
public class NotificationManager {

    public enum NotificationType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    private static final long ANIMATION_IN_MS = 260L;
    private static final long ANIMATION_OUT_MS = 240L;
    private static final int MAX_NOTIFICATIONS = 6;

    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public void push(String title, String message, NotificationType type, long durationMs) {
        if (title == null) title = "";
        if (message == null) message = "";
        if (type == null) type = NotificationType.INFO;
        if (durationMs < 500L) durationMs = 500L;

        while (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }

        notifications.add(new Notification(title, message, type, durationMs));
    }

    public void push(String title, String message, NotificationType type) {
        push(title, message, type, 2500L);
    }

    public void clear() {
        notifications.clear();
    }

    public void render(float screenW, float screenH) {
        if (notifications.isEmpty()) {
            return;
        }

        FontUtil.ensureInitialized();
        long now = System.currentTimeMillis();
        float bottomMargin = 14.0f;
        float rightMargin = 10.0f;
        float spacing = 6.0f;
        float currentTargetY = screenH - bottomMargin;

        // Prune expired notifications first
        Iterator<Notification> it = notifications.iterator();
        while (it.hasNext()) {
            Notification n = it.next();
            if (now - n.createdAt >= n.durationMs + ANIMATION_OUT_MS) {
                notifications.remove(n);
            }
        }

        // Render newest at bottom, stacking upwards
        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification n = notifications.get(i);
            long elapsed = now - n.createdAt;

            boolean hasMessage = !n.message.isEmpty();
            float titleW = FontUtil.bold.getStringWidth(n.title);
            float msgW = hasMessage ? FontUtil.small.getStringWidth(n.message) : 0.0f;
            float cardW = Math.max(140.0f, Math.min(250.0f, Math.max(titleW, msgW) + 30.0f));
            float cardH = hasMessage ? 31.0f : 22.0f;

            currentTargetY -= cardH;
            if (!n.initializedY) {
                n.renderY = currentTargetY + 12.0f;
                n.initializedY = true;
            } else {
                n.renderY = AnimationUtil.lerp(n.renderY, currentTargetY, 15.0f);
            }

            float slideProgress;
            float alphaFactor;
            if (elapsed < ANIMATION_IN_MS) {
                float t = (float) elapsed / (float) ANIMATION_IN_MS;
                slideProgress = AnimationUtil.easeOutCubic(t);
                alphaFactor = slideProgress;
            } else if (elapsed > n.durationMs) {
                float outT = (float) (elapsed - n.durationMs) / (float) ANIMATION_OUT_MS;
                slideProgress = 1.0f - AnimationUtil.easeOutCubic(outT);
                alphaFactor = Math.max(0.0f, 1.0f - outT);
            } else {
                slideProgress = 1.0f;
                alphaFactor = 1.0f;
            }

            float targetX = screenW - cardW - rightMargin;
            float hiddenX = screenW + 12.0f;
            float cardX = hiddenX + (targetX - hiddenX) * slideProgress;
            float cardY = n.renderY;

            int accentColor = resolveTypeColor(n.type);
            int alphaByte = Math.max(0, Math.min(255, (int) (240 * alphaFactor)));
            int shadowAlpha = Math.max(0, Math.min(255, (int) (90 * alphaFactor)));
            int bgColor = ColorUtil.modifyAlpha(0xFF161619, alphaByte);
            int borderColor = ColorUtil.modifyAlpha(ColorUtil.BORDER, alphaByte);
            int accentAlpha = ColorUtil.modifyAlpha(accentColor, Math.max(0, Math.min(255, (int) (255 * alphaFactor))));
            int titleColor = ColorUtil.modifyAlpha(ColorUtil.TEXT_PRIMARY, Math.max(0, Math.min(255, (int) (255 * alphaFactor))));
            int msgColor = ColorUtil.modifyAlpha(ColorUtil.TEXT_MUTED, Math.max(0, Math.min(255, (int) (255 * alphaFactor))));

            // Drop shadow and card body
            RenderUtil.drawDropShadow(cardX, cardY, cardW, cardH, 4.0f, shadowAlpha << 24);
            RenderUtil.drawRoundedRect(cardX, cardY, cardW, cardH, 4.0f, bgColor);
            RenderUtil.drawRoundedOutline(cardX, cardY, cardW, cardH, 4.0f, 1.0f, borderColor);

            // Left accent pill
            RenderUtil.drawRoundedRect(cardX + 4.0f, cardY + 5.0f, 2.5f, cardH - 11.0f, 1.25f, accentAlpha);

            // Status dot & texts
            float textX = cardX + 12.0f;
            if (hasMessage) {
                FontUtil.bold.drawString(n.title, textX, cardY + 5.0f, titleColor);
                FontUtil.small.drawString(n.message, textX, cardY + 17.5f, msgColor);
            } else {
                FontUtil.bold.drawString(n.title, textX, cardY + (cardH - FontUtil.bold.getHeight()) / 2.0f - 1.0f, titleColor);
            }

            // Progress timer bar along bottom
            float timeRemaining = 1.0f - Math.max(0.0f, Math.min(1.0f, (float) elapsed / (float) n.durationMs));
            float barTrackX = cardX + 3.0f;
            float barTrackY = cardY + cardH - 2.5f;
            float barTrackW = cardW - 6.0f;
            RenderUtil.drawRoundedRect(barTrackX, barTrackY, barTrackW, 1.5f, 0.75f, ColorUtil.modifyAlpha(0xFF26262E, alphaByte));
            if (timeRemaining > 0.01f) {
                RenderUtil.drawRoundedRect(barTrackX, barTrackY, barTrackW * timeRemaining, 1.5f, 0.75f, accentAlpha);
            }

            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            currentTargetY -= spacing;
        }
    }

    private int resolveTypeColor(NotificationType type) {
        switch (type) {
            case SUCCESS:
                return 0xFF10B981;
            case WARNING:
                return 0xFFF59E0B;
            case ERROR:
                return 0xFFEF4444;
            case INFO:
            default:
                return ColorUtil.getAccent();
        }
    }

    private static final class Notification {
        final String title;
        final String message;
        final NotificationType type;
        final long durationMs;
        final long createdAt;
        float renderY;
        boolean initializedY = false;

        Notification(String title, String message, NotificationType type, long durationMs) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.durationMs = durationMs;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
