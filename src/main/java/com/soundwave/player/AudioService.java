package com.soundwave.player;

import com.soundwave.core.Song;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.media.EqualizerBand; // Правильный импорт
import javafx.util.Duration;

public interface AudioService {
    void play(Song song);
    void pause();
    void resume();
    void stop();
    void seek(Duration duration);
    void setOnEndOfMedia(Runnable action);
    
    boolean isPlaying();
    
    DoubleProperty volumeProperty();
    ReadOnlyObjectProperty<Duration> currentTimeProperty();
    
    // Используем EqualizerBand
    ObservableList<EqualizerBand> getEqualizerBands();
}