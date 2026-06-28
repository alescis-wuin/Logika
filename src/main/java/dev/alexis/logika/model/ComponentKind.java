package dev.alexis.logika.model;

public enum ComponentKind {
    BUTTON("Button", "Hold source", 224.0, 152.0),
    SWITCH("Switch", "Toggle source", 232.0, 152.0),
    NAND("NAND", "!(A && B)", 280.0, 176.0),
    LED("LED", "Input indicator", 214.0, 152.0);

    private static final double MIN_WIDTH = 280.0;

    private final String label;
    private final String description;
    private final double naturalWidth;
    private final double height;

    ComponentKind(String label, String description, double naturalWidth, double height) {
        this.label = label;
        this.description = description;
        this.naturalWidth = naturalWidth;
        this.height = height;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public double width() {
        return Math.max(naturalWidth, MIN_WIDTH);
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
