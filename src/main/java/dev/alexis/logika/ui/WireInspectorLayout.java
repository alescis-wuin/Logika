package dev.alexis.logika.ui;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.util.Rect;
import dev.alexis.logika.util.Vec2;

public final class WireInspectorLayout {
    private static final double PANEL_TOP = 158.0;
    private static final double PICKER_TOP_OFFSET = 302.0;
    private static final double PICKER_HEIGHT = 132.0;

    private WireInspectorLayout() { }

    public static Rect panelBounds(Viewport viewport) {
        double width = Math.min(UiMetrics.WIRE_INSPECTOR_WIDTH_SCREEN, Math.max(286.0, viewport.windowWidth() * 0.28));
        double x = Math.max(20.0, viewport.windowWidth() - width - UiMetrics.WIRE_INSPECTOR_MARGIN_SCREEN);
        double bottom = viewport.windowHeight() - UiMetrics.TOOLBAR_PANEL_HEIGHT_SCREEN - UiMetrics.TOOLBAR_PANEL_MARGIN_SCREEN - 20.0;
        double height = Math.max(340.0, Math.min(560.0, bottom - PANEL_TOP));
        return new Rect(x, PANEL_TOP, width, height);
    }

    public static Rect colorPickerBounds(Viewport viewport) {
        Rect panel = panelBounds(viewport);
        return new Rect(panel.x() + 20.0, panel.y() + PICKER_TOP_OFFSET, panel.width() - 40.0, PICKER_HEIGHT);
    }

    public static Rgb colorAt(Rect picker, double mouseX, double mouseY) {
        double hue = clamp((mouseX - picker.x()) / picker.width(), 0.0, 1.0);
        double value = 1.0 - 0.72 * clamp((mouseY - picker.y()) / picker.height(), 0.0, 1.0);
        return fromHsv(hue, 0.88, value);
    }

    public static Vec2 selectorPosition(Rect picker, int rgb) {
        Hsv hsv = toHsv(rgb);
        double x = picker.x() + hsv.hue() * picker.width();
        double y = picker.y() + clamp((1.0 - hsv.value()) / 0.72, 0.0, 1.0) * picker.height();
        return new Vec2(x, y);
    }

    public static int hsvToRgb(double hue, double saturation, double value) {
        return fromHsv(hue, saturation, value).packedRgb();
    }

    private static Rgb fromHsv(double hue, double saturation, double value) {
        double h = (hue - Math.floor(hue)) * 6.0;
        int sector = (int) Math.floor(h);
        double fraction = h - sector;
        double p = value * (1.0 - saturation);
        double q = value * (1.0 - fraction * saturation);
        double t = value * (1.0 - (1.0 - fraction) * saturation);
        double r;
        double g;
        double b;
        switch (sector) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return new Rgb(toChannel(r), toChannel(g), toChannel(b));
    }

    private static Hsv toHsv(int rgb) {
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double delta = max - min;
        double hue;
        if (delta == 0.0) hue = 0.0;
        else if (max == r) hue = ((g - b) / delta) / 6.0;
        else if (max == g) hue = (((b - r) / delta) + 2.0) / 6.0;
        else hue = (((r - g) / delta) + 4.0) / 6.0;
        if (hue < 0.0) hue += 1.0;
        double saturation = max == 0.0 ? 0.0 : delta / max;
        return new Hsv(hue, saturation, max);
    }

    private static int toChannel(double value) {
        return (int) Math.round(clamp(value, 0.0, 1.0) * 255.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Rgb(int r, int g, int b) {
        public int packedRgb() {
            return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }
    }

    private record Hsv(double hue, double saturation, double value) { }
}
