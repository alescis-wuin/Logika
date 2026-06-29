# UI/UX Improvement Plan

## Implemented slice

This branch builds on the block editing workflow and adds placement quality-of-life plus undo/redo.

### Placement hologram

- A placement preview is shown while a placement tool is active.
- The preview appears only if the computed placement rectangle is free.
- The preview displays the component kind and active alignment mode.

### Alignment modes

- Default: snap to the grid.
- `Alt`: free placement.
- `Ctrl`: aligns candidate pins with opposite-direction pins on the same horizontal row.
- `Ctrl+Alt`: aligns candidate left/right edges with components above.

### Undo/redo

- `Ctrl+Z`: undo.
- `Ctrl+Y` or `Ctrl+Shift+Z`: redo.
- History is snapshot-backed for full restoration of circuit structure and editor selection.
- Tracked edits include placement, movement, connection, chaining, switch toggle, deletion, clear, paste, and duplicate.

## Next recommended slice

1. Replace snapshot history with command objects when circuit sizes grow.
2. Add save/load JSON.
3. Add reusable custom components.
4. Add wire selection/rerouting.
5. Add graph diagnostics.
