package com.soundwave.library;

import com.soundwave.core.Album;
import com.soundwave.core.Artist;
import com.soundwave.core.Playlist;
import com.soundwave.core.Song;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LibraryManager {
    private static final Logger logger = LoggerFactory.getLogger(LibraryManager.class);
    private final LibraryRepository repository = new LibraryRepository();
    private List<Song> currentSongs = new ArrayList<>();
    private List<Playlist> playlists = new ArrayList<>();
    
    // --- ПРАВИЛЬНЫЕ ПУТИ ДЛЯ EXE ---
    // Данные теперь хранятся в C:\Users\Имя\AppData\Roaming\SoundWave
    private static final String STORAGE_DIR = System.getProperty("user.home") + File.separator + ".soundwave";
    private static final String PLAYLISTS_FILE = STORAGE_DIR + File.separator + "playlists.dat";
    private static final String FAVORITES_FILE = STORAGE_DIR + File.separator + "favorites.txt";

    public LibraryManager() {
        ensureStorageDirectory();
        loadPlaylists();
    }

    private void ensureStorageDirectory() {
        try {
            Files.createDirectories(Paths.get(STORAGE_DIR));
            // Создаем также папку для импортированных артистов
            Files.createDirectories(Paths.get(STORAGE_DIR, "artists"));
            logger.info("Директория данных: {}", STORAGE_DIR);
        } catch (IOException e) {
            logger.error("Не удалось создать директорию хранилища", e);
        }
    }

    public List<Song> loadPersistedLibrary() {
        List<Song> loaded = repository.load();
        this.currentSongs = (loaded != null) ? new ArrayList<>(loaded) : new ArrayList<>();
        logger.info("Загружено из памяти: {} треков", currentSongs.size());
        return currentSongs;
    }

    // --- УПРАВЛЕНИЕ ПЕСНЯМИ ---

    public void removeSong(Song song) {
        String pathStr = song.filePath().toString();
        currentSongs.removeIf(s -> s.filePath().toString().equals(pathStr));
        
        for (Playlist p : playlists) {
            p.getSongPaths().remove(pathStr);
        }
        
        updateLibrary(currentSongs);
        savePlaylists();
        logger.info("Песня удалена из библиотеки: {}", song.title());
    }

    public boolean deleteSongTotally(Song song) {
        try {
            Files.deleteIfExists(song.filePath());
            removeSong(song);
            return true; 
        } catch (IOException e) {
            logger.error("Не удалось удалить файл: {}", song.filePath(), e);
            return false;
        }
    }

    // --- УПРАВЛЕНИЕ ЛЮБИМЫМИ ---

    public void saveFavorites(List<Song> favorites) {
        try {
            List<String> paths = favorites.stream()
                    .map(s -> s.filePath().toString())
                    .collect(Collectors.toList());
            Files.write(Paths.get(FAVORITES_FILE), paths);
        } catch (IOException e) {
            logger.error("Ошибка при сохранении любимых треков", e);
        }
    }

    public List<String> loadFavoritesPaths() {
        try {
            Path path = Paths.get(FAVORITES_FILE);
            if (Files.exists(path)) return Files.readAllLines(path);
        } catch (IOException e) {
            logger.error("Ошибка при загрузке любимых треков", e);
        }
        return new ArrayList<>();
    }

    // --- УПРАВЛЕНИЕ ПЛЕЙЛИСТАМИ ---

    private void savePlaylists() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(PLAYLISTS_FILE))) {
            oos.writeObject(playlists);
            logger.info("Плейлисты синхронизированы.");
        } catch (IOException e) {
            logger.error("Ошибка при сохранении плейлистов", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadPlaylists() {
        File file = new File(PLAYLISTS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                playlists = (List<Playlist>) ois.readObject();
                logger.info("Загружено плейлистов: {}", playlists.size());
            } catch (Exception e) {
                logger.error("Ошибка при загрузке плейлистов", e);
                playlists = new ArrayList<>();
            }
        }
    }

    public List<Playlist> getPlaylists() { return playlists; }

    public void createPlaylist(String name) {
        if (playlists.stream().noneMatch(p -> p.getName().equalsIgnoreCase(name))) {
            playlists.add(new Playlist(name));
            savePlaylists();
            logger.info("Создан плейлист: {}", name);
        }
    }

    public void deletePlaylist(Playlist playlist) {
        if (playlists.remove(playlist)) {
            savePlaylists();
            logger.info("Плейлист удален.");
        }
    }

    public void addSongToPlaylist(String playlistName, Song song) {
        playlists.stream()
                .filter(p -> p.getName().equals(playlistName))
                .findFirst()
                .ifPresent(p -> {
                    String path = song.filePath().toString();
                    if (!p.getSongPaths().contains(path)) {
                        p.addSong(path);
                        savePlaylists();
                        logger.info("Песня добавлена в плейлист {}", playlistName);
                    }
                });
    }

    // --- ИМПОРТ И СКАН ---

    public void importTrackAuto(File sourceFile) {
        try {
            AudioFile f = AudioFileIO.read(sourceFile);
            Tag tag = f.getTag();
            String artist = (tag != null && !tag.getFirst(FieldKey.ARTIST).isEmpty()) 
                    ? tag.getFirst(FieldKey.ARTIST) : "Unknown Artist";
            String album = (tag != null && !tag.getFirst(FieldKey.ALBUM).isEmpty()) 
                    ? tag.getFirst(FieldKey.ALBUM) : "Unknown Album";
            
            artist = artist.replaceAll("[\\\\/:*?\"<>|]", "_");
            album = album.replaceAll("[\\\\/:*?\"<>|]", "_");

            // Копируем файлы в личную папку пользователя, где всегда есть доступ
            Path targetDir = Paths.get(STORAGE_DIR, "artists", artist, album);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(sourceFile.getName());
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            Song newSong = extractMetadata(targetPath);
            currentSongs.removeIf(s -> s.filePath().toString().equals(newSong.filePath().toString()));
            currentSongs.add(newSong);
            
            updateLibrary(currentSongs);
            logger.info("Трек импортирован в хранилище: {}", newSong.title());
        } catch (Exception e) {
            logger.error("Ошибка авто-импорта: " + sourceFile.getName(), e);
        }
    }

    public List<Song> scanDirectory(Path rootPath) {
        try (Stream<Path> paths = Files.walk(rootPath)) {
            List<Song> scannedSongs = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedAudioFile)
                    .map(this::extractMetadata)
                    .toList();

            for (Song newSong : scannedSongs) {
                currentSongs.removeIf(s -> s.filePath().toString().equals(newSong.filePath().toString()));
                currentSongs.add(newSong);
            }
            updateLibrary(currentSongs);
            return currentSongs;
        } catch (IOException e) {
            logger.error("Ошибка сканирования", e);
            return currentSongs;
        }
    }

    public Song extractMetadata(Path path) {
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

            return new Song(path, title, artist, album, formatDuration(durationSeconds), durationSeconds);
        } catch (Exception e) {
            return new Song(path, path.getFileName().toString(), "Unknown", "Unknown", "0:00", 0);
        }
    }

    public void updateLibrary(List<Song> newSongs) {
        this.currentSongs = new ArrayList<>(newSongs);
        repository.save(this.currentSongs);
    }

    public List<Artist> getArtistsHierarchy(List<Song> allSongs) {
        return allSongs.stream()
                .collect(Collectors.groupingBy(Song::artist,
                        Collectors.groupingBy(Song::album)))
                .entrySet().stream()
                .map(artistEntry -> {
                    List<Album> albums = artistEntry.getValue().entrySet().stream()
                            .map(albumEntry -> new Album(albumEntry.getKey(), artistEntry.getKey(), albumEntry.getValue()))
                            .toList();
                    return new Artist(artistEntry.getKey(), albums);
                })
                .toList();
    }

    private boolean isSupportedAudioFile(Path path) {
        String name = path.toString().toLowerCase();
        return name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac") || name.endsWith(".m4a");
    }

    private String formatDuration(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}