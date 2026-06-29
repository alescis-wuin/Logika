package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.PinEndpoint;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.model.Wire;
import dev.alexis.logika.ui.UiMetrics;
import dev.alexis.logika.util.Vec2;

import java.util.Optional;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_RIGHT;

final class GridWireRenderer {
    private final NvgCanvas canvas;

    GridWireRenderer(NvgCanvas canvas) {
        this.canvas = canvas;
    }

    static double pinHitRadiusWorld(Camera2D camera) {
        return UiMetrics.PIN_HIT_RADIUS_SCREEN / Math.max(0.18, camera.zoom());
    }

    PinRef hoveredPin(Camera2D camera, Viewport viewport, Circuit circuit, double mouseX, double mouseY) {
        Vec2 mouseWorld = camera.screenToWorld(new Vec2(mouseX, mouseY), viewport);
        return circuit.findPin(mouseWorld, pinHitRadiusWorld(camera)).map(PinEndpoint::ref).orElse(null);
    }

    void draw(Camera2D camera, Viewport viewport, Circuit circuit, PinRef pendingWire, double mouseX, double mouseY) {
        drawGrid(camera, viewport);
        drawWires(camera, viewport, circuit);
        drawPendingWire(camera, viewport, circuit, pendingWire, mouseX, mouseY);
    }

    private void drawGrid(Camera2D camera, Viewport viewport) {
        Vec2 topLeft = camera.screenToWorld(new Vec2(0, 0), viewport);
        Vec2 bottomRight = camera.screenToWorld(new Vec2(viewport.windowWidth(), viewport.windowHeight()), viewport);
        double minX = Math.min(topLeft.x(), bottomRight.x());
        double maxX = Math.max(topLeft.x(), bottomRight.x());
        double minY = Math.min(topLeft.y(), bottomRight.y());
        double maxY = Math.max(topLeft.y(), bottomRight.y());
        double step = UiMetrics.GRID_SIZE;
        while (step * camera.zoom() < 24.0) {
            step *= 2.0;
        }
        while (step * camera.zoom() > 92.0) {
            step /= 2.0;
        }
        drawGridLayer(camera, viewport, minX, maxX, minY, maxY, step, new RenderTheme.Rgba(104, 128, 170, 44), 1.0f);
        drawGridLayer(camera, viewport, minX, maxX, minY, maxY, step * 4.0, new RenderTheme.Rgba(143, 181, 242, 82), 1.45f);
        drawAxes(camera, viewport, minX, maxX, minY, maxY);
    }

    private void drawGridLayer(Camera2D camera, Viewport viewport, double minX, double maxX, double minY, double maxY,
                               double step, RenderTheme.Rgba color, float width) {
        for (double x = Math.floor(minX / step) * step; x <= maxX; x += step) {
            Vec2 p = camera.worldToScreen(new Vec2(x, minY), viewport);
            canvas.line(p.x(), 0.0, p.x(), viewport.windowHeight(), color, width);
        }
        for (double y = Math.floor(minY / step) * step; y <= maxY; y += step) {
            Vec2 p = camera.worldToScreen(new Vec2(minX, y), viewport);
            canvas.line(0.0, p.y(), viewport.windowWidth(), p.y(), color, width);
        }
    }

    private void drawAxes(Camera2D camera, Viewport viewport, double minX, double maxX, double minY, double maxY) {
        if (minY <= 0.0 && maxY >= 0.0) {
            Vec2 start = camera.worldToScreen(new Vec2(minX, 0.0), viewport);
            Vec2 end = camera.worldToScreen(new Vec2(maxX, 0.0), viewport);
            canvas.line(start.x(), start.y(), end.x(), end.y(), new RenderTheme.Rgba(146, 186, 244, 168), 2.2f);
            canvas.text("X axis", viewport.windowWidth() - 78.0f, (float) start.y() - 15.0f, 16.0f,
                    NVG_ALIGN_RIGHT, RenderTheme.TEXT_MUTED, false);
        }
        if (minX <= 0.0 && maxX >= 0.0) {
            Vec2 start = camera.worldToScreen(new Vec2(0.0, minY), viewport);
            Vec2 end = camera.worldToScreen(new Vec2(0.0, maxY), viewport);
            canvas.line(start.x(), start.y(), end.x(), end.y(), new RenderTheme.Rgba(146, 186, 244, 168), 2.2f);
            canvas.text("Y axis", (float) start.x() + 18.0f, 110.0f, 16.0f,
                    NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED, false);
        }
    }

    private void drawWires(Camera2D camera, Viewport viewport, Circuit circuit) {
        for (Wire wire : circuit.wires()) {
            Optional<Vec2> start = circuit.pinPosition(wire.from());
            Optional<Vec2> end = circuit.pinPosition(wire.to());
            if (start.isPresent() && end.isPresent()) {
                drawWire(camera, viewport, start.get(), end.get(), circuit.pinValue(wire.from()), false);
            }
        }
    }

    private void drawPendingWire(Camera2D camera, Viewport viewport, Circuit circuit, PinRef pendingWire,
                                 double mouseX, double mouseY) {
        if (pendingWire == null) {
            return;
        }
        circuit.pinPosition(pendingWire).ifPresent(start -> drawWire(camera, viewport, start,
                camera.screenToWorld(new Vec2(mouseX, mouseY), viewport), circuit.pinValue(pendingWire), true));
    }

    private void drawWire(Camera2D camera, Viewport viewport, Vec2 startWorld, Vec2 endWorld, boolean active, boolean pending) {
        Vec2 start = camera.worldToScreen(startWorld, viewport);
        Vec2 end = camera.worldToScreen(endWorld, viewport);
        double control = Math.max(70.0, Math.abs(end.x() - start.x()) * 0.45);
        RenderTheme.Rgba color = pending ? RenderTheme.WARNING : active ? RenderTheme.ACTIVE : new RenderTheme.Rgba(131, 149, 187, 230);
        canvas.bezier(start.x(), start.y(), end.x(), end.y(), control, new RenderTheme.Rgba(3, 5, 11, 220), pending ? 10.0f : 8.5f);
        canvas.bezier(start.x(), start.y(), end.x(), end.y(), control, color, pending ? 5.0f : 4.2f);
    }
}
