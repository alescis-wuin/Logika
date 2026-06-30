package dev.alexis.logika.graphics;

import java.util.EnumMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.GLFW_CROSSHAIR_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_IBEAM_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_NOT_ALLOWED_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_POINTING_HAND_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZE_ALL_CURSOR;
import static org.lwjgl.glfw.GLFW.glfwCreateStandardCursor;
import static org.lwjgl.glfw.GLFW.glfwDestroyCursor;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwSetCursor;
import static org.lwjgl.system.MemoryUtil.NULL;

final class CursorFeedback implements AutoCloseable {
    private final Map<Style, Long> cursors = new EnumMap<>(Style.class);
    private Style current = Style.DEFAULT;
    private boolean initialized;

    void init() {
        if (initialized) {
            return;
        }
        cursors.put(Style.HAND, glfwCreateStandardCursor(GLFW_POINTING_HAND_CURSOR));
        cursors.put(Style.WIRE, glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR));
        cursors.put(Style.FORBIDDEN, glfwCreateStandardCursor(GLFW_NOT_ALLOWED_CURSOR));
        cursors.put(Style.MOVE, glfwCreateStandardCursor(GLFW_RESIZE_ALL_CURSOR));
        cursors.put(Style.TEXT, glfwCreateStandardCursor(GLFW_IBEAM_CURSOR));
        initialized = true;
    }

    void apply(Style style) {
        if (!initialized || style == current) {
            return;
        }
        long window = glfwGetCurrentContext();
        if (window == NULL) {
            return;
        }
        Long cursor = cursors.get(style);
        glfwSetCursor(window, cursor == null ? NULL : cursor);
        current = style;
    }

    @Override
    public void close() {
        for (long cursor : cursors.values()) {
            if (cursor != NULL) {
                glfwDestroyCursor(cursor);
            }
        }
        cursors.clear();
        current = Style.DEFAULT;
        initialized = false;
    }

    enum Style {
        DEFAULT,
        HAND,
        WIRE,
        FORBIDDEN,
        MOVE,
        TEXT
    }
}
