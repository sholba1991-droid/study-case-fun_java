@echo off
chcp 65001 > nul
echo =====================================================================
echo  FUTSAL & MINI SOCCER ARENA MALANG - RESERVASI LAPANGAN OLAHRAGA
echo =====================================================================
echo Menyiapkan aplikasi Java...

REM Cek java di PATH atau gunakan path JDK yang terinstal
set "JAVA_CMD=java"
set "JAVAC_CMD=javac"

if exist "C:\Program Files\Java\jdk-21.0.12.1\bin\javac.exe" (
    set "JAVAC_CMD=C:\Program Files\Java\jdk-21.0.12.1\bin\javac.exe"
    set "JAVA_CMD=C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe"
)

if not exist bin (
    mkdir bin
)

echo Mengompilasi kode program...
dir /s /b src\*.java > sources.txt
"%JAVAC_CMD%" -encoding UTF-8 -d bin @sources.txt
del sources.txt

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Gagal mengompilasi program!
    pause
    exit /b %ERRORLEVEL%
)

echo Menjalankan aplikasi GUI...
start "" "%JAVA_CMD%" -cp bin com.arenamalang.futsal.Main
echo Aplikasi berhasil diluncurkan!
exit /b 0
