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
    private static final double CONNECTION_EFFECT_SECONDS = 0.68;
    private static final double ACTIVE_WIRE_PULSE_SPEED = 4.4;

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
        float shadowWidth = pending ? 10.0f : (float) (8.5 + activePulse * 3.0);
        float coreWidth = pending ? 5.0f : (float) (4.2 + activePulse * 1.1);
        RenderTheme.Rgba shadow = shadowColor(active, pending, validPreview, activePulse);
        RenderTheme.Rgba color = wireColor(active, pending, validPreview, activePulse);
        canvas.bezier(start.x(), start.y(), end.x(), end.y(), control, shadow, shadowWidth);
        canvas.bezier(start.x(), start.y(), end.x(), end.y(), control, color, coreWidth);
    }

    private void drawConnectionEffect(Camera2D camera, Viewport viewport, Vec2 startWorld, Vec2 endWorld, double progress) {
        Vec2 start = camera.worldToScreen(startWorld, viewport);
        Vec2 end = camera.worldToScreen(endWorld, viewport);
        double control = Math.max(70.0, Math.abs(end.x() - start.x()) * 0.45);
        double fade = 1.0 - progress;
        int glowAlpha = (int) Math.round(130.0 * fade);
        int pulseAlpha = (int) Math.round(220.0 * fade);
        float glowWidth = (float) (13.5 - progress * 6.0);
        canvas.bezier(start.x(), start.y(), end.x(), end.y(), control, RenderTheme.ACTIVE.withAlpha(glowAlpha), glowWidth);

        Vec2 controlStart = new Vec2(start.x() + control, start.y());
        Vec2 controlEnd = new Vec2(end.x() - control, end.y());
        Vec2 pulse = cubic(start, controlStart, controlEnd, end, easeOutCubic(progress));
        double outer = 17.0 + 9.0 * fade;
        double inner = 4.2 + 2.0 * fade;
        canvas.circle(pulse.x(), pulse.y(), outer, RenderTheme.ACTIVE.withAlpha((int) Math.round(55.0 * fade)));
        canvas.strokeCircle(pulse.x(), pulse.y(), outer * 0.62, RenderTheme.ACTIVE.withAlpha(pulseAlpha), 2.4f);
        canvas.circle(pulse.x(), pulse.y(), inner, RenderTheme.TEXT.withAlpha(pulseAlpha));
    }

    private RenderTheme.Rgba shadowColor(boolean active, boolean pending, boolean validPreview, double activePulse) {
        if (pending && !validPreview) {
            return new RenderTheme.Rgba(74, 10, 22, 212);
        }
        if (active && !pending) {
            return new RenderTheme.Rgba(4, 33, 24, (int) Math.round(170.0 + activePulse * 48.0));
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
            return RenderTheme.ACTIVE.withAlpha((int) Math.round(196.0 + activePulse * 59.0));
        }
        return new RenderTheme.Rgba(131, 149, 187, 230);
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ConnectionEffect(Wire wire, double startedAtSeconds) {
    }
}
