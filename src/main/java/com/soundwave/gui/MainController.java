package com.soundwave.gui;

import com.soundwave.core.*;
import com.soundwave.library.LibraryManager;
import com.soundwave.player.AudioService;
import com.soundwave.player.JavaFXAudioService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.EqualizerBand;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {

    @FXML private FlowPane albumGrid;
    @FXML private TextField searchField;
    @FXML private Label currentTitle, currentArtist, currentTimeLabel, totalTimeLabel, clockLabel;
    @FXML private Slider progressSlider, volumeSlider;
    @FXML private Button playBtn, likeBtn, eqMenuBtn; 
    @FXML private ImageView currentAlbumArt;
    @FXML private SVGPath likeIcon;

    private AudioService audioService;
    private LibraryManager libraryManager;
    private final ObservableList<Song> songList = FXCollections.observableArrayList();
    private final List<Song> favoriteSongs = new ArrayList<>(); 
    private final List<Song> selectedSongs = new ArrayList<>(); // Список для множественного выбора
    private Song currentlyPlayingSong;
    private List<Song> activeQueue = new ArrayList<>();
    
    private ContextMenu eqPopup;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            audioService = new JavaFXAudioService();
            libraryManager = new LibraryManager();
            setupVolume();
            setupRewindLogic();
            setupSearch();
            startClock();
            loadLibraryAsync(); 
            audioService.setOnEndOfMedia(() -> Platform.runLater(this::handleNext));
        });
    }

    // --- ЛОГИКА МНОЖЕСТВЕННОГО ВЫБОРА ---

    private void handleSongClick(MouseEvent event, Song song, VBox card) {
        if (event.getButton() == MouseButton.PRIMARY) {
            if (event.isControlDown()) {
                // Ctrl + Click: добавить/убрать из выделения
                if (selectedSongs.contains(song)) {
                    selectedSongs.remove(song);
                    updateCardStyle(card, false);
                } else {
                    selectedSongs.add(song);
                    updateCardStyle(card, true);
                }
            } else {
                // Обычный клик: играть песню и сбросить выделение
                clearSelection();
                playSong(song);
            }
        } else if (event.getButton() == MouseButton.SECONDARY) {
            // Правый клик: если песня не в выделении, выделяем только её
            if (!selectedSongs.contains(song)) {
                clearSelection();
                selectedSongs.add(song);
                updateCardStyle(card, true);
            }
            showSongContextMenu(event, song);
        }
    }

    private void clearSelection() {
        selectedSongs.clear();
        albumGrid.getChildren().forEach(node -> {
            if (node instanceof VBox) updateCardStyle((VBox) node, false);
        });
    }

    private void updateCardStyle(VBox card, boolean isSelected) {
        if (isSelected) {
            card.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 15; -fx-border-color: #ff5500; -fx-border-width: 2; -fx-border-radius: 15;");
        } else {
            card.setStyle("-fx-background-color: #111; -fx-background-radius: 15; -fx-border-color: transparent;");
        }
    }

    // --- КОНТЕКСТНОЕ МЕНЮ (С ПОДДЕРЖКОЙ МНОЖЕСТВА ПЕСЕН) ---

    private void showSongContextMenu(MouseEvent event, Song song) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("custom-context-menu");

        int count = selectedSongs.size();
        String suffix = count > 1 ? " (" + count + ")" : "";

        MenuItem play = new MenuItem("Воспроизвести" + (count > 1 ? " выбранные" : ""));
        play.setOnAction(e -> {
            if (count > 1) {
                activeQueue = new ArrayList<>(selectedSongs);
                playSong(activeQueue.get(0));
            } else {
                playSong(song);
            }
        });

        Menu addToPlaylistMenu = new Menu("Добавить в плейлист" + suffix);
        for (Playlist p : libraryManager.getPlaylists()) {
            MenuItem pItem = new MenuItem(p.getName());
            pItem.setOnAction(e -> {
                for (Song s : selectedSongs) {
                    libraryManager.addSongToPlaylist(p.getName(), s);
                }
                clearSelection();
            });
            addToPlaylistMenu.getItems().add(pItem);
        }

        MenuItem delete = new MenuItem("Удалить из библиотеки" + suffix);
        delete.getStyleClass().add("menu-item-delete");
        delete.setOnAction(e -> {
            songList.removeAll(selectedSongs);
            favoriteSongs.removeAll(selectedSongs);
            selectedSongs.forEach(libraryManager::removeSong);
            clearSelection();
            showAllSongs();
        });

        menu.getItems().addAll(play, new SeparatorMenuItem(), addToPlaylistMenu, delete);
        menu.show(albumGrid, event.getScreenX(), event.getScreenY());
    }

    // --- ЭКВАЛАЙЗЕР (НЕОНОВЫЙ СТИЛЬ) ---

    @FXML
    private void handleEqPopup() {
        if (eqPopup == null) {
            eqPopup = new ContextMenu();
            eqPopup.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
        }
        eqPopup.getItems().clear();

        VBox mainLayout = new VBox(15);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #0d0d0d; -fx-background-radius: 15; -fx-border-color: #ff5500; -fx-border-radius: 15; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(255,85,0,0.4), 15, 0, 0, 0);");

        HBox slidersContainer = new HBox(12);
        slidersContainer.setAlignment(Pos.CENTER);
        
        ObservableList<EqualizerBand> bands = audioService.getEqualizerBands();
        if (bands != null) {
            for (EqualizerBand band : bands) {
                Slider s = new Slider(-24, 12, band.getGain());
                s.setOrientation(javafx.geometry.Orientation.VERTICAL);
                s.setPrefHeight(140);
                s.getStyleClass().add("slider");
                s.valueProperty().addListener((obs, old, val) -> band.setGain(val.doubleValue()));
                
                VBox col = new VBox(5, s, new Label(formatFrequency(band.getCenterFrequency())));
                col.setAlignment(Pos.CENTER);
                slidersContainer.getChildren().add(col);
            }
        }

        Button resetBtn = new Button("СБРОСИТЬ");
        resetBtn.setStyle("-fx-background-color: #222; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> { if(bands != null) bands.forEach(b -> b.setGain(0)); handleEqPopup(); });

        mainLayout.getChildren().addAll(slidersContainer, resetBtn);
        eqPopup.getItems().add(new CustomMenuItem(mainLayout));
        eqPopup.show(eqMenuBtn, Side.BOTTOM, -220, 10);
    }

    // --- ПЛЕЙЛИСТЫ И СЕТКА ---

    @FXML
    private void showPlaylists() {
        albumGrid.getChildren().clear();
        selectedSongs.clear();

        VBox createCard = createBaseCard("+", "Создать плейлист");
        createCard.setStyle("-fx-background-color: #1a1a1a; -fx-background-radius: 15; -fx-border-color: #ff5500; -fx-border-style: dashed; -fx-border-radius: 15; -fx-border-width: 2;");
        createCard.setOnMouseClicked(e -> handleCreatePlaylist());
        albumGrid.getChildren().add(createCard);

        for (Playlist p : libraryManager.getPlaylists()) {
            VBox card = createBaseCard(p.getName(), p.getSongPaths().size() + " треков");
            card.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    List<Song> filtered = songList.stream()
                            .filter(s -> p.getSongPaths().contains(s.filePath().toString()))
                            .toList();
                    updateGrid(filtered);
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    showPlaylistContextMenu(e, p);
                }
            });
            albumGrid.getChildren().add(card);
        }
    }

    private void updateGrid(List<Song> songs) {
        albumGrid.getChildren().clear();
        selectedSongs.clear();
        for (Song song : songs) {
            VBox card = createBaseCard(song.title(), song.artist());
            card.setOnMouseClicked(e -> handleSongClick(e, song, card));
            albumGrid.getChildren().add(card);
        }
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ (БЕЗ ИЗМЕНЕНИЙ) ---

    private void playSong(Song song) {
        if (song == null) return;
        currentlyPlayingSong = song;
        activeQueue = new ArrayList<>(songList);
        audioService.play(song);
        currentTitle.setText(song.title());
        currentArtist.setText(song.artist());
        playBtn.setText("⏸");
        totalTimeLabel.setText(song.durationFormatted());
        updateLikeButtonIcon();
        
        File folder = song.filePath().toFile().getParentFile();
        File cover = new File(folder, "cover.jpg");
        currentAlbumArt.setImage(cover.exists() ? new Image(cover.toURI().toString()) : null);

        audioService.currentTimeProperty().addListener((obs, old, newTime) -> {
            if (newTime != null) {
                double progress = (newTime.toSeconds() / (double) song.durationSeconds()) * 100;
                progressSlider.setValue(progress);
                currentTimeLabel.setText(formatDuration(newTime));
            }
        });
    }

    private VBox createBaseCard(String title, String subtitle) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setPrefWidth(160);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #111; -fx-background-radius: 15; -fx-cursor: hand;");
        Label t = new Label(title); t.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;");
        Label s = new Label(subtitle); s.setStyle("-fx-text-fill: #777; -fx-font-size: 11;");
        card.getChildren().addAll(t, s);
        return card;
    }

    private void handleCreatePlaylist() {
        TextInputDialog dialog = new TextInputDialog("Мой плейлист");
        dialog.setTitle("SoundWave"); dialog.setHeaderText(null); dialog.setContentText("Название:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) { libraryManager.createPlaylist(name); showPlaylists(); }
        });
    }

    private void showPlaylistContextMenu(MouseEvent event, Playlist p) {
        ContextMenu pMenu = new ContextMenu();
        pMenu.getStyleClass().add("custom-context-menu");
        MenuItem del = new MenuItem("Удалить плейлист");
        del.setOnAction(ev -> { libraryManager.deletePlaylist(p); showPlaylists(); });
        pMenu.getItems().add(del);
        pMenu.show(albumGrid, event.getScreenX(), event.getScreenY());
    }

    @FXML private void handlePlayPause() {
        if (audioService.isPlaying()) { audioService.pause(); playBtn.setText("▶"); }
        else { audioService.resume(); playBtn.setText("⏸"); }
    }

    @FXML private void handleNext() {
        if (activeQueue.isEmpty()) return;
        int idx = activeQueue.indexOf(currentlyPlayingSong);
        playSong(activeQueue.get((idx + 1) % activeQueue.size()));
    }

    @FXML private void handlePrevious() {
        if (activeQueue.isEmpty()) return;
        int idx = activeQueue.indexOf(currentlyPlayingSong);
        playSong(activeQueue.get((idx - 1 + activeQueue.size()) % activeQueue.size()));
    }

    @FXML private void handleLike() {
        if (currentlyPlayingSong == null) return;
        boolean removed = favoriteSongs.removeIf(s -> s.filePath().equals(currentlyPlayingSong.filePath()));
        if (!removed) favoriteSongs.add(currentlyPlayingSong);
        updateLikeButtonIcon();
        libraryManager.saveFavorites(favoriteSongs);
    }

    private void updateLikeButtonIcon() {
        Platform.runLater(() -> {
            if (currentlyPlayingSong == null || likeIcon == null) return;
            boolean isLiked = favoriteSongs.stream().anyMatch(s -> s.filePath().equals(currentlyPlayingSong.filePath()));
            likeIcon.setFill(isLiked ? Color.web("#ff5500") : Color.TRANSPARENT);
            likeIcon.setStroke(isLiked ? Color.web("#ff5500") : Color.WHITE);
        });
    }

    @FXML private void showFavorites() { updateGrid(new ArrayList<>(favoriteSongs)); }
    @FXML private void showAllSongs() { updateGrid(songList); }
    @FXML private void showArtists() {
        albumGrid.getChildren().clear();
        for (Artist a : libraryManager.getArtistsHierarchy(songList)) {
            VBox card = createBaseCard(a.name(), a.albums().size() + " альбомов");
            card.setOnMouseClicked(e -> {
                albumGrid.getChildren().clear();
                for (Album al : a.albums()) {
                    VBox ac = createBaseCard(al.title(), al.artistName());
                    ac.setOnMouseClicked(ev -> updateGrid(al.songs()));
                    albumGrid.getChildren().add(ac);
                }
            });
            albumGrid.getChildren().add(card);
        }
    }

    @FXML private void handleOpenDirectory() {
        File dir = new DirectoryChooser().showDialog(null);
        if (dir != null) { songList.setAll(libraryManager.scanDirectory(dir.toPath())); showAllSongs(); }
    }

    @FXML 
private void handleImportSongs() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Импорт треков");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("MP3 Files", "*.mp3")
    );
    
    List<File> files = fileChooser.showOpenMultipleDialog(null);
    if (files != null) {
        // Импортируем каждый файл
        files.forEach(libraryManager::importTrackAuto);
        
        // ВАЖНО: Вместо сложной цепочки просто обновляем основной список
        // и сразу перерисовываем сетку
        songList.setAll(libraryManager.loadPersistedLibrary());
        showAllSongs(); 
        
        System.out.println("Импортировано файлов: " + files.size());
    }
}

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, val) -> {
            if (val.isEmpty()) updateGrid(songList);
            else updateGrid(songList.stream().filter(s -> s.title().toLowerCase().contains(val.toLowerCase())).toList());
        });
    }

    private void setupRewindLogic() {
        progressSlider.setOnMousePressed(e -> {
            if (currentlyPlayingSong != null) {
                double pct = (e.getX() / progressSlider.getWidth());
                audioService.seek(Duration.seconds(pct * currentlyPlayingSong.durationSeconds()));
            }
        });
    }

    private void setupVolume() {
        volumeSlider.setValue(50);
        volumeSlider.valueProperty().addListener((obs, old, val) -> audioService.volumeProperty().set(val.doubleValue() / 100.0));
    }

    private void loadLibraryAsync() {
        songList.setAll(libraryManager.loadPersistedLibrary());
        List<String> favPaths = libraryManager.loadFavoritesPaths();
        favoriteSongs.clear();
        songList.forEach(s -> { if (favPaths.contains(s.filePath().toString())) favoriteSongs.add(s); });
        showAllSongs();
    }

    private void startClock() {
        Thread t = new Thread(() -> {
            while (true) {
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                Platform.runLater(() -> { if (clockLabel != null) clockLabel.setText(time); });
                try { Thread.sleep(15000); } catch (Exception e) { break; }
            }
        });
        t.setDaemon(true); t.start();
    }

    private String formatFrequency(double f) { return f < 1000 ? (int)f + "Hz" : String.format("%.1fkHz", f/1000); }
    private String formatDuration(Duration d) { return String.format("%d:%02d", (int)d.toSeconds()/60, (int)d.toSeconds()%60); }
}