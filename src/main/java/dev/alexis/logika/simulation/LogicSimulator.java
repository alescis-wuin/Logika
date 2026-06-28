package dev.alexis.logika.simulation;

import dev.alexis.logika.model.Circuit;
import dev.alexis.logika.model.CircuitComponent;
import dev.alexis.logika.model.ComponentKind;

public final class LogicSimulator {
    public void tick(Circuit circuit) {
        if (circuit.components().isEmpty()) {
            return;
        }

        initializeOutputs(circuit);
        int iterations = Math.max(4, circuit.components().size() + circuit.wires().size() + 1);

        for (int i = 0; i < iterations; i++) {
            circuit.components().forEach(CircuitComponent::clearInputs);

            for (var wire : circuit.wires()) {
                boolean value = circuit.pinValue(wire.from());
                circuit.componentById(wire.to().componentId())
                        .ifPresent(component -> component.setInput(wire.to().index(), value));
            }

            boolean changed = false;
            for (CircuitComponent component : circuit.components()) {
                changed |= component.setOutput(computeOutput(component));
            }

            if (!changed) {
                break;
            }
        }
    }

    private static void initializeOutputs(Circuit circuit) {
        for (CircuitComponent component : circuit.components()) {
            if (component.kind().isSource()) {
                component.setOutput(component.sourceActive());
            }
        }
    }

    private static boolean computeOutput(CircuitComponent component) {
        if (component.kind() == ComponentKind.NAND) {
            return !(component.input(0) && component.input(1));
        }
        return component.sourceActive();
    }
}
