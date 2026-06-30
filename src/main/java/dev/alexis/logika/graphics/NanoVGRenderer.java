package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.model.Wire;
import dev.alexis.logika.model.WireId;
import dev.alexis.logika.ui.PlacementPreview;
import dev.alexis.logika.ui.Tool;
import dev.alexis.logika.ui.Toolbar;
import dev.alexis.logika.ui.UiMetrics;
import dev.alexis.logika.util.Rect;

import java.util.List;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
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
    private TextInputOverlay textInputOverlay;
    private GlfwTextInputBridge textInputBridge;
    private final CursorFeedback cursorFeedback = new CursorFeedback();
    private final InteractionFeedbackResolver feedbackResolver = new InteractionFeedbackResolver();

    public void init() {
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == NULL) {
            throw new IllegalStateException("Unable to create NanoVG context.");
        }
        cursorFeedback.init();
        canvas = new NvgCanvas(vg);
        canvas.loadFonts();
        circuitRenderer = new CircuitCanvasRenderer(canvas);
        overlayRenderer = new EditorOverlayRenderer(canvas);
        textInputOverlay = new TextInputOverlay();
        textInputBridge = new GlfwTextInputBridge(glfwGetCurrentContext(), textInputOverlay);
        textInputBridge.install();
    }

    public void render(Viewport viewport, Camera2D camera, Circuit circuit, Toolbar toolbar, Tool tool,
                       List<PlacementPreview> placementPreviews, PinRef pendingWire, Set<Integer> selectedComponentIds,
                       int hoveredComponentId, boolean draggingComponent, Rect selectionMarquee,
                       WireId selectedWireId, WireId hoveredWireId, int hoveredWireControlPointIndex,
                       boolean draggingWireControlPoint, boolean simulationRunning, String status, double mouseX, double mouseY,
                       int clipboardCount, String chainVariantLabel, int undoCount, int redoCount) {
        glViewport(0, 0, viewport.framebufferWidth(), viewport.framebufferHeight());
        glClearColor(RenderTheme.BACKGROUND.rf(), RenderTheme.BACKGROUND.gf(), RenderTheme.BACKGROUND.bf(), 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        double timeSeconds = System.nanoTime() / 1_000_000_000.0;
        if (textInputOverlay != null) {
            textInputOverlay.update(viewport, mouseX, mouseY);
        }
        PinRef hoveredPin = circuitRenderer.hoveredPin(camera, viewport, circuit, mouseX, mouseY);
        WireTargetFeedback targetFeedback = feedbackResolver.resolveWireTarget(camera, viewport, circuit,
                pendingWire, hoveredPin, mouseX, mouseY);
        CursorFeedback.Style cursorStyle = textInputOverlay != null && textInputOverlay.hovered()
                ? CursorFeedback.Style.TEXT
                : feedbackResolver.resolveCursor(toolbar, viewport, placementPreviews, selectedComponentIds,
                hoveredComponentId, draggingComponent, hoveredPin, hoveredWireId, hoveredWireControlPointIndex,
                draggingWireControlPoint, targetFeedback, mouseX, mouseY);
        cursorFeedback.apply(cursorStyle);

        Wire selectedWire = selectedWireId == null ? null : circuit.wireById(selectedWireId).orElse(null);
        nvgBeginFrame(vg, viewport.windowWidth(), viewport.windowHeight(), (float) viewport.devicePixelRatio());
        canvas.fillRect(0, 0, viewport.windowWidth(), viewport.windowHeight(), RenderTheme.BACKGROUND);
        circuitRenderer.draw(camera, viewport, circuit, placementPreviews, selectedComponentIds, hoveredComponentId,
                hoveredPin, pendingWire, selectedWireId, hoveredWireId, hoveredWireControlPointIndex,
                targetFeedback, selectionMarquee, timeSeconds, mouseX, mouseY);
        overlayRenderer.draw(toolbar, viewport, tool, pendingWire, draggingComponent, draggingWireControlPoint,
                simulationRunning, status, selectedComponentIds.size(), selectedWire, clipboardCount, chainVariantLabel,
                undoCount, redoCount);
        if (textInputOverlay != null) {
            textInputOverlay.draw(canvas, viewport, timeSeconds);
        }
        nvgEndFrame(vg);
    }

    public double gridSize() {
        return UiMetrics.GRID_SIZE;
    }

    @Override
    public void close() {
        if (textInputBridge != null) {
            textInputBridge.close();
            textInputBridge = null;
        }
        textInputOverlay = null;
        cursorFeedback.close();
        if (vg != NULL) {
            nvgDelete(vg);
            vg = NULL;
        }
    }
}
