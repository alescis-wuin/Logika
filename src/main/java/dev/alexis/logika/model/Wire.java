package dev.alexis.logika.model;

import dev.alexis.logika.util.Vec2;

import java.util.List;
import java.util.Objects;

public record Wire(PinRef from, PinRef to, int colorRgb, List<Vec2> controlPoints) {
    public static final int DEFAULT_COLOR_RGB = 0x7CA7FF;

    public Wire(PinRef from, PinRef to) {
        this(from, to, DEFAULT_COLOR_RGB, List.of());
    }

    public Wire {
        from = Objects.requireNonNull(from, "from");
        to = Objects.requireNonNull(to, "to");
        colorRgb = colorRgb & 0xFF_FF_FF;
        controlPoints = controlPoints == null ? List.of() : List.copyOf(controlPoints);
    }

    public WireId id() {
        return new WireId(from, to);
    }

    public boolean sameEndpoints(WireId id) {
        return id != null && from.equals(id.from()) && to.equals(id.to());
    }

    public Wire withColor(int nextColorRgb) {
        return new Wire(from, to, nextColorRgb, controlPoints);
    }

    public Wire withControlPoints(List<Vec2> nextControlPoints) {
        return new Wire(from, to, colorRgb, nextControlPoints);
    }

    public Wire withInsertedControlPoint(int index, Vec2 point) {
        Objects.requireNonNull(point, "point");
        List<Vec2> next = new java.util.ArrayList<>(controlPoints);
        int safeIndex = Math.max(0, Math.min(index, next.size()));
        next.add(safeIndex, point);
        return withControlPoints(next);
    }

    public Wire withMovedControlPoint(int index, Vec2 point) {
        Objects.requireNonNull(point, "point");
        if (index < 0 || index >= controlPoints.size()) {
            return this;
        }
        List<Vec2> next = new java.util.ArrayList<>(controlPoints);
        next.set(index, point);
        return withControlPoints(next);
    }

    public boolean touchesComponent(int componentId) {
        return from.componentId() == componentId || to.componentId() == componentId;
    }
}
