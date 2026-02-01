package com.soundwave.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;

/**
 * Неизменяемая модель данных для музыкального трека.
 * Использует Java Record для краткости.
 */
public record Song(
    @JsonProperty("filePath") Path filePath,
    @JsonProperty("title") String title,
    @JsonProperty("artist") String artist,
    @JsonProperty("album") String album,
    @JsonProperty("durationFormatted") String durationFormatted,
    @JsonProperty("durationSeconds") long durationSeconds
) {    /**
     * Кастомный компактный конструктор для валидации и обработки пустых значений.
     */
    public Song {
        if (title == null || title.isBlank()) {
            title = (filePath != null) ? filePath.getFileName().toString() : "Unknown Title";
        }
        if (artist == null || artist.isBlank()) {
            artist = "Unknown Artist";
        }
        if (album == null || album.isBlank()) {
            album = "Unknown Album";
        }
    }

    // Вспомогательный метод для отображения в логах или UI
    public String getFullDisplayTitle() {
        return artist + " - " + title;
    }
    
}