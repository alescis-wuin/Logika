# Architecture

## Goals

Logika keeps a compact Java/LWJGL editor architecture with separate responsibilities for window lifecycle, rendering, camera math, circuit data, simulation, UI state, and optional audio feedback.

## Packages

```text
dev.alexis.logika
- audio: optional OpenAL click feedback
- engine: application loop, GLFW callbacks, viewport state, direct manipulation
- graphics: camera, NanoVG facade, circuit passes, and overlay passes
- model: circuit graph, components, pins, wires
- simulation: logic propagation
- ui: bottom toolbar, editor tools, shared UI metrics
- util: small value objects
```

## Boundaries

- `LogikaEngine` owns orchestration, callbacks, hover state, drag state, selection state, clipboard state, chaining state, and state transitions.
- `NanoVGRenderer` owns the NanoVG context and delegates drawing to focused render passes.
- `GridWireRenderer` draws the grid, axes, wires, and pending wire affordance.
- `ComponentCanvasRenderer` draws cards, signal badges, pins, hover borders, selected-card feedback, and component actions.
- `EditorOverlayRenderer` draws toolbar, status, selected count, clipboard count, and chain variant hints.
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

The explicit `Select` and `Wire` toolbar actions remain unnecessary. Interaction is always available:

1. click a node to start a pending connection;
2. click a compatible node to complete the connection;
3. click an output node, then a component body, to auto-connect and keep chaining from the target output;
4. press `V` to choose the chain variant: automatic input, A, B, or A+B;
5. click or Shift/Ctrl-click component bodies to select one or many components;
6. drag from empty grid space to area-select components;
7. drag any selected component body to move the selected block;
8. copy/paste selected blocks with internal wires and source states preserved;
9. click sources directly to use them.

## Simulation model

Logika remains a combinational simulator with deterministic iteration. Source components publish their current state, wires copy output values into target inputs, NAND components recompute outputs, and LED components mirror their input value for rendering.

Cycles are not yet diagnosed. Future versions should add graph validation, clocked components, and explicit oscillation reporting.
