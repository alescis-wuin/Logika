package dev.alexis.logika.util;

/** Immutable rectangle. */
public record Rect(double x, double y, double width, double height) {
    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public boolean contains(Vec2 point) {
        return contains(point.x(), point.y());
    }

    public double centerX() {
        return x + width / 2.0;
    }

    public double centerY() {
        return y + height / 2.0;
    }
}
