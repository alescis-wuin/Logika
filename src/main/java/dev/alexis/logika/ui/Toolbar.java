package dev.alexis.logika.ui;

import dev.alexis.logika.util.Rect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Toolbar {
    public enum Action {
        BUTTON,
        SWITCH,
        NAND,
        LED,
        SIMULATION,
        CLEAR
    }

    public List<Item> layout(int windowWidth, int windowHeight) {
        double y = windowHeight - 88.0;
        double x = 24.0;
        double height = 62.0;
        double gap = 12.0;

        List<Item> items = new ArrayList<>();
        x = add(items, Action.BUTTON, "Button", "hold = 1", x, y, 136.0, height, gap);
        x = add(items, Action.SWITCH, "Switch", "click = toggle", x, y, 150.0, height, gap);
        x = add(items, Action.NAND, "NAND", "2 inputs", x, y, 122.0, height, gap);
        x = add(items, Action.LED, "LED", "input light", x, y, 118.0, height, gap);
        x = add(items, Action.SIMULATION, "Sim", "pause / run", x, y, 128.0, height, gap);
        add(items, Action.CLEAR, "Clear", "reset grid", x, y, 124.0, height, gap);

        return items;
    }

    public Optional<Action> actionAt(double mouseX, double mouseY, int windowWidth, int windowHeight) {
        return layout(windowWidth, windowHeight).stream()
                .filter(item -> item.rect().contains(mouseX, mouseY))
                .map(Item::action)
                .findFirst();
    }

    public boolean contains(double mouseY, int windowHeight) {
        return mouseY >= windowHeight - 110.0;
    }

    private static double add(List<Item> items, Action action, String label, String hint,
                              double x, double y, double width, double height, double gap) {
        items.add(new Item(action, label, hint, new Rect(x, y, width, height)));
        return x + width + gap;
    }

    public record Item(Action action, String label, String hint, Rect rect) {
    }
}
