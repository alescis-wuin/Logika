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
| Link nodes | Click one node, then click a compatible node; output-first and input-first are both accepted |
| Move a component | Hold on the component body and drag |
| Delete a component | Hover it and click the trash icon in the upper-left corner |
| Cancel pending node/tool action | `Esc` |
| Delete selected component | `Delete` |

## Simulation

| Component | Behavior |
|---|---|
| Button | Momentary source. Hold the component body to emit `true`. |
| Switch | Toggle source. Click the component body to switch state. |
| NAND | Emits `!(A && B)`. Missing inputs are treated as `false`. |
| LED | Input indicator. It is bright when its input node is `true`, dark when `false`. |

The simulation is live by default. Press `S` or use the toolbar simulation button to pause or resume it.
