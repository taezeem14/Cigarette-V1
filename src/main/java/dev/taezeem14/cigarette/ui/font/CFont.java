package dev.taezeem14.cigarette.ui.font;

import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class CFont {

    private static final char[] EXTRA_GLYPHS = {
            '\u2022', // bullet
            '\u2013', // en dash
            '\u2014', // em dash
            '\u2018', // left single quote
            '\u2019', // right single quote
            '\u201C', // left double quote
            '\u201D', // right double quote
            '\u2026', // ellipsis
            '\u25B6', // play / right triangle
            '\u25BC', // down triangle
            '\u25B2', // up triangle
            '\u2713', // check mark
            '\u2715', // multiply / close
            '\u2605', // star
            '\u2764'  // heart
    };

    protected final float imgSize = 512.0f;
    protected CharData[] charData = new CharData[256];
    protected final Map<Character, CharData> extraChars = new HashMap<>();
    protected Font font;
    protected boolean antiAlias;
    protected boolean fractionalMetrics;
    protected int fontHeight = -1;
    protected int charOffset = 0;
    protected DynamicTexture tex;

    public CFont(Font font, boolean antiAlias, boolean fractionalMetrics) {
        this.font = font;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.tex = setupTexture(font, antiAlias, fractionalMetrics, this.charData, this.extraChars);
    }

    protected DynamicTexture setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
        return setupTexture(font, antiAlias, fractionalMetrics, chars, null);
    }

    protected DynamicTexture setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars, Map<Character, CharData> extras) {
        BufferedImage img = generateFontImage(font, antiAlias, fractionalMetrics, chars, extras);
        try {
            return new DynamicTexture(img);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected BufferedImage generateFontImage(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars, Map<Character, CharData> extras) {
        int imageSize = (int) this.imgSize;
        BufferedImage bufferedImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        g.setFont(font);
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, imageSize, imageSize);
        g.setColor(Color.WHITE);

        if (antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        if (fractionalMetrics) {
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        FontMetrics fontMetrics = g.getFontMetrics();
        int charHeight = 0;
        int positionX = 0;
        int positionY = 1;

        for (int i = 0; i < chars.length; i++) {
            char ch = (char) i;
            CharData data = createCharData(g, fontMetrics, ch, imageSize, positionX, positionY, charHeight);
            if (data.wrapped) {
                positionX = 0;
                positionY += charHeight;
                charHeight = 0;
                data.storedX = positionX;
                data.storedY = positionY;
            }
            if (data.height > charHeight) {
                charHeight = data.height;
            }
            if (data.height > this.fontHeight) {
                this.fontHeight = data.height;
            }
            chars[i] = data;
            if (positionY + data.height < imageSize) {
                g.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
            }
            positionX += data.width;
        }

        if (extras != null) {
            extras.clear();
            for (char ch : EXTRA_GLYPHS) {
                CharData data = createCharData(g, fontMetrics, ch, imageSize, positionX, positionY, charHeight);
                if (data.wrapped) {
                    positionX = 0;
                    positionY += charHeight;
                    charHeight = 0;
                    data.storedX = positionX;
                    data.storedY = positionY;
                }
                if (data.height > charHeight) {
                    charHeight = data.height;
                }
                if (data.height > this.fontHeight) {
                    this.fontHeight = data.height;
                }
                extras.put(ch, data);
                if (positionY + data.height < imageSize) {
                    g.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
                }
                positionX += data.width;
            }
        }

        g.dispose();
        return bufferedImage;
    }

    private CharData createCharData(Graphics2D g, FontMetrics fontMetrics, char ch, int imageSize, int positionX, int positionY, int charHeight) {
        CharData data = new CharData();
        Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(ch), g);
        data.width = Math.max(8, dimensions.getBounds().width + 8);
        data.height = Math.max(8, dimensions.getBounds().height);

        if (positionX + data.width >= imageSize) {
            data.wrapped = true;
            data.storedX = 0;
            data.storedY = positionY + charHeight;
        } else {
            data.wrapped = false;
            data.storedX = positionX;
            data.storedY = positionY;
        }
        return data;
    }

    protected CharData resolveCharData(CharData[] primary, char c) {
        if (c < primary.length && primary[c] != null) {
            return primary[c];
        }
        if (primary == this.charData) {
            CharData extra = extraChars.get(c);
            if (extra != null) {
                return extra;
            }
        }
        char mapped = mapUnsupportedChar(c);
        if (mapped < primary.length && primary[mapped] != null) {
            return primary[mapped];
        }
        return primary['?'];
    }

    protected static char mapUnsupportedChar(char c) {
        switch (c) {
            case '\u2022':
            case '\u25CF':
            case '\u2219':
                return '\u00B7';
            case '\u2013':
            case '\u2014':
                return '-';
            case '\u2018':
            case '\u2019':
                return '\'';
            case '\u201C':
            case '\u201D':
                return '"';
            case '\u2026':
                return '.';
            case '\u25B6':
            case '\u25BA':
                return '>';
            case '\u25BC':
                return 'v';
            case '\u25B2':
                return '^';
            default:
                return '?';
        }
    }

    public void drawChar(CharData[] chars, char c, float x, float y) {
        CharData data = resolveCharData(chars, c);
        if (data == null) return;
        drawQuad(x, y, data.width, data.height, data.storedX, data.storedY, data.width, data.height);
    }

    protected void drawQuad(float x, float y, float width, float height, float srcX, float srcY, float srcWidth, float srcHeight) {
        float renderSRCX = srcX / this.imgSize;
        float renderSRCY = srcY / this.imgSize;
        float renderSRCWidth = srcWidth / this.imgSize;
        float renderSRCHeight = srcHeight / this.imgSize;

        GL11.glTexCoord2f(renderSRCX, renderSRCY);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
        GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY + renderSRCHeight);
        GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
        GL11.glVertex2f(x + width, y);
    }

    public int getStringHeight(String text) {
        return getHeight();
    }

    public int getHeight() {
        return Math.max(4, (this.fontHeight - 8) / 2);
    }

    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            CharData data = resolveCharData(this.charData, text.charAt(i));
            if (data != null) {
                width += data.width - 8 + this.charOffset;
            }
        }
        return width / 2;
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public void setAntiAlias(boolean antiAlias) {
        if (this.antiAlias != antiAlias) {
            this.antiAlias = antiAlias;
            this.tex = setupTexture(this.font, antiAlias, this.fractionalMetrics, this.charData, this.extraChars);
        }
    }

    public boolean isFractionalMetrics() {
        return fractionalMetrics;
    }

    public void setFractionalMetrics(boolean fractionalMetrics) {
        if (this.fractionalMetrics != fractionalMetrics) {
            this.fractionalMetrics = fractionalMetrics;
            this.tex = setupTexture(this.font, this.antiAlias, fractionalMetrics, this.charData, this.extraChars);
        }
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        this.tex = setupTexture(font, this.antiAlias, this.fractionalMetrics, this.charData, this.extraChars);
    }

    public static class CharData {
        public int width;
        public int height;
        public int storedX;
        public int storedY;
        boolean wrapped;
    }
}
