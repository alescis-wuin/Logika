# Logika

Logika is a small Java/LWJGL logic-circuit sandbox. Version 1.2 keeps the clear 2D editor loop and adds placement holograms, modifier-based alignment, and snapshot-backed undo/redo on top of the multi-selection and chaining workflow.

## Requirements

- JDK 21+
- Maven 3.9+
- Linux, Windows, or macOS with OpenGL support

On Linux Wayland sessions, Logika requests GLFW's X11 backend before `glfwInit()` because the current editor uses GLFW/OpenGL/NanoVG through the X11 path.

## Run

```bash
git clone https://github.com/alescis-wuin/Logika.git
cd Logika
git checkout ai/feat/placement-preview-undo
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
- Bottom toolbar: choose `Button`, `Switch`, `NAND`, or `LED`.
- Placement hologram: hover the canvas with a placement tool; the hologram appears only when the component can be placed.
- Placement alignment:
  - default: grid snap;
  - `Alt`: free placement;
  - `Ctrl`: pin-row alignment, aligning output and input pins on the horizontal axis;
  - `Ctrl+Alt`: edge-column alignment with components above.
- Selection: click a component to select it; Shift/Ctrl-click toggles it in the current selection.
- Area selection: drag from an empty grid cell to select every component touched by the selection rectangle.
- Group move: drag any selected component to move the whole selected block.
- Copy/paste: `Ctrl+C` copies selected components, their source states, and internal wires; `Ctrl+V` pastes the block near the cursor; `Ctrl+D` duplicates.
- Undo/redo: `Ctrl+Z` undoes editor changes; `Ctrl+Y` or `Ctrl+Shift+Z` redoes them.
- Node linking: click one node, then click a compatible node. Output -> input and input -> output are both accepted.
- Chaining: click an output node, then click a component body. The connection is made automatically and the target output remains selected for the next link.
- Chain variant: press `V` to cycle between automatic input, input A, input B, and A+B for NAND-as-inverter wiring.
- Button component: hold left click on the body to emit `true`.
- Switch component: click the body to toggle `true` or `false`.
- Delete component(s): hover a component and click its trash icon, or press `Delete` for the current selection.
- `Esc`: cancel pending node/tool/area state.
- `S`: pause/resume simulation.
- `C`: re-center the camera.
- `1`/`2`/`3`/`4`: choose Button, Switch, NAND, LED.

See [`docs/CONTROLS.md`](docs/CONTROLS.md), [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), [`docs/RESEARCH_NOTES.md`](docs/RESEARCH_NOTES.md), and [`docs/UI_UX_PLAN.md`](docs/UI_UX_PLAN.md).
