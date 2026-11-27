package dev.taezeem14.cigarette.desktop;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Pure Java2D vector FontAwesome-style (fa-*) icon renderer for zero-dependency
 * Cigarette V1 Swing UI and System Tray rendering.
 */
public final class FaIcon implements Icon {

    public enum Glyph {
        LOGO,
        GAUGE,
        CUBES,
        LAYER_GROUP,
        ROCKET,
        GEAR,
        CROSSHAIRS,
        BOLT,
        EYE,
        EYE_SLASH,
        WRENCH,
        GLOBE,
        SHIELD,
        SPRINT,
        SHOE,
        GAMEPAD,
        CLOUD_DOWNLOAD,
        SEARCH,
        PALETTE,
        SLIDERS,
        DOWNLOAD,
        FOLDER_OPEN,
        SYNC,
        CHECK,
        WARNING,
        PIN,
        SAVE,
        PLUS,
        TRASH,
        POWER,
        MINUS,
        PLAY,
        TERMINAL,
        KEYBOARD,
        COPY,
        CLOSE,
        CHEVRON_DOWN,
        CHEVRON_LEFT,
        CHEVRON_RIGHT,
        EXTERNAL_LINK,
        INFO,
        CPU,
        RESET,
        SPARKLES
    }

    private final Glyph glyph;
    private final int size;
    private Color color;

    public FaIcon(Glyph glyph, int size, Color color) {
        this.glyph = glyph;
        this.size = Math.max(10, size);
        this.color = color != null ? color : Color.WHITE;
    }

    public static FaIcon of(Glyph glyph, int size, Color color) {
        return new FaIcon(glyph, size, color);
    }

    public void setColor(Color color) {
        this.color = color != null ? color : Color.WHITE;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            double s = size / 24.0;
            g2.scale(s, s);
            g2.setColor(color);
            drawGlyph(g2, glyph, color);
        } finally {
            g2.dispose();
        }
    }

    public static BufferedImage createAppImage(int size, Color accent) {
        Color safeAccent = accent != null ? accent : new Color(0x38, 0x82, 0xF6);
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            int arc = Math.max(5, size / 4);
            g2.setPaint(new GradientPaint(0, 0, new Color(0x1D, 0x1D, 0x26), 0, size, new Color(0x10, 0x10, 0x15)));
            g2.fillRoundRect(0, 0, size, size, arc, arc);

            g2.setColor(new Color(safeAccent.getRed(), safeAccent.getGreen(), safeAccent.getBlue(), 42));
            g2.fillRoundRect(2, 2, size - 4, size - 4, Math.max(3, arc - 2), Math.max(3, arc - 2));

            g2.setColor(new Color(safeAccent.getRed(), safeAccent.getGreen(), safeAccent.getBlue(), 215));
            g2.setStroke(new BasicStroke(Math.max(1.3f, size / 14.0f)));
            g2.drawRoundRect(1, 1, size - 3, size - 3, arc, arc);

            FaIcon.of(Glyph.LOGO, (int) Math.round(size * 0.74), safeAccent)
                    .paintIcon(null, g2, (int) Math.round(size * 0.13), (int) Math.round(size * 0.13));
        } finally {
            g2.dispose();
        }
        return img;
    }

    private static void drawGlyph(Graphics2D g, Glyph glyph, Color col) {
        BasicStroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        BasicStroke thin = new BasicStroke(1.65f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g.setStroke(stroke);

        switch (glyph) {
            case LOGO: {
                AffineTransform saved = g.getTransform();
                g.translate(11.0, 15.0);
                g.rotate(Math.toRadians(-24.0));

                // Stylized Cigarette filter tip in active theme accent
                g.setColor(col);
                g.fill(new RoundRectangle2D.Double(-8.5, -2.1, 4.3, 4.2, 1.6, 1.6));

                // Sleek cigarette barrel
                g.setColor(new Color(0xEC, 0xEC, 0xF3));
                g.fill(new RoundRectangle2D.Double(-3.6, -2.1, 9.0, 4.2, 1.0, 1.0));

                // Glowing ember aura, ember cherry, and hot spark core
                g.setColor(new Color(0xFF, 0x57, 0x22, 95));
                g.fill(new Ellipse2D.Double(4.4, -3.7, 7.4, 7.4));
                g.setColor(new Color(0xFF, 0x57, 0x22));
                g.fill(new RoundRectangle2D.Double(5.8, -2.1, 2.8, 4.2, 1.4, 1.4));
                g.setColor(new Color(0xFF, 0xCA, 0x28));
                g.fill(new Ellipse2D.Double(6.6, -1.2, 1.6, 2.4));

                g.setTransform(saved);

                // Rising smoke curls + floating ember spark
                g.setColor(col);
                g.setStroke(new BasicStroke(1.85f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                GeneralPath wisp1 = new GeneralPath();
                wisp1.moveTo(18.0, 9.2);
                wisp1.curveTo(16.2, 7.2, 19.8, 5.4, 17.5, 2.9);
                g.draw(wisp1);

                g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 170));
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                GeneralPath wisp2 = new GeneralPath();
                wisp2.moveTo(14.8, 9.9);
                wisp2.curveTo(13.4, 8.1, 16.0, 6.5, 14.3, 4.4);
                g.draw(wisp2);

                g.setColor(new Color(0xFF, 0x70, 0x43));
                g.fill(new Ellipse2D.Double(19.9, 6.0, 2.1, 2.1));
                g.setColor(col);
                g.setStroke(stroke);
                break;
            }
            case GAUGE: {
                g.draw(new Arc2D.Double(3.0, 5.0, 18.0, 18.0, 0, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(3.0, 14.0, 21.0, 14.0));
                g.draw(new Line2D.Double(12.0, 14.0, 17.0, 9.0));
                g.fill(new Ellipse2D.Double(10.3, 12.3, 3.4, 3.4));
                break;
            }
            case CUBES: {
                g.setStroke(thin);
                g.draw(new RoundRectangle2D.Double(8.0, 3.0, 8.0, 7.0, 1.5, 1.5));
                g.draw(new RoundRectangle2D.Double(3.0, 12.5, 8.0, 7.5, 1.5, 1.5));
                g.draw(new RoundRectangle2D.Double(13.0, 12.5, 8.0, 7.5, 1.5, 1.5));
                break;
            }
            case LAYER_GROUP: {
                GeneralPath top = diamond(12.0, 6.5, 8.0, 3.2);
                g.fill(top);
                g.setStroke(thin);
                g.draw(chevronLine(4.0, 12.0, 12.0, 15.4, 20.0, 12.0));
                g.draw(chevronLine(4.0, 16.5, 12.0, 19.9, 20.0, 16.5));
                break;
            }
            case ROCKET: {
                GeneralPath body = new GeneralPath();
                body.moveTo(12.0, 3.0);
                body.curveTo(16.5, 5.5, 17.5, 11.0, 15.5, 15.8);
                body.lineTo(8.5, 15.8);
                body.curveTo(6.5, 11.0, 7.5, 5.5, 12.0, 3.0);
                body.closePath();
                g.draw(body);
                g.fill(new Ellipse2D.Double(10.3, 8.2, 3.4, 3.4));
                g.draw(new Line2D.Double(8.2, 13.5, 5.0, 16.5));
                g.draw(new Line2D.Double(15.8, 13.5, 19.0, 16.5));
                g.draw(new Line2D.Double(12.0, 16.0, 12.0, 20.5));
                break;
            }
            case GEAR: {
                g.setStroke(thin);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4.0;
                    g.draw(new Line2D.Double(12.0 + Math.cos(a) * 6.2, 12.0 + Math.sin(a) * 6.2,
                            12.0 + Math.cos(a) * 9.0, 12.0 + Math.sin(a) * 9.0));
                }
                g.setStroke(stroke);
                g.draw(new Ellipse2D.Double(6.8, 6.8, 10.4, 10.4));
                g.draw(new Ellipse2D.Double(9.6, 9.6, 4.8, 4.8));
                break;
            }
            case CROSSHAIRS: {
                g.draw(new Ellipse2D.Double(5.0, 5.0, 14.0, 14.0));
                g.draw(new Line2D.Double(12.0, 2.5, 12.0, 6.5));
                g.draw(new Line2D.Double(12.0, 17.5, 12.0, 21.5));
                g.draw(new Line2D.Double(2.5, 12.0, 6.5, 12.0));
                g.draw(new Line2D.Double(17.5, 12.0, 21.5, 12.0));
                g.fill(new Ellipse2D.Double(10.3, 10.3, 3.4, 3.4));
                break;
            }
            case BOLT: {
                GeneralPath p = new GeneralPath();
                p.moveTo(13.5, 2.5);
                p.lineTo(5.5, 13.0);
                p.lineTo(11.5, 13.0);
                p.lineTo(10.2, 21.5);
                p.lineTo(18.5, 10.5);
                p.lineTo(12.5, 10.5);
                p.closePath();
                g.fill(p);
                break;
            }
            case EYE: {
                GeneralPath eye = new GeneralPath();
                eye.moveTo(2.5, 12.0);
                eye.quadTo(12.0, 4.0, 21.5, 12.0);
                eye.quadTo(12.0, 20.0, 2.5, 12.0);
                g.draw(eye);
                g.fill(new Ellipse2D.Double(9.5, 9.5, 5.0, 5.0));
                break;
            }
            case EYE_SLASH: {
                GeneralPath eye = new GeneralPath();
                eye.moveTo(2.5, 12.0);
                eye.quadTo(12.0, 4.8, 21.5, 12.0);
                eye.quadTo(12.0, 19.2, 2.5, 12.0);
                g.draw(eye);
                g.draw(new Line2D.Double(4.0, 19.0, 20.0, 5.0));
                break;
            }
            case WRENCH: {
                g.draw(new Arc2D.Double(12.5, 3.0, 7.5, 7.5, 35, 270, Arc2D.OPEN));
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(13.0, 11.0, 5.0, 19.0));
                break;
            }
            case GLOBE: {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17.0, 17.0));
                g.setStroke(thin);
                g.draw(new Ellipse2D.Double(8.0, 3.5, 8.0, 17.0));
                g.draw(new Line2D.Double(3.8, 12.0, 20.2, 12.0));
                g.draw(new Line2D.Double(5.2, 8.0, 18.8, 8.0));
                g.draw(new Line2D.Double(5.2, 16.0, 18.8, 16.0));
                break;
            }
            case SHIELD: {
                GeneralPath s = new GeneralPath();
                s.moveTo(12.0, 3.0);
                s.lineTo(19.5, 6.2);
                s.lineTo(19.5, 11.8);
                s.curveTo(19.5, 16.5, 15.5, 19.5, 12.0, 21.2);
                s.curveTo(8.5, 19.5, 4.5, 16.5, 4.5, 11.8);
                s.lineTo(4.5, 6.2);
                s.closePath();
                g.draw(s);
                break;
            }
            case SPRINT: {
                g.setStroke(thin);
                g.draw(new Line2D.Double(2.5, 8.5, 7.2, 8.5));
                g.draw(new Line2D.Double(1.8, 12.5, 6.2, 12.5));
                g.draw(new Line2D.Double(3.0, 16.5, 7.8, 16.5));
                g.setStroke(stroke);
                GeneralPath shoe = new GeneralPath();
                shoe.moveTo(9.5, 7.5);
                shoe.lineTo(13.2, 7.5);
                shoe.lineTo(15.2, 12.2);
                shoe.quadTo(20.5, 12.8, 21.2, 16.8);
                shoe.lineTo(8.5, 16.8);
                shoe.closePath();
                g.draw(shoe);
                break;
            }
            case SHOE: {
                GeneralPath shoe = new GeneralPath();
                shoe.moveTo(6.5, 7.0);
                shoe.lineTo(11.0, 7.0);
                shoe.lineTo(13.5, 12.0);
                shoe.quadTo(19.8, 12.5, 20.5, 17.2);
                shoe.lineTo(5.0, 17.2);
                shoe.lineTo(5.0, 11.0);
                shoe.closePath();
                g.draw(shoe);
                g.setStroke(thin);
                g.draw(new Line2D.Double(5.2, 15.0, 20.2, 15.0));
                break;
            }
            case GAMEPAD: {
                GeneralPath pad = new GeneralPath();
                pad.moveTo(6.8, 7.0);
                pad.lineTo(17.2, 7.0);
                pad.curveTo(20.2, 7.0, 21.5, 13.5, 19.8, 17.2);
                pad.quadTo(18.0, 19.0, 15.8, 16.2);
                pad.lineTo(14.2, 14.5);
                pad.lineTo(9.8, 14.5);
                pad.lineTo(8.2, 16.2);
                pad.quadTo(6.0, 19.0, 4.2, 17.2);
                pad.curveTo(2.5, 13.5, 3.8, 7.0, 6.8, 7.0);
                pad.closePath();
                g.draw(pad);
                g.setStroke(thin);
                g.draw(new Line2D.Double(6.8, 11.0, 10.2, 11.0));
                g.draw(new Line2D.Double(8.5, 9.3, 8.5, 12.7));
                g.fill(new Ellipse2D.Double(14.3, 9.0, 2.1, 2.1));
                g.fill(new Ellipse2D.Double(16.4, 11.0, 2.1, 2.1));
                break;
            }
            case CLOUD_DOWNLOAD: {
                GeneralPath cloud = new GeneralPath();
                cloud.moveTo(6.8, 15.5);
                cloud.curveTo(3.2, 15.5, 2.5, 10.5, 5.8, 9.5);
                cloud.curveTo(6.5, 5.2, 12.2, 4.2, 14.8, 7.2);
                cloud.curveTo(18.8, 6.8, 21.2, 10.2, 19.5, 13.8);
                cloud.quadTo(19.0, 15.5, 17.2, 15.5);
                g.draw(cloud);
                g.draw(new Line2D.Double(12.0, 10.5, 12.0, 19.8));
                g.draw(chevronLine(8.8, 16.6, 12.0, 20.0, 15.2, 16.6));
                break;
            }
            case SEARCH: {
                g.draw(new Ellipse2D.Double(4.0, 4.0, 11.5, 11.5));
                g.draw(new Line2D.Double(14.0, 14.0, 20.0, 20.0));
                break;
            }
            case PALETTE: {
                g.draw(new Arc2D.Double(3.5, 3.5, 17.0, 17.0, 0, 315, Arc2D.OPEN));
                g.fill(new Ellipse2D.Double(7.0, 8.0, 2.6, 2.6));
                g.fill(new Ellipse2D.Double(11.0, 6.2, 2.6, 2.6));
                g.fill(new Ellipse2D.Double(14.8, 8.8, 2.6, 2.6));
                break;
            }
            case SLIDERS: {
                g.setStroke(thin);
                g.draw(new Line2D.Double(4.0, 6.5, 20.0, 6.5));
                g.fill(new Ellipse2D.Double(7.0, 4.5, 4.0, 4.0));
                g.draw(new Line2D.Double(4.0, 12.0, 20.0, 12.0));
                g.fill(new Ellipse2D.Double(13.0, 10.0, 4.0, 4.0));
                g.draw(new Line2D.Double(4.0, 17.5, 20.0, 17.5));
                g.fill(new Ellipse2D.Double(8.5, 15.5, 4.0, 4.0));
                break;
            }
            case DOWNLOAD: {
                g.draw(new Line2D.Double(12.0, 3.5, 12.0, 15.0));
                g.draw(chevronLine(7.5, 10.5, 12.0, 15.2, 16.5, 10.5));
                g.draw(new Line2D.Double(4.5, 19.0, 19.5, 19.0));
                break;
            }
            case FOLDER_OPEN: {
                GeneralPath f = new GeneralPath();
                f.moveTo(3.0, 6.0);
                f.lineTo(9.0, 6.0);
                f.lineTo(11.0, 8.5);
                f.lineTo(20.5, 8.5);
                f.lineTo(20.5, 18.5);
                f.lineTo(3.0, 18.5);
                f.closePath();
                g.draw(f);
                break;
            }
            case SYNC: {
                g.draw(new Arc2D.Double(4.5, 4.5, 15.0, 15.0, 30, 130, Arc2D.OPEN));
                g.draw(new Arc2D.Double(4.5, 4.5, 15.0, 15.0, 210, 130, Arc2D.OPEN));
                g.draw(new Line2D.Double(16.5, 4.5, 19.0, 7.5));
                g.draw(new Line2D.Double(5.0, 16.5, 7.5, 19.5));
                break;
            }
            case CHECK: {
                g.draw(chevronLine(4.5, 12.5, 9.8, 17.5, 19.5, 6.8));
                break;
            }
            case WARNING: {
                GeneralPath tri = new GeneralPath();
                tri.moveTo(12.0, 3.5);
                tri.lineTo(20.8, 19.5);
                tri.lineTo(3.2, 19.5);
                tri.closePath();
                g.draw(tri);
                g.draw(new Line2D.Double(12.0, 9.5, 12.0, 13.5));
                g.fill(new Ellipse2D.Double(10.9, 15.6, 2.2, 2.2));
                break;
            }
            case PIN: {
                g.draw(new Line2D.Double(7.5, 5.0, 16.5, 5.0));
                g.draw(new Line2D.Double(9.0, 5.0, 8.0, 12.5));
                g.draw(new Line2D.Double(15.0, 5.0, 16.0, 12.5));
                g.draw(new Line2D.Double(6.0, 12.5, 18.0, 12.5));
                g.draw(new Line2D.Double(12.0, 12.5, 12.0, 20.5));
                break;
            }
            case SAVE: {
                g.draw(new RoundRectangle2D.Double(4.0, 4.0, 16.0, 16.0, 2.0, 2.0));
                g.draw(new RoundRectangle2D.Double(7.5, 4.0, 7.5, 5.0, 1.0, 1.0));
                g.draw(new RoundRectangle2D.Double(7.0, 13.0, 10.0, 7.0, 1.0, 1.0));
                break;
            }
            case PLUS: {
                g.draw(new Line2D.Double(12.0, 5.0, 12.0, 19.0));
                g.draw(new Line2D.Double(5.0, 12.0, 19.0, 12.0));
                break;
            }
            case TRASH: {
                g.draw(new Line2D.Double(4.5, 6.5, 19.5, 6.5));
                g.draw(new Line2D.Double(9.5, 4.0, 14.5, 4.0));
                g.draw(new RoundRectangle2D.Double(6.5, 6.5, 11.0, 13.5, 1.5, 1.5));
                g.setStroke(thin);
                g.draw(new Line2D.Double(10.0, 10.0, 10.0, 16.5));
                g.draw(new Line2D.Double(14.0, 10.0, 14.0, 16.5));
                break;
            }
            case POWER: {
                g.draw(new Arc2D.Double(5.0, 5.5, 14.0, 14.0, 125, 290, Arc2D.OPEN));
                g.draw(new Line2D.Double(12.0, 3.5, 12.0, 11.5));
                break;
            }
            case MINUS: {
                g.draw(new Line2D.Double(5.0, 12.0, 19.0, 12.0));
                break;
            }
            case PLAY: {
                GeneralPath tri = new GeneralPath();
                tri.moveTo(7.0, 5.0);
                tri.lineTo(19.0, 12.0);
                tri.lineTo(7.0, 19.0);
                tri.closePath();
                g.fill(tri);
                break;
            }
            case TERMINAL: {
                g.draw(new RoundRectangle2D.Double(3.5, 4.5, 17.0, 15.0, 2.5, 2.5));
                g.draw(chevronLine(7.0, 9.0, 10.5, 12.0, 7.0, 15.0));
                g.draw(new Line2D.Double(12.5, 15.0, 16.5, 15.0));
                break;
            }
            case KEYBOARD: {
                g.setStroke(thin);
                g.draw(new RoundRectangle2D.Double(2.5, 6.0, 19.0, 12.0, 2.5, 2.5));
                g.fill(new Ellipse2D.Double(5.5, 9.0, 2.0, 2.0));
                g.fill(new Ellipse2D.Double(9.2, 9.0, 2.0, 2.0));
                g.fill(new Ellipse2D.Double(12.8, 9.0, 2.0, 2.0));
                g.fill(new Ellipse2D.Double(16.5, 9.0, 2.0, 2.0));
                g.draw(new Line2D.Double(7.5, 14.5, 16.5, 14.5));
                break;
            }
            case COPY: {
                g.setStroke(thin);
                g.draw(new RoundRectangle2D.Double(8.5, 8.0, 11.0, 12.0, 2.0, 2.0));
                g.draw(new Line2D.Double(5.0, 15.5, 5.0, 5.5));
                g.draw(new Line2D.Double(5.0, 5.5, 15.0, 5.5));
                break;
            }
            case CLOSE: {
                g.draw(new Line2D.Double(6.0, 6.0, 18.0, 18.0));
                g.draw(new Line2D.Double(18.0, 6.0, 6.0, 18.0));
                break;
            }
            case CHEVRON_DOWN: {
                g.draw(chevronLine(6.0, 9.0, 12.0, 15.5, 18.0, 9.0));
                break;
            }
            case CHEVRON_LEFT: {
                g.draw(chevronLine(15.0, 6.0, 9.0, 12.0, 15.0, 18.0));
                break;
            }
            case CHEVRON_RIGHT: {
                g.draw(chevronLine(9.0, 6.0, 15.0, 12.0, 9.0, 18.0));
                break;
            }
            case EXTERNAL_LINK: {
                g.setStroke(thin);
                GeneralPath box = new GeneralPath();
                box.moveTo(11.0, 6.0);
                box.lineTo(5.0, 6.0);
                box.lineTo(5.0, 19.0);
                box.lineTo(18.0, 19.0);
                box.lineTo(18.0, 13.0);
                g.draw(box);
                g.setStroke(stroke);
                g.draw(new Line2D.Double(11.5, 12.5, 19.5, 4.5));
                g.draw(chevronLine(14.5, 4.5, 19.5, 4.5, 19.5, 9.5));
                break;
            }
            case INFO: {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17.0, 17.0));
                g.fill(new Ellipse2D.Double(10.8, 7.2, 2.4, 2.4));
                g.draw(new Line2D.Double(12.0, 11.5, 12.0, 16.8));
                break;
            }
            case CPU: {
                g.setStroke(thin);
                g.draw(new RoundRectangle2D.Double(6.5, 6.5, 11.0, 11.0, 2.0, 2.0));
                g.draw(new RoundRectangle2D.Double(9.5, 9.5, 5.0, 5.0, 1.0, 1.0));
                g.draw(new Line2D.Double(9.5, 3.5, 9.5, 6.5));
                g.draw(new Line2D.Double(14.5, 3.5, 14.5, 6.5));
                g.draw(new Line2D.Double(9.5, 17.5, 9.5, 20.5));
                g.draw(new Line2D.Double(14.5, 17.5, 14.5, 20.5));
                g.draw(new Line2D.Double(3.5, 9.5, 6.5, 9.5));
                g.draw(new Line2D.Double(3.5, 14.5, 6.5, 14.5));
                g.draw(new Line2D.Double(17.5, 9.5, 20.5, 9.5));
                g.draw(new Line2D.Double(17.5, 14.5, 20.5, 14.5));
                break;
            }
            case RESET: {
                g.draw(new Arc2D.Double(4.5, 4.5, 15.0, 15.0, 40, 280, Arc2D.OPEN));
                g.draw(chevronLine(15.5, 4.0, 18.8, 7.2, 14.5, 8.8));
                break;
            }
            case SPARKLES: {
                g.setStroke(thin);
                g.draw(diamond(11.0, 12.0, 6.0, 6.0));
                g.fill(new Ellipse2D.Double(17.2, 4.5, 3.0, 3.0));
                g.fill(new Ellipse2D.Double(4.5, 16.5, 2.4, 2.4));
                break;
            }
        }
    }

    private static GeneralPath diamond(double cx, double cy, double hw, double hh) {
        GeneralPath p = new GeneralPath();
        p.moveTo(cx, cy - hh);
        p.lineTo(cx + hw, cy);
        p.lineTo(cx, cy + hh);
        p.lineTo(cx - hw, cy);
        p.closePath();
        return p;
    }

    private static GeneralPath chevronLine(double x1, double y1, double x2, double y2, double x3, double y3) {
        GeneralPath p = new GeneralPath();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        return p;
    }
}
