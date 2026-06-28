package dev.alexis.logika.ui;

import dev.alexis.logika.model.ComponentKind;

import java.util.Optional;

public enum Tool {
    SELECT,
    WIRE,
    PLACE_BUTTON,
    PLACE_SWITCH,
    PLACE_NAND;

    public boolean isPlacement() {
        return this == PLACE_BUTTON || this == PLACE_SWITCH || this == PLACE_NAND;
    }

    public Optional<ComponentKind> componentKind() {
        return switch (this) {
            case PLACE_BUTTON -> Optional.of(ComponentKind.BUTTON);
            case PLACE_SWITCH -> Optional.of(ComponentKind.SWITCH);
            case PLACE_NAND -> Optional.of(ComponentKind.NAND);
            default -> Optional.empty();
        };
    }

    public String label() {
        return switch (this) {
            case SELECT -> "Select";
            case WIRE -> "Wire";
            case PLACE_BUTTON -> "Button";
            case PLACE_SWITCH -> "Switch";
            case PLACE_NAND -> "NAND";
        };
    }
}
