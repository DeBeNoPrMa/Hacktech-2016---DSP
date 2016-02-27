import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import javax.sound.sampled.*;
import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marce Coll on 27/02/16.
 */
public class SoundInput implements OnsetHandler {
    // Audio helper classes
    Mixer soundMixer;
    AudioDispatcher dispatcher;

    // Parameters
    double sensitivity = 0;
    double threshold = 0;

    final int sampleRate = 44100;
    final int bufferSize = 512;
    final int overlap = 0;

    public SoundInput() {
        // Setup the sound dispatcher
        try {
            setupDispatcher();
        } catch (LineUnavailableException | UnsupportedAudioFileException e) {
            System.out.println("Shit is nuts yo!");
            e.printStackTrace();
        }
    }

    public void setupDispatcher()
            throws LineUnavailableException,
            UnsupportedAudioFileException
    {
        // float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian
        final AudioFormat audioFormat = new AudioFormat(sampleRate, 16, 1, true, true);
        // Info about the sound data line
        final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        soundMixer = getMixerByName("Built-in Microphone");
        if(soundMixer == null) {
            throw new LineUnavailableException();
        }

        TargetDataLine line;
        line = (TargetDataLine) soundMixer.getLine(dataLineInfo);
        line.open(audioFormat, sampleRate);
        line.start();
        final AudioInputStream stream = new AudioInputStream(line);

        JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);

        dispatcher = new AudioDispatcher(audioStream, bufferSize, overlap);

        // add a processor, handle percussion event.
        dispatcher.addAudioProcessor(
                new PercussionOnsetDetector(sampleRate,
                        bufferSize,
                        this,
                        sensitivity,
                        threshold));

        // run the dispatcher (on a new thread).
        new Thread(dispatcher, "Audio dispatching").start();
    }

    public static void main(String... strings)
            throws InterruptedException,
            InvocationTargetException
    {
        SwingUtilities.invokeAndWait(() -> {
            System.out.println("Initializing");
            SoundInput si = new SoundInput();
        });
    }

    @Override
    public void handleOnset(double v, double v1) {
        System.out.println("Percussion");
    }

    // PRIVATES
    private List<Mixer.Info> getMixers() {
        List<Mixer.Info> ret = new ArrayList<>();

        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            ret.add(mi);
        }
        return ret;
    }

    private Mixer getMixerByName(String name) {
        for(Mixer.Info mi : getMixers()) {
            if(mi.getName().equals(name)) {
                return AudioSystem.getMixer(mi);
            }
        }
        return null;
    }
}
