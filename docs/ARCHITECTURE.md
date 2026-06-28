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
├── graphics     Camera and NanoVG rendering
├── model        Circuit graph, components, pins, wires
├── simulation   Logic propagation
├── ui           Bottom toolbar and editor tools
└── util         Small value objects
```

## SOLID boundaries

- `LogikaEngine` owns orchestration only: lifecycle, callbacks, direct manipulation, hover state, drag state, and state transitions.
- `NanoVGRenderer` is read-only relative to the circuit model and only draws.
- `LogicSimulator` receives a `Circuit` and updates runtime values; it does not know GLFW or NanoVG.
- `Circuit` validates graph mutations such as compatible pin connections.
- `Camera2D` is purely mathematical and can be tested without LWJGL.

## Projection model

The editor uses a 2D orthographic projection implemented by `Camera2D`:

- world units are grid units;
- screen coordinates are GLFW content-area coordinates;
- framebuffer dimensions are used only for pixel-based OpenGL calls;
- zoom-at-cursor preserves the world coordinate under the pointer.

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
