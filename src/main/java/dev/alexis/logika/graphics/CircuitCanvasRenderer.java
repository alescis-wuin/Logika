package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.ui.PlacementPreview;
import dev.alexis.logika.util.Rect;
import dev.alexis.logika.util.Vec2;

import java.util.List;
import java.util.Set;

final class CircuitCanvasRenderer {
    private static final RenderTheme.Rgba SELECTION_COLOR = new RenderTheme.Rgba(87, 177, 255, 255);

    private final NvgCanvas canvas;
    private final GridWireRenderer gridWireRenderer;
    private final ComponentCanvasRenderer componentRenderer;

    CircuitCanvasRenderer(NvgCanvas canvas) {
        this.canvas = canvas;
        gridWireRenderer = new GridWireRenderer(canvas);
        componentRenderer = new ComponentCanvasRenderer(canvas);
    }

    PinRef hoveredPin(Camera2D camera, Viewport viewport, Circuit circuit, double mouseX, double mouseY) {
        return gridWireRenderer.hoveredPin(camera, viewport, circuit, mouseX, mouseY);
    }

    void draw(Camera2D camera, Viewport viewport, Circuit circuit, List<PlacementPreview> placementPreviews,
              Set<Integer> selectedIds, int hoveredId, PinRef hoveredPin, PinRef pendingWire,
              WireTargetFeedback targetFeedback, Rect selectionMarquee, double timeSeconds, double mouseX, double mouseY) {
        gridWireRenderer.draw(camera, viewport, circuit, pendingWire, targetFeedback, timeSeconds, mouseX, mouseY);
        drawSelectionMarquee(camera, viewport, selectionMarquee);
        componentRenderer.drawPlacementPreviews(camera, viewport, placementPreviews);
        componentRenderer.draw(camera, viewport, circuit, selectedIds, hoveredId, hoveredPin, pendingWire,
                targetFeedback, timeSeconds);
    }

    private void drawSelectionMarquee(Camera2D camera, Viewport viewport, Rect worldRect) {
        if (worldRect == null || worldRect.width() <= 0.0 || worldRect.height() <= 0.0) {
            return;
        }

        Vec2 a = camera.worldToScreen(new Vec2(worldRect.x(), worldRect.y()), viewport);
        Vec2 b = camera.worldToScreen(new Vec2(worldRect.x() + worldRect.width(), worldRect.y() + worldRect.height()), viewport);
        double x = Math.min(a.x(), b.x());
        double y = Math.min(a.y(), b.y());
        double width = Math.abs(a.x() - b.x());
        double height = Math.abs(a.y() - b.y());
        canvas.fillRound(x, y, width, height, 18.0, new RenderTheme.Rgba(81, 156, 255, 42));
        canvas.strokeRound(x, y, width, height, 18.0, SELECTION_COLOR, 2.2f);
    }
}
