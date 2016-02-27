import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.onsets.OnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.util.fft.FFT;

/**
 * Created by Marce Coll on 27/02/16.
 */
public class PercussionDetector implements AudioProcessor, OnsetDetector {
    public static final double DEFAULT_THRESHOLD = 8;

    public static final double DEFAULT_SENSITIVITY = 20;

    private final FFT fft;

    private final float[] priorMagnitudes;
    private final float[] currentMagnitudes;

    private float dfMinus1, dfMinus2;

    private OnsetHandler handler;

    private final float sampleRate;//samples per second (Hz)
    private long processedSamples;//in samples

    private final double sensitivity;

    private final double threshold;

    public PercussionDetector(float sampleRate, int bufferSize,
                                   int bufferOverlap, OnsetHandler handler) {
        this(sampleRate, bufferSize, handler,
                DEFAULT_SENSITIVITY, DEFAULT_THRESHOLD);
    }

    public PercussionDetector(float sampleRate, int bufferSize, OnsetHandler handler, double sensitivity, double threshold) {
        fft = new FFT(bufferSize / 2);
        this.threshold = threshold;
        this.sensitivity = sensitivity;
        priorMagnitudes = new float[bufferSize / 2];
        currentMagnitudes = new float[bufferSize / 2];
        this.handler = handler;
        this.sampleRate = sampleRate;

    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        float[] audioFloatBuffer = audioEvent.getFloatBuffer();
        this.processedSamples += audioFloatBuffer.length;
        this.processedSamples -= audioEvent.getOverlap();

        fft.forwardTransform(audioFloatBuffer);
        fft.modulus(audioFloatBuffer, currentMagnitudes);
        int binsOverThreshold = 0;
        for (int i = 0; i < currentMagnitudes.length; i++) {
            if (priorMagnitudes[i] > 0.f) {
                double diff = 10 * Math.log10(currentMagnitudes[i]
                        / priorMagnitudes[i]);
                if (diff >= threshold) {
                    binsOverThreshold++;
                }
            }
            priorMagnitudes[i] = currentMagnitudes[i];
        }

        if (dfMinus2 < dfMinus1
                && dfMinus1 >= binsOverThreshold
                && dfMinus1 > ((100 - sensitivity) * audioFloatBuffer.length) / 200) {
            float timeStamp = processedSamples / sampleRate;
            handler.handleOnset(timeStamp,-1);
        }

        dfMinus2 = dfMinus1;
        dfMinus1 = binsOverThreshold;

        return true;
    }

    @Override
    public void processingFinished() {
    }

    @Override
    public void setHandler(OnsetHandler handler) {
        this.handler = handler;
    }
}
