package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.util.Vec2;

public final class Camera2D {
    private static final double MIN_ZOOM = 0.18;
    private static final double MAX_ZOOM = 4.5;

    private double centerX;
    private double centerY;
    private double zoom = 1.0;

    public Vec2 worldToScreen(Vec2 world, Viewport viewport) {
        double x = (world.x() - centerX) * zoom + viewport.windowWidth() / 2.0;
        double y = (world.y() - centerY) * zoom + viewport.windowHeight() / 2.0;
        return new Vec2(x, y);
    }

    public Vec2 screenToWorld(Vec2 screen, Viewport viewport) {
        double x = (screen.x() - viewport.windowWidth() / 2.0) / zoom + centerX;
        double y = (screen.y() - viewport.windowHeight() / 2.0) / zoom + centerY;
        return new Vec2(x, y);
    }

    public void panByScreenDelta(double deltaX, double deltaY) {
        centerX -= deltaX / zoom;
        centerY -= deltaY / zoom;
    }

    public void zoomAt(double screenX, double screenY, double factor, Viewport viewport) {
        Vec2 before = screenToWorld(new Vec2(screenX, screenY), viewport);
        zoom = clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
        Vec2 after = screenToWorld(new Vec2(screenX, screenY), viewport);
        centerX += before.x() - after.x();
        centerY += before.y() - after.y();
    }

    public void reset() {
        centerX = 0.0;
        centerY = 0.0;
        zoom = 1.0;
    }

    public double zoom() {
        return zoom;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
