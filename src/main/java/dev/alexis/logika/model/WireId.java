package dev.alexis.logika.model;

import java.util.Objects;

public record WireId(PinRef from, PinRef to) {
    public WireId {
        from = Objects.requireNonNull(from, "from");
        to = Objects.requireNonNull(to, "to");
    }
}
