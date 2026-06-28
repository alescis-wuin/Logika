package dev.alexis.logika.model;

public enum ComponentKind {
    BUTTON("Button", "Hold source", 132.0, 82.0),
    SWITCH("Switch", "Toggle source", 150.0, 82.0),
    NAND("NAND", "!(A && B)", 152.0, 98.0),
    LED("LED", "Input indicator", 116.0, 82.0);

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
        return switch (this) {
            case NAND -> 2;
            case LED -> 1;
            default -> 0;
        };
    }

    public int outputCount() {
        return this == LED ? 0 : 1;
    }
}
