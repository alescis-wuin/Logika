package dev.alexis.logika.ui;

import dev.alexis.logika.util.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Toolbar {
    public enum Action {
        SELECT,
        WIRE,
        BUTTON,
        SWITCH,
        NAND,
        SIMULATION,
        CLEAR
    }

    public List<Item> layout(int windowWidth, int windowHeight) {
        double y = windowHeight - 76.0;
        double x = 24.0;
        double height = 52.0;
        double gap = 10.0;

        List<Item> items = new ArrayList<>();
        x = add(items, Action.SELECT, "Select", "inspect", x, y, 94.0, height, gap);
        x = add(items, Action.WIRE, "Wire", "pins", x, y, 86.0, height, gap);
        x = add(items, Action.BUTTON, "Button", "momentary", x, y, 112.0, height, gap);
        x = add(items, Action.SWITCH, "Switch", "toggle", x, y, 120.0, height, gap);
        x = add(items, Action.NAND, "NAND", "gate", x, y, 94.0, height, gap);
        x = add(items, Action.SIMULATION, "Sim", "pause", x, y, 96.0, height, gap);
        add(items, Action.CLEAR, "Clear", "reset", x, y, 96.0, height, gap);

        return items;
    }

    public Optional<Action> actionAt(double mouseX, double mouseY, int windowWidth, int windowHeight) {
        return layout(windowWidth, windowHeight).stream()
                .filter(item -> item.rect().contains(mouseX, mouseY))
                .map(Item::action)
                .findFirst();
    }

    public boolean contains(double mouseY, int windowHeight) {
        return mouseY >= windowHeight - 92.0;
    }

    private static double add(List<Item> items, Action action, String label, String hint,
                              double x, double y, double width, double height, double gap) {
        items.add(new Item(action, label, hint, new Rect(x, y, width, height)));
        return x + width + gap;
    }

    public record Item(Action action, String label, String hint, Rect rect) {
    }
}
