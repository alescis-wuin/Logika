package dev.alexis.logika.model;

import dev.alexis.logika.util.Vec2;

/** A pin reference with its current world position. */
public record PinEndpoint(PinRef ref, String label, Vec2 worldPosition) {
}
