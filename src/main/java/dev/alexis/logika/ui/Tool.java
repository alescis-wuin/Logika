package dev.alexis.logika.ui;

import dev.alexis.logika.model.ComponentKind;

import java.util.Optional;

public enum Tool {
    INTERACT,
    PLACE_BUTTON,
    PLACE_SWITCH,
    PLACE_NAND,
    PLACE_LED;

    public boolean isPlacement() {
        return this != INTERACT;
    }

    public Optional<ComponentKind> componentKind() {
        return switch (this) {
            case PLACE_BUTTON -> Optional.of(ComponentKind.BUTTON);
            case PLACE_SWITCH -> Optional.of(ComponentKind.SWITCH);
            case PLACE_NAND -> Optional.of(ComponentKind.NAND);
            case PLACE_LED -> Optional.of(ComponentKind.LED);
            case INTERACT -> Optional.empty();
        };
    }

    public String label() {
        return switch (this) {
            case INTERACT -> "Interact";
            case PLACE_BUTTON -> "Button";
            case PLACE_SWITCH -> "Switch";
            case PLACE_NAND -> "NAND";
            case PLACE_LED -> "LED";
        };
    }
}
