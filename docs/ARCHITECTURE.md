# Architecture

## Goals

Logika keeps a compact Java/LWJGL editor architecture with separate responsibilities for window lifecycle, rendering, camera math, circuit data, simulation, UI state, and optional audio feedback.

## Boundaries

- `LogikaEngine` owns callbacks, drag state, selection state, clipboard state, placement alignment, undo/redo history, chaining state, and state transitions.
- `Circuit` owns graph validation plus snapshot/restore support for editor history.
- `NanoVGRenderer` owns the NanoVG context and delegates drawing to focused render passes.
- `CircuitCanvasRenderer` draws grid/wire layers, placement previews, selection rectangles, and component layers.
- `ComponentCanvasRenderer` draws cards, placement holograms, signal badges, pins, selected feedback, and component actions.
- `EditorOverlayRenderer` draws toolbar, status, selected count, clipboard count, chain variant, and history counts.
- `LogicSimulator` receives a `Circuit` and updates runtime values; it does not know GLFW or NanoVG.

## Placement model

Placement uses a single candidate center derived from cursor world coordinates and the active modifier state:

- default: grid snap;
- `Alt`: free world placement;
- `Ctrl`: pin-row alignment between output and input pins on the horizontal axis;
- `Ctrl+Alt`: edge-column alignment against components above.

A placement hologram is emitted only when the resulting candidate rectangle does not collide with existing components.

## Undo/redo model

Undo/redo uses immutable editor snapshots. Each edit stores the before/after state for the circuit, selection, pending pin, tool, and chain variant. Redo history is cleared when a new edit is recorded.

Tracked edits include placement, movement, component removal, group removal, clear, wire connection, component chaining, paste, duplicate, and switch toggles.
