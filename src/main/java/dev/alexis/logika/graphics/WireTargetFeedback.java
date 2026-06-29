package dev.alexis.logika.graphics;

import dev.alexis.logika.model.PinRef;

record WireTargetFeedback(PinRef pin, boolean compatible) {
    static WireTargetFeedback none() {
        return new WireTargetFeedback(null, true);
    }

    boolean active() {
        return pin != null;
    }

    boolean invalid() {
        return active() && !compatible;
    }
}
