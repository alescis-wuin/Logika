# Research Notes

## GLFW

The editor keeps GLFW callbacks for mouse and keyboard transitions so selection, drag, shortcut, undo/redo, modifier, and chaining states are processed when events are received.

The render loop still polls events every frame because Logika renders continuously.

Modifier state is stored in `InputState` because placement previews need to react while the cursor is moving, not only when a mouse click arrives.

## NanoVG

NanoVG remains the UI layer. World positions are converted with `Camera2D`, then rendered as screen-space cards, pins, wires, badges, overlays, placement holograms, and selection rectangles.

## UX principles used

- visible editor state;
- direct manipulation on the circuit canvas;
- placement previews only for valid placements;
- modifier-based alignment without adding toolbar complexity;
- large pointer targets;
- text labels in addition to color;
- predictable copy/paste of blocks and internal wires;
- undo/redo for destructive and structural edits;
- fast chaining with an explicit variant shortcut.

## Deferred items

- save/load;
- reusable subcircuits;
- graph diagnostics;
- accessibility presets;
- command-level history instead of snapshot-level history if circuit sizes become large.
