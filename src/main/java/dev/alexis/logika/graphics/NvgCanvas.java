package dev.alexis.logika.graphics;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;
import static org.lwjgl.nanovg.NanoVG.nvgBeginPath;
import static org.lwjgl.nanovg.NanoVG.nvgBezierTo;
import static org.lwjgl.nanovg.NanoVG.nvgCircle;
import static org.lwjgl.nanovg.NanoVG.nvgCreateFont;
import static org.lwjgl.nanovg.NanoVG.nvgFill;
import static org.lwjgl.nanovg.NanoVG.nvgFillColor;
import static org.lwjgl.nanovg.NanoVG.nvgFontFace;
import static org.lwjgl.nanovg.NanoVG.nvgFontSize;
import static org.lwjgl.nanovg.NanoVG.nvgLineTo;
import static org.lwjgl.nanovg.NanoVG.nvgMoveTo;
import static org.lwjgl.nanovg.NanoVG.nvgRGBA;
import static org.lwjgl.nanovg.NanoVG.nvgRect;
import static org.lwjgl.nanovg.NanoVG.nvgRestore;
import static org.lwjgl.nanovg.NanoVG.nvgRoundedRect;
import static org.lwjgl.nanovg.NanoVG.nvgSave;
import static org.lwjgl.nanovg.NanoVG.nvgStroke;
import static org.lwjgl.nanovg.NanoVG.nvgStrokeColor;
import static org.lwjgl.nanovg.NanoVG.nvgStrokeWidth;
import static org.lwjgl.nanovg.NanoVG.nvgText;
import static org.lwjgl.nanovg.NanoVG.nvgTextAlign;

final class NvgCanvas {
    private final long vg;
    private boolean fontLoaded;
    private boolean boldFontLoaded;

    NvgCanvas(long vg) {
        this.vg = vg;
    }

    void loadFonts() {
        fontLoaded = loadFont("sans", System.getenv("LOGIKA_FONT"),
                "/usr/share/fonts/noto/NotoSans-Regular.ttf",
                "/usr/share/fonts/TTF/Inter-Regular.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");
        boldFontLoaded = loadFont("sans-bold", System.getenv("LOGIKA_FONT_BOLD"),
                "/usr/share/fonts/noto/NotoSans-Bold.ttf",
                "/usr/share/fonts/TTF/Inter-SemiBold.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf");
    }

    void fillRect(double x, double y, double width, double height, RenderTheme.Rgba color) {
        nvgBeginPath(vg);
        nvgRect(vg, (float) x, (float) y, (float) width, (float) height);
        fill(color);
    }

    void fillRound(double x, double y, double width, double height, double radius, RenderTheme.Rgba color) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, (float) x, (float) y, (float) width, (float) height, (float) radius);
        fill(color);
    }

    void strokeRound(double x, double y, double width, double height, double radius, RenderTheme.Rgba color, float strokeWidth) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, (float) x, (float) y, (float) width, (float) height, (float) radius);
        stroke(color, strokeWidth);
    }

    void circle(double x, double y, double radius, RenderTheme.Rgba color) {
        nvgBeginPath(vg);
        nvgCircle(vg, (float) x, (float) y, (float) radius);
        fill(color);
    }

    void line(double x1, double y1, double x2, double y2, RenderTheme.Rgba color, float strokeWidth) {
        nvgBeginPath(vg);
        nvgMoveTo(vg, (float) x1, (float) y1);
        nvgLineTo(vg, (float) x2, (float) y2);
        stroke(color, strokeWidth);
    }

    void bezier(double x1, double y1, double x2, double y2, double control, RenderTheme.Rgba color, float strokeWidth) {
        nvgBeginPath(vg);
        nvgMoveTo(vg, (float) x1, (float) y1);
        nvgBezierTo(vg, (float) (x1 + control), (float) y1, (float) (x2 - control), (float) y2,
                (float) x2, (float) y2);
        stroke(color, strokeWidth);
    }

    void trashGlyph(double x, double y, RenderTheme.Rgba color) {
        double cx = x + 26.0;
        double top = y + 14.0;
        nvgBeginPath(vg);
        nvgMoveTo(vg, (float) (cx - 12.0), (float) top);
        nvgLineTo(vg, (float) (cx + 12.0), (float) top);
        nvgMoveTo(vg, (float) (cx - 6.5), (float) (top - 5.0));
        nvgLineTo(vg, (float) (cx + 6.5), (float) (top - 5.0));
        nvgRect(vg, (float) (cx - 9.0), (float) (top + 7.0), 18.0f, 18.0f);
        stroke(color, 2.5f);
    }

    void text(String value, float x, float y, float size, int align, RenderTheme.Rgba color, boolean bold) {
        if (!fontLoaded) {
            return;
        }
        nvgSave(vg);
        nvgFontFace(vg, bold && boldFontLoaded ? "sans-bold" : "sans");
        nvgFontSize(vg, size);
        nvgTextAlign(vg, align | NVG_ALIGN_MIDDLE);
        fillColor(color);
        nvgText(vg, Math.round(x), Math.round(y), value);
        nvgRestore(vg);
    }

    private boolean loadFont(String face, String... candidates) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path) && nvgCreateFont(vg, face, path.toString()) >= 0) {
                return true;
            }
        }
        System.err.println("NanoVG font not found for " + face + ". Set LOGIKA_FONT or LOGIKA_FONT_BOLD if needed.");
        return false;
    }

    private void fill(RenderTheme.Rgba color) {
        fillColor(color);
        nvgFill(vg);
    }

    private void stroke(RenderTheme.Rgba color, float width) {
        strokeColor(color);
        nvgStrokeWidth(vg, width);
        nvgStroke(vg);
    }

    private void fillColor(RenderTheme.Rgba color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgRGBA((byte) color.r(), (byte) color.g(), (byte) color.b(), (byte) color.a(), nvgColor);
            nvgFillColor(vg, nvgColor);
        }
    }

    private void strokeColor(RenderTheme.Rgba color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgRGBA((byte) color.r(), (byte) color.g(), (byte) color.b(), (byte) color.a(), nvgColor);
            nvgStrokeColor(vg, nvgColor);
        }
    }
}
