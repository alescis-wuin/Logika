package dev.alexis.logika.ui;

/**
 * Shared screen-space metrics for the editor UI.
 *
 * <p>Keeping hit targets and visual affordances in one place prevents the input
 * layer and the renderer from drifting apart when component sizes evolve.</p>
 */
public final class UiMetrics {
    public static final double GRID_SIZE = 32.0;

    public static final double PIN_RADIUS_SCREEN = 18.0;
    public static final double PIN_HOVER_EXTRA_SCREEN = 4.5;
    public static final double PIN_RING_PADDING_SCREEN = 5.0;
    public static final double PIN_HIT_RADIUS_SCREEN = 38.0;

    public static final double COMPONENT_PADDING_SCREEN = 22.0;
    public static final double COMPONENT_RADIUS_SCREEN = 28.0;
    public static final double SIGNAL_BADGE_WIDTH_SCREEN = 68.0;
    public static final double SIGNAL_BADGE_HEIGHT_SCREEN = 60.0;
    public static final double SIGNAL_BADGE_NODE_GAP_SCREEN = 24.0;

    public static final double TRASH_BUTTON_SIZE_SCREEN = 52.0;
    public static final double TRASH_BUTTON_MARGIN_SCREEN = 12.0;
    public static final double TRASH_CONTENT_GAP_SCREEN = 20.0;

    public static final double TOOLBAR_PANEL_HEIGHT_SCREEN = 118.0;
    public static final double TOOLBAR_PANEL_MARGIN_SCREEN = 14.0;
    public static final double TOOLBAR_ITEM_HEIGHT_SCREEN = 76.0;
    public static final double TOOLBAR_ITEM_GAP_SCREEN = 14.0;
    public static final double TOOLBAR_ITEM_TOP_PADDING_SCREEN = 22.0;

    private UiMetrics() {
    }
}
