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
| Select tool | Bottom toolbar |
| Place a component | Select `Button`, `Switch`, or `NAND`, then click the grid |
| Start a wire | Click an output pin |
| Complete a wire | Click a compatible input pin |
| Cancel pending action | `Esc` |
| Delete selected component | `Delete` |

## Simulation

| Component | Behavior |
|---|---|
| Button | Momentary source. Hold the component body to emit `true`. |
| Switch | Toggle source. Click the component body to switch state. |
| NAND | Emits `!(A && B)`. Missing inputs are treated as `false`. |

The simulation is live by default. Press `S` or use the toolbar simulation button to pause or resume it.
