package com.soundwave.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable {
    private static final long serialVersionUID = 1L; // Хорошая практика для Serializable
    
    private String name;
    private List<String> songPaths;

    public Playlist(String name) {
        this.name = name;
        this.songPaths = new ArrayList<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getSongPaths() {
        return songPaths;
    }

    public void addSong(String path) {
        if (!songPaths.contains(path)) {
            songPaths.add(path);
        }
    }
}