package dev.taezeem14.cigarette.ui.hud;

import dev.taezeem14.cigarette.util.render.AnimationUtil;
import dev.taezeem14.cigarette.util.render.ColorUtil;
import dev.taezeem14.cigarette.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

    public void clear() {
        notifications.clear();
    }

    public void render(GuiGraphicsExtractor g, float screenW, float screenH) {
        if (notifications.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<Notification> it = notifications.iterator();
        while (it.hasNext()) {
            Notification n = it.next();
            if (now - n.createdAt >= n.durationMs + ANIMATION_OUT_MS) {
                notifications.remove(n);
            }
        }

        float bottomMargin = 14.0F;
        float rightMargin = 10.0F;
        float spacing = 6.0F;
        float currentTargetY = screenH - bottomMargin;

        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification n = notifications.get(i);
            long elapsed = now - n.createdAt;
            boolean hasMessage = !n.message.isEmpty();
            float titleW = RenderUtil.getStringWidth(n.title);
            float msgW = hasMessage ? RenderUtil.getSmallStringWidth(n.message) : 0.0F;
            float cardW = Math.max(140.0F, Math.min(250.0F, Math.max(titleW, msgW) + 30.0F));
            float cardH = hasMessage ? 31.0F : 22.0F;

            currentTargetY -= cardH;
            if (!n.initializedY) {
                n.renderY = currentTargetY + 12.0F;
                n.initializedY = true;
            } else {
                n.renderY = AnimationUtil.lerp(n.renderY, currentTargetY, 15.0F);
            }

            float slideProgress;
            float alphaFactor;
            if (elapsed < ANIMATION_IN_MS) {
                float t = (float) elapsed / (float) ANIMATION_IN_MS;
                slideProgress = AnimationUtil.easeOutCubic(t);
                alphaFactor = slideProgress;
            } else if (elapsed > n.durationMs) {
                float outT = (float) (elapsed - n.durationMs) / (float) ANIMATION_OUT_MS;
                slideProgress = 1.0F - AnimationUtil.easeOutCubic(outT);
                alphaFactor = Math.max(0.0F, 1.0F - outT);
            } else {
                slideProgress = 1.0F;
                alphaFactor = 1.0F;
            }

            float targetX = screenW - cardW - rightMargin;
            float hiddenX = screenW + 12.0F;
            float cardX = hiddenX + (targetX - hiddenX) * slideProgress;
            float cardY = n.renderY;

            int accentColor = resolveTypeColor(n.type);
            int alphaByte = Math.max(0, Math.min(255, (int) (240 * alphaFactor)));
            int shadowAlpha = Math.max(0, Math.min(255, (int) (90 * alphaFactor)));
            int accentAlpha = ColorUtil.modifyAlpha(accentColor, Math.max(0, Math.min(255, (int) (255 * alphaFactor))));
            int titleColor = ColorUtil.modifyAlpha(ColorUtil.TEXT_PRIMARY, Math.max(0, Math.min(255, (int) (255 * alphaFactor))));
            int msgColor = ColorUtil.modifyAlpha(ColorUtil.TEXT_MUTED, Math.max(0, Math.min(255, (int) (255 * alphaFactor))));

            RenderUtil.drawDropShadow(g, cardX, cardY, cardW, cardH, 4.0F, shadowAlpha << 24);
            RenderUtil.drawRoundedRect(g, cardX, cardY, cardW, cardH, 4.0F, ColorUtil.modifyAlpha(0xFF161619, alphaByte));
            RenderUtil.drawRoundedOutline(g, cardX, cardY, cardW, cardH, 4.0F, 1.0F, ColorUtil.modifyAlpha(ColorUtil.BORDER, alphaByte));
            RenderUtil.drawRoundedRect(g, cardX + 4.0F, cardY + 5.0F, 2.5F, cardH - 11.0F, 1.25F, accentAlpha);

            float textX = cardX + 12.0F;
            if (hasMessage) {
                RenderUtil.drawText(g, n.title, textX, cardY + 5.0F, titleColor, false);
                RenderUtil.drawSmallText(g, n.message, textX, cardY + 17.5F, msgColor, false);
            } else {
                RenderUtil.drawText(g, n.title, textX, cardY + (cardH - RenderUtil.getFontHeight()) / 2.0F, titleColor, false);
            }

            float remaining = 1.0F - Math.max(0.0F, Math.min(1.0F, (float) elapsed / (float) n.durationMs));
            float barX = cardX + 3.0F;
            float barY = cardY + cardH - 2.5F;
            float barW = cardW - 6.0F;
            RenderUtil.drawRoundedRect(g, barX, barY, barW, 1.5F, 0.75F, ColorUtil.modifyAlpha(0xFF26262E, alphaByte));
            if (remaining > 0.01F) {
                RenderUtil.drawRoundedRect(g, barX, barY, barW * remaining, 1.5F, 0.75F, accentAlpha);
            }

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
        boolean initializedY;

        Notification(String title, String message, NotificationType type, long durationMs) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.durationMs = durationMs;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
