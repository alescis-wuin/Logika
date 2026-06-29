package dev.alexis.logika.model;

public record Wire(PinRef from, PinRef to) {
    public boolean touchesComponent(int componentId) {
        return from.componentId() == componentId || to.componentId() == componentId;
    }
}
