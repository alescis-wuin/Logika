package dev.alexis.logika.engine;

public final class InputState {
    private double mouseX;
    private double mouseY;
    private boolean leftDown;
    private boolean panDown;
    private boolean spaceDown;
    private boolean controlDown;
    private boolean altDown;
    private boolean shiftDown;
    private boolean panning;

    public double mouseX() {
        return mouseX;
    }

    public double mouseY() {
        return mouseY;
    }

    public void setMouse(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public boolean leftDown() {
        return leftDown;
    }

    public void setLeftDown(boolean leftDown) {
        this.leftDown = leftDown;
    }

    public boolean panDown() {
        return panDown;
    }

    public void setPanDown(boolean panDown) {
        this.panDown = panDown;
    }

    public boolean spaceDown() {
        return spaceDown;
    }

    public void setSpaceDown(boolean spaceDown) {
        this.spaceDown = spaceDown;
    }

    public boolean controlDown() {
        return controlDown;
    }

    public void setControlDown(boolean controlDown) {
        this.controlDown = controlDown;
    }

    public boolean altDown() {
        return altDown;
    }

    public void setAltDown(boolean altDown) {
        this.altDown = altDown;
    }

    public boolean shiftDown() {
        return shiftDown;
    }

    public void setShiftDown(boolean shiftDown) {
        this.shiftDown = shiftDown;
    }

    public boolean panning() {
        return panning;
    }

    public void setPanning(boolean panning) {
        this.panning = panning;
    }
}
