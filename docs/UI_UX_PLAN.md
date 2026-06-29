# UI/UX Improvement Plan

## Implemented slice

This branch builds on the block editing workflow and keeps placement quality-of-life plus undo/redo while removing automatic component-body chaining.

### Cancellation

- `Esc` cancels pending node selection, active placement tool, marquee state, drag state, and current selection.
- Right click performs the same cancellation and still works as a pan gesture while held.

### Selection and block editing

- Left click selects one component.
- `Shift`/`Ctrl` + left click toggles a component in the current selection.
- Dragging from an empty grid cell selects every component touched by the rectangle.
- Dragging any selected component moves the selected block.
- `Ctrl+C`, `Ctrl+V`, and `Ctrl+D` preserve selected components, source states, and internal wires.
- `Delete` removes the current selection.

### Placement hologram

- A placement preview is shown while a placement tool is active.
- The preview appears only if the computed placement rectangle is free.
- The preview displays the component kind and active alignment mode.
- Clicking a component body, pin, or trash icon while a placement tool is active suspends placement and runs the normal interaction.

### Alignment modes

- Default: snap to the grid.
- `Alt`: free placement.
- `Ctrl`: aligns candidate pins with opposite-direction pins on the same horizontal row.
- `Ctrl+Alt`: aligns candidate left/right edges with components above.

### Node linking

- Node linking remains explicit: click one node, then click a compatible opposite-direction node.
- Automatic component-body chaining is disabled.
- A completed wire clears the pending node instead of keeping the next output selected.

### Undo/redo

- `Ctrl+Z`: undo.
- `Ctrl+Y` or `Ctrl+Shift+Z`: redo.
- History is snapshot-backed for full restoration of circuit structure and editor selection.
- Tracked edits include placement, movement, connection, switch toggle, deletion, clear, paste, and duplicate.

## Next recommended slice

1. Replace snapshot history with command objects when circuit sizes grow.
2. Add save/load JSON.
3. Add reusable custom components.
4. Add wire selection/rerouting.
5. Add graph diagnostics.


### Placement suspension

- The selected placement tool remains armed while testing existing components.
- Component bodies, pins and trash icons keep their normal interactions unless the click targets one of the four side slots.
- Pending node links suppress placement previews until the link is completed or cancelled.
