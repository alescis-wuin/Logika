package dev.alexis.logika.ui;

import dev.alexis.logika.model.ComponentKind;
import dev.alexis.logika.util.Rect;

public record PlacementPreview(ComponentKind kind, Rect bounds, String alignmentMode, boolean slot, boolean hovered) {
    public PlacementPreview(ComponentKind kind, Rect bounds, String alignmentMode) {
        this(kind, bounds, alignmentMode, false, false);
    }
}
