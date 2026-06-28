package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.CircuitComponent;
import dev.alexis.logika.model.ComponentKind;
import dev.alexis.logika.model.PinDirection;
import dev.alexis.logika.model.PinEndpoint;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.ui.UiMetrics;
import dev.alexis.logika.util.Rect;
import dev.alexis.logika.util.Vec2;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;

final class ComponentCanvasRenderer {
    private final NvgCanvas canvas;

    ComponentCanvasRenderer(NvgCanvas canvas) {
        this.canvas = canvas;
    }

    void draw(Camera2D camera, Viewport viewport, Circuit circuit, int selectedId, int hoveredId,
              PinRef hoveredPin, PinRef pendingWire) {
        for (CircuitComponent component : circuit.components()) {
            Rect bounds = component.bounds();
            Vec2 topLeft = camera.worldToScreen(new Vec2(bounds.x(), bounds.y()), viewport);
            double width = bounds.width() * camera.zoom();
            double height = bounds.height() * camera.zoom();
            boolean active = component.visualActive();
            boolean hovered = component.id() == hoveredId;
            RenderTheme.Rgba border = hovered ? new RenderTheme.Rgba(216, 232, 255, 255)
                    : component.id() == selectedId ? RenderTheme.ACCENT : active ? RenderTheme.ACTIVE : RenderTheme.PANEL_STROKE;
            double radius = Math.min(clamp(UiMetrics.COMPONENT_RADIUS_SCREEN * Math.sqrt(camera.zoom()), 16.0, 32.0),
                    Math.min(width, height) * 0.28);

            canvas.fillRound(topLeft.x() + 6.0, topLeft.y() + 12.0, width, height, radius, new RenderTheme.Rgba(0, 0, 0, 100));
            canvas.fillRound(topLeft.x(), topLeft.y(), width, height, radius, bodyColor(component.kind(), active));
            canvas.strokeRound(topLeft.x(), topLeft.y(), width, height, radius, border, hovered ? 3.6f : 2.0f);
            drawComponentHeader(component, topLeft, width, height, hovered);

            if (width >= 168.0 && height >= 96.0) {
                for (PinEndpoint pin : component.pins()) {
                    drawSignalBadge(camera, viewport, circuit, pin, topLeft, width, height, hovered);
                }
            }
            if (hovered) {
                drawTrashButton(topLeft.x() + UiMetrics.TRASH_BUTTON_MARGIN_SCREEN, topLeft.y() + UiMetrics.TRASH_BUTTON_MARGIN_SCREEN);
            }
            for (PinEndpoint pin : component.pins()) {
                drawPin(camera, viewport, circuit, pin, pin.ref().equals(hoveredPin), pin.ref().equals(pendingWire));
            }
        }
    }

    private void drawComponentHeader(CircuitComponent component, Vec2 topLeft, double width, double height, boolean hovered) {
        double left = hovered ? UiMetrics.TRASH_BUTTON_MARGIN_SCREEN + UiMetrics.TRASH_BUTTON_SIZE_SCREEN + UiMetrics.TRASH_CONTENT_GAP_SCREEN
                : UiMetrics.COMPONENT_PADDING_SCREEN;
        double centerX = topLeft.x() + left + (width - left - UiMetrics.COMPONENT_PADDING_SCREEN) / 2.0;
        double titleY = topLeft.y() + height * 0.23;
        float title = (float) clamp(Math.min(width / 8.8, height / 4.3), 15.0, 27.0);
        canvas.text(component.kind().label(), (float) centerX, (float) titleY, title, NVG_ALIGN_CENTER, RenderTheme.TEXT, true);
        if (height >= 112.0 && width >= 188.0) {
            canvas.text(component.kind().description(), (float) centerX, (float) (titleY + title * 0.92),
                    (float) clamp(title * 0.58, 10.5, 15.0), NVG_ALIGN_CENTER, RenderTheme.TEXT_MUTED, false);
        }
    }

    private void drawSignalBadge(Camera2D camera, Viewport viewport, Circuit circuit, PinEndpoint pin,
                                 Vec2 topLeft, double componentWidth, double componentHeight, boolean componentHovered) {
        Vec2 pinScreen = camera.worldToScreen(pin.worldPosition(), viewport);
        boolean output = pin.ref().direction() == PinDirection.OUTPUT;
        boolean value = circuit.pinValue(pin.ref());
        double scale = clamp(Math.min(componentWidth / 280.0, componentHeight / 152.0), 0.82, 1.12);
        double badgeW = UiMetrics.SIGNAL_BADGE_WIDTH_SCREEN * scale;
        double badgeH = UiMetrics.SIGNAL_BADGE_HEIGHT_SCREEN * scale;
        double x = output ? pinScreen.x() - UiMetrics.SIGNAL_BADGE_NODE_GAP_SCREEN * scale - badgeW
                : pinScreen.x() + UiMetrics.SIGNAL_BADGE_NODE_GAP_SCREEN * scale;
        double minX = topLeft.x() + UiMetrics.COMPONENT_PADDING_SCREEN * scale;
        if (!output && componentHovered) {
            minX = Math.max(minX, topLeft.x() + UiMetrics.TRASH_BUTTON_MARGIN_SCREEN + UiMetrics.TRASH_BUTTON_SIZE_SCREEN + UiMetrics.TRASH_CONTENT_GAP_SCREEN);
        }
        x = clamp(x, minX, topLeft.x() + componentWidth - badgeW - UiMetrics.COMPONENT_PADDING_SCREEN * scale);
        double y = clamp(pinScreen.y() - badgeH / 2.0, topLeft.y() + UiMetrics.COMPONENT_PADDING_SCREEN * scale,
                topLeft.y() + componentHeight - badgeH - UiMetrics.COMPONENT_PADDING_SCREEN * scale);
        canvas.line(pinScreen.x(), pinScreen.y(), output ? x + badgeW : x, pinScreen.y(),
                value ? RenderTheme.ACTIVE : new RenderTheme.Rgba(139, 158, 197, 190), value ? 3.0f : 2.2f);
        canvas.fillRound(x, y, badgeW, badgeH, 14.0 * scale, RenderTheme.SIGNAL_PANEL);
        canvas.strokeRound(x, y, badgeW, badgeH, 14.0 * scale, value ? RenderTheme.ACTIVE : RenderTheme.SIGNAL_BORDER,
                value ? 2.2f : 1.4f);
        canvas.text(pin.label(), (float) (x + badgeW / 2.0), (float) (y + badgeH * 0.34),
                (float) (13.0 * scale), NVG_ALIGN_CENTER, RenderTheme.TEXT_MUTED, true);
        canvas.text(value ? "1" : "0", (float) (x + badgeW / 2.0), (float) (y + badgeH * 0.72),
                (float) (23.0 * scale), NVG_ALIGN_CENTER, value ? new RenderTheme.Rgba(231, 255, 241, 255) : RenderTheme.TEXT, true);
    }

    private void drawPin(Camera2D camera, Viewport viewport, Circuit circuit, PinEndpoint pin, boolean hovered, boolean pending) {
        Vec2 screen = camera.worldToScreen(pin.worldPosition(), viewport);
        boolean value = circuit.pinValue(pin.ref());
        boolean output = pin.ref().direction() == PinDirection.OUTPUT;
        double radius = clamp(UiMetrics.PIN_RADIUS_SCREEN * Math.sqrt(camera.zoom()), 12.0, 24.0)
                + (hovered || pending ? UiMetrics.PIN_HOVER_EXTRA_SCREEN : 0.0);
        canvas.circle(screen.x(), screen.y(), radius + UiMetrics.PIN_RING_PADDING_SCREEN,
                pending ? RenderTheme.WARNING : hovered ? new RenderTheme.Rgba(238, 246, 255, 255) : new RenderTheme.Rgba(3, 6, 13, 190));
        canvas.circle(screen.x(), screen.y(), radius, value ? RenderTheme.ACTIVE : output ? RenderTheme.INACTIVE : new RenderTheme.Rgba(99, 120, 160, 255));
    }

    private void drawTrashButton(double x, double y) {
        double size = UiMetrics.TRASH_BUTTON_SIZE_SCREEN;
        double centerX = x + size / 2.0;
        double centerY = y + size / 2.0;
        canvas.circle(centerX + 2.0, centerY + 3.0, size / 2.0, new RenderTheme.Rgba(0, 0, 0, 100));
        canvas.circle(centerX, centerY, size / 2.0, RenderTheme.DANGER);
        canvas.trashGlyph(x, y, RenderTheme.TEXT);
    }

    private RenderTheme.Rgba bodyColor(ComponentKind kind, boolean active) {
        if (!active) {
            return new RenderTheme.Rgba(16, 23, 38, 252);
        }
        return kind == ComponentKind.LED ? new RenderTheme.Rgba(122, 83, 17, 252) : new RenderTheme.Rgba(20, 99, 70, 252);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
