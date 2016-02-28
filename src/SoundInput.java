import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import javax.sound.sampled.*;
import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Marce Coll on 27/02/16.
 */

enum Note {
    F, G, A, None
}

public interface PitchHandler {
    public void insertIntoQueue();
}

public class SoundInput implements OnsetHandler, PitchDetectionHandler {
    // Audio helper classes
    Mixer soundMixer;
    BetterAudioDispatcher dispatcher;

    PitchProcessor.PitchEstimationAlgorithm algo;

    // Parameters
    double sensitivity = 35;
    double threshold = 10;

    final int sampleRate = 44100;
    final int bufferSize = 1024;
    final int overlap = 0;

    // NOTE PARAMETERS
    float noteOffset = 0.0f;
    Map<Note, Float> baseFreq = new EnumMap<Note, Float>(Note.class);
    float bpm;

    PitchHandler handler;

    public SoundInput() {
        // Setup the sound dispatcher
        try {
            setupDispatcher();
        } catch (LineUnavailableException | UnsupportedAudioFileException e) {
            System.out.println("Shit is nuts yo!");
            e.printStackTrace();
        }
    }

    public void setupDispatcher(float bpm, PitchHandler h)
            throws LineUnavailableException,
            UnsupportedAudioFileException
    {
        // float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian
        final AudioFormat audioFormat = new AudioFormat(sampleRate, 16, 1, true, true);
        // Info about the sound data line
        final DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        // Pitch Recognition algorithm
        algo = PitchProcessor.PitchEstimationAlgorithm.DYNAMIC_WAVELET;

        // Base frequencies
        baseFreq.put(Note.F, 349.23f);
        baseFreq.put(Note.G, 392.00f);
        baseFreq.put(Note.A, 440.00f);

        handler = h;
        this.bpm = bpm;

        // Setup music device
        soundMixer = getMixerByName("Logitech Camera");
        if(soundMixer == null) {
            throw new LineUnavailableException();
        }

        TargetDataLine line;
        line = (TargetDataLine) soundMixer.getLine(dataLineInfo);
        line.open(audioFormat, sampleRate);
        line.start();
        final AudioInputStream stream = new AudioInputStream(line);

        JVMAudioInputStream audioStream = new JVMAudioInputStream(stream);

        // The dispatcher is the manager of the device, it routes the data to audio processors
        dispatcher = new BetterAudioDispatcher(audioStream, bufferSize, overlap);

        // add a processor, handle percussion event.
        /*dispatcher.addAudioProcessor(
                new PercussionDetector(sampleRate,
                        bufferSize,
                        this,
                        sensitivity,
                        threshold));*/

        dispatcher.addAudioProcessor(
                new PitchProcessor(algo, (float)sampleRate, bufferSize, this));

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
        System.out.println("Percussion at " + v);
    }

    // Callback that handles a pitch detection object
    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        if(pitchDetectionResult.getPitch() != -1) {
            double timeStamp = audioEvent.getTimeStamp();
            float pitch = pitchDetectionResult.getPitch();

            Note note = getNoteByFrequency(pitch);
            System.out.println(note.name());
        }
    }

    // PRIVATES
    // Helper method to get a list of mixers
    private List<Mixer.Info> getMixers() {
        List<Mixer.Info> ret = new ArrayList<>();

        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            ret.add(mi);
        }
        return ret;
    }

    // Helper method to get a method by system name
    private Mixer getMixerByName(String name) {
        for(Mixer.Info mi : getMixers()) {
            if(mi.getName().equals(name)) {
                return AudioSystem.getMixer(mi);
            }
        }
        return null;
    }

    // Helper method to get a note by frequency, it takes into account
    // error range and offset
    private Note getNoteByFrequency(float freq) {
        float freqF = getFrequencyByNote(Note.F);
        float freqG = getFrequencyByNote(Note.G);
        float freqA = getFrequencyByNote(Note.A);
        float diffFG = freqG - freqF;
        float diffGA = freqA - freqG;
        if (freq < freqF - diffFG/2.0f || freq > freqA + diffGA/2.0f) {
            return Note.None;
        } else if (freq < freqG - diffFG/2) {
            return Note.F;
        } else if (freq < freqA - diffGA/2) {
            return Note.G;
        } else {
            return Note.A;
        }
    }

    // Helper method to get the base frequency of a note applying
    // the note offset if set
    private float getFrequencyByNote(Note n) {
        if(baseFreq.containsKey(n)) {
            return baseFreq.get(n) + noteOffset;
        }
        return -1.0f;
    }
}
