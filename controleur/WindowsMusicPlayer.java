package controleur;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class WindowsMusicPlayer {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("error");
            return;
        }

        Clip clip;
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(new File(args[0]))) {
            clip = AudioSystem.getClip();
            clip.open(stream);
        }

        System.out.println("ready");
        System.out.flush();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ("exit".equals(line)) {
                    break;
                }
                if ("play".equals(line)) {
                    if (!clip.isRunning()) {
                        clip.loop(Clip.LOOP_CONTINUOUSLY);
                    }
                } else if ("stop".equals(line)) {
                    clip.stop();
                }
            }
        } finally {
            clip.stop();
            clip.close();
        }
    }
}
