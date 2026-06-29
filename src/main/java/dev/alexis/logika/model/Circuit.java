package dev.alexis.logika.model;

import dev.alexis.logika.util.Vec2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Circuit {
    private final List<CircuitComponent> components = new ArrayList<>();
    private final List<Wire> wires = new ArrayList<>();
    private int nextComponentId = 1;

    public CircuitComponent addComponent(ComponentKind kind, Vec2 snappedCenter) {
        CircuitComponent component = new CircuitComponent(nextComponentId++, kind,
                snappedCenter.x() - kind.width() / 2.0,
                snappedCenter.y() - kind.height() / 2.0);
        components.add(component);
        return component;
    }

    public List<CircuitComponent> components() {
        return Collections.unmodifiableList(components);
    }

    public List<Wire> wires() {
        return Collections.unmodifiableList(wires);
    }

    public Optional<CircuitComponent> componentById(int id) {
        return components.stream().filter(component -> component.id() == id).findFirst();
    }

    public Optional<CircuitComponent> findComponent(Vec2 world) {
        for (int i = components.size() - 1; i >= 0; i--) {
            CircuitComponent component = components.get(i);
            if (component.contains(world)) {
                return Optional.of(component);
            }
        }
        return Optional.empty();
    }

    public Optional<PinEndpoint> findPin(Vec2 world, double radiusWorld) {
        PinEndpoint closest = null;
        double best = Double.MAX_VALUE;
        for (int i = components.size() - 1; i >= 0; i--) {
            for (PinEndpoint pin : components.get(i).pins()) {
                double distance = pin.worldPosition().distanceTo(world);
                if (distance <= radiusWorld && distance < best) {
                    best = distance;
                    closest = pin;
                }
            }
        }
        return Optional.ofNullable(closest);
    }

    public ConnectResult connect(PinRef start, PinRef end) {
        if (!start.isOutput()) {
            return ConnectResult.fail("Start from output.");
        }
        if (!end.isInput()) {
            return ConnectResult.fail("End on input.");
        }
        if (start.componentId() == end.componentId()) {
            return ConnectResult.fail("Same component is not supported in V1.");
        }

        Optional<CircuitComponent> source = componentById(start.componentId());
        Optional<CircuitComponent> target = componentById(end.componentId());
        if (source.isEmpty() || target.isEmpty()) {
            return ConnectResult.fail("Pin disappeared.");
        }
        if (start.index() >= source.get().kind().outputCount()) {
            return ConnectResult.fail("Invalid output.");
        }
        if (end.index() >= target.get().kind().inputCount()) {
            return ConnectResult.fail("Invalid input.");
        }

        Wire candidate = new Wire(start, end);
        if (wires.contains(candidate)) {
            return ConnectResult.fail("Already linked.");
        }
        wires.removeIf(existing -> existing.to().equals(end));
        wires.add(candidate);
        return ConnectResult.ok("Connected.");
    }

    public Optional<Vec2> pinPosition(PinRef ref) {
        return componentById(ref.componentId()).flatMap(component -> component.pins().stream()
                .filter(pin -> pin.ref().equals(ref))
                .map(PinEndpoint::worldPosition)
                .findFirst());
    }

    public boolean pinValue(PinRef ref) {
        return componentById(ref.componentId())
                .map(component -> ref.isOutput() ? component.output() : component.input(ref.index()))
                .orElse(false);
    }

    public void removeComponent(int id) {
        components.removeIf(component -> component.id() == id);
        wires.removeIf(item -> item.touchesComponent(id));
    }

    public void removeComponents(Collection<Integer> ids) {
        Set<Integer> idSet = new HashSet<>(ids);
        components.removeIf(component -> idSet.contains(component.id()));
        wires.removeIf(item -> idSet.contains(item.from().componentId()) || idSet.contains(item.to().componentId()));
    }

    public void clear() {
        components.clear();
        wires.clear();
        nextComponentId = 1;
    }

    public record ConnectResult(boolean success, String message) {
        public static ConnectResult ok(String message) {
            return new ConnectResult(true, message);
        }

        public static ConnectResult fail(String message) {
            return new ConnectResult(false, message);
        }
    }
}
