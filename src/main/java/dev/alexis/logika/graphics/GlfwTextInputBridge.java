package dev.alexis.logika.graphics;

import dev.alexis.logika.util.Vec2;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

final class GlfwTextInputBridge implements AutoCloseable {
    private final long window;
    private final TextInputOverlay textInput;
    private GLFWCharCallback previousCharCallback;
    private GLFWKeyCallback previousKeyCallback;
    private GLFWMouseButtonCallback previousMouseButtonCallback;
    private GLFWCharCallback charCallback;
    private GLFWKeyCallback keyCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;
    private boolean installed;

    GlfwTextInputBridge(long window, TextInputOverlay textInput) {
        this.window = window;
        this.textInput = textInput;
    }

    void install() {
        if (installed || window == NULL) {
            return;
        }

        charCallback = GLFWCharCallback.create((handle, codepoint) -> {
            if (!textInput.handleCodepoint(codepoint) && previousCharCallback != null) {
                previousCharCallback.invoke(handle, codepoint);
            }
        });
        previousCharCallback = glfwSetCharCallback(window, charCallback);

        keyCallback = GLFWKeyCallback.create((handle, key, scancode, action, mods) -> {
            if (!textInput.handleKey(handle, key, action, mods) && previousKeyCallback != null) {
                previousKeyCallback.invoke(handle, key, scancode, action, mods);
            }
        });
        previousKeyCallback = glfwSetKeyCallback(window, keyCallback);

        mouseButtonCallback = GLFWMouseButtonCallback.create((handle, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                Vec2 cursor = cursorPosition(handle);
                if (textInput.handleMousePress(cursor.x(), cursor.y())) {
                    return;
                }
            }
            if (previousMouseButtonCallback != null) {
                previousMouseButtonCallback.invoke(handle, button, action, mods);
            }
        });
        previousMouseButtonCallback = glfwSetMouseButtonCallback(window, mouseButtonCallback);
        installed = true;
    }

    @Override
    public void close() {
        if (installed && window != NULL) {
            glfwSetCharCallback(window, previousCharCallback);
            glfwSetKeyCallback(window, previousKeyCallback);
            glfwSetMouseButtonCallback(window, previousMouseButtonCallback);
        }
        if (charCallback != null) {
            charCallback.free();
            charCallback = null;
        }
        if (keyCallback != null) {
            keyCallback.free();
            keyCallback = null;
        }
        if (mouseButtonCallback != null) {
            mouseButtonCallback.free();
            mouseButtonCallback = null;
        }
        previousCharCallback = null;
        previousKeyCallback = null;
        previousMouseButtonCallback = null;
        installed = false;
    }

    private static Vec2 cursorPosition(long handle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1);
            DoubleBuffer y = stack.mallocDouble(1);
            glfwGetCursorPos(handle, x, y);
            return new Vec2(x.get(0), y.get(0));
        }
    }
}
