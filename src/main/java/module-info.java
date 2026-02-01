module com.soundwave {
    // Модули JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    // Библиотеки для JSON (Jackson)
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310; // Исправляет твою ошибку!

    // Библиотека для аудио-тегов и логи
    requires jaudiotagger;
    requires org.slf4j;

    // Разрешаем JavaFX и Jackson доступ к нашим классам (рефлексия)
    opens com.soundwave to javafx.fxml;
    opens com.soundwave.gui to javafx.fxml;
    opens com.soundwave.core to com.fasterxml.jackson.databind;
    opens com.soundwave.library to com.fasterxml.jackson.databind;

    // Экспортируем пакеты для работы приложения
    exports com.soundwave;
    exports com.soundwave.gui;
    exports com.soundwave.core;
    exports com.soundwave.library;
}