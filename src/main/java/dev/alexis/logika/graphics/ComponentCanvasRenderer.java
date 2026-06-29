package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.CircuitComponent;
import dev.alexis.logika.model.ComponentKind;
import dev.alexis.logika.model.PinDirection;
import dev.alexis.logika.model.PinEndpoint;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.ui.PlacementPreview;
import dev.alexis.logika.ui.UiMetrics;
import dev.alexis.logika.util.Rect;
import dev.alexis.logika.util.Vec2;

import java.util.List;
import java.util.Set;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;

final class ComponentCanvasRenderer {
    private static final double MIN_TITLE_SIZE = 7.0;
    private static final double MIN_BADGE_SCALE = 0.42;
    private static final RenderTheme.Rgba SELECTION_COLOR = new RenderTheme.Rgba(87, 177, 255, 255);
    private static final RenderTheme.Rgba HOLOGRAM_COLOR = new RenderTheme.Rgba(110, 220, 255, 210);
    private static final RenderTheme.Rgba SLOT_BUTTON = new RenderTheme.Rgba(24, 58, 86, 250);
    private static final RenderTheme.Rgba SIGNAL_ON = new RenderTheme.Rgba(20, 105, 72, 242);
    private static final RenderTheme.Rgba SIGNAL_OFF = new RenderTheme.Rgba(28, 37, 56, 236);

    private final NvgCanvas canvas;

    ComponentCanvasRenderer(NvgCanvas canvas) {
        this.canvas = canvas;
    }

    void drawPlacementPreviews(Camera2D camera, Viewport viewport, List<PlacementPreview> previews) {
        for (PlacementPreview preview : previews) {
            drawPlacementPreview(camera, viewport, preview);
        }
    }

    private void drawPlacementPreview(Camera2D camera, Viewport viewport, PlacementPreview preview) {
        Rect bounds = preview.bounds();
        Vec2 topLeft = camera.worldToScreen(new Vec2(bounds.x(), bounds.y()), viewport);
        double width = bounds.width() * camera.zoom();
        double height = bounds.height() * camera.zoom();
        double radius = Math.min(clamp(UiMetrics.COMPONENT_RADIUS_SCREEN * Math.sqrt(camera.zoom()), 8.0, 32.0),
                Math.min(width, height) * 0.28);

        canvas.fillRound(topLeft.x(), topLeft.y(), width, height, radius, new RenderTheme.Rgba(55, 145, 220, 54));
        canvas.strokeRound(topLeft.x(), topLeft.y(), width, height, radius, HOLOGRAM_COLOR, 2.6f);
        drawSlotButton(camera, viewport, preview);

        if (width >= 130.0 && height >= 70.0) {
            canvas.text("Place " + preview.kind().label(), (float) (topLeft.x() + width / 2.0),
                    (float) (topLeft.y() + height * 0.43), 18.0f, NVG_ALIGN_CENTER, RenderTheme.TEXT, true);
            canvas.text(preview.alignmentMode(), (float) (topLeft.x() + width / 2.0),
                    (float) (topLeft.y() + height * 0.61), 13.5f, NVG_ALIGN_CENTER, RenderTheme.TEXT_MUTED, false);
        }

        CircuitComponent ghost = new CircuitComponent(-1, preview.kind(), bounds.x(), bounds.y());
        for (PinEndpoint pin : ghost.pins()) {
            Vec2 screen = camera.worldToScreen(pin.worldPosition(), viewport);
            double radiusPin = clamp(UiMetrics.PIN_RADIUS_SCREEN * camera.zoom(), 3.0, 20.0);
            canvas.circle(screen.x(), screen.y(), radiusPin + 4.0, new RenderTheme.Rgba(3, 6, 13, 180));
            canvas.circle(screen.x(), screen.y(), radiusPin, HOLOGRAM_COLOR);
        }
    }

    private void drawSlotButton(Camera2D camera, Viewport viewport, PlacementPreview preview) {
        Vec2 world = slotButtonWorldPosition(preview);
        if (world == null) {
            return;
        }
        Vec2 screen = camera.worldToScreen(world, viewport);
        double outerRadius = 28.0;
        double innerRadius = 20.0;
        canvas.circle(screen.x(), screen.y(), outerRadius, new RenderTheme.Rgba(2, 8, 18, 230));
        canvas.circle(screen.x(), screen.y(), innerRadius, SLOT_BUTTON);
        canvas.text("+", (float) screen.x(), (float) screen.y(), 30.0f, NVG_ALIGN_CENTER, RenderTheme.TEXT, true);
    }

    private Vec2 slotButtonWorldPosition(PlacementPreview preview) {
        Rect bounds = preview.bounds();
        return switch (preview.alignmentMode()) {
            case "left side" -> new Vec2(bounds.x() + bounds.width(), bounds.centerY());
            case "right side" -> new Vec2(bounds.x(), bounds.centerY());
            case "top side" -> new Vec2(bounds.centerX(), bounds.y() + bounds.height());
            case "bottom side" -> new Vec2(bounds.centerX(), bounds.y());
            default -> null;
        };
    }

    void draw(Camera2D camera, Viewport viewport, Circuit circuit, Set<Integer> selectedIds, int hoveredId,
              PinRef hoveredPin, PinRef pendingWire) {
        for (CircuitComponent component : circuit.components()) {
            Rect bounds = component.bounds();
            Vec2 topLeft = camera.worldToScreen(new Vec2(bounds.x(), bounds.y()), viewport);
            double width = bounds.width() * camera.zoom();
            double height = bounds.height() * camera.zoom();
            boolean active = component.visualActive();
            boolean selected = selectedIds.contains(component.id());
            boolean hovered = component.id() == hoveredId;
            RenderTheme.Rgba border = borderColor(active, selected, hovered);
            double radius = Math.min(clamp(UiMetrics.COMPONENT_RADIUS_SCREEN * Math.sqrt(camera.zoom()), 8.0, 32.0),
                    Math.min(width, height) * 0.28);

            canvas.fillRound(topLeft.x() + 6.0, topLeft.y() + 12.0, width, height, radius, new RenderTheme.Rgba(0, 0, 0, 105));
            if (selected) {
                canvas.fillRound(topLeft.x() - 5.0, topLeft.y() - 5.0, width + 10.0, height + 10.0,
                        radius + 6.0, new RenderTheme.Rgba(68, 161, 255, 45));
            }
            canvas.fillRound(topLeft.x(), topLeft.y(), width, height, radius, bodyColor(component.kind(), active));
            canvas.strokeRound(topLeft.x(), topLeft.y(), width, height, radius, border,
                    hovered ? 3.8f : selected ? 3.2f : 2.0f);
            drawComponentHeader(component, topLeft, width, height);

            if (width >= 118.0 && height >= 82.0) {
                for (PinEndpoint pin : component.pins()) {
                    drawSignalBadge(camera, viewport, circuit, pin, topLeft, width, height);
                }
            }
            if (selected) {
                drawSelectionBadge(topLeft.x() + width - 62.0, topLeft.y() + 14.0);
            }
            if (hovered) {
                drawTrashButton(topLeft.x() + UiMetrics.TRASH_BUTTON_MARGIN_SCREEN, topLeft.y() + UiMetrics.TRASH_BUTTON_MARGIN_SCREEN);
            }
            for (PinEndpoint pin : component.pins()) {
                drawPin(camera, viewport, circuit, pin, pin.ref().equals(hoveredPin), pin.ref().equals(pendingWire));
            }
        }
    }

    private void drawComponentHeader(CircuitComponent component, Vec2 topLeft, double width, double height) {
        double centerX = topLeft.x() + width / 2.0;
        double titleY = topLeft.y() + height * 0.20;
        float title = (float) Math.min(34.0, Math.min(width / 7.2, height / 3.3));
        if (title < MIN_TITLE_SIZE) {
            return;
        }
        canvas.text(component.kind().label(), (float) centerX, (float) titleY, title, NVG_ALIGN_CENTER, RenderTheme.TEXT, true);
        float description = (float) Math.min(16.0, title * 0.54);
        if (height >= 96.0 && width >= 168.0 && description >= 7.0) {
            canvas.text(component.kind().description(), (float) centerX, (float) (titleY + title * 0.92),
                    description, NVG_ALIGN_CENTER, RenderTheme.TEXT_MUTED, false);
        }
    }

    private void drawSignalBadge(Camera2D camera, Viewport viewport, Circuit circuit, PinEndpoint pin,
                                 Vec2 topLeft, double componentWidth, double componentHeight) {
        Vec2 pinScreen = camera.worldToScreen(pin.worldPosition(), viewport);
        boolean output = pin.ref().direction() == PinDirection.OUTPUT;
        boolean value = circuit.pinValue(pin.ref());
        double scale = Math.min(1.12, Math.min(componentWidth / 280.0, componentHeight / 184.0));
        if (scale < MIN_BADGE_SCALE) {
            return;
        }
        double badgeW = UiMetrics.SIGNAL_BADGE_WIDTH_SCREEN * scale;
        double badgeH = UiMetrics.SIGNAL_BADGE_HEIGHT_SCREEN * scale;
        double x = output ? pinScreen.x() - UiMetrics.SIGNAL_BADGE_NODE_GAP_SCREEN * scale - badgeW
                : pinScreen.x() + UiMetrics.SIGNAL_BADGE_NODE_GAP_SCREEN * scale;
        x = clamp(x, topLeft.x() + UiMetrics.COMPONENT_PADDING_SCREEN * scale,
                topLeft.x() + componentWidth - badgeW - UiMetrics.COMPONENT_PADDING_SCREEN * scale);

        double minY = topLeft.y() + UiMetrics.COMPONENT_PADDING_SCREEN * scale;
        double maxY = topLeft.y() + componentHeight - badgeH - UiMetrics.COMPONENT_PADDING_SCREEN * scale;
        if (maxY < minY) {
            minY = maxY;
        }
        double y = clamp(pinScreen.y() - badgeH / 2.0, minY, maxY);

        canvas.line(pinScreen.x(), pinScreen.y(), output ? x + badgeW : x, pinScreen.y(),
                value ? RenderTheme.ACTIVE : new RenderTheme.Rgba(139, 158, 197, 190), value ? 3.0f : 2.2f);
        canvas.fillRound(x, y, badgeW, badgeH, 14.0 * scale, value ? SIGNAL_ON : SIGNAL_OFF);
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
        double zoomScale = clamp(camera.zoom(), 0.22, 1.0);
        double radius = clamp(UiMetrics.PIN_RADIUS_SCREEN * camera.zoom(), 3.0, 24.0)
                + (hovered || pending ? UiMetrics.PIN_HOVER_EXTRA_SCREEN * zoomScale : 0.0);
        double ring = UiMetrics.PIN_RING_PADDING_SCREEN * zoomScale;
        canvas.circle(screen.x(), screen.y(), radius + ring,
                pending ? RenderTheme.WARNING : hovered ? new RenderTheme.Rgba(238, 246, 255, 255) : new RenderTheme.Rgba(3, 6, 13, 190));
        canvas.circle(screen.x(), screen.y(), radius, value ? RenderTheme.ACTIVE : output ? RenderTheme.INACTIVE : new RenderTheme.Rgba(99, 120, 160, 255));
    }

    private void drawSelectionBadge(double x, double y) {
        canvas.fillRound(x, y, 48.0, 28.0, 14.0, new RenderTheme.Rgba(37, 89, 152, 238));
        canvas.strokeRound(x, y, 48.0, 28.0, 14.0, SELECTION_COLOR, 1.6f);
        canvas.text("SEL", (float) (x + 24.0), (float) (y + 14.0), 13.5f, NVG_ALIGN_CENTER, RenderTheme.TEXT, true);
    }

    private void drawTrashButton(double x, double y) {
        double size = UiMetrics.TRASH_BUTTON_SIZE_SCREEN;
        double centerX = x + size / 2.0;
        double centerY = y + size / 2.0;
        canvas.circle(centerX + 2.0, centerY + 3.0, size / 2.0, new RenderTheme.Rgba(0, 0, 0, 100));
        canvas.circle(centerX, centerY, size / 2.0, RenderTheme.DANGER);
        canvas.trashGlyph(x, y, RenderTheme.TEXT);
    }

    private RenderTheme.Rgba borderColor(boolean active, boolean selected, boolean hovered) {
        if (hovered) {
            return new RenderTheme.Rgba(216, 232, 255, 255);
        }
        if (selected) {
            return SELECTION_COLOR;
        }
        return active ? RenderTheme.ACTIVE : RenderTheme.PANEL_STROKE;
    }

    private RenderTheme.Rgba bodyColor(ComponentKind kind, boolean active) {
        if (active) {
            return kind == ComponentKind.LED ? new RenderTheme.Rgba(122, 83, 17, 252) : new RenderTheme.Rgba(20, 99, 70, 252);
        }
        return switch (kind) {
            case BUTTON -> new RenderTheme.Rgba(18, 45, 79, 252);
            case SWITCH -> new RenderTheme.Rgba(45, 31, 82, 252);
            case NAND -> new RenderTheme.Rgba(32, 43, 76, 252);
            case LED -> new RenderTheme.Rgba(55, 42, 23, 252);
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
