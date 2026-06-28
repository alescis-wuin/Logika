package dev.alexis.logika.model;

public enum ComponentKind {
    BUTTON("Button", "Momentary source", 96.0, 58.0),
    SWITCH("Switch", "Toggle source", 108.0, 58.0),
    NAND("NAND", "!(A && B)", 104.0, 72.0);

    private final String label;
    private final String description;
    private final double width;
    private final double height;

    ComponentKind(String label, String description, double width, double height) {
        this.label = label;
        this.description = description;
        this.width = width;
        this.height = height;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public boolean isSource() {
        return this == BUTTON || this == SWITCH;
    }

    public int inputCount() {
        return this == NAND ? 2 : 0;
    }

    public int outputCount() {
        return 1;
    }
}
