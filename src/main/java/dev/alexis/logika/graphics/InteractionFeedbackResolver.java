package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.CircuitComponent;
import dev.alexis.logika.model.PinEndpoint;
import dev.alexis.logika.model.PinRef;
import dev.alexis.logika.model.Wire;
import dev.alexis.logika.ui.PlacementPreview;
import dev.alexis.logika.ui.Toolbar;
import dev.alexis.logika.ui.UiMetrics;
import dev.alexis.logika.util.Vec2;

import java.util.List;
import java.util.Optional;
import java.util.Set;

final class InteractionFeedbackResolver {
    WireTargetFeedback resolveWireTarget(Camera2D camera, Viewport viewport, Circuit circuit, PinRef pendingWire,
                                         PinRef hoveredPin, double mouseX, double mouseY) {
        if (pendingWire == null || !validPin(circuit, pendingWire)) {
            return WireTargetFeedback.none();
        }
        if (hoveredPin != null && !hoveredPin.equals(pendingWire)) {
            return new WireTargetFeedback(hoveredPin, canConnect(circuit, pendingWire, hoveredPin));
        }

        Vec2 mouseWorld = camera.screenToWorld(new Vec2(mouseX, mouseY), viewport);
        double searchRadiusWorld = UiMetrics.PIN_HIT_RADIUS_SCREEN / Math.max(0.18, camera.zoom());
        PinRef bestRef = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (CircuitComponent component : circuit.components()) {
            for (PinEndpoint pin : component.pins()) {
                PinRef candidate = pin.ref();
                if (candidate.equals(pendingWire) || !canConnect(circuit, pendingWire, candidate)) {
                    continue;
                }
                double distance = pin.worldPosition().distanceTo(mouseWorld);
                if (distance <= searchRadiusWorld && distance < bestDistance) {
                    bestDistance = distance;
                    bestRef = candidate;
                }
            }
        }
        return bestRef == null ? WireTargetFeedback.none() : new WireTargetFeedback(bestRef, true);
    }

    CursorFeedback.Style resolveCursor(Toolbar toolbar, Viewport viewport, List<PlacementPreview> placementPreviews,
                                       Set<Integer> selectedComponentIds, int hoveredComponentId,
                                       boolean draggingComponent, PinRef hoveredPin,
                                       WireTargetFeedback targetFeedback, double mouseX, double mouseY) {
        if (targetFeedback.invalid()) {
            return CursorFeedback.Style.FORBIDDEN;
        }
        if (hoveredPin != null || targetFeedback.active()) {
            return CursorFeedback.Style.WIRE;
        }
        if (draggingComponent || (hoveredComponentId != -1 && selectedComponentIds.contains(hoveredComponentId))) {
            return CursorFeedback.Style.MOVE;
        }
        if (toolbar.actionAt(mouseX, mouseY, viewport.windowWidth(), viewport.windowHeight()).isPresent()) {
            return CursorFeedback.Style.HAND;
        }
        if (hoveredPlacementPreview(placementPreviews) || hoveredComponentId != -1) {
            return CursorFeedback.Style.HAND;
        }
        return CursorFeedback.Style.DEFAULT;
    }

    private boolean hoveredPlacementPreview(List<PlacementPreview> placementPreviews) {
        for (PlacementPreview preview : placementPreviews) {
            if (preview.hovered()) {
                return true;
            }
        }
        return false;
    }

    private boolean canConnect(Circuit circuit, PinRef a, PinRef b) {
        if (a.equals(b) || a.isOutput() == b.isOutput()) {
            return false;
        }
        PinRef output = a.isOutput() ? a : b;
        PinRef input = a.isInput() ? a : b;
        if (output.componentId() == input.componentId()) {
            return false;
        }
        if (!validPin(circuit, output) || !validPin(circuit, input)) {
            return false;
        }
        return !circuit.wires().contains(new Wire(output, input));
    }

    private boolean validPin(Circuit circuit, PinRef ref) {
        Optional<CircuitComponent> component = circuit.componentById(ref.componentId());
        if (component.isEmpty()) {
            return false;
        }
        int count = ref.isOutput() ? component.get().kind().outputCount() : component.get().kind().inputCount();
        return ref.index() >= 0 && ref.index() < count;
    }
}
