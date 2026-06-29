package dev.alexis.logika;

import dev.alexis.logika.engine.LogikaEngine;

/**
 * Application entry point.
 */
public final class App {
    private App() {
    }

    public static void main(String[] args) {
        new LogikaEngine().run();
    }
}
