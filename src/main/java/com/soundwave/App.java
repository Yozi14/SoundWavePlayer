package com.soundwave;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        System.out.println("[DEBUG] Приложение запускается...");
        
        String fxmlPath = "/com/soundwave/gui/main-view.fxml";
        URL fxmlLocation = getClass().getResource(fxmlPath);

        if (fxmlLocation == null) {
            System.err.println("[ERROR] ФАЙЛ НЕ НАЙДЕН: " + fxmlPath);
            return;
        }

        try {
            System.out.println("[DEBUG] Файл найден: " + fxmlLocation);
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            
            // --- УСТАНОВКА ИКОНКИ ОКНА ---
            try {
                String iconPath = "/com/soundwave/gui/icon.png";
                URL iconUrl = getClass().getResource(iconPath);
                if (iconUrl != null) {
                    stage.getIcons().add(new Image(iconUrl.toString()));
                    System.out.println("[DEBUG] Иконка загружена успешно!");
                } else {
                    System.out.println("[DEBUG] Файл иконки не найден в ресурсах: " + iconPath);
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Ошибка при установке иконки: " + e.getMessage());
            }
            // -----------------------------

            Scene scene = new Scene(root);
            stage.setTitle("SoundWave Player");
            stage.setScene(scene);
            stage.show();
            
            System.out.println("[DEBUG] Окно успешно отображено!");
            
        } catch (Exception e) {
            System.err.println("[ERROR] Ошибка внутри FXML или Контроллера:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}