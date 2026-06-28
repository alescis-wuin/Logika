package dev.alexis.logika.model;

import dev.alexis.logika.util.Rect;
import dev.alexis.logika.util.Vec2;

import java.util.ArrayList;
import java.util.List;

public final class CircuitComponent {
    private final int id;
    private final ComponentKind kind;
    private final double x;
    private final double y;
    private boolean sourceActive;
    private boolean output;
    private final boolean[] inputs = new boolean[2];

    public CircuitComponent(int id, ComponentKind kind, double x, double y) {
        this.id = id;
        this.kind = kind;
        this.x = x;
        this.y = y;
    }

    public int id() {
        return id;
    }

    public ComponentKind kind() {
        return kind;
    }

    public Rect bounds() {
        return new Rect(x, y, kind.width(), kind.height());
    }

    public boolean contains(Vec2 world) {
        return bounds().contains(world);
    }

    public List<PinEndpoint> pins() {
        List<PinEndpoint> pins = new ArrayList<>(3);
        Rect b = bounds();

        if (kind == ComponentKind.NAND) {
            pins.add(new PinEndpoint(
                    new PinRef(id, PinDirection.INPUT, 0),
                    "A",
                    new Vec2(b.x(), b.y() + b.height() * 0.34)
            ));
            pins.add(new PinEndpoint(
                    new PinRef(id, PinDirection.INPUT, 1),
                    "B",
                    new Vec2(b.x(), b.y() + b.height() * 0.66)
            ));
            pins.add(new PinEndpoint(
                    new PinRef(id, PinDirection.OUTPUT, 0),
                    "Y",
                    new Vec2(b.x() + b.width(), b.y() + b.height() * 0.5)
            ));
        } else {
            pins.add(new PinEndpoint(
                    new PinRef(id, PinDirection.OUTPUT, 0),
                    "Y",
                    new Vec2(b.x() + b.width(), b.y() + b.height() * 0.5)
            ));
        }

        return pins;
    }

    public boolean sourceActive() {
        return sourceActive;
    }

    public void setSourceActive(boolean sourceActive) {
        this.sourceActive = sourceActive;
    }

    public boolean output() {
        return output;
    }

    public boolean setOutput(boolean output) {
        boolean changed = this.output != output;
        this.output = output;
        return changed;
    }

    public boolean input(int index) {
        if (index < 0 || index >= inputs.length) {
            return false;
        }
        return inputs[index];
    }

    public void setInput(int index, boolean value) {
        if (index >= 0 && index < inputs.length) {
            inputs[index] = value;
        }
    }

    public void clearInputs() {
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = false;
        }
    }

    public String valueLabel() {
        if (kind == ComponentKind.NAND) {
            return "A=" + bit(inputs[0]) + "  B=" + bit(inputs[1]) + "  Y=" + bit(output);
        }
        return "Y=" + bit(output);
    }

    private static int bit(boolean value) {
        return value ? 1 : 0;
    }
}
