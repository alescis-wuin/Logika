package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.ui.Tool;
import dev.alexis.logika.ui.Toolbar;
import dev.alexis.logika.ui.UiMetrics;
import dev.alexis.logika.util.Rect;

import java.util.List;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_RIGHT;

final class EditorOverlayRenderer {
    private final NvgCanvas canvas;

    EditorOverlayRenderer(NvgCanvas canvas) {
        this.canvas = canvas;
    }

    void draw(Toolbar toolbar, Viewport viewport, Tool tool, PinRef pendingWire, boolean draggingComponent,
              boolean simulationRunning, String status, int selectedCount, int clipboardCount, String chainVariantLabel,
              int undoCount, int redoCount) {
        drawToolbar(toolbar, viewport, tool, simulationRunning);
        drawStatus(tool, pendingWire, draggingComponent, simulationRunning, status, selectedCount, clipboardCount,
                chainVariantLabel, undoCount, redoCount, viewport);
    }

    private void drawToolbar(Toolbar toolbar, Viewport viewport, Tool tool, boolean simulationRunning) {
        double y = viewport.windowHeight() - UiMetrics.TOOLBAR_PANEL_HEIGHT_SCREEN - UiMetrics.TOOLBAR_PANEL_MARGIN_SCREEN;
        canvas.fillRound(14.0, y, viewport.windowWidth() - 28.0, UiMetrics.TOOLBAR_PANEL_HEIGHT_SCREEN, 28.0, RenderTheme.PANEL);
        canvas.strokeRound(14.0, y, viewport.windowWidth() - 28.0, UiMetrics.TOOLBAR_PANEL_HEIGHT_SCREEN, 28.0,
                RenderTheme.PANEL_STROKE, 1.4f);
        List<Toolbar.Item> items = toolbar.layout(viewport.windowWidth(), viewport.windowHeight());
        for (Toolbar.Item item : items) {
            boolean selected = isSelected(item.action(), tool);
            boolean simButton = item.action() == Toolbar.Action.SIMULATION;
            RenderTheme.Rgba fill = selected ? new RenderTheme.Rgba(48, 82, 133, 252) : new RenderTheme.Rgba(25, 36, 58, 244);
            RenderTheme.Rgba outline = selected ? RenderTheme.ACCENT : new RenderTheme.Rgba(122, 148, 194, 170);
            if (simButton && simulationRunning) {
                outline = RenderTheme.ACTIVE;
            }
            Rect r = item.rect();
            canvas.fillRound(r.x(), r.y(), r.width(), r.height(), 20.0, fill);
            canvas.strokeRound(r.x(), r.y(), r.width(), r.height(), 20.0, outline, selected ? 2.6f : 1.4f);
            canvas.text(simButton ? (simulationRunning ? "Sim ON" : "Sim OFF") : item.label(),
                    (float) r.centerX(), (float) (r.y() + 29.0), 19.0f, NVG_ALIGN_CENTER, RenderTheme.TEXT, true);
            canvas.text(item.hint(), (float) r.centerX(), (float) (r.y() + 55.0), 14.0f,
                    NVG_ALIGN_CENTER, RenderTheme.TEXT_MUTED, false);
        }
    }

    private void drawStatus(Tool tool, PinRef pendingWire, boolean draggingComponent, boolean simulationRunning,
                            String status, int selectedCount, int clipboardCount, String chainVariantLabel,
                            int undoCount, int redoCount, Viewport viewport) {
        double width = Math.min(1240.0, viewport.windowWidth() - 40.0);
        canvas.fillRound(20.0, 16.0, width, 128.0, 22.0, new RenderTheme.Rgba(14, 21, 36, 236));
        canvas.strokeRound(20.0, 16.0, width, 128.0, 22.0, RenderTheme.PANEL_STROKE, 1.3f);
        String mode = "Tool: " + tool.label()
                + "  -  Simulation: " + (simulationRunning ? "live" : "paused")
                + "  -  Selected: " + selectedCount
                + "  -  Chain: " + chainVariantLabel
                + "  -  History: " + undoCount + "/" + redoCount;
        if (pendingWire != null) {
            mode += "  -  Node linking";
        } else if (draggingComponent) {
            mode += "  -  Dragging";
        }
        canvas.text(mode, 42.0f, 40.0f, 18.0f, NVG_ALIGN_LEFT, RenderTheme.TEXT, true);
        canvas.text(status, 42.0f, 67.0f, 15.5f, NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED, false);
        canvas.text("Placement: Alt free - Ctrl pin row - Ctrl+Alt edge column - Ctrl+Z undo",
                42.0f, 94.0f, 14.0f, NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED, false);
        canvas.text("Area select, multi-select, copy/paste and variant shortcuts are active",
                42.0f, 117.0f, 14.0f, NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED, false);
        canvas.text("Clipboard: " + clipboardCount + " - zoom, pan and cancel shortcuts available",
                viewport.windowWidth() - 28.0f, 40.0f, 14.0f, NVG_ALIGN_RIGHT, RenderTheme.TEXT_MUTED, false);
    }

    private static boolean isSelected(Toolbar.Action action, Tool tool) {
        return switch (action) {
            case BUTTON -> tool == Tool.PLACE_BUTTON;
            case SWITCH -> tool == Tool.PLACE_SWITCH;
            case NAND -> tool == Tool.PLACE_NAND;
            case LED -> tool == Tool.PLACE_LED;
            case SIMULATION, CLEAR -> false;
        };
    }
}
