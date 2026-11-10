package dev.taezeem14.cigarette.ui.font;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

public class CFontRenderer extends CFont {

    protected CharData[] boldChars = new CharData[256];
    protected CharData[] italicChars = new CharData[256];
    protected CharData[] boldItalicChars = new CharData[256];
    protected final Map<Character, CharData> boldExtraChars = new HashMap<>();
    protected final Map<Character, CharData> italicExtraChars = new HashMap<>();
    protected final Map<Character, CharData> boldItalicExtraChars = new HashMap<>();

    private final int[] colorCode = new int[32];
    protected DynamicTexture texBold;
    protected DynamicTexture texItalic;
    protected DynamicTexture texBoldItalic;

    public CFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        super(font, antiAlias, fractionalMetrics);
        setupMinecraftColorcodes();
        setupBoldItalicIDs();
    }

    private void setupBoldItalicIDs() {
        texBold = setupTexture(this.font.deriveFont(Font.BOLD), this.antiAlias, this.fractionalMetrics, this.boldChars, this.boldExtraChars);
        texItalic = setupTexture(this.font.deriveFont(Font.ITALIC), this.antiAlias, this.fractionalMetrics, this.italicChars, this.italicExtraChars);
        texBoldItalic = setupTexture(this.font.deriveFont(Font.BOLD | Font.ITALIC), this.antiAlias, this.fractionalMetrics, this.boldItalicChars, this.boldItalicExtraChars);
    }

    public float drawString(String text, double x, double y, int color) {
        return drawString(text, (float) x, (float) y, color, false);
    }

    public float drawString(String text, float x, float y, int color) {
        return drawString(text, x, y, color, false);
    }

    public float drawStringWithShadow(String text, double x, double y, int color) {
        return drawStringWithShadow(text, (float) x, (float) y, color);
    }

    public float drawStringWithShadow(String text, float x, float y, int color) {
        float shadowWidth = drawString(text, x + 0.5f, y + 0.5f, color, true);
        return Math.max(shadowWidth, drawString(text, x, y, color, false));
    }

    public float drawCenteredString(String text, double x, double y, int color) {
        return drawString(text, (float) (x - getStringWidth(text) / 2.0f), (float) y, color, false);
    }

    public float drawCenteredString(String text, float x, float y, int color) {
        return drawString(text, x - getStringWidth(text) / 2.0f, y, color, false);
    }

    public float drawCenteredStringWithShadow(String text, double x, double y, int color) {
        return drawStringWithShadow(text, (float) (x - getStringWidth(text) / 2.0f), (float) y, color);
    }

    public float drawCenteredStringWithShadow(String text, float x, float y, int color) {
        return drawStringWithShadow(text, x - getStringWidth(text) / 2.0f, y, color);
    }

    public float drawString(String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty() || this.tex == null) {
            return 0.0f;
        }

        x -= 1.0f;

        if (color == 0x20FFFFFF) {
            color = 0xFFFFFFFF;
        }
        if ((color & 0xFC000000) == 0) {
            color |= 0xFF000000;
        }

        if (shadow) {
            color = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
        }

        CharData[] currentData = this.charData;
        Map<Character, CharData> currentExtras = this.extraChars;
        float alpha = (float) (color >> 24 & 0xFF) / 255.0f;
        boolean bold = false;
        boolean italic = false;
        boolean strikethrough = false;
        boolean underline = false;

        x *= 2.0f;
        y = (y - 3.0f) * 2.0f;

        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean textureWasEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean alphaWasEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(0.5, 0.5, 0.5);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.enableAlpha();
            GlStateManager.color(
                    (float) (color >> 16 & 0xFF) / 255.0f,
                    (float) (color >> 8 & 0xFF) / 255.0f,
                    (float) (color & 0xFF) / 255.0f,
                    alpha
            );
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(this.tex.getGlTextureId());

            int size = text.length();
            for (int i = 0; i < size; i++) {
                char character = text.charAt(i);
                if (character == '§' && i + 1 < size) {
                    int colorIndex = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
                    if (colorIndex < 16) {
                        bold = false;
                        italic = false;
                        underline = false;
                        strikethrough = false;
                        GlStateManager.bindTexture(this.tex.getGlTextureId());
                        currentData = this.charData;
                        currentExtras = this.extraChars;
                        if (colorIndex < 0) {
                            colorIndex = 15;
                        }
                        if (shadow) {
                            colorIndex += 16;
                        }
                        int col = this.colorCode[colorIndex];
                        GlStateManager.color(
                                (float) (col >> 16 & 0xFF) / 255.0f,
                                (float) (col >> 8 & 0xFF) / 255.0f,
                                (float) (col & 0xFF) / 255.0f,
                                alpha
                        );
                    } else if (colorIndex == 17) {
                        bold = true;
                        if (italic && this.texBoldItalic != null) {
                            GlStateManager.bindTexture(this.texBoldItalic.getGlTextureId());
                            currentData = this.boldItalicChars;
                            currentExtras = this.boldItalicExtraChars;
                        } else if (this.texBold != null) {
                            GlStateManager.bindTexture(this.texBold.getGlTextureId());
                            currentData = this.boldChars;
                            currentExtras = this.boldExtraChars;
                        }
                    } else if (colorIndex == 18) {
                        strikethrough = true;
                    } else if (colorIndex == 19) {
                        underline = true;
                    } else if (colorIndex == 20) {
                        italic = true;
                        if (bold && this.texBoldItalic != null) {
                            GlStateManager.bindTexture(this.texBoldItalic.getGlTextureId());
                            currentData = this.boldItalicChars;
                            currentExtras = this.boldItalicExtraChars;
                        } else if (this.texItalic != null) {
                            GlStateManager.bindTexture(this.texItalic.getGlTextureId());
                            currentData = this.italicChars;
                            currentExtras = this.italicExtraChars;
                        }
                    } else if (colorIndex == 21) {
                        bold = false;
                        italic = false;
                        underline = false;
                        strikethrough = false;
                        GlStateManager.color(
                                (float) (color >> 16 & 0xFF) / 255.0f,
                                (float) (color >> 8 & 0xFF) / 255.0f,
                                (float) (color & 0xFF) / 255.0f,
                                alpha
                        );
                        GlStateManager.bindTexture(this.tex.getGlTextureId());
                        currentData = this.charData;
                        currentExtras = this.extraChars;
                    }
                    i++;
                } else {
                    CharData glyph = resolveGlyph(currentData, currentExtras, character);
                    if (glyph != null) {
                        GL11.glBegin(GL11.GL_TRIANGLES);
                        drawQuad(x, y, (float) glyph.width, (float) glyph.height, (float) glyph.storedX, (float) glyph.storedY, (float) glyph.width, (float) glyph.height);
                        GL11.glEnd();

                        if (strikethrough) {
                            drawLine(x, y + (float) (glyph.height / 2), x + (float) glyph.width - 8.0f, y + (float) (glyph.height / 2), 1.0f);
                        }
                        if (underline) {
                            drawLine(x, y + (float) glyph.height - 2.0f, x + (float) glyph.width - 8.0f, y + (float) glyph.height - 2.0f, 1.0f);
                        }

                        x += glyph.width - 8 + this.charOffset;
                    }
                }
            }
        } finally {
            GlStateManager.bindTexture(0);
            if (!textureWasEnabled) {
                GlStateManager.disableTexture2D();
            } else {
                GlStateManager.enableTexture2D();
            }
            if (!blendWasEnabled) {
                GlStateManager.disableBlend();
            } else {
                GlStateManager.enableBlend();
            }
            if (!alphaWasEnabled) {
                GlStateManager.disableAlpha();
            } else {
                GlStateManager.enableAlpha();
            }
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }
        return x / 2.0f;
    }

    @Override
    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int width = 0;
        CharData[] currentData = this.charData;
        Map<Character, CharData> currentExtras = this.extraChars;
        boolean bold = false;
        boolean italic = false;
        int size = text.length();

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);
            if (character == '§' && i + 1 < size) {
                int colorIndex = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                    currentData = this.charData;
                    currentExtras = this.extraChars;
                } else if (colorIndex == 17) {
                    bold = true;
                    currentData = italic ? this.boldItalicChars : this.boldChars;
                    currentExtras = italic ? this.boldItalicExtraChars : this.boldExtraChars;
                } else if (colorIndex == 20) {
                    italic = true;
                    currentData = bold ? this.boldItalicChars : this.italicChars;
                    currentExtras = bold ? this.boldItalicExtraChars : this.italicExtraChars;
                } else if (colorIndex == 21) {
                    bold = false;
                    italic = false;
                    currentData = this.charData;
                    currentExtras = this.extraChars;
                }
                i++;
            } else {
                CharData glyph = resolveGlyph(currentData, currentExtras, character);
                if (glyph != null) {
                    width += glyph.width - 8 + this.charOffset;
                }
            }
        }
        return width / 2;
    }

    private CharData resolveGlyph(CharData[] table, Map<Character, CharData> extras, char c) {
        if (c < table.length && table[c] != null) {
            return table[c];
        }
        if (extras != null) {
            CharData extra = extras.get(c);
            if (extra != null) {
                return extra;
            }
        }
        char fallback = mapUnsupportedChar(c);
        if (fallback < table.length && table[fallback] != null) {
            return table[fallback];
        }
        return table['?'];
    }

    @Override
    public void drawChar(CharData[] chars, char c, float x, float y) {
        CharData data = resolveGlyph(chars, chars == this.charData ? this.extraChars : null, c);
        if (data == null) return;
        drawQuad(x, y, (float) data.width, (float) data.height, (float) data.storedX, (float) data.storedY, (float) data.width, (float) data.height);
    }

    @Override
    protected void drawQuad(float x, float y, float width, float height, float srcX, float srcY, float srcWidth, float srcHeight) {
        float renderSRCX = srcX / this.imgSize;
        float renderSRCY = srcY / this.imgSize;
        float renderSRCWidth = srcWidth / this.imgSize;
        float renderSRCHeight = srcHeight / this.imgSize;

        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
        GL11.glVertex2d(x + width, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY);
        GL11.glVertex2d(x, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x + width, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
        GL11.glVertex2d(x + width, y);
    }

    private void drawLine(float x, float y, float x1, float y1, float width) {
        GlStateManager.disableTexture2D();
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x1, y1);
        GL11.glEnd();
        GL11.glLineWidth(1.0F);
        GlStateManager.enableTexture2D();
    }

    private void setupMinecraftColorcodes() {
        for (int index = 0; index < 32; ++index) {
            int noClue = (index >> 3 & 1) * 85;
            int red = (index >> 2 & 1) * 170 + noClue;
            int green = (index >> 1 & 1) * 170 + noClue;
            int blue = (index & 1) * 170 + noClue;

            if (index == 6) {
                red += 85;
            }

            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }

            this.colorCode[index] = ((red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
        }
    }
}
