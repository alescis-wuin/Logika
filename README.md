# Logika

Logika is a small Java/LWJGL logic-circuit sandbox for building and testing logic circuits. This branch adds a NanoVG text input bar for future component names, cable labels, and free text placement.

## Requirements

- JDK 21+
- Maven 3.9+
- Linux, Windows, or macOS with OpenGL support

## Run

```bash
git fetch
git checkout ai/feat/text-input-bar
mvn clean package
mvn exec:java
```

## Controls

- Mouse wheel: zoom at cursor.
- Right mouse drag, middle mouse drag, or Space + left drag: pan.
- Bottom toolbar: choose `Button`, `Switch`, `NAND`, or `LED`.
- Place a component: choose a tool, then click an empty grid cell or an adjacent dark `+` slot.
- Select one component: left click a component body.
- Move selected components: drag any selected component body.
- Link nodes: click one node, then click a compatible node.
- Text bar: click or drag in the upper text field, use `Shift` + arrows/Home/End for range selection, `Ctrl+A/C/X/V`, focused text undo/redo, `Enter` to validate, or `Esc` to cancel.

See [`docs/CONTROLS.md`](docs/CONTROLS.md), [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), [`docs/RESEARCH_NOTES.md`](docs/RESEARCH_NOTES.md), and [`docs/UI_UX_PLAN.md`](docs/UI_UX_PLAN.md).
