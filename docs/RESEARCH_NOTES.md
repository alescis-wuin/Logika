# Research Notes

## GLFW and OpenGL

GLFW initialization hints are applied before `glfwInit()`. Logika uses this rule to request the X11 platform when a Linux Wayland session is detected.

GLFW exposes both a window content size and a framebuffer size. Logika stores both values in `Viewport`: NanoVG receives the content size and device pixel ratio, while OpenGL viewport updates use framebuffer pixels.

## NanoVG

NanoVG draws in screen coordinates. World positions are converted by `Camera2D` before rendering components, pins, wires, hover affordances, and the grid.

```java
nvgBeginFrame(vg, windowWidth, windowHeight, devicePixelRatio);
```

## UI and UX principles used

- visible state: active placement tool, pending node, simulation state, hover state, and status are always shown;
- recognition over recall: component placement remains visible in the bottom toolbar;
- direct manipulation: linking, source interaction, movement, and deletion happen directly on the grid;
- minimalist V1 surface: placement, direct node linking, simulation, and reset only;
- generous targets: toolbar buttons and pins use large hit areas;
- state is not color-only: logical values also appear as text labels.

## Deferred items

- save/load;
- undo/redo;
- component palette search;
- reusable subcircuits;
- clocks and timing components;
- loop diagnostics;
- custom icon and font assets;
- richer OpenAL feedback.
