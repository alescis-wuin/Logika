package dev.alexis.logika.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.AL_BUFFER;
import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_GAIN;
import static org.lwjgl.openal.AL10.AL_PITCH;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alDeleteSources;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.openal.AL10.alGenSources;
import static org.lwjgl.openal.AL10.alSourcePlay;
import static org.lwjgl.openal.AL10.alSourceStop;
import static org.lwjgl.openal.AL10.alSourcef;
import static org.lwjgl.openal.AL10.alSourcei;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class AudioService implements AutoCloseable {
    private static final int SAMPLE_RATE = 44_100;

    private long device = NULL;
    private long context = NULL;
    private int clickBuffer;
    private int clickSource;
    private boolean available;

    public void init() {
        try {
            device = alcOpenDevice((ByteBuffer) null);
            if (device == NULL) {
                return;
            }

            context = alcCreateContext(device, (IntBuffer) null);
            if (context == NULL) {
                alcCloseDevice(device);
                device = NULL;
                return;
            }

            alcMakeContextCurrent(context);
            ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
            AL.createCapabilities(alcCapabilities);

            clickBuffer = createToneBuffer(880.0f, 0.045f);
            clickSource = alGenSources();
            alSourcei(clickSource, AL_BUFFER, clickBuffer);
            alSourcef(clickSource, AL_GAIN, 0.16f);
            available = true;
        } catch (RuntimeException ex) {
            available = false;
            closeContextOnly();
            System.err.println("OpenAL disabled: " + ex.getMessage());
        }
    }

    public void playClick(boolean high) {
        if (!available) {
            return;
        }

        alSourceStop(clickSource);
        alSourcef(clickSource, AL_PITCH, high ? 1.18f : 0.86f);
        alSourcePlay(clickSource);
    }

    private static int createToneBuffer(float frequency, float durationSeconds) {
        int samples = Math.max(1, (int) (SAMPLE_RATE * durationSeconds));
        ShortBuffer pcm = BufferUtils.createShortBuffer(samples);

        for (int i = 0; i < samples; i++) {
            double t = i / (double) SAMPLE_RATE;
            double envelope = 1.0 - i / (double) samples;
            short value = (short) (Math.sin(2.0 * Math.PI * frequency * t) * envelope * Short.MAX_VALUE * 0.45);
            pcm.put(value);
        }
        pcm.flip();

        int buffer = alGenBuffers();
        alBufferData(buffer, AL_FORMAT_MONO16, pcm, SAMPLE_RATE);
        return buffer;
    }

    @Override
    public void close() {
        if (!available) {
            closeContextOnly();
            return;
        }

        alDeleteSources(clickSource);
        alDeleteBuffers(clickBuffer);
        available = false;
        closeContextOnly();
    }

    private void closeContextOnly() {
        if (context != NULL) {
            alcMakeContextCurrent(NULL);
            alcDestroyContext(context);
            context = NULL;
        }
        if (device != NULL) {
            alcCloseDevice(device);
            device = NULL;
        }
    }
}
