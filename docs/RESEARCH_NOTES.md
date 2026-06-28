# Research Notes

## GLFW and OpenGL

GLFW initialization hints are applied before `glfwInit()`. Logika uses this rule to request the X11 platform when a Linux Wayland session is detected.

GLFW exposes both a window content size and a framebuffer size. Logika stores both values in `Viewport`: NanoVG receives the content size and device pixel ratio, while OpenGL viewport updates use framebuffer pixels.

## NanoVG

NanoVG draws in screen coordinates. World positions are converted by `Camera2D` before rendering components, pins, wires, and the grid.

```java
nvgBeginFrame(vg, windowWidth, windowHeight, devicePixelRatio);
```

## UI and UX principles used

- visible state: the active tool, pending wire, simulation state, and status are always shown;
- recognition over recall: tools remain visible in the bottom toolbar;
- minimalist V1 surface: placement, wiring, simulation, and reset only;
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
