# Logika

Logika is a small Java/LWJGL logic-circuit sandbox. Version 1.0 focuses on a clear 2D editor loop: camera, zoom-at-cursor, grid placement, direct node linking, larger readable components, a simplified bottom toolbar, basic components, wires, and live logic simulation.

## Requirements

- JDK 21+
- Maven 3.9+
- Linux, Windows, or macOS with OpenGL support

On Linux Wayland sessions, Logika requests GLFW's X11 backend before `glfwInit()` because the current editor uses GLFW/OpenGL/NanoVG through the X11 path.

## Run

```bash
git clone https://github.com/alescis-wuin/Logika.git
cd Logika
git checkout ai/feat/ui-readability
mvn clean package
mvn exec:java
```

Optional font override:

```bash
LOGIKA_FONT=/path/to/Inter-Regular.ttf LOGIKA_FONT_BOLD=/path/to/Inter-SemiBold.ttf mvn exec:java
```

## Controls

- Mouse wheel: zoom at cursor.
- Right mouse drag, middle mouse drag, or Space + left drag: pan.
- Bottom toolbar: choose `Button`, `Switch`, `NAND`, or `LED`, then click an empty grid cell.
- Node linking: click one node, then click a compatible node. Output → input and input → output are both accepted.
- Signal badges: each visible node has an internal two-row name/value badge.
- Button component: hold left click on the body to emit `true`.
- Switch component: click the body to toggle `true` or `false`.
- Move component: hold on a component body and drag.
- Delete component: hover a component and click the trash icon in its upper-left corner.
- `Esc`: cancel pending node/tool state.
- `S`: pause/resume simulation.
- `Delete`: remove selected component.
- `C`: re-center the camera.
- `1`/`2`/`3`/`4`: choose Button, Switch, NAND, LED.

See [`docs/CONTROLS.md`](docs/CONTROLS.md), [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), and [`docs/RESEARCH_NOTES.md`](docs/RESEARCH_NOTES.md).
