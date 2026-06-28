package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.CircuitComponent;
import dev.alexis.logika.model.ComponentKind;
import dev.alexis.logika.model.PinDirection;
import dev.alexis.logika.model.PinEndpoint;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.model.Wire;
import dev.alexis.logika.ui.Tool;
import dev.alexis.logika.ui.Toolbar;
import dev.alexis.logika.util.Rect;
import dev.alexis.logika.util.Vec2;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_MIDDLE;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_RIGHT;
import static org.lwjgl.nanovg.NanoVG.nvgBeginFrame;
import static org.lwjgl.nanovg.NanoVG.nvgBeginPath;
import static org.lwjgl.nanovg.NanoVG.nvgBezierTo;
import static org.lwjgl.nanovg.NanoVG.nvgCircle;
import static org.lwjgl.nanovg.NanoVG.nvgCreateFont;
import static org.lwjgl.nanovg.NanoVG.nvgEndFrame;
import static org.lwjgl.nanovg.NanoVG.nvgFill;
import static org.lwjgl.nanovg.NanoVG.nvgFillColor;
import static org.lwjgl.nanovg.NanoVG.nvgFontFace;
import static org.lwjgl.nanovg.NanoVG.nvgFontSize;
import static org.lwjgl.nanovg.NanoVG.nvgLineTo;
import static org.lwjgl.nanovg.NanoVG.nvgMoveTo;
import static org.lwjgl.nanovg.NanoVG.nvgRGBA;
import static org.lwjgl.nanovg.NanoVG.nvgRect;
import static org.lwjgl.nanovg.NanoVG.nvgRestore;
import static org.lwjgl.nanovg.NanoVG.nvgRoundedRect;
import static org.lwjgl.nanovg.NanoVG.nvgSave;
import static org.lwjgl.nanovg.NanoVG.nvgStroke;
import static org.lwjgl.nanovg.NanoVG.nvgStrokeColor;
import static org.lwjgl.nanovg.NanoVG.nvgStrokeWidth;
import static org.lwjgl.nanovg.NanoVG.nvgText;
import static org.lwjgl.nanovg.NanoVG.nvgTextAlign;
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
    private static final double GRID_SIZE = 32.0;
    private static final double TRASH_ICON_SIZE = 34.0;
    private static final double TRASH_ICON_MARGIN = 8.0;

    private static final Rgba BACKGROUND = new Rgba(12, 16, 28, 255);
    private static final Rgba PANEL = new Rgba(24, 31, 48, 240);
    private static final Rgba PANEL_STROKE = new Rgba(105, 126, 164, 130);
    private static final Rgba TEXT = new Rgba(238, 244, 255, 255);
    private static final Rgba TEXT_MUTED = new Rgba(172, 186, 214, 255);
    private static final Rgba ACTIVE = new Rgba(73, 210, 128, 255);
    private static final Rgba INACTIVE = new Rgba(94, 106, 132, 255);
    private static final Rgba ACCENT = new Rgba(132, 184, 255, 255);
    private static final Rgba WARNING = new Rgba(255, 195, 92, 255);
    private static final Rgba DANGER = new Rgba(238, 92, 92, 255);

    private long vg = NULL;
    private boolean fontLoaded;

    public void init() {
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == NULL) {
            throw new IllegalStateException("Unable to create NanoVG context.");
        }
        fontLoaded = loadFont();
    }

    public void render(Viewport viewport, Camera2D camera, Circuit circuit, Toolbar toolbar, Tool tool,
                       PinRef pendingWire, int selectedComponentId, int hoveredComponentId,
                       boolean draggingComponent, boolean simulationRunning, String status,
                       double mouseX, double mouseY) {
        glViewport(0, 0, viewport.framebufferWidth(), viewport.framebufferHeight());
        glClearColor(BACKGROUND.rf(), BACKGROUND.gf(), BACKGROUND.bf(), 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        Vec2 mouseWorld = camera.screenToWorld(new Vec2(mouseX, mouseY), viewport);
        PinRef hoveredPin = circuit.findPin(mouseWorld, Math.max(14.0, 24.0 / camera.zoom()))
                .map(PinEndpoint::ref)
                .orElse(null);

        nvgBeginFrame(vg, viewport.windowWidth(), viewport.windowHeight(), (float) viewport.devicePixelRatio());
        fillRect(0, 0, viewport.windowWidth(), viewport.windowHeight(), BACKGROUND);
        drawGrid(camera, viewport);
        drawWires(camera, viewport, circuit);
        drawPendingWire(camera, viewport, circuit, pendingWire, mouseX, mouseY);
        drawComponents(camera, viewport, circuit, selectedComponentId, hoveredComponentId, hoveredPin, pendingWire);
        drawToolbar(toolbar, viewport, tool, simulationRunning);
        drawStatus(tool, pendingWire, draggingComponent, simulationRunning, status, viewport);
        nvgEndFrame(vg);
    }

    public double gridSize() {
        return GRID_SIZE;
    }

    private boolean loadFont() {
        String envFont = System.getenv("LOGIKA_FONT");
        String[] candidates = {
                envFont,
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
                "/usr/share/fonts/TTF/DejaVuSans.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf",
                "C:\\Windows\\Fonts\\arial.ttf"
        };

        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path) && nvgCreateFont(vg, "sans", path.toString()) >= 0) {
                return true;
            }
        }
        System.err.println("NanoVG text font not found. Set LOGIKA_FONT if text is missing.");
        return false;
    }

    private void drawGrid(Camera2D camera, Viewport viewport) {
        Vec2 topLeft = camera.screenToWorld(new Vec2(0, 0), viewport);
        Vec2 bottomRight = camera.screenToWorld(new Vec2(viewport.windowWidth(), viewport.windowHeight()), viewport);
        double minX = Math.min(topLeft.x(), bottomRight.x());
        double maxX = Math.max(topLeft.x(), bottomRight.x());
        double minY = Math.min(topLeft.y(), bottomRight.y());
        double maxY = Math.max(topLeft.y(), bottomRight.y());

        double step = GRID_SIZE;
        while (step * camera.zoom() < 24.0) {
            step *= 2.0;
        }
        while (step * camera.zoom() > 92.0) {
            step /= 2.0;
        }

        drawGridLayer(camera, viewport, minX, maxX, minY, maxY, step, new Rgba(93, 112, 145, 38), 1.0f);
        drawGridLayer(camera, viewport, minX, maxX, minY, maxY, step * 4.0, new Rgba(116, 154, 204, 78), 1.35f);
        drawAxes(camera, viewport, minX, maxX, minY, maxY);
    }

    private void drawGridLayer(Camera2D camera, Viewport viewport, double minX, double maxX, double minY, double maxY,
                               double step, Rgba color, float width) {
        nvgBeginPath(vg);
        double startX = Math.floor(minX / step) * step;
        double startY = Math.floor(minY / step) * step;

        for (double x = startX; x <= maxX; x += step) {
            Vec2 screen = camera.worldToScreen(new Vec2(x, minY), viewport);
            nvgMoveTo(vg, (float) screen.x(), 0.0f);
            nvgLineTo(vg, (float) screen.x(), viewport.windowHeight());
        }
        for (double y = startY; y <= maxY; y += step) {
            Vec2 screen = camera.worldToScreen(new Vec2(minX, y), viewport);
            nvgMoveTo(vg, 0.0f, (float) screen.y());
            nvgLineTo(vg, viewport.windowWidth(), (float) screen.y());
        }
        stroke(color, width);
    }

    private void drawAxes(Camera2D camera, Viewport viewport, double minX, double maxX, double minY, double maxY) {
        if (minY <= 0.0 && maxY >= 0.0) {
            Vec2 start = camera.worldToScreen(new Vec2(minX, 0.0), viewport);
            Vec2 end = camera.worldToScreen(new Vec2(maxX, 0.0), viewport);
            nvgBeginPath(vg);
            nvgMoveTo(vg, (float) start.x(), (float) start.y());
            nvgLineTo(vg, (float) end.x(), (float) end.y());
            stroke(new Rgba(126, 164, 220, 140), 2.0f);
            text("X axis", viewport.windowWidth() - 74.0f, (float) start.y() - 12.0f, 14.0f,
                    NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE, TEXT_MUTED);
        }
        if (minX <= 0.0 && maxX >= 0.0) {
            Vec2 start = camera.worldToScreen(new Vec2(0.0, minY), viewport);
            Vec2 end = camera.worldToScreen(new Vec2(0.0, maxY), viewport);
            nvgBeginPath(vg);
            nvgMoveTo(vg, (float) start.x(), (float) start.y());
            nvgLineTo(vg, (float) end.x(), (float) end.y());
            stroke(new Rgba(126, 164, 220, 140), 2.0f);
            text("Y axis", (float) start.x() + 16.0f, 104.0f, 14.0f,
                    NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE, TEXT_MUTED);
        }
    }

    private void drawWires(Camera2D camera, Viewport viewport, Circuit circuit) {
        for (Wire wire : circuit.wires()) {
            Optional<Vec2> start = circuit.pinPosition(wire.from());
            Optional<Vec2> end = circuit.pinPosition(wire.to());
            if (start.isPresent() && end.isPresent()) {
                drawWire(camera, viewport, start.get(), end.get(), circuit.pinValue(wire.from()), false);
            }
        }
    }

    private void drawPendingWire(Camera2D camera, Viewport viewport, Circuit circuit, PinRef pendingWire, double mouseX, double mouseY) {
        if (pendingWire == null) {
            return;
        }
        circuit.pinPosition(pendingWire).ifPresent(start -> {
            Vec2 end = camera.screenToWorld(new Vec2(mouseX, mouseY), viewport);
            drawWire(camera, viewport, start, end, circuit.pinValue(pendingWire), true);
        });
    }

    private void drawWire(Camera2D camera, Viewport viewport, Vec2 startWorld, Vec2 endWorld, boolean active, boolean pending) {
        Vec2 start = camera.worldToScreen(startWorld, viewport);
        Vec2 end = camera.worldToScreen(endWorld, viewport);
        double control = Math.max(58.0, Math.abs(end.x() - start.x()) * 0.45);
        Rgba color = pending ? WARNING : (active ? ACTIVE : new Rgba(103, 116, 143, 220));

        nvgBeginPath(vg);
        nvgMoveTo(vg, (float) start.x(), (float) start.y());
        nvgBezierTo(vg, (float) (start.x() + control), (float) start.y(),
                (float) (end.x() - control), (float) end.y(), (float) end.x(), (float) end.y());
        stroke(new Rgba(5, 7, 13, 200), pending ? 8.0f : 7.0f);

        nvgBeginPath(vg);
        nvgMoveTo(vg, (float) start.x(), (float) start.y());
        nvgBezierTo(vg, (float) (start.x() + control), (float) start.y(),
                (float) (end.x() - control), (float) end.y(), (float) end.x(), (float) end.y());
        stroke(color, pending ? 4.0f : 3.5f);
    }

    private void drawComponents(Camera2D camera, Viewport viewport, Circuit circuit, int selectedComponentId,
                                int hoveredComponentId, PinRef hoveredPin, PinRef pendingWire) {
        for (CircuitComponent component : circuit.components()) {
            Rect bounds = component.bounds();
            Vec2 topLeft = camera.worldToScreen(new Vec2(bounds.x(), bounds.y()), viewport);
            double width = bounds.width() * camera.zoom();
            double height = bounds.height() * camera.zoom();
            boolean active = component.visualActive();
            boolean selected = component.id() == selectedComponentId;
            boolean hovered = component.id() == hoveredComponentId;

            Rgba body = bodyColor(component.kind(), active);
            Rgba border = hovered ? new Rgba(190, 214, 255, 255) : selected ? ACCENT : active ? ACTIVE : PANEL_STROKE;
            float borderWidth = hovered ? 3.0f : selected ? 2.5f : 1.6f;

            fillRound(topLeft.x() + 5.0, topLeft.y() + 10.0, width, height, 22.0, new Rgba(0, 0, 0, 80));
            fillRound(topLeft.x(), topLeft.y(), width, height, 22.0, body);
            strokeRound(topLeft.x(), topLeft.y(), width, height, 22.0, border, borderWidth);

            double labelY = topLeft.y() + Math.max(30.0, height * 0.36);
            double valueY = topLeft.y() + Math.max(56.0, height * 0.67);
            text(component.kind().label(), (float) (topLeft.x() + width / 2.0), (float) labelY,
                    (float) clamp(16.0 * camera.zoom(), 13.0, 22.0), NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE, TEXT);
            text(component.valueLabel(), (float) (topLeft.x() + width / 2.0), (float) valueY,
                    (float) clamp(13.0 * camera.zoom(), 11.0, 18.0), NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE, TEXT_MUTED);

            if (hovered) {
                drawTrashIcon(topLeft.x() + TRASH_ICON_MARGIN, topLeft.y() + TRASH_ICON_MARGIN);
            }

            for (PinEndpoint pin : component.pins()) {
                drawPin(camera, viewport, circuit, pin, pin.ref().equals(hoveredPin), pin.ref().equals(pendingWire));
            }
        }
    }

    private Rgba bodyColor(ComponentKind kind, boolean active) {
        if (!active) {
            return new Rgba(23, 30, 46, 248);
        }
        if (kind == ComponentKind.LED) {
            return new Rgba(170, 132, 34, 252);
        }
        return new Rgba(34, 105, 70, 252);
    }

    private void drawPin(Camera2D camera, Viewport viewport, Circuit circuit, PinEndpoint pin, boolean hovered, boolean pending) {
        Vec2 screen = camera.worldToScreen(pin.worldPosition(), viewport);
        boolean value = circuit.pinValue(pin.ref());
        boolean output = pin.ref().direction() == PinDirection.OUTPUT;
        double radius = clamp(12.5 * Math.sqrt(camera.zoom()), 8.5, 17.0);
        if (hovered || pending) {
            radius += 3.0;
        }
        Rgba color = value ? ACTIVE : (output ? new Rgba(145, 164, 200, 255) : new Rgba(91, 107, 140, 255));
        Rgba border = pending ? WARNING : hovered ? new Rgba(225, 236, 255, 255) : new Rgba(4, 7, 13, 160);

        circle(screen.x(), screen.y(), radius + 4.0, border);
        circle(screen.x(), screen.y(), radius, color);

        if (camera.zoom() > 0.36) {
            int align = output ? NVG_ALIGN_LEFT : NVG_ALIGN_RIGHT;
            float dx = output ? 17.0f : -17.0f;
            text(pin.label(), (float) screen.x() + dx, (float) screen.y(), 13.5f, align | NVG_ALIGN_MIDDLE, TEXT);
        }
    }

    private void drawTrashIcon(double x, double y) {
        fillRound(x, y, TRASH_ICON_SIZE, TRASH_ICON_SIZE, 10.0, new Rgba(88, 32, 40, 248));
        strokeRound(x, y, TRASH_ICON_SIZE, TRASH_ICON_SIZE, 10.0, DANGER, 1.6f);

        double cx = x + TRASH_ICON_SIZE / 2.0;
        double top = y + 10.0;
        nvgBeginPath(vg);
        nvgMoveTo(vg, (float) (cx - 9.0), (float) top);
        nvgLineTo(vg, (float) (cx + 9.0), (float) top);
        nvgMoveTo(vg, (float) (cx - 5.0), (float) (top - 3.0));
        nvgLineTo(vg, (float) (cx + 5.0), (float) (top - 3.0));
        nvgRect(vg, (float) (cx - 7.0), (float) (top + 4.0), 14.0f, 14.0f);
        stroke(TEXT, 1.8f);
    }

    private void drawToolbar(Toolbar toolbar, Viewport viewport, Tool tool, boolean simulationRunning) {
        double y = viewport.windowHeight() - 108.0;
        fillRound(14.0, y, viewport.windowWidth() - 28.0, 96.0, 26.0, PANEL);
        strokeRound(14.0, y, viewport.windowWidth() - 28.0, 96.0, 26.0, PANEL_STROKE, 1.2f);

        List<Toolbar.Item> items = toolbar.layout(viewport.windowWidth(), viewport.windowHeight());
        for (Toolbar.Item item : items) {
            boolean selected = isSelected(item.action(), tool);
            boolean simButton = item.action() == Toolbar.Action.SIMULATION;
            Rgba fill = selected ? new Rgba(66, 98, 148, 248) : new Rgba(31, 41, 61, 240);
            Rgba outline = selected ? ACCENT : new Rgba(91, 110, 144, 130);
            if (simButton && simulationRunning) {
                outline = ACTIVE;
            }

            Rect r = item.rect();
            fillRound(r.x(), r.y(), r.width(), r.height(), 18.0, fill);
            strokeRound(r.x(), r.y(), r.width(), r.height(), 18.0, outline, selected ? 2.2f : 1.2f);
            String label = simButton ? (simulationRunning ? "Sim ON" : "Sim OFF") : item.label();
            text(label, (float) r.centerX(), (float) (r.y() + 23.0), 16.0f, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE, TEXT);
            text(item.hint(), (float) r.centerX(), (float) (r.y() + 45.0), 11.5f, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE, TEXT_MUTED);
        }
    }

    private void drawStatus(Tool tool, PinRef pendingWire, boolean draggingComponent, boolean simulationRunning,
                            String status, Viewport viewport) {
        double width = Math.min(1040.0, viewport.windowWidth() - 40.0);
        fillRound(20.0, 16.0, width, 84.0, 20.0, new Rgba(20, 27, 43, 230));
        strokeRound(20.0, 16.0, width, 84.0, 20.0, PANEL_STROKE, 1.1f);

        String mode = "Tool: " + tool.label() + "  ·  Simulation: " + (simulationRunning ? "live" : "paused");
        if (pendingWire != null) {
            mode += "  ·  Node linking: choose an input";
        } else if (draggingComponent) {
            mode += "  ·  Dragging";
        }
        text(mode, 42.0f, 38.0f, 16.0f, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE, TEXT);
        text(status, 42.0f, 62.0f, 14.0f, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE, TEXT_MUTED);
        text("Nodes: output → input · Drag: hold + move · Delete: hover trash", 42.0f, 84.0f,
                13.0f, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE, TEXT_MUTED);
        text("Wheel zoom · RMB/MMB/Space pan · Esc cancel · S sim · C center", viewport.windowWidth() - 26.0f,
                38.0f, 13.0f, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE, TEXT_MUTED);
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

    private void fillRect(double x, double y, double width, double height, Rgba color) {
        nvgBeginPath(vg);
        nvgRect(vg, (float) x, (float) y, (float) width, (float) height);
        fill(color);
    }

    private void fillRound(double x, double y, double width, double height, double radius, Rgba color) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, (float) x, (float) y, (float) width, (float) height, (float) radius);
        fill(color);
    }

    private void strokeRound(double x, double y, double width, double height, double radius, Rgba color, float strokeWidth) {
        nvgBeginPath(vg);
        nvgRoundedRect(vg, (float) x, (float) y, (float) width, (float) height, (float) radius);
        stroke(color, strokeWidth);
    }

    private void circle(double x, double y, double radius, Rgba color) {
        nvgBeginPath(vg);
        nvgCircle(vg, (float) x, (float) y, (float) radius);
        fill(color);
    }

    private void text(String value, float x, float y, float size, int align, Rgba color) {
        if (!fontLoaded) {
            return;
        }
        nvgSave(vg);
        nvgFontFace(vg, "sans");
        nvgFontSize(vg, size);
        nvgTextAlign(vg, align);
        fillColor(color);
        nvgText(vg, x, y, value);
        nvgRestore(vg);
    }

    private void fill(Rgba color) {
        fillColor(color);
        nvgFill(vg);
    }

    private void stroke(Rgba color, float width) {
        strokeColor(color);
        nvgStrokeWidth(vg, width);
        nvgStroke(vg);
    }

    private void fillColor(Rgba color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgRGBA((byte) color.r, (byte) color.g, (byte) color.b, (byte) color.a, nvgColor);
            nvgFillColor(vg, nvgColor);
        }
    }

    private void strokeColor(Rgba color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor nvgColor = NVGColor.malloc(stack);
            nvgRGBA((byte) color.r, (byte) color.g, (byte) color.b, (byte) color.a, nvgColor);
            nvgStrokeColor(vg, nvgColor);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void close() {
        if (vg != NULL) {
            nvgDelete(vg);
            vg = NULL;
        }
    }

    private record Rgba(int r, int g, int b, int a) {
        float rf() {
            return r / 255.0f;
        }

        float gf() {
            return g / 255.0f;
        }

        float bf() {
            return b / 255.0f;
        }
    }
}
