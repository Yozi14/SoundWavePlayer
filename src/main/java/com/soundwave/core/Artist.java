package com.soundwave.core;

import java.util.List;

/**
 * Модель исполнителя, группирующая альбомы
 */
public record Artist(String name, List<Album> albums) {
    @Override
    public String toString() {
        return name;
    }
}