package com.soundwave.player;

import com.soundwave.core.Song;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.media.EqualizerBand; // Исправлено
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class JavaFXAudioService implements AudioService {
    private MediaPlayer mediaPlayer;
    private final DoubleProperty volume = new SimpleDoubleProperty(0.5);
    private Runnable endOfMediaAction;

    @Override
    public void play(Song song) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        try {
            Media media = new Media(song.filePath().toUri().toString());
            mediaPlayer = new MediaPlayer(media);
            
            mediaPlayer.volumeProperty().bind(volume);
            
            if (endOfMediaAction != null) {
                mediaPlayer.setOnEndOfMedia(endOfMediaAction);
            }

            mediaPlayer.play();
        } catch (Exception e) {
            System.err.println("Ошибка воспроизведения: " + e.getMessage());
        }
    }

    @Override
    public void pause() { if (mediaPlayer != null) mediaPlayer.pause(); }

    @Override
    public void resume() { if (mediaPlayer != null) mediaPlayer.play(); }

    @Override
    public void stop() { if (mediaPlayer != null) mediaPlayer.stop(); }

    @Override
    public void seek(Duration duration) { if (mediaPlayer != null) mediaPlayer.seek(duration); }

    @Override
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    @Override
    public void setOnEndOfMedia(Runnable action) {
        this.endOfMediaAction = action;
        if (mediaPlayer != null) {
            mediaPlayer.setOnEndOfMedia(action);
        }
    }

    @Override
    public DoubleProperty volumeProperty() { return volume; }

    @Override
    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return mediaPlayer != null ? mediaPlayer.currentTimeProperty() : null;
    }

    @Override
    public ObservableList<EqualizerBand> getEqualizerBands() {
        // Возвращаем полосы из текущего mediaPlayer
        return mediaPlayer != null ? mediaPlayer.getAudioEqualizer().getBands() : null;
    }
}