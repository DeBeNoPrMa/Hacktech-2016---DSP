import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.onsets.OnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.util.fft.FFT;

import java.io.*;

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

    private float baselineAverage;
    private int  baselineCounter;

    private OnsetHandler handler;

    private final float sampleRate;//samples per second (Hz)
    private long processedSamples;//in samples

    private final double sensitivity;

    private final double threshold;

    //private float[] baselineAverageArray;

    public PercussionDetector(float sampleRate, int bufferSize,
                              int bufferOverlap, OnsetHandler handler) {
        this(sampleRate, bufferSize, handler,
                DEFAULT_SENSITIVITY, DEFAULT_THRESHOLD);
        millis = System.currentTimeMillis() % 1000;
    }

    public PercussionDetector(float sampleRate, int bufferSize, OnsetHandler handler, double sensitivity, double threshold) {
        fft = new FFT(bufferSize / 2);
        this.threshold = threshold;
        this.sensitivity = sensitivity;
        priorMagnitudes = new float[bufferSize / 2];
        currentMagnitudes = new float[bufferSize / 2];
        this.handler = handler;
        this.sampleRate = sampleRate;
       // baselineAverageArray = new float[10];
    }

    private boolean isClap(float ra, float rb, float[] audioFloatBuffer) {
        float average = 0.0f;
        for(int i = (int)ra; i < (int)rb && i < audioFloatBuffer.length; i++) {
            average += Math.abs(audioFloatBuffer[i]);
        }
        average = average/200.0f;

        return average > threshold;
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        float timeStamp = processedSamples / sampleRate;

        float[] audioFloatBuffer = audioEvent.getFloatBuffer();
        this.processedSamples += audioFloatBuffer.length;
        this.processedSamples -= audioEvent.getOverlap();

        /*float total_time = audioFloatBuffer.length / sampleRate;
        for (int i = 0; i < (int)Math.ceil(total_time/0.1f) - 1; i++) {
            if(isClap(i * sampleRate * 0.1f, i * 0.1f * sampleRate + 0.2f * sampleRate, audioFloatBuffer)) {
                i++;
                handler.handleOnset(timeStamp, -1);
            }

        }*/

        printTimeDiff("Before fft: ");

        fft.forwardTransform(audioFloatBuffer);

        PrintWriter writer;
        try {
             writer = new PrintWriter(new BufferedWriter(new FileWriter("myfile.txt", true)));
        }
        catch(IOException e) {
            e.printStackTrace();
            return true;
        }

        for(float f : audioFloatBuffer) {
            writer.println(f);
        }
        writer.close();

        printTimeDiff("After fft: ");

        fft.modulus(audioFloatBuffer, currentMagnitudes);

        printTimeDiff("After modulus: ");

        int binsOverThreshold = 0;
        for (int i = 0; i < currentMagnitudes.length; i++) {
            if (priorMagnitudes[i] > 0.f) {
                double diff = 20 * Math.log10(currentMagnitudes[i]
                        / priorMagnitudes[i]);
                if (diff >= threshold) {
                    binsOverThreshold++;
                }
            }

            priorMagnitudes[i] = currentMagnitudes[i];

            if (dfMinus2 < dfMinus1
                    && dfMinus1 >= binsOverThreshold
                    && dfMinus1 > ((100 - sensitivity) * audioFloatBuffer.length) / 200) {
                handler.handleOnset(timeStamp, -1);
                break;
            }
        }

        printTimeDiff("After loop: ");

        dfMinus2 = dfMinus1;
        dfMinus1 = binsOverThreshold;

        return true;
    }

    public int clamp(int n, int min, int max) {
        if(n > max){
            return max;
        } else if(n < min) {
            return min;
        } else {
            return n;
        }
    }

    public long millis_now;
    public long millis;
    public void printTimeDiff(String label) {
        millis_now = System.currentTimeMillis() % 1000;
        long diff = millis_now - millis;
        //System.out.println(label + diff);
        millis = millis_now;
    }

    @Override
    public void processingFinished() {
    }

    @Override
    public void setHandler(OnsetHandler handler) {
        this.handler = handler;
    }
}
