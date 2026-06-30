package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.util.Rect;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;

final class TextInputOverlay {
    private static final double PANEL_X = 20.0;
    private static final double PANEL_Y = 142.0;
    private static final double PANEL_HEIGHT = 72.0;
    private static final double PANEL_MIN_WIDTH = 360.0;
    private static final double PANEL_MAX_WIDTH = 690.0;
    private static final double TEXT_LEFT_PADDING = 22.0;
    private static final double TEXT_RIGHT_RESERVED = 136.0;
    private static final double MAIN_TEXT_SIZE = 17.0;
    private static final int MAX_CODE_POINTS = 96;
    private static final String PLACEHOLDER = "Nom du composant / label / texte";

    private Rect bounds = new Rect(PANEL_X, PANEL_Y, PANEL_MAX_WIDTH, PANEL_HEIGHT);
    private String committed = "";
    private String draft = "";
    private String helper = "Cliquez pour saisir un nom, label ou texte.";
    private int caret;
    private boolean focused;
    private boolean hovered;
    private boolean allSelected;

    void update(Viewport viewport, double mouseX, double mouseY) {
        bounds = boundsFor(viewport);
        hovered = bounds.contains(mouseX, mouseY);
    }

    boolean hovered() {
        return hovered;
    }

    boolean handleMousePress(double mouseX, double mouseY) {
        if (bounds.contains(mouseX, mouseY)) {
            focus();
            moveCaretFromMouse(mouseX);
            return true;
        }
        if (focused) {
            commit();
        }
        return false;
    }

    boolean handleCodepoint(int codepoint) {
        if (!focused) {
            return false;
        }
        if (!isEditableCodePoint(codepoint)) {
            return true;
        }
        insertText(new String(Character.toChars(codepoint)));
        helper = "Saisie active. Entrée valide, Échap annule.";
        return true;
    }

    boolean handleKey(long window, int key, int action, int mods) {
        if (!focused) {
            return false;
        }
        if (isModifierKey(key)) {
            return false;
        }
        if (action != GLFW_PRESS && action != GLFW_REPEAT) {
            return false;
        }

        boolean shortcut = (mods & (GLFW_MOD_CONTROL | GLFW_MOD_SUPER)) != 0;
        if (shortcut && handleShortcut(window, key)) {
            return true;
        }

        switch (key) {
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> commit();
            case GLFW_KEY_ESCAPE -> cancel();
            case GLFW_KEY_BACKSPACE -> backspace();
            case GLFW_KEY_DELETE -> deleteForward();
            case GLFW_KEY_LEFT -> moveCaretLeft();
            case GLFW_KEY_RIGHT -> moveCaretRight();
            case GLFW_KEY_HOME -> moveCaretHome();
            case GLFW_KEY_END -> moveCaretEnd();
            default -> helper = "Saisie active. Entrée valide, Échap annule.";
        }
        return true;
    }

    void draw(NvgCanvas canvas, Viewport viewport, double timeSeconds) {
        bounds = boundsFor(viewport);
        RenderTheme.Rgba panel = focused ? new RenderTheme.Rgba(18, 32, 56, 250) : new RenderTheme.Rgba(14, 21, 36, 236);
        RenderTheme.Rgba stroke = focused ? RenderTheme.ACCENT : hovered ? RenderTheme.TEXT_MUTED.withAlpha(190) : RenderTheme.PANEL_STROKE;
        canvas.fillRound(bounds.x() + 4.0, bounds.y() + 8.0, bounds.width(), bounds.height(), 20.0, new RenderTheme.Rgba(0, 0, 0, 100));
        canvas.fillRound(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 20.0, panel);
        canvas.strokeRound(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 20.0, stroke, focused ? 2.3f : 1.3f);

        canvas.text("Texte", (float) (bounds.x() + TEXT_LEFT_PADDING), (float) (bounds.y() + 17.0), 12.5f,
                NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED.withAlpha(215), true);
        canvas.text(focused ? "Entrée" : "Cliquez", (float) (bounds.x() + bounds.width() - 64.0),
                (float) (bounds.y() + 22.0), 12.5f, NVG_ALIGN_CENTER, focused ? RenderTheme.ACCENT : RenderTheme.TEXT_MUTED, true);

        String visible = visibleText();
        RenderTheme.Rgba textColor = draft.isEmpty() && !focused ? RenderTheme.TEXT_MUTED.withAlpha(148) : RenderTheme.TEXT;
        if (focused && allSelected && !draft.isEmpty()) {
            double highlightWidth = Math.min(estimatedTextWidth(visible, MAIN_TEXT_SIZE) + 10.0, editableWidth());
            canvas.fillRound(bounds.x() + TEXT_LEFT_PADDING - 5.0, bounds.y() + 28.0, highlightWidth, 27.0,
                    8.0, RenderTheme.ACCENT.withAlpha(68));
        }
        canvas.text(visible, (float) (bounds.x() + TEXT_LEFT_PADDING), (float) (bounds.y() + 42.0),
                (float) MAIN_TEXT_SIZE, NVG_ALIGN_LEFT, textColor, false);

        if (focused && !allSelected && caretVisible(timeSeconds)) {
            double caretX = caretX();
            canvas.line(caretX, bounds.y() + 28.0, caretX, bounds.y() + 55.0, RenderTheme.TEXT, 1.7f);
        }

        canvas.text(helper, (float) (bounds.x() + TEXT_LEFT_PADDING), (float) (bounds.y() + 62.0), 11.8f,
                NVG_ALIGN_LEFT, RenderTheme.TEXT_MUTED.withAlpha(188), false);
    }

    private Rect boundsFor(Viewport viewport) {
        double width = clamp(viewport.windowWidth() - 40.0, PANEL_MIN_WIDTH, PANEL_MAX_WIDTH);
        return new Rect(PANEL_X, PANEL_Y, width, PANEL_HEIGHT);
    }

    private void focus() {
        focused = true;
        helper = "Saisie active. Entrée valide, Échap annule.";
        if (caret < 0 || caret > draft.length()) {
            caret = draft.length();
        }
    }

    private void commit() {
        committed = draft.strip();
        draft = committed;
        caret = draft.length();
        focused = false;
        allSelected = false;
        helper = committed.isEmpty() ? "Texte vide validé." : "Texte validé : " + abbreviated(committed) + ".";
    }

    private void cancel() {
        draft = committed;
        caret = draft.length();
        focused = false;
        allSelected = false;
        helper = "Saisie annulée.";
    }

    private boolean handleShortcut(long window, int key) {
        switch (key) {
            case GLFW_KEY_A -> {
                allSelected = !draft.isEmpty();
                helper = allSelected ? "Texte sélectionné." : "Aucun texte à sélectionner.";
                return true;
            }
            case GLFW_KEY_C -> {
                if (!draft.isEmpty()) {
                    glfwSetClipboardString(window, draft);
                    helper = "Texte copié.";
                }
                return true;
            }
            case GLFW_KEY_X -> {
                if (!draft.isEmpty()) {
                    glfwSetClipboardString(window, draft);
                    draft = "";
                    caret = 0;
                    allSelected = false;
                    helper = "Texte coupé.";
                }
                return true;
            }
            case GLFW_KEY_V -> {
                String clipboard = glfwGetClipboardString(window);
                if (clipboard != null && !clipboard.isBlank()) {
                    insertText(clipboard);
                    helper = "Texte collé.";
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void insertText(String value) {
        String text = sanitize(value);
        if (text.isEmpty()) {
            return;
        }
        if (allSelected) {
            draft = "";
            caret = 0;
            allSelected = false;
        }
        String before = draft.substring(0, caret);
        String after = draft.substring(caret);
        draft = trimToCodePointLimit(before + text + after);
        caret = Math.min((before + text).length(), draft.length());
    }

    private void backspace() {
        if (allSelected) {
            draft = "";
            caret = 0;
            allSelected = false;
            return;
        }
        if (caret <= 0) {
            return;
        }
        int previous = draft.offsetByCodePoints(caret, -1);
        draft = draft.substring(0, previous) + draft.substring(caret);
        caret = previous;
    }

    private void deleteForward() {
        if (allSelected) {
            draft = "";
            caret = 0;
            allSelected = false;
            return;
        }
        if (caret >= draft.length()) {
            return;
        }
        int next = draft.offsetByCodePoints(caret, 1);
        draft = draft.substring(0, caret) + draft.substring(next);
    }

    private void moveCaretLeft() {
        allSelected = false;
        if (caret > 0) {
            caret = draft.offsetByCodePoints(caret, -1);
        }
    }

    private void moveCaretRight() {
        allSelected = false;
        if (caret < draft.length()) {
            caret = draft.offsetByCodePoints(caret, 1);
        }
    }

    private void moveCaretHome() {
        allSelected = false;
        caret = 0;
    }

    private void moveCaretEnd() {
        allSelected = false;
        caret = draft.length();
    }

    private void moveCaretFromMouse(double mouseX) {
        allSelected = false;
        if (draft.isEmpty()) {
            caret = 0;
            return;
        }
        double textWidth = Math.max(1.0, estimatedTextWidth(draft, MAIN_TEXT_SIZE));
        double ratio = clamp((mouseX - bounds.x() - TEXT_LEFT_PADDING) / textWidth, 0.0, 1.0);
        int targetCodePoint = (int) Math.round(draft.codePointCount(0, draft.length()) * ratio);
        caret = draft.offsetByCodePoints(0, Math.min(targetCodePoint, draft.codePointCount(0, draft.length())));
    }

    private String visibleText() {
        String source = draft.isEmpty() ? PLACEHOLDER : draft;
        double width = editableWidth();
        if (estimatedTextWidth(source, MAIN_TEXT_SIZE) <= width) {
            return source;
        }
        String prefix = "…";
        StringBuilder builder = new StringBuilder();
        for (int i = source.length(); i > 0; ) {
            int previous = source.offsetByCodePoints(i, -1);
            String candidate = prefix + source.substring(previous) + builder;
            if (estimatedTextWidth(candidate, MAIN_TEXT_SIZE) > width) {
                break;
            }
            builder.insert(0, source.substring(previous, i));
            i = previous;
        }
        return prefix + builder;
    }

    private double caretX() {
        String beforeCaret = draft.substring(0, Math.min(caret, draft.length()));
        double x = bounds.x() + TEXT_LEFT_PADDING + estimatedTextWidth(beforeCaret, MAIN_TEXT_SIZE);
        return Math.min(x, bounds.x() + TEXT_LEFT_PADDING + editableWidth());
    }

    private double editableWidth() {
        return Math.max(80.0, bounds.width() - TEXT_LEFT_PADDING - TEXT_RIGHT_RESERVED);
    }

    private static boolean caretVisible(double timeSeconds) {
        return ((int) Math.floor(timeSeconds * 2.2)) % 2 == 0;
    }

    private static boolean isModifierKey(int key) {
        return key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL
                || key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT
                || key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT
                || key == GLFW_KEY_LEFT_SUPER || key == GLFW_KEY_RIGHT_SUPER;
    }

    private static boolean isEditableCodePoint(int codepoint) {
        return Character.isValidCodePoint(codepoint) && !Character.isISOControl(codepoint);
    }

    private static String sanitize(String value) {
        StringBuilder builder = new StringBuilder();
        value.codePoints()
                .filter(TextInputOverlay::isEditableCodePoint)
                .map(codepoint -> Character.isWhitespace(codepoint) ? ' ' : codepoint)
                .forEach(builder::appendCodePoint);
        return builder.toString().replaceAll(" {2,}", " ");
    }

    private static String trimToCodePointLimit(String value) {
        int count = value.codePointCount(0, value.length());
        if (count <= MAX_CODE_POINTS) {
            return value;
        }
        int end = value.offsetByCodePoints(0, MAX_CODE_POINTS);
        return value.substring(0, end);
    }

    private static String abbreviated(String value) {
        if (value.codePointCount(0, value.length()) <= 28) {
            return value;
        }
        int end = value.offsetByCodePoints(0, 28);
        return value.substring(0, end) + "…";
    }

    private static double estimatedTextWidth(String value, double size) {
        double width = 0.0;
        for (int i = 0; i < value.length(); ) {
            int codepoint = value.codePointAt(i);
            width += codepoint == ' ' ? size * 0.34 : Character.isUpperCase(codepoint) ? size * 0.66 : size * 0.54;
            i += Character.charCount(codepoint);
        }
        return width;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
