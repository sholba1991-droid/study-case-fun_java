# PowerShell Runner untuk "Futsal & Mini Soccer Arena Malang"
Write-Host "=====================================================================" -ForegroundColor Green
Write-Host " FUTSAL & MINI SOCCER ARENA MALANG - RESERVASI LAPANGAN OLAHRAGA" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Green

$javacCmd = "javac"
$javaCmd = "java"
$customJdk = "C:\Program Files\Java\jdk-21.0.12.1\bin"

if (Test-Path "$customJdk\javac.exe") {
    $javacCmd = "$customJdk\javac.exe"
    $javaCmd = "$customJdk\java.exe"
}

if (!(Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "Mengompilasi file Java..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& $javacCmd -encoding UTF-8 -d bin $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "Kompilasi sukses! Membuka antarmuka GUI..." -ForegroundColor Green
    Start-Process -FilePath $javaCmd -ArgumentList "-cp bin com.arenamalang.futsal.Main"
} else {
    Write-Host "Kompilasi gagal. Silakan periksa pesan kesalahan di atas." -ForegroundColor Red
}
