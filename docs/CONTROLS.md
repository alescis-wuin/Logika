# Controls

## Camera

| Action | Input |
|---|---|
| Pan | Right mouse drag, middle mouse drag, or `Space` + left drag |
| Zoom | Mouse wheel; zoom is anchored under the cursor |
| Re-center camera | `C` |

## Editing

| Action | Input |
|---|---|
| Choose placement tool | Bottom toolbar: `Button`, `Switch`, `NAND`, or `LED` |
| Place a component | Choose a tool, then click an empty grid cell |
| Select one component | Left click a component body |
| Toggle a component in the selection | `Shift`/`Ctrl` + left click a component body |
| Select an area | Drag from an empty grid cell |
| Move a block | Drag any selected component body |
| Copy selected block | `Ctrl+C` |
| Paste copied block | `Ctrl+V`; pasted components keep source states and internal wires |
| Duplicate selected block | `Ctrl+D` |
| Link nodes | Click one node, then click a compatible node; output-first and input-first are both accepted |
| Chain components | Click an output node, then click a target component body; the target output remains selected |
| Change chain variant | `V`: Auto, A, B, A+B |
| Delete component(s) | Hover trash icon, or press `Delete` for the current selection |
| Cancel pending node/tool/action | `Esc` |

## Simulation

| Component | Behavior |
|---|---|
| Button | Momentary source. Hold the component body to emit `true`. |
| Switch | Toggle source. Click the component body to switch state. |
| NAND | Emits `!(A && B)`. Missing inputs are treated as `false`. A+B chaining connects the same source to both inputs, which creates an inverter. |
| LED | Input indicator. It is bright when its input node is `true`, dark when `false`. |

The simulation is live by default. Press `S` or use the toolbar simulation button to pause or resume it.
