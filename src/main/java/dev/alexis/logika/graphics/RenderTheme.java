package dev.alexis.logika.graphics;

final class RenderTheme {
    static final Rgba BACKGROUND = new Rgba(8, 11, 20, 255);
    static final Rgba PANEL = new Rgba(18, 25, 40, 246);
    static final Rgba PANEL_STROKE = new Rgba(132, 157, 204, 185);
    static final Rgba TEXT = new Rgba(250, 252, 255, 255);
    static final Rgba TEXT_MUTED = new Rgba(209, 224, 248, 255);
    static final Rgba ACTIVE = new Rgba(67, 232, 143, 255);
    static final Rgba INACTIVE = new Rgba(126, 148, 188, 255);
    static final Rgba ACCENT = new Rgba(140, 199, 255, 255);
    static final Rgba WARNING = new Rgba(255, 210, 102, 255);
    static final Rgba DANGER = new Rgba(255, 82, 93, 255);
    static final Rgba SIGNAL_PANEL = new Rgba(9, 14, 25, 226);
    static final Rgba SIGNAL_BORDER = new Rgba(159, 183, 226, 180);

    private RenderTheme() {
    }

    static Rgba fromRgb(int rgb, int alpha) {
        return new Rgba((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
    }

    record Rgba(int r, int g, int b, int a) {
        Rgba {
            r = clampChannel(r);
            g = clampChannel(g);
            b = clampChannel(b);
            a = clampChannel(a);
        }

        float rf() {
            return r / 255.0f;
        }

        float gf() {
            return g / 255.0f;
        }

        float bf() {
            return b / 255.0f;
        }

        Rgba withAlpha(int alpha) {
            return new Rgba(r, g, b, alpha);
        }

        Rgba brighten(double amount) {
            double t = clamp(amount, 0.0, 1.0);
            return new Rgba(
                    (int) Math.round(r + (255 - r) * t),
                    (int) Math.round(g + (255 - g) * t),
                    (int) Math.round(b + (255 - b) * t),
                    a);
        }

        Rgba darken(double amount) {
            double t = 1.0 - clamp(amount, 0.0, 1.0);
            return new Rgba((int) Math.round(r * t), (int) Math.round(g * t), (int) Math.round(b * t), a);
        }

        Rgba mix(Rgba other, double amount) {
            double t = clamp(amount, 0.0, 1.0);
            double u = 1.0 - t;
            return new Rgba(
                    (int) Math.round(r * u + other.r * t),
                    (int) Math.round(g * u + other.g * t),
                    (int) Math.round(b * u + other.b * t),
                    (int) Math.round(a * u + other.a * t));
        }

        private static int clampChannel(int value) {
            return Math.max(0, Math.min(255, value));
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
