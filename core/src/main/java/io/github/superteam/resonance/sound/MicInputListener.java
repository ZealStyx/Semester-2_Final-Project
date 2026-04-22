package io.github.superteam.resonance.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioRecorder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Poll-based microphone listener for clap/shout threshold detection.
 */
public final class MicInputListener {
    private static final float NOISE_FLOOR_EMA_ALPHA = 0.08f;
    private static final float CLAP_ZCR_THRESHOLD = 0.18f;
    private static final float SHOUT_ENERGY_MULTIPLIER = 2.2f;
    private static final float GATE_COOLDOWN_SECONDS = 0.10f;
    private static final float SPEAKING_GATE_RATIO = 0.75f;
    private static final float LOW_LEVEL_RATIO = 0.90f;
    private static final float MID_LEVEL_RATIO = 1.35f;

    private final RecorderFactory recorderFactory;
    private final AtomicBoolean captureRunning = new AtomicBoolean(false);
    private final AtomicReference<CaptureSnapshot> latestSnapshot = new AtomicReference<>(CaptureSnapshot.silent());
    private final Object lifecycleLock = new Object();

    private AudioRecorder audioRecorder;
    private short[] sampleBuffer;
    private float thresholdRms;
    private float adaptiveNoiseFloor;
    private float gateCooldownRemainingSeconds;
    private volatile float lastComputedRms;
    private volatile Thread captureThread;
    private boolean active;

    public MicInputListener(float thresholdRms) {
        this(thresholdRms, new DefaultRecorderFactory());
    }

    MicInputListener(float thresholdRms, RecorderFactory recorderFactory) {
        this.thresholdRms = Math.max(0f, thresholdRms);
        this.recorderFactory = recorderFactory == null ? new DefaultRecorderFactory() : recorderFactory;
    }

    public void start(int sampleRate, int sampleCount) {
        synchronized (lifecycleLock) {
            if (active) {
                return;
            }

            int safeSampleRate = Math.max(8000, sampleRate);
            int safeSampleCount = Math.max(256, sampleCount);
            AudioRecorder createdRecorder = recorderFactory.create(safeSampleRate, safeSampleCount);
            if (createdRecorder == null) {
                return;
            }

            audioRecorder = createdRecorder;
            sampleBuffer = new short[safeSampleCount];
            adaptiveNoiseFloor = 0f;
            gateCooldownRemainingSeconds = 0f;
            lastComputedRms = 0f;
            latestSnapshot.set(CaptureSnapshot.silent());
            captureRunning.set(true);
            active = true;

            Thread worker = new Thread(this::runCaptureLoop, "MicInputListener-Capture");
            worker.setDaemon(true);
            captureThread = worker;
            worker.start();
        }
    }

    public void stop() {
        Thread workerToJoin;
        AudioRecorder recorderToDispose;

        synchronized (lifecycleLock) {
            if (!active) {
                return;
            }

            active = false;
            captureRunning.set(false);
            workerToJoin = captureThread;
            captureThread = null;
            recorderToDispose = audioRecorder;
            audioRecorder = null;
            sampleBuffer = null;
        }

        if (recorderToDispose != null) {
            recorderToDispose.dispose();
        }

        if (workerToJoin != null && workerToJoin != Thread.currentThread()) {
            try {
                workerToJoin.join(500L);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        synchronized (lifecycleLock) {
            latestSnapshot.set(CaptureSnapshot.silent());
            adaptiveNoiseFloor = 0f;
            gateCooldownRemainingSeconds = 0f;
            lastComputedRms = 0f;
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setThresholdRms(float thresholdRms) {
        this.thresholdRms = Math.max(0f, thresholdRms);
    }

    public float getLastComputedRms() {
        return lastComputedRms;
    }

    public MicSample sampleRealtime(float deltaSeconds) {
        if (!active) {
            return MicSample.silent();
        }

        CaptureSnapshot snapshot = latestSnapshot.get();
        if (snapshot == null) {
            return MicSample.silent();
        }

        lastComputedRms = snapshot.sample().rms();
        return snapshot.sample();
    }

    public void poll(Consumer<Float> onDetected) {
        poll(1f / 60f, micEvent -> {
            if (onDetected != null) {
                onDetected.accept(lastComputedRms);
            }
        });
    }

    public void poll(float deltaSeconds, Consumer<MicEvent> onDetected) {
        if (onDetected == null) {
            return;
        }
        MicSample micSample = sampleRealtime(deltaSeconds);
        if (micSample.detectedEvent() != null) {
            onDetected.accept(micSample.detectedEvent());
        }
    }

    private void runCaptureLoop() {
        long previousReadNanos = System.nanoTime();

        while (captureRunning.get()) {
            AudioRecorder recorder = audioRecorder;
            short[] localBuffer = sampleBuffer;
            if (recorder == null || localBuffer == null) {
                break;
            }

            try {
                recorder.read(localBuffer, 0, localBuffer.length);
            } catch (Throwable throwable) {
                if (captureRunning.get()) {
                    if (Gdx.app != null) {
                        Gdx.app.log("MicInputListener", "Microphone capture stopped: " + throwable.getMessage());
                    }
                }
                break;
            }

            if (!captureRunning.get()) {
                break;
            }

            long nowNanos = System.nanoTime();
            float elapsedSeconds = Math.max(0f, (nowNanos - previousReadNanos) / 1_000_000_000f);
            previousReadNanos = nowNanos;

            MicSample micSample = buildSample(localBuffer, elapsedSeconds);
            lastComputedRms = micSample.rms();
            latestSnapshot.set(new CaptureSnapshot(micSample, nowNanos));
        }
    }

    private MicSample buildSample(short[] samples, float elapsedSeconds) {
        float clampedElapsedSeconds = Math.max(0f, elapsedSeconds);
        float rms = computeRms(samples);
        updateAdaptiveNoiseFloor(rms);
        gateCooldownRemainingSeconds = Math.max(0f, gateCooldownRemainingSeconds - clampedElapsedSeconds);

        float dynamicThreshold = Math.max(thresholdRms, adaptiveNoiseFloor * SHOUT_ENERGY_MULTIPLIER);
        float normalizedLevel = dynamicThreshold <= 0.0001f ? 0f : rms / dynamicThreshold;
        boolean speakingDetected = rms >= (dynamicThreshold * SPEAKING_GATE_RATIO);
        MicVolumeLevel micVolumeLevel = classifyVolumeLevel(normalizedLevel);

        MicEvent detectedEvent = null;
        if (gateCooldownRemainingSeconds <= 0f && rms >= dynamicThreshold) {
            float zeroCrossingRate = computeZeroCrossingRate(samples);
            detectedEvent = classifyMicEvent(rms, zeroCrossingRate, dynamicThreshold);
            gateCooldownRemainingSeconds = GATE_COOLDOWN_SECONDS;
        }

        return new MicSample(rms, dynamicThreshold, normalizedLevel, micVolumeLevel, speakingDetected, detectedEvent);
    }

    private MicVolumeLevel classifyVolumeLevel(float normalizedLevel) {
        if (normalizedLevel < LOW_LEVEL_RATIO) {
            return MicVolumeLevel.LOW;
        }
        if (normalizedLevel < MID_LEVEL_RATIO) {
            return MicVolumeLevel.MID;
        }
        return MicVolumeLevel.HIGH;
    }

    private float computeRms(short[] samples) {
        if (samples.length == 0) {
            return 0f;
        }
        double sumSquares = 0d;
        for (short sample : samples) {
            float normalized = sample / 32768f;
            sumSquares += normalized * normalized;
        }
        return (float) Math.sqrt(sumSquares / samples.length);
    }

    private float computeZeroCrossingRate(short[] samples) {
        if (samples.length < 2) {
            return 0f;
        }
        int crossings = 0;
        for (int index = 1; index < samples.length; index++) {
            short previous = samples[index - 1];
            short current = samples[index];
            if ((previous < 0 && current >= 0) || (previous >= 0 && current < 0)) {
                crossings++;
            }
        }
        return crossings / (float) (samples.length - 1);
    }

    private void updateAdaptiveNoiseFloor(float rms) {
        if (adaptiveNoiseFloor <= 0f) {
            adaptiveNoiseFloor = rms;
            return;
        }

        if (rms < thresholdRms) {
            adaptiveNoiseFloor = (adaptiveNoiseFloor * (1f - NOISE_FLOOR_EMA_ALPHA)) + (rms * NOISE_FLOOR_EMA_ALPHA);
        }
    }

    private MicEvent classifyMicEvent(float rms, float zeroCrossingRate, float threshold) {
        if (zeroCrossingRate >= CLAP_ZCR_THRESHOLD) {
            return MicEvent.CLAP;
        }
        if (rms >= threshold * 1.25f) {
            return MicEvent.SHOUT;
        }
        return MicEvent.BLOW;
    }

    public record MicSample(
        float rms,
        float dynamicThreshold,
        float normalizedLevel,
        MicVolumeLevel volumeLevel,
        boolean speakingDetected,
        MicEvent detectedEvent
    ) {
        public static MicSample silent() {
            return new MicSample(0f, 1f, 0f, MicVolumeLevel.LOW, false, null);
        }
    }

    interface RecorderFactory {
        AudioRecorder create(int sampleRate, int sampleCount);
    }

    private static final class DefaultRecorderFactory implements RecorderFactory {
        @Override
        public AudioRecorder create(int sampleRate, int sampleCount) {
            if (Gdx.audio == null) {
                return null;
            }
            return Gdx.audio.newAudioRecorder(sampleRate, true);
        }
    }

    private record CaptureSnapshot(MicSample sample, long capturedNanos) {
        private static CaptureSnapshot silent() {
            return new CaptureSnapshot(MicSample.silent(), 0L);
        }
    }
}
