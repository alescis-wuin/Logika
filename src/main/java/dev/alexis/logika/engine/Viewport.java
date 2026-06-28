package dev.alexis.logika.engine;

public final class Viewport {
    private int windowWidth;
    private int windowHeight;
    private int framebufferWidth;
    private int framebufferHeight;

    public Viewport(int windowWidth, int windowHeight, int framebufferWidth, int framebufferHeight) {
        setWindowSize(windowWidth, windowHeight);
        setFramebufferSize(framebufferWidth, framebufferHeight);
    }

    public void setWindowSize(int width, int height) {
        this.windowWidth = Math.max(1, width);
        this.windowHeight = Math.max(1, height);
    }

    public void setFramebufferSize(int width, int height) {
        this.framebufferWidth = Math.max(1, width);
        this.framebufferHeight = Math.max(1, height);
    }

    public int windowWidth() {
        return windowWidth;
    }

    public int windowHeight() {
        return windowHeight;
    }

    public int framebufferWidth() {
        return framebufferWidth;
    }

    public int framebufferHeight() {
        return framebufferHeight;
    }

    public double devicePixelRatio() {
        return Math.max(1.0, framebufferWidth / (double) windowWidth);
    }
}
