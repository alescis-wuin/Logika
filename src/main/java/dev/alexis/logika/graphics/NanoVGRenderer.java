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
    private static final Rgba BACKGROUND = new Rgba(12, 16, 28, 255);
    private static final Rgba PANEL = new Rgba(24, 31, 48, 238);
    private static final Rgba PANEL_STROKE = new Rgba(90, 113, 150, 120);
    private static final Rgba TEXT = new Rgba(232, 238, 249, 255);
    private static final Rgba TEXT_MUTED = new Rgba(150, 164, 190, 255);
    private static final Rgba ACTIVE = new Rgba(83, 232, 139, 255);
    private static final Rgba INACTIVE = new Rgba(117, 128, 150, 255);
    private static final Rgba ACCENT = new Rgba(101, 160, 255, 255);
    private static final Rgba WARNING = new Rgba(255, 192, 89, 255);

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
                       PinRef pendingWire, int selectedComponentId, boolean simulationRunning,
                       String status, double mouseX, double mouseY) {
        glViewport(0, 0, viewport.framebufferWidth(), viewport.framebufferHeight());
        glClearColor(BACKGROUND.rf(), BACKGROUND.gf(), BACKGROUND.bf(), 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        nvgBeginFrame(vg, viewport.windowWidth(), viewport.windowHeight(), (float) viewport.devicePixelRatio());
        fillRect(0, 0, viewport.windowWidth(), viewport.windowHeight(), BACKGROUND);
        fillRect(0, 0, viewport.windowWidth(), 96, new Rgba(16, 22, 38, 90));
        drawGrid(camera, viewport);
        drawWires(camera, viewport, circuit);
        drawPendingWire(camera, viewport, circuit, pendingWire, mouseX, mouseY);
        drawComponents(camera, viewport, circuit, selectedComponentId);
        drawToolbar(toolbar, viewport, tool, simulationRunning);
        drawStatus(tool, pendingWire, simulationRunning, status, viewport);
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
        while (step * camera.zoom() < 18.0) {
            step *= 2.0;
        }
        while (step * camera.zoom() > 72.0) {
            step /= 2.0;
        }

        drawGridLayer(camera, viewport, minX, maxX, minY, maxY, step, new Rgba(93, 112, 145, 36), 1.0f);
        drawGridLayer(camera, viewport, minX, maxX, minY, maxY, step * 4.0, new Rgba(106, 142, 190, 70), 1.15f);
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
        double control = Math.max(48.0, Math.abs(end.x() - start.x()) * 0.45);
        Rgba color = pending ? WARNING : (active ? ACTIVE : new Rgba(96, 108, 132, 210));

        nvgBeginPath(vg);
        nvgMoveTo(vg, (float) start.x(), (float) start.y());
        nvgBezierTo(vg, (float) (start.x() + control), (float) start.y(),
                (float) (end.x() - control), (float) end.y(), (float) end.x(), (float) end.y());
        stroke(new Rgba(6, 8, 14, 190), pending ? 7.0f : 6.0f);

        nvgBeginPath(vg);
        nvgMoveTo(vg, (float) start.x(), (float) start.y());
        nvgBezierTo(vg, (float) (start.x() + control), (float) start.y(),
                (float) (end.x() - control), (float) end.y(), (float) end.x(), (float) end.y());
        stroke(color, pending ? 3.0f : 2.7f);
    }

    private void drawComponents(Camera2D camera, Viewport viewport, Circuit circuit, int selectedComponentId) {
        for (CircuitComponent component : circuit.components()) {
            Rect bounds = component.bounds();
            Vec2 topLeft = camera.worldToScreen(new Vec2(bounds.x(), bounds.y()), viewport);
            double width = bounds.width() * camera.zoom();
            double height = bounds.height() * camera.zoom();
            boolean active = component.output();
            boolean selected = component.id() == selectedComponentId;
            Rgba body = component.kind() == ComponentKind.NAND ? new Rgba(35, 45, 68, 245) : new Rgba(32, 43, 64, 245);

            fillRound(topLeft.x() + 4.0, topLeft.y() + 8.0, width, height, 18.0, new Rgba(0, 0, 0, 70));
            fillRound(topLeft.x(), topLeft.y(), width, height, 18.0, body);
            strokeRound(topLeft.x(), topLeft.y(), width, height, 18.0, selected ? ACCENT : (active ? ACTIVE : PANEL_STROKE), selected ? 2.4f : 1.5f);

            if (component.kind() == ComponentKind.BUTTON) {
                drawButtonSymbol(component, topLeft, width, height);
            } else if (component.kind() == ComponentKind.SWITCH) {
                drawSwitchSymbol(component, topLeft, width, height);
            } else {
                drawNandSymbol(component, topLeft, width, height);
            }

            if (width > 72.0) {
                text(component.kind().label(), (float) (topLeft.x() + width / 2.0), (float) (topLeft.y() + 18.0 * camera.zoom()),
                        (float) clamp(11.0 * camera.zoom(), 9.0, 15.0), NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE, TEXT);
                text(component.valueLabel(), (float) (topLeft.x() + width / 2.0), (float) (topLeft.y() + height - 15.0 * camera.zoom()),
                        (float) clamp(9.0 * camera.zoom(), 8.0, 12.0), NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE, TEXT_MUTED);
            }

            for (PinEndpoint pin : component.pins()) {
                drawPin(camera, viewport, circuit, pin);
            }
        }
    }

    private void drawButtonSymbol(CircuitComponent component, Vec2 topLeft, double width, double height) {
        double radius = Math.max(8.0, Math.min(width, height) * 0.18);
        circle(topLeft.x() + width / 2.0, topLeft.y() + height / 2.0, radius + 5.0, new Rgba(0, 0, 0, 90));
        circle(topLeft.x() + width / 2.0, topLeft.y() + height / 2.0, radius, component.output() ? ACTIVE : INACTIVE);
    }

    private void drawSwitchSymbol(CircuitComponent component, Vec2 topLeft, double width, double height) {
        double switchWidth = Math.max(34.0, width * 0.42);
        double switchHeight = Math.max(16.0, height * 0.26);
        double x = topLeft.x() + width / 2.0 - switchWidth / 2.0;
        double y = topLeft.y() + height / 2.0 - switchHeight / 2.0;
        fillRound(x, y, switchWidth, switchHeight, switchHeight / 2.0,
                component.output() ? new Rgba(58, 145, 95, 255) : new Rgba(73, 81, 101, 255));
        double knobX = component.output() ? x + switchWidth - switchHeight / 2.0 : x + switchHeight / 2.0;
        circle(knobX, y + switchHeight / 2.0, switchHeight * 0.42, TEXT);
    }

    private void drawNandSymbol(CircuitComponent component, Vec2 topLeft, double width, double height) {
        double gateWidth = width * 0.42;
        double gateHeight = height * 0.32;
        double x = topLeft.x() + width / 2.0 - gateWidth / 2.0;
        double y = topLeft.y() + height / 2.0 - gateHeight / 2.0;
        fillRound(x, y, gateWidth, gateHeight, 10.0, new Rgba(21, 28, 43, 180));
        strokeRound(x, y, gateWidth, gateHeight, 10.0, component.output() ? ACTIVE : PANEL_STROKE, 1.2f);
        text("NAND", (float) (x + gateWidth / 2.0), (float) (y + gateHeight / 2.0), 11.0f, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE, TEXT);
    }

    private void drawPin(Camera2D camera, Viewport viewport, Circuit circuit, PinEndpoint pin) {
        Vec2 screen = camera.worldToScreen(pin.worldPosition(), viewport);
        boolean value = circuit.pinValue(pin.ref());
        boolean output = pin.ref().direction() == PinDirection.OUTPUT;
        double radius = clamp(7.0 * Math.sqrt(camera.zoom()), 5.0, 10.0);
        Rgba color = value ? ACTIVE : (output ? new Rgba(134, 152, 184, 255) : new Rgba(80, 95, 125, 255));

        circle(screen.x(), screen.y(), radius + 3.0, new Rgba(0, 0, 0, 120));
        circle(screen.x(), screen.y(), radius, color);

        if (camera.zoom() > 0.48) {
            int align = output ? NVG_ALIGN_LEFT : NVG_ALIGN_RIGHT;
            float dx = output ? 12.0f : -12.0f;
            text(pin.label(), (float) screen.x() + dx, (float) screen.y(), 10.0f, align | NVG_ALIGN_MIDDLE, TEXT_MUTED);
        }
    }

    private void drawToolbar(Toolbar toolbar, Viewport viewport, Tool tool, boolean simulationRunning) {
        double y = viewport.windowHeight() - 92.0;
        fillRound(14.0, y, viewport.windowWidth() - 28.0, 82.0, 24.0, PANEL);
        strokeRound(14.0, y, viewport.windowWidth() - 28.0, 82.0, 24.0, PANEL_STROKE, 1.0f);

        List<Toolbar.Item> items = toolbar.layout(viewport.windowWidth(), viewport.windowHeight());
        for (Toolbar.Item item : items) {
            boolean selected = isSelected(item.action(), tool);
            boolean simButton = item.action() == Toolbar.Action.SIMULATION;
            Rgba fill = selected ? new Rgba(63, 93, 142, 245) : new Rgba(31, 41, 61, 235);
            Rgba outline = selected ? ACCENT : new Rgba(85, 103, 134, 100);
            if (simButton && simulationRunning) {
                outline = ACTIVE;
            }

            Rect r = item.rect();
            fillRound(r.x(), r.y(), r.width(), r.height(), 16.0, fill);
            strokeRound(r.x(), r.y(), r.width(), r.height(), 16.0, outline, selected ? 2.0f : 1.0f);
            String label = simButton ? (simulationRunning ? "Sim ON" : "Sim OFF") : item.label();
            text(label, (float) r.centerX(), (float) (r.y() + 19.0), 13.0f, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE, TEXT);
            text(item.hint(), (float) r.centerX(), (float) (r.y() + 37.0), 9.5f, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE, TEXT_MUTED);
        }
    }

    private void drawStatus(Tool tool, PinRef pendingWire, boolean simulationRunning, String status, Viewport viewport) {
        fillRound(20.0, 18.0, Math.min(760.0, viewport.windowWidth() - 40.0), 52.0, 18.0, new Rgba(20, 27, 43, 225));
        strokeRound(20.0, 18.0, Math.min(760.0, viewport.windowWidth() - 40.0), 52.0, 18.0, PANEL_STROKE, 1.0f);
        String mode = "Tool: " + tool.label() + "  ·  Simulation: " + (simulationRunning ? "live" : "paused");
        if (pendingWire != null) {
            mode += "  ·  Wire: choose input";
        }
        text(mode, 40.0f, 36.0f, 13.0f, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE, TEXT);
        text(status, 40.0f, 57.0f, 11.0f, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE, TEXT_MUTED);
        text("Wheel zoom · RMB/MMB pan · Esc cancel · S simulate", viewport.windowWidth() - 26.0f, 36.0f,
                11.0f, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE, TEXT_MUTED);
    }

    private static boolean isSelected(Toolbar.Action action, Tool tool) {
        return switch (action) {
            case SELECT -> tool == Tool.SELECT;
            case WIRE -> tool == Tool.WIRE;
            case BUTTON -> tool == Tool.PLACE_BUTTON;
            case SWITCH -> tool == Tool.PLACE_SWITCH;
            case NAND -> tool == Tool.PLACE_NAND;
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
