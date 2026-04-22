package io.github.superteam.resonance.sound;

import com.badlogic.gdx.audio.AudioRecorder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;

public class MicInputListenerTest {

    @Test
    public void sampleRealtimeReturnsImmediatelyWhileCaptureThreadBlocks() throws Exception {
        BlockingAudioRecorder recorder = new BlockingAudioRecorder();
        MicInputListener listener = new MicInputListener(0.1f, (sampleRate, sampleCount) -> recorder);

        listener.start(16000, 4);
        Assert.assertTrue("capture thread should enter read", recorder.readEntered.await(1, TimeUnit.SECONDS));

        long startNanos = System.nanoTime();
        MicInputListener.MicSample silentSample = listener.sampleRealtime(0.016f);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        Assert.assertTrue("sampleRealtime should be non-blocking", elapsedMillis < 50L);
        Assert.assertEquals(0f, silentSample.rms(), 0.0001f);

        recorder.allowReadToProceed.countDown();
        Assert.assertTrue("capture thread should finish the blocked read", recorder.samplePublished.await(1, TimeUnit.SECONDS));

        listener.stop();
        Assert.assertFalse(listener.isActive());
    }

    private static final class BlockingAudioRecorder implements AudioRecorder {
        private final CountDownLatch readEntered = new CountDownLatch(1);
        private final CountDownLatch allowReadToProceed = new CountDownLatch(1);
        private final CountDownLatch samplePublished = new CountDownLatch(1);
        private final AtomicBoolean disposed = new AtomicBoolean(false);

        @Override
        public void read(short[] samples, int offset, int numSamples) {
            readEntered.countDown();
            try {
                allowReadToProceed.await();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }

            if (disposed.get()) {
                return;
            }

            for (int index = 0; index < numSamples; index++) {
                samples[offset + index] = 16384;
            }
            samplePublished.countDown();
        }

        @Override
        public void dispose() {
            disposed.set(true);
            allowReadToProceed.countDown();
        }
    }
}