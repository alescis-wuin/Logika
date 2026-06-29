# Controls

## Camera

| Action | Input |
|---|---|
| Pan | Right mouse drag, middle mouse drag, or `Space` + left drag |
| Zoom | Mouse wheel; zoom is anchored under the cursor |
| Re-center camera | `C` |

## Editing

Placement remains armed while interacting with component bodies, pins, switches, buttons, and trash icons; only a side slot or empty canvas click places a new component.

| Action | Input |
|---|---|
| Choose placement tool | Bottom toolbar: `Button`, `Switch`, `NAND`, or `LED` |
| Place a component | Choose a tool, then click an empty grid cell or an adjacent dark `+` slot |
| Interact while placing | Click a component body, pin, or trash icon instead of a slot; placement is suspended and the component interaction runs |
| Cancel active editor state | `Esc` or right click |
| Select one component | Left click a component body |
| Toggle a component in the selection | `Shift`/`Ctrl` + left click a component body |
| Select an area | Drag from an empty grid cell |
| Move a block | Drag any selected component body |
| Copy selected block | `Ctrl+C` |
| Paste copied block | `Ctrl+V`; pasted components keep source states and internal wires |
| Duplicate selected block | `Ctrl+D` |
| Link nodes | Click one node, then click a compatible node; output-first and input-first are both accepted |
| Delete component(s) | Hover trash icon, or press `Delete` for the current selection |

## Visual feedback

| Feedback | Behavior |
|---|---|
| Hover cursor | Changes over toolbar actions, components, placement slots, pins, and selected blocks |
| Pending wire preview | Drawn from the selected pin to the pointer and visually snaps to the probable compatible target pin |
| Target halo | Pulses around the nearest compatible pin or around a rejected hovered target |
| Connection success | A short pulse travels along newly created wires |
| Active wire breathing | Wires carrying `true` gently pulse in width and opacity |

## Simulation

| Component | Behavior |
|---|---|
| Button | Momentary source. Hold the component body to emit `true`. |
| Switch | Toggle source. Click the component body to switch state. |
| NAND | Emits `!(A && B)`. Missing inputs are treated as `false`. |
| LED | Input indicator. It is bright when its input node is `true`, dark when `false`. |

The simulation is live by default. Press `S` or use the toolbar simulation button to pause or resume it.
