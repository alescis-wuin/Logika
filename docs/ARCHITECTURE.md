# Architecture

## Goals

Logika 1.0 keeps the first version intentionally small while separating the main responsibilities:

- GLFW window lifecycle and input callbacks;
- OpenGL/NanoVG rendering;
- mathematical camera/projection;
- circuit data model;
- simulation logic;
- lightweight UI state;
- optional OpenAL feedback.

This keeps the editor easier to extend without creating a large framework.

## Packages

```text
dev.alexis.logika
├── audio        Optional OpenAL click feedback
├── engine       Application loop, GLFW callbacks, viewport state, direct manipulation
├── graphics     Camera, NanoVG facade, theme, circuit passes, and overlay passes
├── model        Circuit graph, components, pins, wires
├── simulation   Logic propagation
├── ui           Bottom toolbar, editor tools, shared UI metrics
└── util         Small value objects
```

## SOLID boundaries

- `LogikaEngine` owns orchestration only: lifecycle, callbacks, direct manipulation, hover state, drag state, and state transitions.
- `NanoVGRenderer` is a facade. It owns the NanoVG context and delegates drawing to focused render passes.
- `NvgCanvas` centralizes NanoVG primitives, crisp text placement, and font fallback loading.
- `RenderTheme` centralizes the high-contrast palette used by components, wires, nodes, and overlays.
- `GridWireRenderer` draws the grid, axes, wires, and pending wire affordance.
- `ComponentCanvasRenderer` draws cards, internal signal badges, pins, hover borders, and trash actions.
- `EditorOverlayRenderer` draws toolbar and status/legend overlays.
- `LogicSimulator` receives a `Circuit` and updates runtime values; it does not know GLFW or NanoVG.
- `Circuit` validates graph mutations such as compatible pin connections.
- `Camera2D` is purely mathematical and can be tested without LWJGL.

## Projection model

The editor uses a 2D orthographic projection implemented by `Camera2D`:

- world units are grid units;
- screen coordinates are GLFW content-area coordinates;
- framebuffer dimensions are used only for pixel-based OpenGL calls;
- zoom-at-cursor preserves the world coordinate under the pointer.

## Readability model

The renderer avoids scaling all text directly with world zoom. Card labels, overlay labels, signal badges, and axes labels use screen-space clamps so zooming out reduces the card footprint without making text proportionally blurrier or oversized.

The current readable card layout uses:

- larger component model sizes with a shared minimum width;
- a high-contrast dark palette;
- regular and bold NanoVG font faces loaded through `LOGIKA_FONT` and `LOGIKA_FONT_BOLD` fallbacks;
- rounded component cards with stronger hover and selection borders;
- larger pins and larger pin hit targets;
- internal two-row signal badges: pin name on the first row, logical value on the second row;
- a larger circular trash action with matching visual and intended click size.

## Direct manipulation model

The explicit `Select` and `Wire` toolbar actions were removed. Interaction is always available:

1. click a node to start a pending connection;
2. click a compatible node to complete the connection;
3. hold and drag a component body to move it on the grid;
4. hover a component to reveal the trash action;
5. click sources directly to use them.

## Simulation model

Logika 1.0 is a combinational simulator with deterministic iteration:

1. source components publish their current state;
2. wires copy output values into target input pins;
3. NAND components recompute output values;
4. LED components mirror their input value for rendering;
5. the loop repeats a bounded number of times to stabilize simple chains.

Cycles are not yet diagnosed. Future versions should add graph validation, clocked components, and explicit oscillation reporting.
