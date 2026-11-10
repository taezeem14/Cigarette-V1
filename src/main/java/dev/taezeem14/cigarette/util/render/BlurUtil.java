package dev.taezeem14.cigarette.util.render;

import dev.taezeem14.cigarette.util.mapping.MappingResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Safe Gaussian backdrop blur using Minecraft's {@code shaders/post/blur.json}.
 * Restores main framebuffer and OpenGL state cleanly and degrades gracefully on unsupported GPUs.
 */
public final class BlurUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ResourceLocation BLUR_SHADER = new ResourceLocation("shaders/post/blur.json");

    private static ShaderGroup blurShader;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    private static boolean shaderUnsupported = false;
    private static Timer cachedTimer;
    private static boolean timerLookupAttempted = false;

    private BlurUtil() {
    }

    public static void cleanup() {
        if (blurShader != null) {
            try {
                blurShader.deleteShaderGroup();
            } catch (Throwable ignored) {
            }
            blurShader = null;
        }
        lastWidth = -1;
        lastHeight = -1;
    }

    private static boolean ensureShaderReady() {
        if (shaderUnsupported || !OpenGlHelper.isFramebufferEnabled() || !OpenGlHelper.areShadersSupported()) {
            return false;
        }
        if (mc.getFramebuffer() == null || mc.getTextureManager() == null || mc.getResourceManager() == null) {
            return false;
        }
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) {
            return false;
        }

        if (blurShader == null || mc.displayWidth != lastWidth || mc.displayHeight != lastHeight) {
            initFboAndShader();
        }
        return blurShader != null;
    }

    private static void initFboAndShader() {
        try {
            if (blurShader != null) {
                blurShader.deleteShaderGroup();
                blurShader = null;
            }
            blurShader = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), BLUR_SHADER);
            blurShader.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
            lastWidth = mc.displayWidth;
            lastHeight = mc.displayHeight;
        } catch (Throwable t) {
            shaderUnsupported = true;
            if (blurShader != null) {
                try {
                    blurShader.deleteShaderGroup();
                } catch (Throwable ignored) {
                }
                blurShader = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Shader> configureUniforms(float radius) {
        if (blurShader == null) {
            return null;
        }
        List<Shader> shaders = MappingResolver.getFieldValue(blurShader, ShaderGroup.class, "listShaders", "field_148031_d");
        if (shaders == null || shaders.isEmpty()) {
            return null;
        }

        float clampedRadius = Math.max(1.0f, Math.min(64.0f, radius));
        for (int i = 0; i < shaders.size(); i++) {
            Shader shader = shaders.get(i);
            if (shader == null) continue;
            ShaderManager manager = shader.getShaderManager();
            if (manager == null) continue;

            ShaderUniform radiusUniform = manager.getShaderUniform("Radius");
            if (radiusUniform != null) {
                radiusUniform.set(clampedRadius);
            }

            ShaderUniform dirUniform = manager.getShaderUniform("BlurDir");
            if (dirUniform != null) {
                if (i == 0) {
                    dirUniform.set(1.0f, 0.0f);
                } else {
                    dirUniform.set(0.0f, 1.0f);
                }
            }
        }
        return shaders;
    }

    private static float getPartialTicks() {
        if (!timerLookupAttempted) {
            timerLookupAttempted = true;
            try {
                Object timerObj = MappingResolver.getFieldValue(mc, mc.getClass(), MappingResolver.MINECRAFT_TIMER);
                if (timerObj instanceof Timer) {
                    cachedTimer = (Timer) timerObj;
                }
            } catch (Throwable ignored) {
            }
        }
        return cachedTimer != null ? cachedTimer.renderPartialTicks : 0.0f;
    }

    public static void blurAll(float radius) {
        if (radius <= 0.5f) {
            return;
        }

        boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean alphaWasEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);

        try {
            if (!ensureShaderReady()) {
                return;
            }
            configureUniforms(radius);
            blurShader.loadShaderGroup(getPartialTicks());
        } catch (Throwable t) {
            shaderUnsupported = true;
        } finally {
            restoreGLState(depthWasEnabled, alphaWasEnabled, blendWasEnabled);
        }
    }

    public static void blurArea(float x, float y, float width, float height, float radius) {
        if (width <= 0.0f || height <= 0.0f || radius <= 0.5f) {
            return;
        }

        boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean alphaWasEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);

        try {
            if (!ensureShaderReady()) {
                return;
            }

            List<Shader> shaders = configureUniforms(radius);
            float partialTicks = getPartialTicks();

            if (shaders != null && shaders.size() >= 2) {
                // Pass 0 writes horizontal blur into the swap target; pad vertically so pass 1 samples valid pixels.
                float pad = Math.min(32.0f, radius);
                RenderUtil.pushScissor(x - pad, y - pad, width + pad * 2.0f, height + pad * 2.0f);
                try {
                    shaders.get(0).loadShader(partialTicks);
                } finally {
                    RenderUtil.popScissor();
                }

                // Pass 1 writes vertical blur back into minecraft:main strictly within the requested bounds.
                RenderUtil.pushScissor(x, y, width, height);
                try {
                    shaders.get(1).loadShader(partialTicks);
                } finally {
                    RenderUtil.popScissor();
                }
            } else {
                RenderUtil.pushScissor(x, y, width, height);
                try {
                    blurShader.loadShaderGroup(partialTicks);
                } finally {
                    RenderUtil.popScissor();
                }
            }
        } catch (Throwable t) {
            shaderUnsupported = true;
        } finally {
            restoreGLState(depthWasEnabled, alphaWasEnabled, blendWasEnabled);
        }
    }

    private static void restoreGLState(boolean depthWasEnabled, boolean alphaWasEnabled, boolean blendWasEnabled) {
        try {
            if (mc.getFramebuffer() != null) {
                mc.getFramebuffer().bindFramebuffer(true);
            }
            if (mc.entityRenderer != null) {
                mc.entityRenderer.setupOverlayRendering();
            }
        } catch (Throwable ignored) {
        }

        GlStateManager.enableTexture2D();
        if (blendWasEnabled) {
            GlStateManager.enableBlend();
        } else {
            GlStateManager.disableBlend();
        }
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        if (alphaWasEnabled) {
            GlStateManager.enableAlpha();
        } else {
            GlStateManager.disableAlpha();
        }

        if (depthWasEnabled) {
            GlStateManager.enableDepth();
        } else {
            GlStateManager.disableDepth();
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
