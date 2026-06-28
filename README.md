# Logika

Logika is a small Java/LWJGL logic-circuit sandbox. Version 1.0 focuses on a clear 2D editor loop: camera, zoom-at-cursor, grid placement, a bottom toolbar, basic components, wires, and live logic simulation.

## Requirements

- JDK 21+
- Maven 3.9+
- Linux, Windows, or macOS with OpenGL support

On Linux Wayland sessions, Logika requests GLFW's X11 backend before `glfwInit()` because the current editor uses GLFW/OpenGL/NanoVG through the X11 path.

## Run

```bash
git clone https://github.com/alescis-wuin/Logika.git
cd Logika
git checkout ai/feat/logika-v1
mvn clean package
mvn exec:java
```

## Controls

- Mouse wheel: zoom at cursor.
- Right mouse drag or middle mouse drag: pan.
- Space + left drag: pan.
- Bottom toolbar: select a tool, then click the grid.
- Wire mode: click an output pin, then click a compatible input pin.
- Button component: hold left click on the component body to emit `true`.
- Switch component: click the component body to toggle `true` or `false`.
- `Esc`: cancel wire/tool state.
- `S`: pause/resume simulation.
- `Delete`: remove selected component.
- `C`: re-center the camera.

See [`docs/CONTROLS.md`](docs/CONTROLS.md) and [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).
