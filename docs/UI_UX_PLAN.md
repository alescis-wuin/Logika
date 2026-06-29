# UI/UX Improvement Plan

## Implemented slice

This branch focuses on editor workflow improvements that are useful before adding richer components or custom subcircuits.

### Selection and block editing

- Single selection remains a plain left click.
- Multi-selection uses Shift/Ctrl-click.
- Empty-grid drag creates a world-space selection rectangle.
- Dragging one selected component moves the full selected block.
- `Ctrl+C` stores selected component kinds, center positions, source states, and internal wires.
- `Ctrl+V` pastes the block near the cursor and tries nearby grid offsets if the first paste would collide.
- `Ctrl+D` duplicates through the same copy/paste path.

### Chaining

- Clicking an output node and then a component body connects to the chosen target automatically.
- The output of the target component remains selected when it exists, so the player can continue chaining.
- `V` cycles the target input policy: `Auto`, `A`, `B`, or `A+B`.

### Visual feedback

- Component bodies now use kind-specific colors.
- Selected components receive a blue glow, stronger border, and `SEL` badge.
- The top status panel shows selection count, clipboard count, chain variant, and active workflow hints.
- Area selection is drawn directly in the canvas.

## Next recommended slice

1. Add undo/redo before adding more destructive actions.
2. Add save/load so custom user components can persist.
3. Add a property panel for selected blocks and components.
4. Add keyboard-only editing paths and theme presets for accessibility.
5. Add graph diagnostics for cycles and invalid custom components.
