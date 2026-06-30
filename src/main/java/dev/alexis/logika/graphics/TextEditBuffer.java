package dev.alexis.logika.graphics;

import java.util.ArrayDeque;
import java.util.Deque;

final class TextEditBuffer {
    private static final int MAX_CODE_POINTS = 96;
    private static final int HISTORY_LIMIT = 180;

    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private final Deque<Snapshot> redoStack = new ArrayDeque<>();
    private String committed = "";
    private String draft = "";
    private int caret;
    private int selectionAnchor = -1;

    String draft() {
        return draft;
    }

    boolean empty() {
        return draft.isEmpty();
    }

    int caret() {
        return caret;
    }

    void setCaret(int nextCaret, boolean selecting) {
        int safeCaret = safeIndex(nextCaret);
        if (selecting) {
            if (selectionAnchor < 0) {
                selectionAnchor = caret;
            }
        } else {
            selectionAnchor = -1;
        }
        caret = safeCaret;
        if (!hasSelection()) {
            selectionAnchor = selecting ? selectionAnchor : -1;
        }
    }

    void beginMouseSelection(int nextCaret, boolean extendSelection) {
        int safeCaret = safeIndex(nextCaret);
        if (extendSelection) {
            if (selectionAnchor < 0) {
                selectionAnchor = caret;
            }
            caret = safeCaret;
        } else {
            caret = safeCaret;
            selectionAnchor = safeCaret;
        }
    }

    void endMouseSelection() {
        if (!hasSelection()) {
            selectionAnchor = -1;
        }
    }

    boolean hasSelection() {
        return selectionAnchor >= 0 && selectionAnchor != caret;
    }

    int selectionStart() {
        return hasSelection() ? Math.min(selectionAnchor, caret) : caret;
    }

    int selectionEnd() {
        return hasSelection() ? Math.max(selectionAnchor, caret) : caret;
    }

    String selectedText() {
        return hasSelection() ? draft.substring(selectionStart(), selectionEnd()) : "";
    }

    void selectAll() {
        if (draft.isEmpty()) {
            caret = 0;
            selectionAnchor = -1;
            return;
        }
        selectionAnchor = 0;
        caret = draft.length();
    }

    String copyValue() {
        String selected = selectedText();
        return selected.isEmpty() ? draft : selected;
    }

    String cutValue() {
        if (draft.isEmpty()) {
            return "";
        }
        String value = copyValue();
        recordUndo();
        if (hasSelection()) {
            deleteSelectionWithoutHistory();
        } else {
            draft = "";
            caret = 0;
            selectionAnchor = -1;
        }
        redoStack.clear();
        return value;
    }

    void insert(String value) {
        String text = sanitize(value);
        if (text.isEmpty()) {
            return;
        }
        recordUndo();
        if (hasSelection()) {
            deleteSelectionWithoutHistory();
        }
        String before = draft.substring(0, caret);
        draft = trimToCodePointLimit(before + text + draft.substring(caret));
        caret = Math.min((before + text).length(), draft.length());
        selectionAnchor = -1;
        redoStack.clear();
    }

    void backspace() {
        if (hasSelection()) {
            recordUndo();
            deleteSelectionWithoutHistory();
            redoStack.clear();
            return;
        }
        if (caret <= 0) {
            return;
        }
        recordUndo();
        int previous = draft.offsetByCodePoints(caret, -1);
        draft = draft.substring(0, previous) + draft.substring(caret);
        caret = previous;
        redoStack.clear();
    }

    void deleteForward() {
        if (hasSelection()) {
            recordUndo();
            deleteSelectionWithoutHistory();
            redoStack.clear();
            return;
        }
        if (caret >= draft.length()) {
            return;
        }
        recordUndo();
        int next = draft.offsetByCodePoints(caret, 1);
        draft = draft.substring(0, caret) + draft.substring(next);
        redoStack.clear();
    }

    void moveLeft(boolean selecting) {
        if (!selecting && hasSelection()) {
            caret = selectionStart();
            selectionAnchor = -1;
            return;
        }
        ensureAnchor(selecting);
        if (caret > 0) {
            caret = draft.offsetByCodePoints(caret, -1);
        }
        if (!selecting) {
            selectionAnchor = -1;
        }
    }

    void moveRight(boolean selecting) {
        if (!selecting && hasSelection()) {
            caret = selectionEnd();
            selectionAnchor = -1;
            return;
        }
        ensureAnchor(selecting);
        if (caret < draft.length()) {
            caret = draft.offsetByCodePoints(caret, 1);
        }
        if (!selecting) {
            selectionAnchor = -1;
        }
    }

    void moveHome(boolean selecting) {
        ensureAnchor(selecting);
        caret = 0;
        if (!selecting) {
            selectionAnchor = -1;
        }
    }

    void moveEnd(boolean selecting) {
        ensureAnchor(selecting);
        caret = draft.length();
        if (!selecting) {
            selectionAnchor = -1;
        }
    }

    String commit() {
        committed = draft.strip();
        draft = committed;
        caret = draft.length();
        selectionAnchor = -1;
        return committed;
    }

    void cancel() {
        draft = committed;
        caret = draft.length();
        selectionAnchor = -1;
    }

    boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        redoStack.addLast(snapshot());
        restore(undoStack.removeLast());
        return true;
    }

    boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        undoStack.addLast(snapshot());
        restore(redoStack.removeLast());
        return true;
    }

    private void ensureAnchor(boolean selecting) {
        if (selecting && selectionAnchor < 0) {
            selectionAnchor = caret;
        }
    }

    private void deleteSelectionWithoutHistory() {
        int start = selectionStart();
        int end = selectionEnd();
        if (start == end) {
            selectionAnchor = -1;
            return;
        }
        draft = draft.substring(0, start) + draft.substring(end);
        caret = start;
        selectionAnchor = -1;
    }

    private void recordUndo() {
        Snapshot state = snapshot();
        Snapshot previous = undoStack.peekLast();
        if (!state.equals(previous)) {
            undoStack.addLast(state);
            while (undoStack.size() > HISTORY_LIMIT) {
                undoStack.removeFirst();
            }
        }
    }

    private Snapshot snapshot() {
        return new Snapshot(draft, caret, selectionAnchor);
    }

    private void restore(Snapshot snapshot) {
        draft = snapshot.draft();
        caret = safeIndex(snapshot.caret());
        selectionAnchor = snapshot.selectionAnchor();
        if (selectionAnchor > draft.length()) {
            selectionAnchor = draft.length();
        }
        if (!hasSelection()) {
            selectionAnchor = -1;
        }
    }

    private int safeIndex(int index) {
        return Math.max(0, Math.min(index, draft.length()));
    }

    private static boolean isEditableCodePoint(int codepoint) {
        return Character.isValidCodePoint(codepoint) && !Character.isISOControl(codepoint);
    }

    private static String sanitize(String value) {
        StringBuilder builder = new StringBuilder();
        value.codePoints()
                .filter(TextEditBuffer::isEditableCodePoint)
                .map(codepoint -> Character.isWhitespace(codepoint) ? ' ' : codepoint)
                .forEach(builder::appendCodePoint);
        return builder.toString().replaceAll(" {2,}", " ");
    }

    private static String trimToCodePointLimit(String value) {
        int count = value.codePointCount(0, value.length());
        if (count <= MAX_CODE_POINTS) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, MAX_CODE_POINTS));
    }

    private record Snapshot(String draft, int caret, int selectionAnchor) { }
}
