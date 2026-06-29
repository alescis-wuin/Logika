package dev.alexis.logika.engine;

import dev.alexis.logika.audio.AudioService;
import dev.alexis.logika.graphics.Camera2D;
import dev.alexis.logika.graphics.NanoVGRenderer;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.CircuitComponent;
import dev.alexis.logika.model.ComponentKind;
import dev.alexis.logika.model.PinDirection;
import dev.alexis.logika.model.PinEndpoint;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.model.Wire;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_V;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_SUPER;
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
    private final Set<Integer> selectedComponentIds = new LinkedHashSet<>();
    private final Map<Integer, Vec2> groupDragStartCenters = new LinkedHashMap<>();

    private GLFWErrorCallback errorCallback;
    private long window = NULL;
    private Tool tool = Tool.INTERACT;
    private PinRef pendingWire;
    private int selectedComponentId = -1;
    private int hoveredComponentId = -1;
    private int pressedButtonId = -1;
    private int dragCandidateId = -1;
    private boolean draggingComponent;
    private boolean componentPressSelectionOnly;
    private Vec2 dragOffsetFromCenter = new Vec2(0.0, 0.0);
    private double pressScreenX;
    private double pressScreenY;
    private boolean marqueeCandidate;
    private boolean marqueeSelecting;
    private boolean marqueeAdditive;
    private Vec2 marqueeStartWorld = new Vec2(0.0, 0.0);
    private Vec2 marqueeEndWorld = new Vec2(0.0, 0.0);
    private boolean simulationRunning = true;
    private String status = "Drag empty grid to select, Ctrl+C/V copies blocks, and click a node then a component to chain.";
    private ChainVariant chainVariant = ChainVariant.AUTO;
    private List<ClipboardComponent> clipboardComponents = List.of();
    private List<ClipboardWire> clipboardWires = List.of();
    private int pasteSequence;

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

        window = glfwCreateWindow(INITIAL_WIDTH, INITIAL_HEIGHT, "Logika 1.1", NULL, NULL);
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

            renderer.render(viewport, camera, circuit, toolbar, tool, pendingWire, selectedComponentIds,
                    hoveredComponentId, draggingComponent, activeSelectionRect(), simulationRunning, status,
                    input.mouseX(), input.mouseY(), clipboardComponents.size(), chainVariant.label());

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
            } else if (input.leftDown() && marqueeCandidate) {
                updateMarqueeSelection(x, y);
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
                        handlePrimaryPress(mods);
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
                handleKeyPress(key, mods);
            }
        });
    }

    private void handlePrimaryPress(int mods) {
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
            if (pendingWire != null && tryChainToComponent(component.get())) {
                return;
            }
            beginComponentPress(component.get(), world, hasSelectionModifier(mods));
            return;
        }

        if (tool.isPlacement()) {
            placeComponent(world);
            return;
        }

        beginMarqueeSelection(world, hasSelectionModifier(mods));
    }

    private void handlePrimaryRelease() {
        if (dragCandidateId != -1) {
            Optional<CircuitComponent> component = circuit.componentById(dragCandidateId);
            if (draggingComponent) {
                status = selectedComponentIds.size() > 1
                        ? selectedComponentIds.size() + " components moved."
                        : "Component moved.";
                audio.playClick(true);
            } else if (!componentPressSelectionOnly) {
                component.ifPresent(this::performComponentClick);
            } else {
                status = selectionSummary();
            }
        } else if (marqueeCandidate) {
            finishMarqueeSelection();
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
        componentPressSelectionOnly = false;
        groupDragStartCenters.clear();
        marqueeCandidate = false;
        marqueeSelecting = false;
        if (!input.panDown()) {
            input.setPanning(false);
        }
        updateHoverState();
    }

    private void beginComponentPress(CircuitComponent component, Vec2 world, boolean selectionModifier) {
        if (selectionModifier) {
            boolean selected = toggleComponentSelection(component.id());
            componentPressSelectionOnly = true;
            if (!selected) {
                dragCandidateId = -1;
                status = selectionSummary();
                audio.playClick(false);
                return;
            }
        } else {
            componentPressSelectionOnly = false;
            if (!selectedComponentIds.contains(component.id())) {
                selectOnly(component.id());
            } else {
                selectedComponentId = component.id();
            }
        }

        dragCandidateId = component.id();
        draggingComponent = false;
        pressScreenX = input.mouseX();
        pressScreenY = input.mouseY();
        dragOffsetFromCenter = world.subtract(component.center());
        rememberGroupDragStartCenters();

        if (componentPressSelectionOnly) {
            status = selectionSummary();
            return;
        }

        if (component.kind() == ComponentKind.BUTTON) {
            component.setSourceActive(true);
            pressedButtonId = component.id();
            status = "Button active while held. Move the pointer to drag it instead.";
            audio.playClick(true);
        } else if (component.kind() == ComponentKind.SWITCH) {
            status = "Release to toggle, or drag the selected block to move it.";
        } else {
            status = component.kind().label() + " selected. Drag to move the selected block.";
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
        Vec2 originalCenter = groupDragStartCenters.getOrDefault(dragCandidateId, targetCenter);
        Vec2 delta = targetCenter.subtract(originalCenter);

        for (Map.Entry<Integer, Vec2> entry : groupDragStartCenters.entrySet()) {
            circuit.componentById(entry.getKey()).ifPresent(component -> component.setCenter(snap(entry.getValue().add(delta))));
        }
        status = selectedComponentIds.size() > 1 ? "Dragging selected block on the grid." : "Dragging component on the grid.";
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
                clearSelection();
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
            case INTERACT -> "Interact directly: select, copy, drag, click nodes, or use sources.";
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
        selectOnly(component.id());
        hoveredComponentId = component.id();
        status = kind.label() + " placed. Click its output, then another component, to chain quickly.";
        audio.playClick(true);
    }

    private void handlePinClick(PinEndpoint pin) {
        if (pendingWire == null) {
            pendingWire = pin.ref();
            selectOnly(pin.ref().componentId());
            status = pin.ref().isOutput()
                    ? "Output node selected. Click a compatible input or component body."
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
        PinRef inputRef;
        if (pendingWire.isOutput() && pin.ref().isInput()) {
            result = circuit.connect(pendingWire, pin.ref());
            inputRef = pin.ref();
        } else if (pendingWire.isInput() && pin.ref().isOutput()) {
            result = circuit.connect(pin.ref(), pendingWire);
            inputRef = pendingWire;
        } else {
            pendingWire = pin.ref();
            selectOnly(pin.ref().componentId());
            status = pin.ref().isOutput()
                    ? "Output node selected. Click a compatible input or component body."
                    : "Input node selected. Click a compatible output node.";
            audio.playClick(true);
            return;
        }

        finishConnection(result, inputRef);
    }

    private boolean tryChainToComponent(CircuitComponent component) {
        if (pendingWire == null || pendingWire.componentId() == component.id()) {
            return false;
        }

        if (pendingWire.isOutput()) {
            if (component.kind().inputCount() == 0) {
                status = component.kind().label() + " has no input node for chaining.";
                audio.playClick(false);
                return true;
            }
            List<Integer> inputIndexes = chainVariant.inputIndexesFor(component, this::inputConnected);
            int success = 0;
            String lastMessage = "No compatible input.";
            for (int inputIndex : inputIndexes) {
                PinRef inputRef = new PinRef(component.id(), PinDirection.INPUT, inputIndex);
                Circuit.ConnectResult result = circuit.connect(pendingWire, inputRef);
                lastMessage = result.message();
                if (result.success()) {
                    success++;
                }
            }
            if (success > 0) {
                selectOnly(component.id());
                pendingWire = firstOutputRef(component).orElse(null);
                status = "Chain linked to " + component.kind().label() + " " + chainVariant.inputLabel(inputIndexes)
                        + (pendingWire == null ? "." : "; output kept for next link.");
                audio.playClick(true);
            } else {
                status = lastMessage;
                audio.playClick(false);
            }
            return true;
        }

        if (pendingWire.isInput()) {
            if (component.kind().outputCount() == 0) {
                status = component.kind().label() + " has no output node for chaining.";
                audio.playClick(false);
                return true;
            }
            PinRef outputRef = new PinRef(component.id(), PinDirection.OUTPUT, 0);
            Circuit.ConnectResult result = circuit.connect(outputRef, pendingWire);
            finishConnection(result, pendingWire);
            return true;
        }

        return false;
    }

    private void finishConnection(Circuit.ConnectResult result, PinRef inputRef) {
        if (result.success()) {
            selectOnly(inputRef.componentId());
            pendingWire = circuit.componentById(inputRef.componentId()).flatMap(this::firstOutputRef).orElse(null);
            status = result.message() + (pendingWire == null ? "" : " Output kept for chaining.");
        } else {
            status = result.message();
        }
        audio.playClick(result.success());
    }

    private void handleKeyPress(int key, int mods) {
        boolean shortcut = hasShortcutModifier(mods);
        switch (key) {
            case GLFW_KEY_ESCAPE -> {
                pendingWire = null;
                tool = Tool.INTERACT;
                marqueeCandidate = false;
                marqueeSelecting = false;
                status = "Interaction mode.";
            }
            case GLFW_KEY_DELETE -> deleteSelectedComponents();
            case GLFW_KEY_S -> {
                simulationRunning = !simulationRunning;
                status = simulationRunning ? "Simulation resumed." : "Simulation paused.";
                audio.playClick(simulationRunning);
            }
            case GLFW_KEY_C -> {
                if (shortcut) {
                    copySelection();
                } else {
                    camera.reset();
                    updateHoverState();
                    status = "Camera re-centered.";
                }
            }
            case GLFW_KEY_V -> {
                if (shortcut) {
                    pasteClipboard();
                } else {
                    chainVariant = chainVariant.next();
                    status = "Chain variant: " + chainVariant.description() + ".";
                    audio.playClick(true);
                }
            }
            case GLFW_KEY_D -> {
                if (shortcut) {
                    duplicateSelection();
                }
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
        if (toolbar.contains(input.mouseY(), viewport.windowHeight()) || marqueeSelecting) {
            hoveredComponentId = -1;
            return;
        }
        Vec2 world = camera.screenToWorld(new Vec2(input.mouseX(), input.mouseY()), viewport);
        hoveredComponentId = circuit.findComponent(world).map(CircuitComponent::id).orElse(-1);
    }

    private void deleteFromTrash(CircuitComponent target) {
        if (selectedComponentIds.contains(target.id()) && selectedComponentIds.size() > 1) {
            deleteSelectedComponents();
            return;
        }

        int id = target.id();
        if (pressedButtonId == id) {
            target.setSourceActive(false);
            pressedButtonId = -1;
        }
        dragCandidateId = -1;
        draggingComponent = false;
        circuit.removeComponent(id);
        selectedComponentIds.remove(id);
        syncPrimarySelection();
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

    private void beginMarqueeSelection(Vec2 world, boolean additive) {
        marqueeCandidate = true;
        marqueeSelecting = false;
        marqueeAdditive = additive;
        marqueeStartWorld = world;
        marqueeEndWorld = world;
        pressScreenX = input.mouseX();
        pressScreenY = input.mouseY();
        status = additive ? "Additive area selection ready." : "Drag to select an area, or release to clear selection.";
    }

    private void updateMarqueeSelection(double screenX, double screenY) {
        double movement = Math.hypot(screenX - pressScreenX, screenY - pressScreenY);
        if (!marqueeSelecting && movement > DRAG_THRESHOLD_PX) {
            marqueeSelecting = true;
            pendingWire = null;
            tool = Tool.INTERACT;
        }
        if (marqueeSelecting) {
            marqueeEndWorld = camera.screenToWorld(new Vec2(screenX, screenY), viewport);
            status = "Selecting components inside the area.";
        }
    }

    private void finishMarqueeSelection() {
        if (!marqueeSelecting) {
            if (!marqueeAdditive) {
                clearSelection();
                status = pendingWire == null ? "Empty grid selected." : "Node still selected: click a compatible node or press Esc.";
            }
            return;
        }

        Rect selection = normalizedRect(marqueeStartWorld, marqueeEndWorld);
        List<Integer> hits = new ArrayList<>();
        for (CircuitComponent component : circuit.components()) {
            if (overlaps(selection, component.bounds())) {
                hits.add(component.id());
            }
        }

        if (!marqueeAdditive) {
            selectedComponentIds.clear();
        }
        selectedComponentIds.addAll(hits);
        syncPrimarySelection();
        status = hits.isEmpty() ? "No component in selection area." : selectionSummary();
        audio.playClick(!hits.isEmpty());
    }

    private Rect activeSelectionRect() {
        return marqueeSelecting ? normalizedRect(marqueeStartWorld, marqueeEndWorld) : null;
    }

    private void copySelection() {
        if (selectedComponentIds.isEmpty()) {
            status = "Nothing selected to copy.";
            audio.playClick(false);
            return;
        }

        List<ClipboardComponent> components = new ArrayList<>();
        for (CircuitComponent component : circuit.components()) {
            if (selectedComponentIds.contains(component.id())) {
                components.add(new ClipboardComponent(component.id(), component.kind(), component.center(), component.sourceActive()));
            }
        }

        List<ClipboardWire> wires = new ArrayList<>();
        for (Wire wire : circuit.wires()) {
            if (selectedComponentIds.contains(wire.from().componentId()) && selectedComponentIds.contains(wire.to().componentId())) {
                wires.add(new ClipboardWire(wire.from().componentId(), wire.from().index(), wire.to().componentId(), wire.to().index()));
            }
        }

        clipboardComponents = List.copyOf(components);
        clipboardWires = List.copyOf(wires);
        pasteSequence = 0;
        status = "Copied " + clipboardComponents.size() + " component(s) and " + clipboardWires.size() + " internal wire(s).";
        audio.playClick(true);
    }

    private void pasteClipboard() {
        if (clipboardComponents.isEmpty()) {
            status = "Clipboard is empty.";
            audio.playClick(false);
            return;
        }

        Vec2 copyAnchor = clipboardAnchor();
        Vec2 offset = choosePasteOffset(copyAnchor);
        Map<Integer, Integer> idMap = new HashMap<>();
        Set<Integer> pastedIds = new LinkedHashSet<>();

        for (ClipboardComponent item : clipboardComponents) {
            CircuitComponent pasted = circuit.addComponent(item.kind(), snap(item.center().add(offset)));
            pasted.setSourceActive(item.sourceActive());
            idMap.put(item.sourceId(), pasted.id());
            pastedIds.add(pasted.id());
        }

        for (ClipboardWire wire : clipboardWires) {
            Integer fromId = idMap.get(wire.fromComponentId());
            Integer toId = idMap.get(wire.toComponentId());
            if (fromId != null && toId != null) {
                circuit.connect(new PinRef(fromId, PinDirection.OUTPUT, wire.fromIndex()),
                        new PinRef(toId, PinDirection.INPUT, wire.toIndex()));
            }
        }

        selectedComponentIds.clear();
        selectedComponentIds.addAll(pastedIds);
        syncPrimarySelection();
        pendingWire = null;
        tool = Tool.INTERACT;
        pasteSequence++;
        status = "Pasted " + pastedIds.size() + " component(s) with preserved internal wiring.";
        audio.playClick(true);
    }

    private void duplicateSelection() {
        if (selectedComponentIds.isEmpty()) {
            status = "Nothing selected to duplicate.";
            audio.playClick(false);
            return;
        }
        copySelection();
        pasteClipboard();
    }

    private Vec2 clipboardAnchor() {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        for (ClipboardComponent component : clipboardComponents) {
            minX = Math.min(minX, component.center().x());
            minY = Math.min(minY, component.center().y());
        }
        return new Vec2(minX, minY);
    }

    private Vec2 choosePasteOffset(Vec2 copyAnchor) {
        Vec2 mouseWorld = snap(camera.screenToWorld(new Vec2(input.mouseX(), input.mouseY()), viewport));
        Vec2 offset = mouseWorld.subtract(copyAnchor)
                .add(new Vec2(pasteSequence * UiMetrics.GRID_SIZE, pasteSequence * UiMetrics.GRID_SIZE));
        for (int attempt = 0; attempt < 16; attempt++) {
            if (!pasteWouldCollide(offset)) {
                return offset;
            }
            offset = offset.add(new Vec2(UiMetrics.GRID_SIZE, UiMetrics.GRID_SIZE));
        }
        return offset;
    }

    private boolean pasteWouldCollide(Vec2 offset) {
        for (ClipboardComponent item : clipboardComponents) {
            Rect candidate = componentBounds(item.kind(), snap(item.center().add(offset)));
            if (collidesWithExistingComponent(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void deleteSelectedComponents() {
        if (selectedComponentIds.isEmpty()) {
            status = "Nothing selected to delete.";
            audio.playClick(false);
            return;
        }
        List<Integer> ids = new ArrayList<>(selectedComponentIds);
        if (ids.contains(pressedButtonId)) {
            pressedButtonId = -1;
        }
        circuit.removeComponents(ids);
        clearSelection();
        pendingWire = null;
        dragCandidateId = -1;
        draggingComponent = false;
        hoveredComponentId = -1;
        status = ids.size() == 1 ? "Component deleted." : ids.size() + " components deleted.";
        audio.playClick(false);
    }

    private void rememberGroupDragStartCenters() {
        groupDragStartCenters.clear();
        for (CircuitComponent component : circuit.components()) {
            if (selectedComponentIds.contains(component.id())) {
                groupDragStartCenters.put(component.id(), component.center());
            }
        }
    }

    private void selectOnly(int componentId) {
        selectedComponentIds.clear();
        selectedComponentIds.add(componentId);
        selectedComponentId = componentId;
    }

    private boolean toggleComponentSelection(int componentId) {
        boolean selected;
        if (selectedComponentIds.contains(componentId)) {
            selectedComponentIds.remove(componentId);
            selected = false;
        } else {
            selectedComponentIds.add(componentId);
            selected = true;
        }
        syncPrimarySelection();
        return selected;
    }

    private void clearSelection() {
        selectedComponentIds.clear();
        selectedComponentId = -1;
    }

    private void syncPrimarySelection() {
        selectedComponentId = -1;
        for (int id : selectedComponentIds) {
            selectedComponentId = id;
        }
    }

    private String selectionSummary() {
        return switch (selectedComponentIds.size()) {
            case 0 -> "No component selected.";
            case 1 -> "1 component selected.";
            default -> selectedComponentIds.size() + " components selected.";
        };
    }

    private Optional<PinRef> firstOutputRef(CircuitComponent component) {
        if (component.kind().outputCount() == 0) {
            return Optional.empty();
        }
        return Optional.of(new PinRef(component.id(), PinDirection.OUTPUT, 0));
    }

    private boolean inputConnected(int componentId, int inputIndex) {
        PinRef inputRef = new PinRef(componentId, PinDirection.INPUT, inputIndex);
        for (Wire wire : circuit.wires()) {
            if (wire.to().equals(inputRef)) {
                return true;
            }
        }
        return false;
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

    private static boolean hasShortcutModifier(int mods) {
        return (mods & (GLFW_MOD_CONTROL | GLFW_MOD_SUPER)) != 0;
    }

    private static boolean hasSelectionModifier(int mods) {
        return (mods & (GLFW_MOD_SHIFT | GLFW_MOD_CONTROL | GLFW_MOD_SUPER)) != 0;
    }

    private static Rect normalizedRect(Vec2 a, Vec2 b) {
        double x = Math.min(a.x(), b.x());
        double y = Math.min(a.y(), b.y());
        return new Rect(x, y, Math.abs(a.x() - b.x()), Math.abs(a.y() - b.y()));
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

    private record ClipboardComponent(int sourceId, ComponentKind kind, Vec2 center, boolean sourceActive) {
    }

    private record ClipboardWire(int fromComponentId, int fromIndex, int toComponentId, int toIndex) {
    }

    private enum ChainVariant {
        AUTO("Auto", "first free input"),
        A("A", "force input A"),
        B("B", "force input B"),
        BOTH("A+B", "same source on A and B for a NAND inverter");

        private final String label;
        private final String description;

        ChainVariant(String label, String description) {
            this.label = label;
            this.description = description;
        }

        String label() {
            return label;
        }

        String description() {
            return description;
        }

        ChainVariant next() {
            ChainVariant[] variants = values();
            return variants[(ordinal() + 1) % variants.length];
        }

        List<Integer> inputIndexesFor(CircuitComponent component, InputConnectionLookup lookup) {
            int count = component.kind().inputCount();
            if (count <= 0) {
                return List.of();
            }
            return switch (this) {
                case AUTO -> List.of(firstAvailableInput(component, lookup));
                case A -> List.of(0);
                case B -> List.of(Math.min(1, count - 1));
                case BOTH -> count >= 2 ? List.of(0, 1) : List.of(0);
            };
        }

        String inputLabel(List<Integer> inputIndexes) {
            if (inputIndexes.size() > 1) {
                return "inputs A+B";
            }
            int index = inputIndexes.isEmpty() ? 0 : inputIndexes.get(0);
            return "input " + (index == 0 ? "A" : "B");
        }

        private static int firstAvailableInput(CircuitComponent component, InputConnectionLookup lookup) {
            for (int i = 0; i < component.kind().inputCount(); i++) {
                if (!lookup.connected(component.id(), i)) {
                    return i;
                }
            }
            return 0;
        }
    }

    @FunctionalInterface
    private interface InputConnectionLookup {
        boolean connected(int componentId, int inputIndex);
    }
}
