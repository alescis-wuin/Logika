package dev.alexis.logika.model;

/** Stable pin identifier. */
public record PinRef(int componentId, PinDirection direction, int index) {
    public boolean isOutput() {
        return direction == PinDirection.OUTPUT;
    }

    public boolean isInput() {
        return direction == PinDirection.INPUT;
    }
}
