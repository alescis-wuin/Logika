# Research Notes

## GLFW

The editor keeps GLFW callbacks for mouse and keyboard transitions so selection, drag, shortcut, and chaining states are processed at the moment the event is received.

The render loop still polls events every frame because Logika renders continuously.

## NanoVG

NanoVG remains the UI layer. World positions are converted with `Camera2D`, then rendered as screen-space cards, pins, wires, badges, overlays, and selection rectangles.

## UX principles used

- visible editor state;
- direct manipulation on the circuit canvas;
- large pointer targets;
- text labels in addition to color;
- predictable copy/paste of blocks and internal wires;
- fast chaining with an explicit variant shortcut.

## Deferred items

- undo/redo;
- save/load;
- reusable subcircuits;
- graph diagnostics;
- accessibility presets.
