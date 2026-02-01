package com.soundwave.library;

import com.soundwave.core.Song;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class LibraryRepository {
    private static final String STORAGE_FILE = "library.txt";

    public void save(List<Song> songs) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(STORAGE_FILE))) {
            for (Song song : songs) {
                // Используем правильное имя метода filePath() из твоего Record
                writer.println(song.filePath().toAbsolutePath().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Song> load() {
        List<Song> songs = new ArrayList<>();
        File file = new File(STORAGE_FILE);
        if (!file.exists()) return songs;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Path p = Paths.get(line);
                if (Files.exists(p)) {
                    // Извлекаем метаданные напрямую, чтобы избежать зацикливания с LibraryManager
                    songs.add(quickExtract(p));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return songs;
    }

    /**
     * Быстрое извлечение метаданных для восстановления списка
     */
    private Song quickExtract(Path path) {
        try {
            AudioFile f = AudioFileIO.read(path.toFile());
            Tag tag = f.getTag();
            int durationSeconds = f.getAudioHeader().getTrackLength();

            String title = (tag != null && !tag.getFirst(FieldKey.TITLE).isEmpty()) 
                    ? tag.getFirst(FieldKey.TITLE) : path.getFileName().toString();
            String artist = (tag != null && !tag.getFirst(FieldKey.ARTIST).isEmpty()) 
                    ? tag.getFirst(FieldKey.ARTIST) : "Unknown Artist";
            String album = (tag != null && !tag.getFirst(FieldKey.ALBUM).isEmpty()) 
                    ? tag.getFirst(FieldKey.ALBUM) : "Unknown Album";

            return new Song(path, title, artist, album, 
                    String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60), 
                    durationSeconds);
        } catch (Exception e) {
            return new Song(path, path.getFileName().toString(), "Unknown", "Unknown", "0:00", 0);
        }
    }
}