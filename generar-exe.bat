@echo off
chcp 65001 >nul
title Generar TallerMecanico.exe
cd /d "%~dp0"
set "PATH=%PATH%;%SystemRoot%\System32\WindowsPowerShell\v1.0"

echo [1/4] Verificando Java...
where jpackage >nul 2>nul
if errorlevel 1 (
    echo ERROR: No se encontro jpackage. Instale el JDK 17 con "Add to PATH" activado.
    pause
    exit /b 1
)

echo [2/4] Compilando el proyecto (la primera vez tarda varios minutos)...
call mvnw.cmd -q clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Fallo la compilacion.
    pause
    exit /b 1
)

echo [3/4] Generando el ejecutable...
if exist dist rmdir /s /q dist
if exist empaque rmdir /s /q empaque
mkdir empaque
copy /y target\taller-mecanico-1.0.0.jar empaque\ >nul
jpackage --type app-image --name TallerMecanico --app-version 1.0.0 --vendor "Walter Cayte" --input empaque --main-jar taller-mecanico-1.0.0.jar --dest dist
if errorlevel 1 (
    echo ERROR: Fallo jpackage.
    pause
    exit /b 1
)
rmdir /s /q empaque

echo [4/4] Comprimiendo...
powershell -NoProfile -Command "Compress-Archive -Path dist\TallerMecanico -DestinationPath dist\TallerMecanico.zip -Force"

echo.
echo LISTO: dist\TallerMecanico\TallerMecanico.exe
echo Para otra PC copie la carpeta dist\TallerMecanico o el archivo dist\TallerMecanico.zip
pause
