package dev.taezeem14.cigarette.desktop;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Pure Java2D vector FontAwesome-style (fa-*) icon renderer for zero-dependency Swing UI and System Tray rendering.
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
        SHIELD,
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
        TERMINAL
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
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0x14, 0x14, 0x18));
            g2.fillRoundRect(0, 0, size, size, Math.max(4, size / 4), Math.max(4, size / 4));
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 190));
            g2.setStroke(new BasicStroke(Math.max(1.2f, size / 14.0f)));
            g2.drawRoundRect(1, 1, size - 3, size - 3, Math.max(4, size / 4), Math.max(4, size / 4));
            FaIcon.of(Glyph.LOGO, (int) Math.round(size * 0.72), accent)
                    .paintIcon(null, g2, (int) Math.round(size * 0.14), (int) Math.round(size * 0.14));
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
                g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(4.0, 4.0, 16.0, 16.0, 42, 276, Arc2D.OPEN));
                g.setColor(new Color(0xFF, 0x57, 0x22));
                g.fill(new Ellipse2D.Double(15.2, 6.2, 4.4, 4.4));
                g.setColor(col);
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
