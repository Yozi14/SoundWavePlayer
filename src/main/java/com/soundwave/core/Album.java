package com.soundwave.core;

import java.util.List;

/**
 * Модель альбома, группирующая песни
 */
public record Album(String title, String artistName, List<Song> songs) {
    // Удобный метод, чтобы сразу видеть количество треков
    public int trackCount() {
        return songs.size();
    }

    @Override
    public String toString() {
        return title;
    }
}