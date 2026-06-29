package dev.alexis.logika.engine;

import dev.alexis.logika.audio.AudioService;
import dev.alexis.logika.graphics.*;
import dev.alexis.logika.model.*;
import dev.alexis.logika.simulation.LogicSimulator;
import dev.alexis.logika.ui.*;
import dev.alexis.logika.util.*;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class LogikaEngine {
    private static final int INITIAL_WIDTH = 1280;
    private static final int INITIAL_HEIGHT = 820;
    private static final int HISTORY_LIMIT = 120;
    private static final double DRAG_THRESHOLD_PX = 7.0;
    private static final double PIN_ALIGNMENT_MAX_DELTA_WORLD = 128.0;
    private static final double EDGE_ALIGNMENT_MAX_DELTA_WORLD = 128.0;
    private static final double PLACEMENT_SLOT_THICKNESS_WORLD = 56.0;
    private static final double PLACEMENT_SLOT_MARGIN_WORLD = 6.0;
    private static final double PLACEMENT_CANDIDATE_MARGIN_WORLD = 18.0;

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
    private final Deque<UndoableEdit> undoStack = new ArrayDeque<>();
    private final Deque<UndoableEdit> redoStack = new ArrayDeque<>();

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
    private EditorSnapshot dragStartSnapshot;
    private boolean marqueeCandidate;
    private boolean marqueeSelecting;
    private boolean marqueeAdditive;
    private Vec2 marqueeStartWorld = new Vec2(0.0, 0.0);
    private Vec2 marqueeEndWorld = new Vec2(0.0, 0.0);
    private boolean simulationRunning = true;
    private String status = "Interact directly, select blocks, or choose a placement tool.";
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
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW.");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_STENCIL_BITS, 8);
        window = glfwCreateWindow(INITIAL_WIDTH, INITIAL_HEIGHT, "Logika 1.3", NULL, NULL);
        if (window == NULL) throw new IllegalStateException("Unable to create GLFW window.");
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
            if (simulationRunning) simulator.tick(circuit);
            renderer.render(viewport, camera, circuit, toolbar, tool, placementPreviews(), pendingWire, selectedComponentIds,
                    hoveredComponentId, draggingComponent, activeSelectionRect(), simulationRunning, status,
                    input.mouseX(), input.mouseY(), clipboardComponents.size(), "Off",
                    undoStack.size(), redoStack.size());
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
            if (input.panning()) camera.panByScreenDelta(x - previousX, y - previousY);
            input.setMouse(x, y);
            if (input.leftDown() && dragCandidateId != -1) updateComponentDrag(x, y);
            else if (input.leftDown() && marqueeCandidate) updateMarqueeSelection(x, y);
            updateHoverState();
        });
        glfwSetScrollCallback(window, (handle, offsetX, offsetY) -> {
            if (toolbar.contains(input.mouseY(), viewport.windowHeight())) return;
            camera.zoomAt(input.mouseX(), input.mouseY(), Math.pow(1.12, offsetY), viewport);
            updateHoverState();
        });
        glfwSetMouseButtonCallback(window, (handle, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                input.setLeftDown(action == GLFW_PRESS);
                if (action == GLFW_PRESS) {
                    if (input.spaceDown()) input.setPanning(true);
                    else handlePrimaryPress(mods);
                } else if (action == GLFW_RELEASE) handlePrimaryRelease();
            }
            if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                if (action == GLFW_PRESS) cancelEditorState("Right click");
                input.setPanDown(action == GLFW_PRESS);
                input.setPanning(action == GLFW_PRESS);
            }
            if (button == GLFW_MOUSE_BUTTON_MIDDLE) {
                input.setPanDown(action == GLFW_PRESS);
                input.setPanning(action == GLFW_PRESS);
            }
        });
        glfwSetKeyCallback(window, (handle, key, scancode, action, mods) -> {
            updateModifierState(key, action);
            if (key == GLFW_KEY_SPACE) {
                input.setSpaceDown(action != GLFW_RELEASE);
                if (action == GLFW_RELEASE && input.panning() && !input.panDown()) input.setPanning(false);
                return;
            }
            if (action == GLFW_PRESS) handleKeyPress(key, mods);
        });
    }

    private void updateModifierState(int key, int action) {
        boolean down = action != GLFW_RELEASE;
        switch (key) {
            case GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL -> input.setControlDown(down);
            case GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT -> input.setAltDown(down);
            case GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> input.setShiftDown(down);
            default -> { }
        }
    }

    private void handlePrimaryPress(int mods) {
        Optional<Toolbar.Action> action = toolbar.actionAt(input.mouseX(), input.mouseY(), viewport.windowWidth(), viewport.windowHeight());
        if (action.isPresent()) {
            handleToolbarAction(action.get());
            return;
        }
        if (toolbar.contains(input.mouseY(), viewport.windowHeight())) return;
        Vec2 world = camera.screenToWorld(new Vec2(input.mouseX(), input.mouseY()), viewport);
        boolean placementMode = tool.isPlacement() && pendingWire == null;
        if (placementMode && tryPlaceFromSlot(world)) return;
        Optional<CircuitComponent> trashTarget = findTrashTarget(input.mouseX(), input.mouseY());
        if (trashTarget.isPresent()) {
            deleteFromTrash(trashTarget.get());
            return;
        }
        Optional<PinEndpoint> pin = circuit.findPin(world, pinHitRadiusWorld());
        if (pin.isPresent()) {
            handlePinClick(pin.get());
            return;
        }
        Optional<CircuitComponent> component = circuit.findComponent(world);
        if (component.isPresent()) {
            beginComponentPress(component.get(), world, hasSelectionModifier(mods));
            return;
        }
        if (placementMode) {
            placeFromEmptyCanvas(world);
            return;
        }
        beginMarqueeSelection(world, hasSelectionModifier(mods));
    }

    private void handlePrimaryRelease() {
        if (dragCandidateId != -1) {
            Optional<CircuitComponent> component = circuit.componentById(dragCandidateId);
            if (draggingComponent) {
                if (dragStartSnapshot != null) recordEdit("Move selection", dragStartSnapshot);
                status = selectedComponentIds.size() > 1 ? selectedComponentIds.size() + " components moved." : "Component moved.";
                audio.playClick(true);
            } else if (!componentPressSelectionOnly) component.ifPresent(this::performComponentClick);
            else status = selectionSummary();
        } else if (marqueeCandidate) finishMarqueeSelection();
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
        dragStartSnapshot = null;
        groupDragStartCenters.clear();
        marqueeCandidate = false;
        marqueeSelecting = false;
        if (!input.panDown()) input.setPanning(false);
        updateHoverState();
    }

    private void beginComponentPress(CircuitComponent component, Vec2 world, boolean selectionModifier) {
        pendingWire = null;
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
            if (!selectedComponentIds.contains(component.id())) selectOnly(component.id());
            else selectedComponentId = component.id();
        }
        dragCandidateId = component.id();
        draggingComponent = false;
        pressScreenX = input.mouseX();
        pressScreenY = input.mouseY();
        dragOffsetFromCenter = world.subtract(component.center());
        rememberGroupDragStartCenters();
        dragStartSnapshot = snapshotEditor();
        if (componentPressSelectionOnly) {
            status = selectionSummary();
            return;
        }
        if (component.kind() == ComponentKind.BUTTON) {
            component.setSourceActive(true);
            pressedButtonId = component.id();
            status = "Button active while held. Move the pointer to drag it instead.";
            audio.playClick(true);
        } else if (component.kind() == ComponentKind.SWITCH) status = "Release to toggle, or drag the selected block to move it.";
        else status = component.kind().label() + " selected. Drag to move the selected block.";
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
        if (!draggingComponent) return;
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
            EditorSnapshot before = snapshotEditor();
            component.setSourceActive(!component.sourceActive());
            recordEdit("Toggle switch", before);
            status = "Switch set to " + (component.sourceActive() ? "true." : "false.");
            audio.playClick(component.sourceActive());
            return;
        }
        if (component.kind() == ComponentKind.BUTTON) return;
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
                if (circuit.components().isEmpty()) {
                    status = "Grid is already empty.";
                    audio.playClick(false);
                    return;
                }
                EditorSnapshot before = snapshotEditor();
                circuit.clear();
                pendingWire = null;
                clearSelection();
                hoveredComponentId = -1;
                tool = Tool.INTERACT;
                recordEdit("Clear grid", before);
                status = "Grid cleared.";
                audio.playClick(false);
            }
        }
    }

    private void setTool(Tool nextTool) {
        tool = nextTool;
        pendingWire = null;
        if (nextTool.isPlacement()) clearSelection();
        status = switch (nextTool) {
            case INTERACT -> "Interact directly: select, copy, drag, click nodes, or use sources.";
            case PLACE_BUTTON, PLACE_SWITCH, PLACE_NAND, PLACE_LED ->
                    "Hover a component for dark + slots; hover a slot to preview; click the slot to place.";
        };
        audio.playClick(true);
    }


    private boolean tryPlaceFromSlot(Vec2 world) {
        ComponentKind kind = tool.componentKind().orElseThrow();
        Optional<PlacementCandidate> candidate = placementSlotAt(kind, world);
        if (candidate.isEmpty()) return false;
        pendingWire = null;
        placeComponent(kind, candidate.get());
        return true;
    }

    private void placeFromEmptyCanvas(Vec2 world) {
        pendingWire = null;
        ComponentKind kind = tool.componentKind().orElseThrow();
        Vec2 center = placementCenter(kind, world);
        if (collidesWithExistingComponent(componentBounds(kind, center))) {
            clearSelection();
            status = "Placement blocked: use a visible dark + slot or an empty grid area.";
            audio.playClick(false);
            return;
        }
        placeComponent(kind, new PlacementCandidate(center, placementAlignment().label(), null, null));
    }

    private void placeComponent(ComponentKind kind, PlacementCandidate candidate) {
        EditorSnapshot before = snapshotEditor();
        CircuitComponent component = circuit.addComponent(kind, candidate.center());
        selectOnly(component.id());
        hoveredComponentId = component.id();
        recordEdit("Place " + kind.label(), before);
        status = kind.label() + " placed with " + candidate.label() + " placement.";
        audio.playClick(true);
    }

    private void handlePinClick(PinEndpoint pin) {
        if (pendingWire == null) {
            pendingWire = pin.ref();
            selectOnly(pin.ref().componentId());
            status = pin.ref().isOutput() ? "Output node selected. Click a compatible input, Esc, or right click."
                    : "Input node selected. Click a compatible output, Esc, or right click.";
            audio.playClick(true);
            return;
        }
        if (pendingWire.equals(pin.ref())) {
            pendingWire = null;
            clearSelection();
            status = "Node selection cancelled.";
            audio.playClick(false);
            return;
        }
        Circuit.ConnectResult result;
        PinRef inputRef;
        EditorSnapshot before = snapshotEditor();
        if (pendingWire.isOutput() && pin.ref().isInput()) {
            result = circuit.connect(pendingWire, pin.ref());
            inputRef = pin.ref();
        } else if (pendingWire.isInput() && pin.ref().isOutput()) {
            result = circuit.connect(pin.ref(), pendingWire);
            inputRef = pendingWire;
        } else {
            pendingWire = pin.ref();
            selectOnly(pin.ref().componentId());
            status = pin.ref().isOutput() ? "Output node selected. Click a compatible input, Esc, or right click."
                    : "Input node selected. Click a compatible output, Esc, or right click.";
            audio.playClick(true);
            return;
        }
        finishConnection(result, inputRef, before, "Connect pins");
    }

    private void finishConnection(Circuit.ConnectResult result, PinRef inputRef, EditorSnapshot before, String label) {
        if (result.success()) {
            selectOnly(inputRef.componentId());
            pendingWire = null;
            recordEdit(label, before);
            status = result.message();
        } else status = result.message();
        audio.playClick(result.success());
    }

    private void handleKeyPress(int key, int mods) {
        boolean shortcut = hasShortcutModifier(mods);
        switch (key) {
            case GLFW_KEY_ESCAPE -> cancelEditorState("Esc");
            case GLFW_KEY_DELETE -> deleteSelectedComponents();
            case GLFW_KEY_S -> {
                simulationRunning = !simulationRunning;
                status = simulationRunning ? "Simulation resumed." : "Simulation paused.";
                audio.playClick(simulationRunning);
            }
            case GLFW_KEY_C -> {
                if (shortcut) copySelection();
                else {
                    camera.reset();
                    updateHoverState();
                    status = "Camera re-centered.";
                }
            }
            case GLFW_KEY_V -> { if (shortcut) pasteClipboard(); }
            case GLFW_KEY_D -> { if (shortcut) duplicateSelection(); }
            case GLFW_KEY_Z -> { if (shortcut && (mods & GLFW_MOD_SHIFT) != 0) redo(); else if (shortcut) undo(); }
            case GLFW_KEY_Y -> { if (shortcut) redo(); }
            case GLFW_KEY_1 -> setTool(Tool.PLACE_BUTTON);
            case GLFW_KEY_2 -> setTool(Tool.PLACE_SWITCH);
            case GLFW_KEY_3 -> setTool(Tool.PLACE_NAND);
            case GLFW_KEY_4 -> setTool(Tool.PLACE_LED);
            default -> { }
        }
    }

    private void cancelEditorState(String trigger) {
        boolean hadState = pendingWire != null || tool != Tool.INTERACT || !selectedComponentIds.isEmpty()
                || marqueeCandidate || marqueeSelecting || dragCandidateId != -1 || draggingComponent || pressedButtonId != -1;
        if (draggingComponent && dragStartSnapshot != null) restoreEditor(dragStartSnapshot);
        else if (pressedButtonId != -1) circuit.componentById(pressedButtonId).ifPresent(component -> component.setSourceActive(false));

        pendingWire = null;
        tool = Tool.INTERACT;
        clearSelection();
        pressedButtonId = -1;
        dragCandidateId = -1;
        draggingComponent = false;
        componentPressSelectionOnly = false;
        dragStartSnapshot = null;
        groupDragStartCenters.clear();
        marqueeCandidate = false;
        marqueeSelecting = false;
        marqueeAdditive = false;
        if (!input.panDown()) input.setPanning(false);
        updateHoverState();
        status = hadState ? trigger + " cancelled the active editor state." : "Interaction mode.";
        audio.playClick(false);
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
        EditorSnapshot before = snapshotEditor();
        int id = target.id();
        if (pressedButtonId == id) pressedButtonId = -1;
        dragCandidateId = -1;
        draggingComponent = false;
        circuit.removeComponent(id);
        selectedComponentIds.remove(id);
        syncPrimarySelection();
        hoveredComponentId = -1;
        pendingWire = null;
        recordEdit("Delete component", before);
        status = "Component deleted from hover trash icon.";
        audio.playClick(false);
    }

    private Optional<CircuitComponent> findTrashTarget(double mouseX, double mouseY) {
        List<CircuitComponent> components = circuit.components();
        for (int i = components.size() - 1; i >= 0; i--) {
            CircuitComponent component = components.get(i);
            if (trashIconRect(component).contains(mouseX, mouseY)) return Optional.of(component);
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
                status = pendingWire == null ? "Empty grid selected." : "Node still selected: click a compatible node or press Esc/right click.";
            }
            return;
        }
        Rect selection = normalizedRect(marqueeStartWorld, marqueeEndWorld);
        List<Integer> hits = new ArrayList<>();
        for (CircuitComponent component : circuit.components()) if (overlaps(selection, component.bounds())) hits.add(component.id());
        if (!marqueeAdditive) selectedComponentIds.clear();
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
            if (selectedComponentIds.contains(component.id())) components.add(new ClipboardComponent(component.id(), component.kind(), component.center(), component.sourceActive()));
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
        if (pasteWouldCollide(offset)) {
            status = "Paste blocked: no nearby free space found.";
            audio.playClick(false);
            return;
        }
        EditorSnapshot before = snapshotEditor();
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
                circuit.connect(new PinRef(fromId, PinDirection.OUTPUT, wire.fromIndex()), new PinRef(toId, PinDirection.INPUT, wire.toIndex()));
            }
        }
        selectedComponentIds.clear();
        selectedComponentIds.addAll(pastedIds);
        syncPrimarySelection();
        pendingWire = null;
        tool = Tool.INTERACT;
        pasteSequence++;
        recordEdit("Paste block", before);
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
        Vec2 offset = mouseWorld.subtract(copyAnchor).add(new Vec2(pasteSequence * UiMetrics.GRID_SIZE, pasteSequence * UiMetrics.GRID_SIZE));
        for (int attempt = 0; attempt < 16; attempt++) {
            if (!pasteWouldCollide(offset)) return offset;
            offset = offset.add(new Vec2(UiMetrics.GRID_SIZE, UiMetrics.GRID_SIZE));
        }
        return offset;
    }

    private boolean pasteWouldCollide(Vec2 offset) {
        for (ClipboardComponent item : clipboardComponents) {
            if (collidesWithExistingComponent(componentBounds(item.kind(), snap(item.center().add(offset))))) return true;
        }
        return false;
    }

    private void deleteSelectedComponents() {
        if (selectedComponentIds.isEmpty()) {
            status = "Nothing selected to delete.";
            audio.playClick(false);
            return;
        }
        EditorSnapshot before = snapshotEditor();
        List<Integer> ids = new ArrayList<>(selectedComponentIds);
        if (ids.contains(pressedButtonId)) pressedButtonId = -1;
        circuit.removeComponents(ids);
        clearSelection();
        pendingWire = null;
        dragCandidateId = -1;
        draggingComponent = false;
        hoveredComponentId = -1;
        recordEdit("Delete selection", before);
        status = ids.size() == 1 ? "Component deleted." : ids.size() + " components deleted.";
        audio.playClick(false);
    }

    private List<PlacementPreview> placementPreviews() {
        if (!tool.isPlacement() || pendingWire != null || toolbar.contains(input.mouseY(), viewport.windowHeight()) || input.leftDown() || input.panning()) return List.of();
        ComponentKind kind = tool.componentKind().orElseThrow();
        Vec2 world = camera.screenToWorld(new Vec2(input.mouseX(), input.mouseY()), viewport);
        Optional<CircuitComponent> hoveredComponent = circuit.findComponent(world);
        if (hoveredComponent.isPresent()) return slotPreviewsFor(kind, hoveredComponent.get(), world);
        Optional<PlacementCandidate> hoveredSlot = placementSlotAt(kind, world);
        if (hoveredSlot.isPresent()) return slotPreviewsFor(kind, componentBySlot(kind, hoveredSlot.get()).orElse(null), world);
        Vec2 center = placementCenter(kind, world);
        Rect bounds = componentBounds(kind, center);
        return collidesWithExistingComponent(bounds) ? List.of() : List.of(new PlacementPreview(kind, bounds, placementAlignment().label()));
    }

    private List<PlacementPreview> slotPreviewsFor(ComponentKind kind, CircuitComponent component, Vec2 mouseWorld) {
        if (component == null) return List.of();
        List<PlacementPreview> previews = new ArrayList<>();
        for (PlacementCandidate candidate : adjacentPlacementCandidates(kind, component)) {
            boolean hovered = candidate.slotBounds().contains(mouseWorld);
            previews.add(new PlacementPreview(kind, candidate.slotBounds(), candidate.label(), true, hovered));
            if (hovered) previews.add(new PlacementPreview(kind, componentBounds(kind, candidate.center()), candidate.label(), false, true));
        }
        return previews;
    }

    private Optional<CircuitComponent> componentBySlot(ComponentKind kind, PlacementCandidate slot) {
        for (CircuitComponent component : circuit.components()) {
            for (PlacementCandidate candidate : adjacentPlacementCandidates(kind, component)) {
                if (candidate.side() == slot.side() && candidate.center().equals(slot.center())) return Optional.of(component);
            }
        }
        return Optional.empty();
    }

    private Optional<PlacementCandidate> placementSlotAt(ComponentKind kind, Vec2 world) {
        for (CircuitComponent component : circuit.components()) {
            for (PlacementCandidate candidate : adjacentPlacementCandidates(kind, component)) {
                if (candidate.slotBounds().contains(world)) return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private List<PlacementCandidate> adjacentPlacementCandidates(ComponentKind kind, CircuitComponent target) {
        List<PlacementCandidate> candidates = new ArrayList<>(4);
        Rect targetBounds = target.bounds();
        for (PlacementSide side : PlacementSide.values()) {
            Rect slotBounds = side.slotBounds(targetBounds);
            Vec2 center = side.candidateCenter(kind, targetBounds);
            if (!collidesWithExistingComponent(componentBounds(kind, center)) && !slotCollidesWithOtherComponent(slotBounds, target.id())) {
                candidates.add(new PlacementCandidate(center, side.label(), side, slotBounds));
            }
        }
        return candidates;
    }

    private boolean slotCollidesWithOtherComponent(Rect slotBounds, int sourceComponentId) {
        for (CircuitComponent component : circuit.components()) {
            if (component.id() != sourceComponentId && overlaps(slotBounds, component.bounds())) return true;
        }
        return false;
    }

    private Vec2 placementCenter(ComponentKind kind, Vec2 rawWorld) {
        return switch (placementAlignment()) {
            case FREE -> rawWorld;
            case GRID -> snap(rawWorld);
            case PIN_ROW -> alignPinsOnHorizontalAxis(kind, snap(rawWorld));
            case EDGE_COLUMN -> alignEdgesWithComponentsAbove(kind, snap(rawWorld));
        };
    }

    private PlacementAlignment placementAlignment() {
        if (input.controlDown() && input.altDown()) return PlacementAlignment.EDGE_COLUMN;
        if (input.controlDown()) return PlacementAlignment.PIN_ROW;
        if (input.altDown()) return PlacementAlignment.FREE;
        return PlacementAlignment.GRID;
    }

    private Vec2 alignPinsOnHorizontalAxis(ComponentKind kind, Vec2 center) {
        double bestDelta = 0.0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (PinEndpoint candidatePin : pinsFor(kind, center)) {
            for (CircuitComponent existingComponent : circuit.components()) {
                for (PinEndpoint existingPin : existingComponent.pins()) {
                    if (!oppositeDirections(candidatePin.ref(), existingPin.ref())) continue;
                    double delta = existingPin.worldPosition().y() - candidatePin.worldPosition().y();
                    double distance = Math.abs(delta);
                    if (distance < bestDistance && distance <= PIN_ALIGNMENT_MAX_DELTA_WORLD) {
                        bestDistance = distance;
                        bestDelta = delta;
                    }
                }
            }
        }
        return bestDistance == Double.POSITIVE_INFINITY ? center : new Vec2(center.x(), center.y() + bestDelta);
    }

    private Vec2 alignEdgesWithComponentsAbove(ComponentKind kind, Vec2 center) {
        Rect candidate = componentBounds(kind, center);
        double bestDelta = 0.0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (CircuitComponent component : circuit.components()) {
            if (component.center().y() >= center.y()) continue;
            Rect existing = component.bounds();
            double[] deltas = {existing.x() - candidate.x(), existing.x() + existing.width() - candidate.x() - candidate.width()};
            for (double delta : deltas) {
                double distance = Math.abs(delta);
                if (distance < bestDistance && distance <= EDGE_ALIGNMENT_MAX_DELTA_WORLD) {
                    bestDistance = distance;
                    bestDelta = delta;
                }
            }
        }
        return bestDistance == Double.POSITIVE_INFINITY ? center : new Vec2(center.x() + bestDelta, center.y());
    }

    private List<PinEndpoint> pinsFor(ComponentKind kind, Vec2 center) {
        return new CircuitComponent(-1, kind, center.x() - kind.width() / 2.0, center.y() - kind.height() / 2.0).pins();
    }

    private static boolean oppositeDirections(PinRef a, PinRef b) {
        return a.direction() != b.direction();
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            status = "Nothing to undo.";
            audio.playClick(false);
            return;
        }
        UndoableEdit edit = undoStack.removeLast();
        restoreEditor(edit.before());
        redoStack.addLast(edit);
        status = "Undo: " + edit.label() + ".";
        audio.playClick(false);
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            status = "Nothing to redo.";
            audio.playClick(false);
            return;
        }
        UndoableEdit edit = redoStack.removeLast();
        restoreEditor(edit.after());
        undoStack.addLast(edit);
        status = "Redo: " + edit.label() + ".";
        audio.playClick(true);
    }

    private void recordEdit(String label, EditorSnapshot before) {
        EditorSnapshot after = snapshotEditor();
        if (before.equals(after)) return;
        undoStack.addLast(new UndoableEdit(label, before, after));
        while (undoStack.size() > HISTORY_LIMIT) undoStack.removeFirst();
        redoStack.clear();
    }

    private EditorSnapshot snapshotEditor() {
        return new EditorSnapshot(circuit.snapshot(), List.copyOf(selectedComponentIds), pendingWire, tool);
    }

    private void restoreEditor(EditorSnapshot snapshot) {
        circuit.restore(snapshot.circuit());
        selectedComponentIds.clear();
        for (int id : snapshot.selectedIds()) if (circuit.componentById(id).isPresent()) selectedComponentIds.add(id);
        syncPrimarySelection();
        pendingWire = validPin(snapshot.pendingWire()) ? snapshot.pendingWire() : null;
        tool = snapshot.tool();
        pressedButtonId = -1;
        dragCandidateId = -1;
        draggingComponent = false;
        componentPressSelectionOnly = false;
        dragStartSnapshot = null;
        groupDragStartCenters.clear();
        updateHoverState();
    }

    private boolean validPin(PinRef ref) {
        if (ref == null) return false;
        Optional<CircuitComponent> component = circuit.componentById(ref.componentId());
        if (component.isEmpty()) return false;
        int count = ref.isOutput() ? component.get().kind().outputCount() : component.get().kind().inputCount();
        return ref.index() >= 0 && ref.index() < count;
    }

    private void rememberGroupDragStartCenters() {
        groupDragStartCenters.clear();
        for (CircuitComponent component : circuit.components()) if (selectedComponentIds.contains(component.id())) groupDragStartCenters.put(component.id(), component.center());
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
        for (int id : selectedComponentIds) selectedComponentId = id;
    }

    private String selectionSummary() {
        return switch (selectedComponentIds.size()) {
            case 0 -> "No component selected.";
            case 1 -> "1 component selected.";
            default -> selectedComponentIds.size() + " components selected.";
        };
    }

    private Vec2 snap(Vec2 world) {
        double grid = renderer.gridSize();
        return new Vec2(Math.round(world.x() / grid) * grid, Math.round(world.y() / grid) * grid);
    }

    private double pinHitRadiusWorld() {
        return UiMetrics.PIN_HIT_RADIUS_SCREEN / Math.max(0.18, camera.zoom());
    }

    private Rect componentBounds(ComponentKind kind, Vec2 center) {
        return new Rect(center.x() - kind.width() / 2.0, center.y() - kind.height() / 2.0, kind.width(), kind.height());
    }

    private boolean collidesWithExistingComponent(Rect candidate) {
        for (CircuitComponent component : circuit.components()) if (overlaps(candidate, expand(component.bounds(), UiMetrics.COMPONENT_COLLISION_GAP_WORLD))) return true;
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
        return a.x() < b.x() + b.width() && a.x() + a.width() > b.x() && a.y() < b.y() + b.height() && a.y() + a.height() > b.y();
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
        if (callback != null) callback.free();
    }

    private record ClipboardComponent(int sourceId, ComponentKind kind, Vec2 center, boolean sourceActive) { }
    private record ClipboardWire(int fromComponentId, int fromIndex, int toComponentId, int toIndex) { }
    private record EditorSnapshot(Circuit.Snapshot circuit, List<Integer> selectedIds, PinRef pendingWire, Tool tool) { }
    private record UndoableEdit(String label, EditorSnapshot before, EditorSnapshot after) { }
    private record PlacementCandidate(Vec2 center, String label, PlacementSide side, Rect slotBounds) { }

    private enum PlacementSide {
        LEFT("left side"), RIGHT("right side"), TOP("top side"), BOTTOM("bottom side");
        private final String label;
        PlacementSide(String label) { this.label = label; }
        String label() { return label; }
        Rect slotBounds(Rect target) {
            double margin = PLACEMENT_SLOT_MARGIN_WORLD;
            double thickness = PLACEMENT_SLOT_THICKNESS_WORLD;
            return switch (this) {
                case LEFT -> new Rect(target.x() - margin - thickness, target.y(), thickness, target.height());
                case RIGHT -> new Rect(target.x() + target.width() + margin, target.y(), thickness, target.height());
                case TOP -> new Rect(target.x(), target.y() - margin - thickness, target.width(), thickness);
                case BOTTOM -> new Rect(target.x(), target.y() + target.height() + margin, target.width(), thickness);
            };
        }
        Vec2 candidateCenter(ComponentKind kind, Rect target) {
            double gap = PLACEMENT_SLOT_MARGIN_WORLD + PLACEMENT_SLOT_THICKNESS_WORLD + PLACEMENT_CANDIDATE_MARGIN_WORLD;
            return switch (this) {
                case LEFT -> new Vec2(target.x() - gap - kind.width() / 2.0, target.centerY());
                case RIGHT -> new Vec2(target.x() + target.width() + gap + kind.width() / 2.0, target.centerY());
                case TOP -> new Vec2(target.centerX(), target.y() - gap - kind.height() / 2.0);
                case BOTTOM -> new Vec2(target.centerX(), target.y() + target.height() + gap + kind.height() / 2.0);
            };
        }
    }

    private enum PlacementAlignment {
        GRID("grid"), FREE("free"), PIN_ROW("pin row"), EDGE_COLUMN("edge column");
        private final String label;
        PlacementAlignment(String label) { this.label = label; }
        String label() { return label; }
    }
}
