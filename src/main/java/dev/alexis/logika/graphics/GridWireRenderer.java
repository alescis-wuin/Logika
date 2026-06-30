package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.*;
import dev.alexis.logika.ui.UiMetrics;
import dev.alexis.logika.util.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;

final class GridWireRenderer {
    private static final double SIGNAL_SPEED = 0.66;
    private static final double SIGNAL_HALF_WIDTH = 0.17;
    private static final int SIGNAL_SEGMENTS = 44;
    private static final double POINT_BREATHING_SPEED = 6.2;
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

    void draw(Camera2D camera, Viewport viewport, Circuit circuit, PinRef pendingWire, WireId selectedWireId,
              WireId hoveredWireId, WireTargetFeedback targetFeedback, double timeSeconds, double mouseX, double mouseY) {
        drawGrid(camera, viewport);
        drawWires(camera, viewport, circuit, selectedWireId, hoveredWireId, timeSeconds);
        drawPendingWire(camera, viewport, circuit, pendingWire, targetFeedback, timeSeconds, mouseX, mouseY);
    }

    void drawForegroundFeedback(Camera2D camera, Viewport viewport, Circuit circuit, WireTargetFeedback targetFeedback,
                                WireId selectedWireId, int hoveredControlPointIndex, double timeSeconds) {
        drawSignalNodePulses(camera, viewport, circuit, timeSeconds);
        drawTargetHalo(camera, viewport, circuit, targetFeedback, timeSeconds);
        drawSelectedWireControlPoints(camera, viewport, circuit, selectedWireId, hoveredControlPointIndex, timeSeconds);
    }

    private void drawGrid(Camera2D camera, Viewport viewport) {
        Vec2 topLeft = camera.screenToWorld(new Vec2(0, 0), viewport);
        Vec2 bottomRight = camera.screenToWorld(new Vec2(viewport.windowWidth(), viewport.windowHeight()), viewport);
        double minX = Math.min(topLeft.x(), bottomRight.x());
        double maxX = Math.max(topLeft.x(), bottomRight.x());
        double minY = Math.min(topLeft.y(), bottomRight.y());
        double maxY = Math.max(topLeft.y(), bottomRight.y());
        double step = UiMetrics.GRID_SIZE;
        while (step * camera.zoom() < 24.0) step *= 2.0;
        while (step * camera.zoom() > 92.0) step /= 2.0;
        drawGridLayer(camera, viewport, minX, maxX, minY, maxY, step, new RenderTheme.Rgba(104, 128, 170, 44), 1.0f);
        drawGridLayer(camera, viewport, minX, maxX, minY, maxY, step * 4.0, new RenderTheme.Rgba(143, 181, 242, 82), 1.45f);
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

    private void drawWires(Camera2D camera, Viewport viewport, Circuit circuit, WireId selectedWireId,
                           WireId hoveredWireId, double timeSeconds) {
        Wire selectedWire = null;
        for (Wire wire : circuit.wires()) {
            if (wire.sameEndpoints(selectedWireId)) {
                selectedWire = wire;
                continue;
            }
            drawKnownWire(camera, viewport, circuit, wire, false, wire.sameEndpoints(hoveredWireId), timeSeconds);
        }
        if (selectedWire != null) drawKnownWire(camera, viewport, circuit, selectedWire, true, true, timeSeconds);
    }

    private void drawKnownWire(Camera2D camera, Viewport viewport, Circuit circuit, Wire wire, boolean selected,
                               boolean hovered, double timeSeconds) {
        Optional<Vec2> start = circuit.pinPosition(wire.from());
        Optional<Vec2> end = circuit.pinPosition(wire.to());
        if (start.isEmpty() || end.isEmpty()) return;
        drawWire(camera, viewport, start.get(), wire.controlPoints(), end.get(), wire.colorRgb(), circuit.pinValue(wire.from()),
                false, true, selected, hovered, timeSeconds);
    }

    private void drawPendingWire(Camera2D camera, Viewport viewport, Circuit circuit, PinRef pendingWire,
                                 WireTargetFeedback targetFeedback, double timeSeconds, double mouseX, double mouseY) {
        if (pendingWire == null) return;
        Vec2 mouseWorld = camera.screenToWorld(new Vec2(mouseX, mouseY), viewport);
        Vec2 endWorld = mouseWorld;
        boolean validPreview = true;
        if (targetFeedback != null && targetFeedback.active()) {
            Optional<Vec2> target = circuit.pinPosition(targetFeedback.pin());
            if (target.isPresent()) endWorld = target.get();
            validPreview = targetFeedback.compatible();
        }
        Vec2 finalEndWorld = endWorld;
        boolean finalValidPreview = validPreview;
        circuit.pinPosition(pendingWire).ifPresent(start -> drawWire(camera, viewport, start, List.of(), finalEndWorld,
                Wire.DEFAULT_COLOR_RGB, circuit.pinValue(pendingWire), true, finalValidPreview, false, false, timeSeconds));
    }

    private void drawWire(Camera2D camera, Viewport viewport, Vec2 startWorld, List<Vec2> controlPointsWorld,
                          Vec2 endWorld, int colorRgb, boolean active, boolean pending, boolean validPreview,
                          boolean selected, boolean hovered, double timeSeconds) {
        Vec2 start = camera.worldToScreen(startWorld, viewport);
        Vec2 end = camera.worldToScreen(endWorld, viewport);
        List<WirePath.Segment> segments = WirePath.segments(start, toScreenControlPoints(camera, viewport, controlPointsWorld), end);
        double pulse = active && !pending ? 0.5 + 0.5 * Math.sin(timeSeconds * 5.8) : 0.0;
        RenderTheme.Rgba core = coreColor(colorRgb, active, pending, validPreview, selected, pulse);
        RenderTheme.Rgba border = borderColor(active, pending, validPreview, selected, pulse);
        if (selected) {
            canvas.bezierPath(segments, core.withAlpha((int) Math.round(86.0 + pulse * 38.0)), (float) UiMetrics.WIRE_SELECTED_HALO_WIDTH_SCREEN);
            canvas.bezierPath(segments, RenderTheme.ACCENT.withAlpha(86), 12.0f);
        } else if (hovered) {
            canvas.bezierPath(segments, core.withAlpha(72), (float) UiMetrics.WIRE_HOVER_HALO_WIDTH_SCREEN);
        }
        canvas.bezierPath(segments, border, pending ? 7.6f : (float) (7.2 + pulse * 2.8));
        canvas.bezierPath(segments, core, pending ? 4.6f : (float) (4.1 + pulse * 1.8));
        if (active && !pending) drawMovingSignalBand(segments, timeSeconds);
    }

    private void drawMovingSignalBand(List<WirePath.Segment> segments, double timeSeconds) {
        double center = signalProgress(timeSeconds);
        for (int i = 0; i < SIGNAL_SEGMENTS; i++) {
            double t0 = i / (double) SIGNAL_SEGMENTS;
            double t1 = (i + 1) / (double) SIGNAL_SEGMENTS;
            double distance = Math.abs(((t0 + t1) * 0.5) - center);
            if (distance > SIGNAL_HALF_WIDTH) continue;
            double weight = smoothStep(1.0 - distance / SIGNAL_HALF_WIDTH);
            Vec2 p0 = WirePath.sample(segments, t0);
            Vec2 p1 = WirePath.sample(segments, t1);
            canvas.line(p0.x(), p0.y(), p1.x(), p1.y(), RenderTheme.ACTIVE.withAlpha((int) Math.round(28.0 + 84.0 * weight)), (float) (6.0 + 8.0 * weight));
            canvas.line(p0.x(), p0.y(), p1.x(), p1.y(), RenderTheme.TEXT.withAlpha((int) Math.round(42.0 + 154.0 * weight)), (float) (2.4 + 7.4 * weight));
        }
    }

    private void drawSignalNodePulses(Camera2D camera, Viewport viewport, Circuit circuit, double timeSeconds) {
        double progress = signalProgress(timeSeconds);
        double depart = Math.pow(1.0 - clamp(progress / 0.22, 0.0, 1.0), 1.55);
        double arrive = Math.pow(clamp((progress - 0.74) / 0.26, 0.0, 1.0), 1.25);
        for (Wire wire : circuit.wires()) {
            if (!circuit.pinValue(wire.from())) continue;
            circuit.pinPosition(wire.from()).ifPresent(position -> drawSignalNodePulse(camera.worldToScreen(position, viewport), depart));
            circuit.pinPosition(wire.to()).ifPresent(position -> drawSignalNodePulse(camera.worldToScreen(position, viewport), arrive));
        }
    }

    private void drawSignalNodePulse(Vec2 screen, double strength) {
        if (strength <= 0.025) return;
        double value = smoothStep(clamp(strength, 0.0, 1.0));
        double radius = 11.0 + 14.0 * value;
        canvas.circle(screen.x(), screen.y(), radius, RenderTheme.ACTIVE.withAlpha((int) Math.round(26.0 + 54.0 * value)));
        canvas.strokeCircle(screen.x(), screen.y(), radius * 0.62, RenderTheme.ACTIVE.withAlpha((int) Math.round(96.0 + 72.0 * value)), (float) (1.6 + 1.4 * value));
        canvas.circle(screen.x(), screen.y(), 4.0 + 2.4 * value, RenderTheme.TEXT.withAlpha((int) Math.round(70.0 + 96.0 * value)));
    }

    private void drawTargetHalo(Camera2D camera, Viewport viewport, Circuit circuit, WireTargetFeedback targetFeedback,
                                double timeSeconds) {
        if (targetFeedback == null || !targetFeedback.active()) return;
        Optional<Vec2> worldPosition = circuit.pinPosition(targetFeedback.pin());
        if (worldPosition.isEmpty()) return;
        Vec2 screen = camera.worldToScreen(worldPosition.get(), viewport);
        double pulse = 0.5 + 0.5 * Math.sin(timeSeconds * 8.2);
        RenderTheme.Rgba color = targetFeedback.compatible() ? RenderTheme.ACCENT : RenderTheme.DANGER;
        double radius = 28.0 + pulse * 14.0;
        canvas.circle(screen.x(), screen.y(), radius + 7.0, color.withAlpha((int) Math.round(52.0 + (1.0 - pulse) * 58.0)));
        canvas.strokeCircle(screen.x(), screen.y(), radius, color.withAlpha((int) Math.round(218.0 + pulse * 37.0)), (float) (3.2 + pulse * 2.0));
        canvas.strokeCircle(screen.x(), screen.y(), radius * 0.50, color.withAlpha(255), 2.3f);
    }

    private void drawSelectedWireControlPoints(Camera2D camera, Viewport viewport, Circuit circuit, WireId selectedWireId,
                                               int hoveredControlPointIndex, double timeSeconds) {
        if (selectedWireId == null) return;
        Optional<Wire> selected = circuit.wireById(selectedWireId);
        if (selected.isEmpty()) return;
        List<Vec2> points = selected.get().controlPoints();
        for (int i = 0; i < points.size(); i++) {
            Vec2 screen = camera.worldToScreen(points.get(i), viewport);
            boolean hovered = i == hoveredControlPointIndex;
            double breath = hovered ? 0.5 + 0.5 * Math.sin(timeSeconds * POINT_BREATHING_SPEED) : 0.0;
            double radius = UiMetrics.WIRE_CONTROL_POINT_RADIUS_SCREEN + (hovered ? 4.0 + breath * 2.0 : 0.0);
            if (hovered) {
                double glow = UiMetrics.WIRE_CONTROL_POINT_HOVER_GLOW_SCREEN + breath * 12.0;
                canvas.circle(screen.x(), screen.y(), glow, RenderTheme.ACCENT.withAlpha((int) Math.round(54.0 + breath * 70.0)));
                canvas.strokeCircle(screen.x(), screen.y(), glow * 0.64, RenderTheme.TEXT.withAlpha((int) Math.round(116.0 + breath * 80.0)), (float) (1.8 + breath * 1.8));
            } else {
                canvas.circle(screen.x(), screen.y(), radius + 9.0, RenderTheme.ACCENT.withAlpha(52));
            }
            canvas.circle(screen.x(), screen.y(), radius, new RenderTheme.Rgba(18, 30, 52, 246));
            canvas.strokeCircle(screen.x(), screen.y(), radius, hovered ? RenderTheme.TEXT : RenderTheme.ACCENT, hovered ? 3.2f : 2.2f);
            canvas.text(String.valueOf(i + 1), (float) screen.x(), (float) screen.y(), 13.0f, NVG_ALIGN_CENTER, RenderTheme.TEXT, true);
        }
    }

    private RenderTheme.Rgba borderColor(boolean active, boolean pending, boolean validPreview, boolean selected, double pulse) {
        if (pending && !validPreview) return new RenderTheme.Rgba(92, 12, 28, 236);
        if (selected) return RenderTheme.TEXT.withAlpha(250);
        if (active && !pending) return RenderTheme.TEXT.withAlpha((int) Math.round(212.0 + pulse * 43.0));
        return new RenderTheme.Rgba(76, 90, 120, 238);
    }

    private RenderTheme.Rgba coreColor(int rgb, boolean active, boolean pending, boolean validPreview, boolean selected, double pulse) {
        if (pending && !validPreview) return RenderTheme.DANGER;
        if (pending) return RenderTheme.WARNING;
        RenderTheme.Rgba base = RenderTheme.fromRgb(rgb, 232);
        if (active) return base.brighten(0.18 + pulse * 0.15).withAlpha((int) Math.round(218.0 + pulse * 37.0));
        if (selected) return base.brighten(0.14).withAlpha(246);
        return base.darken(0.06).withAlpha(226);
    }

    private List<Vec2> toScreenControlPoints(Camera2D camera, Viewport viewport, List<Vec2> controlPointsWorld) {
        if (controlPointsWorld == null || controlPointsWorld.isEmpty()) return List.of();
        List<Vec2> points = new ArrayList<>(controlPointsWorld.size());
        for (Vec2 point : controlPointsWorld) points.add(camera.worldToScreen(point, viewport));
        return points;
    }

    private static double signalProgress(double timeSeconds) {
        double value = timeSeconds * SIGNAL_SPEED;
        return value - Math.floor(value);
    }

    private static double smoothStep(double value) {
        double v = clamp(value, 0.0, 1.0);
        return v * v * (3.0 - 2.0 * v);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
