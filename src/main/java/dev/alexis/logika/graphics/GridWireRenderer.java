package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.PinEndpoint;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.model.Wire;
import dev.alexis.logika.ui.UiMetrics;
import dev.alexis.logika.util.Vec2;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_RIGHT;

final class GridWireRenderer {
    private static final double CONNECTION_EFFECT_SECONDS = 0.74;
    private static final double ACTIVE_WIRE_PULSE_SPEED = 5.8;
    private static final double SIGNAL_BAND_SPEED = 0.82;
    private static final double SIGNAL_BAND_HALF_WIDTH = 0.17;
    private static final int SIGNAL_BAND_SEGMENTS = 34;

    private final NvgCanvas canvas;
    private final Set<Wire> knownWires = new LinkedHashSet<>();
    private final List<ConnectionEffect> connectionEffects = new ArrayList<>();

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

    void draw(Camera2D camera, Viewport viewport, Circuit circuit, PinRef pendingWire,
              WireTargetFeedback targetFeedback, double timeSeconds, double mouseX, double mouseY) {
        drawGrid(camera, viewport);
        refreshConnectionEffects(circuit, timeSeconds);
        drawWires(camera, viewport, circuit, timeSeconds);
        drawConnectionEffects(camera, viewport, circuit, timeSeconds);
        drawPendingWire(camera, viewport, circuit, pendingWire, targetFeedback, timeSeconds, mouseX, mouseY);
    }

    void drawForegroundFeedback(Camera2D camera, Viewport viewport, Circuit circuit, WireTargetFeedback targetFeedback,
                                double timeSeconds) {
        drawActiveWireNodeBreathing(camera, viewport, circuit, timeSeconds);
        drawTargetHalo(camera, viewport, circuit, targetFeedback, timeSeconds);
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

    private void refreshConnectionEffects(Circuit circuit, double timeSeconds) {
        Set<Wire> currentWires = new LinkedHashSet<>(circuit.wires());
        for (Wire wire : currentWires) {
            if (!knownWires.contains(wire)) {
                connectionEffects.add(new ConnectionEffect(wire, timeSeconds));
            }
        }
        knownWires.clear();
        knownWires.addAll(currentWires);
        connectionEffects.removeIf(effect -> !currentWires.contains(effect.wire())
                || timeSeconds - effect.startedAtSeconds() > CONNECTION_EFFECT_SECONDS);
    }

    private void drawWires(Camera2D camera, Viewport viewport, Circuit circuit, double timeSeconds) {
        for (Wire wire : circuit.wires()) {
            Optional<Vec2> start = circuit.pinPosition(wire.from());
            Optional<Vec2> end = circuit.pinPosition(wire.to());
            if (start.isPresent() && end.isPresent()) {
                drawWire(camera, viewport, start.get(), end.get(), circuit.pinValue(wire.from()), false, true, timeSeconds);
            }
        }
    }

    private void drawConnectionEffects(Camera2D camera, Viewport viewport, Circuit circuit, double timeSeconds) {
        for (ConnectionEffect effect : connectionEffects) {
            Optional<Vec2> start = circuit.pinPosition(effect.wire().from());
            Optional<Vec2> end = circuit.pinPosition(effect.wire().to());
            if (start.isPresent() && end.isPresent()) {
                double progress = clamp((timeSeconds - effect.startedAtSeconds()) / CONNECTION_EFFECT_SECONDS, 0.0, 1.0);
                drawConnectionEffect(camera, viewport, start.get(), end.get(), progress);
            }
        }
    }

    private void drawPendingWire(Camera2D camera, Viewport viewport, Circuit circuit, PinRef pendingWire,
                                 WireTargetFeedback targetFeedback, double timeSeconds, double mouseX, double mouseY) {
        if (pendingWire == null) {
            return;
        }
        Vec2 mouseWorld = camera.screenToWorld(new Vec2(mouseX, mouseY), viewport);
        Vec2 endWorld = mouseWorld;
        boolean validPreview = true;
        if (targetFeedback != null && targetFeedback.active()) {
            Optional<Vec2> target = circuit.pinPosition(targetFeedback.pin());
            if (target.isPresent()) {
                endWorld = target.get();
            }
            validPreview = targetFeedback.compatible();
        }
        Vec2 finalEndWorld = endWorld;
        boolean finalValidPreview = validPreview;
        circuit.pinPosition(pendingWire).ifPresent(start -> drawWire(camera, viewport, start, finalEndWorld,
                circuit.pinValue(pendingWire), true, finalValidPreview, timeSeconds));
    }

    private void drawWire(Camera2D camera, Viewport viewport, Vec2 startWorld, Vec2 endWorld, boolean active,
                          boolean pending, boolean validPreview, double timeSeconds) {
        Vec2 start = camera.worldToScreen(startWorld, viewport);
        Vec2 end = camera.worldToScreen(endWorld, viewport);
        double control = Math.max(70.0, Math.abs(end.x() - start.x()) * 0.45);
        double activePulse = active && !pending ? 0.5 + 0.5 * Math.sin(timeSeconds * ACTIVE_WIRE_PULSE_SPEED) : 0.0;
        float shadowWidth = pending ? 11.0f : (float) (9.5 + activePulse * 5.2);
        float coreWidth = pending ? 5.4f : (float) (4.6 + activePulse * 2.4);
        RenderTheme.Rgba shadow = shadowColor(active, pending, validPreview, activePulse);
        RenderTheme.Rgba color = wireColor(active, pending, validPreview, activePulse);
        canvas.bezier(start.x(), start.y(), end.x(), end.y(), control, shadow, shadowWidth);
        canvas.bezier(start.x(), start.y(), end.x(), end.y(), control, color, coreWidth);
        if (active && !pending) {
            drawMovingSignalBand(start, end, control, timeSeconds);
        }
    }

    private void drawMovingSignalBand(Vec2 start, Vec2 end, double control, double timeSeconds) {
        double center = signalProgress(timeSeconds);
        Vec2 controlStart = new Vec2(start.x() + control, start.y());
        Vec2 controlEnd = new Vec2(end.x() - control, end.y());
        for (int i = 0; i < SIGNAL_BAND_SEGMENTS; i++) {
            double t0 = i / (double) SIGNAL_BAND_SEGMENTS;
            double t1 = (i + 1) / (double) SIGNAL_BAND_SEGMENTS;
            double mid = (t0 + t1) * 0.5;
            double distance = Math.abs(mid - center);
            if (distance > SIGNAL_BAND_HALF_WIDTH) {
                continue;
            }
            double local = 1.0 - distance / SIGNAL_BAND_HALF_WIDTH;
            double weight = smoothStep(local);
            Vec2 p0 = cubic(start, controlStart, controlEnd, end, t0);
            Vec2 p1 = cubic(start, controlStart, controlEnd, end, t1);
            canvas.line(p0.x(), p0.y(), p1.x(), p1.y(), RenderTheme.ACTIVE.withAlpha((int) Math.round(38.0 + 120.0 * weight)),
                    (float) (6.0 + 9.5 * weight));
            canvas.line(p0.x(), p0.y(), p1.x(), p1.y(), RenderTheme.TEXT.withAlpha((int) Math.round(22.0 + 92.0 * weight)),
                    (float) (1.8 + 3.2 * weight));
        }
    }

    private void drawActiveWireNodeBreathing(Camera2D camera, Viewport viewport, Circuit circuit, double timeSeconds) {
        double progress = signalProgress(timeSeconds);
        double departurePulse = Math.pow(1.0 - clamp(progress / 0.22, 0.0, 1.0), 1.55);
        double arrivalPulse = Math.pow(clamp((progress - 0.74) / 0.26, 0.0, 1.0), 1.25);
        for (Wire wire : circuit.wires()) {
            if (!circuit.pinValue(wire.from())) {
                continue;
            }
            Optional<Vec2> start = circuit.pinPosition(wire.from());
            Optional<Vec2> end = circuit.pinPosition(wire.to());
            start.ifPresent(position -> drawSignalNodePulse(camera.worldToScreen(position, viewport), departurePulse));
            end.ifPresent(position -> drawSignalNodePulse(camera.worldToScreen(position, viewport), arrivalPulse));
        }
    }

    private void drawSignalNodePulse(Vec2 screen, double strength) {
        if (strength <= 0.025) {
            return;
        }
        double softened = smoothStep(clamp(strength, 0.0, 1.0));
        double radius = 13.0 + 22.0 * softened;
        int fillAlpha = (int) Math.round(36.0 + 78.0 * softened);
        int strokeAlpha = (int) Math.round(120.0 + 110.0 * softened);
        canvas.circle(screen.x(), screen.y(), radius, RenderTheme.ACTIVE.withAlpha(fillAlpha));
        canvas.strokeCircle(screen.x(), screen.y(), radius * 0.64, RenderTheme.ACTIVE.withAlpha(strokeAlpha),
                (float) (2.0 + 2.2 * softened));
        canvas.circle(screen.x(), screen.y(), 4.5 + 4.0 * softened, RenderTheme.TEXT.withAlpha((int) Math.round(78.0 + 122.0 * softened)));
    }

    private void drawTargetHalo(Camera2D camera, Viewport viewport, Circuit circuit, WireTargetFeedback targetFeedback,
                                double timeSeconds) {
        if (targetFeedback == null || !targetFeedback.active()) {
            return;
        }
        Optional<Vec2> worldPosition = circuit.pinPosition(targetFeedback.pin());
        if (worldPosition.isEmpty()) {
            return;
        }
        Vec2 screen = camera.worldToScreen(worldPosition.get(), viewport);
        double pulse = 0.5 + 0.5 * Math.sin(timeSeconds * 8.2);
        RenderTheme.Rgba color = targetFeedback.compatible() ? RenderTheme.ACCENT : RenderTheme.DANGER;
        double radius = 28.0 + pulse * 14.0;
        canvas.circle(screen.x(), screen.y(), radius + 7.0, color.withAlpha((int) Math.round(52.0 + (1.0 - pulse) * 58.0)));
        canvas.strokeCircle(screen.x(), screen.y(), radius, color.withAlpha((int) Math.round(218.0 + pulse * 37.0)),
                (float) (3.2 + pulse * 2.0));
        canvas.strokeCircle(screen.x(), screen.y(), radius * 0.50, color.withAlpha(255), 2.3f);
    }

    private void drawConnectionEffect(Camera2D camera, Viewport viewport, Vec2 startWorld, Vec2 endWorld, double progress) {
        Vec2 start = camera.worldToScreen(startWorld, viewport);
        Vec2 end = camera.worldToScreen(endWorld, viewport);
        double control = Math.max(70.0, Math.abs(end.x() - start.x()) * 0.45);
        double fade = 1.0 - progress;
        int glowAlpha = (int) Math.round(165.0 * fade);
        int pulseAlpha = (int) Math.round(245.0 * fade);
        float glowWidth = (float) (18.0 - progress * 8.0);
        canvas.bezier(start.x(), start.y(), end.x(), end.y(), control, RenderTheme.ACTIVE.withAlpha(glowAlpha), glowWidth);

        Vec2 controlStart = new Vec2(start.x() + control, start.y());
        Vec2 controlEnd = new Vec2(end.x() - control, end.y());
        Vec2 pulse = cubic(start, controlStart, controlEnd, end, easeOutCubic(progress));
        double outer = 22.0 + 12.0 * fade;
        double inner = 5.2 + 3.0 * fade;
        canvas.circle(pulse.x(), pulse.y(), outer, RenderTheme.ACTIVE.withAlpha((int) Math.round(74.0 * fade)));
        canvas.strokeCircle(pulse.x(), pulse.y(), outer * 0.62, RenderTheme.ACTIVE.withAlpha(pulseAlpha), 3.0f);
        canvas.circle(pulse.x(), pulse.y(), inner, RenderTheme.TEXT.withAlpha(pulseAlpha));
    }

    private RenderTheme.Rgba shadowColor(boolean active, boolean pending, boolean validPreview, double activePulse) {
        if (pending && !validPreview) {
            return new RenderTheme.Rgba(92, 12, 28, 228);
        }
        if (active && !pending) {
            return new RenderTheme.Rgba(4, 34, 25, (int) Math.round(190.0 + activePulse * 58.0));
        }
        return new RenderTheme.Rgba(3, 5, 11, 220);
    }

    private RenderTheme.Rgba wireColor(boolean active, boolean pending, boolean validPreview, double activePulse) {
        if (pending && !validPreview) {
            return RenderTheme.DANGER;
        }
        if (pending) {
            return RenderTheme.WARNING;
        }
        if (active) {
            return RenderTheme.ACTIVE.withAlpha((int) Math.round(212.0 + activePulse * 43.0));
        }
        return new RenderTheme.Rgba(131, 149, 187, 230);
    }

    private static double signalProgress(double timeSeconds) {
        double value = timeSeconds * SIGNAL_BAND_SPEED;
        return value - Math.floor(value);
    }

    private static Vec2 cubic(Vec2 p0, Vec2 p1, Vec2 p2, Vec2 p3, double t) {
        double u = 1.0 - t;
        return p0.scale(u * u * u)
                .add(p1.scale(3.0 * u * u * t))
                .add(p2.scale(3.0 * u * t * t))
                .add(p3.scale(t * t * t));
    }

    private static double easeOutCubic(double value) {
        double inverse = 1.0 - value;
        return 1.0 - inverse * inverse * inverse;
    }

    private static double smoothStep(double value) {
        double clamped = clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ConnectionEffect(Wire wire, double startedAtSeconds) {
    }
}
