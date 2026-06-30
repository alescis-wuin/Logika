package dev.alexis.logika.graphics;

import dev.alexis.logika.engine.Viewport;
import dev.alexis.logika.util.Rect;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_CENTER;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_LEFT;

final class TextInputOverlay {
    private static final boolean SHOW_TITLE = Boolean.parseBoolean(System.getProperty("logika.textInput.showTitle", "false"));
    private static final double PANEL_X = 20.0;
    private static final double PANEL_Y = 142.0;
    private static final double PANEL_MIN_WIDTH = 420.0;
    private static final double PANEL_MAX_WIDTH = 760.0;
    private static final double PANEL_PADDING = 14.0;
    private static final double FIELD_HEIGHT = 50.0;
    private static final double FIELD_TEXT_PADDING = 16.0;
    private static final double MESSAGE_SECONDS = 2.8;
    private static final float TITLE_TEXT_SIZE = 16.5f;
    private static final float MAIN_TEXT_SIZE = 22.0f;
    private static final float MESSAGE_TEXT_SIZE = 13.5f;
    private static final String PLACEHOLDER = "Nom du composant / label / texte";

    private static final RenderTheme.Rgba PANEL_FILL = new RenderTheme.Rgba(29, 41, 61, 250);
    private static final RenderTheme.Rgba FIELD_FILL = new RenderTheme.Rgba(240, 246, 255, 255);
    private static final RenderTheme.Rgba FIELD_FILL_FOCUSED = new RenderTheme.Rgba(250, 253, 255, 255);
    private static final RenderTheme.Rgba FIELD_TEXT = new RenderTheme.Rgba(12, 18, 30, 255);
    private static final RenderTheme.Rgba FIELD_PLACEHOLDER = new RenderTheme.Rgba(69, 85, 112, 210);
    private static final RenderTheme.Rgba FIELD_BORDER = new RenderTheme.Rgba(202, 218, 246, 255);
    private static final RenderTheme.Rgba SELECTION_FILL = new RenderTheme.Rgba(34, 110, 220, 218);
    private static final RenderTheme.Rgba SELECTION_TEXT = new RenderTheme.Rgba(255, 255, 255, 255);
    private static final RenderTheme.Rgba ACTIVE_MESSAGE = new RenderTheme.Rgba(140, 199, 255, 255);
    private static final RenderTheme.Rgba COMMITTED_MESSAGE = new RenderTheme.Rgba(67, 232, 143, 255);
    private static final RenderTheme.Rgba CANCELLED_MESSAGE = new RenderTheme.Rgba(255, 210, 102, 255);
    private static final RenderTheme.Rgba HISTORY_MESSAGE = new RenderTheme.Rgba(222, 228, 255, 255);

    private final TextEditBuffer buffer = new TextEditBuffer();
    private Rect bounds = new Rect(PANEL_X, PANEL_Y, PANEL_MAX_WIDTH, panelHeight());
    private Rect fieldBounds = fieldBoundsFor(bounds);
    private String message = "";
    private MessageKind messageKind = MessageKind.ACTIVE;
    private double messageVisibleUntil;
    private boolean focused;
    private boolean hovered;
    private boolean draggingSelection;
    private int lastVisibleStart;
    private int lastVisibleEnd;
    private int[] lastCaretOffsets = {0};
    private double[] lastCaretPositions = {PANEL_X + PANEL_PADDING + FIELD_TEXT_PADDING};

    void update(Viewport viewport, double mouseX, double mouseY) {
        bounds = boundsFor(viewport);
        fieldBounds = fieldBoundsFor(bounds);
        hovered = fieldBounds.contains(mouseX, mouseY);
    }

    boolean hovered() {
        return hovered;
    }

    boolean handleMousePress(double mouseX, double mouseY, boolean shiftDown) {
        if (fieldBounds.contains(mouseX, mouseY)) {
            focus();
            buffer.beginMouseSelection(caretAt(mouseX), shiftDown);
            draggingSelection = true;
            showMessage(MessageKind.ACTIVE, "Saisie active");
            return true;
        }
        if (focused) {
            commit();
        }
        return false;
    }

    boolean handleMouseDrag(double mouseX, double mouseY) {
        if (!draggingSelection) {
            return false;
        }
        buffer.setCaret(caretAt(mouseX), true);
        showMessage(MessageKind.ACTIVE, "Saisie active");
        return true;
    }

    boolean handleMouseRelease() {
        if (!draggingSelection) {
            return false;
        }
        draggingSelection = false;
        buffer.endMouseSelection();
        return true;
    }

    boolean handleCodepoint(int codepoint) {
        if (!focused) {
            return false;
        }
        if (Character.isValidCodePoint(codepoint) && !Character.isISOControl(codepoint)) {
            buffer.insert(new String(Character.toChars(codepoint)));
        }
        showMessage(MessageKind.ACTIVE, "Saisie active");
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
        boolean selecting = (mods & GLFW_MOD_SHIFT) != 0;
        switch (key) {
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> commit();
            case GLFW_KEY_ESCAPE -> cancel();
            case GLFW_KEY_BACKSPACE -> buffer.backspace();
            case GLFW_KEY_DELETE -> buffer.deleteForward();
            case GLFW_KEY_LEFT -> buffer.moveLeft(selecting);
            case GLFW_KEY_RIGHT -> buffer.moveRight(selecting);
            case GLFW_KEY_HOME -> buffer.moveHome(selecting);
            case GLFW_KEY_END -> buffer.moveEnd(selecting);
            default -> { }
        }
        if (key != GLFW_KEY_ENTER && key != GLFW_KEY_KP_ENTER && key != GLFW_KEY_ESCAPE) {
            showMessage(MessageKind.ACTIVE, "Saisie active");
        }
        return true;
    }

    void draw(NvgCanvas canvas, Viewport viewport, double timeSeconds) {
        bounds = boundsFor(viewport);
        fieldBounds = fieldBoundsFor(bounds);
        VisibleText visible = visibleText(canvas);
        cacheVisibleText(visible);

        canvas.fillRound(bounds.x() + 5.0, bounds.y() + 9.0, bounds.width(), bounds.height(), 24.0, new RenderTheme.Rgba(0, 0, 0, 112));
        canvas.fillRound(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 24.0, PANEL_FILL);
        canvas.strokeRound(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 24.0,
                focused ? RenderTheme.ACCENT.withAlpha(230) : RenderTheme.PANEL_STROKE.withAlpha(140), focused ? 2.2f : 1.2f);

        if (SHOW_TITLE) {
            canvas.text("Texte", (float) (bounds.x() + PANEL_PADDING + 2.0), (float) (bounds.y() + 17.5),
                    TITLE_TEXT_SIZE, NVG_ALIGN_LEFT, RenderTheme.TEXT, true);
        }

        canvas.fillRound(fieldBounds.x(), fieldBounds.y(), fieldBounds.width(), fieldBounds.height(), 17.0,
                focused ? FIELD_FILL_FOCUSED : FIELD_FILL);
        canvas.strokeRound(fieldBounds.x(), fieldBounds.y(), fieldBounds.width(), fieldBounds.height(), 17.0,
                focused ? RenderTheme.ACCENT : hovered ? new RenderTheme.Rgba(132, 177, 238, 255) : FIELD_BORDER,
                focused ? 2.5f : 1.5f);
        canvas.line(fieldBounds.x() + 10.0, fieldBounds.y() + 10.0, fieldBounds.x() + 10.0,
                fieldBounds.y() + fieldBounds.height() - 10.0, focused ? RenderTheme.ACCENT : new RenderTheme.Rgba(142, 164, 202, 230), 2.0f);
        canvas.text("Aa", (float) (fieldBounds.x() + fieldBounds.width() - 27.0), (float) fieldBounds.centerY(),
                17.0f, NVG_ALIGN_CENTER, new RenderTheme.Rgba(79, 94, 120, focused ? 230 : 170), true);

        drawSelection(canvas, visible);
        canvas.text(visible.value(), (float) textOriginX(), (float) fieldBounds.centerY(), MAIN_TEXT_SIZE,
                NVG_ALIGN_LEFT, buffer.empty() ? FIELD_PLACEHOLDER : FIELD_TEXT, false);
        drawSelectedText(canvas, visible);
        if (focused && !buffer.hasSelection() && caretVisible(timeSeconds)) {
            double x = xForOffset(canvas, visible, buffer.caret());
            canvas.line(x, fieldBounds.y() + 9.0, x, fieldBounds.y() + fieldBounds.height() - 9.0, FIELD_TEXT, 1.8f);
        }
        drawMessage(canvas, timeSeconds);
    }

    private boolean handleShortcut(long window, int key) {
        switch (key) {
            case GLFW_KEY_A -> {
                buffer.selectAll();
                showMessage(MessageKind.ACTIVE, "Saisie active");
                return true;
            }
            case GLFW_KEY_C -> {
                String value = buffer.copyValue();
                if (!value.isEmpty()) {
                    glfwSetClipboardString(window, value);
                }
                showMessage(MessageKind.ACTIVE, "Saisie active");
                return true;
            }
            case GLFW_KEY_X -> {
                String value = buffer.cutValue();
                if (!value.isEmpty()) {
                    glfwSetClipboardString(window, value);
                }
                showMessage(MessageKind.ACTIVE, "Saisie active");
                return true;
            }
            case GLFW_KEY_V -> {
                String value = glfwGetClipboardString(window);
                if (value != null && !value.isEmpty()) {
                    buffer.insert(value);
                }
                showMessage(MessageKind.ACTIVE, "Saisie active");
                return true;
            }
            case GLFW_KEY_Z -> {
                showMessage(MessageKind.HISTORY, buffer.undo() ? "Retour arrière texte" : "Aucun retour arrière texte");
                return true;
            }
            case GLFW_KEY_W, GLFW_KEY_Y -> {
                showMessage(MessageKind.HISTORY, buffer.redo() ? "Rétablissement texte" : "Aucun rétablissement texte");
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void focus() {
        focused = true;
        showMessage(MessageKind.ACTIVE, "Saisie active");
    }

    private void commit() {
        String value = buffer.commit();
        focused = false;
        draggingSelection = false;
        showMessage(MessageKind.COMMITTED, "Texte validé : [" + abbreviated(value) + "]");
    }

    private void cancel() {
        buffer.cancel();
        focused = false;
        draggingSelection = false;
        showMessage(MessageKind.CANCELLED, "Saisie annulée");
    }

    private VisibleText visibleText(NvgCanvas canvas) {
        if (buffer.empty()) {
            return new VisibleText(PLACEHOLDER, 0, 0, new int[]{0}, new double[]{textOriginX()});
        }
        String draft = buffer.draft();
        int start = visibleStart(canvas, draft);
        int end = visibleEnd(canvas, draft, start);
        int count = draft.codePointCount(start, end);
        int[] offsets = new int[count + 1];
        double[] positions = new double[count + 1];
        offsets[0] = start;
        positions[0] = textOriginX();
        int index = start;
        for (int i = 1; i <= count; i++) {
            index = draft.offsetByCodePoints(index, 1);
            offsets[i] = index;
            positions[i] = textOriginX() + canvas.textWidth(draft.substring(start, index), MAIN_TEXT_SIZE, false);
        }
        return new VisibleText(draft.substring(start, end), start, end, offsets, positions);
    }

    private int visibleStart(NvgCanvas canvas, String draft) {
        if (canvas.textWidth(draft, MAIN_TEXT_SIZE, false) <= editableWidth()) {
            return 0;
        }
        int start = buffer.caret();
        while (start > 0) {
            int previous = draft.offsetByCodePoints(start, -1);
            if (canvas.textWidth(draft.substring(previous, buffer.caret()), MAIN_TEXT_SIZE, false) > editableWidth() - 8.0) {
                break;
            }
            start = previous;
        }
        return start;
    }

    private int visibleEnd(NvgCanvas canvas, String draft, int start) {
        int end = start;
        int index = start;
        while (index < draft.length()) {
            int next = draft.offsetByCodePoints(index, 1);
            if (end > start && canvas.textWidth(draft.substring(start, next), MAIN_TEXT_SIZE, false) > editableWidth()) {
                break;
            }
            end = next;
            index = next;
        }
        return end;
    }

    private void cacheVisibleText(VisibleText visible) {
        lastVisibleStart = visible.start();
        lastVisibleEnd = visible.end();
        lastCaretOffsets = visible.caretOffsets();
        lastCaretPositions = visible.caretPositions();
    }

    private void drawSelection(NvgCanvas canvas, VisibleText visible) {
        if (!buffer.hasSelection() || buffer.empty()) {
            return;
        }
        int start = Math.max(buffer.selectionStart(), visible.start());
        int end = Math.min(buffer.selectionEnd(), visible.end());
        if (start >= end) {
            return;
        }
        double x1 = xForOffset(canvas, visible, start);
        double x2 = xForOffset(canvas, visible, end);
        canvas.fillRound(x1 - 2.0, fieldBounds.y() + 7.0, Math.max(4.0, x2 - x1 + 4.0),
                fieldBounds.height() - 14.0, 8.0, SELECTION_FILL);
    }

    private void drawSelectedText(NvgCanvas canvas, VisibleText visible) {
        if (!buffer.hasSelection() || buffer.empty()) {
            return;
        }
        int start = Math.max(buffer.selectionStart(), visible.start());
        int end = Math.min(buffer.selectionEnd(), visible.end());
        if (start >= end) {
            return;
        }
        canvas.text(buffer.draft().substring(start, end), (float) xForOffset(canvas, visible, start),
                (float) fieldBounds.centerY(), MAIN_TEXT_SIZE, NVG_ALIGN_LEFT, SELECTION_TEXT, false);
    }

    private double xForOffset(NvgCanvas canvas, VisibleText visible, int offset) {
        if (buffer.empty() || offset <= visible.start()) {
            return textOriginX();
        }
        if (offset >= visible.end()) {
            return textOriginX() + Math.min(editableWidth(), canvas.textWidth(buffer.draft().substring(visible.start(), visible.end()), MAIN_TEXT_SIZE, false));
        }
        return textOriginX() + canvas.textWidth(buffer.draft().substring(visible.start(), offset), MAIN_TEXT_SIZE, false);
    }

    private int caretAt(double mouseX) {
        if (buffer.empty() || lastCaretOffsets.length == 0) {
            return 0;
        }
        if (mouseX <= lastCaretPositions[0]) {
            return lastVisibleStart;
        }
        int last = lastCaretPositions.length - 1;
        if (mouseX >= lastCaretPositions[last]) {
            return lastVisibleEnd;
        }
        for (int i = 0; i < lastCaretPositions.length - 1; i++) {
            double left = lastCaretPositions[i];
            double right = lastCaretPositions[i + 1];
            if (mouseX >= left && mouseX <= right) {
                return mouseX < (left + right) * 0.5 ? lastCaretOffsets[i] : lastCaretOffsets[i + 1];
            }
        }
        return buffer.draft().length();
    }

    private void drawMessage(NvgCanvas canvas, double timeSeconds) {
        if (message.isEmpty() || timeSeconds > messageVisibleUntil) {
            return;
        }
        canvas.text(message, (float) (bounds.x() + PANEL_PADDING + 2.0), (float) (bounds.y() + bounds.height() - 13.0),
                MESSAGE_TEXT_SIZE, NVG_ALIGN_LEFT, messageColor(), true);
    }

    private void showMessage(MessageKind kind, String value) {
        messageKind = kind;
        message = value;
        messageVisibleUntil = System.nanoTime() / 1_000_000_000.0 + MESSAGE_SECONDS;
    }

    private RenderTheme.Rgba messageColor() {
        return switch (messageKind) {
            case ACTIVE -> ACTIVE_MESSAGE;
            case COMMITTED -> COMMITTED_MESSAGE;
            case CANCELLED -> CANCELLED_MESSAGE;
            case HISTORY -> HISTORY_MESSAGE;
        };
    }

    private static Rect boundsFor(Viewport viewport) {
        double width = clamp(viewport.windowWidth() - 40.0, PANEL_MIN_WIDTH, PANEL_MAX_WIDTH);
        return new Rect(PANEL_X, PANEL_Y, width, panelHeight());
    }

    private static Rect fieldBoundsFor(Rect panel) {
        double top = SHOW_TITLE ? 34.0 : 14.0;
        return new Rect(panel.x() + PANEL_PADDING, panel.y() + top, panel.width() - PANEL_PADDING * 2.0, FIELD_HEIGHT);
    }

    private double textOriginX() {
        return fieldBounds.x() + FIELD_TEXT_PADDING;
    }

    private double editableWidth() {
        return Math.max(80.0, fieldBounds.width() - FIELD_TEXT_PADDING * 2.0 - 34.0);
    }

    private static double panelHeight() {
        return SHOW_TITLE ? 110.0 : 92.0;
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

    private static String abbreviated(String value) {
        if (value.codePointCount(0, value.length()) <= 28) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, 28)) + "…";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum MessageKind {
        ACTIVE,
        COMMITTED,
        CANCELLED,
        HISTORY
    }

    private record VisibleText(String value, int start, int end, int[] caretOffsets, double[] caretPositions) { }
}
