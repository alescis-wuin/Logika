package dev.alexis.logika.engine;

import dev.alexis.logika.audio.AudioService;
import dev.alexis.logika.graphics.Camera2D;
import dev.alexis.logika.graphics.NanoVGRenderer;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.CircuitComponent;
import dev.alexis.logika.model.ComponentKind;
import dev.alexis.logika.model.PinEndpoint;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.simulation.LogicSimulator;
import dev.alexis.logika.ui.Tool;
import dev.alexis.logika.ui.Toolbar;
import dev.alexis.logika.ui.UiMetrics;
import dev.alexis.logika.util.Rect;
import dev.alexis.logika.util.Vec2;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.List;
import java.util.Optional;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_X11;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_STENCIL_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwInitHint;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class LogikaEngine {
    private static final int INITIAL_WIDTH = 1280;
    private static final int INITIAL_HEIGHT = 820;
    private static final double DRAG_THRESHOLD_PX = 7.0;

    private final Viewport viewport = new Viewport(INITIAL_WIDTH, INITIAL_HEIGHT, INITIAL_WIDTH, INITIAL_HEIGHT);
    private final InputState input = new InputState();
    private final Camera2D camera = new Camera2D();
    private final Circuit circuit = new Circuit();
    private final LogicSimulator simulator = new LogicSimulator();
    private final Toolbar toolbar = new Toolbar();
    private final NanoVGRenderer renderer = new NanoVGRenderer();
    private final AudioService audio = new AudioService();

    private GLFWErrorCallback errorCallback;
    private long window = NULL;
    private Tool tool = Tool.INTERACT;
    private PinRef pendingWire;
    private int selectedComponentId = -1;
    private int hoveredComponentId = -1;
    private int pressedButtonId = -1;
    private int dragCandidateId = -1;
    private boolean draggingComponent;
    private Vec2 dragOffsetFromCenter = new Vec2(0.0, 0.0);
    private double pressScreenX;
    private double pressScreenY;
    private boolean simulationRunning = true;
    private String status = "Pick a component, click an empty grid cell to place it, or click nodes directly to link them.";

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    private void init() {
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(errorCallback);
        applyLinuxWaylandX11Hint();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW.");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_STENCIL_BITS, 8);

        window = glfwCreateWindow(INITIAL_WIDTH, INITIAL_HEIGHT, "Logika 1.0", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Unable to create GLFW window.");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);

        refreshViewport();
        installCallbacks();
        renderer.init();
        audio.init();
        glfwShowWindow(window);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            if (simulationRunning) {
                simulator.tick(circuit);
            }

            renderer.render(viewport, camera, circuit, toolbar, tool, pendingWire, selectedComponentId,
                    hoveredComponentId, draggingComponent, simulationRunning, status, input.mouseX(), input.mouseY());

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void installCallbacks() {
        glfwSetWindowSizeCallback(window, (handle, width, height) -> viewport.setWindowSize(width, height));

        glfwSetFramebufferSizeCallback(window, (handle, width, height) -> {
            viewport.setFramebufferSize(width, height);
            glViewport(0, 0, viewport.framebufferWidth(), viewport.framebufferHeight());
        });

        glfwSetCursorPosCallback(window, (handle, x, y) -> {
            double previousX = input.mouseX();
            double previousY = input.mouseY();
            if (input.panning()) {
                camera.panByScreenDelta(x - previousX, y - previousY);
            }
            input.setMouse(x, y);

            if (input.leftDown() && dragCandidateId != -1) {
                updateComponentDrag(x, y);
            }
            updateHoverState();
        });

        glfwSetScrollCallback(window, (handle, offsetX, offsetY) -> {
            if (toolbar.contains(input.mouseY(), viewport.windowHeight())) {
                return;
            }
            double factor = Math.pow(1.12, offsetY);
            camera.zoomAt(input.mouseX(), input.mouseY(), factor, viewport);
            updateHoverState();
        });

        glfwSetMouseButtonCallback(window, (handle, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                input.setLeftDown(action == GLFW_PRESS);
                if (action == GLFW_PRESS) {
                    if (input.spaceDown()) {
                        input.setPanning(true);
                    } else {
                        handlePrimaryPress();
                    }
                } else if (action == GLFW_RELEASE) {
                    handlePrimaryRelease();
                }
            }

            if (button == GLFW_MOUSE_BUTTON_RIGHT || button == GLFW_MOUSE_BUTTON_MIDDLE) {
                input.setPanDown(action == GLFW_PRESS);
                input.setPanning(action == GLFW_PRESS);
            }
        });

        glfwSetKeyCallback(window, (handle, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_SPACE) {
                input.setSpaceDown(action != GLFW_RELEASE);
                if (action == GLFW_RELEASE && input.panning() && !input.panDown()) {
                    input.setPanning(false);
                }
                return;
            }

            if (action == GLFW_PRESS) {
                handleKeyPress(key);
            }
        });
    }

    private void handlePrimaryPress() {
        Optional<Toolbar.Action> action = toolbar.actionAt(input.mouseX(), input.mouseY(), viewport.windowWidth(), viewport.windowHeight());
        if (action.isPresent()) {
            handleToolbarAction(action.get());
            return;
        }
        if (toolbar.contains(input.mouseY(), viewport.windowHeight())) {
            return;
        }

        Optional<CircuitComponent> trashTarget = findTrashTarget(input.mouseX(), input.mouseY());
        if (trashTarget.isPresent()) {
            deleteFromTrash(trashTarget.get());
            return;
        }

        Vec2 world = camera.screenToWorld(new Vec2(input.mouseX(), input.mouseY()), viewport);

        Optional<PinEndpoint> pin = circuit.findPin(world, pinHitRadiusWorld());
        if (pin.isPresent()) {
            handlePinClick(pin.get());
            return;
        }

        Optional<CircuitComponent> component = circuit.findComponent(world);
        if (component.isPresent()) {
            beginComponentPress(component.get(), world);
            return;
        }

        if (tool.isPlacement()) {
            placeComponent(world);
            return;
        }

        selectedComponentId = -1;
        if (pendingWire != null) {
            status = "Node still selected: click a compatible node or press Esc.";
        } else {
            status = "Empty grid selected.";
        }
    }

    private void handlePrimaryRelease() {
        if (dragCandidateId != -1) {
            Optional<CircuitComponent> component = circuit.componentById(dragCandidateId);
            if (draggingComponent) {
                status = "Component moved.";
                audio.playClick(true);
            } else {
                component.ifPresent(this::performComponentClick);
            }
        }

        if (pressedButtonId != -1) {
            circuit.componentById(pressedButtonId).ifPresent(component -> component.setSourceActive(false));
            pressedButtonId = -1;
            if (!draggingComponent) {
                status = "Button released.";
                audio.playClick(false);
            }
        }

        dragCandidateId = -1;
        draggingComponent = false;
        if (!input.panDown()) {
            input.setPanning(false);
        }
        updateHoverState();
    }

    private void beginComponentPress(CircuitComponent component, Vec2 world) {
        selectedComponentId = component.id();
        dragCandidateId = component.id();
        draggingComponent = false;
        pressScreenX = input.mouseX();
        pressScreenY = input.mouseY();
        dragOffsetFromCenter = world.subtract(component.center());

        if (component.kind() == ComponentKind.BUTTON) {
            component.setSourceActive(true);
            pressedButtonId = component.id();
            status = "Button active while held. Move the pointer to drag it instead.";
            audio.playClick(true);
        } else if (component.kind() == ComponentKind.SWITCH) {
            status = "Release to toggle, or drag to move.";
        } else {
            status = component.kind().label() + " selected. Drag to move.";
        }
    }

    private void updateComponentDrag(double screenX, double screenY) {
        double movement = Math.hypot(screenX - pressScreenX, screenY - pressScreenY);
        if (!draggingComponent && movement > DRAG_THRESHOLD_PX) {
            draggingComponent = true;
            if (pressedButtonId != -1) {
                circuit.componentById(pressedButtonId).ifPresent(component -> component.setSourceActive(false));
                pressedButtonId = -1;
            }
        }

        if (!draggingComponent) {
            return;
        }

        Vec2 mouseWorld = camera.screenToWorld(new Vec2(screenX, screenY), viewport);
        Vec2 targetCenter = snap(mouseWorld.subtract(dragOffsetFromCenter));
        circuit.componentById(dragCandidateId).ifPresent(component -> component.setCenter(targetCenter));
        status = "Dragging component on the grid.";
    }

    private void performComponentClick(CircuitComponent component) {
        selectedComponentId = component.id();

        if (component.kind() == ComponentKind.SWITCH) {
            component.setSourceActive(!component.sourceActive());
            status = "Switch set to " + (component.sourceActive() ? "true." : "false.");
            audio.playClick(component.sourceActive());
            return;
        }

        if (component.kind() == ComponentKind.BUTTON) {
            return;
        }

        status = component.kind().label() + " selected.";
        audio.playClick(true);
    }

    private void handleToolbarAction(Toolbar.Action action) {
        switch (action) {
            case BUTTON -> setTool(Tool.PLACE_BUTTON);
            case SWITCH -> setTool(Tool.PLACE_SWITCH);
            case NAND -> setTool(Tool.PLACE_NAND);
            case LED -> setTool(Tool.PLACE_LED);
            case SIMULATION -> {
                simulationRunning = !simulationRunning;
                status = simulationRunning ? "Simulation resumed." : "Simulation paused.";
                audio.playClick(simulationRunning);
            }
            case CLEAR -> {
                circuit.clear();
                pendingWire = null;
                selectedComponentId = -1;
                hoveredComponentId = -1;
                tool = Tool.INTERACT;
                status = "Grid cleared.";
                audio.playClick(false);
            }
        }
    }

    private void setTool(Tool nextTool) {
        tool = nextTool;
        pendingWire = null;
        status = switch (nextTool) {
            case INTERACT -> "Interact directly: click nodes, click sources, drag components.";
            case PLACE_BUTTON -> "Click an empty grid cell to place a momentary button.";
            case PLACE_SWITCH -> "Click an empty grid cell to place a toggle switch.";
            case PLACE_NAND -> "Click an empty grid cell to place a NAND gate.";
            case PLACE_LED -> "Click an empty grid cell to place a LED indicator.";
        };
        audio.playClick(true);
    }

    private void placeComponent(Vec2 world) {
        ComponentKind kind = tool.componentKind().orElseThrow();
        Vec2 snapped = snap(world);
        Rect candidate = componentBounds(kind, snapped);
        if (collidesWithExistingComponent(candidate)) {
            status = "Placement blocked: another component already occupies this area.";
            audio.playClick(false);
            return;
        }
        CircuitComponent component = circuit.addComponent(kind, snapped);
        selectedComponentId = component.id();
        hoveredComponentId = component.id();
        status = kind.label() + " placed. Click a node to start linking or drag the component to move it.";
        audio.playClick(true);
    }

    private void handlePinClick(PinEndpoint pin) {
        if (pendingWire == null) {
            pendingWire = pin.ref();
            selectedComponentId = pin.ref().componentId();
            status = pin.ref().isOutput()
                    ? "Output node selected. Click a compatible input node."
                    : "Input node selected. Click a compatible output node.";
            audio.playClick(true);
            return;
        }

        if (pendingWire.equals(pin.ref())) {
            pendingWire = null;
            status = "Node selection cancelled.";
            audio.playClick(false);
            return;
        }

        Circuit.ConnectResult result;
        if (pendingWire.isOutput() && pin.ref().isInput()) {
            result = circuit.connect(pendingWire, pin.ref());
        } else if (pendingWire.isInput() && pin.ref().isOutput()) {
            result = circuit.connect(pin.ref(), pendingWire);
        } else {
            pendingWire = pin.ref();
            selectedComponentId = pin.ref().componentId();
            status = pin.ref().isOutput()
                    ? "Output node selected. Click a compatible input node."
                    : "Input node selected. Click a compatible output node.";
            audio.playClick(true);
            return;
        }

        status = result.message();
        pendingWire = result.success() ? null : pendingWire;
        audio.playClick(result.success());
    }

    private void handleKeyPress(int key) {
        switch (key) {
            case GLFW_KEY_ESCAPE -> {
                pendingWire = null;
                tool = Tool.INTERACT;
                status = "Interaction mode.";
            }
            case GLFW_KEY_DELETE -> {
                if (selectedComponentId != -1) {
                    circuit.removeComponent(selectedComponentId);
                    selectedComponentId = -1;
                    pendingWire = null;
                    status = "Component deleted.";
                    audio.playClick(false);
                }
            }
            case GLFW_KEY_S -> {
                simulationRunning = !simulationRunning;
                status = simulationRunning ? "Simulation resumed." : "Simulation paused.";
                audio.playClick(simulationRunning);
            }
            case GLFW_KEY_C -> {
                camera.reset();
                updateHoverState();
                status = "Camera re-centered.";
            }
            case GLFW_KEY_1 -> setTool(Tool.PLACE_BUTTON);
            case GLFW_KEY_2 -> setTool(Tool.PLACE_SWITCH);
            case GLFW_KEY_3 -> setTool(Tool.PLACE_NAND);
            case GLFW_KEY_4 -> setTool(Tool.PLACE_LED);
            default -> {
            }
        }
    }

    private void updateHoverState() {
        if (toolbar.contains(input.mouseY(), viewport.windowHeight())) {
            hoveredComponentId = -1;
            return;
        }
        Vec2 world = camera.screenToWorld(new Vec2(input.mouseX(), input.mouseY()), viewport);
        hoveredComponentId = circuit.findComponent(world).map(CircuitComponent::id).orElse(-1);
    }

    private void deleteFromTrash(CircuitComponent target) {
        int id = target.id();
        if (pressedButtonId == id) {
            target.setSourceActive(false);
            pressedButtonId = -1;
        }
        dragCandidateId = -1;
        draggingComponent = false;
        circuit.removeComponent(id);
        selectedComponentId = -1;
        hoveredComponentId = -1;
        pendingWire = null;
        status = "Component deleted from hover trash icon.";
        audio.playClick(false);
    }

    private Optional<CircuitComponent> findTrashTarget(double mouseX, double mouseY) {
        List<CircuitComponent> components = circuit.components();
        for (int i = components.size() - 1; i >= 0; i--) {
            CircuitComponent component = components.get(i);
            Rect icon = trashIconRect(component);
            if (icon.contains(mouseX, mouseY)) {
                return Optional.of(component);
            }
        }
        return Optional.empty();
    }

    private Rect trashIconRect(CircuitComponent component) {
        Rect bounds = component.bounds();
        Vec2 topLeft = camera.worldToScreen(new Vec2(bounds.x(), bounds.y()), viewport);
        return new Rect(topLeft.x() + UiMetrics.TRASH_BUTTON_MARGIN_SCREEN,
                topLeft.y() + UiMetrics.TRASH_BUTTON_MARGIN_SCREEN,
                UiMetrics.TRASH_BUTTON_SIZE_SCREEN,
                UiMetrics.TRASH_BUTTON_SIZE_SCREEN);
    }

    private Vec2 snap(Vec2 world) {
        double grid = renderer.gridSize();
        return new Vec2(Math.round(world.x() / grid) * grid, Math.round(world.y() / grid) * grid);
    }

    private double pinHitRadiusWorld() {
        return UiMetrics.PIN_HIT_RADIUS_SCREEN / Math.max(0.18, camera.zoom());
    }

    private Rect componentBounds(ComponentKind kind, Vec2 center) {
        return new Rect(center.x() - kind.width() / 2.0, center.y() - kind.height() / 2.0,
                kind.width(), kind.height());
    }

    private boolean collidesWithExistingComponent(Rect candidate) {
        for (CircuitComponent component : circuit.components()) {
            Rect padded = expand(component.bounds(), UiMetrics.COMPONENT_COLLISION_GAP_WORLD);
            if (overlaps(candidate, padded)) {
                return true;
            }
        }
        return false;
    }

    private static Rect expand(Rect rect, double amount) {
        return new Rect(rect.x() - amount, rect.y() - amount, rect.width() + amount * 2.0, rect.height() + amount * 2.0);
    }

    private static boolean overlaps(Rect a, Rect b) {
        return a.x() < b.x() + b.width()
                && a.x() + a.width() > b.x()
                && a.y() < b.y() + b.height()
                && a.y() + a.height() > b.y();
    }

    private void refreshViewport() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);

            glfwGetWindowSize(window, w, h);
            viewport.setWindowSize(w.get(0), h.get(0));

            glfwGetFramebufferSize(window, w, h);
            viewport.setFramebufferSize(w.get(0), h.get(0));
            glViewport(0, 0, viewport.framebufferWidth(), viewport.framebufferHeight());
        }
    }

    private static void applyLinuxWaylandX11Hint() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        boolean linux = os.contains("linux");
        boolean wayland = "wayland".equalsIgnoreCase(sessionType) || System.getenv("WAYLAND_DISPLAY") != null;

        if (linux && wayland) {
            glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11);
            System.out.println("Linux Wayland session detected: forcing GLFW X11 platform.");
        }
    }

    private void cleanup() {
        audio.close();
        renderer.close();

        if (window != NULL) {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
            window = NULL;
        }

        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}
