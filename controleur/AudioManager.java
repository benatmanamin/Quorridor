package controleur;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

final class AudioManager {
    private static final String WALL_SOUND_PATH = "audio/wall.wav";
    private static final String MOVE_SOUND_PATH = "audio/move.wav";
    private static final String BACKGROUND_MUSIC_PATH = "audio/background.wav";
    private static final int AUDIO_BUFFER_SIZE = 4096;

    private final File wallSoundFile;
    private final File moveSoundFile;
    private final File backgroundMusicFile;
    private final boolean javaAudioAvailable;
    private final WindowsEffectPlayer wallWindowsPlayer;
    private final WindowsEffectPlayer moveWindowsPlayer;
    private final JavaMusicController javaMusicController;
    private final WindowsMusicController windowsMusicController;
    private boolean effectsEnabled;
    private boolean musicEnabled;
    private String lastError;
    private String lastPlaybackMethod;

    AudioManager() {
        this.wallSoundFile = findAudioFile(WALL_SOUND_PATH);
        this.moveSoundFile = findAudioFile(MOVE_SOUND_PATH);
        this.backgroundMusicFile = findAudioFile(BACKGROUND_MUSIC_PATH);
        this.javaAudioAvailable = AudioSystem.getMixerInfo().length > 0;
        this.wallWindowsPlayer = new WindowsEffectPlayer(wallSoundFile, "quoridor_wall.wav", "quoridor-wall-sound");
        this.moveWindowsPlayer = new WindowsEffectPlayer(moveSoundFile, "quoridor_move.wav", "quoridor-move-sound");
        this.javaMusicController = new JavaMusicController(backgroundMusicFile);
        this.windowsMusicController = new WindowsMusicController(backgroundMusicFile);
        this.effectsEnabled = false;
        this.musicEnabled = false;
        this.lastError = "";
        this.lastPlaybackMethod = "";
        preloadAvailableEffects();
    }

    void setEffectsEnabled(boolean enabled) {
        this.effectsEnabled = enabled;
        if (enabled) {
            preloadAvailableEffects();
        }
    }

    boolean isEffectsEnabled() {
        return effectsEnabled;
    }

    boolean setMusicEnabled(boolean enabled) {
        if (!enabled) {
            this.musicEnabled = false;
            javaMusicController.stop();
            windowsMusicController.stop();
            return true;
        }

        lastError = "";
        lastPlaybackMethod = "";
        String javaMusicError = "aucune sortie audio Java detectee";
        if (javaAudioAvailable) {
            if (javaMusicController.play()) {
                this.musicEnabled = true;
                lastPlaybackMethod = "Java";
                return true;
            }
            javaMusicError = javaMusicController.getLastError();
        }

        if (windowsMusicController.play()) {
            this.musicEnabled = true;
            lastPlaybackMethod = "Windows";
            return true;
        }

        this.musicEnabled = false;
        lastError = javaMusicError + " ; repli Windows : " + windowsMusicController.getLastError();
        return false;
    }

    boolean isMusicEnabled() {
        return musicEnabled;
    }

    boolean isBackgroundMusicAvailable() {
        return isReadableAudioFile(backgroundMusicFile);
    }

    boolean isWallEffectAvailable() {
        return isReadableAudioFile(wallSoundFile);
    }

    boolean isMoveEffectAvailable() {
        return isReadableAudioFile(moveSoundFile);
    }

    String getLastError() {
        return lastError;
    }

    String getLastPlaybackMethod() {
        return lastPlaybackMethod;
    }

    boolean playWallEffect() {
        return playEffect(wallSoundFile, wallWindowsPlayer);
    }

    boolean playMoveEffect() {
        return playEffect(moveSoundFile, moveWindowsPlayer);
    }

    void close() {
        wallWindowsPlayer.close();
        moveWindowsPlayer.close();
        javaMusicController.close();
        windowsMusicController.close();
    }

    private void preloadAvailableEffects() {
        if (isWallEffectAvailable()) {
            wallWindowsPlayer.start();
        }
        if (isMoveEffectAvailable()) {
            moveWindowsPlayer.start();
        }
    }

    private boolean playEffect(File file, WindowsEffectPlayer windowsPlayer) {
        if (!effectsEnabled) {
            return false;
        }

        lastError = "";
        lastPlaybackMethod = "";
        if (!isReadableAudioFile(file)) {
            lastError = "fichier introuvable : " + file.getPath();
            return false;
        }

        if (javaAudioAvailable && playWithJavaSound(file)) {
            lastPlaybackMethod = "Java";
            return true;
        }

        String javaSoundError = javaAudioAvailable ? lastError : "aucune sortie audio Java detectee";
        if (windowsPlayer.play()) {
            lastError = "";
            lastPlaybackMethod = "Windows";
            return true;
        }

        if (playWithSystemCommand(file)) {
            return true;
        }

        if (!javaSoundError.isEmpty()) {
            lastError = javaSoundError;
        }
        return false;
    }

    private boolean isReadableAudioFile(File file) {
        return file.exists() && file.isFile() && file.canRead();
    }

    private boolean playWithJavaSound(File file) {
        AudioInputStream stream = null;
        SourceDataLine line = null;

        try {
            stream = AudioSystem.getAudioInputStream(file);
            AudioFormat playbackFormat = playbackFormat(stream.getFormat());
            AudioInputStream playbackStream = AudioSystem.getAudioInputStream(playbackFormat, stream);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, playbackFormat);
            if (!AudioSystem.isLineSupported(info)) {
                playbackStream.close();
                lastError = AudioSystem.getMixerInfo().length == 0
                    ? "aucune sortie audio Java detectee"
                    : "sortie audio Java indisponible";
                return false;
            }

            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(playbackFormat);
            SourceDataLine playbackLine = line;
            AudioInputStream activeStream = playbackStream;

            Thread thread = new Thread(() -> playStream(activeStream, playbackLine), "quoridor-java-sound");
            thread.setDaemon(true);
            thread.start();
            return true;
        } catch (Exception exception) {
            closeQuietly(stream);
            if (line != null) {
                line.close();
            }
            lastError = "lecture Java impossible";
            return false;
        }
    }

    private void playStream(AudioInputStream stream, SourceDataLine line) {
        try (AudioInputStream activeStream = stream; SourceDataLine activeLine = line) {
            byte[] buffer = new byte[AUDIO_BUFFER_SIZE];
            int bytesRead;
            activeLine.start();
            while ((bytesRead = activeStream.read(buffer, 0, buffer.length)) != -1) {
                activeLine.write(buffer, 0, bytesRead);
            }
            activeLine.drain();
        } catch (IOException ignored) {
        }
    }

    private AudioFormat playbackFormat(AudioFormat sourceFormat) {
        return new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sourceFormat.getSampleRate(),
            16,
            sourceFormat.getChannels(),
            sourceFormat.getChannels() * 2,
            sourceFormat.getSampleRate(),
            false
        );
    }

    private boolean playWithSystemCommand(File file) {
        String[][] commands = {
            {"paplay", file.getAbsolutePath()},
            {"pw-play", file.getAbsolutePath()},
            {"aplay", "-q", file.getAbsolutePath()},
            {"afplay", file.getAbsolutePath()}
        };

        for (String[] command : commands) {
            if (!isCommandAvailable(command[0])) {
                continue;
            }

            try {
                new ProcessBuilder(command).start();
                lastPlaybackMethod = command[0];
                return true;
            } catch (IOException exception) {
                lastError = "lecteur systeme indisponible : " + command[0];
            }
        }

        if (lastError.isEmpty()) {
            lastError = "aucun lecteur audio systeme trouve";
        }
        return false;
    }

    private boolean isCommandAvailable(String command) {
        String path = System.getenv("PATH");
        if (path == null || path.trim().isEmpty()) {
            return false;
        }

        String[] folders = path.split(File.pathSeparator);
        for (String folder : folders) {
            File candidate = new File(folder, command);
            if (candidate.exists() && candidate.canExecute()) {
                return true;
            }
        }
        return false;
    }

    private void closeQuietly(AudioInputStream stream) {
        if (stream == null) {
            return;
        }

        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    private String toWindowsPath(File file) {
        if (!isCommandAvailable("wslpath")) {
            return null;
        }

        Process process = null;
        try {
            process = new ProcessBuilder("wslpath", "-w", file.getAbsolutePath()).start();
            byte[] output = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return new String(output).trim();
            }
        } catch (Exception ignored) {
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return null;
    }

    private String windowsClassPath() {
        try {
            File codeRoot = new File(AudioManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (codeRoot.isFile()) {
                codeRoot = codeRoot.getParentFile();
            }

            String windowsPath = toWindowsPath(codeRoot);
            if (windowsPath != null && !windowsPath.trim().isEmpty()) {
                return windowsPath;
            }
        } catch (Exception ignored) {
        }

        return toWindowsPath(new File(System.getProperty("user.dir")));
    }

    private File findAudioFile(String path) {
        File directFile = new File(path);
        if (directFile.exists()) {
            return directFile;
        }

        File found = findAudioFileFromBase(new File(System.getProperty("user.dir")), path);
        if (found.exists()) {
            return found;
        }

        try {
            File codeRoot = new File(AudioManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (codeRoot.isFile()) {
                codeRoot = codeRoot.getParentFile();
            }

            found = findAudioFileFromBase(codeRoot, path);
            if (found.exists()) {
                return found;
            }
        } catch (Exception ignored) {
        }

        File projectFile = new File(System.getProperty("user.home"), "PROGHA/groupe-15-Hamzzav2/" + path);
        if (projectFile.exists()) {
            return projectFile;
        }

        return directFile;
    }

    private File findAudioFileFromBase(File base, String path) {
        File current = base;
        while (current != null) {
            File candidate = new File(current, path);
            if (candidate.exists()) {
                return candidate;
            }

            candidate = new File(new File(current, "groupe-15-Hamzzav2"), path);
            if (candidate.exists()) {
                return candidate;
            }

            candidate = new File(new File(current, "prog66"), path);
            if (candidate.exists()) {
                return candidate;
            }

            candidate = new File(new File(new File(current, "prog6"), "prog66"), path);
            if (candidate.exists()) {
                return candidate;
            }

            current = current.getParentFile();
        }

        return new File(path);
    }

    private final class WindowsEffectPlayer {
        private final File file;
        private final String tempFileName;
        private final String threadName;
        private Process process;
        private BufferedWriter writer;

        private WindowsEffectPlayer(File file, String tempFileName, String threadName) {
            this.file = file;
            this.tempFileName = tempFileName;
            this.threadName = threadName;
            this.process = null;
            this.writer = null;
        }

        private boolean play() {
            if (!start()) {
                return false;
            }

            if (writeCommand("play")) {
                return true;
            }

            close();
            return start() && writeCommand("play");
        }

        private boolean start() {
            if (process != null && process.isAlive() && writer != null) {
                return true;
            }

            if (startWindowsJavaSoundPlayer()) {
                return true;
            }

            return startPowerShellSoundPlayer();
        }

        private boolean startWindowsJavaSoundPlayer() {
            if (!isCommandAvailable("java.exe")) {
                return false;
            }

            String windowsAudioPath = toWindowsPath(file);
            String windowsClassPath = windowsClassPath();
            if (windowsAudioPath == null || windowsAudioPath.trim().isEmpty()
                    || windowsClassPath == null || windowsClassPath.trim().isEmpty()) {
                return false;
            }

            try {
                process = new ProcessBuilder(
                    "java.exe",
                    "-cp",
                    windowsClassPath,
                    "controleur.WindowsAudioPlayer",
                    windowsAudioPath
                )
                    .redirectErrorStream(true)
                    .start();
                writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(),
                    StandardCharsets.UTF_8
                ));
                CompletableFuture<String> readyLine = CompletableFuture.supplyAsync(() -> readLineQuietly(reader));
                String line = readyLine.get(1500, TimeUnit.MILLISECONDS);
                if (!"ready".equals(line)) {
                    close();
                    return false;
                }

                Thread drainThread = new Thread(() -> drainReader(reader), threadName + "-windows-java");
                drainThread.setDaemon(true);
                drainThread.start();
                return true;
            } catch (Exception exception) {
                close();
                return false;
            }
        }

        private boolean startPowerShellSoundPlayer() {
            String windowsPath = toWindowsPath(file);
            if (windowsPath == null || windowsPath.trim().isEmpty()) {
                return false;
            }

            String command = isCommandAvailable("powershell.exe") ? "powershell.exe" : null;
            if (command == null && isCommandAvailable("pwsh.exe")) {
                command = "pwsh.exe";
            }
            if (command == null) {
                return false;
            }

            String escapedPath = windowsPath.replace("'", "''");
            String escapedTempFileName = tempFileName.replace("'", "''");
            String script = ""
                + "$source='" + escapedPath + "'; "
                + "$target=Join-Path $env:TEMP '" + escapedTempFileName + "'; "
                + "Copy-Item $source $target -Force; "
                + "Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; "
                + "public static class QuoridorWinSound { "
                + "[DllImport(\"winmm.dll\", SetLastError=true)] "
                + "public static extern bool PlaySound(byte[] sound, IntPtr module, uint flags); }'; "
                + "$bytes=[System.IO.File]::ReadAllBytes($target); "
                + "$flags=0x0004 -bor 0x0002; "
                + "while (($line=[Console]::In.ReadLine()) -ne $null) { "
                + "if ($line -eq 'exit') { break } "
                + "if ($line -eq 'play') { [QuoridorWinSound]::PlaySound($bytes, [IntPtr]::Zero, $flags) | Out-Null } "
                + "}";

            try {
                process = new ProcessBuilder(command, "-NoProfile", "-Command", script)
                    .redirectErrorStream(true)
                    .start();
                writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
                Thread drainThread = new Thread(() -> drainProcess(process), threadName + "-powershell");
                drainThread.setDaemon(true);
                drainThread.start();
                return true;
            } catch (IOException exception) {
                close();
                lastError = "lecture Windows impossible";
                return false;
            }
        }

        private boolean writeCommand(String command) {
            if (writer == null) {
                return false;
            }

            try {
                writer.write(command);
                writer.newLine();
                writer.flush();
                return true;
            } catch (IOException exception) {
                lastError = "commande audio Windows impossible";
                return false;
            }
        }

        private void close() {
            if (writer != null) {
                try {
                    writer.write("exit");
                    writer.newLine();
                    writer.flush();
                    writer.close();
                } catch (IOException ignored) {
                }
            }

            if (process != null) {
                process.destroy();
            }

            writer = null;
            process = null;
        }
    }

    private final class JavaMusicController {
        private final File file;
        private Clip clip;
        private String lastControllerError;

        private JavaMusicController(File file) {
            this.file = file;
            this.clip = null;
            this.lastControllerError = "";
        }

        private boolean play() {
            if (!isReadableAudioFile(file)) {
                lastControllerError = "musique introuvable : " + file.getPath();
                return false;
            }

            lastControllerError = "";
            try {
                if (clip == null || !clip.isOpen()) {
                    openClip();
                }
                if (!clip.isRunning()) {
                    if (clip.getFramePosition() >= clip.getFrameLength()) {
                        clip.setFramePosition(0);
                    }
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                }
                return true;
            } catch (Exception exception) {
                close();
                if (lastControllerError.isEmpty()) {
                    lastControllerError = "lecture musique Java impossible";
                }
                return false;
            }
        }

        private void openClip() throws Exception {
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(file);
                    AudioInputStream playbackStream = AudioSystem.getAudioInputStream(
                        playbackFormat(stream.getFormat()), stream)) {
                DataLine.Info info = new DataLine.Info(Clip.class, playbackStream.getFormat());
                if (!AudioSystem.isLineSupported(info)) {
                    lastControllerError = "sortie musique Java indisponible";
                    throw new IOException(lastControllerError);
                }
                clip = (Clip) AudioSystem.getLine(info);
                clip.open(playbackStream);
            }
        }

        private String getLastError() {
            return lastControllerError;
        }

        private void stop() {
            if (clip != null && clip.isOpen()) {
                clip.stop();
                clip.setFramePosition(0);
            }
        }

        private void close() {
            if (clip != null) {
                clip.stop();
                clip.close();
                clip = null;
            }
        }
    }

    private final class WindowsMusicController {
        private final File file;
        private Process process;
        private BufferedWriter writer;
        private String lastControllerError;

        private WindowsMusicController(File file) {
            this.file = file;
            this.process = null;
            this.writer = null;
            this.lastControllerError = "";
        }

        private boolean play() {
            if (!isReadableAudioFile(file)) {
                lastControllerError = "musique introuvable : " + file.getPath();
                return false;
            }
            if (!start()) {
                return false;
            }
            return writeCommand("play");
        }

        private void stop() {
            writeCommand("stop");
        }

        private String getLastError() {
            return lastControllerError;
        }

        private boolean start() {
            if (process != null && process.isAlive() && writer != null) {
                return true;
            }

            if (!isCommandAvailable("java.exe")) {
                lastControllerError = "java.exe Windows indisponible";
                return false;
            }

            String windowsAudioPath = toWindowsPath(file);
            String windowsClassPath = windowsClassPath();
            if (windowsAudioPath == null || windowsAudioPath.trim().isEmpty()
                    || windowsClassPath == null || windowsClassPath.trim().isEmpty()) {
                lastControllerError = "chemin Windows indisponible";
                return false;
            }

            try {
                process = new ProcessBuilder(
                    "java.exe",
                    "-cp",
                    windowsClassPath,
                    "controleur.WindowsMusicPlayer",
                    windowsAudioPath
                )
                    .redirectErrorStream(true)
                    .start();
                writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                CompletableFuture<String> readyLine = CompletableFuture.supplyAsync(() -> readLineQuietly(reader));
                String line = readyLine.get(2500, TimeUnit.MILLISECONDS);
                if (!"ready".equals(line)) {
                    close();
                    lastControllerError = "lecteur musique Windows indisponible";
                    return false;
                }

                Thread drainThread = new Thread(() -> drainReader(reader), "quoridor-background-music");
                drainThread.setDaemon(true);
                drainThread.start();
                return true;
            } catch (Exception exception) {
                close();
                lastControllerError = "lecture musique Windows impossible";
                return false;
            }
        }

        private boolean writeCommand(String command) {
            if (writer == null) {
                lastControllerError = "commande musique indisponible";
                return false;
            }

            try {
                writer.write(command);
                writer.newLine();
                writer.flush();
                return true;
            } catch (IOException exception) {
                lastControllerError = "commande musique Windows impossible";
                return false;
            }
        }

        private void close() {
            if (writer != null) {
                try {
                    writer.write("exit");
                    writer.newLine();
                    writer.flush();
                    writer.close();
                } catch (IOException ignored) {
                }
            }

            if (process != null) {
                process.destroy();
            }

            writer = null;
            process = null;
        }
    }

    private String readLineQuietly(BufferedReader reader) {
        try {
            return reader.readLine();
        } catch (IOException exception) {
            return null;
        }
    }

    private void drainReader(BufferedReader reader) {
        try {
            while (reader.readLine() != null) {
            }
        } catch (IOException ignored) {
        }
    }

    private void drainProcess(Process process) {
        try {
            process.getInputStream().readAllBytes();
            process.waitFor();
        } catch (Exception ignored) {
        } finally {
            process.destroy();
        }
    }
}
