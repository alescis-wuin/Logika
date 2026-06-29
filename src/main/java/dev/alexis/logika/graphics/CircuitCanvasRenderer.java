package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.PinRef;

final class CircuitCanvasRenderer {
    private final GridWireRenderer gridWireRenderer;
    private final ComponentCanvasRenderer componentRenderer;

    CircuitCanvasRenderer(NvgCanvas canvas) {
        gridWireRenderer = new GridWireRenderer(canvas);
        componentRenderer = new ComponentCanvasRenderer(canvas);
    }

    PinRef hoveredPin(Camera2D camera, Viewport viewport, Circuit circuit, double mouseX, double mouseY) {
        return gridWireRenderer.hoveredPin(camera, viewport, circuit, mouseX, mouseY);
    }

    void draw(Camera2D camera, Viewport viewport, Circuit circuit, int selectedId, int hoveredId,
              PinRef hoveredPin, PinRef pendingWire, double mouseX, double mouseY) {
        gridWireRenderer.draw(camera, viewport, circuit, pendingWire, mouseX, mouseY);
        componentRenderer.draw(camera, viewport, circuit, selectedId, hoveredId, hoveredPin, pendingWire);
    }
}
