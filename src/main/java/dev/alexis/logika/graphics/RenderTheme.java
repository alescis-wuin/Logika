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

    record Rgba(int r, int g, int b, int a) {
        float rf() {
            return r / 255.0f;
        }

        float gf() {
            return g / 255.0f;
        }

        float bf() {
            return b / 255.0f;
        }
    }
}
