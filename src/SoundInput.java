import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;

import javax.sound.sampled.*;

/**
 * Created by Marce Coll on 27/02/16.
 */
public class SoundInput implements OnsetHandler {
    // Audio helper classes
    Mixer soundMixer;
    AudioDispatcher dispatcher;

    // Parameters
    double sensitivity;
    double threshold;
    int counter;

    final int sampleRate = 44100;
    final int bufferSize = 512;
    final int overlap = 0;

    public SoundInput() {
        // TODO
    }

    public SoundInput()

    public void generateNewMixer(Mixer mixer)
            throws LineUnavailableException,
            UnsupportedAudioFileException
    {
        // float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian
        final AudioFormat audioFormat = new AudioFormat(sampleRate, 16, 1, true, true);
        // Info about the sound data line
        final DataLine.Info dataLineInfo  = new DataLine.Info(TargetDataLine.class, audioFormat);

        if(dispatcher != null) {
            dispatcher.stop();
        }

        soundMixer = mixer;

        TargetDataLine line;
        line = (TargetDataLine) mixer.getLine(dataLineInfo);
        line.open(audioFormat, sampleRate);
        line.start();
        final AudioInputStream stream = new AudioInputStream(line);

        JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);

        dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);

        // add a processor, handle percussion event.
        dispatcher.addAudioProcessor(
                new PercussionOnsetDetector(sampleRate,
                                            bufferSize,
                                            this,sensitivity,
                                            threshold));

        // run the dispatcher (on a new thread).
        new Thread(dispatcher,"Audio dispatching").start();
    }
    }
}
