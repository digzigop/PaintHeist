import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/** Lightweight synthesized sound effects using standard Java Sound PCM audio. */
public final class SoundEffects {
    public enum Cue {
        CLICK,
        COPY_COMPLETE,
        SWAP,
        DISTRACT,
        HIDE,
        PASSAGE,
        SWITCH,
        ALERT,
        VICTORY,
        BUSTED
    }

    private static final float SAMPLE_RATE = 22050f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final Map<Cue, byte[]> BUFFERS = buildBuffers();
    private static final ExecutorService AUDIO_THREAD = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "paintheist-audio");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile boolean muted;
    private static volatile boolean audioUnavailable;

    private SoundEffects() { }

    public static boolean isMuted() {
        return muted;
    }

    public static boolean toggleMute() {
        muted = !muted;
        return muted;
    }

    public static void play(Cue cue) {
        if (muted || audioUnavailable || cue == null) return;
        byte[] data = BUFFERS.get(cue);
        if (data == null || data.length == 0) return;
        AUDIO_THREAD.submit(() -> playBuffer(data));
    }

    private static void playBuffer(byte[] data) {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
            line.open(FORMAT);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
        } catch (LineUnavailableException | SecurityException | IllegalArgumentException ignored) {
            audioUnavailable = true;
        }
    }

    private static Map<Cue, byte[]> buildBuffers() {
        Map<Cue, byte[]> map = new EnumMap<>(Cue.class);
        map.put(Cue.CLICK, sequence(new double[][] {{680, 45, 0.16}}));
        map.put(Cue.COPY_COMPLETE, sequence(new double[][] {{587, 70, 0.20}, {880, 120, 0.22}}));
        map.put(Cue.SWAP, sequence(new double[][] {{440, 55, 0.18}, {660, 75, 0.20}, {990, 95, 0.20}}));
        map.put(Cue.DISTRACT, sequence(new double[][] {{1040, 40, 0.18}, {740, 60, 0.15}}));
        map.put(Cue.HIDE, sequence(new double[][] {{280, 70, 0.16}}));
        map.put(Cue.PASSAGE, sequence(new double[][] {{330, 80, 0.18}, {495, 110, 0.18}}));
        map.put(Cue.SWITCH, sequence(new double[][] {{520, 55, 0.18}, {390, 85, 0.18}}));
        map.put(Cue.ALERT, sequence(new double[][] {{780, 95, 0.24}, {620, 95, 0.24}, {780, 115, 0.24}}));
        map.put(Cue.VICTORY, sequence(new double[][] {
            {523, 90, 0.20}, {659, 90, 0.20}, {784, 90, 0.22}, {1046, 180, 0.24}
        }));
        map.put(Cue.BUSTED, sequence(new double[][] {{310, 130, 0.22}, {220, 200, 0.22}}));
        return map;
    }

    private static byte[] sequence(double[][] notes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (double[] note : notes) {
            appendTone(out, note[0], (int) note[1], note[2]);
        }
        return out.toByteArray();
    }

    private static void appendTone(ByteArrayOutputStream out, double freqHz, int durationMs, double volume) {
        int samples = Math.max(1, (int) (SAMPLE_RATE * durationMs / 1000.0));
        int attack = Math.max(1, samples / 12);
        for (int i = 0; i < samples; i++) {
            double env = i < attack ? (double) i / attack : (double) (samples - i) / (samples - attack);
            double wave = Math.sin(2.0 * Math.PI * freqHz * i / SAMPLE_RATE);
            short sample = (short) Math.round(wave * env * volume * Short.MAX_VALUE);
            out.write(sample & 0xFF);
            out.write((sample >> 8) & 0xFF);
        }
    }
}
