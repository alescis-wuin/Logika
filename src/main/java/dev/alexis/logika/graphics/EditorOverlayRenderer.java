package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.model.Wire;
import dev.alexis.logika.ui.*;
import dev.alexis.logika.util.Rect;
import dev.alexis.logika.util.Vec2;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_RIGHT;

final class EditorOverlayRenderer {
    private final NvgCanvas canvas;

    EditorOverlayRenderer(NvgCanvas canvas) { this.canvas = canvas; }

    void draw(Toolbar toolbar, Viewport viewport, Tool tool, PinRef pendingWire, boolean draggingComponent,
              boolean draggingWirePoint, boolean simulationRunning, String status, int selectedCount,
              Wire selectedWire, int clipboardCount, String modeLabel, int undoCount, int redoCount) {
        drawToolbar(toolbar, viewport, tool, simulationRunning);
        drawStatus(viewport, tool, pendingWire, draggingComponent, draggingWirePoint, simulationRunning, status,
                selectedCount, selectedWire != null, clipboardCount, undoCount, redoCount);
        if (selectedWire != null) drawWireInspector(viewport, selectedWire);
    }

    private void drawToolbar(Toolbar toolbar, Viewport viewport, Tool tool, boolean simulationRunning) {
        double y = viewport.windowHeight() - UiMetrics.TOOLBAR_PANEL_HEIGHT_SCREEN - UiMetrics.TOOLBAR_PANEL_MARGIN_SCREEN;
        canvas.fillRound(14.0, y, viewport.windowWidth() - 28.0, UiMetrics.TOOLBAR_PANEL_HEIGHT_SCREEN, 28.0, RenderTheme.PANEL);
        canvas.strokeRound(14.0, y, viewport.windowWidth() - 28.0, UiMetrics.TOOLBAR_PANEL_HEIGHT_SCREEN, 28.0, RenderTheme.PANEL_STROKE, 1.4f);
        for (Toolbar.Item item : toolbar.layout(viewport.windowWidth(), viewport.windowHeight())) {
            boolean selected = selectedTool(item.action(), tool);
            boolean simButton = item.action() == Toolbar.Action.SIMULATION;
            Rect r = item.rect();
            canvas.fillRound(r.x(), r.y(), r.width(), r.height(), 20.0, selected ? new RenderTheme.Rgba(48, 82, 133, 252) : new RenderTheme.Rgba(25, 36, 58, 244));
            canvas.strokeRound(r.x(), r.y(), r.width(), r.height(), 20.0, simButton && simulationRunning ? RenderTheme.ACTIVE : selected ? RenderTheme.ACCENT : RenderTheme.PANEL_STROKE, selected ? 2.6f : 1.4f);
            canvas.text(simButton ? (simulationRunning ? "Sim ON" : "Sim OFF") : item.label(), (float) r.centerX(), (float) (r.y() + 29.0), 19.0f, NVG_ALIGN_CENTER, RenderTheme.TEXT, true);
            canvas.text(item.hint(), (float) r.centerX(), (float) (r.y() + 55.0), 14.0f, NVG_ALIGN_CENTER, RenderTheme.TEXT_MUTED, false);
        }
    }

    private void drawStatus(Viewport viewport, Tool tool, PinRef pendingWire, boolean draggingComponent, boolean draggingWirePoint,
                            boolean simulationRunning, String status, int selectedCount, boolean wireSelected,
                            int clipboardCount, int undoCount, int redoCount) {
        double reserve = wireSelected ? UiMetrics.WIRE_INSPECTOR_WIDTH_SCREEN + 30.0 : 0.0;
        double width = Math.max(520.0, Math.min(1240.0, viewport.windowWidth() - 40.0 - reserve));
        canvas.fillRound(20.0, 16.0, width, 112.0, 22.0, new RenderTheme.Rgba(14, 21, 36, 236));
        canvas.strokeRound(20.0, 16.0, width, 112.0, 22.0, RenderTheme.PANEL_STROKE, 1.3f);
        String mode = "Tool: " + tool.label() + "  -  Sim: " + (simulationRunning ? "live" : "paused")
                + "  -  Selected: " + selectedCount + (wireSelected ? " + cable" : "")
                + "  -  Undo/redo: " + undoCount + "/" + redoCount;
        if (pendingWire != null) mode += "  -  node link";
        if (draggingComponent) mode += "  -  component drag";
        if (draggingWirePoint) mode += "  -  curve drag";
        canvas.text(mode, 42.0f, 40.0f, 18.0f, NVG_ALIGN_LEFT, RenderTheme.TEXT, true);
        canvas.text(status, 42.0f, 67.0f, 15.5f, NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED, false);
        canvas.text("Cable: select near the curve, click selected cable to add a point, drag numbered points.", 42.0f, 95.0f, 14.0f, NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED, false);
        canvas.text("Clipboard: " + clipboardCount, (float) (20.0 + width - 14.0), 40.0f, 14.0f, NVG_ALIGN_RIGHT, RenderTheme.TEXT_MUTED, false);
    }

    private void drawWireInspector(Viewport viewport, Wire wire) {
        Rect panel = WireInspectorLayout.panelBounds(viewport);
        canvas.fillRound(panel.x(), panel.y(), panel.width(), panel.height(), 26.0, RenderTheme.PANEL);
        canvas.strokeRound(panel.x(), panel.y(), panel.width(), panel.height(), 26.0, RenderTheme.PANEL_STROKE, 1.5f);
        canvas.text("Cable", (float) (panel.x() + 24.0), (float) (panel.y() + 30.0), 22.0f, NVG_ALIGN_LEFT, RenderTheme.TEXT, true);
        double y = panel.y() + 76.0;
        row(panel, y, "Color", String.format("#%06X", wire.colorRgb() & 0xFF_FF_FF));
        canvas.fillRound(panel.x() + panel.width() - 78.0, y - 13.0, 42.0, 24.0, 8.0, RenderTheme.fromRgb(wire.colorRgb(), 255));
        y += 34.0;
        row(panel, y, "From", pin(wire.from()));
        y += 34.0;
        row(panel, y, "To", pin(wire.to()));
        y += 34.0;
        row(panel, y, "Points", Integer.toString(wire.controlPoints().size()));
        Rect picker = WireInspectorLayout.colorPickerBounds(viewport);
        canvas.text("Color selector", (float) (panel.x() + 24.0), (float) (picker.y() - 22.0), 15.0f, NVG_ALIGN_LEFT, RenderTheme.TEXT, true);
        drawColorPicker(picker);
        Vec2 selector = WireInspectorLayout.selectorPosition(picker, wire.colorRgb());
        canvas.circle(selector.x(), selector.y(), 10.5, RenderTheme.TEXT.withAlpha(82));
        canvas.strokeCircle(selector.x(), selector.y(), 10.0, RenderTheme.TEXT, 2.2f);
        y = picker.y() + picker.height() + 34.0;
        if (wire.controlPoints().isEmpty()) {
            canvas.text("Click selected cable to add a curve point.", (float) (panel.x() + 24.0), (float) y, 13.5f, NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED, false);
        } else {
            for (int i = 0; i < Math.min(6, wire.controlPoints().size()); i++) {
                Vec2 point = wire.controlPoints().get(i);
                canvas.text((i + 1) + ": x=" + Math.round(point.x()) + ", y=" + Math.round(point.y()), (float) (panel.x() + 24.0), (float) y, 13.5f, NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED, false);
                y += 22.0;
            }
        }
    }

    private void row(Rect panel, double y, String label, String value) {
        canvas.text(label, (float) (panel.x() + 24.0), (float) y, 13.5f, NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED, false);
        canvas.text(value, (float) (panel.x() + 102.0), (float) y, 14.5f, NVG_ALIGN_LEFT, RenderTheme.TEXT, false);
    }

    private void drawColorPicker(Rect picker) {
        int columns = 24;
        int rows = 10;
        double cellWidth = picker.width() / columns;
        double cellHeight = picker.height() / rows;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                double hue = column / (double) Math.max(1, columns - 1);
                double value = 1.0 - row / (double) Math.max(1, rows - 1);
                canvas.fillRect(picker.x() + column * cellWidth, picker.y() + row * cellHeight, cellWidth + 0.7, cellHeight + 0.7,
                        RenderTheme.fromRgb(WireInspectorLayout.hsvToRgb(hue, 0.78, Math.max(0.16, value)), 255));
            }
        }
        canvas.strokeRound(picker.x(), picker.y(), picker.width(), picker.height(), 16.0, RenderTheme.TEXT.withAlpha(130), 1.2f);
    }

    private static String pin(PinRef ref) {
        return "#" + ref.componentId() + ":" + ref.direction().name().charAt(0) + ref.index();
    }

    private static boolean selectedTool(Toolbar.Action action, Tool tool) {
        return switch (action) {
            case BUTTON -> tool == Tool.PLACE_BUTTON;
            case SWITCH -> tool == Tool.PLACE_SWITCH;
            case NAND -> tool == Tool.PLACE_NAND;
            case LED -> tool == Tool.PLACE_LED;
            case SIMULATION, CLEAR -> false;
        };
    }
}
