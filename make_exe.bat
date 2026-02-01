@echo off
echo === BUILDING FINAL EXE (NO CONSOLE) ===

:: Путь к твоему JDK 21
set "JP_PATH=C:\Program Files\Java\jdk-21\bin\jpackage.exe"

:: Чистим старую сборку
rd /s /q dist 2>nul

:: Сборка
"%JP_PATH%" ^
  --type app-image ^
  --name "SoundWavePlayer" ^
  --input target/ ^
  --main-jar soundwave-player-1.0-SNAPSHOT.jar ^
  --main-class com.soundwave.gui.MainLauncher ^
  --icon "icon.ico" ^
  --dest dist

:: ВАЖНО: Убрали флаг --win-console, теперь окно не появится.
:: Параметр --icon "icon.ico" применит иконку к самому .exe файлу.

if %errorlevel% neq 0 (
    echo [ERROR] Build failed!
) else (
    echo === SUCCESS! Check dist/SoundWavePlayer ===
)
pause