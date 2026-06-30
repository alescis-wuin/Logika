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

        WireId candidate = new WireId(start, end);
        if (hasWire(candidate)) {
            return ConnectResult.fail("Already linked.");
        }
        wires.removeIf(existing -> existing.to().equals(end));
        wires.add(new Wire(start, end));
        return ConnectResult.ok("Connected.");
    }

    public Optional<Wire> wireById(WireId id) {
        if (id == null) {
            return Optional.empty();
        }
        return wires.stream().filter(wire -> wire.sameEndpoints(id)).findFirst();
    }

    public boolean hasWire(PinRef from, PinRef to) {
        return hasWire(new WireId(from, to));
    }

    public boolean hasWire(WireId id) {
        return wireById(id).isPresent();
    }

    public boolean replaceWire(Wire wire) {
        int index = wireIndex(wire.id());
        if (index < 0) {
            return false;
        }
        wires.set(index, wire);
        return true;
    }

    public boolean removeWire(WireId id) {
        return wires.removeIf(wire -> wire.sameEndpoints(id));
    }

    public boolean setWireColor(WireId id, int colorRgb) {
        Optional<Wire> wire = wireById(id);
        return wire.isPresent() && replaceWire(wire.get().withColor(colorRgb));
    }

    public boolean configureWire(WireId id, int colorRgb, List<Vec2> controlPoints) {
        Optional<Wire> wire = wireById(id);
        return wire.isPresent() && replaceWire(new Wire(wire.get().from(), wire.get().to(), colorRgb, controlPoints));
    }

    public boolean addWireControlPoint(WireId id, int insertIndex, Vec2 point) {
        Optional<Wire> wire = wireById(id);
        return wire.isPresent() && replaceWire(wire.get().withInsertedControlPoint(insertIndex, point));
    }

    public boolean moveWireControlPoint(WireId id, int pointIndex, Vec2 point) {
        Optional<Wire> wire = wireById(id);
        return wire.isPresent() && replaceWire(wire.get().withMovedControlPoint(pointIndex, point));
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

    public Snapshot snapshot() {
        List<ComponentSnapshot> componentSnapshots = new ArrayList<>(components.size());
        for (CircuitComponent component : components) {
            componentSnapshots.add(new ComponentSnapshot(component.id(), component.kind(),
                    component.bounds().x(), component.bounds().y(), component.sourceActive()));
        }
        return new Snapshot(List.copyOf(componentSnapshots), List.copyOf(wires), nextComponentId);
    }

    public void restore(Snapshot snapshot) {
        components.clear();
        wires.clear();

        for (ComponentSnapshot state : snapshot.components()) {
            CircuitComponent component = new CircuitComponent(state.id(), state.kind(), state.x(), state.y());
            component.setSourceActive(state.sourceActive());
            components.add(component);
        }

        wires.addAll(snapshot.wires());
        nextComponentId = snapshot.nextComponentId();
    }

    public void clear() {
        components.clear();
        wires.clear();
        nextComponentId = 1;
    }

    private int wireIndex(WireId id) {
        for (int i = 0; i < wires.size(); i++) {
            if (wires.get(i).sameEndpoints(id)) {
                return i;
            }
        }
        return -1;
    }

    public record Snapshot(List<ComponentSnapshot> components, List<Wire> wires, int nextComponentId) {
    }

    public record ComponentSnapshot(int id, ComponentKind kind, double x, double y, boolean sourceActive) {
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
