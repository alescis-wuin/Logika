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
├── engine       Application loop, GLFW callbacks, viewport state
├── graphics     Camera and NanoVG rendering
├── model        Circuit graph, components, pins, wires
├── simulation   Logic propagation
├── ui           Bottom toolbar and editor tools
└── util         Small value objects
```

## SOLID boundaries

- `LogikaEngine` owns orchestration only: lifecycle, callbacks, state transitions.
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

## Simulation model

Logika 1.0 is a combinational simulator with deterministic iteration:

1. source components publish their current state;
2. wires copy output values into target input pins;
3. NAND components recompute output values;
4. the loop repeats a bounded number of times to stabilize simple chains.

Cycles are not yet diagnosed. Future versions should add graph validation, clocked components, and explicit oscillation reporting.
