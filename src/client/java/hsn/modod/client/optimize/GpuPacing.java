package hsn.modod.client.optimize;

import com.mojang.blaze3d.opengl.GlStateManager;
import hsn.modod.config.HSNConfig;
import org.lwjgl.opengl.GL11C;

import java.util.Locale;

public final class GpuPacing {

    private static volatile boolean scanned;
    private static volatile boolean unevenOnImmutable;

    private GpuPacing() {
    }

    public static boolean shouldUseMutableBuffers() {
        if (!HSNConfig.get().framePacingFixEnabled) {
            return false;
        }
        if (!scanned) {
            scanOnce();
        }
        return unevenOnImmutable;
    }

    private static synchronized void scanOnce() {
        if (scanned) {
            return;
        }

        try {
            String renderer = getGlString(7937); // GL_RENDERER
            String vendor = getGlString(7936);   // GL_VENDOR

            unevenOnImmutable = isLegacyIntelHd(renderer, vendor);
        } catch (Throwable ignored) {
            // Safe fallback if queried before GL context is ready
            unevenOnImmutable = false;
        } finally {
            scanned = true; // Always set to true to prevent per-frame GL overhead
        }
    }

    private static String getGlString(int pname) {
        String result = "";
        try {
            result = GlStateManager._getString(pname);
        } catch (Throwable ignored) {
            // Direct LWJGL fallback if GlStateManager isn't ready
            result = GL11C.glGetString(pname);
        }
        return lower(result);
    }

    private static boolean isLegacyIntelHd(String renderer, String vendor) {
        // Broad Intel detection covering Windows (Intel) and Linux/Mesa (freedesktop/Mesa) drivers
        boolean isIntelDriver = vendor.contains("intel") 
                || vendor.contains("mesa") 
                || vendor.contains("freedesktop") 
                || renderer.contains("intel");

        if (!isIntelDriver) {
            return false;
        }

        // Target ONLY broken Gen6–Gen7 Intel HD GPUs (HD 2000, 3000, 2500, 4000, Bay Trail)
        // Avoid matching "HD Graphics 500+", "UHD Graphics", "Iris Xe", or "Arc"
        return renderer.contains("hd graphics 2000")
                || renderer.contains("hd graphics 3000")
                || renderer.contains("hd graphics 2500")
                || renderer.contains("hd graphics 4000")
                || renderer.contains("hd graphics (byt)")
                || (renderer.contains("hd graphics") && !renderer.matches(".*hd graphics \\d{3,}.*"));
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
