package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.ui.Tool;
import dev.alexis.logika.ui.Toolbar;
import dev.alexis.logika.ui.UiMetrics;
import dev.alexis.logika.util.Rect;

import java.util.Set;

import static org.lwjgl.nanovg.NanoVG.nvgBeginFrame;
import static org.lwjgl.nanovg.NanoVG.nvgEndFrame;
import static org.lwjgl.nanovg.NanoVGGL3.NVG_ANTIALIAS;
import static org.lwjgl.nanovg.NanoVGGL3.NVG_STENCIL_STROKES;
import static org.lwjgl.nanovg.NanoVGGL3.nvgCreate;
import static org.lwjgl.nanovg.NanoVGGL3.nvgDelete;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class NanoVGRenderer implements AutoCloseable {
    private long vg = NULL;
    private NvgCanvas canvas;
    private CircuitCanvasRenderer circuitRenderer;
    private EditorOverlayRenderer overlayRenderer;

    public void init() {
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == NULL) {
            throw new IllegalStateException("Unable to create NanoVG context.");
        }
        canvas = new NvgCanvas(vg);
        canvas.loadFonts();
        circuitRenderer = new CircuitCanvasRenderer(canvas);
        overlayRenderer = new EditorOverlayRenderer(canvas);
    }

    public void render(Viewport viewport, Camera2D camera, Circuit circuit, Toolbar toolbar, Tool tool,
                       PinRef pendingWire, Set<Integer> selectedComponentIds, int hoveredComponentId,
                       boolean draggingComponent, Rect selectionMarquee, boolean simulationRunning, String status,
                       double mouseX, double mouseY, int clipboardCount, String chainVariantLabel) {
        glViewport(0, 0, viewport.framebufferWidth(), viewport.framebufferHeight());
        glClearColor(RenderTheme.BACKGROUND.rf(), RenderTheme.BACKGROUND.gf(), RenderTheme.BACKGROUND.bf(), 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        PinRef hoveredPin = circuitRenderer.hoveredPin(camera, viewport, circuit, mouseX, mouseY);

        nvgBeginFrame(vg, viewport.windowWidth(), viewport.windowHeight(), (float) viewport.devicePixelRatio());
        canvas.fillRect(0, 0, viewport.windowWidth(), viewport.windowHeight(), RenderTheme.BACKGROUND);
        circuitRenderer.draw(camera, viewport, circuit, selectedComponentIds, hoveredComponentId, hoveredPin,
                pendingWire, selectionMarquee, mouseX, mouseY);
        overlayRenderer.draw(toolbar, viewport, tool, pendingWire, draggingComponent, simulationRunning, status,
                selectedComponentIds.size(), clipboardCount, chainVariantLabel);
        nvgEndFrame(vg);
    }

    public double gridSize() {
        return UiMetrics.GRID_SIZE;
    }

    @Override
    public void close() {
        if (vg != NULL) {
            nvgDelete(vg);
            vg = NULL;
        }
    }
}
